package xyz.candycrawler.authservice.infrastructure.db.mapper.sql

import com.nimbusds.jose.jwk.RSAKey
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import xyz.candycrawler.authservice.infrastructure.db.entity.RsaKeyRecord
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.UUID

@Component
class RsaKeySqlMapper(
    private val jdbc: NamedParameterJdbcTemplate,
    private val transactionTemplate: TransactionTemplate,
) {

    internal fun loadOrGenerate(): RSAKey {
        val existing = jdbc.query(
            "SELECT key_id, public_key, private_key FROM rsa_keys LIMIT 1",
            MapSqlParameterSource(),
        ) { rs, _ ->
            RsaKeyRecord(
                keyId = rs.getString("key_id"),
                publicKey = rs.getString("public_key"),
                privateKey = rs.getString("private_key"),
            )
        }.firstOrNull()

        if (existing != null) return deserialize(existing)

        val generated = generate()
        transactionTemplate.executeWithoutResult { persist(generated) }
        return generated
    }

    private fun generate(): RSAKey {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(RSA_KEY_SIZE_BITS)
        val keyPair = keyPairGenerator.generateKeyPair()
        return RSAKey.Builder(keyPair.public as RSAPublicKey)
            .privateKey(keyPair.private as RSAPrivateKey)
            .keyID(UUID.randomUUID().toString())
            .build()
    }

    private fun persist(key: RSAKey) {
        val encoder = Base64.getEncoder()
        val params = MapSqlParameterSource()
            .addValue("keyId", key.keyID)
            .addValue("publicKey", encoder.encodeToString(key.toPublicKey().encoded))
            .addValue("privateKey", encoder.encodeToString(key.toPrivateKey().encoded))

        jdbc.update(
            "INSERT INTO rsa_keys (key_id, public_key, private_key) VALUES (:keyId, :publicKey, :privateKey)",
            params,
        )
    }

    private fun deserialize(record: RsaKeyRecord): RSAKey {
        val decoder = Base64.getDecoder()
        val keyFactory = KeyFactory.getInstance("RSA")

        val publicKey = keyFactory.generatePublic(
            X509EncodedKeySpec(decoder.decode(record.publicKey)),
        ) as RSAPublicKey

        val privateKey = keyFactory.generatePrivate(
            PKCS8EncodedKeySpec(decoder.decode(record.privateKey)),
        ) as RSAPrivateKey

        return RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(record.keyId)
            .build()
    }

    private companion object {
        const val RSA_KEY_SIZE_BITS = 2048
    }
}
