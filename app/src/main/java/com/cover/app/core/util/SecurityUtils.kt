package com.cover.app.core.util

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

object SecurityUtils {
    
    fun generateRandomId(): String {
        return UUID.randomUUID().toString()
    }
    
    fun generateSecureRandomBytes(length: Int): ByteArray {
        val bytes = ByteArray(length)
        SecureRandom().nextBytes(bytes)
        return bytes
    }
    
    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(pin.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    fun verifyPin(pin: String, hash: String): Boolean {
        return hashPin(pin) == hash
    }
}
