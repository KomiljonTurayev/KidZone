# Faza 10: Play Store Release Build — Dizayn Spesifikatsiyasi

**Sana:** 2026-06-23
**Holat:** Approved

---

## Maqsad

`proguard-rules.pro`'dagi noto'g'ri WebView JS bridge klass yo'lini to'g'irlash, versiya raqamini oshirish va imzolangan release AAB generatsiya qilish — Play Console'ga yuklashga tayyor holat.

---

## Joriy holat

| Komponent | Holat |
|-----------|-------|
| `kidzone2026.jks` keystore | Mavjud ✅ |
| `keystore.properties` (real parollar) | Mavjud, `.gitignore`'da ✅ |
| Signing config `build.gradle`'da | Mavjud ✅ |
| `minifyEnabled true` + `shrinkResources true` | Yoqilgan ✅ |
| `onSessionStart()` / `onSessionEnd()` wiring | `MainActivity.kt` da mavjud ✅ |
| AdMob `APPLICATION_ID` manifest'da | Mavjud ✅ |
| **ProGuard `AdMobBridge` yo'li** | **Noto'g'ri** ❌ |
| **versionCode / versionName** | 9 / "1.0.3" — bump kerak ❌ |
| **Imzolangan release AAB** | Hali generatsiya qilinmagan ❌ |

---

## Muammo tahlili

### ProGuard noto'g'ri klass yo'li

`proguard-rules.pro`'da:
```proguard
-keepclassmembers class uz.kidzone.app.MainActivity$AdMobBridge {
    public *;
}
```

`AdMobBridge` Kotlin migratsiyasidan (Faza 1) keyin `MainScreen.kt`'da top-level `private class` sifatida joylashgan. Compiled bytecode'da klass yo'li `uz.kidzone.app.ui.AdMobBridge` (yoki R8 tomonidan rename qilingan nom). Hozirgi qoida hech bir klassga mos kelmaydi.

`@JavascriptInterface` annotatsiyasi bilan belgilangan metodlar rename qilinsa, WebView JavaScript chaqiruvlari (`showBanner()`, `hideBanner()`, `showInterstitial()`, `gameLaunched()`, va h.k.) ishlamay qoladi. Bu release build'da yashirin xato.

---

## Yechim

### Task 1: `proguard-rules.pro` to'g'irlash

Annotatsiyaga asoslangan keep qoidasi — klass yo'li o'zgarsayam ishlaydigan yondashuv:

```proguard
# AdMob
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }

# WebView JavaScript interface — annotatsiyaga asoslanib saqlanadi
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface

# Firebase
-keep class com.google.firebase.** { *; }
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# KidZone public API (R8 internal klasslarni optimizatsiya qiladi)
-keep public class uz.kidzone.app.** { public *; }
```

**O'chiriladi:** `-keepclassmembers class uz.kidzone.app.MainActivity$AdMobBridge`
**O'chiriladi:** `-keep class uz.kidzone.app.** { *; }` (juda keng, R8 ni bekor qiladi)
**Qo'shiladi:** `@JavascriptInterface` annotatsiyasiga asoslangan qoida
**Qo'shiladi:** Firebase keep qoidalari (Faza 7 bilan kelgan Firebase)

### Task 2: `build.gradle` — version bump

```groovy
// Oldin:
versionCode  project.hasProperty('versionCode') ? project.versionCode.toInteger() : 9
versionName  project.hasProperty('versionName') ? project.versionName : "1.0.3"

// Yangi:
versionCode  project.hasProperty('versionCode') ? project.versionCode.toInteger() : 10
versionName  project.hasProperty('versionName') ? project.versionName : "1.1.0"
```

`versionCode 10` → Play Console birinchi yuklashda `1`dan yuqori bo'lishi kerak emas, lekin `10` 9 ta fazani ifodalaydi va mantiqiy.
`versionName "1.1.0"` → Faza 1-9 major ishlari uchun minor bump.

### Task 3: `bundleRelease` — imzolangan AAB

```powershell
.\gradlew bundleRelease
```

`keystore.properties` → `keystoreProps` orqali o'qiladi, `signingConfig signingConfigs.release` ishlatiladi.

Natija: `app/build/outputs/bundle/release/app-release.aab`

Play Console → Production → New release → AAB yuklash.

---

## Fayl O'zgarishlari

| Fayl | Nima |
|------|------|
| `app/proguard-rules.pro` | `MainActivity$AdMobBridge` o'chiriladi, annotatsiyaga asoslangan + Firebase keep qoidalari qo'shiladi |
| `app/build.gradle` | `versionCode` default `9→10`, `versionName` `"1.0.3"→"1.1.0"` |

**O'zgartirilmaydi:** `MainActivity.kt`, `MainScreen.kt`, `AndroidManifest.xml`, barcha boshqa fayllar.

---

## Muvaffaqiyat mezonlari

- [ ] `.\gradlew bundleRelease` → `BUILD SUCCESSFUL`
- [ ] `app-release.aab` fayli generatsiya qilinadi
- [ ] AAB `jarsigner -verify` yoki Play Console upload bilan imzo tekshiriladi
- [ ] Runtime crash yo'q (WebView JS bridge ishlaydi)
