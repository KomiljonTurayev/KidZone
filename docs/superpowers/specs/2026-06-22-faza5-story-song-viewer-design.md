# Faza 5: Story Viewer Fix + Song Lyrics Viewer — Dizayn Spesifikatsiyasi

**Sana:** 2026-06-22
**Holat:** Approved

---

## Maqsad

1. **Bug fix:** `#ai-viewer` overlay KidzoAI uchun ham, content.json ertaklari uchun ham ishlatiladi — "🔄 Yangi ertak" AI regen tugmasi ertak ko'rishda ham ko'rinadi. Foydalanuvchi uni bossa avvalgi ertakni o'chirib AI bilan yangi ertak generatsiya qiladi.
2. **Yangi feature:** Qo'shiqlar (`songs` array) ning `text` maydoni (lyrics) hech qayerda ko'rsatilmaydi. Qo'shiq bosilganda faqat audio player bar chiqadi — lyrics panel yo'q.

---

## Joriy holat

- `#ai-viewer` overlay — `position:fixed; inset:0; z-index:350` — to'liq ekran overlay.
- `StoryManager._play(item)` (main.js line 363) `#ai-viewer`ni ochadi, matn + TTS ishlatadi ✅
- `generateAiStory()` (main.js line 647) ham `#ai-viewer`ni ochadi, AI generatsiya + TTS ✅
- Ikkala holat ham `#ai-viewer` ochib beradi — lekin "Yangi ertak" tugmasi har ikkalasida ko'rinadi ❌
- `SongManager._play(item)` (main.js line 404) faqat badge hook + `super._play()` ✅ (audio player)
- Songs `content.json` da `text` (lyrics) maydoni bor: `item.text[lang]` ✅ — lekin ko'rsatilmaydi ❌

---

## Fix 1 — "Yangi ertak" tugmasini default yashirish

### Muammo
```html
<!-- Hozirgi holat: har doim ko'rinadi -->
<button class="aiv-play-btn" style="background:#4CAF50" onclick="generateAiStory()">
    <span>🔄</span> <span id="ai-regen-label">Yangi ertak</span>
</button>
```

### Yechim
Regen tugmasiga `id="aiv-regen-btn"` va default `h` class qo'shish:
```html
<button id="aiv-regen-btn" class="aiv-play-btn h" style="background:#4CAF50" onclick="generateAiStory()">
    <span>🔄</span> <span id="ai-regen-label">Yangi ertak</span>
</button>
```

`generateAiStory()` (main.js) — viewer ochilishidan oldin regen tugmasini ko'rsatish:
```javascript
generateAiStory() {
    const viewer = document.getElementById("ai-viewer");
    const loading = document.getElementById("aiv-loading");
    const content = document.getElementById("aiv-content-wrap");
    document.getElementById('aiv-regen-btn').classList.remove('h'); // <-- ADD THIS
    viewer.classList.remove("h");
    // ... qolgan kod o'zgarishsiz
}
```

`StoryManager._play()` — hech qanday o'zgarish kerak emas (tugma default yashirin).

---

## Feature — Song Lyrics Viewer

### Qanday ishlaydi
1. Foydalanuvchi qo'shiq kartasini bosadi
2. `SongManager._play(item)` chaqiriladi
3. `#lyrics-viewer` overlay ochiladi — qo'shiq sarlavhasi + so'zlari (scrollable)
4. Audio player bar (pastki) ham ko'rinadi — `super._play()` dan
5. ✕ tugmasi — `closeLyrics()` — overlayni yopadi

### Yangi HTML (`#lyrics-viewer`)

```html
<!-- SONG LYRICS VIEWER -->
<div id="lyrics-viewer" class="h">
  <div class="aiv-hdr">
    <button class="ni-pill" style="width:40px;background:#F0F0F0" onclick="closeLyrics()">✕</button>
    <div style="color:var(--text);flex:1;text-align:center;font-weight:800" id="lv-title">🎵</div>
    <div style="width:40px"></div>
  </div>
  <div class="aiv-body">
    <div class="aiv-story-title" id="lv-song-title"></div>
    <pre class="lv-lyrics" id="lv-lyrics"></pre>
  </div>
</div>
```

### Yangi CSS

```css
#lyrics-viewer {
  position:fixed; inset:0; z-index:350; background:var(--bg);
  display:flex; flex-direction:column; animation:shUp 0.4s var(--spring);
}
#lyrics-viewer.h { display:none }
.lv-lyrics {
  font-size:16px; color:var(--text); font-family:'Nunito',sans-serif;
  white-space:pre-wrap; line-height:2; margin:0;
}
```

`.aiv-hdr` va `.aiv-body` klasslari mavjud — qayta ishlatiladi (yangi CSS shart emas).

### `SongManager._play()` o'zgarishi (main.js)

```javascript
_play(item) {
    if (window.badgeManager) badgeManager.onSongPlayed();
    const lang = this.translator.lang;
    const title = item.title[lang] || item.title.en;
    const text = item.text ? (item.text[lang] || item.text.en || '') : '';
    if (text) {
        document.getElementById('lv-title').textContent = item.emoji + ' ' + title;
        document.getElementById('lv-song-title').textContent = item.emoji + ' ' + title;
        document.getElementById('lv-lyrics').textContent = text;
        document.getElementById('lyrics-viewer').classList.remove('h');
    }
    super._play(item);
}
```

### `closeLyrics()` (index.html)

```javascript
function closeLyrics() {
    document.getElementById('lyrics-viewer').classList.add('h');
}
```

---

## Fayl O'zgarishlari

| Fayl | Nima |
|------|------|
| `app/src/main/assets/www/index.html` | regen btn `id` + `h` class; `#lyrics-viewer` HTML; `.lv-lyrics` CSS; `#lyrics-viewer` CSS; `closeLyrics()` |
| `app/src/main/assets/www/main.js` | `generateAiStory()` — regen btn show; `SongManager._play()` — lyrics show |

---

## Muvaffaqiyat mezonlari

- [ ] Content.json ertak bosilganda "Yangi ertak" tugmasi ko'rinmaydi
- [ ] KidzoAI "Ertak To'qish" bosilganda "Yangi ertak" tugmasi ko'rinadi
- [ ] Qo'shiq bosilganda `#lyrics-viewer` ochiladi — sarlavha + so'zlar ko'rinadi
- [ ] So'zlar 3 tilda ishlaydi (UZ/RU/EN) — til o'zgartirsa re-render yo'q (yangi bosish kerak)
- [ ] ✕ tugmasi lyrics viewerni yopadi
- [ ] Audio player bar qo'shiq uchun ishlaydi (lyrics bilan parallel)
- [ ] Build muvaffaqiyatli — JS xatosi yo'q
