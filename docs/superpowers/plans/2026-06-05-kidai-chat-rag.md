# KidAI Chat Assistant Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** KidZone ilovasiga offline ishlaydigan bolalar AI chat assistantini qo'shish — intent detection + RAG retrieval + templates asosida ertak aytish, o'yin tavsiya qilish va savollarga javob berish.

**Architecture:** `kidai.js` da `KidAIEngine` klassi yaratiladi. U `content.json` ertaklari va `GAMES` massivini RAG manbai sifatida ishlatadi. `index.html` da uchinchi tab (`KidAI`) va chat paneli qo'shiladi. `main.js` dagi `generateAiStory()` ham shu engine orqali yangilanadi.

**Tech Stack:** Vanilla JS, HTML5, CSS3, Android WebView — hech qanday tashqi kutubxona yoki API yo'q.

---

## Fayllar

| Fayl | O'zgarish |
|------|-----------|
| `app/src/main/assets/www/kidai.js` | Yangi — KidAIEngine class |
| `app/src/main/assets/www/index.html` | Tab tugmasi, chat HTML, CSS, JS glue, translations |
| `app/src/main/assets/www/main.js` | UIManager.switchTab + generateAiStory() |

---

## Task 1: `kidai.js` — KidAIEngine klassi

**Files:**
- Create: `app/src/main/assets/www/kidai.js`

- [ ] **Step 1: Faylni yarating**

```js
// app/src/main/assets/www/kidai.js
class KidAIEngine {
  constructor(stories, games) {
    this.stories = stories || [];
    this.games   = games   || [];
  }

  // ── PUBLIC API ────────────────────────────────────────────────

  respond(text, lang) {
    if (!text || !text.trim()) return null;
    const input  = text.slice(0, 200);
    const intent = this._detectIntent(input);
    switch (intent) {
      case 'greeting': return this._greeting(lang);
      case 'story':    return this._story(input, lang);
      case 'game':     return this._game(input, lang);
      case 'fact':     return this._fact(input, lang);
      default:         return this._fallback(lang);
    }
  }

  retrieveStory(lang, topic) {
    let pool = this.stories;
    if (topic) {
      const CAT = {
        hayvon:'animals', animal:'animals',  животн:'animals',
        koinot:'space',   space:'space',     космос:'space',
        oila:'family',    family:'family',   семья:'family',
        tabiat:'nature',  nature:'nature',   природ:'nature',
        qahramon:'heroes',hero:'heroes',     герои:'heroes'
      };
      const lower = topic.toLowerCase();
      for (const [kw, cat] of Object.entries(CAT)) {
        if (lower.includes(kw)) {
          const filtered = this.stories.filter(s => s.category === cat);
          if (filtered.length) { pool = filtered; break; }
        }
      }
    }
    if (!pool.length) pool = this.stories;
    return pool.length ? pool[Math.floor(Math.random() * pool.length)] : null;
  }

  // ── INTENT DETECTION ─────────────────────────────────────────

  _detectIntent(text) {
    const lower = text.toLowerCase();
    const KW = {
      greeting: ['salom','assalom','xayr','yaxshimisan',
                 'привет','здравствуй','пока',
                 'hello','hi','bye'],
      story:    ["ertak","hikoya","aytib ber","ayt","eshit","o'qi",
                 'сказку','сказка','расскажи',
                 'story','tale','tell','read'],
      game:     ["o'yin","o'yna","ko'rsat",
                 'игру','игра','играть','покажи',
                 'game','play','show'],
      fact:     ['nima','nega','qanday','qancha','kim','qachon',
                 'что','почему','как','сколько','кто',
                 'what','why','how','who','when']
    };
    const scores = { greeting: 0, story: 0, game: 0, fact: 0 };
    for (const [intent, keywords] of Object.entries(KW)) {
      for (const kw of keywords) { if (lower.includes(kw)) scores[intent]++; }
    }
    const max = Math.max(...Object.values(scores));
    if (!max) return 'fallback';
    if (scores.fact === max) return 'fact';
    return Object.keys(scores).find(k => scores[k] === max) || 'fallback';
  }

  // ── RESPONSE BUILDERS ─────────────────────────────────────────

  _greeting(lang) {
    const T = {
      uz: "Salom! 👋 Men KidAI — sening bilim do'sting! Ertak eshitmoqchimisan, o'yin topmoqchimisan yoki savol bermoqchimisan?",
      ru: "Привет! 👋 Я KidAI — твой умный друг! Хочешь сказку, игру или узнать что-то новое?",
      en: "Hello! 👋 I'm KidAI — your learning buddy! Want a story, a game, or do you have a question?"
    };
    return { type: 'greeting', text: T[lang]||T.en, chips: this._defaultChips(lang) };
  }

  _story(text, lang) {
    const story = this.retrieveStory(lang, text);
    if (!story) return this._fallback(lang);
    const title   = story.title[lang]  || story.title.en;
    const snippet = (story.text[lang]  || story.text.en  || '').slice(0, 120);
    const T = {
      uz: `📖 ${story.emoji} **${title}**\n\n"${snippet}..."`,
      ru: `📖 ${story.emoji} **${title}**\n\n"${snippet}..."`,
      en: `📖 ${story.emoji} **${title}**\n\n"${snippet}..."`
    };
    const CHIPS = {
      uz: ["📖 To'liq o'qi", '🔄 Boshqa ertak', "🎮 O'yin"],
      ru: ['📖 Читать полностью', '🔄 Другую сказку', '🎮 Игра'],
      en: ['📖 Read full', '🔄 Another story', '🎮 Game']
    };
    return { type:'story', text:T[lang]||T.en, storyId:story.id, chips:CHIPS[lang]||CHIPS.en };
  }

  _game(text, lang) {
    const CATS = {
      math:   ['math','raqam','число','matematika','hisob','count'],
      colors: ["rang","color","цвет","paint","bo'yash"],
      music:  ['musiqa','music','музыка','piano','doira','drums','sing'],
      puzzle: ['puzzle','bosh qotirma','головоломка','jigsaw'],
      sport:  ['sport','futbol','soccer','jump'],
      nature: ["tabiat","nature","природа","o'simlik"]
    };
    const lower = text.toLowerCase();
    let pool = this.games;
    for (const [cat, kws] of Object.entries(CATS)) {
      if (kws.some(kw => lower.includes(kw))) {
        const filtered = this.games.filter(g => g.cat === cat);
        if (filtered.length) { pool = filtered; break; }
      }
    }
    const g = pool[Math.floor(Math.random() * pool.length)];
    if (!g) return this._fallback(lang);
    const name = typeof g.name === 'object' ? (g.name[lang]||g.name.en) : g.name;
    const tag  = typeof g.tag  === 'object' ? (g.tag[lang] ||g.tag.en)  : (g.tag||'');
    const T = {
      uz: `🎮 ${g.em} **${name}**\n${tag} — ${g.age} yoshlar uchun.`,
      ru: `🎮 ${g.em} **${name}**\n${tag} — для детей ${g.age}.`,
      en: `🎮 ${g.em} **${name}**\n${tag} — for ages ${g.age}.`
    };
    const CHIPS = {
      uz: ["🎮 O'yna!", "🔄 Boshqa o'yin", '📖 Ertak'],
      ru: ['🎮 Играть!', '🔄 Другая игра', '📖 Сказка'],
      en: ['🎮 Play!', '🔄 Another game', '📖 Story']
    };
    return { type:'game', text:T[lang]||T.en, gameId:g.id, gameObj:g, chips:CHIPS[lang]||CHIPS.en };
  }

  _fact(text, lang) {
    const lower = text.toLowerCase();
    const DB = {
      hayvon: {
        kws: ['hayvon','animal','животн','sher','fil','mushuk','qush','bear','lion','elephant','tiger','aysiq'],
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
        kws: ["rang","color","цвет","colour","kamalak","qizil","yashil","ko'k","red","blue","green","rainbow","sariq"],
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
        kws: ["son","raqam","number","число","hisob","count","matematik","qo'sh","plus","sana"],
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
        kws: ['shakl','shape','фигур','doira','kvadrat','circle','square','triangle','uchburchak','rectangle'],
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
        kws: ['sayyora','planet','планет','koinot','space','космос','oy','moon','quyosh','sun','yulduz','star','mars'],
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
        kws: ['meva','fruit','фрукт','olma','banan','apple','banana','orange','apelsin','uzum','grape'],
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

    for (const [, data] of Object.entries(DB)) {
      if (data.kws.some(kw => lower.includes(kw))) {
        const list = data[lang] || data.en;
        const CHIPS = {
          uz: ["❓ Yana savol", '📖 Ertak', "🎮 O'yin"],
          ru: ['❓ Ещё вопрос', '📖 Сказка', '🎮 Игра'],
          en: ['❓ More facts', '📖 Story', '🎮 Game']
        };
        return { type:'fact', text:list[Math.floor(Math.random()*list.length)], chips:CHIPS[lang]||CHIPS.en };
      }
    }

    const generic = {
      uz: "🌍 Hayvonlar, ranglar, sayyoralar, sonlar, shakllar yoki mevalar haqida so'ra!",
      ru: '🌍 Спроси меня о животных, цветах, планетах, числах, формах или фруктах!',
      en: '🌍 Ask me about animals, colors, planets, numbers, shapes or fruits!'
    };
    const CHIPS2 = {
      uz: ['🦁 Hayvonlar', '🌈 Ranglar', '🪐 Sayyoralar'],
      ru: ['🦁 Животные', '🌈 Цвета', '🪐 Планеты'],
      en: ['🦁 Animals', '🌈 Colors', '🪐 Planets']
    };
    return { type:'fact', text:generic[lang]||generic.en, chips:CHIPS2[lang]||CHIPS2.en };
  }

  _fallback(lang) {
    const T = {
      uz: "🤔 Tushunmadim, lekin yordam bera olaman! Ertak, o'yin yoki savol — nima kerak?",
      ru: '🤔 Не понял, но я помогу! Сказка, игра или вопрос — что нужно?',
      en: "🤔 I didn't get that, but I can help! Story, game, or question?"
    };
    return { type:'fallback', text:T[lang]||T.en, chips:this._defaultChips(lang) };
  }

  _defaultChips(lang) {
    return {
      uz: ["📖 Ertak ayt", "🎮 O'yin topchi", '❓ Savol ber'],
      ru: ['📖 Расскажи сказку', '🎮 Найди игру', '❓ Вопрос'],
      en: ['📖 Tell a story', '🎮 Find a game', '❓ Ask me']
    }[lang] || ['📖 Tell a story', '🎮 Find a game', '❓ Ask me'];
  }
}
```

- [ ] **Step 2: Tekshirish — brauzerda console test**

`index.html` ni brauzerda oching, console da:
```js
// Temp test (kidai.js yuklanmagan, lekin sinab ko'rish uchun)
const engine = new KidAIEngine([], []);
console.log(engine._detectIntent('ertak ayt'));      // "story"
console.log(engine._detectIntent('salom'));           // "greeting"
console.log(engine._detectIntent("nima bu hayvon")); // "fact"
console.log(engine._detectIntent("o'yin ko'rsat"));  // "game"
console.log(engine._detectIntent('blah blah'));       // "fallback"
```
Kutilgan natija: 5 ta to'g'ri intent.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/www/kidai.js
git commit -m "feat(kidai): KidAIEngine class — intent detection, RAG retrieval, templates"
```

---

## Task 2: `main.js` — UIManager.switchTab yangilanishi

**Files:**
- Modify: `app/src/main/assets/www/main.js:92`

`UIManager.switchTab()` ichida `['games', 'stories', 'songs']` ni `['games', 'stories', 'kidai']` ga o'zgartirish kerak (`songs` allaqachon olib tashlangan).

- [ ] **Step 1: `main.js` ni o'zgartiring**

Fayl: `app/src/main/assets/www/main.js`, ~92-qator.

`old_string`:
```js
        ['games', 'stories', 'songs'].forEach(s => {
```

`new_string`:
```js
        ['games', 'stories', 'kidai'].forEach(s => {
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/assets/www/main.js
git commit -m "fix(ui): add kidai tab to UIManager.switchTab section list"
```

---

## Task 3: `index.html` — CSS + HTML

**Files:**
- Modify: `app/src/main/assets/www/index.html`

### 3a — CSS

- [ ] **Step 1: CSS qo'shing**

`index.html` da `/* ── CONTENT SECTIONS ── */` blokidan keyin (hozir ~508-qator) quyidagi CSS ni qo'shing:

```css
    /* ── KIDAI CHAT ── */
    #kidai-section{display:flex;flex-direction:column;height:100%;overflow:hidden}
    #kidai-section.h{display:none}
    #kidai-messages{flex:1;overflow-y:auto;padding:16px var(--pad);display:flex;flex-direction:column;gap:12px;-webkit-overflow-scrolling:touch}
    .kai-msg{display:flex;align-items:flex-start;gap:8px;max-width:85%}
    .kai-msg .kai-av{font-size:22px;flex-shrink:0;margin-top:2px}
    .kai-msg .kai-bub{background:var(--surface);border-radius:0 16px 16px 16px;padding:10px 14px;font-size:15px;color:var(--text);line-height:1.6;box-shadow:0 1px 3px rgba(0,0,0,0.08);word-break:break-word}
    .kai-msg.user{flex-direction:row-reverse;align-self:flex-end}
    .kai-msg.user .kai-bub{background:var(--accent);color:#fff;border-radius:16px 0 16px 16px}
    .kai-chips{display:flex;flex-wrap:wrap;gap:6px;margin-top:8px}
    .kai-chip{background:#fff;border:1.5px solid var(--accent);border-radius:20px;padding:5px 12px;font-size:13px;color:var(--accent);font-family:'Fredoka One',cursive;cursor:pointer;touch-action:manipulation}
    .kai-typing span{display:inline-block;width:7px;height:7px;background:var(--dim);border-radius:50%;margin:0 2px;animation:kaiBounce 1.2s infinite}
    .kai-typing span:nth-child(2){animation-delay:.2s}
    .kai-typing span:nth-child(3){animation-delay:.4s}
    @keyframes kaiBounce{0%,80%,100%{transform:translateY(0)}40%{transform:translateY(-6px)}}
    #kai-quick{display:flex;gap:8px;padding:8px var(--pad);overflow-x:auto;background:var(--surface);border-top:1px solid rgba(0,0,0,0.06);flex-shrink:0;scrollbar-width:none}
    #kai-quick::-webkit-scrollbar{display:none}
    .kai-qchip{background:#fff;border:1.5px solid var(--accent);border-radius:20px;padding:6px 14px;font-size:13px;color:var(--accent);font-family:'Fredoka One',cursive;white-space:nowrap;cursor:pointer;flex-shrink:0;touch-action:manipulation}
    #kai-input-row{display:flex;gap:8px;padding:8px var(--pad) calc(8px + env(safe-area-inset-bottom,0px));background:#fff;flex-shrink:0;border-top:1px solid rgba(0,0,0,0.06)}
    #kai-input{flex:1;border:2px solid #E0E0E0;border-radius:24px;padding:10px 16px;font-size:15px;font-family:'Fredoka One',cursive;outline:none;background:#fff;color:var(--text)}
    #kai-input:focus{border-color:var(--accent)}
    #kai-send{background:var(--accent);color:#fff;border:none;border-radius:50%;width:44px;height:44px;font-size:20px;cursor:pointer;flex-shrink:0;display:flex;align-items:center;justify-content:center;touch-action:manipulation}
```

### 3b — Tab tugmasi

- [ ] **Step 2: KidAI tab tugmasini qo'shing**

Tab bar da Ertaklar tugmasidan keyin (`index.html` ~1022-qator):

`old_string`:
```html
    <button class="kz-tab" id="tab-stories" onclick="switchTab('stories')">📖 <span id="tab-lbl-stories">Ertaklar</span></button>
  </div>
```

`new_string`:
```html
    <button class="kz-tab" id="tab-stories" onclick="switchTab('stories')">📖 <span id="tab-lbl-stories">Ertaklar</span></button>
    <button class="kz-tab" id="tab-kidai" onclick="switchTab('kidai')">🤖 <span id="tab-lbl-kidai">KidAI</span></button>
  </div>
```

### 3c — Chat section HTML

- [ ] **Step 3: Chat section HTML qo'shing**

Stories section dan keyin, `</div><!-- /#main -->` dan oldin:

`old_string`:
```html
  </div><!-- /#main -->
</div><!-- /#shell -->
```

`new_string`:
```html
    <!-- KIDAI SECTION -->
    <div id="kidai-section" class="h">
      <div id="kidai-messages"></div>
      <div id="kai-quick">
        <button class="kai-qchip" id="kai-q1" onclick="kaiQuick('story')">📖 Ertak ayt</button>
        <button class="kai-qchip" id="kai-q2" onclick="kaiQuick('game')">🎮 O'yin topchi</button>
        <button class="kai-qchip" id="kai-q3" onclick="kaiQuick('fact')">❓ Savol ber</button>
      </div>
      <div id="kai-input-row">
        <input id="kai-input" type="text" placeholder="Yoz..." maxlength="200"
               onkeydown="if(event.key==='Enter')kaiSend()">
        <button id="kai-send" onclick="kaiSend()">➤</button>
      </div>
    </div><!-- /#kidai-section -->

  </div><!-- /#main -->
</div><!-- /#shell -->
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/assets/www/index.html
git commit -m "feat(kidai): chat UI — CSS, tab button, chat section HTML"
```

---

## Task 4: `index.html` — Chat JS, tarjimalar, init

**Files:**
- Modify: `app/src/main/assets/www/index.html`
- Modify: `app/src/main/assets/www/index.html` (translations T object)

### 4a — `kidai.js` script tag

- [ ] **Step 1: `kidai.js` ni yuklash**

`<script src="main.js"></script>` dan oldin qo'shing:

`old_string`:
```html
  <script src="main.js"></script>
```

`new_string`:
```html
  <script src="kidai.js"></script>
  <script src="main.js"></script>
```

### 4b — Tarjima kalitlari

- [ ] **Step 2: UZ tarjimalarini qo'shing**

`T` obyektining `uz` bo'limida `noAudio` dan keyin:

`old_string`:
```js
        noAudio:"Audio fayl yo'q 🎵",
      },
      ru: {
```

`new_string`:
```js
        noAudio:"Audio fayl yo'q 🎵",
        tabKidai:"KidAI", kidaiPlaceholder:"Yoz...", kidaiClear:"Tozala",
        kidaiQ1:"📖 Ertak ayt", kidaiQ2:"🎮 O'yin topchi", kidaiQ3:"❓ Savol ber",
      },
      ru: {
```

- [ ] **Step 3: RU tarjimalarini qo'shing**

`T` obyektining `ru` bo'limida `noAudio` dan keyin:

`old_string`:
```js
        noAudio:"Аудио недоступно 🎵",
      },
      en: {
```

`new_string`:
```js
        noAudio:"Аудио недоступно 🎵",
        tabKidai:"KidAI", kidaiPlaceholder:"Напиши...", kidaiClear:"Очистить",
        kidaiQ1:"📖 Расскажи сказку", kidaiQ2:"🎮 Найди игру", kidaiQ3:"❓ Вопрос",
      },
      en: {
```

- [ ] **Step 4: EN tarjimalarini qo'shing**

`T` obyektining `en` bo'limida `noAudio` dan keyin:

`old_string`:
```js
        noAudio:"Audio unavailable 🎵",
      },
    };
```

`new_string`:
```js
        noAudio:"Audio unavailable 🎵",
        tabKidai:"KidAI", kidaiPlaceholder:"Type...", kidaiClear:"Clear",
        kidaiQ1:"📖 Tell a story", kidaiQ2:"🎮 Find a game", kidaiQ3:"❓ Ask me",
      },
    };
```

### 4c — `updateLangUI` dagi KidAI yangilanishi

- [ ] **Step 5: `updateLangUI` ga KidAI qo'shing**

`updateLangUI` ichida tab labels qismidan keyin (`if (ts) ts.textContent = t2.tabStories;` dan keyin):

`old_string`:
```js
        if (tg) tg.textContent = t2.tabGames;
        if (ts) ts.textContent = t2.tabStories;

        // Search placeholders
```

`new_string`:
```js
        if (tg) tg.textContent = t2.tabGames;
        if (ts) ts.textContent = t2.tabStories;
        const tk = document.getElementById('tab-lbl-kidai');
        if (tk) tk.textContent = t2.tabKidai;
        const kInput = document.getElementById('kai-input');
        if (kInput) kInput.placeholder = t2.kidaiPlaceholder;
        const q1 = document.getElementById('kai-q1'); if (q1) q1.textContent = t2.kidaiQ1;
        const q2 = document.getElementById('kai-q2'); if (q2) q2.textContent = t2.kidaiQ2;
        const q3 = document.getElementById('kai-q3'); if (q3) q3.textContent = t2.kidaiQ3;

        // Search placeholders
```

### 4d — Chat JS funksiyalari

- [ ] **Step 6: Chat JS funksiyalarini qo'shing**

Inline script da `function switchTab(tab) {` dan oldin qo'shing:

`old_string`:
```js
    function switchTab(tab) {
      app.ui.switchTab(tab);
      if (tab === 'stories') app.storyManager?.render();
    }
```

`new_string`:
```js
    // ── KIDAI CHAT ──────────────────────────────────────────────
    var _kaiCtx = {};
    var _kaiMsgId = 0;

    function kaiSend() {
      var inp = document.getElementById('kai-input');
      var text = inp.value.trim();
      if (!text) return;
      inp.value = '';
      kaiHandleInput(text);
    }

    function kaiQuick(intent) {
      var lang = app?.translator?.lang || 'uz';
      var prompts = {
        story: {uz:"Ertak ayt", ru:"Расскажи сказку", en:"Tell me a story"},
        game:  {uz:"O'yin topchi", ru:"Найди игру", en:"Find me a game"},
        fact:  {uz:"Savol beraman", ru:"Задам вопрос", en:"I have a question"}
      };
      kaiHandleInput((prompts[intent] || prompts.fact)[lang] || 'story');
    }

    function kaiHandleInput(text) {
      kaiAppendMsg(text, 'user', null, null);
      kaiShowTyping();
      setTimeout(function() {
        kaiHideTyping();
        var lang = app?.translator?.lang || 'uz';
        var response = window.kidAI
          ? window.kidAI.respond(text, lang)
          : {type:'fallback', text:'...', chips:[]};
        if (response) kaiAppendMsg(response.text, 'bot', response.chips, response);
      }, 600);
    }

    function kaiAppendMsg(text, role, chips, ctx) {
      var list = document.getElementById('kidai-messages');
      var msgId = ++_kaiMsgId;
      if (ctx) _kaiCtx[msgId] = ctx;
      var div = document.createElement('div');
      div.className = 'kai-msg' + (role === 'user' ? ' user' : '');
      if (role === 'bot') {
        var html = text
          .replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
          .replace(/\*\*(.+?)\*\*/g,'<strong>$1</strong>')
          .replace(/\n/g,'<br>');
        var chipsHtml = '';
        if (chips && chips.length) {
          chipsHtml = '<div class="kai-chips">' +
            chips.map(function(c) {
              return '<button class="kai-chip" data-chip="' +
                c.replace(/"/g,'&quot;') + '" data-mid="' + msgId +
                '" onclick="kaiChipTap(this)">' + c + '</button>';
            }).join('') + '</div>';
        }
        div.innerHTML = '<span class="kai-av">🤖</span><div><div class="kai-bub">' +
          html + '</div>' + chipsHtml + '</div>';
      } else {
        var safe = text.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
        div.innerHTML = '<div class="kai-bub">' + safe + '</div>';
      }
      list.appendChild(div);
      list.scrollTop = list.scrollHeight;
    }

    function kaiShowTyping() {
      var list = document.getElementById('kidai-messages');
      var div = document.createElement('div');
      div.className = 'kai-msg';
      div.id = 'kai-typing';
      div.innerHTML = '<span class="kai-av">🤖</span>' +
        '<div class="kai-bub kai-typing"><span></span><span></span><span></span></div>';
      list.appendChild(div);
      list.scrollTop = list.scrollHeight;
    }

    function kaiHideTyping() {
      var el = document.getElementById('kai-typing');
      if (el) el.remove();
    }

    function kaiChipTap(btn) {
      var chip = btn.getAttribute('data-chip');
      var msgId = parseInt(btn.getAttribute('data-mid'));
      var ctx = _kaiCtx[msgId];
      var lang = app?.translator?.lang || 'uz';

      if (chip.indexOf("o'qi") !== -1 || chip.indexOf('Read full') !== -1 ||
          chip.indexOf('полностью') !== -1) {
        if (ctx && ctx.storyId && app && app.storyManager) {
          var item = app.storyManager.items &&
            app.storyManager.items.find(function(i){ return i.id === ctx.storyId; });
          if (item) { app.storyManager._play(item); return; }
        }
      }
      if (chip.indexOf("O'yna") !== -1 || chip.indexOf('Play!') !== -1 ||
          chip.indexOf('Играть') !== -1) {
        if (ctx && ctx.gameObj) { if (app) app.openGame(ctx.gameObj); return; }
        if (ctx && ctx.gameId) {
          var g = GAMES.find(function(x){ return x.id === ctx.gameId; });
          if (g && app) { app.openGame(g); return; }
        }
      }
      kaiHandleInput(chip);
    }

    function kaiInit() {
      var list = document.getElementById('kidai-messages');
      if (list && list.childElementCount === 0) {
        var lang = app?.translator?.lang || 'uz';
        var r = window.kidAI ? window.kidAI._greeting(lang)
          : {type:'greeting',
             text:"Salom! 👋 Men KidAI — sening bilim do'sting!",
             chips:["📖 Ertak ayt","🎮 O'yin topchi","❓ Savol ber"]};
        kaiAppendMsg(r.text, 'bot', r.chips, r);
      }
    }
    // ────────────────────────────────────────────────────────────

    function switchTab(tab) {
      app.ui.switchTab(tab);
      if (tab === 'stories') app.storyManager?.render();
      if (tab === 'kidai') kaiInit();
    }
```

- [ ] **Step 7: Commit**

```bash
git add app/src/main/assets/www/index.html
git commit -m "feat(kidai): chat JS functions, translations, init on tab switch"
```

---

## Task 5: `main.js` — `generateAiStory()` yangilanishi

**Files:**
- Modify: `app/src/main/assets/www/main.js`

`generateAiStory()` ichida `this.getAiStory(this.translator.lang)` ni `window.kidAI.retrieveStory()` bilan almashtiramiz.

- [ ] **Step 1: `generateAiStory()` ni o'zgartiring**

Fayl: `app/src/main/assets/www/main.js`, `generateAiStory()` metodi ichida.

`old_string`:
```js
            const story = this.getAiStory(this.translator.lang);
            document.getElementById("aiv-story-title").textContent = story.title;
            document.getElementById("aiv-content").textContent = story.text;
```

`new_string`:
```js
            const raw = window.kidAI
              ? window.kidAI.retrieveStory(this.translator.lang, null)
              : this.getAiStory(this.translator.lang);
            const story = raw && raw.title
              ? { title: raw.title[this.translator.lang] || raw.title.en,
                  text:  raw.text[this.translator.lang]  || raw.text.en  }
              : raw;
            document.getElementById("aiv-story-title").textContent = story.title;
            document.getElementById("aiv-content").textContent = story.text;
```

`KidAIEngine.retrieveStory()` `content.json` formatidagi obyekt qaytaradi (`title`, `text` — multilingual object). `getAiStory()` esa `{title: string, text: string}` qaytaradi. Yuqoridagi kod ikkalasini ham qabul qiladi.

- [ ] **Step 2: `KidAIEngine` init ni `storyManager.load().then()` ga qo'shing**

Fayl: `app/src/main/assets/www/index.html`, init qismida.

`old_string`:
```js
    app.storyManager.load().then(() => app.storyManager.render());
```

`new_string`:
```js
    app.storyManager.load().then(() => {
      app.storyManager.render();
      window.kidAI = new KidAIEngine(app.storyManager.items, GAMES);
    });
```

- [ ] **Step 3: Tekshirish**

Ilovani oching:
1. "Ertaklar" tabiga o'ting → "Ertak To'qish" tugmasini bosing
2. AI Viewer ochilishi, `content.json` dan ertak ko'rinishi kerak (hardcoded 3 ta emas, 20 ta orasidan)
3. Bir necha marta bosing — har safar boshqa ertak chiqishi kerak

- [ ] **Step 4: Commit**

```bash
git add app/src/main/assets/www/main.js app/src/main/assets/www/index.html
git commit -m "feat(kidai): wire generateAiStory() and init to KidAIEngine"
```

---

## Task 6: Manual QA

KidAI tabida quyidagi stsenariylarni sinab ko'ring:

- [ ] **Salomlashuv:** `salom` yozing → greeting + 3 ta chip chiqadi
- [ ] **Ertak:** `hayvonlar haqida ertak ayt` → animals kategoriyasidan ertak snippet
- [ ] **"To'liq o'qi" chip:** bosganda ai-viewer ochilishi va to'liq ertak ko'rinishi
- [ ] **"Boshqa ertak" chip:** yangi ertak snippet chiqishi
- [ ] **O'yin:** `matematika o'yini ko'rsat` → math kategoriyasidan o'yin tavsiyasi
- [ ] **"O'yna!" chip:** bosganda o'yin ochilishi
- [ ] **Savol:** `sher haqida nima bilasan` → hayvonlar faktidan biri
- [ ] **Noma'lum input:** `dfhsdkfh` → fallback + default chips
- [ ] **Bo'sh input:** send bosish → hech narsa bo'lmasligi
- [ ] **Til almashtirish:** RU ga o'ting → KidAI javobi rus tilida chiqishi
- [ ] **`generateAiStory()`:** AI Studio da "Ertak To'qish" → content.json dan ertak
- [ ] **Enter tugmasi:** input maydonda Enter → xabar yuborilishi

---

## Spec Coverage Check

| Spec talab | Task |
|-----------|------|
| KidAIEngine klassi | Task 1 |
| respond(text, lang) | Task 1 |
| detectIntent — 5 niyat | Task 1 |
| retrieveStory — category RAG | Task 1 |
| retrieveGame — cat qidiruv | Task 1 |
| retrieveFact — 6 mavzu | Task 1 |
| 3 tildagi templates | Task 1 |
| ResponseObject format | Task 1 |
| UIManager switchTab + kidai | Task 2 |
| KidAI tab tugmasi | Task 3 |
| Chat panel HTML | Task 3 |
| Chat CSS (bubbles, typing, chips) | Task 3 |
| kaiSend, kaiAppendMsg, kaiChipTap | Task 4 |
| kaiInit — greeting on first open | Task 4 |
| Tarjima kalitlari (3 til) | Task 4 |
| generateAiStory() → KidAIEngine | Task 5 |
| window.kidAI init after stories load | Task 5 |
| Bo'sh input ignored | Task 4 (kaiSend checks) |
| 200 belgi cheklovi | Task 1 (respond slices) |
| kidai.js script tegi | Task 4 |
