// app/src/main/assets/www/kidai.js

const _KAI_FACT_DB = {
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

const _KAI_GENERIC_FACT = {
  uz: "🌍 Hayvonlar, ranglar, sayyoralar, sonlar, shakllar yoki mevalar haqida so'ra!",
  ru: '🌍 Спроси меня о животных, цветах, планетах, числах, формах или фруктах!',
  en: '🌍 Ask me about animals, colors, planets, numbers, shapes or fruits!'
};

const _KAI_GENERIC_CHIPS = {
  uz: ['🦁 Hayvonlar', '🌈 Ranglar', '🪐 Sayyoralar'],
  ru: ['🦁 Животные', '🌈 Цвета', '🪐 Планеты'],
  en: ['🦁 Animals', '🌈 Colors', '🪐 Planets']
};

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

  // lang is accepted for API consistency; topic-based filtering is language-agnostic
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
                 'game','play'],
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
    const storyText = `📖 ${story.emoji} **${title}**\n\n"${snippet}..."`;
    const CHIPS = {
      uz: ["📖 To'liq o'qi", '🔄 Boshqa ertak', "🎮 O'yin"],
      ru: ['📖 Читать полностью', '🔄 Другую сказку', '🎮 Игра'],
      en: ['📖 Read full', '🔄 Another story', '🎮 Game']
    };
    return { type:'story', text:storyText, storyId:story.id, chips:CHIPS[lang]||CHIPS.en };
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

    for (const [, data] of Object.entries(_KAI_FACT_DB)) {
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

    return { type:'fact', text:_KAI_GENERIC_FACT[lang]||_KAI_GENERIC_FACT.en, chips:_KAI_GENERIC_CHIPS[lang]||_KAI_GENERIC_CHIPS.en };
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
