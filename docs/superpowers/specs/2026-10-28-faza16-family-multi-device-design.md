# Faza 16 — Ko'p qurilma va oila hisobi: Dizayn spesifikatsiyasi

**Sana:** 2026-10-28
**Holat:** Approved — A bosqichi `master`da (PR #14); B bosqichi qarorlari qabul qilindi
**Bog'liq:** `2026-06-28-faza11-multi-profile-design.md`, `2026-06-23-faza7-firebase-sync-design.md`

---

## Maqsad

Bir oila KidZone'ni bir nechta telefonda ishlatsa — masalan, onasining va otasining telefonida, yoki bolaning planshetida — profillar, vaqt limiti va ota-ona PIN'i hamma qurilmada bir xil bo'lsin, bir joyda qilingan o'zgarish boshqalariga o'zi yetib borsin. Ikkinchi ota-ona o'z Google/Telegram hisobi bilan oilaga qo'shila olsin.

Ish ikki bosqichga bo'linadi. **A bosqichi** mustaqil ravishda reliz qilinishi mumkin va B'ning asosi hisoblanadi.

| Bosqich | Nima | Kim uchun |
|---|---|---|
| **A** — Bitta hisob, ko'p qurilma | Mavjud sinxronlashni ishonchli qilish | Bir ota-ona, bir nechta qurilma |
| **B** — Oila a'zolari | Ikkinchi ota-onani taklif kodi orqali ulash | Ikki ota-ona |

---

## Hozirgi holat (kod bo'yicha tekshirildi)

Profillar `users/{uid}/profiles/{profileId}` da saqlanadi (`ProfileSyncManager`). Kirishda `ProfileCloudSyncListener` bir marta pull qiladi, keyin lokal profillarni push qiladi. Bu bilan bog'liq **mavjud xatolar**:

1. **Tahrirlar boshqa qurilmaga yetib bormaydi.** `ProfileDao.mergeProfiles` masofadagi profilni faqat `remote.createdAt > local.createdAt` bo'lsa yozadi. `createdAt` tahrirda o'zgarmaydi, shuning uchun A telefonda o'zgartirilgan ism, vaqt limiti yoki **PIN** B telefondagi mavjud profilga hech qachon tushmaydi.
2. **O'chirish sinxronlanmaydi.** `ProfileRepository.delete` faqat Room'dan o'chiradi; Firestore'dagi hujjat qoladi va keyingi kirishda profil qaytib keladi.
3. **Faqat kirishda sinxronlanadi.** Ilova ochiq turganda boshqa qurilmadagi o'zgarish ko'rinmaydi — faqat qayta kirilganda.
4. **PIN** (`ProfileViewModel.setParentPin`) har profilga yoziladi va push qilinadi, lekin 1-xato tufayli boshqa qurilmalarga tarqalmaydi.

Backend (`PlaytimeService.heartbeat(uid, profileId)`) vaqt limitini `users/{uid}/profiles/{profileId}.timeLimitMinutes` dan o'qiydi — ya'ni chaqiruvchining **o'z** `uid` i ostidan.

---

## A bosqichi — Bitta hisob, ko'p qurilma

### Qamrov

**Kiradi:**
- `ProfileEntity` ga `updatedAt: Long` va `deleted: Boolean` (Room migratsiyasi 3→4)
- Har bir lokal o'zgarishda `updatedAt` = joriy vaqt (`ProfileRepository` ga test uchun almashtiriladigan `nowMs: () -> Long` beriladi, default `System::currentTimeMillis`); Firestore'ga ham yoziladi
- `mergeProfiles` — `updatedAt` bo'yicha "oxirgi yozuv yutadi" (last-write-wins)
- O'chirish = "tombstone": `deleted = true` + `updatedAt`, Firestore'ga push; UI `deleted` larni ko'rsatmaydi
- Firestore snapshot listener: ota-ona hisobi bilan kirilgan va ilova ochiq turganda `users/{uid}/profiles` dagi o'zgarishlar darhol Room'ga merge qilinadi
- `firestore.rules`: `updatedAt` (int) va `deleted` (bool) maydonlariga ruxsat + tekshiruv; `firestore-tests` ga testlar

**Kirmaydi:**
- Avatar rasmlarini sinxronlash (avvalgidek faqat lokal)
- Statistika (`stats/`) va streak sinxronlash mantiqi — o'zgarmaydi
- Maydon darajasidagi birlashtirish (field-level merge) — butun profil bo'yicha last-write-wins yetarli: ota-ona bir vaqtda ikki telefonda bitta profilni tahrirlashi kam uchraydi

### Ma'lumotlar modeli

`ProfileEntity` ga qo'shiladi:

| Maydon | Tip | Default | Izoh |
|---|---|---|---|
| `updatedAt` | `Long` | `createdAt` (migratsiyada) | Oxirgi o'zgarish vaqti, ms |
| `deleted` | `Boolean` | `false` | Tombstone; UI va o'yin oqimi ko'rmaydi |

Firestore `profiles/{id}` hujjatiga xuddi shu ikki maydon qo'shiladi.

### Birlashtirish qoidasi

```
remote.updatedAt > local.updatedAt  →  masofadagini olamiz (avatarPath lokal qoladi)
aks holda                           →  lokal qoladi
lokal yo'q                          →  masofadagini qo'shamiz (deleted bo'lsa ham — tombstone saqlanadi)
```

Soat farqi xavfi: qurilma soatlari bir necha soniyaga farq qilishi mumkin. Ota-ona tahrirlari orasida odatda daqiqalar bo'lgani uchun bu qabul qilinadi; Firestore'ga `updatedAt` bilan birga `serverUpdatedAt: FieldValue.serverTimestamp()` ham yoziladi — kelajakda kerak bo'lsa, server vaqtiga o'tish oson bo'ladi.

### Tombstone'lar

- `ProfileRepository.delete` → `update(profile.copy(deleted = true, updatedAt = now))` + push.
- `ProfileDao.getAll()` / `count()` / faol profil tanlash — `WHERE deleted = 0`.
- Oxirgi tirik profilni o'chirib bo'lmaydi (mavjud qoida saqlanadi).
- Tombstone'lar 30 kundan keyin lokal tozalanadi; Firestore'dagisi qoladi (arzon, va eski qurilma qayta ulanganda profilni "tiriltirmaslik" uchun kerak).

### Real vaqtda tinglash

`ProfileCloudSyncListener` hisob `SignedIn` bo'lganda `addSnapshotListener` o'rnatadi, `SignedOut`/`Guest` da olib tashlaydi. Har snapshot → `mergeProfiles`. Lokal yozuvning o'zi qaytib kelganda (`hasPendingWrites`) e'tiborsiz qoldiriladi.

### Test

- `ProfileRepositoryTest` (mavjud `FakeProfileDao` orqali — `mergeProfiles` interfeysning default metodi): tahrir tarqaladi (yangi `updatedAt` yutadi, eskisi yutqazadi), tombstone tarqaladi va UI ro'yxatidan yo'qoladi, avatar lokal qoladi.
- Migratsiya 3→4: mavjud qatorlar `updatedAt = createdAt`, `deleted = 0`.
- `firestore.rules.test.js`: `updatedAt`/`deleted` qabul qilinadi, noto'g'ri tiplar rad etiladi.
- Qo'lda: ikki emulator, bitta Google hisob — ism/limit/PIN o'zgarishi va o'chirish ikkinchisida ilovani qayta ochmasdan ko'rinadi.

---

## B bosqichi — Oila a'zolari (ikkinchi ota-ona)

### Asosiy qaror: ma'lumot egasining `uid` i ostida qoladi

Oila ma'lumotlari ko'chirilmaydi: profillar avvalgidek **oila egasi**ning `users/{ownerUid}/profiles` ida qoladi. A'zolar unga ruxsat oladi. Muqobil — hammasini `families/{familyId}/…` ga ko'chirish — barcha mavjud foydalanuvchilar uchun migratsiya, backend playtime/streak yo'llarini va qoidalarni qayta yozishni talab qiladi; foydasi esa bu bosqichda yo'q.

### Qamrov

**Kiradi:**
- Firestore: `families/{ownerUid}` (egasi, yaratilgan vaqt) va `families/{ownerUid}/members/{memberUid}` (rol, qo'shilgan vaqt, ko'rinadigan ism)
- `users/{memberUid}.familyOwner = ownerUid` — a'zoning ilovasi qaysi oilaga qarashini biladi
- Taklif kodi: ega Ota-ona panelida "Oila a'zosini taklif qilish" → 6 xonali kod, 24 soat amal qiladi, bir marta ishlatiladi
- Backend endpointlari (Firebase Admin SDK, qoidalarni chetlab o'tadi):
  - `POST /family/invite` → `{code, expiresAt}` (faqat ega, imzolangan hisob)
  - `POST /family/join {code}` → kodni tekshiradi, a'zolik va `familyOwner` ni yozadi
  - `POST /family/leave`, `DELETE /family/members/{uid}` (ega a'zoni chiqaradi)
- `PlaytimeService.heartbeat`: `uid` o'rniga **samarali egasi** (`familyOwner ?: uid`) ostidan o'qiydi/yozadi
- `firestore.rules`: `users/{uid}/profiles/**` ga `isFamilyMember(uid)` bo'lsa o'qish/yozish (status va o'chirish emas)
- Ilova: sinxronlash, listener va push `effectiveOwnerUid` dan foydalanadi; Ota-ona panelida "Oila" bo'limi (a'zolar ro'yxati, taklif, chiqarish/tark etish)
- Oilaga qo'shilish **faqat imzolangan hisob bilan** va **PIN darvozasidan keyin** — bola kod kiritib oilaga kira olmaydi

**Kirmaydi:**
- Premium'ni oila bo'ylab ulashish — RevenueCat obunasi `uid` ga bog'langan; oila egasi Premium bo'lsa a'zo ham ko'rishi uchun backend entitlement tekshiruvi kerak. Alohida spec.
- Ikkidan ortiq ota-ona uchun rollar/huquqlar farqi — barcha a'zolar teng (ega faqat a'zo chiqara oladi va oilani o'chira oladi)
- Bir ota-onaning bir vaqtda bir nechta oilada bo'lishi

### Xavfsizlik

- A'zolikni **faqat backend** yozadi; qoidalar `families/**` ga mijoz yozuvini taqiqlaydi. Aks holda istalgan kishi o'zini birovning oilasiga yozib, bolalar ma'lumotini o'qiy olardi.
- Kod 6 xonali (10⁶ variant) bo'lgani uchun: urinishlar `uid` bo'yicha cheklanadi (masalan, soatiga 10 ta), kod 24 soatda va birinchi muvaffaqiyatli ishlatilganda o'chadi.
- `users/{ownerUid}` hujjatining o'zi (status, ban) a'zoga **ochilmaydi** — faqat `profiles/**`.
- Hisob o'chirilganda: ega o'chirsa — oila va a'zoliklar o'chadi, a'zolarning `familyOwner` i tozalanadi; a'zo o'chirsa — faqat a'zolik o'chadi.

### Test

- Backend: `FamilyServiceTest` — kod yaratish, muddati o'tgan/ishlatilgan kod, urinishlar cheklovi, egasi o'zini qo'sha olmaydi, chiqarish/tark etish; `PlaytimeServiceTest` — a'zo heartbeat'i egasi ostiga yoziladi.
- `firestore.rules.test.js`: a'zo profilni o'qiydi/yozadi, a'zo bo'lmagan o'qiy olmaydi, a'zo `users/{owner}` status maydoniga tega olmaydi, mijoz `families/**` ga yoza olmaydi.
- Ilova: `effectiveOwnerUid` hisob holatiga qarab to'g'ri tanlanishi (unit test).
- Qo'lda: ikki emulator, ikki xil Google hisob — taklif, qo'shilish, PIN/limit o'zgarishi ikki tomonda, a'zoni chiqarish.

---

## Qarorlar (2026-10-29)

1. **Premium oila bo'ylab** — B bosqichiga kirmaydi; alohida spec. B'da Premium har kimning o'z hisobida qoladi.
2. **Bola qurilmasi** — planshetda ota-ona o'z hisobi bilan kiradi. A bosqichi buni allaqachon qo'llab-quvvatlaydi; alohida "bola qurilmasi" roli yo'q.
3. **Taklif kanali** — faqat 6 xonali kod. Havola/deep link keyinroq, kerak bo'lsa.

---

## Reliz tartibi

1. A bosqichi (ilova + qoidalar) — mustaqil reliz.
2. B bosqichi backend (endpointlar + heartbeat o'zgarishi) — ilovadan oldin deploy qilinadi; eski ilova versiyalari `familyOwner` ni bilmaydi, lekin ular uchun hech narsa o'zgarmaydi.
3. B bosqichi qoidalar + ilova.

Batafsil vazifalar: `docs/superpowers/plans/2026-10-28-faza16-family-multi-device.md`.
