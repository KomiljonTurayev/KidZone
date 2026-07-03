// app/src/test/java/uz/kidzone/app/data/DailyChallengeRepositoryTest.kt
package uz.kidzone.app.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DailyChallengeRepositoryTest {

    private lateinit var challengeDao: FakeDailyChallengeDao
    private lateinit var streakDao: FakeStreakDao
    private lateinit var repo: DailyChallengeRepository
    private val fixedDate = "2026-07-01"
    private val yesterday = "2026-06-30"
    private val olderDate = "2026-06-28"

    @Before
    fun setUp() {
        challengeDao = FakeDailyChallengeDao()
        streakDao = FakeStreakDao()
        repo = DailyChallengeRepository(
            challengeDao = challengeDao,
            streakDao = streakDao,
            firestoreSync = NoOpFirestoreSync(),
            todayProvider = { fixedDate },
        )
    }

    @Test
    fun `getTodayChallenge returns null when no games loaded`() = runTest {
        assertNull(repo.getTodayChallenge("p1"))
    }

    @Test
    fun `getTodayChallenge creates challenge after games loaded`() = runTest {
        repo.updateGamesList("""[{"id":"memory","title":"Memory Match"}]""")
        val result = repo.getTodayChallenge("p1")
        assertNotNull(result)
        assertEquals("memory", result!!.gameId)
        assertEquals("Memory Match", result.gameTitle)
        assertEquals(fixedDate, result.date)
        assertEquals("p1", result.profileId)
        assertFalse(result.completed)
    }

    @Test
    fun `getTodayChallenge returns same challenge on second call`() = runTest {
        repo.updateGamesList("""[{"id":"memory","title":"Memory Match"},{"id":"paint","title":"Paint"}]""")
        val first = repo.getTodayChallenge("p1")
        val second = repo.getTodayChallenge("p1")
        assertEquals(first!!.gameId, second!!.gameId)
    }

    @Test
    fun `markChallengeCompleted sets streak to 1 on first completion`() = runTest {
        repo.updateGamesList("""[{"id":"memory","title":"Memory Match"}]""")
        repo.getTodayChallenge("p1")
        repo.markChallengeCompleted("p1", "memory")
        val streak = repo.getStreak("p1")
        assertEquals(1, streak.count)
        assertEquals(fixedDate, streak.lastCompletedDate)
    }

    @Test
    fun `markChallengeCompleted increments streak when last date is yesterday`() = runTest {
        streakDao.upsert(StreakEntity("p1", count = 4, lastCompletedDate = yesterday))
        repo.updateGamesList("""[{"id":"memory","title":"Memory Match"}]""")
        repo.getTodayChallenge("p1")
        repo.markChallengeCompleted("p1", "memory")
        assertEquals(5, repo.getStreak("p1").count)
    }

    @Test
    fun `markChallengeCompleted keeps streak unchanged when called twice same day`() = runTest {
        streakDao.upsert(StreakEntity("p1", count = 3, lastCompletedDate = fixedDate))
        repo.updateGamesList("""[{"id":"memory","title":"Memory Match"}]""")
        repo.getTodayChallenge("p1")
        repo.markChallengeCompleted("p1", "memory")
        assertEquals(3, repo.getStreak("p1").count)
    }

    @Test
    fun `markChallengeCompleted resets streak when last date is older than yesterday`() = runTest {
        streakDao.upsert(StreakEntity("p1", count = 10, lastCompletedDate = olderDate))
        repo.updateGamesList("""[{"id":"memory","title":"Memory Match"}]""")
        repo.getTodayChallenge("p1")
        repo.markChallengeCompleted("p1", "memory")
        assertEquals(1, repo.getStreak("p1").count)
    }

    @Test
    fun `markChallengeCompleted does nothing when gameId does not match challenge`() = runTest {
        repo.updateGamesList("""[{"id":"memory","title":"Memory Match"}]""")
        repo.getTodayChallenge("p1")
        repo.markChallengeCompleted("p1", "paint")  // wrong game
        assertFalse(challengeDao.getById("p1-$fixedDate")!!.completed)
    }
}

// ---- Fake implementations ----

class FakeDailyChallengeDao : DailyChallengeDao {
    private val store = mutableMapOf<String, DailyChallengeEntity>()

    override suspend fun getById(id: String) = store[id]
    override suspend fun insert(entity: DailyChallengeEntity) { store[entity.id] = entity }
    override suspend fun markCompleted(id: String) {
        store[id] = store[id]?.copy(completed = true) ?: return
    }
    override fun getByProfile(profileId: String): Flow<List<DailyChallengeEntity>> =
        MutableStateFlow(store.values.filter { it.profileId == profileId })
}

class FakeStreakDao : StreakDao {
    private val store = mutableMapOf<String, StreakEntity>()
    private val flow = MutableStateFlow<List<StreakEntity>>(emptyList())

    override suspend fun getByProfile(profileId: String) = store[profileId]
    override suspend fun upsert(entity: StreakEntity) {
        store[entity.profileId] = entity
        flow.value = store.values.toList()
    }
    override fun getAll(): Flow<List<StreakEntity>> = flow
}

class NoOpFirestoreSync : uz.kidzone.app.FirestoreSync(null) {
    override fun syncStreak(uid: String, profileId: String, count: Int, lastDate: String) {}
    override fun syncChallengeCompleted(uid: String, profileId: String, date: String, gameId: String, gameTitle: String) {}
}
