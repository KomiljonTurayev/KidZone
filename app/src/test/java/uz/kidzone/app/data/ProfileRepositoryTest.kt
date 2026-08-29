// app/src/test/java/uz/kidzone/app/data/ProfileRepositoryTest.kt
package uz.kidzone.app.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

    override fun getAll(): Flow<List<ProfileEntity>> = flow
    override suspend fun getById(id: String) = store.firstOrNull { it.id == id }
    override suspend fun count() = store.size
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

    override suspend fun mergeProfiles(profiles: List<ProfileEntity>) {
        insertAll(profiles)
    }
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

class NoOpSyncManager : ProfileSyncManager(null) {
    override fun pushProfile(profile: ProfileEntity) {}
    override fun pushStats(stats: ProfileStatsEntity) {}
    override suspend fun pullProfiles(uid: String) = emptyList<ProfileEntity>()
}
