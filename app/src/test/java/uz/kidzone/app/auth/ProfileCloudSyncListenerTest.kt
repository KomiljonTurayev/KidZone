package uz.kidzone.app.auth

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.kidzone.app.data.FakeProfileDao
import uz.kidzone.app.data.ProfileEntity
import uz.kidzone.app.data.ProfileStatsEntity
import uz.kidzone.app.data.ProfileSyncManager

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileCloudSyncListenerTest {

    private val dao = FakeProfileDao()
    private val sync = ListeningSyncManager()

    private fun TestScope.listener() = ProfileCloudSyncListener(dao, sync, this)

    @Test
    fun `signing in starts listening to that account`() = runTest {
        listener().onAccountChanged(AccountState.SignedIn("parent", "P", AuthProvider.GOOGLE))

        assertEquals(listOf("parent"), sync.listening)
    }

    @Test
    fun `a change from another device is merged while the app is open`() = runTest {
        dao.insert(profile("p1", "Before", updatedAt = 100L))
        listener().onAccountChanged(AccountState.SignedIn("parent", "P", AuthProvider.GOOGLE))

        sync.emit(listOf(profile("p1", "After", updatedAt = 200L)))
        advanceUntilIdle()

        assertEquals("After", dao.getById("p1")!!.name)
    }

    @Test
    fun `switching accounts stops the old listener before starting the new one`() = runTest {
        val l = listener()
        l.onAccountChanged(AccountState.SignedIn("first", "A", AuthProvider.GOOGLE))
        l.onAccountChanged(AccountState.SignedIn("second", "B", AuthProvider.TELEGRAM))

        assertEquals(1, sync.closed)
        assertEquals(listOf("first", "second"), sync.listening)
    }

    @Test
    fun `signing out stops listening and a guest never listens`() = runTest {
        val l = listener()
        l.onAccountChanged(AccountState.SignedIn("parent", "P", AuthProvider.GOOGLE))
        l.onAccountChanged(AccountState.Guest("guest"))

        assertEquals(1, sync.closed)
        assertEquals(listOf("parent"), sync.listening)
    }

    @Test
    fun `sign-in pushes this device's profiles, tombstones included`() = runTest {
        dao.insert(profile("alive", "A", updatedAt = 1L))
        dao.insert(profile("gone", "G", updatedAt = 1L).copy(deleted = true))
        listener().onAccountChanged(AccountState.SignedIn("parent", "P", AuthProvider.GOOGLE))

        assertEquals(setOf("alive", "gone"), sync.pushed.map { it.id }.toSet())
        assertTrue(sync.pushed.first { it.id == "gone" }.deleted)
    }

    private fun profile(id: String, name: String, updatedAt: Long) = ProfileEntity(
        id = id, name = name, avatarPath = null, language = "uz", timeLimitMinutes = 0,
        pinHash = null, isDefault = false, createdAt = 1L, updatedAt = updatedAt,
    )

    /** Records listen/close calls and lets the test deliver a snapshot. */
    private class ListeningSyncManager : ProfileSyncManager(null) {
        val listening = mutableListOf<String>()
        val pushed = mutableListOf<ProfileEntity>()
        var closed = 0
        private var onChange: ((List<ProfileEntity>) -> Unit)? = null

        override fun pushProfile(profile: ProfileEntity) { pushed += profile }
        override fun pushStats(stats: ProfileStatsEntity) {}
        override suspend fun pullProfiles(uid: String) = emptyList<ProfileEntity>()
        override fun listenProfiles(uid: String, onChange: (List<ProfileEntity>) -> Unit): AutoCloseable {
            listening += uid
            this.onChange = onChange
            return AutoCloseable { closed++ }
        }

        fun emit(profiles: List<ProfileEntity>) = onChange!!.invoke(profiles)
    }
}
