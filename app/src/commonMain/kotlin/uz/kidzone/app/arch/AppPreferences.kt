package uz.kidzone.app.arch

expect class AppPreferences {
    fun getString(key: String, defaultValue: String): String
    fun putString(key: String, value: String)
    
    fun getInt(key: String, defaultValue: Int): Int
    fun putInt(key: String, value: Int)
    
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
}
