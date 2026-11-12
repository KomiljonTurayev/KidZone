package uz.kidzone.app.agegate

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.Period

/**
 * Google Play Families Policy — "Neutral Age Screen".
 *
 * MUHIM qoidalar (Google rad etish sababi shu bandlar):
 *  1. Ekran NEYTRAL bo'lishi kerak — hech qanday maydon oldindan to'ldirilmaydi,
 *     "13+ bo'lishingiz kerak" degan yozuv bo'lmaydi.
 *  2. Natija bir marta olinadi va saqlanadi. Foydalanuvchi qayta urinib,
 *     boshqa yosh kiritolmasligi kerak (reset tugmasi qo'yilmaydi).
 *  3. Ekran ilovaning BIRINCHI ishga tushishida, har qanday kontent,
 *     reklama yoki SDK initsializatsiyasidan OLDIN ko'rsatiladi.
 *  4. Natijaga qarab kontent va SDK'lar filtrlanadi ([AgeGatePolicy]).
 *
 * SharedPreferences'da saqlanadi (DataStore emas): natija `Application.onCreate`da, SDK'lar
 * yoqilishidan oldin sinxron o'qilishi kerak.
 */

/** Yosh guruhi — butun ilova shu qiymatga qarab ishlaydi. */
enum class AgeBracket {
    /** 13 yosh va undan kichik → faqat bolalarga mos kontent va approved SDK'lar. */
    CHILD,

    /** 13 yoshdan katta → to'liq tajriba. */
    TEEN_OR_ADULT,
}

data class AgeGateState(
    val completed: Boolean,
    val bracket: AgeBracket?,
) {
    /** Ekran hali o'tilmagan bo'lsa ham `true` — natija bo'lmaguncha cheklangan rejim. */
    val isRestricted: Boolean get() = bracket != AgeBracket.TEEN_OR_ADULT
}

class AgeGateRepository(
    private val prefs: SharedPreferences,
    private val today: () -> LocalDate = LocalDate::now,
) {
    private val _state = MutableStateFlow(read())
    val state: StateFlow<AgeGateState> = _state.asStateFlow()

    fun current(): AgeGateState = _state.value

    /**
     * Foydalanuvchi kiritgan tug'ilgan oy/yilni saqlaydi.
     * Faqat bir marta yoziladi — qayta chaqirilsa `false` qaytaradi va hech narsa o'zgarmaydi.
     */
    fun save(birthYear: Int, birthMonth: Int): Boolean {
        require(birthMonth in 1..12) { "month out of range: $birthMonth" }
        require(isPlausibleYear(birthYear, today())) { "implausible year: $birthYear" }
        if (prefs.getBoolean(KEY_COMPLETED, false)) return false // qayta urinishga yo'l yo'q
        prefs.edit()
            .putInt(KEY_BIRTH_YEAR, birthYear)
            .putInt(KEY_BIRTH_MONTH, birthMonth)
            .putBoolean(KEY_COMPLETED, true)
            .commit()
        _state.value = read()
        return true
    }

    private fun read(): AgeGateState {
        val completed = prefs.getBoolean(KEY_COMPLETED, false)
        val year = prefs.getInt(KEY_BIRTH_YEAR, 0)
        val month = prefs.getInt(KEY_BIRTH_MONTH, 0)
        return AgeGateState(
            completed = completed,
            bracket = if (completed && year > 0 && month in 1..12) bracketFor(year, month, today()) else null,
        )
    }

    companion object {
        const val PREFS_NAME = "age_gate"
        private const val KEY_COMPLETED = "age_gate_completed"
        private const val KEY_BIRTH_YEAR = "birth_year"
        private const val KEY_BIRTH_MONTH = "birth_month"

        /** 13 yosh — Families Policy chegarasi. */
        const val CHILD_MAX_AGE = 13

        fun bracketFor(
            birthYear: Int,
            birthMonth: Int,
            today: LocalDate = LocalDate.now(),
        ): AgeBracket {
            val birth = LocalDate.of(birthYear, birthMonth, 1)
            val age = Period.between(birth, today).years
            return if (age <= CHILD_MAX_AGE) AgeBracket.CHILD else AgeBracket.TEEN_OR_ADULT
        }

        fun isPlausibleYear(year: Int, today: LocalDate = LocalDate.now()): Boolean =
            year in (today.year - 120)..today.year
    }
}
