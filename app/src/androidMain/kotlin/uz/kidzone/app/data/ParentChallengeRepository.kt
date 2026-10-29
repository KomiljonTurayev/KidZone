package uz.kidzone.app.data

import kotlinx.coroutines.flow.Flow

open class ParentChallengeRepository(
    private val dao: ParentChallengeDao,
    private val levels: LevelUpDao,
    private val now: () -> Long = System::currentTimeMillis,
) {
    fun observe(profileId: String): Flow<ParentChallengeEntity?> = dao.observe(profileId)

    /** Best level the child has in [gameId] so far (1 if never played). */
    open suspend fun bestLevel(profileId: String, gameId: String): Int =
        levels.bestLevel(profileId, gameId) ?: 1

    open suspend fun set(profileId: String, gameId: String, targetLevel: Int) {
        if (!GameProgressRepository.isValid(gameId, targetLevel)) return
        val done = bestLevel(profileId, gameId) >= targetLevel
        dao.upsert(ParentChallengeEntity(profileId, gameId, targetLevel, now(), if (done) now() else null))
    }

    open suspend fun clear(profileId: String) = dao.clear(profileId)

    /** True when this level just finished the parent's challenge. */
    open suspend fun onLevelReached(profileId: String, gameId: String, level: Int): Boolean {
        val c = dao.get(profileId) ?: return false
        if (c.completedAt != null || c.gameId != gameId || level < c.targetLevel) return false
        dao.upsert(c.copy(completedAt = now()))
        return true
    }
}
