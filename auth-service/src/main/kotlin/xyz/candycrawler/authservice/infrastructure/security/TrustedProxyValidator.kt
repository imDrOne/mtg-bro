package xyz.candycrawler.authservice.infrastructure.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.InetAddress

@Component
class TrustedProxyValidator(
    @Value("\${auth.trusted-proxy-cidr}") cidr: String,
) {

    private val networkAddress: InetAddress
    private val prefixLength: Int

    init {
        val parts = cidr.split("/")
        networkAddress = InetAddress.getByName(parts[0])
        prefixLength = parts[1].toInt()
    }

    fun isTrusted(remoteAddr: String): Boolean {
        return try {
            val remote = InetAddress.getByName(remoteAddr)
            isInCidr(remote)
        } catch (_: Exception) {
            false
        }
    }

    private fun isInCidr(address: InetAddress): Boolean {
        val networkBytes = networkAddress.address
        val addressBytes = address.address
        if (networkBytes.size != addressBytes.size) return false

        val fullBytes = prefixLength / 8
        val remainingBits = prefixLength % 8

        for (i in 0 until fullBytes) {
            if (networkBytes[i] != addressBytes[i]) return false
        }

        if (remainingBits > 0 && fullBytes < networkBytes.size) {
            val mask = (0xFF shl (8 - remainingBits)) and 0xFF
            if ((networkBytes[fullBytes].toInt() and mask) != (addressBytes[fullBytes].toInt() and mask)) return false
        }

        return true
    }
}
