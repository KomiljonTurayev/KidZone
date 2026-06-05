# KidAI Improvement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix KidAI so it gives useful, varied responses to both specific ("sher haqida") and general ("qiziq narsa ayt") inputs instead of always showing the same fallback message.

**Architecture:** Flip detection order in `respond()` — check fact topics first via a new `_detectTopic()` method, then intent (story/game/greeting), then fall back to a random fact. Expand all keyword lists ~3x. Replace single fallback string with 5 random variants.

**Tech Stack:** Vanilla JavaScript (no framework, no test runner). Tests are run manually in the browser console after each task.

---

## File Map

| File | Changes |
|---|---|
| `app/src/main/assets/www/kidai.js` | All changes in this plan |

---

## How to Test (Manual)

Open the KidZone app → KidAI tab. Or open `index.html` in a browser → KidAI tab.

Browser console quick test (paste after opening index.html):
```js
// After page load, window.kidAI is available
const e = window.kidAI;
console.log(e._detectTopic('sher haqida'));   // expects: 'hayvon'
console.log(e._detectTopic('kamalak nima'));  // expects: 'rang'
console.log(e._detectTopic('xyz bla bla'));   // expects: null
```

---

## Task 1: Expand `_KAI_FACT_DB` keywords

**Files:**
- Modify: `app/src/main/assets/www/kidai.js:3-97`

- [ ] **Step 1: Replace the entire `_KAI_FACT_DB` block**

Open `kidai.js`. Replace the `_KAI_FACT_DB` const (lines 3–97) with the expanded version below. The fact arrays (uz/ru/en) stay exactly the same — only `kws` arrays change.

```js
const _KAI_FACT_DB = {
  hayvon: {
    kws: [
      'hayvon','animal','животн',
      'sher','fil','mushuk','qush','bear','lion','elephant','tiger','aysiq',
      'it','kuchuk','baliq','qo\'y','ot','echki','tulki','bo\'ri','kiyik',
      'dog','cat','fish','bird','horse','sheep','wolf','fox','deer','rabbit',
      'quyon','maymun','zebra','krokodil','crocodile','monkey',
      'кошка','собака','рыба','птица','лошадь','овца','волк','лиса','олень',
      'медведь','тигр','обезьяна','крокодил','зебра','кролик'
    ],
    uz: ["🦁 Sher — o'rmonning qiroli! U juda kuchli va tez yuguradi.",
         '🐘 Fil — quruqlikdagi eng katta hayvon, juda aqlli!',
         "🦒 Jirafa — eng bo'yi baland hayvon. Bo'yni 2 metrga yetadi!",
         '🐬 Delfinlar — dengizning eng aqlli jonzotlari.',
         "🦋 Kapalak 4 bosqichdan o'tadi: tuxum → lichinka → g'umbak → kapalak!"],
    ru: ['🦁 Лев — царь зверей! Очень сильный и быстрый.',
         '🐘 Слон — самое большое животное суши, очень умный!',
         '🦒 Жираф — самое высокое животное, шея до 2 метров!',
         '🐬 Дельфины — самые умные обитатели океана.',
         '🦋 Бабочка проходит 4 стадии: яйцо → гусеница → куколка → бабочка!'],
    en: ['🦁 The lion is king of the jungle! Very powerful and fast.',
         '🐘 The elephant is the largest land animal — very intelligent!',
         '🦒 The giraffe is the tallest animal, neck up to 2 meters!',
         '🐬 Dolphins are the smartest ocean creatures.',
         '🦋 A butterfly goes through 4 stages: egg → caterpillar → pupa → butterfly!']
  },
  rang: {
    kws: [
      "rang","color","цвет","colour","kamalak","qizil","yashil","ko'k",
      "red","blue","green","rainbow","sariq",
      "oq","qora","white","black","purple","binafsha","pink","atirgul",
      "kulrang","grey","gray","brown","jigarrang","moviy","to'q sariq","orange",
      "синий","красный","жёлтый","белый","чёрный","розовый","фиолетовый",
      "серый","коричневый","оранжевый","радуга"
    ],
    uz: ["🌈 Kamalakda 7 ta rang: qizil, to'q sariq, sariq, yashil, ko'k, moviy, binafsha.",
         "❤️ Qizil rang — issiqlik va kuchni bildiradi!",
         "💛 Sariq rang — quyosh rangi, quvonch va baxtni bildiradi.",
         "💚 Yashil rang — tabiat va umidni bildiradi."],
    ru: ['🌈 В радуге 7 цветов: красный, оранжевый, жёлтый, зелёный, синий, голубой, фиолетовый.',
         '❤️ Красный — цвет тепла и силы!',
         '💛 Жёлтый — цвет солнца, радости и счастья.',
         '💚 Зелёный — цвет природы и надежды.'],
    en: ['🌈 A rainbow has 7 colors: red, orange, yellow, green, blue, indigo, violet.',
         '❤️ Red represents warmth and strength!',
         '💛 Yellow is the color of the sun, joy and happiness.',
         '💚 Green is the color of nature and hope.']
  },
  son: {
    kws: [
      "son","raqam","number","число","hisob","count","matematik","qo'sh","plus","sana",
      "ikki","uch","to'rt","besh","olti","yetti","sakkiz","to'qqiz","o'n",
      "two","three","four","five","six","seven","eight","nine","ten",
      "два","три","четыре","пять","шесть","семь","восемь","девять","десять",
      "ayir","minus","subtract","ko'payt","multiply","bo'l","divide",
      "juft","toq","even","odd","чётный","нечётный","нуль","zero","nol"
    ],
    uz: ["🔢 1 dan 10 gacha: bir, ikki, uch, to'rt, besh, olti, yetti, sakkiz, to'qqiz, o'n!",
         "➕ 2+3=5, 4+4=8, 5+5=10 — qo'shishni bilasanmi?",
         '🔢 Eng katta bir xonali son — 9!',
         '🔢 0 (nol) — matematikada juda muhim son!'],
    ru: ['🔢 От 1 до 10: один, два, три, четыре, пять, шесть, семь, восемь, девять, десять!',
         '➕ 2+3=5, 4+4=8, 5+5=10 — умеешь складывать?',
         '🔢 Наибольшая однозначная цифра — 9!',
         '🔢 0 (ноль) — очень важное число в математике!'],
    en: ['🔢 Count 1–10: one, two, three, four, five, six, seven, eight, nine, ten!',
         '➕ 2+3=5, 4+4=8, 5+5=10 — can you add?',
         '🔢 The largest single-digit number is 9!',
         '🔢 Zero (0) is very important in math!']
  },
  shakl: {
    kws: [
      'shakl','shape','фигур','doira','kvadrat','circle','square','triangle','uchburchak','rectangle',
      'yulduz','star','oval','kub','cube','silindr','cylinder','konus','cone',
      'to\'rtburchak','pentagon','hexagon','olti burchak',
      'звезда','куб','цилиндр','конус','овал','прямоугольник','треугольник','круг','квадрат'
    ],
    uz: ["🔵 Doira — burchaklari yo'q dumaloq shakl. G'ildirak, quyosh doira!",
         '🔶 Uchburchak — 3 ta burchagi va 3 ta tomoni bor.',
         '🟦 Kvadrat — 4 ta teng tomoni bor.',
         "🟥 To'rtburchak — 4 burchak, lekin tomonlari har xil uzunlikda bo'lishi mumkin."],
    ru: ['🔵 Круг — фигура без углов. Колесо и солнце — круглые!',
         '🔶 Треугольник — 3 угла и 3 стороны.',
         '🟦 Квадрат — 4 равные стороны.',
         '🟥 Прямоугольник — 4 угла, стороны могут быть разной длины.'],
    en: ['🔵 A circle has no corners. Wheels and the sun are circles!',
         '🔶 A triangle has 3 corners and 3 sides.',
         '🟦 A square has 4 equal sides.',
         '🟥 A rectangle has 4 corners but sides can be different lengths.']
  },
  sayyora: {
    kws: [
      'sayyora','planet','планет','koinot','space','космос','oy','moon','quyosh','sun','yulduz','star','mars',
      'raketa','rocket','astronavt','astronaut','galaktika','galaxy','kosmoschi',
      'saturn','yupiter','jupiter','venera','venus','merkuriy','mercury','neptun','neptune','uran','uranus',
      'ракета','астронавт','галактика','звезда','луна','солнце','сатурн','юпитер'
    ],
    uz: ['🪐 Quyosh sistemasida 8 ta sayyora bor. Yer — uchinchisi!',
         "🌙 Oy — Yerning yo'ldoshi, Yer atrofida aylanadi.",
         "☀️ Quyosh — bizning yulduzimiz. Yer undan million marta kichik!",
         "🔴 Mars — qizil sayyora. U Yerdan keyingi to'rtinchi sayyora."],
    ru: ['🪐 В Солнечной системе 8 планет. Земля — третья!',
         '🌙 Луна — спутник Земли, вращается вокруг неё.',
         '☀️ Солнце — наша звезда. Земля в миллион раз меньше!',
         '🔴 Марс — красная планета, четвёртая от Солнца.'],
    en: ['🪐 The Solar System has 8 planets. Earth is third!',
         "🌙 The Moon is Earth's satellite, orbiting around it.",
         '☀️ The Sun is our star. Earth is a million times smaller!',
         '🔴 Mars is the red planet, fourth from the Sun.']
  },
  meva: {
    kws: [
      'meva','fruit','фрукт','olma','banan','apple','banana','orange','apelsin','uzum','grape',
      'tarvuz','watermelon','nok','pear','gilos','cherry','qovun','melon','anor','pomegranate',
      'limon','lemon','mango','ananas','pineapple','shaftoli','peach','o\'rik','apricot',
      'sabzavot','vegetable','овощ','арбуз','груша','вишня','дыня','гранат','лимон','манго'
    ],
    uz: ["🍎 Olma — juda foydali meva, vitaminlar ko'p!",
         '🍌 Banan — sariq, mazali va quvvat beradi!',
         "🍊 Apelsin — C vitamini ko'p, shamollashdan saqlaydi!",
         "🍇 Uzum — kichkina, lekin juda mazali va foydali!"],
    ru: ['🍎 Яблоко — очень полезный фрукт, богат витаминами!',
         '🍌 Банан — жёлтый, вкусный и даёт энергию!',
         '🍊 Апельсин богат витамином C, защищает от простуды!',
         '🍇 Виноград — маленький, но очень вкусный и полезный!'],
    en: ['🍎 Apples are very healthy, full of vitamins!',
         '🍌 Bananas are yellow, tasty and give you energy!',
         '🍊 Oranges have lots of vitamin C to fight colds!',
         '🍇 Grapes are small but very tasty and healthy!']
  }
};
```

- [ ] **Step 2: Verify keyword counts look right**

Each category should now have 20–35 keywords. Quick check in browser console:
```js
// After page load
Object.entries(_KAI_FACT_DB).forEach(([k,v]) => console.log(k, v.kws.length));
// Expected: hayvon ~35, rang ~30, son ~30, shakl ~22, sayyora ~28, meva ~28
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/www/kidai.js
git commit -m "fix(kidai): expand fact DB keyword lists 3x"
```

---

## Task 2: Add `_detectTopic()` method

**Files:**
- Modify: `app/src/main/assets/www/kidai.js` (inside `KidAIEngine` class, after `retrieveStory`)

- [ ] **Step 1: Add `_detectTopic()` after `retrieveStory()` method**

In `kidai.js`, find the line `// ── INTENT DETECTION ─────────────────────────────────────────` (around line 155). Insert the new method just before that comment block:

```js
  _detectTopic(text) {
    const lower = text.toLowerCase();
    for (const [cat, data] of Object.entries(_KAI_FACT_DB)) {
      if (data.kws.some(kw => lower.includes(kw))) return cat;
    }
    return null;
  }
```

- [ ] **Step 2: Verify in browser console**

```js
const e = window.kidAI;
console.assert(e._detectTopic('sher haqida') === 'hayvon',    'hayvon kw test');
console.assert(e._detectTopic('kamalak nima') === 'rang',     'rang kw test');
console.assert(e._detectTopic('beshta olma') === 'meva',      'meva kw test');
console.assert(e._detectTopic('raketa') === 'sayyora',        'sayyora kw test');
console.assert(e._detectTopic('xyz bla bla') === null,        'null test');
console.log('_detectTopic tests passed');
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/www/kidai.js
git commit -m "fix(kidai): add _detectTopic() method"
```

---

## Task 3: Add `_randomFact()` method

**Files:**
- Modify: `app/src/main/assets/www/kidai.js` (inside `KidAIEngine` class, after `_detectTopic`)

- [ ] **Step 1: Add `_randomFact()` after `_detectTopic()` method**

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

- [ ] **Step 2: Verify in browser console**

```js
const e = window.kidAI;
const r = e._randomFact('uz');
console.assert(typeof r.text === 'string' && r.text.length > 5, 'text exists');
console.assert(Array.isArray(r.chips) && r.chips.length === 3, 'chips ok');
console.assert(r.type === 'fact', 'type is fact');
// Run 5 times to check randomness
for (let i = 0; i < 5; i++) console.log(e._randomFact('uz').text);
console.log('_randomFact tests passed');
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/www/kidai.js
git commit -m "fix(kidai): add _randomFact() method"
```

---

## Task 4: Update `_fallback()` — 5 random variants

**Files:**
- Modify: `app/src/main/assets/www/kidai.js` — `_fallback()` method

- [ ] **Step 1: Replace the existing `_fallback()` method**

Find the current `_fallback(lang)` method and replace it entirely:

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

- [ ] **Step 2: Verify randomness in browser console**

```js
const e = window.kidAI;
const seen = new Set();
for (let i = 0; i < 20; i++) seen.add(e._fallback('uz').text);
console.assert(seen.size > 1, 'fallback should vary — got: ' + seen.size + ' unique');
console.log('Unique fallback variants seen:', seen.size, '(expect 2-5)');
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/www/kidai.js
git commit -m "fix(kidai): 5 random fallback variants"
```

---

## Task 5: Update `_detectIntent()` — remove `fact` branch, expand keywords

**Files:**
- Modify: `app/src/main/assets/www/kidai.js` — `_detectIntent()` method

- [ ] **Step 1: Replace `_detectIntent()` method**

Find the current `_detectIntent(text)` method and replace it entirely. Remove the `fact` entry from `KW` and its case from the switch in `respond()`:

```js
  _detectIntent(text) {
    const lower = text.toLowerCase();
    const KW = {
      greeting: [
        'salom','assalom','xayr','yaxshimisan','qalaysan','nima gap',
        'привет','здравствуй','пока','как дела',
        'hello','bye','hi','hey','sup','yo'
      ],
      story: [
        "ertak","hikoya","aytib ber","ayt","eshit","o'qi","bitta","boshla","davom","eshitay",
        'сказку','сказка','расскажи','начни','продолжи',
        'story','tale','tell','read','begin','once upon'
      ],
      game: [
        "o'yin","o'yna","ko'rsat","qanday o'yin","nimani o'ynayman",
        'игру','игра','играть','покажи','какую игру','запусти',
        'game','play','show','launch'
      ]
    };
    const scores = { greeting: 0, story: 0, game: 0 };
    for (const [intent, keywords] of Object.entries(KW)) {
      for (const kw of keywords) { if (lower.includes(kw)) scores[intent]++; }
    }
    const max = Math.max(...Object.values(scores));
    if (!max) return 'fallback';
    return Object.keys(scores).find(k => scores[k] === max) || 'fallback';
  }
```

- [ ] **Step 2: Verify in browser console**

```js
const e = window.kidAI;
console.assert(e._detectIntent('salom') === 'greeting',    'greeting uz');
console.assert(e._detectIntent('hello') === 'greeting',    'greeting en');
console.assert(e._detectIntent('ertak ayt') === 'story',   'story uz');
console.assert(e._detectIntent("o'yin topchi") === 'game', 'game uz');
console.assert(e._detectIntent('xyz bla') === 'fallback',  'unknown');
console.log('_detectIntent tests passed');
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/www/kidai.js
git commit -m "fix(kidai): expand intent keywords, remove fact branch from _detectIntent"
```

---

## Task 6: Wire `respond()` — topic-first flow

**Files:**
- Modify: `app/src/main/assets/www/kidai.js` — `respond()` method

- [ ] **Step 1: Replace `respond()` method**

Find the current `respond(text, lang)` method and replace it entirely:

```js
  respond(text, lang) {
    if (!text || !text.trim()) return null;
    const input  = text.slice(0, 200);

    const topic = this._detectTopic(input);
    if (topic) return this._fact(input, lang);

    const intent = this._detectIntent(input);
    switch (intent) {
      case 'greeting': return this._greeting(lang);
      case 'story':    return this._story(input, lang);
      case 'game':     return this._game(input, lang);
      default:         return this._randomFact(lang);
    }
  }
```

Note: `_fact()` already uses `_KAI_FACT_DB` keyword matching internally. With `_detectTopic()` already confirming a topic exists, `_fact()` will always find a match.

- [ ] **Step 2: Full integration test in browser console**

```js
const e = window.kidAI;

// Topic-first: specific animal query (no "nima" keyword)
const r1 = e.respond('sher haqida', 'uz');
console.assert(r1.type === 'fact', 'sher → fact: ' + r1.type);
console.log('sher:', r1.text);

// Topic-first: color
const r2 = e.respond('kamalak', 'uz');
console.assert(r2.type === 'fact', 'kamalak → fact');
console.log('kamalak:', r2.text);

// Intent: story
const r3 = e.respond('ertak ayt', 'uz');
console.assert(['story','fallback'].includes(r3.type), 'ertak → story/fallback');
console.log('ertak:', r3.type, r3.text.slice(0,40));

// Intent: greeting
const r4 = e.respond('salom', 'uz');
console.assert(r4.type === 'greeting', 'salom → greeting');

// No match → randomFact (not static fallback)
const seen = new Set();
for (let i = 0; i < 10; i++) seen.add(e.respond('xyz bla bla', 'uz').text);
console.assert(seen.size > 1, 'unknown input → varied random fact, got: ' + seen.size);

console.log('All respond() integration tests passed');
```

- [ ] **Step 3: Manual app test** — open KidAI tab and type these one by one:
  - `sher haqida` → should show lion/animal fact (not "Tushunmadim")
  - `kamalak` → should show rainbow/color fact
  - `raketa` → should show space fact
  - `xyz bla bla` → should show a random fact (different each time)
  - `salom` → should show greeting
  - `ertak ayt` → should show story snippet

- [ ] **Step 4: Commit**

```bash
git add app/src/main/assets/www/kidai.js
git commit -m "fix(kidai): topic-first detection in respond(), random fact fallback"
```

---

## Task 7: Final verification

- [ ] **Step 1: Run full console test suite**

Open the app in browser, paste this full suite into console:

```js
const e = window.kidAI;
let pass = 0, fail = 0;
function check(label, cond) {
  if (cond) { console.log('✅', label); pass++; }
  else       { console.error('❌', label); fail++; }
}

check('sher → fact',    e.respond('sher haqida','uz').type === 'fact');
check('kamalak → fact', e.respond('kamalak','uz').type === 'fact');
check('dog → fact',     e.respond('dog','en').type === 'fact');
check('raketa → fact',  e.respond('raketa','uz').type === 'fact');
check('besh → fact',    e.respond('besh','uz').type === 'fact');
check('apple → fact',   e.respond('apple','en').type === 'fact');
check('salom → greet',  e.respond('salom','uz').type === 'greeting');
check('hello → greet',  e.respond('hello','en').type === 'greeting');
check('ertak → story',  ['story','fallback'].includes(e.respond('ertak ayt','uz').type));
check("o'yin → game",   ['game','fallback'].includes(e.respond("o'yin topchi",'uz').type));

const fallbacks = new Set();
for (let i=0; i<15; i++) fallbacks.add(e.respond('xyz123','uz').text);
check('unknown → varied (2+ unique)', fallbacks.size >= 2);

console.log(`\n${pass} passed, ${fail} failed`);
```

Expected: `11 passed, 0 failed`

- [ ] **Step 2: Final commit if any last fixes needed**

```bash
git add app/src/main/assets/www/kidai.js
git commit -m "fix(kidai): final verification fixes"
```
