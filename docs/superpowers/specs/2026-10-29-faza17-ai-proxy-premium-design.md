# Faza 17 — AI proksi, kunlik bepul limit va oila bo'ylab Premium

## Muammo (2026-10-29 da topildi)

- Ilova `/ai/story`, `/ai/ask`, `/ai/tts` ni chaqiradi, lekin backendda bu endpointlar **yo'q**. Har bir so'rov xato bilan tugaydi va ilova jimgina oflayn dvigatellarga (`PersonalizedStoryEngine`, `OfflineKidzoBrain`) o'tadi.
- Premium hech narsani yopmaydi: `RevenueCatManager.isPremium()` hech qayerda chaqirilmaydi. Bepul foydalanuvchiga kuniga 1 ta ertak limiti `7e00f50` (2026-09-03) da `KidzoSheet` qayta yozilganda tushib qolgan. Paywall esa "cheksiz AI ertaklar" ni sotmoqda.

## Qarorlar

1. Limit **backendda** tekshiriladi (ilova ma'lumotini tozalab chetlab o'tib bo'lmaydi).
2. AI kaliti hozircha yo'q: backend `AiProvider` interfeysi orqali ishlaydi. Kalit yo'q bo'lsa endpoint **503** qaytaradi va ilova hozirgidek oflayn javob beradi. Gemini (yoki boshqa) provayder kalit paydo bo'lganda qo'shiladi.
3. Premium holatini backend **RevenueCat webhook** orqali biladi (REST API emas): har so'rovda RevenueCat'ga murojaat yo'q, oila egasining Premium'ini tekshirish oson.
4. `/ai/tts` bu bosqichga kirmaydi — provayder yo'q.

## Dizayn (backend)

### Endpointlar

- `POST /ai/story` `{lang, ageRange, childName, scenario}` → `{title, text}`
- `POST /ai/ask` `{query, lang, childName, ageRange}` → `{answer}`
  - Faqat imzolangan ota-ona hisobi (mehmon → 403). Ilovada AI baribir `AccountFeature.AI_COMPANION` orqali yopilgan.
  - `lang` ∈ uz/ru/en; `childName` ≤ 30, `scenario` ≤ 200, `query` ≤ 500 belgi.
- `POST /revenuecat/webhook` — `Authorization` sarlavhasi `REVENUECAT_WEBHOOK_AUTH` ga teng bo'lishi shart (doimiy vaqtda solishtiriladi). Sozlanmagan bo'lsa 503.

### Limit

| | Ertak / kun | Savol / kun |
|---|---|---|
| Bepul | 1 | 10 |
| Premium | 30 | 200 (suiiste'mol chegarasi) |

- Kun — Toshkent vaqti bilan (`ClockConfig.BUSINESS_ZONE`), soat serverniki.
- Hisob **samarali egasi** bo'yicha yuritiladi (`familyOwner ?: uid`): profillar umumiy bo'lgani uchun oila bitta limitni bo'lishadi.
- Saqlanadi: `users/{owner}/ai_usage/{yyyy-MM-dd}` `{stories, questions}` — tranzaksiya bilan; provayder xato bersa (jumladan 503) hisob qaytariladi.
- Limit tugasa: bepul → **402 Payment Required** (ilova paywall ko'rsatadi), Premium → **429**.

### Premium

- `entitlements/{uid}` `{premiumUntil, eventAtMs}` — faqat backend yozadi (qoidalarda bu yo'l yo'q → mijozga yopiq).
- Hodisalar (`entitlement_ids` ichida `premium` bo'lsa):
  - `INITIAL_PURCHASE`, `RENEWAL`, `PRODUCT_CHANGE`, `UNCANCELLATION`, `NON_RENEWING_PURCHASE`, `SUBSCRIPTION_EXTENDED`, `TEMPORARY_ENTITLEMENT_GRANT` → `premiumUntil = expiration_at_ms` (yo'q bo'lsa — umrbod);
  - `EXPIRATION` → `premiumUntil = expiration_at_ms ?: hozir`;
  - `CANCELLATION`, `BILLING_ISSUE`, `TEST`, boshqalar → o'zgarishsiz (obuna muddat tugaguncha faol).
  - `app_user_id` `$RCAnonymousID:` bilan boshlansa — e'tiborsiz (ilova kirishda `Purchases.logIn(uid)` qiladi).
  - Eskirgan hodisa (`event_timestamp_ms` saqlangandan kichik) e'tiborsiz — RevenueCat tartibni kafolatlamaydi.
- `isPremium(uid)` = o'zining yoki oila egasining `premiumUntil > hozir`.

## Keyingi qadam (ilova, alohida)

- 402 da paywall'ni ko'rsatish, hozirgi oflayn fallback'ni 503/tarmoq xatosi uchun qoldirish.
- Paywall matni haqiqatga mos bo'lishi: "kuniga 1 ta bepul ertak, Premium — ko'proq".
- Oila a'zosi uchun "Premium oila egasidan" yozuvi.

## Test

- `AiServiceTest`: limit, oila bitta limitni bo'lishadi, Premium (o'zi / egasi), kun almashishi, provayder xatosida hisob qaytariladi, mehmon rad etiladi.
- `RevenueCatWebhookServiceTest`: har bir hodisa turi, anonim id, eskirgan hodisa.
- Controller testlari: status kodlari (402/429/503/403/400/401), webhook avtorizatsiyasi.
