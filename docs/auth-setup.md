# Kirish (Guest / Google / Telegram) — sozlash

Ilova har doim **mehmon (guest)** sifatida ochiladi: o'yinlar, ertaklar, qo'shiqlar login
so'ramaydi. Quyidagi imkoniyatlar ota-ona hisobi talab qiladi va avval ota-ona qulfi
(PIN yoki matematik savol), so'ng kirish ekrani chiqadi:

| Imkoniyat | Qayerda tekshiriladi |
|---|---|
| Kidzo AI (suhbat, ovoz, AI ertak) | `MainScreen` (🐥 tugma, `AndroidBridge.openKidzo/askKidzo/generateStory`) |
| Ota-ona paneli | `ParentDashboardScreen` (PIN'dan keyin) |
| Bulutli sinxronlash, Premium | Ota-ona paneli ichida (faqat kirgan hisob uchun) |

Ro'yxat bitta joyda: `auth/FeatureGate.kt` → `AccountFeature`.

## 1. Google bilan kirish (Firebase console)

1. Firebase console → **kidzone-9cc89** → Authentication → Sign-in method → **Google** → Enable.
2. Project settings → Android ilova `uz.kidzone.app` → **SHA-1** barmoq izlarini qo'shing:
   - upload kaliti (`C:\kidzone`, alias `kidzone`):
     `keytool -list -v -keystore C:\kidzone -alias kidzone`
   - Play Console → App signing → **App signing key** SHA-1 (Play'dan o'rnatilgan ilova shu kalit bilan imzolanadi)
   - debug kaliti: `keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android`
3. Yangi `google-services.json` ni yuklab `app/` ga qo'ying. Unda `oauth_client` ichida
   `client_type: 3` (web client) paydo bo'lishi kerak — ilova uni `default_web_client_id`
   sifatida avtomatik o'qiydi. Usiz Google tugmasi "Bu kirish usuli hozircha sozlanmagan" deydi.

## 2. Telegram bilan kirish (bot + backend)

1. Telegram'da **@BotFather** → `/newbot` → nom va username (masalan `KidZoneUzBot`). Token beriladi.
2. Render → kidzone-backend → Environment:

   | O'zgaruvchi | Qiymat |
   |---|---|
   | `TELEGRAM_BOT_TOKEN` | BotFather bergan token |
   | `TELEGRAM_BOT_USERNAME` | botning username'i, `@` siz |
   | `TELEGRAM_WEBHOOK_SECRET` | tasodifiy uzun satr (`A-Z a-z 0-9 _ -`, 1–256 belgi) |
   | `TELEGRAM_WEBHOOK_URL` | `https://kidzone-backend-s7to.onrender.com/telegram/webhook` |

3. Backend'ni deploy qiling — ishga tushganda webhook o'zi ro'yxatdan o'tadi
   (logda `Telegram webhook registered`). O'zgaruvchilar bo'lmasa, Telegram endpointlari 503 qaytaradi.

Oqim: ilova `POST /auth/telegram/start` → `t.me/<bot>?start=<id>` ochiladi → ota-ona **Start**
bosadi → bot tasdiqlaydi → ilova `GET /auth/telegram/status/<id>` orqali Firebase custom token
oladi va `telegram:<telegramId>` hisobiga kiradi. Havola 5 daqiqa amal qiladi, bir marta
ishlatiladi va faqat uni yaratgan qurilma (guest hisob) uni ishlata oladi.

## Ma'lumotlar

- Google: mehmon hisobi Google'ga **bog'lanadi** (uid o'zgarmaydi, ma'lumot saqlanadi). Agar shu
  Google hisobi avval ishlatilgan bo'lsa, o'sha hisobga kiriladi.
- Telegram / boshqa qurilmadan kirish: hisobdagi profillar yuklab olinadi va shu qurilmadagilar
  bilan birlashtiriladi (`ProfileCloudSyncListener`).
- Chiqish yoki hisobni o'chirish ilovani yangi mehmon holatiga qaytaradi.
