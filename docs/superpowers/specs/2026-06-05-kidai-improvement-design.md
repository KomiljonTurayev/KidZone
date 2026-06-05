# KidAI Improvement Design
**Date:** 2026-06-05
**Status:** Approved

## Problem

KidAI returns the same static fallback text for most user inputs. Two root causes:

1. **Intent detection is too narrow** — only triggers `fact` intent when user writes "nima/what/что" style question words. Natural inputs like "sher haqida ayt" or "kapalak" don't match any intent → fallback.
2. **Fact topic matching is too narrow** — even when `fact` intent triggers, topic keywords miss many common word forms and synonyms → generic hint shown instead of actual fact.

Result: both specific queries ("sher haqida") and general queries ("qiziq narsa ayt") hit the same static fallback message every time.

## Goal

Every input should produce a useful, varied response. The fallback message should never be the only answer a child sees repeatedly.

## Design

### Scope

Only `kidai.js` is modified. No changes to `index.html` or `main.js`.

---

### 1. New `respond()` flow — topic-first detection

**Current order:**
```
text → _detectIntent() → [greeting|story|game|fact|fallback]
```

**New order:**
```
text → _detectTopic()   → matched  → _fact()
            ↓ no match
       _detectIntent()  → greeting | story | game
            ↓ no match
       _randomFact()    → random interesting fact (never empty)
```

`_detectTopic()` is a new dedicated method that checks only the fact DB keywords. `_detectIntent()` is unchanged except its `fact` branch is removed (topic detection now owns that path).

---

### 2. New `_detectTopic(text)` method

Scans `_KAI_FACT_DB` keyword lists against the lowercased input. Returns the matching category key (e.g. `"hayvon"`) or `null`. First match wins.

```js
_detectTopic(text) {
  const lower = text.toLowerCase();
  for (const [cat, data] of Object.entries(_KAI_FACT_DB)) {
    if (data.kws.some(kw => lower.includes(kw))) return cat;
  }
  return null;
}
```

---

### 3. Keyword expansion

Each category's `kws` array is expanded ~3x. Examples:

| Category | Added keywords |
|---|---|
| hayvon | `it`, `kuchuk`, `baliq`, `qo'y`, `ot`, `dog`, `cat`, `fish`, `bird`, `кошка`, `собака`, `рыба`, `horse`, `sheep`, `wolf`, `bo'ri`, `волк` |
| rang | `oq`, `qora`, `white`, `black`, `purple`, `binafsha`, `pink`, `kulrang`, `grey`, `brown`, `jigarrang`, `orange`, `to'q sariq` |
| son | `ikki`, `uch`, `to'rt`, `besh`, `two`, `three`, `four`, `five`, `два`, `три`, `ayir`, `minus`, `ko'pay`, `multiply` |
| shakl | `yulduz`, `star`, `oval`, `kub`, `cube`, `silindr`, `cylinder`, `звезда`, `куб` |
| sayyora | `raketa`, `rocket`, `astronavt`, `astronaut`, `galaktika`, `galaxy`, `kosmoschi`, `ракета`, `астронавт`, `галактика` |
| meva | `tarvuz`, `watermelon`, `nok`, `pear`, `gilos`, `cherry`, `qovun`, `sabzavot`, `vegetable`, `арбуз`, `груша` |

Intent keywords are also expanded:

| Intent | Added keywords |
|---|---|
| story | `bitta`, `boshla`, `davom`, `eshitay`, `begin`, `once upon`, `начни`, `продолжи` |
| game | `qanday o'yin`, `nimani o'ynayman`, `show`, `launch`, `какую игру`, `запусти` |
| greeting | `qalaysan`, `nima gap`, `как дела`, `sup`, `yo`, `hey`, `привет` |
| fact | removed — topic detection owns this path; `fact` branch deleted from `_detectIntent()` |

---

### 4. `_randomFact(lang)` — new method

Called when neither topic nor intent matches. Returns a random fact from any category.

```js
_randomFact(lang) {
  const cats = Object.values(_KAI_FACT_DB);
  const cat = cats[Math.floor(Math.random() * cats.length)];
  const list = cat[lang] || cat.en;
  const text = list[Math.floor(Math.random() * list.length)];
  const CHIPS = {
    uz: ['❓ Yana', '📖 Ertak', "🎮 O'yin"],
    ru: ['❓ Ещё', '📖 Сказка', '🎮 Игра'],
    en: ['❓ More', '📖 Story', '🎮 Game']
  };
  return { type: 'fact', text, chips: CHIPS[lang] || CHIPS.en };
}
```

---

### 5. Varied `_fallback()` — 5 random variants

Replace the single hardcoded string with a pool of 5 per language. Selected randomly.

```js
_fallback(lang) {
  const pool = {
    uz: [
      "🤔 Tushunmadim, lekin yordam bera olaman! Ertak, o'yin yoki savol?",
      "🌟 Qiziq so'rov! Menga ertak yoki o'yin haqida so'ra.",
      "💡 Bilmayman, lekin ko'p narsani bilaman! Hayvonlar, sayyoralar, ranglar...",
      "🔍 Izladim, topolmadim. Boshqacha yozsang-chi?",
      "😊 Keling, birga o'rganamiz! Nima haqida bilmoqchisan?"
    ],
    ru: [
      '🤔 Не понял, но помогу! Сказка, игра или вопрос?',
      '🌟 Интересный запрос! Спроси про сказку или игру.',
      '💡 Не знаю этого, но знаю многое! Животные, планеты, цвета...',
      '🔍 Поискал — не нашёл. Попробуй написать иначе?',
      '😊 Давай учиться вместе! О чём хочешь узнать?'
    ],
    en: [
      "🤔 I didn't get that, but I can help! Story, game, or question?",
      '🌟 Interesting! Ask me about a story or a game.',
      "💡 I don't know that, but I know lots! Animals, planets, colors...",
      "🔍 Searched but didn't find it. Try writing differently?",
      "😊 Let's learn together! What do you want to know?"
    ]
  };
  const list = pool[lang] || pool.en;
  const text = list[Math.floor(Math.random() * list.length)];
  return { type: 'fallback', text, chips: this._defaultChips(lang) };
}
```

---

## Files Changed

| File | Change |
|---|---|
| `app/src/main/assets/www/kidai.js` | All changes — new methods, expanded keywords, varied fallback |

## Not In Scope

- Conversation memory / context between messages (future improvement)
- Voice input/output
- New fact categories
- Changes to `index.html` or `main.js`
