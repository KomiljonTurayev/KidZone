// Every fixed Uzbek text the app reads aloud, as JSON: [{id, text}]. Used by build_voice.py.
import fs from 'node:fs';
const root = new URL('../../app/src/androidMain/', import.meta.url);
const read = p => fs.readFileSync(new URL(p, root), 'utf8');
const out = [];

const content = JSON.parse(read('assets/www/content.json'));
for (const s of content.stories) out.push({ id: s.id, text: s.text.uz });

// Kidzo's offline brain and fixed lines; the name slot is recorded with the default word.
const strings = (src, word) => [...src.matchAll(/"([^"]+)"/g)]
  .map(m => m[1].replace('${childName.ifBlank { ', '').replace(/\$name/g, word)).filter(t => t.length > 25);
const brain = read('kotlin/uz/kidzone/app/ai/KidzoCompanionEngine.kt');
strings(brain.slice(brain.indexOf('fun respondUz'), brain.indexOf('fun respondRu')), 'Bolajonim')
  .forEach((t, i) => out.push({ id: `kidzo-${i + 1}`, text: t }));
const vm = read('kotlin/uz/kidzone/app/ui/viewmodel/KidzoViewModel.kt');
for (const fn of ['greeting']) {
  const at = vm.indexOf(`internal fun ${fn}(`);
  const line = vm.slice(at).match(/else -> "(.*)"\r?\n/)[1];
  out.push({ id: `kidzo-${fn}`, text: line.replace(/\$\{childName\.ifBlank \{ "(.*?)" \}\}/, '$1') });
}
process.stdout.write(JSON.stringify(out));
