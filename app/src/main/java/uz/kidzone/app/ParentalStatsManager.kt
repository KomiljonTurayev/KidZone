package uz.kidzone.app

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ParentalStatsManager {

    companion object {
        private const val KEY_TIME_LIMIT = "kz_time_limit"

        @JvmStatic
        fun todayPtKey(): String =
            "kz_pt_" + SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

        @JvmStatic
        fun todayGlKey(): String =
            "kz_gl_" + SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

        @JvmStatic
        fun parseList(csv: String?): MutableList<String> {
            if (csv.isNullOrEmpty()) return mutableListOf()
            return csv.split(",").toMutableList()
        }

        @JvmStatic
        fun joinList(list: List<String>): String = list.joinToString(",")
    }

    private val prefs: SharedPreferences
    private var sessionStartMs: Long = 0L
    private val sessionGames: MutableList<String> = mutableListOf()

    constructor(ctx: Context) : this(ctx.getSharedPreferences("kz_prefs", Context.MODE_PRIVATE))

    // Package-private-equivalent constructor for testing
    internal constructor(prefs: SharedPreferences) {
        this.prefs = prefs
    }

    fun onSessionStart() {
        sessionStartMs = System.currentTimeMillis()
        sessionGames.clear()
    }

    fun onSessionEnd() {
        if (sessionStartMs == 0L) return
        val elapsed = ((System.currentTimeMillis() - sessionStartMs) / 60_000L).toInt()
        sessionStartMs = 0L
        if (elapsed <= 0) return
        val key = todayPtKey()
        prefs.edit().putInt(key, prefs.getInt(key, 0) + elapsed).apply()
    }

    fun onGameLaunched(gameId: String?) {
        if (gameId.isNullOrEmpty()) return
        if (!sessionGames.contains(gameId)) sessionGames.add(gameId)
        val key = todayGlKey()
        val existing = prefs.getString(key, "") ?: ""
        val list = parseList(existing)
        if (!list.contains(gameId)) {
            list.add(gameId)
            prefs.edit().putString(key, joinList(list)).apply()
        }
    }

    fun getSessionMinutes(): Long {
        if (sessionStartMs == 0L) return 0L
        return (System.currentTimeMillis() - sessionStartMs) / 60_000L
    }

    fun getSessionGames(): List<String> = ArrayList(sessionGames)

    fun getTodayMinutes(): Int {
        val saved = prefs.getInt(todayPtKey(), 0)
        val current = if (sessionStartMs > 0L)
            ((System.currentTimeMillis() - sessionStartMs) / 60_000L).toInt()
        else 0
        return saved + current
    }

    fun getWeeklyMinutes(): IntArray {
        val result = IntArray(7)
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.US)
        val cal = Calendar.getInstance()
        for (i in 6 downTo 0) {
            val key = "kz_pt_" + sdf.format(cal.time)
            result[i] = prefs.getInt(key, 0)
            if (i == 6 && sessionStartMs > 0L) {
                result[i] += ((System.currentTimeMillis() - sessionStartMs) / 60_000L).toInt()
            }
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return result
    }

    fun getTodayGames(): List<String> =
        parseList(prefs.getString(todayGlKey(), "") ?: "")

    fun getTimeLimitMinutes(): Int = prefs.getInt(KEY_TIME_LIMIT, 0)

    fun setTimeLimitMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_TIME_LIMIT, maxOf(0, minutes)).apply()
    }

    fun isTimeLimitReached(): Boolean {
        val limit = getTimeLimitMinutes()
        return limit > 0 && getTodayMinutes() >= limit
    }
}
