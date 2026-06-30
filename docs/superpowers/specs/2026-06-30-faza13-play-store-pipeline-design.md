# Faza 13 — Play Store Release Pipeline: Dizayn Spesifikatsiyasi

**Sana:** 2026-06-30
**Holat:** Approved
**Versiya:** versionCode `13`, versionName `"1.3.0"`
**Bog'liq:** `docs/superpowers/specs/2026-06-29-faza12-bugfix-ads-design.md`

---

## Maqsad

KidZone ni Google Play Store ga chiqarish uchun to'liq avtomatlashtirilgan pipeline yaratish: GitHub Actions orqali sign + upload, staged rollout (Internal → Alpha → Beta → Production), va Play Store listing assets (EN asosiy, UZ/RU qo'shimcha).

---

## Qamrov

**Kiradi:**
- T1: `android_build.yml` — `workflow_dispatch` trigger + Play Store upload step
- T2: `versionCode` strategiyasini tuzatish (tag-dan deterministik hisoblash)
- T3: `store_listing/` papkasi — whatsnew + EN/UZ/RU matnlar
- T4: Screenshots tayyorlash (4–8 ta telefon screenshot)
- T5: Google Play service account sozlash + `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` secret
- T6: Birinchi qo'lda upload (Play Console) — app ni ro'yxatdan o'tkazish
- T7: Version bump → `versionCode 13`, `versionName "1.3.0"`

**Kirmaydi:**
- Fastlane — Variant B tanlov bo'yicha ishlatilmaydi
- Full listing CI orqali boshqarish — Play Console da bir marta sozlanadi
- Tablet screenshots — ixtiyoriy, keyingi fazaga qoldiriladi

---

## Umumiy arxitektura

```
git tag v1.3.0 && git push --tags
        │
        ▼
GitHub Actions: android_build.yml
        │
  ┌─────┴─────┐
  │  debug    │  (har push/PR — o'zgarmaydi)
  └─────┬─────┘
        │ needs: debug
        ▼
  ┌─────────────────────────────┐
  │  release (tag yoki manual)  │
  │  1. Checkout                │
  │  2. versionCode hisoblash   │ ← TUZATILADI
  │  3. Keystore decode         │
  │  4. bundleRelease + sign    │
  │  5. GitHub Release yaratish │
  │  6. Play Store upload       │ ← YANGI
  │     └─ track: internal      │
  └─────────────────────────────┘
        │
  Play Console da qo'lda promote:
  internal → alpha → beta → production
```

---

## Texnik dizayn

### T1 + T2: `android_build.yml` o'zgarishlari

#### Trigger — `workflow_dispatch` qo'shiladi

```yaml
on:
  push:
    branches: [master, main]
    tags: ['v[0-9]+.[0-9]+.[0-9]+']
  pull_request:
    branches: [master, main]
  workflow_dispatch:
    inputs:
      version_name:
        description: 'Version name (e.g. 1.3.0)'
        required: true
      track:
        description: 'Play Store track'
        required: true
        default: 'internal'
        type: choice
        options: [internal, alpha, beta, production]
      rollout_percentage:
        description: 'Rollout % (production track uchun)'
        default: '10'
```

#### `versionCode` deterministik hisoblash

Tag `v1.3.0` → `1×10000 + 3×100 + 0 = 10300`. `run_number` ishlatilmaydi.

```yaml
- name: Parse version from tag or input
  id: version
  run: |
    if [ "${{ github.event_name }}" = "workflow_dispatch" ]; then
      RAW="${{ inputs.version_name }}"
    else
      RAW="${GITHUB_REF#refs/tags/v}"
    fi
    MAJOR=$(echo $RAW | cut -d. -f1)
    MINOR=$(echo $RAW | cut -d. -f2)
    PATCH=$(echo $RAW | cut -d. -f3)
    CODE=$((MAJOR * 10000 + MINOR * 100 + PATCH))
    echo "code=$CODE"  >> $GITHUB_OUTPUT
    echo "name=$RAW"   >> $GITHUB_OUTPUT
    echo "tag=v$RAW"   >> $GITHUB_OUTPUT
```

#### Play Store upload step (release job oxiriga qo'shiladi)

```yaml
- name: Upload to Play Store
  uses: r0adkll/upload-google-play@v1
  with:
    serviceAccountJsonPlainText: ${{ secrets.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON }}
    packageName: uz.kidzone.app
    releaseFiles: ${{ steps.artifacts.outputs.aab }}
    track: ${{ inputs.track || 'internal' }}
    rolloutPercentage: ${{ inputs.rollout_percentage || '100' }}
    whatsNewDirectory: store_listing/whatsnew
    mappingFile: app/build/outputs/mapping/release/mapping.txt
```

---

### T3: `store_listing/` papka strukturasi

```
store_listing/
├── whatsnew/
│   ├── whatsnew-en-US        # "What's new" — har release yangilanadi (maks 500 belgi)
│   ├── whatsnew-uz-UZ
│   └── whatsnew-ru-RU
├── en-US/
│   ├── title.txt             # maks 50 belgi
│   ├── short_description.txt # maks 80 belgi
│   └── full_description.txt  # maks 4000 belgi
├── uz-UZ/
│   ├── title.txt
│   ├── short_description.txt
│   └── full_description.txt
└── ru-RU/
    ├── title.txt
    ├── short_description.txt
    └── full_description.txt
```

Screenshots (`en-US/screenshots/phone_*.png`) repoda saqlanmaydi — Play Console da bir marta yuklanadi.

**Mazmun strategiyasi:**
- `en-US` — asosiy, to'liq tavsif
- `uz-UZ`, `ru-RU` — qisqaroq tarjima (screenshots EN dan meros)
- `whatsnew` — har release oldidan yangilanadi

---

### T4: Screenshots

- **Soni:** 4–8 ta telefon screenshot
- **O'lcham:** min 1080×1920px, PNG
- **Mazmun:** Bosh ekran, o'yinlar ro'yxati, o'yin jarayoni, profil ekrani
- **Yuklash:** Play Console → Store presence → Main store listing → Graphics (bir marta qo'lda)

---

### T5: Google Play service account sozlash

**Bir martalik qadamlar:**

1. **Google Play Console** → Setup → API access → Link to Google Cloud project
2. **Google Cloud Console** → IAM & Admin → Service Accounts → Create:
   - Name: `kidzone-github-actions`
   - Role: kerak emas (Play Console da beriladi)
3. Service account → Keys → Add Key → JSON → yuklab olish
4. **Play Console** → Users & permissions → Invite new user:
   - Email: service account email
   - Permission: **Release manager**
5. **GitHub** → Settings → Secrets → Actions → New secret:
   - Name: `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`
   - Value: JSON fayl to'liq matni

---

### T6: Birinchi qo'lda upload (Play Console)

`r0adkll/upload-google-play` faqat mavjud app uchun ishlaydi. Birinchi AAB qo'lda yuklanishi shart:

1. Play Console → Create app → `uz.kidzone.app`
2. Store listing → Asosiy ma'lumotlar (title, description, category, contact)
3. Content rating → Questionnaire
4. Pricing & distribution
5. App releases → Internal testing → Upload AAB (lokal `bundleRelease` dan)
6. Keydan CI pipeline ishlaydi

---

### T7: Version bump

`app/build.gradle`:
```groovy
versionCode  ... : 13      // lokal fallback (CI tag-dan oladi)
versionName  ... : "1.3.0"
```

---

## Staged rollout jarayoni

```
1. git tag v1.3.0 → GitHub Actions → upload to INTERNAL
2. Play Console → test 1-2 kun
3. Promote → ALPHA (yopiq test)
4. Promote → BETA (ochiq test, 3-7 kun)
5. Promote → PRODUCTION, rollout 10% → 50% → 100%
   (har bosqich 1-2 kun, Crashlytics crash rate kuzatish)

Hotfix yo'li:
  workflow_dispatch → track: alpha (Internal ni o'tkazib)
```

---

## Fayl o'zgarishlari jadvali

| Fayl | O'zgarish | Task |
|------|-----------|------|
| `.github/workflows/android_build.yml` | `workflow_dispatch` trigger + `versionCode` tuzatish + Play Store upload step | T1, T2 |
| `store_listing/whatsnew/whatsnew-en-US` | Yangi fayl — "What's new" EN | T3 |
| `store_listing/whatsnew/whatsnew-uz-UZ` | Yangi fayl — "What's new" UZ | T3 |
| `store_listing/whatsnew/whatsnew-ru-RU` | Yangi fayl — "What's new" RU | T3 |
| `store_listing/en-US/title.txt` | Yangi fayl | T3 |
| `store_listing/en-US/short_description.txt` | Yangi fayl | T3 |
| `store_listing/en-US/full_description.txt` | Yangi fayl | T3 |
| `store_listing/uz-UZ/title.txt` | Yangi fayl | T3 |
| `store_listing/uz-UZ/short_description.txt` | Yangi fayl | T3 |
| `store_listing/uz-UZ/full_description.txt` | Yangi fayl | T3 |
| `store_listing/ru-RU/title.txt` | Yangi fayl | T3 |
| `store_listing/ru-RU/short_description.txt` | Yangi fayl | T3 |
| `store_listing/ru-RU/full_description.txt` | Yangi fayl | T3 |
| `app/build.gradle` | versionCode `13`, versionName `"1.3.0"` | T7 |

Screenshots va service account JSON → repo ga kirmaydi (Play Console + GitHub Secrets).

---

## Muvaffaqiyat mezonlari

- [ ] `git tag v1.3.0 && git push --tags` → GitHub Actions → AAB Play Store Internal track ga yuklanadi
- [ ] `workflow_dispatch` → track va rollout_percentage tanlash mumkin
- [ ] `versionCode` tag dan deterministik hisoblanadi (`v1.3.0` → `10300`)
- [ ] `store_listing/whatsnew/` fayllar Play Console da ko'rinadi
- [ ] Play Console da app ro'yxatdan o'tgan, birinchi AAB qo'lda yuklangan
- [ ] Internal → Alpha → Beta → Production promote qilish mumkin
- [ ] `.\gradlew bundleRelease` lokal ham ishlaydi (keystore.properties orqali)
