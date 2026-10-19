package com.example.data

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Enterprise cryptographic security manager demonstrating:
 * 1. Cryptographically secure per-user Salting (16 random bytes).
 * 2. Secret application Pepper applied to prevent offline rainbow table attacks.
 * 3. PBKDF2 with HMAC-SHA256 key derivation.
 * 4. RFC 6238 Time-based One-Time Password (TOTP) two-factor authentication.
 * 5. Single-use emergency backup recovery codes.
 */
object SecurityCryptoManager {

    // Server-side / application-level secret pepper. Never stored in database tables.
    const val AUTH_PASSWORD_PEPPER = "02456504a869a6e52b03d281897c926ddb2bfc1f88f5b1f678b064c49858989b"

    private const val PBKDF2_ITERATIONS = 10000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTE_LENGTH = 16

    private val secureRandom = SecureRandom()

    /**
     * Generates a cryptographically random 16-byte salt, returned as a hex string.
     */
    fun generateSalt(): String {
        val saltBytes = ByteArray(SALT_BYTE_LENGTH)
        secureRandom.nextBytes(saltBytes)
        return saltBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Hashes a password using the per-user salt and application pepper via PBKDF2WithHmacSHA256.
     * The pepper is combined with the password before derivation.
     */
    fun hashPassword(password: String, saltHex: String, pepper: String = AUTH_PASSWORD_PEPPER): String {
        val saltBytes = hexStringToByteArray(saltHex)
        // Pepper is concatenated to the password before hashing
        val pepperedPassword = "$password:$pepper".toCharArray()
        val spec = PBEKeySpec(pepperedPassword, saltBytes, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hashBytes = factory.generateSecret(spec).encoded
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifies an entered password against a stored salt and hash using constant-time comparison.
     */
    fun verifyPassword(enteredPassword: String, saltHex: String, storedHash: String, pepper: String = AUTH_PASSWORD_PEPPER): Boolean {
        return try {
            val computedHash = hashPassword(enteredPassword, saltHex, pepper)
            MessageDigest.isEqual(computedHash.toByteArray(Charsets.UTF_8), storedHash.toByteArray(Charsets.UTF_8))
        } catch (_: Exception) {
            false
        }
    }

    // =========================================================================
    // RFC 6238 TOTP (Time-Based One-Time Password) Implementation for 2FA
    // =========================================================================

    private const val TOTP_TIME_STEP_SECONDS = 30L
    private const val TOTP_DIGITS = 6

    /**
     * Generates a new Base32 secret key for Google Authenticator / Authy.
     */
    fun generateTotpSecret(): String {
        val bytes = ByteArray(20)
        secureRandom.nextBytes(bytes)
        return Base32.encode(bytes)
    }

    /**
     * Calculates the current 6-digit TOTP code for a Base32 secret.
     */
    fun generateTotpCode(secretBase32: String, timeMillis: Long = System.currentTimeMillis()): String {
        val key = Base32.decode(secretBase32)
        val timeStep = timeMillis / 1000L / TOTP_TIME_STEP_SECONDS
        return computeTotp(key, timeStep)
    }

    /**
     * Verifies a 6-digit TOTP code against the secret, allowing a +/- 1 step window for clock drift.
     */
    fun verifyTotpCode(secretBase32: String, code: String, timeMillis: Long = System.currentTimeMillis()): Boolean {
        if (code.isBlank() || code.length != TOTP_DIGITS) return false
        val key = try {
            Base32.decode(secretBase32)
        } catch (_: Exception) {
            return false
        }
        val currentStep = timeMillis / 1000L / TOTP_TIME_STEP_SECONDS
        // Check window: previous step (-30s), current step, next step (+30s)
        for (stepOffset in -1..1) {
            val candidate = computeTotp(key, currentStep + stepOffset)
            if (candidate == code) {
                return true
            }
        }
        return false
    }

    private fun computeTotp(key: ByteArray, timeStep: Long): String {
        val data = ByteBuffer.allocate(8).putLong(timeStep).array()
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "RAW"))
        val hash = mac.doFinal(data)
        val offset = hash[hash.size - 1].toInt() and 0x0F
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)
        val otp = binary % 1000000
        return "%06d".format(otp)
    }

    /**
     * Generates 5 emergency recovery backup codes (e.g., RECV-A1B2-C3D4).
     */
    fun generateRecoveryCodes(): List<String> {
        val alphabet = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        return (1..5).map {
            val part1 = (1..4).map { alphabet[secureRandom.nextInt(alphabet.length)] }.joinToString("")
            val part2 = (1..4).map { alphabet[secureRandom.nextInt(alphabet.length)] }.joinToString("")
            "RECV-$part1-$part2"
        }
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    /**
     * Minimal RFC 4648 Base32 encoder/decoder for TOTP secrets without heavy external dependencies.
     */
    object Base32 {
        private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

        fun encode(bytes: ByteArray): String {
            val sb = StringBuilder()
            var buffer = 0
            var bitsLeft = 0
            for (b in bytes) {
                buffer = (buffer shl 8) or (b.toInt() and 0xFF)
                bitsLeft += 8
                while (bitsLeft >= 5) {
                    val index = (buffer shr (bitsLeft - 5)) and 0x1F
                    sb.append(ALPHABET[index])
                    bitsLeft -= 5
                }
            }
            if (bitsLeft > 0) {
                val index = (buffer shl (5 - bitsLeft)) and 0x1F
                sb.append(ALPHABET[index])
            }
            return sb.toString()
        }

        fun decode(s: String): ByteArray {
            val clean = s.uppercase().filter { it in ALPHABET }
            val bytes = mutableListOf<Byte>()
            var buffer = 0
            var bitsLeft = 0
            for (c in clean) {
                val val5 = ALPHABET.indexOf(c)
                if (val5 < 0) continue
                buffer = (buffer shl 5) or val5
                bitsLeft += 5
                if (bitsLeft >= 8) {
                    bytes.add(((buffer shr (bitsLeft - 8)) and 0xFF).toByte())
                    bitsLeft -= 8
                }
            }
            return bytes.toByteArray()
        }
    }
}
