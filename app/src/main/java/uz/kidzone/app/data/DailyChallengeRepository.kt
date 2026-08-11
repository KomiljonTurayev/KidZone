// app/src/main/java/uz/kidzone/app/data/DailyChallengeRepository.kt
package uz.kidzone.app.data

import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import uz.kidzone.app.FirestoreSync
import com.google.firebase.auth.FirebaseAuth

data class GameItem(val id: String, val title: String)

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
                GameItem(obj.getString("id"), obj.getString("title"))
            }
        } catch (_: Exception) {}
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
        return updateStreak(profileId, today, gameId, challenge.gameTitle)
    }

    private suspend fun updateStreak(profileId: String, today: String, gameId: String, gameTitle: String): Int? {
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

        val uid = try {
            FirebaseAuth.getInstance().currentUser?.uid
        } catch (_: Exception) {
            null
        } ?: return newlyReached
        firestoreSync.syncStreak(uid, profileId, newCount, today)
        firestoreSync.syncChallengeCompleted(uid, profileId, today, gameId, gameTitle)
        return newlyReached
    }

    open suspend fun getStreak(profileId: String): StreakEntity =
        streakDao.getByProfile(profileId) ?: StreakEntity(profileId)

    fun getAllStreaks(): Flow<List<StreakEntity>> = streakDao.getAll()

    fun getChallengesFlow(profileId: String): Flow<List<DailyChallengeEntity>> =
        challengeDao.getByProfile(profileId)
}
