'use strict';
/* KidUI — injects a Toca Boca header into any game page.
   Call KidUI.init() after DOM is ready (or at top of <body>).
   Depends on: kids-theme.css being linked in <head>.
   Optional: if KZL is loaded, reads persisted level from it.
*/
const KidUI = (() => {
  const KT = {
    uz: { level: 'Daraja' },
    ru: { level: 'Уровень' },
    en: { level: 'Level' }
  };

  let _lang = 'en';

  /* Public API */
  function init({ title, icon, color, gameId, lang }) {
    _lang  = lang  || 'en';
    const lv = (typeof KZL !== 'undefined' && gameId)
               ? KZL.getLevel(gameId)
               : 1;

    const hdr = document.createElement('header');
    hdr.id = 'kt-header';
    hdr.style.background = color || '#FF6B35';
    hdr.innerHTML =
      `<button class="kt-back" onclick="history.back()">&#8592;</button>` +
      `<span class="kt-title">${icon} ${title}</span>` +
      `<span class="kt-lv" id="kt-lv">${_t('level')} ${lv}</span>`;

    /* Prepend to body so it sits above the flex children */
    document.body.prepend(hdr);
  }

  function updateLevel(n) {
    const el = document.getElementById('kt-lv');
    if (el) el.textContent = `${_t('level')} ${n}`;
  }

  function _t(k) {
    return (KT[_lang] || KT.en)[k];
  }

  return { init, updateLevel };
})();
