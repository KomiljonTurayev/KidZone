# KidAI Chat Assistant — RAG Design Spec
**Date:** 2026-06-05
**Status:** Approved

## Overview

KidZone ilovasiga offline ishlaydigan bolalar AI yordamchisi qo'shiladi. U uchta funksiyani bajaradi: savollarga javob berish, interaktiv ertak aytish va o'yinlarga yo'naltirish. Barcha mantiq `kidai.js` da joylashadi va Intent detection + keyword routing + templates yondashuvi ishlatiladi — hech qanday tashqi API yoki model yuklanmaydi.

`generateAiStory()` ham shu engine orqali yangilanadi: hardcoded 3 ta ertak o'rniga `content.json` dagi 30+ ertak RAG orqali topiladi.

---

## Architecture

### Files

| Fayl | O'zgarish |
|------|-----------|
| `app/src/main/assets/www/kidai.js` | **Yangi** — KidAIEngine class, RAG, templates |
| `app/src/main/assets/www/index.html` | KidAI tab tugmasi, `#kidai-panel` chat UI, script tag |
| `app/src/main/assets/www/main.js` | `generateAiStory()` → `window.kidAI.retrieveStory()` ishlatadi |

### Data Flow

```
User input
  ↓
KidAIEngine.respond(text, lang, age)
  ↓
detectIntent(text, lang)  →  intent: greeting | story | game | fact | fallback
  ↓
RAG retrieval (intent ga qarab):
  story   → content.json  → category + age filter → best match
  game    → GAMES array   → cat/name/tag match    → top result
  fact    → fact templates → keyword match        → template fill
  ↓
buildResponse(intent, retrieved, lang)  →  { type, text, action? }
  ↓
Chat UI render
```

---

## KidAI Engine (`kidai.js`)

### Class: `KidAIEngine`

```js
class KidAIEngine {
  constructor(stories, games)   // content.json stories + GAMES array
  respond(text, lang)           // main entry point → ResponseObject
  detectIntent(text, lang)      // returns intent string
  retrieveStory(lang, topic)    // RAG story fetch
  retrieveGame(text, lang)      // RAG game fetch
  retrieveFact(text, lang)      // fact template fetch
}
```

**ResponseObject:**
```js
{
  type: 'greeting' | 'story' | 'game' | 'fact' | 'fallback',
  text: string,           // display text
  storyId?: string,       // content.json story id (RAG result)
  gameId?: string,        // GAMES id → openGame() uchun
  chips?: string[]        // quick reply chips
}
```

### Intent Detection

Kalit so'z ro'yxatlari (3 til):

| Intent | UZ | RU | EN |
|--------|----|----|-----|
| `greeting` | salom, assalom, xayr, yaxshimisan | привет, здравствуй, пока | hello, hi, bye, how are you |
| `story` | ertak, hikoya, ayt, aytib ber, o'qi | сказку, сказка, расскажи, почитай | story, tale, tell, read |
| `game` | o'yin, o'yna, ko'rsat, qaysi | игру, игра, играть, покажи | game, play, show, which |
| `fact` | nima, nega, qanday, qancha, kim | что, почему, как, сколько, кто | what, why, how, who, when |

Algorit: `text.toLowerCase()` → tokenize → kalit so'z bilan kesishish → eng ko'p mos kelgan intent. Tenglashsa — `fact` ustunlik qiladi.

### RAG Retrieval

**Story retrieval:**
- Mavzu kalit so'zlari: `hayvon/animal/животное → animals`, `koinot/space/космос → space`, `oila/family/семья → family`, `tabiat/nature/природа → nature`, `qahramon/hero/герой → heroes`
- `content.json` stories → `category` field filter (animals, space, family, nature, heroes)
- Bir nechta mos kelsa → random bittasi
- Hech biri mos kelmasa → butun to'plamdan random

**Game retrieval:**
- Kalit so'zlar: `math/raqam/число → cat:math`, `rang/color/цвет → cat:colors`, `musiqa/music → cat:music`, `puzzle/bosh qotirma → cat:puzzle`, `sport → cat:sport`, `tabiat/nature → cat:nature`
- `GAMES` array → `cat` yoki `name[lang]` da qidirish
- Topilgan o'yindan: emoji, name, file → `openGame()` action

**Fact retrieval:**
- Mavzular: hayvonlar, ranglar, sonlar, shakllar, sayyoralar, tana a'zolari, mevalar, transport
- Har mavzu uchun 3 tilda 3-5 ta template
- Foydalanuvchi matni ichida mavzu kalit so'zi topilsa → random template

### Response Templates

**Greeting (uz):** "Salom! 👋 Men KidAI — sening bilim do'sting! Ertak eshitmoqchimisan, o'yin topmoqchimisan yoki savol bermoqchimisan?"

**Story (uz):** "📖 [emoji] [title] ertagi! [text ning birinchi 100 ta belgisi]..."

**Game (uz):** "🎮 [emoji] [name] o'yinini sinab ko'r! [tag] — [age]+ yoshlar uchun."

**Fact (uz):** "[emoji] [fact text]"

**Fallback (uz):** "🤔 Tushunmadim, lekin yordam bera olaman! Ertak, o'yin yoki savol — nima kerak?"

---

## Chat UI (`index.html`)

### Tab bar

Mavjud 2 ta tab (`O'yinlar`, `Ertaklar`) ga uchinchi tab qo'shiladi:

```html
<button class="kz-tab" id="tab-kidai" onclick="switchTab('kidai')">
  🤖 <span id="tab-lbl-kidai">KidAI</span>
</button>
```

### Chat Panel (`#kidai-section`)

```
┌─────────────────────────────┐
│ 🤖 KidAI            [✕ Tozala] │  header
├─────────────────────────────┤
│                             │
│  🤖  Salom! 👋 Men KidAI...│  bot xabari (chap, oq)
│                             │
│            Salom!  🧑       │  user xabari (o'ng, accent)
│                             │
│  🤖  📖 Sehrli O'rmon...   │
│     [📖 O'qi] [🔄 Boshqa]  │  action chips
│                             │
├─────────────────────────────┤
│ [📖 Ertak] [🎮 O'yin] [❓ Savol] │  quick reply chips
├─────────────────────────────┤
│ ✏️ Yoz...         [➤]      │  input row
└─────────────────────────────┘
```

**Styling:**
- Bot xabari: `background: var(--surface)`, border-radius, chap tomoni yassi
- User xabari: `background: var(--accent)`, oq matn, o'ng tomoni yassi
- Typing indicator: `...` animatsiyali 3 nuqta, 600ms keyin javob keladi
- `#kidai-messages` — `overflow-y: auto`, `flex-direction: column`, yangi xabar pastga scroll

### Translation keys (3 til)
`tabKidai`, `kidaiGreeting`, `kidaiPlaceholder`, `kidaiClear` — mavjud `T` obyektiga qo'shiladi.

---

## `generateAiStory()` Update (`main.js`)

```js
// Oldin:
const story = this.getAiStory(this.translator.lang);  // hardcoded 3 ta

// Keyin:
const story = window.kidAI
  ? window.kidAI.retrieveStory(this.translator.lang, null)
  : this.getAiStory(this.translator.lang);  // fallback
```

`getAiStory()` metodi saqlanib qoladi fallback sifatida, lekin asosiy yo'l `KidAIEngine.retrieveStory()`.

---

## Edge Cases

- `content.json` yuklanmagan payt `retrieveStory()` chaqirilsa → `getAiStory()` fallback ishlatiladi
- Foydalanuvchi bo'sh satr yuborsа → input ignore qilinadi
- Juda uzun input (500+ belgi) → birinchi 200 belgisi ishlatiladi
- Til o'zgarsa → suhbat tarixi tozalanmaydi, faqat keyingi javoblar yangi tilda bo'ladi

---

## Out of Scope

- Server yoki API chaqiruvi
- Suhbat tarixini saqlash (localStorage ga yozilmaydi)
- Ovozli input (mikrofon)
- Ko'p navbatli suhbat konteksti (har xabar mustaqil)
