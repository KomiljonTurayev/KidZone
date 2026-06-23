# Faza 8: AdMob Banner Wiring — Dizayn Spesifikatsiyasi

**Sana:** 2026-06-23
**Holat:** Approved

---

## Maqsad

`AdsManager.loadBanner()` hech qerda chaqirilmaydi va Compose daraxtida haqiqiy `AdView` yo'q. Natijada banner ekranda ko'rinmaydi. Faza 8 buni to'g'irlaydi: `MainScreen` pastida doimiy banner `AndroidView` qo'shiladi, `MainViewModel` da `bannerLoaded` state qo'shiladi.

---

## Joriy holat

| Komponent | Holat |
|-----------|-------|
| `AdsManager.initialize()` | `loadInterstitial()` + `loadRewarded()` chaqiradi ✅ |
| `AdsManager.loadBanner()` | Hech qerda chaqirilmaydi ❌ |
| `main.js` `showBanner()` / `hideBanner()` | `viewModel.setBannerVisible()` ni to'g'ri chaqiradi ✅ |
| `MainViewModel.bannerVisible` | State bor, lekin Compose da haqiqiy view yo'q ❌ |
| Interstitial / Rewarded | To'liq ishlaydi ✅ |

---

## Yondashuv

### Layout o'zgarishi (`MainScreen.kt`)

Hozirgi:
```
Box(fillMaxSize) {
  AndroidView(WebView, fillMaxSize)
  Lock overlay
  Promo banner
  FAB
}
```

Yangi:
```
Column(fillMaxSize) {
  Box(fillMaxWidth, weight(1f)) {
    AndroidView(WebView, fillMaxSize)
    Lock overlay
    Promo banner
    FAB
  }
  AndroidView(FrameLayout banner, fillMaxWidth, wrapContentHeight)
}
```

Banner container `Column` ning eng pastida joylashadi. `weight(1f)` bilan `Box` qolgan joyni to'ldiradi — banner yuklanmagan yoki yashirin bo'lsa, WebView to'liq ekranni egallaydi.

### Banner holat boshqaruvi

**Muammo:** `loadBanner()` ga `AdView` qo'shilganda (ad load bo'lmagan holda) bo'sh joy ko'rinishi mumkin.

**Yechim:** `bannerLoaded: Boolean = false` state qo'shiladi. Banner faqat `bannerVisible && bannerLoaded` bo'lganida ko'rinadi.

```
App start → bannerVisible=true, bannerLoaded=false → banner GONE (0 height)
Ad loads  → BannerListener.onBannerLoaded() → bannerLoaded=true → banner VISIBLE
O'yin     → hideBanner() → bannerVisible=false → banner GONE → WebView kengayadi
O'yin end → showBanner() → bannerVisible=true → bannerLoaded=true → banner VISIBLE
```

### Tablet aniqlash

`ctx.resources.configuration.smallestScreenWidthDp >= 600` → `isTablet`

`isTablet=true` → `AdSize.LEADERBOARD` (728×90dp)
`isTablet=false` → `AdSize.BANNER` (320×50dp)

---

## Fayl O'zgarishlari

| Fayl | Nima |
|------|------|
| `app/src/main/java/uz/kidzone/app/ui/viewmodel/MainViewModel.kt` | `bannerLoaded: Boolean = false` state maydoni + `setBannerLoaded(Boolean)` metod |
| `app/src/main/java/uz/kidzone/app/ui/MainScreen.kt` | Box → Column, banner `AndroidView` qo'shish, `IAdsManager` import |

**O'zgartirilmaydi:** `AdsManager.kt`, `IAdsManager.kt`, `main.js`, `index.html`, `MainActivity.kt`

---

## MainViewModel o'zgarishi

`UiState` ga yangi maydon:
```kotlin
data class UiState(
    // ... mavjud maydonlar ...
    val bannerVisible: Boolean = true,
    val bannerLoaded: Boolean = false,   // yangi
)
```

Yangi metod:
```kotlin
fun setBannerLoaded(loaded: Boolean) { _state.update { it.copy(bannerLoaded = loaded) } }
```

---

## MainScreen o'zgarishi

### Import qo'shish
```kotlin
import android.view.View
import android.widget.FrameLayout
import uz.kidzone.app.IAdsManager
```

### Layout qayta tuzish

Hozirgi `Box(modifier = Modifier.fillMaxSize())` → `Column(modifier = Modifier.fillMaxSize())` ga aylanadi.

Mavjud `Box` kontenti `Box(modifier = Modifier.fillMaxWidth().weight(1f))` ichiga ko'chiriladi.

### Banner AndroidView (Column oxirida)

```kotlin
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

**Eslatma:** `adsManager` `MainScreen` da allaqachon `AdsManager` concrete type sifatida keladi (not `IAdsManager`), shuning uchun 3-argument overload `loadBanner(container, isTablet, listener)` to'g'ridan chaqiriladi.

---

## Muvaffaqiyat mezonlari

- [ ] App ochilganda banner pastda ko'rinadi (ad yuklanganidan keyin)
- [ ] O'yin boshlanganida banner yashirinadi (`hideBanner()`)
- [ ] O'yin yopilganida banner yana ko'rinadi (`showBanner()`)
- [ ] Blank/bo'sh banner flash ko'rinmaydi
- [ ] Tablet qurilmada LEADERBOARD o'lchami ishlatiladi
- [ ] Build `BUILD SUCCESSFUL`, runtime xato yo'q
