#!/usr/bin/env python3
"""
qdrant-migrate.py - idempotent Qdrant schema migration for mtg-bro.

Usage:
  python3 scripts/qdrant-migrate.py

Configuration:
  QDRANT_URL                 http://localhost:6333
  QDRANT_API_KEY             optional
  QDRANT_COLLECTION          draftsim_article_insights_v1
  QDRANT_VECTOR_SIZE         1536
  QDRANT_DISTANCE            Cosine
  AI_EMBEDDING_MODEL         text-embedding-3-small
"""

import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass
from typing import Any, Dict, Optional, Tuple


PAYLOAD_INDEXES: Dict[str, str] = {
    "article_id": "integer",
    "favorite": "bool",
    "article_type": "keyword",
    "processing_profile": "keyword",
    "insight_type": "keyword",
    "subject": "keyword",
    "tags": "keyword",
}


@dataclass(frozen=True)
class Config:
    url: str
    api_key: Optional[str]
    collection: str
    vector_size: int
    distance: str
    embedding_model: str


def load_config() -> Config:
    vector_size = int(os.getenv("QDRANT_VECTOR_SIZE", "1536"))
    if vector_size <= 0:
        raise ValueError("QDRANT_VECTOR_SIZE must be positive")

    return Config(
        url=os.getenv("QDRANT_URL", "http://localhost:6333").rstrip("/"),
        api_key=os.getenv("QDRANT_API_KEY") or None,
        collection=os.getenv("QDRANT_COLLECTION", "draftsim_article_insights_v1"),
        vector_size=vector_size,
        distance=os.getenv("QDRANT_DISTANCE", "Cosine"),
        embedding_model=os.getenv("AI_EMBEDDING_MODEL", "text-embedding-3-small"),
    )


def request(
    config: Config,
    method: str,
    path: str,
    body: Optional[Dict[str, Any]] = None,
) -> Tuple[int, Optional[Dict[str, Any]]]:
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(
        url=f"{config.url}{path}",
        data=data,
        method=method,
        headers={
            "Content-Type": "application/json",
            **({"api-key": config.api_key} if config.api_key else {}),
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            text = response.read().decode("utf-8")
            return response.status, json.loads(text) if text else None
    except urllib.error.HTTPError as exc:
        if exc.code == 404:
            return exc.code, None
        details = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"{method} {path} failed: HTTP {exc.code}: {details}") from exc


def get_collection(config: Config) -> Optional[Dict[str, Any]]:
    status, response = request(config, "GET", f"/collections/{urllib.parse.quote(config.collection)}")
    if status == 404:
        return None
    return response


def create_collection(config: Config) -> None:
    body = {
        "vectors": {
            "size": config.vector_size,
            "distance": config.distance,
        },
        "metadata": {
            "schema_version": 1,
            "embedding_model": config.embedding_model,
            "embedding_dimensions": config.vector_size,
        },
    }
    request(config, "PUT", f"/collections/{urllib.parse.quote(config.collection)}?wait=true", body)
    print(f"created collection {config.collection} ({config.vector_size}, {config.distance})")


def vector_params(collection_response: Dict[str, Any]) -> Dict[str, Any]:
    return collection_response.get("result", {}).get("config", {}).get("params", {}).get("vectors", {})


def payload_schema(collection_response: Dict[str, Any]) -> Dict[str, Any]:
    return collection_response.get("result", {}).get("payload_schema", {})


def validate_collection(config: Config, collection_response: Dict[str, Any]) -> None:
    vectors = vector_params(collection_response)
    size = vectors.get("size")
    distance = vectors.get("distance")
    if size != config.vector_size or str(distance).lower() != config.distance.lower():
        raise RuntimeError(
            f"collection {config.collection} has incompatible vector params: "
            f"size={size}, distance={distance}; "
            f"expected size={config.vector_size}, distance={config.distance}. "
            "Create a new versioned collection instead of changing this one in-place."
        )
    print(f"collection {config.collection} already exists ({size}, {distance})")


def schema_data_type(field_schema: Any) -> Optional[str]:
    if isinstance(field_schema, str):
        return field_schema
    if isinstance(field_schema, dict):
        value = field_schema.get("data_type") or field_schema.get("type")
        return str(value) if value is not None else None
    return None


def ensure_payload_indexes(config: Config, collection_response: Dict[str, Any]) -> None:
    existing_schema = payload_schema(collection_response)
    path = f"/collections/{urllib.parse.quote(config.collection)}/index?wait=true"
    for field_name, field_schema in PAYLOAD_INDEXES.items():
        existing_type = schema_data_type(existing_schema.get(field_name))
        if existing_type is not None:
            if existing_type.lower() != field_schema.lower():
                raise RuntimeError(
                    f"payload index {field_name} has incompatible type {existing_type}; "
                    f"expected {field_schema}"
                )
            print(f"payload index {field_name}:{field_schema} already exists")
            continue

        body = {
            "field_name": field_name,
            "field_schema": field_schema,
        }
        request(config, "PUT", path, body)
        print(f"ensured payload index {field_name}:{field_schema}")


def main() -> int:
    config = load_config()
    collection = get_collection(config)
    if collection is None:
        create_collection(config)
        collection = get_collection(config)
        if collection is None:
            raise RuntimeError(f"collection {config.collection} was created but cannot be read back")
    else:
        validate_collection(config, collection)

    ensure_payload_indexes(config, collection)
    print("qdrant migration complete")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        raise SystemExit(1)
