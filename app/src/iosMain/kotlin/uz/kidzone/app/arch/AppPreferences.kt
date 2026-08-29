package uz.kidzone.app.arch

import platform.Foundation.NSUserDefaults

actual class AppPreferences {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun getString(key: String, defaultValue: String): String {
        return defaults.stringForKey(key) ?: defaultValue
    }

    actual fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }

    actual fun getInt(key: String, defaultValue: Int): Int {
        if (defaults.objectForKey(key) == null) return defaultValue
        return defaults.integerForKey(key).toInt()
    }

    actual fun putInt(key: String, value: Int) {
        defaults.setInteger(value.toLong(), forKey = key)
    }

    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        if (defaults.objectForKey(key) == null) return defaultValue
        return defaults.boolForKey(key)
    }

    actual fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, forKey = key)
    }
}
