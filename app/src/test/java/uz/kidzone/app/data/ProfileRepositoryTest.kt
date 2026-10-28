// app/src/test/java/uz/kidzone/app/data/ProfileRepositoryTest.kt
package uz.kidzone.app.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileRepositoryTest {

    private lateinit var fakeDao: FakeProfileDao
    private lateinit var fakeStatsDao: FakeProfileStatsDao
    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var fakeSync: NoOpSyncManager
    private lateinit var repo: ProfileRepository

    @Before
    fun setUp() {
        fakeDao = FakeProfileDao()
        fakeStatsDao = FakeProfileStatsDao()
        fakePrefs = FakeSharedPreferences()
        fakeSync = NoOpSyncManager()
        repo = ProfileRepository(fakeDao, fakeStatsDao, fakePrefs, fakeSync)
    }

    @Test
    fun `insert then getAll returns profile`() = runTest {
        val profile = makeProfile("p1", "Ali")
        repo.insert(profile)
        val list = repo.profiles.first()
        assertEquals(1, list.size)
        assertEquals("Ali", list[0].name)
    }

    @Test
    fun `getActiveProfile returns null when no active_profile_id set`() = runTest {
        assertNull(repo.getActiveProfile())
    }

    @Test
    fun `setActiveProfileId then getActiveProfile returns correct profile`() = runTest {
        val profile = makeProfile("p2", "Zara")
        repo.insert(profile)
        repo.setActiveProfileId("p2")
        val active = repo.getActiveProfile()
        assertNotNull(active)
        assertEquals("Zara", active!!.name)
    }

    @Test
    fun `count returns correct number`() = runTest {
        assertEquals(0, repo.count())
        repo.insert(makeProfile("a", "A"))
        repo.insert(makeProfile("b", "B"))
        assertEquals(2, repo.count())
    }

    @Test
    fun `delete removes profile`() = runTest {
        val profile = makeProfile("del1", "Delete Me")
        repo.insert(profile)
        repo.delete(profile)
        assertEquals(0, repo.profiles.first().size)
    }

    @Test
    fun `update changes profile fields`() = runTest {
        val profile = makeProfile("u1", "Before")
        repo.insert(profile)
        repo.update(profile.copy(name = "After", language = "en"))
        val updated = fakeDao.getById("u1")
        assertEquals("After", updated?.name)
        assertEquals("en", updated?.language)
    }

    // ---- Faza 16: multi-device sync ----

    private var clock = 1_000L
    private fun stampedRepo(sync: ProfileSyncManager) =
        ProfileRepository(fakeDao, fakeStatsDao, fakePrefs, sync, nowMs = { clock })

    @Test
    fun `insert and update stamp updatedAt and push the stamped copy`() = runTest {
        val sync = RecordingSyncManager()
        val repo = stampedRepo(sync)
        clock = 2_000L
        repo.insert(makeProfile("s1", "Ali"))
        clock = 3_000L
        repo.update(fakeDao.getById("s1")!!.copy(name = "Vali"))

        assertEquals(3_000L, fakeDao.getById("s1")!!.updatedAt)
        assertEquals(listOf(2_000L, 3_000L), sync.pushed.map { it.updatedAt })
    }

    @Test
    fun `delete keeps a pushed tombstone and hides the profile`() = runTest {
        val sync = RecordingSyncManager()
        val repo = stampedRepo(sync)
        repo.insert(makeProfile("t1", "Gone"))
        clock = 5_000L
        repo.delete(fakeDao.getById("t1")!!)

        assertTrue(repo.profiles.first().isEmpty())
        assertEquals(0, repo.count())
        assertNull(fakeDao.getById("t1"))
        val tombstone = fakeDao.getAllSync().single()
        assertTrue(tombstone.deleted)
        assertEquals(5_000L, tombstone.updatedAt)
        assertTrue(sync.pushed.last().deleted)
    }

    @Test
    fun `the active profile is not returned once deleted`() = runTest {
        val repo = stampedRepo(NoOpSyncManager())
        repo.insert(makeProfile("a1", "Active"))
        repo.setActiveProfileId("a1")
        repo.delete(fakeDao.getById("a1")!!)

        assertNull(repo.getActiveProfile())
    }

    @Test
    fun `a newer remote edit replaces the local profile but keeps the local avatar`() = runTest {
        fakeDao.insert(makeProfile("m1", "Old").copy(avatarPath = "/local.jpg", updatedAt = 100L))
        fakeDao.mergeProfiles(listOf(makeProfile("m1", "New").copy(pinHash = "h", timeLimitMinutes = 45, updatedAt = 200L)))

        val merged = fakeDao.getById("m1")!!
        assertEquals("New", merged.name)
        assertEquals("h", merged.pinHash)
        assertEquals(45, merged.timeLimitMinutes)
        assertEquals("/local.jpg", merged.avatarPath)
    }

    @Test
    fun `an older remote copy does not overwrite a newer local edit`() = runTest {
        fakeDao.insert(makeProfile("m2", "Local").copy(updatedAt = 300L))
        fakeDao.mergeProfiles(listOf(makeProfile("m2", "Stale").copy(updatedAt = 200L)))

        assertEquals("Local", fakeDao.getById("m2")!!.name)
    }

    @Test
    fun `an edit to an existing profile reaches this device even though createdAt is the same`() = runTest {
        // The bug Faza 16 fixes: the old merge compared createdAt, which edits never change.
        val original = makeProfile("m3", "Before").copy(createdAt = 10L, updatedAt = 10L)
        fakeDao.insert(original)
        fakeDao.mergeProfiles(listOf(original.copy(name = "After", updatedAt = 20L)))

        assertEquals("After", fakeDao.getById("m3")!!.name)
    }

    @Test
    fun `a remote tombstone deletes the profile here`() = runTest {
        fakeDao.insert(makeProfile("m4", "Here").copy(updatedAt = 100L))
        fakeDao.mergeProfiles(listOf(makeProfile("m4", "Here").copy(deleted = true, updatedAt = 200L)))

        assertNull(fakeDao.getById("m4"))
        assertTrue(fakeDao.getAllSync().single().deleted)
    }

    @Test
    fun `an unknown remote profile is added`() = runTest {
        fakeDao.mergeProfiles(listOf(makeProfile("m5", "From cloud").copy(updatedAt = 100L)))

        assertFalse(fakeDao.getById("m5")!!.deleted)
    }

    private fun makeProfile(id: String, name: String) = ProfileEntity(
        id = id, name = name, avatarPath = null, language = "uz",
        timeLimitMinutes = 30, pinHash = null, isDefault = false,
        createdAt = System.currentTimeMillis(),
    )
}

// ---- Fake implementations ----

class FakeProfileDao : ProfileDao {
    private val store = mutableListOf<ProfileEntity>()
    private val flow = MutableStateFlow<List<ProfileEntity>>(emptyList())

    override fun getAll(): Flow<List<ProfileEntity>> = flow.map { list -> list.filterNot { it.deleted } }
    override suspend fun getById(id: String) = store.firstOrNull { it.id == id && !it.deleted }
    override suspend fun count() = store.count { !it.deleted }
    override suspend fun insert(profile: ProfileEntity) {
        store.removeAll { it.id == profile.id }
        store.add(profile)
        flow.value = store.toList()
    }
    override suspend fun update(profile: ProfileEntity) {
        store.replaceAll { if (it.id == profile.id) profile else it }
        flow.value = store.toList()
    }
    override suspend fun delete(profile: ProfileEntity) {
        store.removeAll { it.id == profile.id }
        flow.value = store.toList()
    }
    
    override suspend fun insertAll(profiles: List<ProfileEntity>) {
        profiles.forEach { p ->
            store.removeAll { it.id == p.id }
            store.add(p)
        }
        flow.value = store.toList()
    }

    override suspend fun getAllSync(): List<ProfileEntity> = store.toList()
    // mergeProfiles: the interface's own implementation, so the real merge rule is tested.
}

class FakeProfileStatsDao : ProfileStatsDao {
    private val store = mutableMapOf<String, ProfileStatsEntity>()
    override suspend fun getByProfileAndDate(profileId: String, date: String) =
        store["$profileId|$date"]
    override suspend fun upsert(stats: ProfileStatsEntity) {
        store["${stats.profileId}|${stats.date}"] = stats
    }
}

class FakeSharedPreferences : android.content.SharedPreferences {
    private val map = mutableMapOf<String, Any?>()
    override fun getString(key: String, defValue: String?) = map[key] as? String ?: defValue
    override fun edit() = object : android.content.SharedPreferences.Editor {
        override fun putString(key: String, value: String?): android.content.SharedPreferences.Editor {
            map[key] = value; return this
        }
        override fun apply() {}
        override fun commit() = true
        override fun putStringSet(k: String, v: MutableSet<String>?) = this
        override fun putInt(k: String, v: Int) = this
        override fun putLong(k: String, v: Long) = this
        override fun putFloat(k: String, v: Float) = this
        override fun putBoolean(k: String, v: Boolean) = this
        override fun remove(k: String) = this
        override fun clear() = this
    }
    override fun getAll() = map
    override fun getStringSet(k: String, d: MutableSet<String>?) = d
    override fun getInt(k: String, d: Int) = d
    override fun getLong(k: String, d: Long) = d
    override fun getFloat(k: String, d: Float) = d
    override fun getBoolean(k: String, d: Boolean) = d
    override fun contains(k: String) = map.containsKey(k)
    override fun registerOnSharedPreferenceChangeListener(l: android.content.SharedPreferences.OnSharedPreferenceChangeListener) {}
    override fun unregisterOnSharedPreferenceChangeListener(l: android.content.SharedPreferences.OnSharedPreferenceChangeListener) {}
}

/** Records what would have been pushed to Firestore. */
class RecordingSyncManager : ProfileSyncManager(null) {
    val pushed = mutableListOf<ProfileEntity>()
    override fun pushProfile(profile: ProfileEntity) { pushed += profile }
    override fun pushStats(stats: ProfileStatsEntity) {}
    override suspend fun pullProfiles(uid: String) = emptyList<ProfileEntity>()
}

class NoOpSyncManager : ProfileSyncManager(null) {
    override fun pushProfile(profile: ProfileEntity) {}
    override fun pushStats(stats: ProfileStatsEntity) {}
    override suspend fun pullProfiles(uid: String) = emptyList<ProfileEntity>()
}
