/* KZPhoto — show a bundled real photo for a quiz question, with emoji fallback.
 *
 * Quiz games used to show the same emoji in the question and in the answers,
 * so a child matched two identical pictures instead of recognising the thing.
 * The question now shows a photo; the answers keep their emoji.
 *
 * Photos live in img/<set>/<id>.jpg (open-licensed, from Wikimedia Commons) and
 * img/<set>/credits.json lists every bundled photo: {id: {by, license, src}}.
 * An id that is not in credits.json (no good photo exists, e.g. UFO) shows the
 * emoji, and so does a photo that fails to load.
 *
 *   KZPhoto.show({set:'fruits', id:'apple', emoji:'🍎', box:el, credit:el})
 *   KZPhoto.slug('Fire truck')  // → 'fire-truck'
 */
(function () {
  const credits = {};   // set -> Promise<{id: credit}>

  function loadCredits(set) {
    if (!credits[set]) {
      credits[set] = fetch('img/' + set + '/credits.json')
        .then(r => (r.ok ? r.json() : {}))
        .catch(() => ({}));
    }
    return credits[set];
  }

  let token = 0;

  function show(opts) {
    const box = opts.box, credit = opts.credit;
    const my = ++token;                       // newer question wins
    const fallback = () => {
      if (my !== token) return;
      box.classList.remove('kz-photo');
      box.textContent = opts.emoji;
      if (credit) credit.textContent = '';
    };
    // Don't show the emoji while the photo loads: it is also on one of the
    // answer buttons, so a flash of it would give the answer away.
    box.style.visibility = 'hidden';
    loadCredits(opts.set).then(map => {
      if (my !== token) return;
      box.style.visibility = '';
      const c = map[opts.id];
      if (!c) { fallback(); return; }
      const img = new Image();
      img.alt = '';
      img.onload = () => {
        if (my !== token) return;
        box.classList.remove('kz-photo'); void box.offsetWidth;  // restart pop-in
        box.classList.add('kz-photo');
        box.replaceChildren(img);
        if (credit) credit.textContent = '📷 ' + c.by + ' · ' + c.license + ' · Wikimedia Commons';
      };
      img.onerror = fallback;
      img.src = 'img/' + opts.set + '/' + opts.id + '.jpg';
    });
  }

  function slug(s) {
    return String(s).toLowerCase().trim().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
  }

  const css = document.createElement('style');
  css.textContent =
    '.kz-photo{animation:kzPhotoIn .35s ease-out!important;font-size:0!important;line-height:0;' +
    'display:block!important;width:100%;max-width:300px;aspect-ratio:4/3;margin:0 auto;border-radius:22px;' +
    'overflow:hidden;box-shadow:0 6px 20px rgba(0,0,0,.18);background:#EDE9FE}' +
    '.kz-photo img{width:100%;height:100%;object-fit:cover;display:block}' +
    '@keyframes kzPhotoIn{from{transform:scale(.85);opacity:0}to{transform:scale(1);opacity:1}}' +
    '.kz-photo-credit{font-size:9px;color:#9CA3AF;margin-top:4px;min-height:11px;overflow:hidden;' +
    'white-space:nowrap;text-overflow:ellipsis;text-align:center;line-height:11px}';
  document.head.appendChild(css);

  window.KZPhoto = { show, slug, loadCredits };
})();
