# 🎮 KidZone — Android Studio O'rnatish Qo'llanmasi

## ⏱️ Vaqt: 15 daqiqa | Daraja: Oson

---

## 📋 KERAKLI DASTURLAR (faqat bir marta)

| Dastur | Versiya | Link |
|--------|---------|------|
| Android Studio | Hedgehog+ | developer.android.com/studio |
| JDK | 17 | (Android Studio o'zi o'rnatadi) |

---

## 🚀 QADAM 1 — Loyihani Ochish

```
1. Bu ZIP ni istalgan papkaga chiqaring
   Masalan: C:\Projects\KidZoneApp\

2. Android Studio ni oching

3. "Open" tugmasini bosing
   (yoki File → Open)

4. KidZoneApp papkasini tanlang → OK

5. Gradle sync tugashini kuting (~2 daqiqa)
```

---

## 🔧 QADAM 2 — SDK O'rnatish (agar kerak bo'lsa)

```
Android Studio → Tools → SDK Manager
→ Android 14 (API 34) → Install
→ OK
```

---

## 📱 QADAM 3 — Qurilmani Ulash

### USB bilan (eng oson):
```
1. Telefoningizda: Sozlamalar → Qurilma haqida
   → Qurilma raqami → 7 marta bosing
   → "Developer options" yoqiladi

2. Developer options → USB Debugging → ON

3. USB kabel bilan kompyuterga ulang

4. Android Studio da qurilmangiz ko'rinadi
```

### Emulator bilan:
```
Android Studio → Device Manager → Create Device
→ Pixel 7 → Next → API 34 → Finish
```

---

## ▶️ QADAM 4 — Ishga Tushirish

```
Android Studio da yuqoridagi ▶ Run tugmasini bosing
           yoki
Klaviatura: Shift + F10
```

**Ko'rinadigan narsa:**
- 🟠 Splash screen (KidZone logo)
- 🌈 Asosiy ekran (30 o'yin)
- 📢 Pastda test banner reklama

---

## 💰 QADAM 5 — AdMob ID Qo'shish

### 5a. Test rejimida ishlash (HOZIR HAM ISHLAYDI ✅)
```
Hozir test ID lar bilan ishlayapti.
Haqiqiy reklama uchun 5b–5d qadamlarni bajaring.
```

### 5b. admob.google.com da hisob oching
```
1. admob.google.com ga kiring
2. "Apps" → "Add app" → Android
3. App nomi: KidZone
4. App ID oling: ca-app-pub-XXXX~XXXX
```

### 5c. Ad Units yarating (4 ta)
```
Banner      → "GZ_Banner"       → ID saqlang
Interstitial → "GZ_Inter"       → ID saqlang
Rewarded    → "GZ_Rewarded"     → ID saqlang
```

### 5d. MainActivity.java da almashtiring:
```
Fayl: app/src/main/java/uz/kidzone/app/MainActivity.java

// HOZIRGI (test):
private static final String BANNER_ID = "ca-app-pub-3940256099942544/6300978111";

// O'ZINGIZNIKIGA ALMASHTIRING:
private static final String BANNER_ID = "ca-app-pub-SIZNING/SIZNING";
```

### 5e. AndroidManifest.xml da almashtiring:
```
Fayl: app/src/main/AndroidManifest.xml

<!-- HOZIRGI (test): -->
android:value="ca-app-pub-3940256099942544~3347511713"

<!-- O'ZINGIZNIKIGA: -->
android:value="ca-app-pub-SIZNING~APPID"
```

---

## 📦 QADAM 6 — APK Yaratish (Play Store uchun)

```
Android Studio:
Build → Generate Signed Bundle / APK

→ APK tanlang → Next

→ "Create new..." (birinchi marta keystore yaratish)
  Key store path: C:\MyKeys\kidzone.jks
  Password: [kuchli parol]
  Alias: kidzone
  → OK

→ Release tanlang
→ Finish

APK joyi:
app/release/app-release.apk  ← SHU FAYL PLAY STORE GA YUKLANADI
```

---

## 🏪 QADAM 7 — Play Store ga Yuklash

```
1. play.google.com/console → $25 to'lov (bir marta)
2. Create app → Android → Free
3. App nomi: KidZone — Bolalar Olami
4. APK yuklash: Production → Create release → Upload APK
5. 3 tilda tavsif yozing (UZ, RU, EN)
6. Skrinshotlar yuklang (min 2 ta)
7. Content rating: Everyone (Bolalar)
8. "Designed for tablets" → ✅ belgilang
9. Review → Publish (1-3 kun tekshiruv)
```

---

## 🐛 KO'P UCHRAYDIGAN XATOLAR

### "Gradle sync failed"
```
File → Invalidate Caches → Invalidate and Restart
```

### "SDK not found"
```
File → Project Structure → SDK Location
→ Android SDK ni ko'rsating
```

### "Build tools not found"
```
Tools → SDK Manager → SDK Tools
→ Android SDK Build-Tools → Install latest
```

### "Minimum SDK" xatosi
```
build.gradle da:
minSdk 21  ← 24 dan past qurilmalar uchun
```

### Reklama chiqmayapti
```
1. Internet bor-yo'qligini tekshiring
2. Logcat da "AdMob" qidiring
3. isTesting = true bo'lishini tekshiring (test uchun)
```

---

## 📁 LOYIHA TUZILMASI

```
KidZoneApp/
├── app/
│   ├── src/main/
│   │   ├── assets/www/
│   │   │   ├── index.html         ← Asosiy app (30 o'yin)
│   │   │   ├── bubble-pop.html    ← 🫧 O'yin 1
│   │   │   ├── color-rush.html    ← 🎨 O'yin 2
│   │   │   ├── memory-game.html   ← 🧠 O'yin 3
│   │   │   ├── animals.html       ← 🐾 O'yin 4
│   │   │   ├── math-kids.html     ← 🔢 O'yin 5
│   │   │   └── colors-shapes.html ← 🌈 O'yin 6
│   │   ├── java/uz/kidzone/app/
│   │   │   └── MainActivity.java  ← AdMob + WebView
│   │   ├── res/
│   │   │   ├── layout/activity_main.xml
│   │   │   ├── values/themes.xml
│   │   │   └── drawable/splash_icon.xml
│   │   └── AndroidManifest.xml    ← Ruxsatlar + AdMob ID
│   └── build.gradle               ← Dependencies
├── build.gradle
├── settings.gradle
└── README.md
```

---

## 💡 QISQA ESLATMALAR

| Narsa | Joyi |
|-------|------|
| AdMob App ID | AndroidManifest.xml |
| Banner/Inter/Rewarded ID | MainActivity.java |
| O'yin fayllari | assets/www/ |
| Ikonka | res/drawable/ |
| Ranglar | res/values/colors.xml |

---

*Muammo bo'lsa — Logcat oching va "KidZone" yoki "AdMob" qidiring 🔍*
