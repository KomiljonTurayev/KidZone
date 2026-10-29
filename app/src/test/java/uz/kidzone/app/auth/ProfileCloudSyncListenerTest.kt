package uz.kidzone.app.auth

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Test
import uz.kidzone.app.data.FakeProfileDao
import uz.kidzone.app.data.LocalProfilesOwner
import uz.kidzone.app.data.ProfileEntity
import uz.kidzone.app.data.ProfileOwner
import uz.kidzone.app.data.ProfileStatsEntity
import uz.kidzone.app.data.ProfileSyncManager

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileCloudSyncListenerTest {

    private val dao = FakeProfileDao()
    private val sync = ListeningSyncManager()
    private val localOwner = object : LocalProfilesOwner { override var uid: String? = null }

    private fun TestScope.listener() =
        ProfileCloudSyncListener(dao, sync, this, localOwner, nowMs = { 500L }, newId = { "fresh" })

    /** Signs in and delivers the first `familyOwner` value, as Firestore does right away. */
    private suspend fun TestScope.signIn(
        uid: String,
        familyOwner: String? = null,
        l: ProfileCloudSyncListener = listener(),
    ): ProfileCloudSyncListener {
        l.onAccountChanged(AccountState.SignedIn(uid, "P", AuthProvider.GOOGLE))
        sync.familyOwnerIs(familyOwner)
        advanceUntilIdle()
        return l
    }

    @Test
    fun `signing in starts listening to that account once familyOwner is known`() = runTest {
        listener().onAccountChanged(AccountState.SignedIn("parent", "P", AuthProvider.GOOGLE))
        assertEquals(emptyList<String>(), sync.listening)

        sync.familyOwnerIs(null)
        advanceUntilIdle()

        assertEquals(listOf("parent"), sync.listening)
        assertEquals("parent", localOwner.uid)
    }

    @Test
    fun `a change from another device is merged while the app is open`() = runTest {
        dao.insert(profile("p1", "Before", updatedAt = 100L))
        signIn("parent")

        sync.emit(listOf(profile("p1", "After", updatedAt = 200L)))
        advanceUntilIdle()

        assertEquals("After", dao.getById("p1")!!.name)
    }

    @Test
    fun `switching accounts stops the old listener before starting the new one`() = runTest {
        val l = signIn("first")
        signIn("second", l = l)

        assertEquals(1, sync.closed)
        assertEquals(listOf("first", "second"), sync.listening)
    }

    @Test
    fun `signing out stops listening and a guest never listens`() = runTest {
        val l = signIn("parent")
        l.onAccountChanged(AccountState.Guest("guest"))

        assertEquals(1, sync.closed)
        assertEquals(listOf("parent"), sync.listening)
    }

    @Test
    fun `a guest's profiles are uploaded on the first sign-in, tombstones included`() = runTest {
        dao.insert(profile("alive", "A", updatedAt = 1L))
        dao.insert(profile("gone", "G", updatedAt = 1L).copy(deleted = true))
        signIn("parent")

        assertEquals(setOf("alive", "gone"), sync.pushed.map { it.id }.toSet())
        assertTrue(sync.pushed.first { it.id == "gone" }.deleted)
    }

    @Test
    fun `a family member syncs the owner's profiles`() = runTest {
        signIn("second", familyOwner = "owner")

        assertEquals("owner", ProfileOwner.familyOwnerUid)
        assertEquals(listOf("owner"), sync.listening)
        assertEquals("owner", localOwner.uid)
    }

    @Test
    fun `joining a family replaces this device's profiles instead of adding them to the family`() = runTest {
        localOwner.uid = "second"
        dao.insert(profile("mine", "Asosiy", updatedAt = 1L))
        sync.serverProfiles["owner"] = listOf(profile("theirs", "Ali", updatedAt = 1L))
        signIn("second")
        sync.pushed.clear()

        sync.familyOwnerIs("owner")
        advanceUntilIdle()

        assertEquals(listOf("theirs"), dao.getAllSync().map { it.id })
        assertTrue(sync.pushed.isEmpty())
        assertEquals("owner", localOwner.uid)
    }

    @Test
    fun `when a member opens the app again, the family's profiles are pushed to the owner`() = runTest {
        localOwner.uid = "owner"
        dao.insert(profile("theirs", "Ali", updatedAt = 1L))

        signIn("second", familyOwner = "owner")

        assertEquals(listOf("owner"), sync.pushedTo.distinct())
    }

    @Test
    fun `a member's guest profiles on a new phone don't join the family`() = runTest {
        dao.insert(profile("guest-default", "Asosiy", updatedAt = 1L))
        sync.serverProfiles["owner"] = listOf(profile("theirs", "Ali", updatedAt = 1L))

        signIn("second", familyOwner = "owner")

        assertEquals(listOf("theirs"), dao.getAllSync().map { it.id })
        assertTrue(sync.pushed.isEmpty())
    }

    @Test
    fun `leaving the family goes back to the parent's own profiles`() = runTest {
        sync.serverProfiles["owner"] = listOf(profile("theirs", "Ali", updatedAt = 1L))
        sync.serverProfiles["second"] = listOf(profile("mine", "Asosiy", updatedAt = 1L))
        localOwner.uid = "owner"
        signIn("second", familyOwner = "owner")

        sync.familyOwnerIs(null)
        advanceUntilIdle()

        assertEquals(null, ProfileOwner.familyOwnerUid)
        assertEquals(listOf("owner", "second"), sync.listening)
        assertEquals(listOf("mine"), dao.getAllSync().map { it.id })
    }

    @Test
    fun `offline, a switch keeps this device's profiles and their owner`() = runTest {
        localOwner.uid = "second"
        dao.insert(profile("mine", "Asosiy", updatedAt = 1L))
        sync.serverReachable = false

        signIn("second", familyOwner = "owner")

        assertEquals(listOf("mine"), dao.getAllSync().map { it.id })
        assertEquals("second", localOwner.uid)
        assertEquals(null, ProfileOwner.familyOwnerUid)
    }

    @Test
    fun `an account with no profiles yet starts with a default one`() = runTest {
        localOwner.uid = "someone-else"
        dao.insert(profile("old", "Old", updatedAt = 1L))

        signIn("new-parent")

        val only = dao.getAllSync().single()
        assertEquals("fresh", only.id)
        assertTrue(only.isDefault)
        assertEquals(listOf("fresh"), sync.pushed.map { it.id })
    }

    @Test
    fun `not being in a family keeps the first listener`() = runTest {
        signIn("parent")

        sync.familyOwnerIs(null)
        advanceUntilIdle()

        assertEquals(listOf("parent"), sync.listening)
        assertEquals(0, sync.closed)
    }

    @Test
    fun `signing out forgets the family`() = runTest {
        val l = signIn("second", familyOwner = "owner")

        l.onAccountChanged(AccountState.Guest("guest"))

        assertEquals(null, ProfileOwner.familyOwnerUid)
    }

    @After
    fun resetOwner() {
        ProfileOwner.familyOwnerUid = null
    }

    private fun profile(id: String, name: String, updatedAt: Long) = ProfileEntity(
        id = id, name = name, avatarPath = null, language = "uz", timeLimitMinutes = 0,
        pinHash = null, isDefault = false, createdAt = 1L, updatedAt = updatedAt,
    )

    /** Records listen/close calls and lets the test deliver a snapshot. */
    private class ListeningSyncManager : ProfileSyncManager(null) {
        val listening = mutableListOf<String>()
        val pushed = mutableListOf<ProfileEntity>()
        val serverProfiles = mutableMapOf<String, List<ProfileEntity>>()
        var serverReachable = true
        var closed = 0
        private var onChange: ((List<ProfileEntity>) -> Unit)? = null
        private var onFamilyOwner: ((String?) -> Unit)? = null

        val pushedTo = mutableListOf<String?>()

        override fun pushProfile(profile: ProfileEntity) {
            pushed += profile
            pushedTo += ProfileOwner.uidFor("signed-in-user")
        }
        override fun pushStats(stats: ProfileStatsEntity) {}
        override suspend fun pullProfiles(uid: String): List<ProfileEntity> = serverProfiles[uid].orEmpty()
        override suspend fun pullProfilesFromServer(uid: String): List<ProfileEntity>? =
            if (serverReachable) serverProfiles[uid].orEmpty() else null
        override fun listenProfiles(uid: String, onChange: (List<ProfileEntity>) -> Unit): AutoCloseable {
            listening += uid
            this.onChange = onChange
            return AutoCloseable { closed++ }
        }
        override fun listenFamilyOwner(uid: String, onChange: (String?) -> Unit): AutoCloseable {
            onFamilyOwner = onChange
            return AutoCloseable {}
        }

        fun emit(profiles: List<ProfileEntity>) = onChange!!.invoke(profiles)
        fun familyOwnerIs(owner: String?) = onFamilyOwner!!.invoke(owner)
    }
}
