// app/src/main/java/uz/kidzone/app/data/DailyChallengeRepository.kt
package uz.kidzone.app.data

import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import uz.kidzone.app.FirestoreSync
import com.google.firebase.auth.FirebaseAuth

data class GameItem(
    val id: String,
    val title: String,
    val emoji: String = "",
    val names: Map<String, String> = emptyMap(),
)

open class DailyChallengeRepository(
    private val challengeDao: DailyChallengeDao,
    private val streakDao: StreakDao,
    private val firestoreSync: FirestoreSync,
    private val todayProvider: () -> String = {
        AppClock.today()
    },
) {
    private var gamesList: List<GameItem> = emptyList()
    private val milestones = listOf(3, 7, 14, 30)

    open fun updateGamesList(json: String) {
        try {
            val arr = JSONArray(json)
            gamesList = (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                val names = obj.optJSONObject("names")
                GameItem(
                    obj.getString("id"),
                    obj.getString("title"),
                    obj.optString("em", ""),
                    names?.keys()?.asSequence()?.associateWith { names.getString(it) }.orEmpty(),
                )
            }
        } catch (_: Exception) {}
    }

    /** Catalog game ids in catalog order (popular first). */
    open fun gameIds(): List<String> = gamesList.map { it.id }

    /** "🐾 Animals" for a catalog game id, in [lang] when the catalog has it; null if unknown. */
    open fun gameLabel(id: String, lang: String): String? {
        val g = gamesList.firstOrNull { it.id == id } ?: return null
        val name = g.names[lang] ?: g.title
        return if (g.emoji.isEmpty()) name else "${g.emoji} $name"
    }

    open suspend fun getTodayChallenge(profileId: String): DailyChallengeEntity? {
        val today = todayProvider()
        val id = "$profileId-$today"
        challengeDao.getById(id)?.let { return it }

        val games = gamesList.ifEmpty { return null }
        val game = games.random()
        val entity = DailyChallengeEntity(
            id = id,
            profileId = profileId,
            date = today,
            gameId = game.id,
            gameTitle = game.title,
        )
        challengeDao.insert(entity)
        return entity
    }

    open suspend fun markChallengeCompleted(profileId: String, gameId: String): Int? {
        val today = todayProvider()
        val id = "$profileId-$today"
        val challenge = challengeDao.getById(id) ?: return null
        if (challenge.gameId != gameId || challenge.completed) return null

        challengeDao.markCompleted(id)
        val milestone = updateStreak(profileId, today)
        cloudUid()?.let { firestoreSync.syncChallengeCompleted(it, profileId, today, gameId, challenge.gameTitle) }
        return milestone
    }

    /**
     * Today's three tasks (Faza 18): play the picked game, reach a new level anywhere, earn
     * some stars. Null while the WebView has not sent the game list yet.
     */
    open suspend fun getTodayTasks(profileId: String, age: String): List<DailyChallengeEntity>? {
        val play = getTodayChallenge(profileId) ?: return null
        val today = play.date
        val level = ensureTask(profileId, today, DailyChallengeEntity.SLOT_LEVEL, 1)
        val stars = ensureTask(profileId, today, DailyChallengeEntity.SLOT_STARS, starsTarget(age))
        return listOf(play, level, stars)
    }

    /** A new best level in any game. Returns a newly reached streak milestone, if any. */
    open suspend fun onLevelUp(profileId: String): Int? =
        addProgress(profileId, DailyChallengeEntity.SLOT_LEVEL, 1)

    open suspend fun onStarsEarned(profileId: String, stars: Int): Int? =
        if (stars <= 0) null else addProgress(profileId, DailyChallengeEntity.SLOT_STARS, stars)

    private suspend fun ensureTask(profileId: String, date: String, slot: Int, target: Int): DailyChallengeEntity {
        val id = DailyChallengeEntity.idFor(profileId, date, slot)
        challengeDao.getById(id)?.let { return it }
        val task = DailyChallengeEntity(
            id = id, profileId = profileId, date = date, gameId = "", gameTitle = "",
            slot = slot, target = target,
        )
        challengeDao.insert(task)
        return task
    }

    private suspend fun addProgress(profileId: String, slot: Int, amount: Int): Int? {
        val today = todayProvider()
        val task = challengeDao.getById(DailyChallengeEntity.idFor(profileId, today, slot)) ?: return null
        if (task.completed) return null
        val progress = (task.progress + amount).coerceAtMost(task.target)
        val done = progress >= task.target
        challengeDao.setProgress(task.id, progress, done)
        return if (done) updateStreak(profileId, today) else null
    }

    /** Any finished task keeps the streak going: small children rarely manage all three. */
    private suspend fun updateStreak(profileId: String, today: String): Int? {
        val current = streakDao.getByProfile(profileId) ?: StreakEntity(profileId)
        val yesterday = java.time.LocalDate.parse(today).minusDays(1).toString()
        val newCount = when (current.lastCompletedDate) {
            today -> current.count
            yesterday -> current.count + 1
            else -> 1
        }
        val resetMilestone = if (newCount == 1 && current.count > 1) 0 else current.lastCelebratedMilestone
        val newlyReached = milestones.lastOrNull { it <= newCount && it > resetMilestone }
        val celebratedMilestone = newlyReached ?: resetMilestone
        streakDao.upsert(StreakEntity(profileId, newCount, today, celebratedMilestone))
        cloudUid()?.let { firestoreSync.syncStreak(it, profileId, newCount, today) }
        return newlyReached
    }

    private fun cloudUid(): String? = ProfileOwner.uidFor(
        try {
            FirebaseAuth.getInstance().currentUser?.uid
        } catch (_: Exception) {
            null
        }
    )

    companion object {
        /** Stars a child of [age] can earn in a short play session. */
        fun starsTarget(age: String): Int = when (age) {
            "2-4" -> 30
            "8+" -> 80
            else -> 50
        }
    }

    open suspend fun getStreak(profileId: String): StreakEntity =
        streakDao.getByProfile(profileId) ?: StreakEntity(profileId)

    fun getAllStreaks(): Flow<List<StreakEntity>> = streakDao.getAll()

    fun getChallengesFlow(profileId: String): Flow<List<DailyChallengeEntity>> =
        challengeDao.getByProfile(profileId)
}
