# Faza 14 — Daily Challenge + Streak Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Har kungi tasodifiy o'yin vazifasi (Daily Challenge) va ketma-ket kunlar hisoblagichi (Streak) qo'shish — per-profile, Room DB local + Firestore sync.

**Architecture:** Yangi `DailyChallengeRepository` Room DB (v1→v2) ustida streak hisoblaydi; `DailyChallengeViewModel` MainScreen va ParentDashboard uchun state beradi; JS side `openGame()`/`closeGame()` hooks orqali Kotlin ga xabar beradi; `FirestoreSync` streak va challenge completion ni cloud ga yuboradi.

**Tech Stack:** Kotlin, Jetpack Compose, Room 2.6.1, Firestore, `org.json` (JSON parsing), `java.time.LocalDate` (streak date logic), `kotlinx-coroutines-test` (tests).

## Global Constraints

- Room DB: `version = 1 → 2`, `MIGRATION_1_2` (empty SQL migration, yangi jadvallar)
- `main.js`: faqat `GameManager` constructor + `openGame()` + `closeGame()` o'zgaradi
- Streak hisobi: qurilma local vaqti (`java.time.LocalDate.now()`) — server vaqt yo'q
- Firestore xatolikda: silent fail — local Room DB asosiy manba
- `DailyChallengeRepository.getTodayChallenge()`: `gamesList` bo'sh bo'lsa `null` qaytaradi
- `DailyChallengeRepository` constructor-da `todayProvider: () -> String` — test uchun injectable
- Version bump: `versionCode 14`, `versionName "1.4.0"` (faqat oxirgi taskda)

---

## File Map

| Harakat | Fayl | Nima uchun |
|---------|------|-----------|
| Create | `app/.../data/DailyChallengeEntity.kt` | Room entity |
| Create | `app/.../data/StreakEntity.kt` | Room entity |
| Create | `app/.../data/DailyChallengeDao.kt` | Room DAO |
| Create | `app/.../data/StreakDao.kt` | Room DAO |
| Modify | `app/.../data/KidZoneDatabase.kt` | v1→v2, entities + DAOs + migration |
| Create | `app/.../data/DailyChallengeRepository.kt` | biznes logika |
| Create | `app/.../ui/viewmodel/DailyChallengeViewModel.kt` | UI state |
| Create | `app/.../ui/screens/DailyChallengeCard.kt` | Compose composable |
| Modify | `app/src/main/assets/www/main.js` | JS hooks |
| Modify | `app/.../KidWebViewManager.kt` | `addInterface()` metodi |
| Modify | `app/.../ui/MainScreen.kt` | ChallengeBridge + DailyChallengeCard |
| Modify | `app/.../ui/KidZoneApp.kt` | challengeViewModel yaratish va uzatish |
| Modify | `app/.../FirestoreSync.kt` | streak + challenge sync metodlari |
| Modify | `app/.../ui/screens/ParentDashboardScreen.kt` | streak satri |
| Create | `app/src/test/.../data/DailyChallengeRepositoryTest.kt` | streak logika testlari |
| Create | `app/src/test/.../ui/viewmodel/DailyChallengeViewModelTest.kt` | state testlari |

---

### Task 1: Room DB — Entity, DAO, Migration

**Files:**
- Create: `app/src/main/java/uz/kidzone/app/data/DailyChallengeEntity.kt`
- Create: `app/src/main/java/uz/kidzone/app/data/StreakEntity.kt`
- Create: `app/src/main/java/uz/kidzone/app/data/DailyChallengeDao.kt`
- Create: `app/src/main/java/uz/kidzone/app/data/StreakDao.kt`
- Modify: `app/src/main/java/uz/kidzone/app/data/KidZoneDatabase.kt`

**Interfaces:**
- Produces: `DailyChallengeDao`, `StreakDao` — keyingi tasklar shu interfeyslarga tayanadi

- [ ] **Step 1: DailyChallengeEntity.kt yaratish**

```kotlin
// app/src/main/java/uz/kidzone/app/data/DailyChallengeEntity.kt
package uz.kidzone.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_challenge")
data class DailyChallengeEntity(
    @PrimaryKey val id: String,         // "$profileId-$date", e.g. "p1-2026-07-01"
    val profileId: String,
    val date: String,                   // "YYYY-MM-DD"
    val gameId: String,
    val gameTitle: String,
    val completed: Boolean = false,
)
```

- [ ] **Step 2: StreakEntity.kt yaratish**

```kotlin
// app/src/main/java/uz/kidzone/app/data/StreakEntity.kt
package uz.kidzone.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streak")
data class StreakEntity(
    @PrimaryKey val profileId: String,
    val count: Int = 0,
    val lastCompletedDate: String = "",  // "YYYY-MM-DD"
)
```

- [ ] **Step 3: DailyChallengeDao.kt yaratish**

```kotlin
// app/src/main/java/uz/kidzone/app/data/DailyChallengeDao.kt
package uz.kidzone.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyChallengeDao {
    @Query("SELECT * FROM daily_challenge WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DailyChallengeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DailyChallengeEntity)

    @Query("UPDATE daily_challenge SET completed = 1 WHERE id = :id")
    suspend fun markCompleted(id: String)

    @Query("SELECT * FROM daily_challenge WHERE profileId = :profileId ORDER BY date DESC")
    fun getByProfile(profileId: String): Flow<List<DailyChallengeEntity>>
}
```

- [ ] **Step 4: StreakDao.kt yaratish**

```kotlin
// app/src/main/java/uz/kidzone/app/data/StreakDao.kt
package uz.kidzone.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StreakDao {
    @Query("SELECT * FROM streak WHERE profileId = :profileId LIMIT 1")
    suspend fun getByProfile(profileId: String): StreakEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: StreakEntity)

    @Query("SELECT * FROM streak")
    fun getAll(): Flow<List<StreakEntity>>
}
```

- [ ] **Step 5: KidZoneDatabase.kt yangilash**

Mavjud fayl (`version = 1`, 2 ta entity):

```kotlin
// app/src/main/java/uz/kidzone/app/data/KidZoneDatabase.kt
package uz.kidzone.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProfileEntity::class,
        ProfileStatsEntity::class,
        DailyChallengeEntity::class,
        StreakEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class KidZoneDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun profileStatsDao(): ProfileStatsDao
    abstract fun dailyChallengeDao(): DailyChallengeDao
    abstract fun streakDao(): StreakDao

    companion object {
        @Volatile private var instance: KidZoneDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `daily_challenge` " +
                    "(`id` TEXT NOT NULL, `profileId` TEXT NOT NULL, `date` TEXT NOT NULL, " +
                    "`gameId` TEXT NOT NULL, `gameTitle` TEXT NOT NULL, " +
                    "`completed` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `streak` " +
                    "(`profileId` TEXT NOT NULL, `count` INTEGER NOT NULL DEFAULT 0, " +
                    "`lastCompletedDate` TEXT NOT NULL DEFAULT '', PRIMARY KEY(`profileId`))"
                )
            }
        }

        fun getInstance(context: Context): KidZoneDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KidZoneDatabase::class.java,
                    "kidzone.db",
                )
                .addMigrations(MIGRATION_1_2)
                .build().also { instance = it }
            }
    }
}
```

- [ ] **Step 6: Build compile bo'lishini tekshirish**

```bash
./gradlew assembleDebug --stacktrace 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/data/DailyChallengeEntity.kt \
        app/src/main/java/uz/kidzone/app/data/StreakEntity.kt \
        app/src/main/java/uz/kidzone/app/data/DailyChallengeDao.kt \
        app/src/main/java/uz/kidzone/app/data/StreakDao.kt \
        app/src/main/java/uz/kidzone/app/data/KidZoneDatabase.kt
git commit -m "feat(faza14): Room DB v2 — DailyChallengeEntity, StreakEntity, DAOs, migration"
```

---

### Task 2: DailyChallengeRepository (TDD)

**Files:**
- Create: `app/src/main/java/uz/kidzone/app/data/DailyChallengeRepository.kt`
- Create: `app/src/test/java/uz/kidzone/app/data/DailyChallengeRepositoryTest.kt`

**Interfaces:**
- Consumes: `DailyChallengeDao`, `StreakDao`, `FirestoreSync` (Task 1 dan)
- Produces:
  - `fun updateGamesList(json: String)`
  - `suspend fun getTodayChallenge(profileId: String): DailyChallengeEntity?`
  - `suspend fun markChallengeCompleted(profileId: String, gameId: String)`
  - `suspend fun getStreak(profileId: String): StreakEntity`
  - `fun getAllStreaks(): Flow<List<StreakEntity>>`
  - `fun getChallengesFlow(profileId: String): Flow<List<DailyChallengeEntity>>`

- [ ] **Step 1: Failing testlarni yozish**

```kotlin
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
```

- [ ] **Step 2: Testlarni ishlatib, barchasi FAIL ekanligini tekshirish**

```bash
./gradlew test --tests "uz.kidzone.app.data.DailyChallengeRepositoryTest" 2>&1 | tail -20
```

Expected: `FAILED` — `DailyChallengeRepository` hali mavjud emas.

- [ ] **Step 3: DailyChallengeRepository.kt yaratish**

```kotlin
// app/src/main/java/uz/kidzone/app/data/DailyChallengeRepository.kt
package uz.kidzone.app.data

import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import uz.kidzone.app.FirestoreSync
import com.google.firebase.auth.FirebaseAuth

data class GameItem(val id: String, val title: String)

class DailyChallengeRepository(
    private val challengeDao: DailyChallengeDao,
    private val streakDao: StreakDao,
    private val firestoreSync: FirestoreSync,
    private val todayProvider: () -> String = {
        java.time.LocalDate.now().toString()
    },
) {
    private var gamesList: List<GameItem> = emptyList()

    fun updateGamesList(json: String) {
        try {
            val arr = JSONArray(json)
            gamesList = (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                GameItem(obj.getString("id"), obj.getString("title"))
            }
        } catch (_: Exception) {}
    }

    suspend fun getTodayChallenge(profileId: String): DailyChallengeEntity? {
        val today = todayProvider()
        val id = "$profileId-$today"
        challengeDao.getById(id)?.let { return it }

        val games = gamesList.ifEmpty { return null }
        val game = games.random()
        val entity = DailyChallengeEntity(
            id = id,
            profileId = profileId,
            date = today,
            gameId = game.id,
            gameTitle = game.title,
        )
        challengeDao.insert(entity)
        return entity
    }

    suspend fun markChallengeCompleted(profileId: String, gameId: String) {
        val today = todayProvider()
        val id = "$profileId-$today"
        val challenge = challengeDao.getById(id) ?: return
        if (challenge.gameId != gameId || challenge.completed) return

        challengeDao.markCompleted(id)
        updateStreak(profileId, today, challenge.gameTitle)
    }

    private suspend fun updateStreak(profileId: String, today: String, gameTitle: String) {
        val current = streakDao.getByProfile(profileId) ?: StreakEntity(profileId)
        val yesterday = java.time.LocalDate.parse(today).minusDays(1).toString()
        val newCount = when (current.lastCompletedDate) {
            today -> current.count
            yesterday -> current.count + 1
            else -> 1
        }
        streakDao.upsert(StreakEntity(profileId, newCount, today))

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        firestoreSync.syncStreak(uid, profileId, newCount, today)
        firestoreSync.syncChallengeCompleted(uid, profileId, today, current.profileId, gameTitle)
    }

    suspend fun getStreak(profileId: String): StreakEntity =
        streakDao.getByProfile(profileId) ?: StreakEntity(profileId)

    fun getAllStreaks(): Flow<List<StreakEntity>> = streakDao.getAll()

    fun getChallengesFlow(profileId: String): Flow<List<DailyChallengeEntity>> =
        challengeDao.getByProfile(profileId)
}
```

- [ ] **Step 4: `FirestoreSync.kt` ga stub metodlar qo'shish** (Task 6 da to'liq implementatsiya)

`FirestoreSync.kt` oxiriga (class ichida, `recordSession` dan keyin) qo'shish:

```kotlin
open fun syncStreak(uid: String, profileId: String, count: Int, lastDate: String) {
    // Task 6 da implementatsiya qilinadi
}

open fun syncChallengeCompleted(uid: String, profileId: String, date: String, gameId: String, gameTitle: String) {
    // Task 6 da implementatsiya qilinadi
}
```

- [ ] **Step 5: Testlarni ishlatib, barchasi PASS ekanligini tekshirish**

```bash
./gradlew test --tests "uz.kidzone.app.data.DailyChallengeRepositoryTest" 2>&1 | tail -20
```

Expected: `8 tests completed, 0 failed`

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/data/DailyChallengeRepository.kt \
        app/src/main/java/uz/kidzone/app/FirestoreSync.kt \
        app/src/test/java/uz/kidzone/app/data/DailyChallengeRepositoryTest.kt
git commit -m "feat(faza14): DailyChallengeRepository — streak logika TDD bilan"
```

---

### Task 3: DailyChallengeViewModel (TDD)

**Files:**
- Create: `app/src/main/java/uz/kidzone/app/ui/viewmodel/DailyChallengeViewModel.kt`
- Create: `app/src/test/java/uz/kidzone/app/ui/viewmodel/DailyChallengeViewModelTest.kt`

**Interfaces:**
- Consumes: `DailyChallengeRepository` (Task 2 dan)
- Produces:
  - `data class ChallengeState(challenge, streakCount, isLoading)`
  - `val state: StateFlow<ChallengeState>`
  - `val allStreaks: StateFlow<List<StreakEntity>>`
  - `fun onProfileChanged(profileId: String)`
  - `fun updateGamesList(json: String)`
  - `fun onGameClosed(gameId: String)`

- [ ] **Step 1: Failing testlarni yozish**

```kotlin
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

    override suspend fun markChallengeCompleted(profileId: String, gameId: String) {
        markCompletedCallCount++
    }
}
```

- [ ] **Step 2: Testlarni ishlatib, FAIL ekanligini tekshirish**

```bash
./gradlew test --tests "uz.kidzone.app.ui.viewmodel.DailyChallengeViewModelTest" 2>&1 | tail -20
```

Expected: `FAILED` — `DailyChallengeViewModel` hali yo'q

- [ ] **Step 3: DailyChallengeViewModel.kt yaratish**

```kotlin
// app/src/main/java/uz/kidzone/app/ui/viewmodel/DailyChallengeViewModel.kt
package uz.kidzone.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.kidzone.app.data.DailyChallengeEntity
import uz.kidzone.app.data.DailyChallengeRepository
import uz.kidzone.app.data.StreakEntity

data class ChallengeState(
    val challenge: DailyChallengeEntity? = null,
    val streakCount: Int = 0,
    val isLoading: Boolean = true,
)

open class DailyChallengeViewModel(
    private val repository: DailyChallengeRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ChallengeState())
    val state: StateFlow<ChallengeState> = _state.asStateFlow()

    val allStreaks: StateFlow<List<StreakEntity>> = repository.getAllStreaks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var activeProfileId: String? = null

    open fun onProfileChanged(profileId: String) {
        if (activeProfileId == profileId) return
        activeProfileId = profileId
        loadChallenge(profileId)
    }

    private fun loadChallenge(profileId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val challenge = repository.getTodayChallenge(profileId)
            val streak = repository.getStreak(profileId)
            _state.update {
                ChallengeState(
                    challenge = challenge,
                    streakCount = streak.count,
                    isLoading = false,
                )
            }
        }
    }

    fun updateGamesList(json: String) {
        repository.updateGamesList(json)
        activeProfileId?.let { loadChallenge(it) }
    }

    fun onGameClosed(gameId: String) {
        val profileId = activeProfileId ?: return
        viewModelScope.launch {
            repository.markChallengeCompleted(profileId, gameId)
            val challenge = repository.getTodayChallenge(profileId)
            val streak = repository.getStreak(profileId)
            _state.update { it.copy(challenge = challenge, streakCount = streak.count) }
        }
    }
}

class DailyChallengeViewModelFactory(
    private val repository: DailyChallengeRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        DailyChallengeViewModel(repository) as T
}
```

- [ ] **Step 4: Testlarni ishlatib, PASS ekanligini tekshirish**

```bash
./gradlew test --tests "uz.kidzone.app.ui.viewmodel.DailyChallengeViewModelTest" 2>&1 | tail -20
```

Expected: `5 tests completed, 0 failed`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/ui/viewmodel/DailyChallengeViewModel.kt \
        app/src/test/java/uz/kidzone/app/ui/viewmodel/DailyChallengeViewModelTest.kt
git commit -m "feat(faza14): DailyChallengeViewModel — TDD bilan state boshqaruvi"
```

---

### Task 4: DailyChallengeCard Composable

**Files:**
- Create: `app/src/main/java/uz/kidzone/app/ui/screens/DailyChallengeCard.kt`

**Interfaces:**
- Consumes: `ChallengeState` (Task 3 dan)
- Produces: `@Composable fun DailyChallengeCard(streakCount, challenge, visible, onPlay)`

- [ ] **Step 1: DailyChallengeCard.kt yaratish**

```kotlin
// app/src/main/java/uz/kidzone/app/ui/screens/DailyChallengeCard.kt
package uz.kidzone.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uz.kidzone.app.data.DailyChallengeEntity

@Composable
fun DailyChallengeCard(
    streakCount: Int,
    challenge: DailyChallengeEntity?,
    visible: Boolean,
    onPlay: (gameId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = visible && challenge != null) {
        val c = challenge ?: return@AnimatedVisibility
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            AnimatedContent(
                targetState = c.completed,
                transitionSpec = {
                    (fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.92f))
                        .togetherWith(fadeOut(tween(200)))
                },
                label = "challenge_card",
            ) { completed ->
                if (completed) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "🔥 $streakCount kun streak",
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "✅ Bajarildi!",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            "Ertaga yangi vazifa kutilmoqda 🌟",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "🔥 $streakCount kun streak",
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Text(
                                "Bugungi vazifa: ${c.gameTitle}",
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { onPlay(c.gameId) }) {
                            Text("O'ynash →")
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Build compile bo'lishini tekshirish**

```bash
./gradlew assembleDebug --stacktrace 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/ui/screens/DailyChallengeCard.kt
git commit -m "feat(faza14): DailyChallengeCard composable — faol + bajarilgan holat animatsiyasi"
```

---

### Task 5: main.js — JS interface hooks

**Files:**
- Modify: `app/src/main/assets/www/main.js`

**Qo'shiladigan joylar:** GameManager constructor (~line 446), `openGame()` (line 562), `closeGame()` (line 592).

- [ ] **Step 1: GameManager constructor ga `onGamesLoaded` qo'shish**

`main.js` da `constructor(games, ui, translator)` ichiga, `this.pinEntry = "";` dan keyin:

```javascript
        if (window.AndroidChallenge) {
            const list = JSON.stringify(this.games.map(function(g) {
                return {
                    id: g.id,
                    title: typeof g.name === 'object' ? (g.name.uz || g.name.en) : g.name
                };
            }));
            window.AndroidChallenge.onGamesLoaded(list);
        }
```

- [ ] **Step 2: `openGame()` ga `onGameOpened` qo'shish**

`openGame()` ichida, `if (window.AndroidAdMob) { ... }` blokidan keyin:

```javascript
        if (window.AndroidChallenge) {
            window.AndroidChallenge.onGameOpened(g.id);
        }
```

- [ ] **Step 3: `closeGame()` ga `onGameClosed` qo'shish**

`closeGame()` ichida, `if (game) { ... }` blokidan keyin (yoki blok ichida `this.showRewardScreen(earned)` dan oldin emas, blokdan tashqarida):

```javascript
        if (game && window.AndroidChallenge) {
            window.AndroidChallenge.onGameClosed(game.id);
        }
```

- [ ] **Step 4: O'zgarishlarni tekshirish**

```bash
grep -n "AndroidChallenge" app/src/main/assets/www/main.js
```

Expected: 3 ta satr — constructor, openGame, closeGame da.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/assets/www/main.js
git commit -m "feat(faza14): main.js — AndroidChallenge JS interface hooks (onGamesLoaded, onGameOpened, onGameClosed)"
```

---

### Task 6: KidWebViewManager + ChallengeBridge + MainScreen + KidZoneApp

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/KidWebViewManager.kt`
- Modify: `app/src/main/java/uz/kidzone/app/ui/MainScreen.kt`
- Modify: `app/src/main/java/uz/kidzone/app/ui/KidZoneApp.kt`

**Interfaces:**
- Consumes: `DailyChallengeViewModel` (Task 3), `DailyChallengeCard` (Task 4), `ChallengeState`
- Produces: `ChallengeBridge` private class in MainScreen.kt

- [ ] **Step 1: KidWebViewManager.kt ga `addInterface()` metodi qo'shish**

`KidWebViewManager.kt` da `fun loadUrl(url: String)` dan oldin:

```kotlin
    fun addInterface(obj: Any, name: String) {
        webView.addJavascriptInterface(obj, name)
    }
```

- [ ] **Step 2: MainScreen.kt — challengeViewModel parameter qo'shish va DailyChallengeCard joylashtirish**

`MainScreen` funksiya signature ga `challengeViewModel: DailyChallengeViewModel` qo'shish:

```kotlin
@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    adsManager: AdsManager,
    prefs: SharedPreferences,
    statsManager: ParentalStatsManager,
    profileViewModel: ProfileViewModel,
    challengeViewModel: DailyChallengeViewModel,
    onOpenDashboard: () -> Unit,
) {
```

`activeProfile` collect qiluvchi satrdan keyin `challengeViewModel` ga profil o'zgarishini xabar berish:

```kotlin
    val challengeState by challengeViewModel.state.collectAsState()

    // Profil o'zgarganda challengeViewModel ga xabar ber
    LaunchedEffect(activeProfile) {
        activeProfile?.id?.let { challengeViewModel.onProfileChanged(it) }
    }
```

`Column` ichida, `Box` dan OLDIN `DailyChallengeCard` qo'shish:

```kotlin
    Column(modifier = Modifier.fillMaxSize()) {
        // Daily Challenge Card — o'yin ko'rinisida emas, lock holatida emas
        DailyChallengeCard(
            streakCount = challengeState.streakCount,
            challenge = challengeState.challenge,
            visible = !uiState.inGame && !uiState.isLocked,
            onPlay = { gameId ->
                webMgrRef.value?.evaluateJavascript(
                    "if(window.app){app.openGame(app.games.find(function(g){return g.id==='$gameId';}))||null}"
                )
            },
        )
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            // ... mavjud kod o'zgarmaydi
```

WebView factory blokida, `mgr.setup(...)` dan keyin `mgr.loadUrl(...)` dan OLDIN ChallengeBridge qo'shish:

```kotlin
                    mgr.setup(
                        AdMobBridge(mainViewModel, adsManager, onOpenDashboard, context as Activity),
                        "AndroidAdMob",
                    )
                    mgr.addInterface(
                        ChallengeBridge(challengeViewModel, context as Activity),
                        "AndroidChallenge",
                    )
```

Import qo'shish:
```kotlin
import uz.kidzone.app.ui.screens.DailyChallengeCard
import uz.kidzone.app.ui.viewmodel.DailyChallengeViewModel
import uz.kidzone.app.ui.viewmodel.ChallengeState
```

- [ ] **Step 3: ChallengeBridge private class qo'shish** (MainScreen.kt oxirida, `AdMobBridge` dan keyin)

```kotlin
private class ChallengeBridge(
    private val viewModel: DailyChallengeViewModel,
    activity: Activity,
) {
    private val activity = WeakReference(activity)

    private fun onMain(block: () -> Unit) {
        activity.get()?.runOnUiThread(block)
    }

    @android.webkit.JavascriptInterface
    fun onGamesLoaded(json: String) {
        onMain { viewModel.updateGamesList(json) }
    }

    @android.webkit.JavascriptInterface
    fun onGameOpened(gameId: String) {
        // future use
    }

    @android.webkit.JavascriptInterface
    fun onGameClosed(gameId: String) {
        onMain { viewModel.onGameClosed(gameId) }
    }
}
```

- [ ] **Step 4: KidZoneApp.kt — challengeViewModel yaratish va uzatish**

`KidZoneApp.kt` da `profileRepository` dan keyin:

```kotlin
    val challengeRepository = remember {
        val db = KidZoneDatabase.getInstance(context)
        val firestoreSync = uz.kidzone.app.FirestoreSync.getInstance()
        uz.kidzone.app.data.DailyChallengeRepository(
            challengeDao = db.dailyChallengeDao(),
            streakDao = db.streakDao(),
            firestoreSync = firestoreSync,
        )
    }
    val challengeViewModel: DailyChallengeViewModel = viewModel(
        factory = DailyChallengeViewModelFactory(challengeRepository)
    )
```

`MainScreen(...)` chaqiruviga `challengeViewModel = challengeViewModel` qo'shish:

```kotlin
        composable("main") {
            MainScreen(
                mainViewModel = mainViewModel,
                adsManager = adsManager,
                prefs = prefs,
                statsManager = statsManager,
                profileViewModel = profileViewModel,
                challengeViewModel = challengeViewModel,
                onOpenDashboard = { navController.navigate("dashboard") },
            )
        }
```

Import qo'shish:
```kotlin
import uz.kidzone.app.data.DailyChallengeRepository
import uz.kidzone.app.ui.viewmodel.DailyChallengeViewModel
import uz.kidzone.app.ui.viewmodel.DailyChallengeViewModelFactory
```

- [ ] **Step 5: Build va test**

```bash
./gradlew assembleDebug --stacktrace 2>&1 | tail -15
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/KidWebViewManager.kt \
        app/src/main/java/uz/kidzone/app/ui/MainScreen.kt \
        app/src/main/java/uz/kidzone/app/ui/KidZoneApp.kt
git commit -m "feat(faza14): ChallengeBridge + DailyChallengeCard MainScreen ga wire"
```

---

### Task 7: FirestoreSync — streak + challenge sync

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/FirestoreSync.kt`

- [ ] **Step 1: `syncStreak` metodini to'ldirish**

`FirestoreSync.kt` da mavjud `open fun syncStreak(...)` stub ni almashtirish:

```kotlin
    open fun syncStreak(uid: String, profileId: String, count: Int, lastDate: String) {
        if (!isAvailable()) return
        val data = mapOf<String, Any>(
            "count" to count,
            "lastCompletedDate" to lastDate,
        )
        db!!.collection("users").document(uid)
            .collection("profiles").document(profileId)
            .document("streak")
            .set(data, SetOptions.merge())
            .addOnFailureListener { e -> Log.w(TAG, "syncStreak failed: $e") }
    }
```

- [ ] **Step 2: `syncChallengeCompleted` metodini to'ldirish**

```kotlin
    open fun syncChallengeCompleted(
        uid: String,
        profileId: String,
        date: String,
        gameId: String,
        gameTitle: String,
    ) {
        if (!isAvailable()) return
        val data = mapOf<String, Any>(
            "gameId" to gameId,
            "gameTitle" to gameTitle,
            "completed" to true,
            "completedAt" to FieldValue.serverTimestamp(),
        )
        db!!.collection("users").document(uid)
            .collection("profiles").document(profileId)
            .collection("daily_challenges").document(date)
            .set(data, SetOptions.merge())
            .addOnFailureListener { e -> Log.w(TAG, "syncChallengeCompleted failed: $e") }
    }
```

**Izoh:** `DailyChallengeRepository.updateStreak()` da `syncChallengeCompleted` chaqirig'i `current.profileId` o'rniga `gameId` olishi kerak. Task 2, Step 3 dagi repository kodida:

```kotlin
firestoreSync.syncChallengeCompleted(uid, profileId, today, current.profileId, gameTitle)
```

Bu **xato**. To'g'risi:

```kotlin
firestoreSync.syncChallengeCompleted(uid, profileId, today, /* game id kerak */ ...)
```

`updateStreak()` funksiyasiga `gameId: String` parametri qo'shilishi kerak. `DailyChallengeRepository.kt` da:

```kotlin
    private suspend fun updateStreak(profileId: String, today: String, gameId: String, gameTitle: String) {
        // ...
        firestoreSync.syncChallengeCompleted(uid, profileId, today, gameId, gameTitle)
    }
```

Va `markChallengeCompleted()` da chaqiruv:

```kotlin
    suspend fun markChallengeCompleted(profileId: String, gameId: String) {
        val today = todayProvider()
        val id = "$profileId-$today"
        val challenge = challengeDao.getById(id) ?: return
        if (challenge.gameId != gameId || challenge.completed) return

        challengeDao.markCompleted(id)
        updateStreak(profileId, today, gameId, challenge.gameTitle)
    }
```

- [ ] **Step 3: DailyChallengeRepository.kt tuzatish** (yuqoridagi signature fix)

`DailyChallengeRepository.kt` da `markChallengeCompleted` va `updateStreak` ni yuqoridagi to'g'ri versiyaga o'zgartiring.

- [ ] **Step 4: Build va barcha testlar**

```bash
./gradlew test 2>&1 | tail -20
./gradlew assembleDebug 2>&1 | tail -10
```

Expected: barcha testlar PASS, build SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/FirestoreSync.kt \
        app/src/main/java/uz/kidzone/app/data/DailyChallengeRepository.kt
git commit -m "feat(faza14): FirestoreSync — syncStreak + syncChallengeCompleted implementatsiya"
```

---

### Task 8: ParentalDashboard — streak satri + version bump

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/ui/screens/ParentDashboardScreen.kt`
- Modify: `app/src/main/java/uz/kidzone/app/ui/KidZoneApp.kt`
- Modify: `app/build.gradle`

- [ ] **Step 1: `ParentDashboardScreen` signature ga `challengeViewModel` qo'shish**

`ParentDashboardScreen.kt` da funksiya signature:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(
    prefs: SharedPreferences,
    onBack: () -> Unit,
    profileViewModel: ProfileViewModel,
    challengeViewModel: uz.kidzone.app.ui.viewmodel.DailyChallengeViewModel,
    onNavigateToAddEdit: (String?) -> Unit,
) {
```

Funksiya ichida `allStreaks` collect qilish (mavjud `profiles` collect qiluvchi satrdan keyin):

```kotlin
    val allStreaks by challengeViewModel.allStreaks.collectAsState()
```

- [ ] **Step 2: Profil kartasiga streak satri qo'shish**

`ParentDashboardScreen.kt` da profil kartasi `Card` ichida, profilning `name` Text dan keyin streak Row qo'shish:

```kotlin
                // Streak satri
                val streak = allStreaks.firstOrNull { it.profileId == profile.id }
                val streakCount = streak?.count ?: 0
                val todayDate = java.time.LocalDate.now().toString()
                val doneToday = streak?.lastCompletedDate == todayDate
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    Text(
                        "🔥 $streakCount kun streak",
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        if (doneToday) "Bugun: ✅" else "Bugun: ⏳",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
```

- [ ] **Step 3: KidZoneApp.kt — challengeViewModel ni dashboard ga uzatish**

`KidZoneApp.kt` da `ParentDashboardScreen(...)` chaqiruviga `challengeViewModel = challengeViewModel` qo'shish:

```kotlin
        composable("dashboard") {
            ParentDashboardScreen(
                prefs = prefs,
                onBack = { navController.popBackStack() },
                profileViewModel = profileViewModel,
                challengeViewModel = challengeViewModel,
                onNavigateToAddEdit = { profileId ->
                    navController.navigate("add_edit_profile/${profileId ?: "new"}")
                },
            )
        }
```

- [ ] **Step 4: Version bump**

`app/build.gradle` da:

```groovy
versionCode 14
versionName "1.4.0"
```

- [ ] **Step 5: Barcha testlar va build**

```bash
./gradlew test 2>&1 | tail -20
./gradlew assembleDebug 2>&1 | tail -10
```

Expected: barcha testlar PASS, build SUCCESSFUL.

- [ ] **Step 6: Final commit**

```bash
git add app/src/main/java/uz/kidzone/app/ui/screens/ParentDashboardScreen.kt \
        app/src/main/java/uz/kidzone/app/ui/KidZoneApp.kt \
        app/build.gradle
git commit -m "feat(faza14): ParentDashboard streak satri + version bump 1.4.0 (versionCode 14)"
```

---

## Self-Review

**Spec qamrovi tekshiruvi:**

| Spec talabi | Plan da qaerda |
|-------------|---------------|
| T1: KidZoneDatabase v2 + entities + DAOs | Task 1 |
| T2: DailyChallengeRepository | Task 2 |
| T3: DailyChallengeViewModel | Task 3 |
| T4: DailyChallengeCard composable | Task 4 |
| T5: MainScreen DailyChallengeCard | Task 6 |
| T6: main.js callbacks | Task 5 |
| T7: KidWebViewManager addInterface | Task 6 |
| T8: FirestoreSync sync | Task 7 |
| T9: ParentDashboardScreen streak | Task 8 |
| Version bump 1.4.0 | Task 8 |

**Topilgan va tuzatilgan muammolar:**

1. Task 2 da `syncChallengeCompleted` ga noto'g'ri `current.profileId` argument uzatilgan edi — Task 7 da aniq tuzatma ko'rsatildi.
2. `FakeDailyChallengeRepository` test faylidagi `updateGamesList` override to'g'rilandi — JSON manually quriladi.
3. `NoOpFirestoreSync` test classida `open` metodlar override qilinadi — `FirestoreSync` class da `open` keyword kerakligi aniqlanib Task 2 spec ga qo'shildi.
