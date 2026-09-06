// app/src/main/java/uz/kidzone/app/data/AppClock.kt
package uz.kidzone.app.data

import java.time.LocalDate
import java.time.ZoneId

object AppClock {
    val ZONE: ZoneId = ZoneId.of("Asia/Tashkent")
    fun today(): String = LocalDate.now(ZONE).toString()
}
