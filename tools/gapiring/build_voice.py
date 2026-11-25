"""Records the app's fixed Uzbek texts with Gapiring Large into assets/www/audio/voice/<key>.mp3.

    pip install gapiring soundfile
    node texts.mjs > texts.json && python build_voice.py texts.json [model folder]

key = first 16 hex digits of SHA-1 of the trimmed text (BundledVoice.kt uses the same rule), so an edited text
simply has no clip until this is run again. Existing clips are kept; clips of texts that are gone are deleted.
The model sometimes starts a line with "mmm" and a pause, so every sentence is made several times and the cleanest
take (no hum, no stray pauses, normal speed) is kept. Listen to new clips before a release.
"""
import hashlib, json, re, sys
from pathlib import Path
import numpy as np, soundfile as sf

VOICE, MANNERS, TAKES = "f1", ("warm", "neutral"), 8
SR, H, QUIET = 24000, 480, .012
OUT = Path(__file__).resolve().parents[2] / "app/src/androidMain/assets/www/audio/voice"


def key(text): return hashlib.sha1(text.strip().encode()).hexdigest()[:16]


def spoken(text):
    text = re.sub(r"[^\w\s.,!?:;'\"‘’“”()—–-]", "", text)                  # emoji
    return re.sub(r"(?<!\w)'|(?<=[.,!?])'|\"|[“”()]", "", text)            # quotes and brackets, not o' / g'


def sentences(text):
    for para in re.split(r"\n+", spoken(text)):
        parts = [p.strip() for p in re.split(r"(?<=[.!?])\s+", para) if re.search(r"\w", p)]
        for i, p in enumerate(parts):
            yield p, .7 if i == len(parts) - 1 else .5


def frames(a):
    n = len(a) // H
    f = a[:n * H].reshape(n, H)
    spec = np.abs(np.fft.rfft(f * np.hanning(H), 1024)) ** 2
    low = spec[:, np.fft.rfftfreq(1024, 1 / SR) < 600].sum(1) / (spec.sum(1) + 1e-9)
    return np.sqrt((f ** 2).mean(1)), low


def runs(mask):
    start = None
    for i, x in enumerate([*mask, False]):
        if x and start is None: start = i
        if not x and start is not None:
            yield start * H / SR, (i - start) * H / SR
            start = None


def trim(a):
    loud = np.where(frames(a)[0] > QUIET)[0]
    return a[max(0, loud[0] - 2) * H:(loud[-1] + 4) * H] if len(loud) else a


def score(a, text):
    """0 is a clean take."""
    rms, low = frames(a)
    quiet = [r for r in runs(rms <= QUIET) if r[1] >= .2]
    s = 3 * sum(d for _, d in runs((rms > QUIET) & (low > .97)) if d >= .26)
    s += max(0, len(quiet) - len(re.findall(r"[,:;—–]", text))) + sum(max(0, d - .45) for _, d in quiet)
    first = re.split(r"[,:;—–.!?]", text)[0].strip()
    if quiet and quiet[0][0] < min(.9, len(first) / 18): s += 3             # "mmm", a pause, then the sentence
    return s + max(0, abs(len(text) / (len(a) / SR) - 14.5) - 1) * .5


def polish(a, pause=.35):
    out, pos = [], 0
    for s, d in runs(frames(a)[0] <= QUIET):
        if d > pause:
            out.append(a[pos:int((s + pause / 2) * SR)])
            pos = int((s + d - pause / 2) * SR)
    a = np.concatenate([*out, a[pos:]])
    a = np.clip(a * .075 / (np.sqrt((a ** 2).mean()) + 1e-9), -.97, .97)
    ramp = np.linspace(0, 1, 240)
    a[:240] *= ramp
    a[-240:] *= ramp[::-1]
    return a


def record(tts, text):
    parts, worst = [], 0
    for line, gap in sentences(text):
        best = None
        for manner in MANNERS:
            for seed in range(TAKES):
                a = trim(tts.speak(line, voice=VOICE, role="assistant", manner=manner, seed=seed))
                s = score(a, line)
                if best is None or s < best[0]: best = (s, a)
                if s < .15: break
            if best[0] < .6: break
        worst = max(worst, best[0])
        if best[0] >= 1.5: print(f"    check {best[0]:.1f}: {line}", flush=True)
        parts += [polish(best[1]), np.zeros(int(gap * SR))]
    return np.concatenate(parts).astype("f4"), worst


def main():
    from gapiring import Gapiring
    sys.stdout.reconfigure(encoding="utf8")
    texts = json.loads(Path(sys.argv[1]).read_text(encoding="utf8"))
    OUT.mkdir(parents=True, exist_ok=True)
    keep = {key(t["text"]) for t in texts}
    for f in OUT.glob("*.mp3"):
        if f.stem not in keep: f.unlink()
    tts = Gapiring("large", precision="int8", threads=5, normalize="literary", path=sys.argv[2] if len(sys.argv) > 2 else None)
    for t in texts:
        f = OUT / f"{key(t['text'])}.mp3"
        if f.exists(): continue
        a, worst = record(tts, t["text"])
        tmp = f.with_suffix(".tmp")
        sf.write(str(tmp), a, SR, format="MP3", subtype="MPEG_LAYER_III", compression_level=.8)
        tmp.replace(f)
        print(f"{t['id']} {f.name} {len(a) / SR:.1f}s worst {worst:.1f}", flush=True)


if __name__ == "__main__":
    assert list(sentences("Salom, do'stim! 🐥 (Bu — Boyo'g'li!) 'Nega?' debdi.")) == [
        ("Salom, do'stim!", .5), ("Bu — Boyo'g'li!", .5), ("Nega?", .5), ("debdi.", .7)]
    main()
