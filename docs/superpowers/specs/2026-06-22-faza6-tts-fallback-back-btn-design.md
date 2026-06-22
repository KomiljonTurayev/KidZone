# Faza 6: Song TTS Fallback + Android Back Button Fix — Dizayn Spesifikatsiyasi

**Sana:** 2026-06-22
**Holat:** Approved

---

## Maqsad

1. **Song TTS Fallback:** Qo'shiq bosilganda MP3 yuklanmasa (0-byte placeholder) — "Audio unavailable" toast o'rniga TTS avtomatik `#lv-lyrics` matnini o'qiydi.
2. **Android Back Button:** `BackHandler` faqat `#gv` (game viewer) ni biladi. `#lyrics-viewer` va `#ai-viewer` ochiq bo'lsa Back bosilganda exit dialog chiqadi — bu bug. Uchala overlayni ham tekshirish kerak.

---

## Joriy holat

### Song audio fail flow
`SongManager._play()` → `super._play(item)` (`ContentManager._play()`) → `player.play(src, ...)` → MP3 0-byte → error callback → `this.ui.showToast('Audio unavailable')`.

`ContentManager._play()` error callback (line ~286):
```javascript
() => {
    this.ui.showToast(this.translator.get('noAudio') || 'Audio unavailable');
    this._hidePlayer();
    this.currentId = null;
    this.render();
}
```

### BackHandler (MainScreen.kt line 178-192)
```kotlin
BackHandler {
    if (uiState.isLocked) return@BackHandler
    val mgr = webMgrRef.value
    webMgrRef.value?.evaluateJavascript(
        "document.getElementById('gv') ? !document.getElementById('gv').classList.contains('h') : false"
    ) { result ->
        if (result == "true") {
            mainViewModel.showExitDialog(true)
        } else if (mgr?.canGoBack() == true) {
            mgr.goBack()
        } else {
            mainViewModel.showExitDialog(false)
        }
    }
}
```

---

## Fix 1 — Song TTS Fallback (`main.js`)

### Yondashuv: `_onAudioError()` hook pattern

`ContentManager` ga `_onAudioError()` virtual metod qo'shiladi. Subklasslar override qilib default xatti-harakatni o'zgartirishi mumkin.

#### ContentManager o'zgarishi

`ContentManager._play()` error callback:

**Hozirgi:**
```javascript
() => {
    this.ui.showToast(this.translator.get('noAudio') || 'Audio unavailable');
    this._hidePlayer();
    this.currentId = null;
    this.render();
}
```

**Yangi:**
```javascript
() => {
    this._onAudioError();
    this._hidePlayer();
    this.currentId = null;
    this.render();
}
```

`ContentManager` ga yangi metod (default — toast ko'rsatadi):
```javascript
_onAudioError() {
    this.ui.showToast(this.translator.get('noAudio') || 'Audio unavailable');
}
```

#### SongManager o'zgarishi

`SongManager` da ikkita yangi metod qo'shiladi:

```javascript
_onAudioError() {
    const text = document.getElementById('lv-lyrics')?.textContent;
    if (text) this._speakLyrics(text, this.translator.lang);
}

_speakLyrics(text, lang) {
    if (!window.speechSynthesis) return;
    window.speechSynthesis.cancel();
    const langMap = { uz: 'uz-UZ', ru: 'ru-RU', en: 'en-US' };
    const targetLang = langMap[lang] || 'en-US';
    const voices = window.speechSynthesis.getVoices();
    const voice = voices.find(v => v.lang === targetLang)
               || voices.find(v => v.lang.startsWith(targetLang.split('-')[0]))
               || (lang === 'uz' ? voices.find(v => v.lang.startsWith('ru')) : null)
               || null;
    const msg = new SpeechSynthesisUtterance(text);
    if (voice) msg.voice = voice;
    msg.lang = voice ? voice.lang : targetLang;
    msg.rate = 0.9;
    window.speechSynthesis.speak(msg);
}
```

**Voice selection:** `App._doSpeak()` bilan bir xil mantiq — `uz-UZ` → `uz` prefix → `ru` fallback → null. Rate 0.9 (story uchun 0.86, qo'shiq uchun ozroq tezroq).

**Toast:** `SongManager._onAudioError()` ichida toast yo'q — TTS o'zi audio sifatida ishlaydi.

**Null-safe:** `document.getElementById('lv-lyrics')?.textContent` — lyrics viewer ochiq bo'lmasa (text null) → TTS chaqirilmaydi, `_hidePlayer()` bajariladi.

---

## Fix 2 — Android Back Button (`MainScreen.kt`)

### Yangi `evaluateJavascript` skripti

```javascript
(function(){
    var lv = document.getElementById('lyrics-viewer');
    if (lv && !lv.classList.contains('h')) { closeLyrics(); return 'overlay'; }
    var ai = document.getElementById('ai-viewer');
    if (ai && !ai.classList.contains('h')) { closeAi(); return 'overlay'; }
    var gv = document.getElementById('gv');
    return (gv && !gv.classList.contains('h')) ? 'game' : 'none';
})()
```

**Tekshirish tartibi:** lyrics-viewer → ai-viewer → gv. Birinchi ochiq overlay yopiladi.

### Kotlin callback

```kotlin
) { result ->
    when (result?.trim('"')) {
        "overlay" -> { /* JS da yopildi — hech narsa qilish shart emas */ }
        "game" -> mainViewModel.showExitDialog(true)
        else -> if (mgr?.canGoBack() == true) { mgr.goBack() } else { mainViewModel.showExitDialog(false) }
    }
}
```

`result?.trim('"')` — `evaluateJavascript` callback Kotlin string ga tırnak belgilari bilan keladi: `"\"overlay\""` → `trim('"')` → `"overlay"`.

---

## Fayl O'zgarishlari

| Fayl | Nima |
|------|------|
| `app/src/main/assets/www/main.js` | `ContentManager._onAudioError()` + error callback refactor; `SongManager._onAudioError()` + `_speakLyrics()` |
| `app/src/main/java/uz/kidzone/app/ui/MainScreen.kt` | BackHandler `evaluateJavascript` skripti + `when` callback |

---

## Muvaffaqiyat mezonlari

- [ ] Qo'shiq bosilganda "Audio unavailable" toast **ko'rinmaydi** (0-byte MP3 bilan)
- [ ] Qo'shiq bosilganda TTS lyrics matnini o'qiydi (UZ/RU/EN tilida)
- [ ] `#lyrics-viewer` ochiq + Back → viewer yopiladi (toast/exit dialog yo'q)
- [ ] `#ai-viewer` ochiq + Back → viewer yopiladi
- [ ] O'yin ochiq + Back → "O'yindan chiqish?" dialog (avvalgidek)
- [ ] Hech narsa ochiq + Back → "KidZone'dan chiqish?" dialog (avvalgidek)
- [ ] Build `BUILD SUCCESSFUL`, JS xatosi yo'q
