// app/src/test/java/uz/kidzone/app/ui/viewmodel/DailyChallengeViewModelTest.kt
package uz.kidzone.app.ui.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import uz.kidzone.app.data.DailyChallengeEntity
import uz.kidzone.app.data.DailyChallengeRepository
import uz.kidzone.app.data.GameItem
import uz.kidzone.app.data.StreakEntity

@OptIn(ExperimentalCoroutinesApi::class)
class DailyChallengeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var fakeRepo: FakeDailyChallengeRepository
    private lateinit var vm: DailyChallengeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        fakeRepo = FakeDailyChallengeRepository()
        vm = DailyChallengeViewModel(fakeRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has isLoading true`() {
        assertTrue(vm.state.value.isLoading)
    }

    @Test
    fun `onProfileChanged loads challenge and streak`() = runTest {
        fakeRepo.games = listOf(GameItem("memory", "Memory Match"))
        fakeRepo.streakToReturn = StreakEntity("p1", count = 3, lastCompletedDate = "2026-06-30")
        vm.onProfileChanged("p1")
        advanceUntilIdle()
        val state = vm.state.value
        assertFalse(state.isLoading)
        assertEquals(3, state.streakCount)
        assertNotNull(state.challenge)
    }

    @Test
    fun `onProfileChanged twice with same id does not reload`() = runTest {
        fakeRepo.games = listOf(GameItem("memory", "Memory Match"))
        vm.onProfileChanged("p1")
        advanceUntilIdle()
        val callCount1 = fakeRepo.getChallengeCallCount
        vm.onProfileChanged("p1")
        advanceUntilIdle()
        assertEquals(callCount1, fakeRepo.getChallengeCallCount)
    }

    @Test
    fun `onGameClosed marks completion and refreshes state`() = runTest {
        fakeRepo.games = listOf(GameItem("memory", "Memory Match"))
        vm.onProfileChanged("p1")
        advanceUntilIdle()
        fakeRepo.streakToReturn = StreakEntity("p1", count = 1, lastCompletedDate = "2026-07-01")
        vm.onGameClosed("memory")
        advanceUntilIdle()
        assertEquals(1, vm.state.value.streakCount)
        assertEquals(1, fakeRepo.markCompletedCallCount)
    }

    @Test
    fun `onGameClosed sets celebrateMilestone when repository returns a milestone`() = runTest {
        fakeRepo.games = listOf(GameItem("memory", "Memory Match"))
        vm.onProfileChanged("p1")
        advanceUntilIdle()
        fakeRepo.milestoneToReturn = 7
        vm.onGameClosed("memory")
        advanceUntilIdle()
        assertEquals(7, vm.state.value.celebrateMilestone)
    }

    @Test
    fun `onGameClosed leaves celebrateMilestone null when repository returns null`() = runTest {
        fakeRepo.games = listOf(GameItem("memory", "Memory Match"))
        vm.onProfileChanged("p1")
        advanceUntilIdle()
        fakeRepo.milestoneToReturn = null
        vm.onGameClosed("memory")
        advanceUntilIdle()
        assertNull(vm.state.value.celebrateMilestone)
    }

    @Test
    fun `onCelebrationShown clears celebrateMilestone`() = runTest {
        fakeRepo.games = listOf(GameItem("memory", "Memory Match"))
        vm.onProfileChanged("p1")
        advanceUntilIdle()
        fakeRepo.milestoneToReturn = 3
        vm.onGameClosed("memory")
        advanceUntilIdle()
        vm.onCelebrationShown()
        assertNull(vm.state.value.celebrateMilestone)
    }

    @Test
    fun `onProfileChanged clears any pending celebration from the previous profile`() = runTest {
        fakeRepo.games = listOf(GameItem("memory", "Memory Match"))
        vm.onProfileChanged("p1")
        advanceUntilIdle()
        fakeRepo.milestoneToReturn = 3
        vm.onGameClosed("memory")
        advanceUntilIdle()
        assertEquals(3, vm.state.value.celebrateMilestone)

        vm.onProfileChanged("p2")
        advanceUntilIdle()
        assertNull(vm.state.value.celebrateMilestone)
    }
}

class FakeDailyChallengeRepository : DailyChallengeRepository(
    challengeDao = object : uz.kidzone.app.data.DailyChallengeDao {
        override suspend fun getById(id: String) = null
        override suspend fun insert(entity: DailyChallengeEntity) {}
        override suspend fun markCompleted(id: String) {}
        override fun getByProfile(profileId: String): Flow<List<DailyChallengeEntity>> =
            MutableStateFlow(emptyList())
    },
    streakDao = object : uz.kidzone.app.data.StreakDao {
        override suspend fun getByProfile(profileId: String) = null
        override suspend fun upsert(entity: StreakEntity) {}
        override fun getAll(): Flow<List<StreakEntity>> = MutableStateFlow(emptyList())
    },
    firestoreSync = uz.kidzone.app.FirestoreSync(null),
    todayProvider = { "2026-07-01" },
) {
    var games: List<GameItem> = emptyList()
    var streakToReturn: StreakEntity = StreakEntity("", 0, "")
    var getChallengeCallCount = 0
    var markCompletedCallCount = 0
    var milestoneToReturn: Int? = null

    override fun updateGamesList(json: String) {
        super.updateGamesList("""${games.joinToString(",", "[", "]") { """{"id":"${it.id}","title":"${it.title}"}""" }}""")
    }

    override suspend fun getTodayChallenge(profileId: String): DailyChallengeEntity? {
        getChallengeCallCount++
        if (games.isEmpty()) return null
        return DailyChallengeEntity(
            id = "$profileId-2026-07-01",
            profileId = profileId,
            date = "2026-07-01",
            gameId = games.first().id,
            gameTitle = games.first().title,
        )
    }

    override suspend fun getStreak(profileId: String) = streakToReturn.copy(profileId = profileId)

    override suspend fun markChallengeCompleted(profileId: String, gameId: String): Int? {
        markCompletedCallCount++
        return milestoneToReturn
    }
}
