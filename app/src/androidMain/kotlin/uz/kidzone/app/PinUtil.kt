package uz.kidzone.app

import android.content.SharedPreferences
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Parent PIN hashing. Current format: "v2$<salt>$<hash>" — PBKDF2-HMAC-SHA256 with a random
 * per-PIN salt, both parts unpadded base64. The hash is synced to Firestore (profiles/{id}),
 * whose rules cap pinHash at 64 chars, so the salt and output are 128 bits each (48 chars total).
 *
 * A 4-digit PIN has only 10k values, so no hash makes it uncrackable offline; the salt stops
 * one precomputed table from covering every family, and the iterations make each guess costly.
 * The real defense on-device is PinGate's lockout.
 *
 * Legacy hashes (unsalted SHA-256, 64 hex chars) still verify; callers should replace them via
 * [needsRehash] after a successful [matches].
 */
object PinUtil {

    private const val PREFIX = "v2$"
    private const val ITERATIONS = 100_000
    private const val SALT_BYTES = 16
    private const val KEY_BITS = 128

    private val random = SecureRandom()
    private val b64Encoder = Base64.getEncoder().withoutPadding()
    private val b64Decoder = Base64.getDecoder()

    @JvmStatic
    fun hash(pin: String): String {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        return PREFIX + b64Encoder.encodeToString(salt) + "$" + b64Encoder.encodeToString(pbkdf2(pin, salt))
    }

    @JvmStatic
    fun matches(pin: String, storedHash: String?): Boolean {
        if (storedHash.isNullOrEmpty()) return false
        if (!storedHash.startsWith(PREFIX)) {
            return MessageDigest.isEqual(legacyHash(pin).toByteArray(), storedHash.toByteArray())
        }
        val parts = storedHash.removePrefix(PREFIX).split('$')
        if (parts.size != 2) return false
        return try {
            val salt = b64Decoder.decode(parts[0])
            val expected = b64Decoder.decode(parts[1])
            MessageDigest.isEqual(pbkdf2(pin, salt), expected)
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    /** True for a stored hash in an older, weaker format that should be replaced. */
    @JvmStatic
    fun needsRehash(storedHash: String?): Boolean =
        !storedHash.isNullOrEmpty() && !storedHash.startsWith(PREFIX)

    @JvmStatic
    fun isPlainPin(stored: String?): Boolean =
        stored != null && stored.length == 4 && stored.all { it.isDigit() }

    @JvmStatic
    fun getOrMigrateHash(prefs: SharedPreferences, key: String): String? {
        val stored = prefs.getString(key, null) ?: return null
        if (stored.isEmpty()) return stored
        if (isPlainPin(stored)) {
            val hashed = hash(stored)
            prefs.edit().putString(key, hashed).apply()
            return hashed
        }
        return stored
    }

    private fun pbkdf2(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun legacyHash(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(pin.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
