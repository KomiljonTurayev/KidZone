# Faza 8: AdMob Banner Wiring — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `MainScreen` pastiga haqiqiy AdMob banner `AndroidView` qo'shish — `loadBanner()` chaqiriladi, o'yin davomida yashirinadi, o'yin tugagach ko'rinadi.

**Architecture:** `MainUiState` ga `bannerLoaded: Boolean = false` qo'shiladi. `MainScreen` dagi `Box` → `Column` ga aylanadi: ichki `Box(weight(1f))` mavjud WebView + overlaylarni saqlaydi, `Column` oxirida yangi banner `AndroidView` turadi. `BannerListener.onBannerLoaded()` → `mainViewModel.setBannerLoaded(true)` → `update` bloki banner `visibility` ni boshqaradi.

**Tech Stack:** Kotlin, Jetpack Compose, AndroidView, Google Mobile Ads SDK (`AdsManager.loadBanner(container, isTablet, listener)`).

## Global Constraints

- `AdsManager.kt` va `IAdsManager.kt` o'zgartirilmaydi
- `main.js` va `index.html` o'zgartirilmaydi
- `MainActivity.kt` o'zgartirilmaydi
- Banner faqat `bannerVisible && bannerLoaded` bo'lganda ko'rinadi (blank flash yo'q)
- Tablet aniqlash: `ctx.resources.configuration.smallestScreenWidthDp >= 600`
- `adsManager.loadBanner(container, isTablet, listener)` — 3-argument overload (already on `AdsManager`, not on `IAdsManager`) — faqat bir marta, factory blokida chaqiriladi

---

## File Map

| Harakat | Fayl | Nima o'zgaradi |
|---------|------|----------------|
| Modify | `app/src/main/java/uz/kidzone/app/ui/viewmodel/MainViewModel.kt` | `bannerLoaded: Boolean = false` + `setBannerLoaded()` |
| Modify | `app/src/main/java/uz/kidzone/app/ui/MainScreen.kt` | 3 import qo'shish, `Box` → `Column`, banner `AndroidView` |

---

## Task 1: MainViewModel — `bannerLoaded` state

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/ui/viewmodel/MainViewModel.kt:11-34`

**Interfaces:**
- Produces: `MainUiState.bannerLoaded: Boolean` — Task 2 da `uiState.bannerLoaded` orqali o'qiladi
- Produces: `MainViewModel.setBannerLoaded(loaded: Boolean)` — Task 2 da `BannerListener` callback dan chaqiriladi

---

- [ ] **Step 1: `MainUiState` ga `bannerLoaded` maydoni qo'shish**

`MainViewModel.kt` faylini oching. `MainUiState` data class (line 11-20):

**Hozirgi:**
```kotlin
data class MainUiState(
    val language: String = "uz",
    val age: String = "2-4",
    val inGame: Boolean = false,
    val showExitDialog: Boolean = false,
    val isExitFromGame: Boolean = false,
    val promoBanner: PromoBannerData? = null,
    val isLocked: Boolean = false,
    val bannerVisible: Boolean = true,
)
```

**Yangi** (`bannerLoaded` qo'shiladi, `bannerVisible` dan keyin):
```kotlin
data class MainUiState(
    val language: String = "uz",
    val age: String = "2-4",
    val inGame: Boolean = false,
    val showExitDialog: Boolean = false,
    val isExitFromGame: Boolean = false,
    val promoBanner: PromoBannerData? = null,
    val isLocked: Boolean = false,
    val bannerVisible: Boolean = true,
    val bannerLoaded: Boolean = false,
)
```

---

- [ ] **Step 2: `setBannerLoaded()` metod qo'shish**

`MainViewModel` class ichida (line 34 — `setBannerVisible` dan keyin) qo'shing:

**Hozirgi (line 34):**
```kotlin
    fun setBannerVisible(visible: Boolean) { _state.update { it.copy(bannerVisible = visible) } }
}
```

**Yangi:**
```kotlin
    fun setBannerVisible(visible: Boolean) { _state.update { it.copy(bannerVisible = visible) } }
    fun setBannerLoaded(loaded: Boolean) { _state.update { it.copy(bannerLoaded = loaded) } }
}
```

---

- [ ] **Step 3: Build qilib compile xatosi yo'qligini tekshirish**

```powershell
.\gradlew :app:compileDebugKotlin
```

Kutilgan natija: `BUILD SUCCESSFUL` (xato bo'lmaydi).

---

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/ui/viewmodel/MainViewModel.kt
git commit -m "feat(faza8): add bannerLoaded state to MainViewModel"
```

---

## Task 2: MainScreen — Box → Column + banner AndroidView

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/ui/MainScreen.kt`

**Interfaces:**
- Consumes: `MainUiState.bannerLoaded: Boolean` — Task 1 da qo'shildi
- Consumes: `MainViewModel.setBannerLoaded(Boolean)` — Task 1 da qo'shildi
- Consumes: `AdsManager.loadBanner(container: ViewGroup, isTablet: Boolean, listener: IAdsManager.BannerListener?)` — `AdsManager.kt` line 65 da mavjud
- Consumes: `IAdsManager.BannerListener` — `IAdsManager.kt` da mavjud interfeys

---

- [ ] **Step 1: 3 ta import qo'shish**

`MainScreen.kt` import blokchasiga (mavjud importlardan keyin) qo'shing:

```kotlin
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.layout.wrapContentHeight
import uz.kidzone.app.IAdsManager
```

---

- [ ] **Step 2: `Box` → `Column` ga almashtirib, `Box(weight(1f))` ichiga ko'chirish**

`MainScreen.kt` da `Box(modifier = Modifier.fillMaxSize())` (line 75) ni toping.

**Hozirgi tuzilma (line 75-175 atrofida):**
```kotlin
    Box(modifier = Modifier.fillMaxSize()) {
        // WebView
        AndroidView(
            factory = { ctx -> ... },
            modifier = Modifier.fillMaxSize(),
        )

        // Lock overlay
        if (uiState.isLocked) { ... }

        // Promo banner
        AnimatedVisibility(
            visible = uiState.promoBanner != null,
            modifier = Modifier.align(Alignment.TopCenter),
        ) { ... }

        // FAB (Kidzo) — only on home screen (not in-game)
        if (!uiState.inGame && kidzoViewModel != null) { ... }
    }
```

**Yangi tuzilma** — tashqi `Box` → `Column`, ichki `Box(weight(1f))` ga o'raladi:
```kotlin
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            // WebView
            AndroidView(
                factory = { ctx -> ... },
                modifier = Modifier.fillMaxSize(),
            )

            // Lock overlay
            if (uiState.isLocked) { ... }

            // Promo banner
            AnimatedVisibility(
                visible = uiState.promoBanner != null,
                modifier = Modifier.align(Alignment.TopCenter),
            ) { ... }

            // FAB (Kidzo) — only on home screen (not in-game)
            if (!uiState.inGame && kidzoViewModel != null) { ... }
        }

        // Banner (Task 2 Step 3 da qo'shiladi)
    }
```

Faqat tashqi wrapper o'zgaradi: `Box(fillMaxSize)` → `Column(fillMaxSize) { Box(fillMaxWidth.weight(1f)) { ... } }`. Ichki kontent (WebView, LockOverlay, PromoBanner, FAB) o'zgarmaydi.

---

- [ ] **Step 3: Banner `AndroidView` qo'shish**

`Column` ichida, ichki `Box` yopilgandan (`}`) keyin banner `AndroidView` qo'shing:

```kotlin
        // Banner
        AndroidView(
            factory = { ctx ->
                FrameLayout(ctx).apply {
                    val isTablet = ctx.resources.configuration.smallestScreenWidthDp >= 600
                    adsManager.loadBanner(this, isTablet, object : IAdsManager.BannerListener {
                        override fun onBannerLoaded(heightDp: Int) {
                            mainViewModel.setBannerLoaded(true)
                        }
                        override fun onBannerFailed() {}
                    })
                }
            },
            update = { container ->
                val show = uiState.bannerVisible && uiState.bannerLoaded
                container.visibility = if (show) View.VISIBLE else View.GONE
            },
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
        )
```

---

- [ ] **Step 4: Build qilib compile xatosi yo'qligini tekshirish**

```powershell
.\gradlew :app:compileDebugKotlin
```

Kutilgan natija: `BUILD SUCCESSFUL`. Agar `Unresolved reference` xatosi bo'lsa — import blokini tekshiring.

---

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/ui/MainScreen.kt
git commit -m "feat(faza8): wire AdMob banner AndroidView in MainScreen"
```

---

## Task 3: Build, Install va Verify

**Files:** Hech qanday fayl o'zgartirilmaydi.

---

- [ ] **Step 1: To'liq build va qurilmaga o'rnatish**

```powershell
.\gradlew installDebug
```

Kutilgan natija: `BUILD SUCCESSFUL` + APK qurilmaga o'rnatiladi.

---

- [ ] **Step 2: Banner ko'rinishini tekshirish**

1. App ochiladi → bir necha soniya kutiladi (reklama yuklanadi)
2. Ekran pastida banner ko'rinishi kerak ✅
3. WebView konteyneri banner yuklanmagunicha to'liq ekranni egallaydi ✅
4. Banner yuklanganida WebView ozroq siqilishi kerak (banner balandligiga teng) ✅

---

- [ ] **Step 3: O'yin davomida banner yashirinishini tekshirish**

1. Biror o'yin kartasini bosish → o'yin ochiladi
2. Banner yashirinishi kerak (ekran to'liq bo'lishi) ✅
3. Back tugmasi → "O'yindan chiqish?" dialog → "Ha" → o'yin yopiladi
4. Banner yana paydo bo'lishi kerak ✅

---

- [ ] **Step 4: Blank banner flash tekshiruvi**

1. App yangidan ochiladi
2. Banner joyi blank/bo'sh ko'rinmasligi kerak (banner yuklanmaguncha 0 height) ✅

---

- [ ] **Step 5: Logcat tekshiruvi**

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb logcat -s AdsManager -d 2>&1 | Select-Object -Last 15
```

Kutilgan natija: `AdMob Initialized`, `Banner` log satrlari. Runtime crash yo'q.

---

- [ ] **Step 6: Commit**

```bash
git commit --allow-empty -m "chore(faza8): build verified — AdMob banner wiring working"
```
