# Faza 11 — Ko'p Farzand Profili: Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Har bir bola uchun to'liq izolyatsiyalangan profil (til, vaqt limiti, PIN, statistika, avatar) tizimini qo'shish — app start'da profil tanlash ekrani + dashboard'dan almashtirish, Room DB lokal saqlash, Firestore sync.

**Architecture:** Room database `ProfileEntity` va `ProfileStatsEntity` saqlaydi; `ProfileRepository` Room + Firestore sync wrapper'i; `ProfileViewModel` barcha UI'ga `activeProfile: StateFlow<ProfileEntity?>` tarqatadi. Mavjud `ParentalStatsManager` profil ID prefiksi bilan profile-aware bo'ladi; vaqt limiti `ProfileEntity.timeLimitMinutes`'dan o'qiladi (endi SharedPreferences'dan emas).

**Tech Stack:** Room 2.6.1 + kapt, Coil Compose 2.6.0, Kotlin Coroutines Flow, Firebase Firestore, ActivityResultContracts (kamera/galereya).

## Global Constraints

- `minSdk 26`, `targetSdk 35`, `compileSdk 35`
- `versionCode 11`, `versionName "1.2.0"` (Task 12'da bump)
- Package: `uz.kidzone.app`
- Room DB nomi: `"kidzone.db"`, version 1, `exportSchema = false`
- Profil ID: `UUID.randomUUID().toString()`
- Avatar yo'li: `context.filesDir.absolutePath + "/profiles/{profileId}.jpg"`
- SharedPreferences fayl: `"kz_prefs"`, faol profil kaliti: `"active_profile_id"`
- Barcha yangi Kotlin fayllar `uz.kidzone.app` paketi yoki pastki paketlarda

---

## Fayl xaritasi

**Yangi fayllar:**
- `app/src/main/java/uz/kidzone/app/data/ProfileEntity.kt`
- `app/src/main/java/uz/kidzone/app/data/ProfileStatsEntity.kt`
- `app/src/main/java/uz/kidzone/app/data/ProfileDao.kt`
- `app/src/main/java/uz/kidzone/app/data/ProfileStatsDao.kt`
- `app/src/main/java/uz/kidzone/app/data/KidZoneDatabase.kt`
- `app/src/main/java/uz/kidzone/app/data/ProfileSyncManager.kt`
- `app/src/main/java/uz/kidzone/app/data/ProfileRepository.kt`
- `app/src/main/java/uz/kidzone/app/ui/viewmodel/ProfileViewModel.kt`
- `app/src/main/java/uz/kidzone/app/ui/screens/ProfileSelectScreen.kt`
- `app/src/main/java/uz/kidzone/app/ui/screens/AddEditProfileScreen.kt`
- `app/src/test/java/uz/kidzone/app/data/ProfileRepositoryTest.kt`
- `app/src/test/java/uz/kidzone/app/ui/viewmodel/ProfileViewModelTest.kt`

**O'zgaradigan fayllar:**
- `app/build.gradle` — kapt plugin, Room, Coil dependency
- `app/proguard-rules.pro` — Room keep qoidasi
- `app/src/main/java/uz/kidzone/app/ParentalStatsManager.kt` — profileId prefix
- `app/src/main/java/uz/kidzone/app/KidZoneApplication.kt` — migration
- `app/src/main/java/uz/kidzone/app/MainActivity.kt` — ProfileRepository + ProfileViewModel
- `app/src/main/java/uz/kidzone/app/ui/KidZoneApp.kt` — profile_select route
- `app/src/main/java/uz/kidzone/app/ui/MainScreen.kt` — activeProfile language/PIN/timeLimit
- `app/src/main/java/uz/kidzone/app/ui/screens/ParentDashboardScreen.kt` — profil bo'limi
- `app/src/main/java/uz/kidzone/app/ui/viewmodel/DashboardViewModel.kt` — timeLimitMinutes → profile

---

### Task 1: Room + Coil dependency qo'shish

**Files:**
- Modify: `app/build.gradle`
- Modify: `app/proguard-rules.pro`

**Interfaces:**
- Produces: `kapt` annotation processing, `Room` + `Coil` importlar barcha keyingi tasklarda ishlaydi

- [ ] **Step 1: build.gradle — kapt plugin qo'shish**

`app/build.gradle` fayli `plugins` blokiga:
```groovy
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'kotlin-kapt'
}
```

- [ ] **Step 2: build.gradle — Room va Coil dependency qo'shish**

`dependencies` blokiga (test dependency'lardan oldin):
```groovy
    // Room
    implementation "androidx.room:room-runtime:2.6.1"
    implementation "androidx.room:room-ktx:2.6.1"
    kapt "androidx.room:room-compiler:2.6.1"

    // Coil (avatar image loading)
    implementation "io.coil-kt:coil-compose:2.6.0"
```

- [ ] **Step 3: proguard-rules.pro — Room keep qoidasi qo'shish**

```
# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
```

- [ ] **Step 4: Build'ni tekshirish**

```
./gradlew assembleDebug
```
Kutilgan natija: `BUILD SUCCESSFUL`. Xato bo'lsa — sync qiling: `./gradlew --refresh-dependencies`.

- [ ] **Step 5: Commit**

```bash
git add app/build.gradle app/proguard-rules.pro
git commit -m "feat(faza11): add Room 2.6.1 + Coil 2.6.0 + kapt dependencies"
```

---

### Task 2: Room data layer — Entity'lar, DAO'lar, Database

**Files:**
- Create: `app/src/main/java/uz/kidzone/app/data/ProfileEntity.kt`
- Create: `app/src/main/java/uz/kidzone/app/data/ProfileStatsEntity.kt`
- Create: `app/src/main/java/uz/kidzone/app/data/ProfileDao.kt`
- Create: `app/src/main/java/uz/kidzone/app/data/ProfileStatsDao.kt`
- Create: `app/src/main/java/uz/kidzone/app/data/KidZoneDatabase.kt`

**Interfaces:**
- Produces:
  - `ProfileEntity(id, name, avatarPath, language, timeLimitMinutes, pinHash, isDefault, createdAt)`
  - `ProfileStatsEntity(id, profileId, date, minutesPlayed, gamesPlayed)`
  - `ProfileDao.getAll(): Flow<List<ProfileEntity>>`
  - `ProfileDao.getById(id: String): ProfileEntity?`
  - `ProfileDao.count(): Int`
  - `ProfileDao.insert(profile: ProfileEntity)`
  - `ProfileDao.update(profile: ProfileEntity)`
  - `ProfileDao.delete(profile: ProfileEntity)`
  - `ProfileStatsDao.getByProfileAndDate(profileId, date): ProfileStatsEntity?`
  - `ProfileStatsDao.upsert(stats: ProfileStatsEntity)`
  - `KidZoneDatabase.getInstance(context): KidZoneDatabase`
  - `KidZoneDatabase.profileDao(): ProfileDao`
  - `KidZoneDatabase.profileStatsDao(): ProfileStatsDao`

- [ ] **Step 1: ProfileEntity.kt yaratish**

```kotlin
// app/src/main/java/uz/kidzone/app/data/ProfileEntity.kt
package uz.kidzone.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val avatarPath: String?,
    val language: String,
    val timeLimitMinutes: Int,
    val pinHash: String?,
    val isDefault: Boolean,
    val createdAt: Long,
)
```

- [ ] **Step 2: ProfileStatsEntity.kt yaratish**

```kotlin
// app/src/main/java/uz/kidzone/app/data/ProfileStatsEntity.kt
package uz.kidzone.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "profile_stats",
    foreignKeys = [ForeignKey(
        entity = ProfileEntity::class,
        parentColumns = ["id"],
        childColumns = ["profileId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["profileId", "date"], unique = true)],
)
data class ProfileStatsEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val profileId: String,
    val date: String,
    val minutesPlayed: Int,
    val gamesPlayed: Int,
)
```

- [ ] **Step 3: ProfileDao.kt yaratish**

```kotlin
// app/src/main/java/uz/kidzone/app/data/ProfileDao.kt
package uz.kidzone.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY createdAt ASC")
    fun getAll(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ProfileEntity?

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: ProfileEntity)

    @Update
    suspend fun update(profile: ProfileEntity)

    @Delete
    suspend fun delete(profile: ProfileEntity)
}
```

- [ ] **Step 4: ProfileStatsDao.kt yaratish**

```kotlin
// app/src/main/java/uz/kidzone/app/data/ProfileStatsDao.kt
package uz.kidzone.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProfileStatsDao {
    @Query("SELECT * FROM profile_stats WHERE profileId = :profileId AND date = :date LIMIT 1")
    suspend fun getByProfileAndDate(profileId: String, date: String): ProfileStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: ProfileStatsEntity)
}
```

- [ ] **Step 5: KidZoneDatabase.kt yaratish**

```kotlin
// app/src/main/java/uz/kidzone/app/data/KidZoneDatabase.kt
package uz.kidzone.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProfileEntity::class, ProfileStatsEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class KidZoneDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun profileStatsDao(): ProfileStatsDao

    companion object {
        @Volatile private var instance: KidZoneDatabase? = null

        fun getInstance(context: Context): KidZoneDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KidZoneDatabase::class.java,
                    "kidzone.db",
                ).build().also { instance = it }
            }
    }
}
```

- [ ] **Step 6: Compile tekshirish**

```
./gradlew compileDebugKotlin
```
Kutilgan natija: `BUILD SUCCESSFUL`. Room annotation processor kapt orqali `ProfileDao_Impl` va boshqalarni generate qilishi kerak.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/data/
git commit -m "feat(faza11): Room data layer — ProfileEntity, ProfileStatsEntity, DAOs, KidZoneDatabase"
```

---

### Task 3: ProfileSyncManager — Firestore push/pull

**Files:**
- Create: `app/src/main/java/uz/kidzone/app/data/ProfileSyncManager.kt`

**Interfaces:**
- Consumes: `FirebaseFirestore?` (nullable — offline-safe), `FirebaseAuth`
- Produces:
  - `ProfileSyncManager(db: FirebaseFirestore?)`
  - `fun pushProfile(profile: ProfileEntity)`
  - `fun pushStats(stats: ProfileStatsEntity)`
  - `suspend fun pullProfiles(uid: String): List<ProfileEntity>`

- [ ] **Step 1: ProfileSyncManager.kt yaratish**

```kotlin
// app/src/main/java/uz/kidzone/app/data/ProfileSyncManager.kt
package uz.kidzone.app.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class ProfileSyncManager(private val db: FirebaseFirestore?) {

    companion object {
        private const val TAG = "ProfileSyncManager"
    }

    fun pushProfile(profile: ProfileEntity) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = db ?: return
        val data = mapOf(
            "name" to profile.name,
            "language" to profile.language,
            "timeLimitMinutes" to profile.timeLimitMinutes,
            "pinHash" to (profile.pinHash ?: ""),
            "isDefault" to profile.isDefault,
            "createdAt" to profile.createdAt,
        )
        db.collection("users").document(uid)
            .collection("profiles").document(profile.id)
            .set(data, SetOptions.merge())
            .addOnFailureListener { e -> Log.w(TAG, "pushProfile failed: $e") }
    }

    fun pushStats(stats: ProfileStatsEntity) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = db ?: return
        val data = mapOf(
            "minutesPlayed" to stats.minutesPlayed,
            "gamesPlayed" to stats.gamesPlayed,
        )
        db.collection("users").document(uid)
            .collection("profiles").document(stats.profileId)
            .collection("stats").document(stats.date)
            .set(data, SetOptions.merge())
            .addOnFailureListener { e -> Log.w(TAG, "pushStats failed: $e") }
    }

    suspend fun pullProfiles(uid: String): List<ProfileEntity> {
        val db = db ?: return emptyList()
        return try {
            val snap = db.collection("users").document(uid)
                .collection("profiles").get().await()
            snap.documents.mapNotNull { doc ->
                val name = doc.getString("name") ?: return@mapNotNull null
                ProfileEntity(
                    id = doc.id,
                    name = name,
                    avatarPath = null,
                    language = doc.getString("language") ?: "uz",
                    timeLimitMinutes = (doc.getLong("timeLimitMinutes") ?: 0L).toInt(),
                    pinHash = doc.getString("pinHash")?.takeIf { it.isNotEmpty() },
                    isDefault = doc.getBoolean("isDefault") ?: false,
                    createdAt = doc.getLong("createdAt") ?: 0L,
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "pullProfiles failed: $e")
            emptyList()
        }
    }
}
```

- [ ] **Step 2: Compile tekshirish**

```
./gradlew compileDebugKotlin
```
Kutilgan: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/data/ProfileSyncManager.kt
git commit -m "feat(faza11): ProfileSyncManager — Firestore push/pull for profiles and stats"
```

---

### Task 4: ProfileRepository

**Files:**
- Create: `app/src/main/java/uz/kidzone/app/data/ProfileRepository.kt`

**Interfaces:**
- Consumes: `ProfileDao`, `ProfileStatsDao`, `SharedPreferences`, `ProfileSyncManager`
- Produces:
  - `ProfileRepository(profileDao, profileStatsDao, prefs, syncManager)`
  - `val profiles: Flow<List<ProfileEntity>>`
  - `fun getActiveProfileId(): String?`
  - `fun setActiveProfileId(id: String)`
  - `suspend fun getActiveProfile(): ProfileEntity?`
  - `suspend fun count(): Int`
  - `suspend fun insert(profile: ProfileEntity)`
  - `suspend fun update(profile: ProfileEntity)`
  - `suspend fun delete(profile: ProfileEntity)`
  - `suspend fun upsertStats(stats: ProfileStatsEntity)`
  - `suspend fun getStats(profileId: String, date: String): ProfileStatsEntity?`
  - `suspend fun pullAndMergeFromFirestore(uid: String)`

- [ ] **Step 1: ProfileRepository.kt yaratish**

```kotlin
// app/src/main/java/uz/kidzone/app/data/ProfileRepository.kt
package uz.kidzone.app.data

import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow

class ProfileRepository(
    private val profileDao: ProfileDao,
    private val profileStatsDao: ProfileStatsDao,
    private val prefs: SharedPreferences,
    private val syncManager: ProfileSyncManager,
) {
    val profiles: Flow<List<ProfileEntity>> = profileDao.getAll()

    fun getActiveProfileId(): String? = prefs.getString("active_profile_id", null)

    fun setActiveProfileId(id: String) {
        prefs.edit().putString("active_profile_id", id).apply()
    }

    suspend fun getActiveProfile(): ProfileEntity? {
        val id = getActiveProfileId() ?: return null
        return profileDao.getById(id)
    }

    suspend fun count(): Int = profileDao.count()

    suspend fun insert(profile: ProfileEntity) {
        profileDao.insert(profile)
        syncManager.pushProfile(profile)
    }

    suspend fun update(profile: ProfileEntity) {
        profileDao.update(profile)
        syncManager.pushProfile(profile)
    }

    suspend fun delete(profile: ProfileEntity) {
        profileDao.delete(profile)
    }

    suspend fun upsertStats(stats: ProfileStatsEntity) {
        profileStatsDao.upsert(stats)
        syncManager.pushStats(stats)
    }

    suspend fun getStats(profileId: String, date: String): ProfileStatsEntity? =
        profileStatsDao.getByProfileAndDate(profileId, date)

    suspend fun pullAndMergeFromFirestore(uid: String) {
        val remote = syncManager.pullProfiles(uid)
        remote.forEach { remoteProfile ->
            val local = profileDao.getById(remoteProfile.id)
            if (local == null || remoteProfile.createdAt > local.createdAt) {
                profileDao.insert(remoteProfile.copy(avatarPath = local?.avatarPath))
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/data/ProfileRepository.kt
git commit -m "feat(faza11): ProfileRepository — CRUD + Firestore sync wrapper"
```

---

### Task 5: ParentalStatsManager — profile-aware qilish

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/ParentalStatsManager.kt`

**Interfaces:**
- Consumes: Mavjud `ParentalStatsManager` API
- Produces: `ParentalStatsManager(ctx, profileId: String = "default")` — daily stat kalitlari `${profileId}_` prefiksi bilan; `getTimeLimitMinutes()` va `setTimeLimitMinutes()` ham profil-prefixli

- [ ] **Step 1: Constructor'ga `profileId` qo'shish**

Mavjud ikki constructor'ni o'zgartirish:

```kotlin
class ParentalStatsManager {

    companion object {
        // KEY_TIME_LIMIT endi companion'da yo'q — instance method'da ishlatiladi

        @JvmStatic
        fun todayPtKey(profileId: String): String =
            "${profileId}_kz_pt_" + SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

        @JvmStatic
        fun todayGlKey(profileId: String): String =
            "${profileId}_kz_gl_" + SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

        @JvmStatic
        fun parseList(csv: String?): MutableList<String> {
            if (csv.isNullOrEmpty()) return mutableListOf()
            return csv.split(",").toMutableList()
        }

        @JvmStatic
        fun joinList(list: List<String>): String = list.joinToString(",")
    }

    private val prefs: SharedPreferences
    private val profileId: String
    private val timeLimitKey: String
    private var sessionStartMs: Long = 0L
    private val sessionGames: MutableList<String> = mutableListOf()

    constructor(ctx: Context, profileId: String = "default") :
        this(ctx.getSharedPreferences("kz_prefs", Context.MODE_PRIVATE), profileId)

    internal constructor(prefs: SharedPreferences, profileId: String = "default") {
        this.prefs = prefs
        this.profileId = profileId
        this.timeLimitKey = "${profileId}_kz_time_limit"
    }

    fun onSessionStart() {
        sessionStartMs = System.currentTimeMillis()
        sessionGames.clear()
    }

    fun onSessionEnd() {
        if (sessionStartMs == 0L) return
        val elapsed = ((System.currentTimeMillis() - sessionStartMs) / 60_000L).toInt()
        sessionStartMs = 0L
        if (elapsed <= 0) return
        val key = todayPtKey(profileId)
        prefs.edit().putInt(key, prefs.getInt(key, 0) + elapsed).apply()
    }

    fun onGameLaunched(gameId: String?) {
        if (gameId.isNullOrEmpty()) return
        if (!sessionGames.contains(gameId)) sessionGames.add(gameId)
        val key = todayGlKey(profileId)
        val existing = prefs.getString(key, "") ?: ""
        val list = parseList(existing)
        if (!list.contains(gameId)) {
            list.add(gameId)
            prefs.edit().putString(key, joinList(list)).apply()
        }
    }

    fun getSessionMinutes(): Long {
        if (sessionStartMs == 0L) return 0L
        return (System.currentTimeMillis() - sessionStartMs) / 60_000L
    }

    fun getSessionGames(): List<String> = ArrayList(sessionGames)

    fun getTodayMinutes(): Int {
        val saved = prefs.getInt(todayPtKey(profileId), 0)
        val current = if (sessionStartMs > 0L)
            ((System.currentTimeMillis() - sessionStartMs) / 60_000L).toInt()
        else 0
        return saved + current
    }

    fun getWeeklyMinutes(): IntArray {
        val result = IntArray(7)
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.US)
        val cal = Calendar.getInstance()
        for (i in 6 downTo 0) {
            val key = "${profileId}_kz_pt_" + sdf.format(cal.time)
            result[i] = prefs.getInt(key, 0)
            if (i == 6 && sessionStartMs > 0L) {
                result[i] += ((System.currentTimeMillis() - sessionStartMs) / 60_000L).toInt()
            }
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return result
    }

    fun getTodayGames(): List<String> =
        parseList(prefs.getString(todayGlKey(profileId), "") ?: "")

    fun getTimeLimitMinutes(): Int = prefs.getInt(timeLimitKey, 0)

    fun setTimeLimitMinutes(minutes: Int) {
        prefs.edit().putInt(timeLimitKey, maxOf(0, minutes)).apply()
    }

    fun isTimeLimitReached(): Boolean {
        val limit = getTimeLimitMinutes()
        return limit > 0 && getTodayMinutes() >= limit
    }
}
```

- [ ] **Step 2: Compile tekshirish**

```
./gradlew compileDebugKotlin
```
Kutilgan: `BUILD SUCCESSFUL`. `DashboardViewModel` hali ham `ParentalStatsManager(context)` deb chaqiradi — bu hali ham ishlaydi (default `profileId = "default"`).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/ParentalStatsManager.kt
git commit -m "feat(faza11): ParentalStatsManager — profileId prefix for per-profile daily stats"
```

---

### Task 6: KidZoneApplication — migration

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/KidZoneApplication.kt`

**Interfaces:**
- Consumes: `KidZoneDatabase.getInstance()`, `ProfileDao`, `PinUtil.getOrMigrateHash()`
- Produces: On first launch with 0 profiles → creates "Asosiy" profile in Room, writes `active_profile_id` to prefs

- [ ] **Step 1: import'lar qo'shish**

`KidZoneApplication.kt` boshiga:
```kotlin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uz.kidzone.app.data.KidZoneDatabase
import uz.kidzone.app.data.ProfileEntity
import java.util.UUID
```

- [ ] **Step 2: `onCreate()`'ga migration call qo'shish**

`onCreate()` ichida `createNotificationChannel()` dan keyin:
```kotlin
CoroutineScope(Dispatchers.IO).launch { migrateToProfilesIfNeeded() }
```

- [ ] **Step 3: `migrateToProfilesIfNeeded()` funksiyasi qo'shish**

```kotlin
private suspend fun migrateToProfilesIfNeeded() {
    val db = KidZoneDatabase.getInstance(this)
    if (db.profileDao().count() > 0) return

    val prefs = getSharedPreferences("kz_prefs", MODE_PRIVATE)
    val lang = prefs.getString("kz_lang", "uz") ?: "uz"
    val timeLimit = prefs.getInt("kz_time_limit", 0)
    val pinHash = PinUtil.getOrMigrateHash(prefs, "kz_pin")
    val profileId = UUID.randomUUID().toString()

    val profile = ProfileEntity(
        id = profileId,
        name = "Asosiy",
        avatarPath = null,
        language = lang,
        timeLimitMinutes = timeLimit,
        pinHash = pinHash,
        isDefault = true,
        createdAt = System.currentTimeMillis(),
    )
    db.profileDao().insert(profile)
    prefs.edit()
        .putString("active_profile_id", profileId)
        .putInt("kz_profile_count", 1)
        .apply()
    android.util.Log.d("KZ_DEBUG", "Migration complete: default profile=$profileId")
}
```

- [ ] **Step 4: Compile va run tekshirish**

```
./gradlew assembleDebug
```
Emulator yoki real qurilmada bir marta ishga tushiring. Logcat'da `Migration complete: default profile=...` ko'rinishi kerak.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/KidZoneApplication.kt
git commit -m "feat(faza11): one-time migration from SharedPreferences to Room default profile"
```

---

### Task 7: ProfileViewModel

**Files:**
- Create: `app/src/main/java/uz/kidzone/app/ui/viewmodel/ProfileViewModel.kt`

**Interfaces:**
- Consumes: `ProfileRepository`
- Produces:
  - `ProfileViewModel(repository: ProfileRepository)`
  - `ProfileViewModelFactory(repository: ProfileRepository)`
  - `val profiles: StateFlow<List<ProfileEntity>>`
  - `val activeProfile: StateFlow<ProfileEntity?>`
  - `fun setActiveProfile(profile: ProfileEntity)`
  - `fun insertProfile(profile: ProfileEntity)`
  - `fun updateProfile(profile: ProfileEntity)`
  - `fun deleteProfile(profile: ProfileEntity, onSwitched: (ProfileEntity?) -> Unit)`

- [ ] **Step 1: ProfileViewModel.kt yaratish**

```kotlin
// app/src/main/java/uz/kidzone/app/ui/viewmodel/ProfileViewModel.kt
package uz.kidzone.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.kidzone.app.data.ProfileEntity
import uz.kidzone.app.data.ProfileRepository

class ProfileViewModel(private val repository: ProfileRepository) : ViewModel() {

    val profiles: StateFlow<List<ProfileEntity>> = repository.profiles.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    private val _activeProfile = MutableStateFlow<ProfileEntity?>(null)
    val activeProfile: StateFlow<ProfileEntity?> = _activeProfile.asStateFlow()

    init {
        viewModelScope.launch {
            _activeProfile.value = repository.getActiveProfile()
        }
    }

    fun setActiveProfile(profile: ProfileEntity) {
        repository.setActiveProfileId(profile.id)
        _activeProfile.value = profile
    }

    fun insertProfile(profile: ProfileEntity) {
        viewModelScope.launch { repository.insert(profile) }
    }

    fun updateProfile(profile: ProfileEntity) {
        viewModelScope.launch {
            repository.update(profile)
            if (_activeProfile.value?.id == profile.id) {
                _activeProfile.value = profile
            }
        }
    }

    fun deleteProfile(profile: ProfileEntity, onSwitched: (ProfileEntity?) -> Unit) {
        viewModelScope.launch {
            repository.delete(profile)
            if (_activeProfile.value?.id == profile.id) {
                val next = profiles.first().firstOrNull { it.id != profile.id }
                if (next != null) setActiveProfile(next)
                onSwitched(next)
            } else {
                onSwitched(_activeProfile.value)
            }
        }
    }
}

class ProfileViewModelFactory(private val repository: ProfileRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ProfileViewModel(repository) as T
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/ui/viewmodel/ProfileViewModel.kt
git commit -m "feat(faza11): ProfileViewModel — active profile state + CRUD"
```

---

### Task 8: ProfileSelectScreen

**Files:**
- Create: `app/src/main/java/uz/kidzone/app/ui/screens/ProfileSelectScreen.kt`

**Interfaces:**
- Consumes: `List<ProfileEntity>`, `onSelect: (ProfileEntity) -> Unit`, `onAddNew: () -> Unit`
- Produces: `ProfileSelectScreen(profiles, onSelect, onAddNew)` Composable

- [ ] **Step 1: ProfileSelectScreen.kt yaratish**

```kotlin
// app/src/main/java/uz/kidzone/app/ui/screens/ProfileSelectScreen.kt
package uz.kidzone.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import uz.kidzone.app.data.ProfileEntity
import java.io.File
import kotlin.math.absoluteValue

@Composable
fun ProfileSelectScreen(
    profiles: List<ProfileEntity>,
    onSelect: (ProfileEntity) -> Unit,
    onAddNew: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Kim o'ynaydi?",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp),
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(profiles, key = { it.id }) { profile ->
                    ProfileCard(profile = profile, onClick = { onSelect(profile) })
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onAddNew) {
                Text("+ Yangi profil")
            }
        }
    }
}

@Composable
fun ProfileCard(profile: ProfileEntity, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.size(120.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (profile.avatarPath != null && File(profile.avatarPath).exists()) {
                AsyncImage(
                    model = File(profile.avatarPath),
                    contentDescription = profile.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape),
                )
            } else {
                ProfileInitialAvatar(name = profile.name, size = 72.dp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = profile.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun ProfileInitialAvatar(name: String, size: Dp) {
    val avatarColors = listOf(
        Color(0xFF4CAF50), Color(0xFF2196F3),
        Color(0xFFFF9800), Color(0xFF9C27B0),
        Color(0xFFE91E63), Color(0xFF00BCD4),
    )
    val color = avatarColors[name.hashCode().absoluteValue % avatarColors.size]
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.firstOrNull()?.uppercase() ?: "?",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/ui/screens/ProfileSelectScreen.kt
git commit -m "feat(faza11): ProfileSelectScreen — avatar grid profile picker"
```

---

### Task 9: AddEditProfileScreen — avatar + forma

**Files:**
- Create: `app/src/main/java/uz/kidzone/app/ui/screens/AddEditProfileScreen.kt`

**Interfaces:**
- Consumes: `profile: ProfileEntity?` (null = yangi profil), `onSave: (ProfileEntity) -> Unit`, `onCancel: () -> Unit`
- Produces: `AddEditProfileScreen(profile, onSave, onCancel)` Composable

- [ ] **Step 1: AddEditProfileScreen.kt yaratish**

```kotlin
// app/src/main/java/uz/kidzone/app/ui/screens/AddEditProfileScreen.kt
package uz.kidzone.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import uz.kidzone.app.PinUtil
import uz.kidzone.app.data.ProfileEntity
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProfileScreen(
    profile: ProfileEntity?,
    onSave: (ProfileEntity) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val isNew = profile == null
    val profileId = remember { profile?.id ?: UUID.randomUUID().toString() }

    var name by remember { mutableStateOf(profile?.name ?: "") }
    var avatarPath by remember { mutableStateOf(profile?.avatarPath) }
    var language by remember { mutableStateOf(profile?.language ?: "uz") }
    var timeLimitSlider by remember { mutableFloatStateOf((profile?.timeLimitMinutes ?: 0).toFloat()) }
    var pinInput by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            avatarPath = saveAvatarFromUri(context, uri, profileId)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            avatarPath = saveBitmapAsAvatar(context, bitmap, profileId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Yangi profil" else "Profilni tahrirlash") },
                navigationIcon = { TextButton(onClick = onCancel) { Text("Bekor") } },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Avatar
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2196F3))
                        .clickable {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (avatarPath != null && File(avatarPath!!).exists()) {
                        AsyncImage(
                            model = File(avatarPath!!),
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(
                            text = if (name.isNotEmpty()) name.first().uppercase() else "?",
                            color = Color.White,
                            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Galereya") }
                OutlinedButton(
                    onClick = { cameraLauncher.launch(null) },
                    modifier = Modifier.weight(1f),
                ) { Text("Kamera") }
            }

            // Ism
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = false },
                label = { Text("Bola ismi") },
                isError = nameError,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (nameError) {
                Text("Ism bo'sh bo'lmasin", color = Color.Red)
            }

            // Til
            Text("Til:")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("uz" to "O'zbek", "ru" to "Русский", "en" to "English").forEach { (code, label) ->
                    FilterChip(
                        selected = language == code,
                        onClick = { language = code },
                        label = { Text(label) },
                    )
                }
            }

            // Vaqt limiti
            val limitMinutes = timeLimitSlider.roundToInt()
            Text("Vaqt limiti: ${if (limitMinutes == 0) "Cheksiz" else "$limitMinutes daqiqa"}")
            Slider(
                value = timeLimitSlider,
                onValueChange = { timeLimitSlider = it },
                valueRange = 0f..180f,
                steps = 11,
                modifier = Modifier.fillMaxWidth(),
            )

            // PIN (ixtiyoriy)
            OutlinedTextField(
                value = pinInput,
                onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinInput = it },
                label = { Text("PIN (ixtiyoriy, 4 xona)") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    if (name.isBlank()) { nameError = true; return@Button }
                    val pinHash = when {
                        pinInput.length == 4 -> PinUtil.hash(pinInput)
                        pinInput.isEmpty() -> profile?.pinHash
                        else -> { nameError = false; return@Button }
                    }
                    val saved = (profile ?: ProfileEntity(
                        id = profileId,
                        name = "",
                        avatarPath = null,
                        language = "uz",
                        timeLimitMinutes = 0,
                        pinHash = null,
                        isDefault = false,
                        createdAt = System.currentTimeMillis(),
                    )).copy(
                        name = name.trim(),
                        avatarPath = avatarPath,
                        language = language,
                        timeLimitMinutes = limitMinutes,
                        pinHash = pinHash,
                    )
                    onSave(saved)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Saqlash")
            }
        }
    }
}

private fun saveAvatarFromUri(context: Context, uri: Uri, profileId: String): String {
    val dir = File(context.filesDir, "profiles").apply { mkdirs() }
    val file = File(dir, "$profileId.jpg")
    context.contentResolver.openInputStream(uri)?.use { input ->
        val bmp = BitmapFactory.decodeStream(input)
        FileOutputStream(file).use { out -> bmp.compress(Bitmap.CompressFormat.JPEG, 80, out) }
    }
    return file.absolutePath
}

private fun saveBitmapAsAvatar(context: Context, bitmap: Bitmap, profileId: String): String {
    val dir = File(context.filesDir, "profiles").apply { mkdirs() }
    val file = File(dir, "$profileId.jpg")
    FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out) }
    return file.absolutePath
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/ui/screens/AddEditProfileScreen.kt
git commit -m "feat(faza11): AddEditProfileScreen — name, avatar camera/gallery, lang, time limit, PIN"
```

---

### Task 10: ParentDashboardScreen — profil bo'limi qo'shish

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/ui/screens/ParentDashboardScreen.kt`

**Interfaces:**
- Consumes: `profileViewModel: ProfileViewModel` (yangi param), mavjud `prefs`, `onBack`
- Produces: Dashboard'da "Profillar" bo'limi — ro'yxat, yangi qo'shish, tahrirlash, o'chirish

- [ ] **Step 1: Function signature'ga profileViewModel qo'shish**

`ParentDashboardScreen` function parametrlarini o'zgartirish:
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(
    prefs: SharedPreferences,
    onBack: () -> Unit,
    profileViewModel: ProfileViewModel,
    onNavigateToAddEdit: (String?) -> Unit,  // null = yangi, profileId = tahrirlash
)
```

- [ ] **Step 2: Profil bo'limi import'lari qo'shish**

`ParentDashboardScreen.kt` fayli boshiga qo'shish:
```kotlin
import androidx.compose.runtime.collectAsState
import uz.kidzone.app.data.ProfileEntity
import uz.kidzone.app.ui.viewmodel.ProfileViewModel
```

- [ ] **Step 3: PIN gate'ni activeProfile'ga bog'lash**

Mavjud `val savedPinHash = remember { PinUtil.getOrMigrateHash(prefs, "kz_pin") }` o'rniga:
```kotlin
val activeProfile by profileViewModel.activeProfile.collectAsState()
val savedPinHash = activeProfile?.pinHash
```

- [ ] **Step 4: LazyColumn'ga "Profillar" bo'limi qo'shish**

`LazyColumn` ichida, mavjud statistika bo'limidan KEYIN:
```kotlin
// Profillar bo'limi
item {
    Spacer(Modifier.height(16.dp))
    Text("Profillar", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
}

val profiles by profileViewModel.profiles.collectAsState()
items(profiles) { profile ->
    ProfileListItem(
        profile = profile,
        isActive = profile.id == activeProfile?.id,
        onEdit = { onNavigateToAddEdit(profile.id) },
        onDelete = {
            if (profiles.size > 1) {
                profileViewModel.deleteProfile(profile) {}
            }
        },
        onSwitch = { profileViewModel.setActiveProfile(profile) },
    )
}

item {
    Spacer(Modifier.height(8.dp))
    OutlinedButton(
        onClick = { onNavigateToAddEdit(null) },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("+ Yangi profil qo'shish") }
}
```

- [ ] **Step 5: `ProfileListItem` composable qo'shish**

Faylga private composable qo'shish:
```kotlin
@Composable
private fun ProfileListItem(
    profile: ProfileEntity,
    isActive: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSwitch: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProfileInitialAvatar(name = profile.name, size = 40.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${profile.language.uppercase()} | ${if (profile.timeLimitMinutes == 0) "Cheksiz" else "${profile.timeLimitMinutes} daq"}",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (isActive) Text("✓ Faol", style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onSwitch) { Text("Tanlash") }
            TextButton(onClick = onEdit) { Text("Tahrir") }
            TextButton(onClick = onDelete) { Text("O'chir") }
        }
    }
}
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/ui/screens/ParentDashboardScreen.kt
git commit -m "feat(faza11): ParentDashboardScreen — profile list, add/edit/delete/switch"
```

---

### Task 11: MainActivity + KidZoneApp + MainScreen — hammani ulash

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/MainActivity.kt`
- Modify: `app/src/main/java/uz/kidzone/app/ui/KidZoneApp.kt`
- Modify: `app/src/main/java/uz/kidzone/app/ui/MainScreen.kt`

**Interfaces:**
- Consumes: `ProfileViewModel`, `ProfileRepository`, `KidZoneDatabase`
- Produces: App oqimi — `profile_select` route, `MainScreen` activeProfile'dan language/PIN/timeLimit oladi

- [ ] **Step 1: MainActivity — import'lar qo'shish**

```kotlin
import uz.kidzone.app.data.KidZoneDatabase
import uz.kidzone.app.data.ProfileRepository
import uz.kidzone.app.data.ProfileSyncManager
import uz.kidzone.app.ui.viewmodel.ProfileViewModel
import uz.kidzone.app.ui.viewmodel.ProfileViewModelFactory
```

- [ ] **Step 2: MainActivity — profileViewModel qo'shish**

`MainActivity` klassi ichiga, `mainViewModel` dan keyin:
```kotlin
private val profileViewModel: ProfileViewModel by viewModels {
    val db = KidZoneDatabase.getInstance(this)
    ProfileViewModelFactory(
        ProfileRepository(
            db.profileDao(),
            db.profileStatsDao(),
            kzPrefs,
            ProfileSyncManager(FirestoreSync.getInstance().getDb()),
        )
    )
}
```

- [ ] **Step 3: MainActivity — statsManager profil ID bilan yaratish**

`onCreate()` ichida `statsManager = ParentalStatsManager(this)` o'rniga:
```kotlin
val activeProfileId = kzPrefs.getString("active_profile_id", "default") ?: "default"
statsManager = ParentalStatsManager(this, activeProfileId)
```

- [ ] **Step 4: MainActivity — KidZoneApp'ga profileViewModel uzatish**

`setContent {}` bloki:
```kotlin
setContent {
    KidZoneApp(
        prefs = kzPrefs,
        mainViewModel = mainViewModel,
        adsManager = adsManager,
        statsManager = statsManager,
        profileViewModel = profileViewModel,
    )
}
```

- [ ] **Step 5: KidZoneApp.kt — profileViewModel param + yangi routelar**

`KidZoneApp.kt` to'liq yangi versiyasi:
```kotlin
package uz.kidzone.app.ui

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import uz.kidzone.app.AdsManager
import uz.kidzone.app.ParentalStatsManager
import uz.kidzone.app.ui.screens.AddEditProfileScreen
import uz.kidzone.app.ui.screens.OnboardingScreen
import uz.kidzone.app.ui.screens.ParentDashboardScreen
import uz.kidzone.app.ui.screens.ProfileSelectScreen
import uz.kidzone.app.ui.viewmodel.MainViewModel
import uz.kidzone.app.ui.viewmodel.ProfileViewModel

@Composable
fun KidZoneApp(
    prefs: SharedPreferences,
    mainViewModel: MainViewModel,
    adsManager: AdsManager,
    statsManager: ParentalStatsManager,
    profileViewModel: ProfileViewModel,
) {
    val navController = rememberNavController()
    val onboardingDone = prefs.getBoolean("kz_onboarding_done", false)
    val profiles by profileViewModel.profiles.collectAsState()

    val startDestination = when {
        !onboardingDone -> "onboarding"
        profiles.size >= 2 -> "profile_select"
        else -> "main"
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("onboarding") {
            OnboardingScreen(
                prefs = prefs,
                onDone = {
                    navController.navigate("main") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
        composable("profile_select") {
            ProfileSelectScreen(
                profiles = profiles,
                onSelect = { profile ->
                    profileViewModel.setActiveProfile(profile)
                    navController.navigate("main") {
                        popUpTo("profile_select") { inclusive = true }
                    }
                },
                onAddNew = { navController.navigate("add_edit_profile/new") },
            )
        }
        composable("main") {
            MainScreen(
                mainViewModel = mainViewModel,
                adsManager = adsManager,
                prefs = prefs,
                statsManager = statsManager,
                profileViewModel = profileViewModel,
                onOpenDashboard = { navController.navigate("dashboard") },
            )
        }
        composable("dashboard") {
            ParentDashboardScreen(
                prefs = prefs,
                onBack = { navController.popBackStack() },
                profileViewModel = profileViewModel,
                onNavigateToAddEdit = { profileId ->
                    navController.navigate("add_edit_profile/${profileId ?: "new"}")
                },
            )
        }
        composable("add_edit_profile/{profileId}") { backStack ->
            val profileId = backStack.arguments?.getString("profileId")
            val profile = if (profileId == "new") null
                          else profiles.firstOrNull { it.id == profileId }
            AddEditProfileScreen(
                profile = profile,
                onSave = { saved ->
                    if (profile == null) profileViewModel.insertProfile(saved)
                    else profileViewModel.updateProfile(saved)
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() },
            )
        }
    }
}
```

- [ ] **Step 6: MainScreen.kt — profileViewModel param qo'shish va activeProfile ishlatish**

`MainScreen` function signature o'zgartirish:
```kotlin
@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    adsManager: AdsManager,
    prefs: SharedPreferences,
    statsManager: ParentalStatsManager,
    profileViewModel: ProfileViewModel,
    onOpenDashboard: () -> Unit,
)
```

`MainScreen` function body boshiga, `uiState` dan keyin:
```kotlin
val activeProfile by profileViewModel.activeProfile.collectAsState()
```

- [ ] **Step 7: MainScreen — language'ni activeProfile'dan olish**

WebView `factory` lambdasi ichida mavjud:
```kotlin
val lang = prefs.getString("kz_lang", "uz") ?: "uz"
val age = prefs.getString("kz_age", "2-4") ?: "2-4"
```
O'rniga:
```kotlin
val lang = activeProfile?.language ?: prefs.getString("kz_lang", "uz") ?: "uz"
val age = prefs.getString("kz_age", "2-4") ?: "2-4"
```

- [ ] **Step 8: MainScreen — PIN gate'ni activeProfile.pinHash'dan olish**

Lock overlay ichida mavjud:
```kotlin
val savedPinHash = remember { PinUtil.getOrMigrateHash(prefs, "kz_pin") }
```
O'rniga:
```kotlin
val savedPinHash = activeProfile?.pinHash
```
`PinGate` `hasPinSet` parametri:
```kotlin
hasPinSet = !savedPinHash.isNullOrEmpty(),
```

- [ ] **Step 9: MainScreen — vaqt limiti tekshiruvini activeProfile'dan olish**

Mavjud time limit `LaunchedEffect` topish (while(true) bilan) va o'zgartirish. Agar mavjud bo'lsa:
```kotlin
LaunchedEffect(activeProfile) {
    while (true) {
        delay(30_000)
        val limit = activeProfile?.timeLimitMinutes ?: 0
        if (limit > 0 && statsManager.getTodayMinutes() >= limit) {
            mainViewModel.showLock()
        }
    }
}
```

- [ ] **Step 10: Build tekshirish**

```
./gradlew assembleDebug
```
Kutilgan: `BUILD SUCCESSFUL`. Barcha compile xatolari bartaraf etilishi kerak.

- [ ] **Step 11: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/MainActivity.kt
git add app/src/main/java/uz/kidzone/app/ui/KidZoneApp.kt
git add app/src/main/java/uz/kidzone/app/ui/MainScreen.kt
git commit -m "feat(faza11): wire ProfileViewModel into KidZoneApp, MainScreen, MainActivity"
```

---

### Task 12: Version bump + TDD testlar

**Files:**
- Modify: `app/build.gradle` (version bump)
- Create: `app/src/test/java/uz/kidzone/app/data/ProfileRepositoryTest.kt`
- Create: `app/src/test/java/uz/kidzone/app/ui/viewmodel/ProfileViewModelTest.kt`

**Interfaces:**
- Consumes: `ProfileRepository`, `ProfileViewModel`, fake DAO va fake prefs

- [ ] **Step 1: versionCode va versionName bump**

`app/build.gradle` `defaultConfig` blokida:
```groovy
versionCode  project.hasProperty('versionCode')  ? project.versionCode.toInteger()  : 11
versionName  project.hasProperty('versionName')  ? project.versionName               : "1.2.0"
```

- [ ] **Step 2: Test papkasini yaratish**

```bash
mkdir -p app/src/test/java/uz/kidzone/app/data
mkdir -p app/src/test/java/uz/kidzone/app/ui/viewmodel
```

- [ ] **Step 3: ProfileRepositoryTest.kt yozish**

```kotlin
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
```

- [ ] **Step 4: Testlarni ishga tushirish**

```
./gradlew testDebugUnitTest --tests "uz.kidzone.app.data.ProfileRepositoryTest"
```
Kutilgan: `BUILD SUCCESSFUL`, barcha 6 test PASS.

- [ ] **Step 5: ProfileViewModelTest.kt yozish**

```kotlin
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
        val list = vm.profiles.first()
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
```

- [ ] **Step 6: Testlarni ishga tushirish**

```
./gradlew testDebugUnitTest --tests "uz.kidzone.app.ui.viewmodel.ProfileViewModelTest"
```
Kutilgan: `BUILD SUCCESSFUL`, barcha 5 test PASS.

- [ ] **Step 7: To'liq test suite**

```
./gradlew testDebugUnitTest
```
Kutilgan: barcha testlar PASS.

- [ ] **Step 8: Commit**

```bash
git add app/build.gradle
git add app/src/test/java/uz/kidzone/app/data/ProfileRepositoryTest.kt
git add app/src/test/java/uz/kidzone/app/ui/viewmodel/ProfileViewModelTest.kt
git commit -m "feat(faza11): version 1.2.0 + TDD tests — ProfileRepositoryTest, ProfileViewModelTest"
```

---

## Spec Coverage Self-Review

| Spec talabi | Task |
|-------------|------|
| ProfileEntity, ProfileStatsEntity | Task 2 |
| KidZoneDatabase Room v1 | Task 2 |
| ProfileDao, ProfileStatsDao | Task 2 |
| ProfileRepository | Task 4 |
| ProfileSyncManager Firestore push/pull | Task 3 |
| ParentalStatsManager profile-aware | Task 5 |
| Migration SharedPreferences → Room | Task 6 |
| ProfileViewModel | Task 7 |
| ProfileSelectScreen (app start) | Task 8 |
| AddEditProfileScreen (avatar kamera/galereya) | Task 9 |
| ParentDashboardScreen profil bo'limi | Task 10 |
| KidZoneApp profile_select route | Task 11 |
| MainScreen activeProfile language/PIN/timeLimit | Task 11 |
| Room + Coil dependency | Task 1 |
| TDD testlar (Repository, ViewModel) | Task 12 |
| versionCode 11 / versionName 1.2.0 | Task 12 |
| Avatar filesDir/profiles/{id}.jpg | Task 9 |
| Kamida 1 profil qolishi tekshiruvi | Task 10 (o'chirish tugmasi faqat `profiles.size > 1` da) |
| Offline Firestore xatoli | Task 3 (nullable db, try/catch) |
| Avatar fallback (initial harf) | Task 8 (ProfileInitialAvatar) |
