// Run: node --test www-tests/kz-ui.test.js
// Loads the WebView's kz-ui.js into a sandbox with a fake browser and checks the level rules.
const test = require('node:test');
const assert = require('node:assert');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const SRC = fs.readFileSync(
  path.join(__dirname, '..', 'app', 'src', 'androidMain', 'assets', 'www', 'kz-ui.js'), 'utf8');

function load({search = '', store = {}, bridge = null, app = null} = {}) {
  const data = {...store};
  const localStorage = {
    getItem: k => (k in data ? String(data[k]) : null),
    setItem: (k, v) => { data[k] = String(v); },
    removeItem: k => { delete data[k]; },
  };
  // A game runs in an iframe; its parent is index.html with the star counter (app).
  const window = {AndroidBridge: bridge, parent: {app}};
  const ctx = {window, localStorage, location: {search}, URLSearchParams, document: {}, setTimeout: () => 0};
  vm.runInNewContext(SRC, ctx);
  return {KZL: window.KZL, data};
}

test('a 2-4 year old never gets a timer and always keeps a hint', () => {
  const {KZL} = load();
  for (let n = 1; n <= 500; n++) {
    const c = KZL.cfgFor(n, '2-4');
    assert.strictEqual(c.timer, 0, `level ${n}`);
    assert.ok(c.hints >= 1, `level ${n}`);
  }
});

test('5-7 timers stay at 15 s or more, 8+ at 8 s or more', () => {
  const {KZL} = load();
  for (let n = 1; n <= 500; n++) {
    const mid = KZL.cfgFor(n, '5-7').timer;
    const big = KZL.cfgFor(n, '8+').timer;
    assert.ok(mid === 0 || mid >= 15, `5-7 level ${n}: ${mid}`);
    assert.ok(big === 0 || big >= 8, `8+ level ${n}: ${big}`);
  }
});

test('difficulty never drops as the level grows', () => {
  const {KZL} = load();
  for (const age of ['2-4', '5-7', '8+']) {
    for (let n = 1; n < 500; n++) {
      const a = KZL.cfgFor(n, age), b = KZL.cfgFor(n + 1, age);
      assert.ok(b.q >= a.q && b.c >= a.c && b.pool >= a.pool && b.hints <= a.hints, `${age} ${n}`);
      const timerHarder = a.timer === 0 || (b.timer > 0 && b.timer <= a.timer);
      assert.ok(timerHarder, `${age} timer ${n}`);
    }
  }
});

test('levels go past 20 and are kept per child', () => {
  const env = load({store: {'kz-native-profile': 'kid-a'}});
  env.KZL.setLevel('animals', 20);
  assert.strictEqual(env.KZL.nextLevel('animals'), 21);
  assert.strictEqual(env.KZL.getLevel('animals'), 21);
  env.data['kz-native-profile'] = 'kid-b';
  assert.strictEqual(env.KZL.getLevel('animals'), 1);
});

test('replaying an old level keeps the best level', () => {
  const {KZL} = load();
  KZL.setLevel('abc', 30);
  KZL.setLevel('abc', 3);
  assert.strictEqual(KZL.getLevel('abc'), 3);
  assert.strictEqual(KZL.getBest('abc'), 30);
  const w = KZL.levelWindow('abc');
  assert.deepStrictEqual([w[0].n, w[w.length - 1].n, w[w.length - 1].locked], [20, 31, true]);
});

test('the old shared level moves to the first child who opens the game', () => {
  const env = load({store: {'kzl-math': '12', 'kz-native-profile': 'kid-a'}});
  assert.strictEqual(env.KZL.getLevel('math'), 12);
  assert.strictEqual(env.data['kzl-math'], undefined);
  env.data['kz-native-profile'] = 'kid-b';
  assert.strictEqual(env.KZL.getLevel('math'), 1);
});

test('two misses in a row soften the level until it is passed', () => {
  const {KZL} = load({search: '?age=8%2B'});
  KZL.setLevel('fruits', 10);
  const normal = KZL.cfg('fruits');
  KZL.recordResult('fruits', false);
  assert.strictEqual(KZL.cfg('fruits').ease, false);
  KZL.recordResult('fruits', false);
  const eased = KZL.cfg('fruits');
  assert.ok(eased.ease && eased.timer > normal.timer && eased.c < normal.c && eased.hints > normal.hints);
  KZL.recordResult('fruits', true);
  assert.strictEqual(KZL.cfg('fruits').ease, false);
});

test('choices are capped to what the game can lay out', () => {
  const {KZL} = load({search: '?age=8%2B'});
  KZL.setLevel('x', 45);
  assert.strictEqual(KZL.cfg('x').c, 4);
  KZL.maxChoices = 6;
  assert.strictEqual(KZL.cfg('x').c, 6);
});

test('a new best level is reported to the app, a replay is not', () => {
  const calls = [];
  const {KZL} = load({bridge: {levelReached: (g, n) => calls.push([g, n])}});
  KZL.setLevel('maze', 5);
  KZL.setLevel('maze', 2);
  KZL.nextLevel('maze');
  assert.deepStrictEqual(calls, [['maze', 5]]);
});

test('every 5th new level gives a sticker, every 10th also 100 stars', () => {
  const added = [];
  const {KZL} = load({app: {addPoints: n => added.push(n)}});
  KZL.setLevel('maze', 4);
  for (let i = 0; i < 6; i++) KZL.nextLevel('maze'); // 5..10
  const got = KZL.stickers();
  assert.deepStrictEqual([...got.map(s => s.lv)], [5, 10]);
  assert.ok(got.every(s => s.g === 'maze' && s.e && !s.gold));
  assert.deepStrictEqual(added, [100]);
});

test('replaying a level does not hand out its sticker twice', () => {
  const {KZL} = load();
  KZL.setLevel('abc', 4);
  KZL.nextLevel('abc');
  KZL.setLevel('abc', 4);
  KZL.nextLevel('abc');
  assert.strictEqual(KZL.stickers().length, 1);
});

test('level 50 gives a gold sticker; stickers stay with their child', () => {
  const env = load({store: {'kz-native-profile': 'kid-a'}});
  env.KZL.setLevel('clock', 49);
  env.KZL.nextLevel('clock');
  assert.strictEqual(env.KZL.stickers()[0].gold, true);
  env.data['kz-native-profile'] = 'kid-b';
  assert.strictEqual(env.KZL.stickers().length, 0);
});

test('playing together: turns alternate and only right answers score', () => {
  const {KZL} = load({search: '?duo=1'});
  assert.strictEqual(KZL.duo.active, true);
  KZL.duo.answer(true);   // child right
  KZL.duo.answer(false);  // parent wrong
  KZL.duo.answer(true);   // child right
  assert.deepStrictEqual([...KZL.duo.scores], [2, 0]);
  assert.strictEqual(KZL.duo.turn, 1);
  assert.strictEqual(load().KZL.duo.active, false);
});
