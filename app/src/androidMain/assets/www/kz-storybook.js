/* KZBooks — illustrated story books from the backend (GET /stories), shown as a
 * narrated slideshow: one picture per page with a slow pan/zoom (Ken Burns), the
 * page text underneath, read aloud page by page and turning the page when the
 * narration ends.
 *
 * Books are open-licensed (StoryWeaver, CC BY 4.0) and imported on the backend
 * with tools/import_storybook.py; the last slide shows their attribution, which
 * the licence requires.
 *
 * Narration per page: the page's recorded audio when it has one, otherwise
 * AndroidBridge.speakText (Uzbek goes through the backend /ai/tts voice), otherwise
 * the browser's speechSynthesis, otherwise a reading-time timer.
 *
 * main.js forwards window.onNativeSpeechEnd/Error here first (KZBooks.onSpeechEnd).
 */
(function () {
  // Keep in sync with BackendConfig.BASE_URL (Kotlin).
  const BACKEND = 'https://kidzone-backend-s7to.onrender.com';
  const LIST_TIMEOUT_MS = 25000;   // Render free tier can take ~20 s to wake up

  const T = {
    uz: { read: "O'qib berish", pause: 'Pauza', end: 'Tugadi! 🎉', again: '🔁 Yana', close: '✕ Yopish',
          source: 'Manba', book: 'Rasmli kitob', offline: "Kitobni ochib bo'lmadi. Internetni tekshiring." },
    ru: { read: 'Читать', pause: 'Пауза', end: 'Конец! 🎉', again: '🔁 Ещё раз', close: '✕ Закрыть',
          source: 'Источник', book: 'Книжка с картинками', offline: 'Не удалось открыть книгу. Проверьте интернет.' },
    en: { read: 'Read to me', pause: 'Pause', end: 'The end! 🎉', again: '🔁 Again', close: '✕ Close',
          source: 'Source', book: 'Picture book', offline: "Couldn't open the book. Check the internet." },
  };
  const tr = (lang) => T[lang] || T.en;

  function timeoutFetch(url, ms) {
    const ctl = new AbortController();
    const t = setTimeout(() => ctl.abort(), ms);
    return fetch(url, { signal: ctl.signal }).finally(() => clearTimeout(t));
  }

  function cacheGet(key) {
    try { return JSON.parse(localStorage.getItem(key) || 'null'); } catch (_) { return null; }
  }
  function cacheSet(key, val) {
    try { localStorage.setItem(key, JSON.stringify(val)); } catch (_) { /* quota: fine */ }
  }

  const abs = (path) => (path ? BACKEND + path : null);

  /** Cached list right away (may be empty), then the fresh one via onFresh. */
  function list(lang, onFresh) {
    const key = 'kz-books-' + lang;
    const cached = cacheGet(key) || [];
    timeoutFetch(BACKEND + '/stories?lang=' + encodeURIComponent(lang), LIST_TIMEOUT_MS)
      .then(r => (r.ok ? r.json() : Promise.reject(r.status)))
      .then(books => {
        if (!Array.isArray(books)) return;
        cacheSet(key, books);
        if (JSON.stringify(books) !== JSON.stringify(cached)) onFresh && onFresh(books);
      })
      .catch(() => { /* offline or asleep: keep the cached list */ });
    return cached;
  }

  function getBook(id) {
    const key = 'kz-book-' + id;
    return timeoutFetch(BACKEND + '/stories/' + encodeURIComponent(id), LIST_TIMEOUT_MS)
      .then(r => (r.ok ? r.json() : Promise.reject(r.status)))
      .then(b => { cacheSet(key, b); return b; })
      .catch(err => cacheGet(key) || Promise.reject(err));
  }

  /* ───────────────────────── viewer ───────────────────────── */

  let st = null;   // {book, lang, idx, auto, el, timer, audio, speaking}

  const css = document.createElement('style');
  css.textContent = `
#kzb{position:fixed;inset:0;z-index:9000;background:#0F0A1E;color:#fff;display:flex;flex-direction:column;
  font-family:'Nunito','KZ Cyrillic',sans-serif;user-select:none;-webkit-user-select:none}
#kzb .kzb-top{display:flex;align-items:center;gap:8px;padding:10px 12px;padding-top:max(10px,env(safe-area-inset-top))}
#kzb .kzb-title{flex:1;font-weight:900;font-size:16px;overflow:hidden;white-space:nowrap;text-overflow:ellipsis}
#kzb button{border:0;border-radius:999px;font-weight:900;cursor:pointer;font-family:inherit}
#kzb .kzb-x{width:40px;height:40px;background:rgba(255,255,255,.14);color:#fff;font-size:18px}
#kzb .kzb-stage{position:relative;flex:1;overflow:hidden;margin:0 12px;border-radius:22px;background:#1E1638}
#kzb .kzb-img{position:absolute;inset:0;width:100%;height:100%;object-fit:contain;transform-origin:50% 50%;
  opacity:0;transition:opacity .6s ease}
#kzb .kzb-img.on{opacity:1}
#kzb .kzb-img.kb0{animation:kzbKb0 14s ease-in-out forwards}
#kzb .kzb-img.kb1{animation:kzbKb1 14s ease-in-out forwards}
#kzb .kzb-img.kb2{animation:kzbKb2 14s ease-in-out forwards}
@keyframes kzbKb0{from{transform:scale(1) translate(0,0)}to{transform:scale(1.14) translate(-3%,-2%)}}
@keyframes kzbKb1{from{transform:scale(1.14) translate(3%,2%)}to{transform:scale(1) translate(0,0)}}
@keyframes kzbKb2{from{transform:scale(1.04) translate(-3%,0)}to{transform:scale(1.14) translate(3%,-1%)}}
#kzb .kzb-text{margin:10px 12px 0;padding:14px 16px;min-height:88px;max-height:30vh;overflow:auto;
  background:#fff;color:#2D1B69;border-radius:20px;font-size:19px;line-height:1.45;font-weight:800;white-space:pre-line}
#kzb .kzb-text:empty{visibility:hidden}
#kzb .kzb-bar{display:flex;align-items:center;gap:10px;padding:12px;padding-bottom:max(12px,env(safe-area-inset-bottom))}
#kzb .kzb-nav{width:52px;height:52px;background:rgba(255,255,255,.16);color:#fff;font-size:22px}
#kzb .kzb-nav:disabled{opacity:.3}
#kzb .kzb-play{flex:1;height:52px;background:linear-gradient(135deg,#FF8A00,#FF3D77);color:#fff;font-size:16px}
#kzb .kzb-dots{display:flex;gap:5px;justify-content:center;padding:8px 0 0;flex-wrap:wrap}
#kzb .kzb-dots i{width:7px;height:7px;border-radius:50%;background:rgba(255,255,255,.3)}
#kzb .kzb-dots i.on{background:#FFD54F;width:18px;border-radius:4px}
#kzb .kzb-end{position:absolute;inset:0;display:flex;flex-direction:column;align-items:center;justify-content:center;
  gap:12px;padding:20px;text-align:center;background:#1E1638}
#kzb .kzb-end h2{font-size:30px;margin:0}
#kzb .kzb-end .kzb-btns{display:flex;gap:10px}
#kzb .kzb-end .kzb-btns button{padding:12px 20px;font-size:16px;background:#FFD54F;color:#4A2C00}
#kzb .kzb-credits{font-size:11px;line-height:1.4;color:rgba(255,255,255,.7);max-height:34vh;overflow:auto;white-space:pre-line}
#kzb .kzb-cover{width:150px;height:110px;object-fit:cover;border-radius:16px}`;
  document.head.appendChild(css);

  function el(tag, cls, text) {
    const e = document.createElement(tag);
    if (cls) e.className = cls;
    if (text != null) e.textContent = text;
    return e;
  }

  function open(id, lang) {
    close();
    const t = tr(lang);
    const root = el('div');
    root.id = 'kzb';
    root.innerHTML =
      '<div class="kzb-top"><div class="kzb-title">📖</div><button class="kzb-x" aria-label="close">✕</button></div>' +
      '<div class="kzb-stage"><div style="position:absolute;inset:0;display:flex;align-items:center;justify-content:center;font-size:48px">⏳</div></div>' +
      '<div class="kzb-dots"></div><div class="kzb-text"></div>' +
      '<div class="kzb-bar"><button class="kzb-nav kzb-prev">◀</button><button class="kzb-play"></button>' +
      '<button class="kzb-nav kzb-next">▶</button></div>';
    document.body.appendChild(root);
    st = { book: null, lang, idx: 0, auto: true, el: root, timer: null, audio: null, speaking: false };
    const my = st;
    root.querySelector('.kzb-x').onclick = close;
    root.querySelector('.kzb-prev').onclick = () => { st.auto = false; go(st.idx - 1); };
    root.querySelector('.kzb-next').onclick = () => { st.auto = false; go(st.idx + 1); };
    root.querySelector('.kzb-play').onclick = togglePlay;
    swipe(root.querySelector('.kzb-stage'));

    getBook(id).then(book => {
      if (st !== my) return;
      st.book = book;
      root.querySelector('.kzb-title').textContent = '📖 ' + book.title;
      const dots = root.querySelector('.kzb-dots');
      book.pages.forEach(() => dots.appendChild(el('i')));
      go(0);
    }).catch(() => {
      if (st !== my) return;
      if (window.app && app.ui && app.ui.showToast) app.ui.showToast('📡 ' + t.offline);
      close();
    });
  }

  function go(i) {
    if (!st || !st.book) return;
    stopVoice();
    const pages = st.book.pages;
    if (i < 0) i = 0;
    if (i >= pages.length) { showEnd(); return; }
    st.idx = i;
    const root = st.el, page = pages[i];
    const stage = root.querySelector('.kzb-stage');
    stage.replaceChildren();
    if (page.image) {
      const img = el('img', 'kzb-img kb' + (i % 3));
      img.alt = '';
      img.onload = () => img.classList.add('on');
      img.src = abs(page.image);
      stage.appendChild(img);
    }
    const next = pages[i + 1];
    if (next && next.image) { const pre = new Image(); pre.src = abs(next.image); }
    root.querySelector('.kzb-text').textContent = page.text || '';
    root.querySelector('.kzb-text').scrollTop = 0;
    root.querySelectorAll('.kzb-dots i').forEach((d, n) => d.classList.toggle('on', n === i));
    root.querySelector('.kzb-prev').disabled = i === 0;
    updatePlay();
    if (st.auto) narrate(page);
  }

  function showEnd() {
    stopVoice();
    st.auto = false;
    const t = tr(st.lang), b = st.book, root = st.el;
    st.idx = b.pages.length;
    const end = el('div', 'kzb-end');
    if (b.cover) { const c = el('img', 'kzb-cover'); c.alt = ''; c.src = abs(b.cover); end.appendChild(c); }
    end.appendChild(el('h2', null, t.end));
    const btns = el('div', 'kzb-btns');
    const again = el('button', null, t.again);
    again.onclick = () => { st.auto = true; go(0); };
    const cl = el('button', null, t.close);
    cl.onclick = close;
    btns.append(again, cl);
    end.appendChild(btns);
    const s = b.source || {};
    const credit = [t.source + ': ' + [s.name, s.license].filter(Boolean).join(' · '), s.url, s.credits]
      .filter(Boolean).join('\n\n');
    end.appendChild(el('div', 'kzb-credits', credit));
    root.querySelector('.kzb-stage').replaceChildren(end);
    root.querySelector('.kzb-text').textContent = '';
    root.querySelectorAll('.kzb-dots i').forEach(d => d.classList.remove('on'));
    updatePlay();
  }

  /* ── narration ── */

  function narrate(page) {
    const my = st;
    const done = () => {
      if (st !== my || !st.auto) return;
      st.timer = setTimeout(() => go(st.idx + 1), 1200);
    };
    st.onDone = done;
    const text = (page.text || '').trim();
    if (page.audio) {
      const a = new Audio(abs(page.audio));
      st.audio = a;
      a.onended = done;
      a.onerror = () => speak(text, done);
      a.play().catch(() => speak(text, done));
      return;
    }
    speak(text, done);
  }

  function speak(text, done) {
    if (!text) { st.timer = setTimeout(done, 4000); return; }
    if (window.AndroidBridge && window.AndroidBridge.speakText) {
      st.speaking = true;
      window.AndroidBridge.speakText(text, st.book.lang || st.lang);
      return;   // KZBooks.onSpeechEnd → done
    }
    if (window.speechSynthesis && window.SpeechSynthesisUtterance) {
      const u = new SpeechSynthesisUtterance(text);
      u.lang = { uz: 'uz-UZ', ru: 'ru-RU', en: 'en-US' }[st.book.lang] || 'en-US';
      u.rate = 0.9;
      u.onend = done;
      u.onerror = () => { st.timer = setTimeout(done, readingMs(text)); };
      st.speaking = true;
      window.speechSynthesis.cancel();
      window.speechSynthesis.speak(u);
      return;
    }
    st.timer = setTimeout(done, readingMs(text));
  }

  const readingMs = (text) => Math.max(4000, text.split(/\s+/).length * 450);

  function stopVoice() {
    if (!st) return;
    clearTimeout(st.timer);
    st.onDone = null;
    if (st.audio) { st.audio.onended = st.audio.onerror = null; st.audio.pause(); st.audio = null; }
    if (st.speaking) {
      st.speaking = false;
      if (window.AndroidBridge && window.AndroidBridge.stopSpeaking) window.AndroidBridge.stopSpeaking();
      if (window.speechSynthesis) window.speechSynthesis.cancel();
    }
  }

  function togglePlay() {
    if (!st || !st.book) return;
    if (st.auto) { st.auto = false; stopVoice(); updatePlay(); return; }
    st.auto = true;
    if (st.idx >= st.book.pages.length) { go(0); return; }
    updatePlay();
    narrate(st.book.pages[st.idx]);
  }

  function updatePlay() {
    const t = tr(st.lang);
    st.el.querySelector('.kzb-play').textContent = st.auto ? '⏸ ' + t.pause : '🔊 ' + t.read;
  }

  function swipe(area) {
    let x0 = null;
    area.addEventListener('touchstart', e => { x0 = e.touches[0].clientX; }, { passive: true });
    area.addEventListener('touchend', e => {
      if (x0 == null || !st) return;
      const dx = e.changedTouches[0].clientX - x0;
      x0 = null;
      if (Math.abs(dx) < 50) return;
      st.auto = false;
      go(st.idx + (dx < 0 ? 1 : -1));
    });
  }

  function close() {
    if (!st) return;
    stopVoice();
    st.el.remove();
    st = null;
  }

  /** Called by main.js's native speech callbacks; true when the book handled it. */
  function onSpeechEnd() {
    if (!st || !st.speaking) return false;
    st.speaking = false;
    const done = st.onDone;
    if (done) done();
    return true;
  }

  /** Device TTS failed (e.g. no voice): fall back to a reading-time pause. */
  function onSpeechError() {
    if (!st || !st.speaking) return false;
    st.speaking = false;
    const page = st.book && st.book.pages[st.idx];
    const done = st.onDone;
    if (done) st.timer = setTimeout(done, readingMs((page && page.text) || ''));
    return true;
  }

  window.KZBooks = { list, open, close, onSpeechEnd, onSpeechError, abs, label: (lang) => tr(lang).book,
                     isOpen: () => !!st, pause: () => { if (st && st.auto) togglePlay(); } };
})();
