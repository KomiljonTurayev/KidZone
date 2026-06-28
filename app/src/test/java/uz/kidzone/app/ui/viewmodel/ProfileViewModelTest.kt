// app/src/test/java/uz/kidzone/app/ui/viewmodel/ProfileViewModelTest.kt
package uz.kidzone.app.ui.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import uz.kidzone.app.data.FakeProfileDao
import uz.kidzone.app.data.FakeProfileStatsDao
import uz.kidzone.app.data.FakeSharedPreferences
import uz.kidzone.app.data.NoOpSyncManager
import uz.kidzone.app.data.ProfileEntity
import uz.kidzone.app.data.ProfileRepository

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: ProfileRepository
    private lateinit var vm: ProfileViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repo = ProfileRepository(FakeProfileDao(), FakeProfileStatsDao(), FakeSharedPreferences(), NoOpSyncManager())
        vm = ProfileViewModel(repo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `activeProfile is null before migration`() = runTest {
        advanceUntilIdle()
        assertNull(vm.activeProfile.value)
    }

    @Test
    fun `setActiveProfile updates activeProfile state`() = runTest {
        val p = makeProfile("x1", "Kamol")
        repo.insert(p)
        advanceUntilIdle()
        vm.setActiveProfile(p)
        assertEquals("Kamol", vm.activeProfile.value?.name)
    }

    @Test
    fun `insertProfile appears in profiles list`() = runTest {
        vm.insertProfile(makeProfile("y1", "Nilufar"))
        advanceUntilIdle()
        val list = repo.profiles.first()
        assertEquals(1, list.size)
        assertEquals("Nilufar", list[0].name)
    }

    @Test
    fun `updateProfile changes active profile when it matches`() = runTest {
        val p = makeProfile("z1", "Old Name")
        repo.insert(p)
        vm.setActiveProfile(p)
        advanceUntilIdle()
        vm.updateProfile(p.copy(name = "New Name"))
        advanceUntilIdle()
        assertEquals("New Name", vm.activeProfile.value?.name)
    }

    @Test
    fun `deleteProfile switches active profile to next one`() = runTest {
        val p1 = makeProfile("d1", "First")
        val p2 = makeProfile("d2", "Second")
        repo.insert(p1)
        repo.insert(p2)
        vm.setActiveProfile(p1)
        advanceUntilIdle()
        var switched: ProfileEntity? = null
        vm.deleteProfile(p1) { switched = it }
        advanceUntilIdle()
        assertEquals("Second", switched?.name)
    }

    private fun makeProfile(id: String, name: String) = ProfileEntity(
        id = id, name = name, avatarPath = null, language = "uz",
        timeLimitMinutes = 0, pinHash = null, isDefault = false,
        createdAt = System.currentTimeMillis(),
    )
}
