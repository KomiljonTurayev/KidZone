package uz.kidzone.app.agegate

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Neytral yosh ekrani natijasini jarayon bo'ylab ulaydi ([uz.kidzone.app.auth.AuthGraph] kabi).
 * [init] `Application.onCreate`da, har qanday SDK'dan oldin chaqiriladi va [AgeGatePolicy]ni
 * saqlangan natija bilan qo'llaydi.
 */
object AgeGate {

    private val NOT_COMPLETED = AgeGateState(completed = false, bracket = null)

    @Volatile private var repository: AgeGateRepository? = null

    /** [init]dan oldin (va unit testlarda) — o'tilmagan holat, ya'ni cheklangan rejim. */
    val state: StateFlow<AgeGateState>
        get() = repository?.state ?: MutableStateFlow(NOT_COMPLETED)

    fun current(): AgeGateState = repository?.current() ?: NOT_COMPLETED

    /** Kidzo AI, kirish, to'lovlar va push yopiqmi (bola yoki ekran hali o'tilmagan). */
    val isRestricted: Boolean get() = current().isRestricted

    fun init(context: Context) {
        if (repository != null) return
        val repo = AgeGateRepository(
            context.getSharedPreferences(AgeGateRepository.PREFS_NAME, Context.MODE_PRIVATE),
        )
        repository = repo
        AgeGatePolicy.apply(context, repo.current())
    }

    /**
     * Ekran natijasini bir marta saqlaydi va siyosatni darhol qo'llaydi. Allaqachon saqlangan
     * bo'lsa hech narsa o'zgarmaydi — mavjud natija qaytariladi.
     */
    fun submit(context: Context, birthYear: Int, birthMonth: Int): AgeGateState {
        val repo = repository ?: error("AgeGate.init() must run in Application.onCreate()")
        if (repo.save(birthYear, birthMonth)) AgeGatePolicy.apply(context, repo.current())
        return repo.current()
    }
}
