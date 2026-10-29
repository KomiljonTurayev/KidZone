package uz.kidzone.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameProgressRepositoryTest {

    private val dao = FakeLevelUpDao()
    private val sync = RecordingProgressSync()
    private var today = "2026-10-29"
    private var uid: String? = "parent"
    private val repo = GameProgressRepository(dao, sync, { today }, { uid })

    @After
    fun tearDown() {
        ProfileOwner.familyOwnerUid = null
    }

    @Test
    fun `a new level is stored and its best level goes to the cloud`() = runTest {
        repo.onLevelReached("kid", "animals", 21)

        assertEquals(listOf(LevelUpEntity("kid", "animals", 21, today)), dao.rows.value)
        assertEquals(listOf(Sync("parent", "kid", "animals", 21)), sync.calls)
    }

    @Test
    fun `a level reported twice is stored and sent once`() = runTest {
        repo.onLevelReached("kid", "animals", 5)
        repo.onLevelReached("kid", "animals", 5)

        assertEquals(1, dao.rows.value.size)
        assertEquals(1, sync.calls.size)
    }

    @Test
    fun `the cloud always gets the best level, even for a late lower report`() = runTest {
        repo.onLevelReached("kid", "maze", 30)
        repo.onLevelReached("kid", "maze", 12)

        assertEquals(30, sync.calls.last().level)
    }

    @Test
    fun `a family member's device writes under the family owner`() = runTest {
        ProfileOwner.familyOwnerUid = "owner"
        repo.onLevelReached("kid", "abc-uz", 3)

        assertEquals("owner", sync.calls.single().uid)
    }

    @Test
    fun `a guest keeps levels on the phone only`() = runTest {
        uid = null
        repo.onLevelReached("kid", "abc-uz", 3)

        assertEquals(1, dao.rows.value.size)
        assertTrue(sync.calls.isEmpty())
    }

    @Test
    fun `bad game ids and levels from the WebView are ignored`() = runTest {
        repo.onLevelReached("kid", "../users", 3)
        repo.onLevelReached("kid", "animals", 0)
        repo.onLevelReached("kid", "animals", GameProgressRepository.MAX_LEVEL + 1)
        repo.onLevelReached("kid", "", 3)

        assertTrue(dao.rows.value.isEmpty())
        assertFalse(GameProgressRepository.isValid("Animals", 3))
    }

    @Test
    fun `progress counts only the last seven days as this week`() = runTest {
        today = "2026-10-20"
        repo.onLevelReached("kid", "animals", 1)
        repo.onLevelReached("kid", "animals", 2)
        today = "2026-10-29"
        repo.onLevelReached("kid", "animals", 3)
        repo.onLevelReached("kid", "math", 7)
        repo.onLevelReached("other-kid", "math", 40)

        val got = repo.progress("kid").first()

        assertEquals(listOf(GameProgress("math", 7, 1), GameProgress("animals", 3, 1)), got)
    }
}

private data class Sync(val uid: String, val profileId: String, val gameId: String, val level: Int)

private class RecordingProgressSync : uz.kidzone.app.FirestoreSync(null) {
    val calls = mutableListOf<Sync>()
    override fun syncGameProgress(uid: String, profileId: String, gameId: String, level: Int) {
        calls += Sync(uid, profileId, gameId, level)
    }
}

/** Mirrors LevelUpDao's SQL over a list. */
private class FakeLevelUpDao : LevelUpDao {
    val rows = MutableStateFlow<List<LevelUpEntity>>(emptyList())

    override suspend fun insert(entity: LevelUpEntity): Long {
        if (rows.value.any { it.profileId == entity.profileId && it.gameId == entity.gameId && it.level == entity.level }) return -1
        rows.value = rows.value + entity
        return rows.value.size.toLong()
    }

    override suspend fun bestLevel(profileId: String, gameId: String): Int? =
        rows.value.filter { it.profileId == profileId && it.gameId == gameId }.maxOfOrNull { it.level }

    override fun progress(profileId: String, since: String): Flow<List<GameProgress>> = rows.map { all ->
        all.filter { it.profileId == profileId }
            .groupBy { it.gameId }
            .map { (game, ups) -> GameProgress(game, ups.maxOf { it.level }, ups.count { it.date >= since }) }
            .sortedByDescending { it.level }
    }
}
