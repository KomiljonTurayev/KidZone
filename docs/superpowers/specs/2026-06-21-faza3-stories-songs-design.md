# Faza 3: Stories & Songs — Dizayn Spesifikatsiyasi

**Sana:** 2026-06-21
**Holat:** Approved

---

## Maqsad

WebView dagi KidAI tabni Songs tab bilan almashtirish va Songs funksionalligini qo'shish.
Stories feature allaqachon katta qismida implementatsiya qilingan — faqat Songs va placeholder MP3 fayllar qolgan.

---

## Joriy holat (implementatsiya qilingan)

- `#stories-section` HTML: kategori chiplar, qidiruv, grid — tayyor
- `AudioPlayer` class, `ContentManager` class, `StoryManager` class — tayyor
- `#kz-player` sticky audio player HTML — tayyor
- `content.json` — stories massivi (20 ta ertak) — tayyor
- `audio/stories/{uz,ru,en}/` papkalar — tayyor (bo'sh, placeholder kerak)

---

## O'zgarish: Tab almashtirish

```
Hozir:  [ 🎮 O'yinlar ] [ 📖 Ertaklar ] [ 🤖 KidAI ]
Keyin:  [ 🎮 O'yinlar ] [ 📖 Ertaklar ] [ 🎵 Qo'shiqlar ]
```

KidAI native Compose (KidzoSheet FAB) ga ko'chirilgan — WebView tab endi keraksiz.

---

## Arxitektura

Faqat HTML5/JS layer o'zgaradi. Native Kotlin/Compose qismga tegish yo'q.

```
assets/www/
├── content.json          ← songs massivi qo'shiladi (20 ta)
├── index.html            ← Songs tab, songs-section HTML, KidAI o'chiriladi
├── main.js               ← SongManager class, switchTab va validTabs update
└── audio/
    ├── stories/
    │   ├── uz/story-001.mp3 … story-020.mp3   ← 20 silent placeholder
    │   ├── ru/story-001.mp3 … story-020.mp3   ← 20 silent placeholder
    │   └── en/story-001.mp3 … story-020.mp3   ← 20 silent placeholder
    └── songs/
        ├── uz/song-001.mp3 … song-020.mp3     ← 20 silent placeholder
        ├── ru/song-001.mp3 … song-020.mp3     ← 20 silent placeholder
        └── en/song-001.mp3 … song-020.mp3     ← 20 silent placeholder
```

---

## content.json — Songs massivi

20 ta qo'shiq, 5 ta kategoriya:

| Kategoriya | Count | Misol |
|---|---|---|
| `lullaby` (allalar) | 4 | 🌙 Alla, ⭐ Yoqimli Tush |
| `alphabet` (alifbo) | 4 | 🔤 Alifbo Qo'shig'i, 🎵 ABC |
| `animals` (hayvonlar) | 4 | 🐄 Mol Bola, 🐸 Baqa |
| `dance` (raqs) | 4 | 💃 Chalg'i, 🥁 Doira |
| `games` (o'yin) | 4 | 🎈 Sharlar, 🎪 Sirk |

Har bir qo'shiq:
```json
{
  "id": "song-001",
  "category": "lullaby",
  "emoji": "🌙",
  "title": { "uz": "Alla", "ru": "Колыбельная", "en": "Lullaby" },
  "audio": {
    "uz": "audio/songs/uz/song-001.mp3",
    "ru": "audio/songs/ru/song-001.mp3",
    "en": "audio/songs/en/song-001.mp3"
  }
}
```

`text` maydoni yo'q (qo'shiqlar faqat audio, matn yo'q).

---

## index.html o'zgarishlari

### 1. Tab button

```html
<!-- O'CHIRILADI: -->
<button class="kz-tab" id="tab-kidai" onclick="switchTab('kidai')">🤖 <span id="tab-lbl-kidai">KidAI</span></button>

<!-- QO'SHILADI: -->
<button class="kz-tab" id="tab-songs" onclick="switchTab('songs')">🎵 <span id="tab-lbl-songs">Qo'shiqlar</span></button>
```

### 2. Songs section HTML (stories-section bilan bir xil pattern)

```html
<div id="songs-section" class="h">
  <div class="cc-cats" id="songs-cats">
    <button class="cc-chip active" onclick="songFilter(document.getElementById('songs-search').value,'all',this)">🎯 <span id="sqcat-all">Hammasi</span></button>
    <button class="cc-chip" onclick="songFilter(document.getElementById('songs-search').value,'lullaby',this)">🌙 <span id="sqcat-lullaby">Allalar</span></button>
    <button class="cc-chip" onclick="songFilter(document.getElementById('songs-search').value,'alphabet',this)">🔤 <span id="sqcat-alphabet">Alifbo</span></button>
    <button class="cc-chip" onclick="songFilter(document.getElementById('songs-search').value,'animals',this)">🐾 <span id="sqcat-animals">Hayvonlar</span></button>
    <button class="cc-chip" onclick="songFilter(document.getElementById('songs-search').value,'dance',this)">💃 <span id="sqcat-dance">Raqs</span></button>
    <button class="cc-chip" onclick="songFilter(document.getElementById('songs-search').value,'games',this)">🎈 <span id="sqcat-games">O'yin</span></button>
  </div>
  <div class="cs-search-wrap">
    <input class="cs-search" type="text" id="songs-search"
           placeholder="🔍 Qo'shiq qidirish..."
           oninput="clearTimeout(window._sqTs);window._sqTs=setTimeout(()=>{var v=this.value;document.getElementById('songs-search-clear').classList.toggle('h',!v);songFilter(v,window._songCat||'all')},180)">
    <button class="cs-clear h" id="songs-search-clear" onclick="clearSongSearch()">✕</button>
  </div>
  <div class="cs-count" id="songs-count"></div>
  <div class="content-grid" id="songs-grid"></div>
</div>
```

### 3. KidAI section va kidai.js o'chiriladi

`#kidai-section` HTML bloki va `<script src="kidai.js"></script>` qatori o'chiriladi.

### 4. CSS qo'shimchasi

```css
#songs-section { padding-top: 10px; }
#songs-section.h { display: none; }
```

### 5. i18n o'zgarishlari (T ob'ekti)

Har 3 tilda:
- `kidai*` kalitlari o'chiriladi
- `tabSongs`, qidirish placeholder, kategoriya nomlari qo'shiladi

```js
// Uzbek misol:
tabSongs: "Qo'shiqlar", songSearch: "Qo'shiq qidirish...",
sqCatAll: "Hammasi", sqCatLullaby: "Allalar",
sqCatAlphabet: "Alifbo", sqCatAnimals: "Hayvonlar",
sqCatDance: "Raqs", sqCatGames: "O'yin"
```

---

## main.js o'zgarishlari

### 1. SongManager class

```js
class SongManager extends ContentManager {
    constructor(player, translator, ui) {
        super('songs', player, translator, ui);
    }

    _play(item) {
        const lang = this.translator.lang;
        const title = item.title[lang] || item.title.en;
        const src = item.audio ? (item.audio[lang] || item.audio.en) : null;
        if (!src) return;
        this.currentId = item.id;
        this.render();
        this._showPlayer(item.emoji + ' ' + title);
        this.player.play(
            src, title,
            (cur, dur) => this._onTimeUpdate(cur, dur),
            () => this._onEnded(),
            () => { this._hidePlayer(); this.currentId = null; this.render(); }
        );
        const playBtn = document.getElementById('kzp-play');
        if (playBtn) playBtn.textContent = '⏸';
    }
}
```

### 2. UIManager.switchTab update

```js
// O'ZGARADI:
['games', 'stories', 'kidai'].forEach(...)
// →
['games', 'stories', 'songs'].forEach(...)
```

### 3. App initialization

```js
// QO'SHILADI:
app.songManager = new SongManager(audioPlayer, translator, ui);
app.songManager.load().then(() => app.songManager.render());

// O'CHIRILADI:
// window.kidAI = new KidAIEngine(...)
```

### 4. validTabs update

```js
// O'ZGARADI:
const validTabs = ['games', 'stories', 'kidai'];
// →
const validTabs = ['games', 'stories', 'songs'];
```

### 5. Global helper funksiyalari (index.html dan chaqiriladi)

```js
function songFilter(query, cat, chipEl) { ... }  // storyFilter bilan parallel
function clearSongSearch() { ... }
```

---

## Placeholder MP3 fayllar

Jami 120 ta fayl (20 × 3 × 2):
- `audio/stories/{uz,ru,en}/story-001.mp3` … `story-020.mp3`
- `audio/songs/{uz,ru,en}/song-001.mp3` … `song-020.mp3`

**Usul:** PowerShell bilan minimal valid MP3 binary yaratiladi va 120 ta joyga nusxalanadi.

Minimal 1-sekundlik jim MP3 — MPEG1 Layer3, 8kHz, mono, 8kbps. Base64:
```
//uQxAAAAAAAAAAAAAAAAAAAAAAASW5mbwAAAA8AAAACAAACcQCAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICA
```

PowerShell qismi (plan da to'liq kod beriladi):
```powershell
$bytes = [Convert]::FromBase64String("<base64>")
foreach ($lang in @('uz','ru','en')) {
    foreach ($i in 1..20) {
        $name = "story-{0:D3}.mp3" -f $i
        [IO.File]::WriteAllBytes("audio/stories/$lang/$name", $bytes)
    }
}
```

Haqiqiy MP3 fayllar tayyor bo'lgach, to'g'ridan-to'g'ri almashtirish — kod o'zgartirilmaydi.

---

## Global funksiyalar (index.html inline script)

Stories bilan parallel:

```js
function storyFilter(query, cat, chipEl) { ... }  // allaqachon bor
function songFilter(query, cat, chipEl) { app.songManager?.filter(query, cat); ... }
function clearSongSearch() { ... }
```

---

## Muvaffaqiyat mezonlari

- [ ] Songs tab bosilganda `#songs-section` ko'rinadi
- [ ] 20 ta qo'shiq kartasi grid da ko'rinadi
- [ ] Kategoriya chip bilan filtrlash ishlaydi
- [ ] Qidiruv real-time ishlaydi
- [ ] Karta bosilganda `#kz-player` ko'rinadi, audio yuklanadi
- [ ] Play/Pause/Stop ishlaydi
- [ ] Til o'zgarganda audio to'xtaydi va yangi tilda qayta yuklanadi
- [ ] KidAI tab ko'rinmaydi
- [ ] `kz-tab` localStorage 'songs' ni saqlab qoladi
