package uz.kidzone.app

import android.content.SharedPreferences
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

object PinUtil {

    @JvmStatic
    fun hash(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(pin.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    @JvmStatic
    fun matches(pin: String, storedHash: String?): Boolean =
        storedHash != null && storedHash == hash(pin)

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
}
