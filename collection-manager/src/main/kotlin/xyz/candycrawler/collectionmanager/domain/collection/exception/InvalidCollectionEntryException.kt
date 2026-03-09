package xyz.candycrawler.collectionmanager.domain.collection.exception

class InvalidCollectionEntryException(reason: String) : RuntimeException("Invalid collection entry: $reason")
