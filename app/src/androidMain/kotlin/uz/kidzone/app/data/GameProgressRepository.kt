package uz.kidzone.app.data

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import uz.kidzone.app.FirestoreSync
import java.time.LocalDate

/**
 * Game levels reported by the WebView (AndroidBridge.levelReached). Room keeps them for the
 * parent dashboard; the family's cloud copy keeps the best level per game.
 */
open class GameProgressRepository(
    private val dao: LevelUpDao,
    private val firestoreSync: FirestoreSync,
    private val todayProvider: () -> String = { AppClock.today() },
    private val currentUid: () -> String? = {
        try { FirebaseAuth.getInstance().currentUser?.uid } catch (_: Exception) { null }
    },
) {
    open suspend fun onLevelReached(profileId: String, gameId: String, level: Int) {
        if (!isValid(gameId, level)) return
        val inserted = dao.insert(LevelUpEntity(profileId, gameId, level, todayProvider())) != -1L
        if (!inserted) return
        val uid = ProfileOwner.uidFor(currentUid()) ?: return
        val best = dao.bestLevel(profileId, gameId) ?: level
        firestoreSync.syncGameProgress(uid, profileId, gameId, best)
    }

    /** Best level per game, highest first, with the levels gained over the last 7 days. */
    fun progress(profileId: String): Flow<List<GameProgress>> =
        dao.progress(profileId, LocalDate.parse(todayProvider()).minusDays(6).toString())

    companion object {
        const val MAX_LEVEL = 10_000
        private val GAME_ID = Regex("[a-z0-9_-]{1,40}")

        fun isValid(gameId: String, level: Int): Boolean =
            GAME_ID.matches(gameId) && level in 1..MAX_LEVEL
    }
}
