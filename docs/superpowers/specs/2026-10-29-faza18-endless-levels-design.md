# Faza 18 — Cheksiz darajalar, darajalar xaritasi, kunlik vazifalar, ota-ona va birga o'ynash

## Muammo (2026-10-29, real telefonda sinovda topildi)

- 32 ta o'yin umumiy `KZL` (`www/kz-ui.js`) daraja tizimidan foydalanadi, lekin daraja **20 bilan cheklangan**: `CONFIGS` — 20 qatorli jadval, `getLevel/setLevel` `Math.min(20, …)`. 13 ta o'yinda `level < 20`, `n <= 20` qattiq yozilgan.
- 20-darajada taymer 8 soniya — 2–4 yoshli bola uchun o'ynab bo'lmaydi. Yosh hisobga olinmaydi.
- Darajalar `localStorage['kzl-<gameId>']` da — **profilga bog'lanmagan**: bir telefondagi ikki bola bitta darajani bo'lishadi. Native bundan bexabar: ota-ona paneli va oila boshqa qurilmalari darajani ko'rmaydi.
- Mukofot oynalari ustma-ust chiqadi ("+20 ⭐" va "Birinchi qadam +50" bir vaqtda — ikkinchisini o'qib bo'lmaydi).
- Kunlik vazifa bor (Faza 14), lekin bitta va faqat "o'yinni och-yop" — darajaga bog'liq emas.

## Qarorlar (foydalanuvchi bilan, 2026-10-29)

1. Birinchi bosqich — KZL ishlatadigan **32 ta o'yin**. Daraja tizimi yo'q 16 ta arkada o'yini (dino-run, snake, flappy-dino, …) keyingi fazada.
2. Qiyinlik **yoshga moslashuvchan** (2-4 / 5-7 / 8+) va ketma-ket 2 marta yiqilsa yengillashadi.
3. Jalb qilish: darajalar xaritasi + sovg'alar, kunlik 3 vazifa + seriya, ota-ona panelida darajalar, birga o'ynash rejimi — hammasi.

## Dizayn

### A. Cheksiz daraja yadrosi (`kz-ui.js`, KZL v3)

- `CONFIGS` jadvali o'rniga `KZL.cfgFor(level, age)` funksiyasi. Qaytaradi: `{n, q, c, hints, timer, pool, stars, tier}`. Eski `KZL.cfg(id)`, `KZL.CONFIGS[i]`, `slicePool(arr, level)` ishlashda davom etadi (orqaga moslik — 32 o'yinni birdaniga qayta yozmaymiz).
- Formula (daraja `n ≥ 1`, cheksiz):
  - `q` (savollar): `5 + floor((n-1)/2)`, yuqori chegara 15. 20 dan keyin savol soni o'smaydi — qiyinlik boshqa o'qlardan keladi.
  - `c` (variantlar): 2 → 3 (3-daraja) → 4 (6-daraja); 8+ yoshda 25-darajadan 5, 40-darajadan 6 (o'yin qo'llasa, `KZL.maxChoices` bilan cheklanadi).
  - `pool`: `min(1, 0.2 + 0.04·(n-1))` — 20-darajada butun hovuz.
  - `hints`: 3 → 0 ga kamayadi; **2-4 yosh hech qachon 1 dan kam emas**.
  - `timer` (soniya/savol, 0 = yo'q):
    - 2-4: hech qachon taymer yo'q.
    - 5-7: 8-darajadan boshlanadi, 40 s dan pastga, **eng kami 15 s**.
    - 8+: 5-darajadan, 60 s dan pastga, eng kami 8 s.
  - 20+ dan keyin "qiyinlik o'qi" (`tier = floor((n-1)/10)`) — o'yinlar ixtiyoriy ishlatadi: aralash savollar, teskari savol ("Mushuk qaysi?" ↔ "Bu kim?"), chalg'ituvchi o'xshash variantlar. Yadro faqat `tier` ni beradi.
- **Moslashuv**: `KZL.recordResult(id, passed)`. Ketma-ket 2 yiqilish → shu daraja uchun `ease = 1` (taymer +50 %, variant −1, hint +1). O'tilganda `ease` nolga qaytadi. Daraja tushmaydi — bola orqaga ketganini sezmaydi.
- **Profil**: kalitlar `kzl-<profileId>-<gameId>`. `profileId` — native faol profil ID'si (WebView URL'iga `?profile=` qo'shiladi; yo'q bo'lsa eski `kz-active-profile` → `p1`). Birinchi o'qishda eski `kzl-<gameId>` qiymati yangi kalitga ko'chiriladi (bir marta, faqat birinchi profil uchun).
- **Native'ga xabar**: daraja oshganda `AndroidBridge.levelReached(gameId, level)` (bo'lmasa jim). Native — manba-haqiqat, WebView `localStorage` faqat kesh.
- 13 o'yindagi `< 20` / `<= 20` olib tashlanadi; "Barcha darajalar" panjarasi — joriy darajaga yaqin oyna (oxirgi 12 ta + keyingi qulf).

### B. Darajalar xaritasi va sovg'alar

- `KZL.showMap(id)` — o'yinning start ekranidagi "📊 Darajalar" tugmasi xaritani ochadi: ilon-izi yo'l, o'tilgan tugunlar ⭐, joriy tugun pulsatsiyada, keyingi 5 tasi qulf. Har 5-daraja — sovg'a qutisi 🎁, har 10-daraja — qal'a 🏰.
- Sovg'a: **stiker** (har o'yinning o'z mavzusidagi emoji to'plami, cheksiz — ro'yxat tugasa kamyob "oltin" variantlar). Har 10-darajada katta bayram (konfetti + ovoz + "+100 ⭐").
- Stikerlar albomi — bosh sahifadagi 🏅 tugmasi ichida yangi "Stikerlar" bo'limi.
- **Mukofot navbati**: `KZL.queueReward(fn)` — level-up, nishon (badge), stiker oynalari ketma-ket ko'rsatiladi, bir vaqtda emas (topilgan xatoni tuzatadi). `main.js` badge popup ham shu navbatdan o'tadi.

### C. Kunlik 3 vazifa + seriya

- Faza 14 `daily_challenge` kengaytiriladi: kuniga 3 vazifa (`slot` 0..2):
  1. "{o'yin}ni o'yna" (hozirgi),
  2. "Istalgan o'yinda yangi darajaga chiq",
  3. "Bugun N ⭐ yig'" (N yoshga qarab: 30 / 50 / 80).
- Seriya (`streak`) faqat **uchala** vazifa bajarilganda emas, **kamida bittasi** bajarilganda oshadi (kichik bola uchun adolatli); 3/3 — kunlik "toj" 👑 (hisoblanadi, ota-onaga ko'rsatiladi).
- Bosh sahifada mavjud vazifa kartasi 3 qatorli ro'yxatga aylanadi.
- Room migratsiyasi: `daily_challenge` ga `slot`, `kind`, `target`, `progress` ustunlari; PK `"$profileId-$date-$slot"`. Eski qatorlar `slot = 0`.

### D. Ota-ona panelida darajalar

- Room: yangi `game_progress(profileId, gameId, level, bestLevelAt, updatedAt)`.
- `levelReached` → upsert (faqat oshsa) → `FirestoreSync` → `users/{owner}/profiles/{pid}/progress/{gameId}` (LWW `updatedAt`, Faza 16 qoidalari bilan; oila a'zolari o'qiy oladi).
- Panelda yangi "📈 Darajalar" kartasi: har o'yin uchun daraja, so'nggi 7 kunda nechta daraja oshgani, ko'nikma bo'yicha guruhlash (Ta'lim / Matematika / Ranglar / Mantiq — `content.json` dagi `cat`).
- Firestore qoidalari: `progress` hujjati — faqat egasi/oila a'zosi yozadi, `level` butun son, `1 ≤ level ≤ 10000`.

### E. Birga o'ynash

- **Navbatma-navbat** (savolli o'yinlar, KZL ishlatadiganlar): start ekranida "👨‍👧 Birga o'ynash" tugmasi. Ikki o'yinchi (bola avatari va "Ota-ona"), savollar navbat bilan, har kimning hisobi alohida, oxirida "G'olib" ekrani — ota-onaga savollar **bir daraja qiyinroq** (`cfgFor(level+3)`) bo'ladi, shunda bellashuv teng. Bu rejim darajani oshirmaydi, lekin yulduz beradi.
- **Ota-ona chaqiruvi**: panelda "Chaqiruv yuborish" — o'yin va maqsad daraja tanlanadi. Bosh sahifada bolaga karta: "Dadang chaqiruv yubordi: Hayvonlarda 12-darajaga chiq! 🎯". Bajarilganda ota-onaga (FCM orqali, oila qurilmalariga) "Ali chaqiruvni bajardi!" bildirishnomasi. Saqlanadi: `users/{owner}/profiles/{pid}/challenges/{id}`.

## Bosqichlar (har biri alohida PR)

1. **A** — yadro + 13 o'yindagi chegaralarni olib tashlash + profil kalitlari + `levelReached` bridge + mukofot navbati. Unit testlar (JS uchun Node'da `cfgFor` testi, Kotlin uchun bridge).
2. **B** — xarita, stikerlar, albom.
3. **D** — `game_progress`, sync, qoidalar + testlar, panel kartasi.
4. **C** — 3 vazifa, migratsiya, karta.
5. **E** — navbatma-navbat rejimi, chaqiruvlar, FCM.

D C dan oldin: C ning 2-vazifasi `game_progress` ga tayanadi.

## Tekshiruv

- `cfgFor` uchun jadval testi: 2-4 yoshda hech qachon taymer yo'q; 5-7 da taymer ≥ 15; `n = 1..500` da monotonlik (qiyinlik kamaymaydi, `ease` dan tashqari).
- Real telefonda (USB): Hayvonlar 1 → 22-darajaga `setLevel` bilan sakrab, 21-daraja ochilishi, "Keyingi" tugmasi 20 dan keyin ham chiqishi; ikki profil bilan darajalar alohidaligi.
- Mukofot navbati: level-up + badge bir vaqtda — ikkalasi ketma-ket ko'rinadi.

## Amalga oshirishdagi o'zgarishlar (2026-10-29)

- E (birga o'ynash): ota-onaga qiyinroq savol o'rniga **yordam (💡) berilmaydi** — har o'yinni qayta yozmaslik uchun. Rejim 10 ta savolli o'yinda (`KZL.duo`, `?duo=1`).
- E (chaqiruv): hozircha **bitta qurilmada** — Room `parent_challenge` (migratsiya 6→7), bildirishnoma va oila qurilmalariga sinxron yo'q (backend kerak, keyingi qadam).
- D: panel darajalarni shu qurilmadagi Room'dan oladi; boshqa oila qurilmasidagi `progress` hujjatlarini o'qish keyingi qadam.
- C: kunlik vazifa kartasi ilgari umuman ko'rinmas edi (`#challenge-bar` yashirilgan) — endi `#tasks-card`.

## Doiradan tashqari

- 16 ta arkada o'yiniga daraja (keyingi faza).
- Onlayn (ikki qurilmada) bellashuv.
- Stikerlarni sotib olish / Premium bilan bog'lash.
