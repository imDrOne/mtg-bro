package xyz.candycrawler.authservice.infrastructure.db.entity

internal data class RsaKeyRecord(val keyId: String, val publicKey: String, val privateKey: String)
