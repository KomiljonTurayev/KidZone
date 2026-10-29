package uz.kidzone.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParentChallengeRepositoryTest {

    private val dao = FakeParentChallengeDao()
    private val levels = FakeBestLevels()
    private val repo = ParentChallengeRepository(dao, levels) { 1000L }

    @Test
    fun `reaching the target level in that game finishes the challenge once`() = runTest {
        repo.set("kid", "animals", 12)

        assertFalse(repo.onLevelReached("kid", "animals", 11))
        assertFalse(repo.onLevelReached("kid", "fruits", 12))
        assertTrue(repo.onLevelReached("kid", "animals", 12))
        assertFalse(repo.onLevelReached("kid", "animals", 13))
        assertEquals(1000L, repo.observe("kid").first()!!.completedAt)
    }

    @Test
    fun `a target the child already passed counts as done right away`() = runTest {
        levels.best["kid" to "maze"] = 20
        repo.set("kid", "maze", 15)

        assertNotNull(repo.observe("kid").first()!!.completedAt)
        assertEquals(20, repo.bestLevel("kid", "maze"))
        assertEquals(1, repo.bestLevel("kid", "never-played"))
    }

    @Test
    fun `a new challenge replaces the old one and clear removes it`() = runTest {
        repo.set("kid", "animals", 5)
        repo.set("kid", "math", 8)
        assertEquals("math", repo.observe("kid").first()!!.gameId)

        repo.clear("kid")
        assertNull(repo.observe("kid").first())
    }

    @Test
    fun `bad game ids or levels are not stored`() = runTest {
        repo.set("kid", "Bad Id", 5)
        repo.set("kid", "animals", 0)
        assertNull(repo.observe("kid").first())
    }
}

private class FakeParentChallengeDao : ParentChallengeDao {
    private val rows = MutableStateFlow<Map<String, ParentChallengeEntity>>(emptyMap())
    override fun observe(profileId: String): Flow<ParentChallengeEntity?> = rows.map { it[profileId] }
    override suspend fun get(profileId: String) = rows.value[profileId]
    override suspend fun upsert(entity: ParentChallengeEntity) { rows.value = rows.value + (entity.profileId to entity) }
    override suspend fun clear(profileId: String) { rows.value = rows.value - profileId }
}

private class FakeBestLevels : LevelUpDao {
    val best = mutableMapOf<Pair<String, String>, Int>()
    override suspend fun insert(entity: LevelUpEntity): Long = 0
    override suspend fun bestLevel(profileId: String, gameId: String): Int? = best[profileId to gameId]
    override fun progress(profileId: String, since: String): Flow<List<GameProgress>> = MutableStateFlow(emptyList())
}
