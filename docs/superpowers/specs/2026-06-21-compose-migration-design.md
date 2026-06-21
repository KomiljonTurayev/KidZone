# To'liq Kotlin + Compose Migratsiya — Dizayn Spesifikatsiyasi

**Sana:** 2026-06-21
**Holat:** Approved
**Boshlang'ich nuqta:** `eb58aaf` — toza Java/XML app

---

## Maqsad

KidZone ilovasini Java/XML dan to'liq Kotlin + Jetpack Compose ga ko'chirish.
3 ta alohida Activity → 1 ta `ComponentActivity` + `NavHost`.
Barcha managerlar Kotlin ga, barcha UI Compose ga ko'chiriladi.

---

## Arxitektura

```
ComponentActivity (MainActivity.kt)
  └── setContent { KidZoneApp() }
        └── NavHost
              ├── "onboarding"  → OnboardingScreen
              ├── "main"        → MainScreen
              └── "dashboard"   → ParentDashboardScreen
                                   (PIN gate ichida)
```

`BottomBar` yo'q — WebView o'zining tab UI sini boshqaradi.

---

## Migratsiya tartibi

### Bosqich 1 — build.gradle: Kotlin + Compose setup

`app/build.gradle` ga qo'shimchalar:

```gradle
buildFeatures { compose true }
composeOptions { kotlinCompilerExtensionVersion '1.5.13' }
kotlinOptions { jvmTarget = '17' }

implementation platform('androidx.compose:compose-bom:2024.04.01')
implementation 'androidx.compose.ui:ui'
implementation 'androidx.compose.ui:ui-tooling-preview'
implementation 'androidx.compose.material3:material3'
implementation 'androidx.activity:activity-compose:1.9.0'
implementation 'androidx.navigation:navigation-compose:2.7.7'
implementation 'androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0'
implementation 'androidx.lifecycle:lifecycle-runtime-compose:2.7.0'
debugImplementation 'androidx.compose.ui:ui-tooling'
```

Muvaffaqiyat: `.\gradlew compileDebugKotlin` xatosiz o'tadi.

---

### Bosqich 2 — Managerlar: Java → Kotlin

Har bir Java manager Kotlin ga ko'chiriladi (xuddi shu logika, Kotlin syntax). Java fayl o'chiriladi.

**Asosiy fayllar:**

| Java | Kotlin | Izoh |
|------|--------|------|
| `KidZoneApplication.java` | `KidZoneApplication.kt` | `object` singleton emas, Application subclass |
| `IAdsManager.java` | `IAdsManager.kt` | Kotlin interface |
| `AdsManager.java` | `AdsManager.kt` | |
| `KidWebViewManager.java` | `KidWebViewManager.kt` | Deprecated API ogohlantirish bartaraf etiladi |
| `MusicManager.java` | `MusicManager.kt` | `companion object` → `object` singleton |
| `SystemUiHelper.java` | `SystemUiHelper.kt` | |
| `FirebaseManager.java` | `FirebaseManager.kt` | |
| `FirestoreSync.java` | `FirestoreSync.kt` | |
| `FcmTokenManager.java` | `FcmTokenManager.kt` | |
| `BanChecker.java` | `BanChecker.kt` | |
| `BannerChecker.java` | `BannerChecker.kt` | |
| `BackendClient.java` | `BackendClient.kt` | |
| `ParentalStatsManager.java` | `ParentalStatsManager.kt` | |
| `PinUtil.java` | `PinUtil.kt` | |
| `AdminConfig.java` | `AdminConfig.kt` | |
| `KidZoneFirebaseMessagingService.java` | `KidZoneFirebaseMessagingService.kt` | |
| `PinDialogHelper.java` | **O'CHIRILADI** | Compose `PinDialog` composable bilan almashtiriladi |

**`kidzo/` papkasi:**

| Java | Kotlin | Izoh |
|------|--------|------|
| `KidzoAgent.java` | `KidzoAgent.kt` | |
| `GeminiCaller.java` | `GeminiCaller.kt` | interface |
| `RealGeminiCaller.java` | `RealGeminiCaller.kt` | |
| `ContentFilter.java` | `ContentFilter.kt` | |
| `ContentCard.java` | `ContentCard.kt` | `data class` |
| `ContentItem.java` | `ContentItem.kt` | `data class` |
| `ActionParser.java` | `ActionParser.kt` | |
| `KidzoState.java` | `KidzoState.kt` | `sealed class` |
| `KidzoStateListener.java` | **O'CHIRILADI** | `StateFlow` + `collect` bilan almashtiriladi |
| `MainThreadRunner.java` | **O'CHIRILADI** | `withContext(Dispatchers.Main)` bilan almashtiriladi |
| `KidzoBottomSheet.java` | **O'CHIRILADI** | `KidzoSheet.kt` Compose bilan almashtiriladi |
| `KidzoCardAdapter.java` | **O'CHIRILADI** | `LazyRow` composable bilan almashtiriladi |

Muvaffaqiyat: barcha Java fayllar o'chirilgan, `compileDebugKotlin` xatosiz.

---

### Bosqich 3 — MainActivity: ComponentActivity + Compose

**O'zgaradigan fayllar:**
- `MainActivity.java` → `MainActivity.kt`
- `ui/MainScreen.kt` (yangi)
- `ui/viewmodel/MainViewModel.kt` (yangi)
- `ui/KidZoneApp.kt` (yangi — NavHost)

**MainActivity.kt:**
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        SplashScreen.installSplashScreen(this)
        super.onCreate(savedInstanceState)
        setContent { KidZoneApp() }
    }
}
```

**MainViewModel.kt — state:**
```kotlin
data class PromoBannerData(val title: String, val body: String, val url: String)

data class MainUiState(
    val language: String = "uz",
    val age: String = "2-4",
    val inGame: Boolean = false,
    val showExitDialog: Boolean = false,
    val promoBanner: PromoBannerData? = null,
    val isLocked: Boolean = false,
)
```

**MainScreen.kt — tarkib:**
- `AndroidView { WebView }` — HTML5 o'yinlar
- `AnimatedVisibility` — promo banner
- `Box` fullscreen — lock overlay (vaqt limiti)
- `AlertDialog` — exit dialog
- `BackHandler` — WebView `canGoBack()` yoki exit dialog
- Pulsing FAB — `KidzoSheet` ochadi

**O'chiriladigan XML fayllar:**
- `activity_main.xml`
- `dialog_exit.xml`
- `view_lock_overlay.xml`

Muvaffaqiyat: `installDebug` → asosiy o'yin ekrani ishlaydi.

---

### Bosqich 4 — OnboardingActivity → OnboardingScreen

**O'zgaradigan fayllar:**
- `OnboardingActivity.java` → **o'chiriladi**
- `ui/screens/OnboardingScreen.kt` (yangi)
- `ui/viewmodel/OnboardingViewModel.kt` (yangi)
- `AndroidManifest.xml` — OnboardingActivity yozuvi o'chiriladi

**OnboardingScreen.kt — tarkib:**
- 3 bosqich: til tanlash → ism kiritish → yosh guruhi
- `AnimatedContent` — bosqichlar orasida silliq o'tish
- `LaunchedEffect` — TTS salomlashuv
- Tugagach `kz_onboarding_done = true` prefs ga yoziladi, NavHost `main` ga o'tadi

**O'chiriladigan XML fayllar:**
- `activity_onboarding.xml`

Muvaffaqiyat: `installDebug` → yangi qurilmada onboarding Compose da ishlaydi.

---

### Bosqich 5 — ParentalDashboardActivity → ParentDashboardScreen

**O'zgaradigan fayllar:**
- `ParentalDashboardActivity.java` → **o'chiriladi**
- `ui/screens/ParentDashboardScreen.kt` (yangi)
- `ui/viewmodel/DashboardViewModel.kt` (yangi)
- `AndroidManifest.xml` — ParentalDashboardActivity yozuvi o'chiriladi

**ParentDashboardScreen.kt — tarkib:**

PIN gate (inline, alohida route emas):
```
[● ● ● ●]  4-xonali klaviatura
To'g'ri PIN → Dashboard ko'rsatiladi
Noto'g'ri → shake animatsiya
```

Dashboard seksiyalari (`LazyColumn`):
1. Statistika — bugungi daqiqalar + 7 kunlik bar chart
2. Vaqt limiti — `[-] 45 min [+]`, 15 daqiqalik qadamlar
3. Yosh guruhi — `FilterChip` row
4. PIN o'zgartirish — `ChangePinDialog` (inline `AlertDialog`): joriy PIN → yangi PIN × 2, mos kelsa `PinUtil.hash()` → prefs
5. Cloud Backup — Firebase Auth sign-in/out
6. Push bildirishnomalar — toggle + oxirgi 10 ta tarix

**O'chiriladigan XML fayllar:**
- `activity_parental_dashboard.xml`
- `dialog_pin.xml`
- `dialog_email_auth.xml`

Muvaffaqiyat: `installDebug` → "🔒 Ota-ona" tugmasi Dashboard ga olib boradi.

---

### Bosqich 6 — Kidzo: Compose ModalBottomSheet

**O'zgaradigan fayllar:**
- `ui/screens/KidzoSheet.kt` (yangi)
- `ui/viewmodel/KidzoViewModel.kt` (yangi)

**KidzoSheet.kt — tarkib:**
```
ModalBottomSheet {
  LazyRow { KidzoCardItem() }   ← KidzoCardAdapter o'rniga
  ChatInput + SendButton
}
```

**KidzoViewModel.kt:**
```kotlin
class KidzoViewModel(private val agent: KidzoAgent) : ViewModel() {
    val state: StateFlow<KidzoState>
    fun sendMessage(text: String)
    fun requestRecommendations()
}
```

**O'chiriladigan XML fayllar:**
- `bottom_sheet_kidzo.xml`
- `item_kidzo_card.xml`

Muvaffaqiyat: FAB bosilganda Compose ModalBottomSheet ochiladi.

---

## Qolgan XML fayl

`dialog_story.xml` — Stories feature (Faza 3 da Compose da yoziladi). Hozircha saqlanadi.

---

## Muvaffaqiyat mezonlari

- [ ] Barcha Java fayllar o'chirilgan, faqat Kotlin qolgan
- [ ] `compileDebugKotlin` xatosiz
- [ ] Onboarding yangi qurilmada to'g'ri ishlaydi
- [ ] HTML5 o'yinlar WebView da ochiladi
- [ ] Kidzo FAB → ModalBottomSheet ochiladi
- [ ] "🔒 Ota-ona" → PIN gate → Dashboard
- [ ] Vaqt limiti qo'yilsa lock overlay ishlaydi
- [ ] Push bildirishnomalar keladi
