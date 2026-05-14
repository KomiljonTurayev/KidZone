# Kidzo AI Agent — Dizayn Spesifikatsiyasi

**Sana:** 2026-05-14
**Loyiha:** KidZone — Bolalar uchun ertaklar, qo'shiqlar va mini-o'yinlar
**Fazalar:** Java Prototip (Phase 1) → Kotlin/Compose Migratsiya (Phase 2)

---

## 1. Maqsad

KidZone ilovasiga `Kidzo` nomli AI agent qo'shish. Kidzo bolalar uchun personalizatsiyalangan kontent tavsiya qiladi va ular bilan mehribon tilda muloqot qiladi. Gemini 1.5 Flash orqali ishlaydi, kontent esa mavjud `content.json` (20 ertak + 20 qo'shiq) dan olinadi.

---

## 2. Asosiy Qarorlar

| Parametr | Tanlov | Sabab |
|----------|--------|-------|
| Muloqot uslubi | Gibrid (tavsiya kartalar + ixtiyoriy chat) | Bolalar uchun qulay (bir bosish), lekin chat ham mavjud |
| Xotira | Lokal — SharedPreferences + Room (keyinchalik) | Internet kerak emas, GDPR muammosi yo'q |
| Kirish nuqtasi | FAB → Full-screen BottomSheet | Hozirgi `btnBirdAi` patternga mos, tez implement |
| Kontent bilimdoni | Lokal filter → Gemini'ga top-5 | Prompt kichik (~400 token), aniq, xarajat past |
| Arxitektura | State Machine (Java enum → Kotlin StateFlow) | Race condition yo'q, 1:1 migratsiya kafolati |
| Implementatsiya | Java prototip → Kotlin/Compose migratsiya | Tez yetkazish + to'g'ri arxitektura |

---

## 3. State Machine

```
IDLE → THINKING → RECOMMENDATIONS → CHATTING → THINKING → ...
                ↘ ERROR
```

| State | Holat | UI |
|-------|-------|----|
| `IDLE` | FAB ko'rinadi, agent tinch | Faqat FAB |
| `THINKING` | Gemini API kutilmoqda | BottomSheet + ProgressBar + "Kidzo o'ylamoqda..." |
| `RECOMMENDATIONS` | Kontent kartalar tayyor | 3 ta karta (emoji + nom + ▶ tugma) |
| `CHATTING` | Multi-turn suhbet rejimi | Chat UI + input maydoni + TTS |
| `ERROR` | Tarmoq yoki API xatosi | Xabar + Retry yoki Close |

**O'tish qoidalari:**
- `IDLE → THINKING`: FAB bosilganda yoki chat xabari yuborganda
- `THINKING → RECOMMENDATIONS`: Gemini tavsiya javobi kelganda
- `THINKING → CHATTING`: Gemini chat javobi kelganda
- `THINKING → ERROR`: Tarmoq xatosi, API xatosi, timeout
- `RECOMMENDATIONS → CHATTING`: "Kidzo bilan gaplash" tugmasi bosilganda
- `CHATTING → THINKING`: Yangi xabar yuborganda
- `Any → IDLE`: BottomSheet yopilganda yoki `dismiss()` chaqirilganda

---

## 4. Komponentlar

### 4.1 KidzoState.java (enum)

```java
public enum KidzoState {
    IDLE, THINKING, RECOMMENDATIONS, CHATTING, ERROR
}
```

Kotlin migratsiyasida `sealed class KidzoState`ga aylanadi.

### 4.2 KidzoStateListener.java (interface)

```java
public interface KidzoStateListener {
    void onStateChanged(KidzoState newState, Object payload);
    void onActionRequested(String contentId);
}
```

- `payload`: `RECOMMENDATIONS` holatida `List<ContentCard>`, `CHATTING`da `String` (Gemini javobi), `ERROR`da `String` (xabar)
- Kotlin migratsiyasida `StateFlow<KidzoUiState>` bilan almashtiriladi

### 4.3 KidzoProfile.java

SharedPreferences wrapper. Faqat quyidagi maydonlar:

| Maydon | Tur | Default |
|--------|-----|---------|
| `childName` | String | `"Bolam"` |
| `lastContentId` | String | `null` |
| `favoriteCategory` | String | `"all"` |

Kotlin migratsiyasida Room `Entity` + `DataStore`ga o'tadi.

### 4.4 ContentFilter.java

`assets/www/content.json`ni Java `AssetManager` orqali bir marta o'qib cache qiladi. Ikki asosiy metod:

```java
List<ContentItem> getTop5()
// Oxirgi kategori va oxirgi content ID'ni hisobga olib top-5 qaytaradi

List<ContentItem> getFiltered(String userMessage)
// userMessage'dan kalit so'z ajratib, title va category bo'yicha filter qiladi
// Natija: ≤5 ta ContentItem (id, title[lang], emoji, category)
```

WebView allaqachon `content.json`ni yuklagani uchun ContentFilter uni mustaqil yuklamaydi — `AssetManager` orqali parallel o'qiydi.

### 4.5 ActionParser.java

```java
// Pattern: \[OPEN:([a-z0-9\-]+)\]
// "Salom! [OPEN:story-003] bu qiziqarli ertak!" → "story-003"
// Mos kelmasa → null qaytaradi, exception yo'q

@Nullable
public static String parse(String geminiResponse)
```

### 4.6 KidzoAgent.java (bosh orkestrator)

```java
public class KidzoAgent {
    private KidzoState currentState = KidzoState.IDLE;
    private final KidzoProfile profile;
    private final ContentFilter contentFilter;
    private final GenerativeModelFutures geminiModel;
    private final TextToSpeech tts;
    private final Handler mainHandler;
    private KidzoStateListener listener;

    public void requestRecommendations()  // IDLE → THINKING → RECOMMENDATIONS
    public void sendChatMessage(String msg) // → THINKING → CHATTING
    public void dismiss()                  // Any → IDLE, TTS to'xtatiladi
    public void setListener(KidzoStateListener l)
}
```

`setState()` metodi `private` — faqat KidzoAgent ichida chaqiriladi (single source of truth).

### 4.7 KidzoBottomSheet.java (UI)

`BottomSheetDialogFragment`. `KidzoStateListener`ni implement qiladi. State'ga qarab uchta `ViewStub`dan birini ko'rsatadi:

- `THINKING` → `stub_thinking` (ProgressBar + "Kidzo o'ylamoqda...")
- `RECOMMENDATIONS` → `stub_recommendations` (RecyclerView, `KidzoCardAdapter`)
- `CHATTING` → `stub_chat` (ScrollView + EditText + Send tugma)
- `ERROR` → `stub_error` (xabar matni + Retry/Close)

---

## 5. Gemini Prompt Shablonlari

### 5.1 Tavsiya Prompt (~400 token)

```
Sen KidZone ilovasidagi "Kidzo" nomli mehribon qushchasan.
Faqat O'zbek tilida, qisqa va bolalarga mos tarzda gaplash.
Bolaning ismi: {profile.childName}.
Oxirgi eshitgan kontenti: {profile.lastContentId}.

Quyidagi kontentlardan {childName} uchun 3 ta mos tavsiya tanlaydi:
{contentFilter.getTop5()}
// Har qator formati: "id|emoji|nom|kategoriya"
// Misol: "story-003|🐢|Toshbaqa va Quyon|animals"

Har bir tavsiyani quyidagi formatda yoz:
[OPEN:content-id] Kontent nomi — qisqa tavsif

Boshqa format ishlatma. Faqat ro'yxatdagi ID'larni ishlat.
```

### 5.2 Chat Prompt (~300 token + history)

```
Sen KidZone ilovasidagi "Kidzo" qushchasan. Juda mehribon, oddiy gaplashasan.
Bolaning ismi: {profile.childName}. Unga ism bilan murojaat qil.

Agar bola kontent so'rasa — faqat quyidagi ro'yxatdan tanlaydi:
{contentFilter.getFiltered(userMessage)}

Ochish uchun: [OPEN:content-id] formatini ishlat.
Agar mos kontent yo'q — shunchaki gapir, [OPEN:none] dema.
Javob 2-3 gapdan oshmasin.
```

**Multi-turn:** `Content.Builder`ga `Content.Role.USER` va `Content.Role.MODEL` navbatlashib qo'shiladi. Tarix maksimal 10 turn bilan chegaralanadi (token overflow oldini olish).

---

## 6. [ACTION] Tag Parsing va Kontent Ochish

```
Gemini javobi → ActionParser.parse() → contentId (yoki null)
                                             ↓
                              KidzoStateListener.onActionRequested(contentId)
                                             ↓
                              MainActivity → WebView JS bridge
                                             ↓
                              openContent("story-003") | openContent("song-001")
```

`contentId` `"story-"` bilan boshlansa → Stories tabiga, `"song-"` bilan boshlansa → Songs tabiga yo'naltiriladi. Noaniq holatlarda `content.json`dan ID qidirilib tur aniqlanadi.

---

## 7. Xato Boshqaruvi

| Xato | State | UI | Fallback |
|------|-------|----|----------|
| Tarmoq xatosi (IOException) | `THINKING → ERROR` | "Internet yo'q. Qaytadan urining 🐥" + Retry | — |
| Gemini API xatosi (429, 500) | `THINKING → ERROR` | "Kidzo biroz charchadi 😴 Keyinroq urinib ko'ring" + Close | — |
| Bo'sh javob (`""` yoki `null`) | `THINKING → RECOMMENDATIONS` | Xabar yo'q | `ContentFilter.getTop5()` natijasi to'g'ridan-to'g'ri kartalar sifatida |
| API kalit xatosi (401) | `THINKING → ERROR` | Log yoziladi, foydalanuvchiga texnik xabar emas | Fallback kartalar |
| TTS ishga tushmasa | Faqat log | TTS belgisi yo'qoladi, matn ko'rinadi | Matnli javob |

---

## 8. API Kalit Xavfsizligi

**Java prototip:** `local.properties`da `GEMINI_API_KEY=...`. `build.gradle`da `BuildConfig.GEMINI_KEY`ga ko'chiriladi. APK reverse-engineer qilinsa ko'rinishi mumkin — prototip uchun qabul qilinadi.

**Kotlin production (Phase 2 bilan):** API kalit backend proxy orqali (Firebase Function yoki minimal server). Ilova hech qachon kalitni o'zi saqlamaydi. Bu Phase 2 (Auth tizimi) bilan birgalikda amalga oshiriladi.

---

## 9. Yangi Fayllar

```
app/src/main/java/uz/kidzone/app/
├── MainActivity.java                    [mod] setupBirdAi() → setupKidzo()
└── kidzo/                               [new package]
    ├── KidzoAgent.java
    ├── KidzoState.java
    ├── KidzoStateListener.java
    ├── KidzoProfile.java
    ├── ContentFilter.java
    └── ActionParser.java

app/src/main/res/layout/
├── bottom_sheet_kidzo.xml               [new]
└── item_kidzo_card.xml                  [new]

app/src/test/java/uz/kidzone/app/kidzo/  [new]
    ├── ContentFilterTest.java
    ├── ActionParserTest.java
    ├── KidzoProfileTest.java
    └── KidzoAgentStateTest.java
```

`BirdAiManager.java` o'zgartirilmaydi — Java prototip davomida parallel yashaydi. Kidzo to'liq ishlagan vaqtda deprecated qilinadi.

---

## 10. Yetkazish Bosqichlari

| Bosqich | Tarkib | Taxminiy vaqt |
|---------|--------|---------------|
| Java P1 | KidzoState + KidzoAgent yadro + ContentFilter + ActionParser + testlar. FAB → BottomSheet THINKING ishlaydi. | ~1-2 kun |
| Java P2 | KidzoProfile (SharedPrefs) + Gemini tavsiya prompt. RECOMMENDATIONS holati, real kartalar, `[OPEN:id]` ishlaydi. | ~1 kun |
| Java P3 | Chat rejimi + multi-turn + TTS. CHATTING holati, ovoz bilan o'qish. | ~1 kun |
| Kotlin | KidzoAgent → KidzoViewModel + StateFlow. BottomSheet → Composable. 1:1 migratsiya. | ~2-3 kun |

---

## 11. Kotlin Migratsiya Xaritasi (1:1)

| Java (Prototip) | Kotlin + Compose |
|-----------------|-----------------|
| `KidzoState` (enum) | `sealed class KidzoState` |
| `KidzoStateListener` (interface callback) | `StateFlow<KidzoUiState>` in `KidzoViewModel` |
| `Handler(Looper.getMainLooper())` | `viewModelScope.launch { _state.emit(...) }` |
| `KidzoBottomSheet` (Fragment) | `KidzoScreen` (Composable) |
| `KidzoProfile` (SharedPrefs) | `KidzoRepository` (Room + DataStore) |
| `ContentFilter` (utility class) | `GetRecommendationsUseCase` (Domain layer) |
| `Executors.newSingleThreadExecutor()` | `Dispatchers.IO` coroutine |

---

## 12. Asosiy Prinsiplar

1. **Single source of truth:** `KidzoState` faqat `KidzoAgent` ichida o'zgaradi. UI faqat listener orqali xabar oladi.
2. **Gemini tejamli:** `ContentFilter` avval ishlaydi — mos kontent yo'q bo'lsa Gemini chaqirilmaydi. Har chaqiruv logga yoziladi.
3. **Parallel yashash:** `BirdAiManager` Java prototip davomida o'chirilmaydi. Kidzo tayyor bo'lganda almashtiriladi.
4. **content.json bir marta:** `ContentFilter` `AssetManager` orqali o'qiydi — WebView yuklamasiga bog'liq emas.
