# Faza 6: Song TTS Fallback + Android Back Button Fix — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Qo'shiq MP3 yuklanmasa TTS avtomatik boshlansin + Android Back tugmasi lyrics/ai-viewer ni yopsin.

**Architecture:** `ContentManager._play()` error callback `_onAudioError()` hook orqali chiqariladi — `SongManager` override qilib TTS chaqiradi. `MainScreen.kt` BackHandler JS skripti uchta overlayni ketma-ket tekshiradi.

**Tech Stack:** Vanilla JavaScript (WebView), Kotlin (Jetpack Compose BackHandler), Android WebView `evaluateJavascript`.

## Global Constraints

- `main.js`: faqat `ContentManager._play()` error callback + `_onAudioError()` default + `SongManager._onAudioError()` + `SongManager._speakLyrics()` o'zgaradi
- `MainScreen.kt`: faqat BackHandler `evaluateJavascript` + callback o'zgaradi
- `index.html` o'zgartirilmaydi
- `SongManager._speakLyrics()` voice selection mantiq: exact → prefix → Russian fallback (uz) → null
- BackHandler JS natija: `'overlay'` | `'game'` | `'none'` (string, quotes bilan qaytadi → `trim('"')`)
- `_onAudioError()` override'da toast chaqirilmaydi — faqat TTS

---

## File Map

| Harakat | Fayl | Nima o'zgaradi |
|---------|------|----------------|
| Modify | `app/src/main/assets/www/main.js:286-291` | error callback → `this._onAudioError()` |
| Modify | `app/src/main/assets/www/main.js:295` | `_onAudioError()` default metod qo'shish |
| Modify | `app/src/main/assets/www/main.js:404-416` | `SongManager._onAudioError()` + `_speakLyrics()` qo'shish |
| Modify | `app/src/main/java/uz/kidzone/app/ui/MainScreen.kt:181-191` | BackHandler JS + Kotlin callback |

---

## Task 1: main.js — ContentManager hook + SongManager TTS

**Files:**
- Modify: `app/src/main/assets/www/main.js`

**Interfaces:**
- Consumes: `ContentManager._play()` (line 273) — mavjud, error callback line 286-291
- Consumes: `window.speechSynthesis` — WebView Speech API
- Consumes: `document.getElementById('lv-lyrics')` — Faza 5 da qo'shilgan, `#lv-lyrics` `<pre>` elementi
- Produces: `ContentManager._onAudioError()` — default toast metod (subklasslar override qila oladi)
- Produces: `SongManager._onAudioError()` — TTS trigger, toast yo'q
- Produces: `SongManager._speakLyrics(text, lang)` — voice selection + speechSynthesis

---

### 1a. ContentManager._play() error callback refactor

`main.js` da `ContentManager._play()` metodni toping (line 273). Error callback line 286-291:

```javascript
            () => {
                this.ui.showToast(this.translator.get('noAudio') || 'Audio unavailable');
                this._hidePlayer();
                this.currentId = null;
                this.render();
            }
```

- [ ] **Step 1: Error callback ni `_onAudioError()` ga ko'chirish**

Yuqoridagi error callback qismini quyidagicha almashtiring:

```javascript
            () => {
                this._onAudioError();
                this._hidePlayer();
                this.currentId = null;
                this.render();
            }
```

---

### 1b. ContentManager._onAudioError() default metod

`_play()` metodi line 295 da `}` bilan tugaydi. Undan keyin `_onTimeUpdate()` boshlanadi (line 297). Ikki metod o'rtasiga (295-297 orasiga) quyidagini qo'shing:

```javascript
    _onAudioError() {
        this.ui.showToast(this.translator.get('noAudio') || 'Audio unavailable');
    }

```

Natija (line 295-300 atrofida):
```javascript
    }                         // _play() tugadi

    _onAudioError() {
        this.ui.showToast(this.translator.get('noAudio') || 'Audio unavailable');
    }

    _onTimeUpdate(cur, dur) {
```

---

### 1c. SongManager._onAudioError() + _speakLyrics() qo'shish

`main.js` da `class SongManager` blokini toping (line 399-417). Hozirgi holat:

```javascript
class SongManager extends ContentManager {
    constructor(player, translator, ui) {
        super('songs', player, translator, ui);
    }

    _play(item) {
        if (window.badgeManager) badgeManager.onSongPlayed();
        const lang = this.translator.lang;
        const title = item.title?.[lang] || item.title?.en || '';
        const text = item.text ? (item.text[lang] || item.text.en || '') : '';
        if (text) {
            document.getElementById('lv-title').textContent = item.emoji + ' ' + title;
            document.getElementById('lv-song-title').textContent = item.emoji + ' ' + title;
            document.getElementById('lv-lyrics').textContent = text;
            document.getElementById('lyrics-viewer').classList.remove('h');
        }
        super._play(item);
    }
}
```

- [ ] **Step 2: `SongManager` ga `_onAudioError()` va `_speakLyrics()` qo'shish**

`_play()` metodidan keyin, yopuvchi `}` dan OLDIN quyidagi ikkita metodni qo'shing:

```javascript
class SongManager extends ContentManager {
    constructor(player, translator, ui) {
        super('songs', player, translator, ui);
    }

    _play(item) {
        if (window.badgeManager) badgeManager.onSongPlayed();
        const lang = this.translator.lang;
        const title = item.title?.[lang] || item.title?.en || '';
        const text = item.text ? (item.text[lang] || item.text.en || '') : '';
        if (text) {
            document.getElementById('lv-title').textContent = item.emoji + ' ' + title;
            document.getElementById('lv-song-title').textContent = item.emoji + ' ' + title;
            document.getElementById('lv-lyrics').textContent = text;
            document.getElementById('lyrics-viewer').classList.remove('h');
        }
        super._play(item);
    }

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
}
```

---

### 1d. Commit

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/www/main.js
git commit -m "feat(faza6): song TTS fallback on audio error, ContentManager._onAudioError hook"
```

---

## Task 2: MainScreen.kt — BackHandler overlay fix

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/ui/MainScreen.kt:181-191`

**Interfaces:**
- Consumes: `closeLyrics()` — Faza 5 da `index.html` ga qo'shilgan global JS funksiya
- Consumes: `closeAi()` — mavjud global JS funksiya (`app.closeAi()` wrapper)
- Consumes: `mainViewModel.showExitDialog(Boolean)` — mavjud ViewModel metod
- Produces: BackHandler yangi xatti-harakat — overlay → yopiladi, o'yin → dialog, boshqa → exit dialog

---

### 2a. BackHandler evaluateJavascript almashtiruv

`MainScreen.kt` da `// BackHandler` kommentini toping (line 177). `webMgrRef.value?.evaluateJavascript(...)` bloki line 181-191 ni toping:

```kotlin
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
```

- [ ] **Step 1: Butun `evaluateJavascript` blokini almashtirish**

Yuqoridagi blokni quyidagicha almashtiring:

```kotlin
        webMgrRef.value?.evaluateJavascript(
            """(function(){
                var lv=document.getElementById('lyrics-viewer');
                if(lv&&!lv.classList.contains('h')){closeLyrics();return 'overlay';}
                var ai=document.getElementById('ai-viewer');
                if(ai&&!ai.classList.contains('h')){closeAi();return 'overlay';}
                var gv=document.getElementById('gv');
                return(gv&&!gv.classList.contains('h'))?'game':'none';
            })()"""
        ) { result ->
            when (result?.trim('"')) {
                "overlay" -> { }
                "game" -> mainViewModel.showExitDialog(true)
                else -> if (mgr?.canGoBack() == true) { mgr.goBack() } else { mainViewModel.showExitDialog(false) }
            }
        }
```

**Eslatma:** `result?.trim('"')` shart — Android `evaluateJavascript` string natijalarni JSON encoded holda qaytaradi: `'overlay'` → `"\"overlay\""`. `trim('"')` tashqi qo'shtirnoqlarni olib tashlaydi.

---

### 2b. Commit

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/ui/MainScreen.kt
git commit -m "fix(faza6): BackHandler closes lyrics/ai-viewer before exit dialog"
```

---

## Task 3: Build, Install va Verify

**Files:** Hech qanday fayl o'zgartirilmaydi

- [ ] **Step 1: Build va install**

```powershell
.\gradlew installDebug
```

Kutilgan natija: `BUILD SUCCESSFUL` + APK qurilmaga o'rnatiladi.

- [ ] **Step 2: Song TTS fallback tekshiruvi**

1. App ochiladi → Qo'shiqlar tabiga o'tish
2. Biror qo'shiq kartasini bosish
3. Lyrics viewer ochiladi ✅
4. Audio player bar pastda ko'rinadi (loading...) — 0-byte MP3 yuklanmaydi
5. "Audio unavailable" toast **ko'rinmasligi** kerak ✅
6. TTS avtomatik boshlanib lyrics matnini o'qishi kerak ✅

- [ ] **Step 3: Back tugmasi — lyrics viewer tekshiruvi**

1. Qo'shiq bosish → lyrics viewer ochiq
2. Android Back tugmasini bosish
3. Lyrics viewer **yopilishi** kerak (exit dialog chiqmasligi kerak) ✅
4. Ana shundan keyin Back bosilsa → "KidZone'dan chiqish?" dialog ✅

- [ ] **Step 4: Back tugmasi — ai-viewer tekshiruvi**

1. Ertak bosish → `#ai-viewer` ochiladi
2. Android Back bosilsin
3. `#ai-viewer` yopilishi kerak ✅
4. Yana Back → "KidZone'dan chiqish?" dialog ✅

- [ ] **Step 5: O'yin Back tugmasi tekshiruvi (regression)**

1. Biror o'yin bosish → o'yin ochiladi (`#gv`)
2. Android Back bosilsin
3. "O'yindan chiqish?" dialog chiqishi kerak ✅ (avvalgidek)

- [ ] **Step 6: Logcat tekshiruvi**

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb logcat -s chromium -d 2>&1 | Select-String "Uncaught|SyntaxError|ReferenceError" | Select-Object -Last 10
```

Kutilgan natija: Hech qanday JS xatosi yo'q.

- [ ] **Step 7: Commit**

```bash
git commit --allow-empty -m "chore(faza6): build verified — TTS fallback + Back btn fix working"
```
