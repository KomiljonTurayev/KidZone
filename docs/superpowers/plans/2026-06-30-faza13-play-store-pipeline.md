# Faza 13 — Play Store Release Pipeline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** GitHub Actions orqali KidZone AAB ni avtomatik imzolash, Play Store Internal track ga yuklash va staged rollout (Internal → Alpha → Beta → Production) boshqarish.

**Architecture:** Mavjud `android_build.yml` ga `workflow_dispatch` trigger + `r0adkll/upload-google-play@v1` step qo'shiladi. `versionCode` `run_number` o'rniga git tag dan deterministik hisoblanadi. `store_listing/` papkasida EN/UZ/RU matnlar va whatsnew fayllar saqlanadi.

**Tech Stack:** GitHub Actions, `r0adkll/upload-google-play@v1`, Google Play Developer API (service account), Gradle `bundleRelease`, `app/keystore.properties` (lokal) / GitHub Secrets (CI).

## Global Constraints

- `packageName`: `uz.kidzone.app`
- `versionCode` formulasi: `MAJOR×10000 + MINOR×100 + PATCH` (e.g. `v1.3.0` → `10300`)
- `versionName` format: `"1.3.0"` (semver, quotes ichida)
- Play Store track default: `internal`
- `r0adkll/upload-google-play` versiyasi: `v1`
- `store_listing/whatsnew/` fayllari maks 500 belgi
- `store_listing/*/title.txt` maks 50 belgi
- `store_listing/*/short_description.txt` maks 80 belgi
- `store_listing/*/full_description.txt` maks 4000 belgi
- Java: 17, distribution: temurin
- Gradle cache: on

---

## Fayl xaritasi

| Fayl | Holat | Task |
|------|-------|------|
| `app/build.gradle` | Modify `:37-39` | T1 |
| `.github/workflows/android_build.yml` | Modify `:3-8` (trigger), `:82-88` (version), after `:156` (upload) | T2, T3 |
| `store_listing/whatsnew/whatsnew-en-US` | Create | T4 |
| `store_listing/whatsnew/whatsnew-uz-UZ` | Create | T4 |
| `store_listing/whatsnew/whatsnew-ru-RU` | Create | T4 |
| `store_listing/en-US/title.txt` | Create | T4 |
| `store_listing/en-US/short_description.txt` | Create | T4 |
| `store_listing/en-US/full_description.txt` | Create | T4 |
| `store_listing/uz-UZ/title.txt` | Create | T4 |
| `store_listing/uz-UZ/short_description.txt` | Create | T4 |
| `store_listing/uz-UZ/full_description.txt` | Create | T4 |
| `store_listing/ru-RU/title.txt` | Create | T4 |
| `store_listing/ru-RU/short_description.txt` | Create | T4 |
| `store_listing/ru-RU/full_description.txt` | Create | T4 |

---

### Task 1: Version bump — `app/build.gradle`

**Files:**
- Modify: `app/build.gradle:37-39`

**Interfaces:**
- Produces: `versionCode 13`, `versionName "1.3.0"` (lokal fallback qiymatlari)

- [ ] **Step 1: `build.gradle` ni yangilash**

`app/build.gradle` `:37-39` qatorlarini o'zgartiring:

```groovy
        // CI passes -PversionCode=<calculated>; local default = 13
        versionCode  project.hasProperty('versionCode')  ? project.versionCode.toInteger()  : 13
        versionName  project.hasProperty('versionName')  ? project.versionName               : "1.3.0"
```

- [ ] **Step 2: Lokal build tekshirish**

```powershell
.\gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```powershell
git add app/build.gradle
git commit -m "chore: version bump to 1.3.0 (versionCode 13)"
```

---

### Task 2: `android_build.yml` — `workflow_dispatch` trigger + `versionCode` tuzatish

**Files:**
- Modify: `.github/workflows/android_build.yml:3-8` (trigger blok)
- Modify: `.github/workflows/android_build.yml:82-88` (version parsing step)

**Interfaces:**
- Produces: `steps.version.outputs.code` (deterministik int), `steps.version.outputs.name` (semver string), `steps.version.outputs.tag` (e.g. `v1.3.0`)
- Consumes: `github.event_name`, `inputs.version_name`, `GITHUB_REF`

- [ ] **Step 1: `on:` blokini almashtirish**

`.github/workflows/android_build.yml` `:3-8` qatorlarini (mavjud `on:` blokini) quyidagi to'liq blok bilan almashtiring:

```yaml
on:
  push:
    branches: [master, main]
    tags:     ['v[0-9]+.[0-9]+.[0-9]+']
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

- [ ] **Step 2: Version parsing step ni almashtirish**

`.github/workflows/android_build.yml` release job dagi `Parse version from tag` step ni (`:82-88`) quyidagi blok bilan almashtiring:

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

- [ ] **Step 3: YAML sintaksisini tekshirish**

```powershell
# python-yaml o'rnatilgan bo'lsa:
python -c "import yaml; yaml.safe_load(open('.github/workflows/android_build.yml'))"
# Yoki:
cat .github/workflows/android_build.yml | python -c "import sys,yaml; yaml.safe_load(sys.stdin); print('OK')"
```

Expected: `OK` (yoki xatosiz chiqish)

- [ ] **Step 4: Commit**

```powershell
git add .github/workflows/android_build.yml
git commit -m "ci: add workflow_dispatch trigger + deterministic versionCode from tag"
```

---

### Task 3: `android_build.yml` — Play Store upload step

**Files:**
- Modify: `.github/workflows/android_build.yml` — release job oxiriga yangi step

**Interfaces:**
- Consumes: `steps.artifacts.outputs.aab`, `steps.version.outputs.name`, `secrets.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`, `inputs.track`, `inputs.rollout_percentage`
- Consumes: `store_listing/whatsnew/` (Task 4 dan)

- [ ] **Step 1: Upload step ni release job oxiriga qo'shish**

`.github/workflows/android_build.yml` da `Upload AAB artifact` step dan KEYIN (fayl oxiriga) quyidagi step qo'shing:

```yaml
      - name: Upload to Play Store
        if: ${{ secrets.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON != '' }}
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

**Muhim:** `if: ${{ secrets.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON != '' }}` — secret sozlanmagan bo'lsa step o'tkazib yuboriladi (debug build lar ta'sirlanmaydi).

- [ ] **Step 2: YAML sintaksisini tekshirish**

```powershell
python -c "import sys,yaml; yaml.safe_load(open('.github/workflows/android_build.yml')); print('OK')"
```

Expected: `OK`

- [ ] **Step 3: Commit**

```powershell
git add .github/workflows/android_build.yml
git commit -m "ci: add Play Store upload step via r0adkll/upload-google-play@v1"
```

---

### Task 4: `store_listing/` papkasi — matnlar va whatsnew

**Files:**
- Create: `store_listing/whatsnew/whatsnew-en-US`
- Create: `store_listing/whatsnew/whatsnew-uz-UZ`
- Create: `store_listing/whatsnew/whatsnew-ru-RU`
- Create: `store_listing/en-US/title.txt`
- Create: `store_listing/en-US/short_description.txt`
- Create: `store_listing/en-US/full_description.txt`
- Create: `store_listing/uz-UZ/title.txt`
- Create: `store_listing/uz-UZ/short_description.txt`
- Create: `store_listing/uz-UZ/full_description.txt`
- Create: `store_listing/ru-RU/title.txt`
- Create: `store_listing/ru-RU/short_description.txt`
- Create: `store_listing/ru-RU/full_description.txt`

**Interfaces:**
- Produces: `store_listing/whatsnew/` — `r0adkll/upload-google-play` action tomonidan o'qiladi (Task 3)

- [ ] **Step 1: `store_listing/whatsnew/whatsnew-en-US` yaratish**

```
• Multi-profile support: each child gets their own profile with time limits
• Smarter ads: interstitial shown every 3 games, not on every tap
• Profile switching now correctly tracks time per child
• Bug fixes and stability improvements
```
(255 belgi — limit 500 ✅)

- [ ] **Step 2: `store_listing/whatsnew/whatsnew-uz-UZ` yaratish**

```
• Ko'p profil: har bir bola o'z profili va vaqt chekloviga ega
• Reklama optimallashtirish: interstitial har 3 o'yinda bir marta
• Profil almashinuvida vaqt to'g'ri hisoblanadi
• Xatolar tuzatildi va barqarorlik yaxshilandi
```
(230 belgi ✅)

- [ ] **Step 3: `store_listing/whatsnew/whatsnew-ru-RU` yaratish**

```
• Мультипрофиль: у каждого ребёнка свой профиль с лимитом времени
• Оптимизация рекламы: межстраничная реклама раз в 3 игры
• Переключение профиля теперь корректно учитывает время
• Исправления ошибок и улучшение стабильности
```
(247 belgi ✅)

- [ ] **Step 4: `store_listing/en-US/title.txt` yaratish**

```
KidZone — Kids Learning Games
```
(29 belgi — limit 50 ✅)

- [ ] **Step 5: `store_listing/en-US/short_description.txt` yaratish**

```
Fun educational mini-games for kids in Uzbek, Russian & English.
```
(65 belgi — limit 80 ✅)

- [ ] **Step 6: `store_listing/en-US/full_description.txt` yaratish**

```
KidZone is a safe, fun, and educational app designed for young children aged 3–10.

🎮 31 Mini-Games
Puzzles, drawing, memory, bubble pop, shape matching, and more — all designed to develop cognitive skills through play.

🌍 3 Languages
Switch instantly between Uzbek, Russian, and English. Every game adapts to the chosen language.

👨‍👩‍👧 Multiple Profiles
Create separate profiles for each child. Set daily time limits per profile. Switch with a single tap.

📚 Stories & Songs
Age-appropriate stories and songs in all three languages, with text-to-speech support.

🔒 Parental Controls
PIN-protected parent dashboard. Monitor daily usage. Set time limits. No ads inside games.

✅ Kid-Safe
No in-app purchases. No social features. No data collection from children.
```
(732 belgi — limit 4000 ✅)

- [ ] **Step 7: `store_listing/uz-UZ/title.txt` yaratish**

```
KidZone — Bolalar O'quv O'yinlari
```
(34 belgi ✅)

- [ ] **Step 8: `store_listing/uz-UZ/short_description.txt` yaratish**

```
3 tilda qiziqarli ta'limiy mini-o'yinlar: O'zbek, Rus, Ingliz.
```
(63 belgi ✅)

- [ ] **Step 9: `store_listing/uz-UZ/full_description.txt` yaratish**

```
KidZone — 3 yoshdan 10 yoshgacha bo'lgan bolalar uchun xavfsiz, qiziqarli va ta'limiy ilova.

🎮 31 mini-o'yin
Boshqotirmalar, chizish, xotira o'yinlari, pufakchalar, shakllar va yana ko'p narsa — barchasi o'yin orqali bilish qobiliyatini rivojlantirish uchun.

🌍 3 til
O'zbek, Rus va Ingliz tillarida bir zumda almashinish. Har bir o'yin tanlangan tilga moslashadi.

👨‍👩‍👧 Ko'p profil
Har bir bola uchun alohida profil yarating. Har bir profil uchun kunlik vaqt cheklovini belgilang.

📚 Hikoyalar va qo'shiqlar
Barcha uch tilda yoshga mos hikoyalar va qo'shiqlar, matnni o'qish (TTS) qo'llab-quvvatlanadi.

🔒 Ota-ona nazorati
PIN bilan himoyalangan boshqaruv paneli. Kunlik foydalanishni kuzatish. Vaqt chegarasini belgilash.

✅ Bolalar uchun xavfsiz
Ilovadagi xaridlar yo'q. Ijtimoiy funksiyalar yo'q. Bolalardan ma'lumot to'planmaydi.
```
(779 belgi ✅)

- [ ] **Step 10: `store_listing/ru-RU/title.txt` yaratish**

```
KidZone — Развивающие игры для детей
```
(36 belgi ✅)

- [ ] **Step 11: `store_listing/ru-RU/short_description.txt` yaratish**

```
Весёлые обучающие мини-игры для детей на узбекском, русском и английском.
```
(73 belgi ✅)

- [ ] **Step 12: `store_listing/ru-RU/full_description.txt` yaratish**

```
KidZone — безопасное, весёлое и развивающее приложение для детей от 3 до 10 лет.

🎮 31 мини-игра
Пазлы, рисование, игры на память, пузыри, фигуры и многое другое — всё для развития когнитивных навыков через игру.

🌍 3 языка
Мгновенное переключение между узбекским, русским и английским. Каждая игра адаптируется к выбранному языку.

👨‍👩‍👧 Несколько профилей
Создайте отдельный профиль для каждого ребёнка. Установите дневной лимит времени для каждого профиля.

📚 Сказки и песни
Возрастные истории и песни на трёх языках с поддержкой синтеза речи (TTS).

🔒 Родительский контроль
Панель управления с PIN-защитой. Отслеживание ежедневного использования. Установка лимита времени.

✅ Безопасно для детей
Без покупок внутри приложения. Без социальных функций. Без сбора данных о детях.
```
(793 belgi ✅)

- [ ] **Step 13: Belgi limitlarini tekshirish**

```powershell
# Har bir fayl uchun belgi sanash
Get-Content "store_listing\en-US\title.txt" | Measure-Object -Character | Select-Object -ExpandProperty Characters
Get-Content "store_listing\en-US\short_description.txt" | Measure-Object -Character | Select-Object -ExpandProperty Characters
Get-Content "store_listing\uz-UZ\title.txt" | Measure-Object -Character | Select-Object -ExpandProperty Characters
Get-Content "store_listing\ru-RU\title.txt" | Measure-Object -Character | Select-Object -ExpandProperty Characters
```

Expected: title ≤ 50, short_description ≤ 80 har til uchun.

- [ ] **Step 14: Commit**

```powershell
git add store_listing/
git commit -m "feat: add Play Store listing assets (EN/UZ/RU) + whatsnew for v1.3.0"
```

---

### Task 5: Google Play service account + GitHub Secret (qo'lda sozlash)

**Files:** Hech qanday fayl o'zgartirilmaydi — bu tashqi tizimlar sozlash.

**Interfaces:**
- Produces: `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` GitHub Secret (Task 3 da ishlatiladi)

- [ ] **Step 1: Google Play Console — API access**

1. [play.google.com/console](https://play.google.com/console) → ochiq
2. **Setup** → **API access** → **Link to a Google Cloud project** → Mavjud yoki yangi project tanlash → **Link**

- [ ] **Step 2: Google Cloud Console — Service Account yaratish**

1. [console.cloud.google.com](https://console.cloud.google.com) → Play Console bilan bog'langan project
2. **IAM & Admin** → **Service Accounts** → **+ Create Service Account**
3. Name: `kidzone-github-actions` → **Create and continue** → **Done** (role kerak emas)
4. Yaratilgan service account → **Keys** tab → **Add Key** → **Create new key** → **JSON** → **Create**
5. JSON fayl yuklab olinadi — bu faylni xavfsiz joyda saqlang

- [ ] **Step 3: Play Console — Service account ga permission berish**

1. Play Console → **Users and permissions** → **Invite new user**
2. Email: service account email (`kidzone-github-actions@<project>.iam.gserviceaccount.com`)
3. Permission: **Release manager** → **Invite user**

- [ ] **Step 4: GitHub Secret qo'shish**

1. GitHub → `KidZone` repo → **Settings** → **Secrets and variables** → **Actions**
2. **New repository secret**:
   - Name: `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`
   - Secret: JSON fayl mazmunini to'liq ko'chirish (butun JSON)
3. **Add secret**

- [ ] **Step 5: Tekshirish**

GitHub → **Settings** → **Secrets** → `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` ko'rinishi kerak (qiymati yashirin, lekin mavjud).

---

### Task 6: Play Console — birinchi qo'lda upload + app ro'yxatdan o'tkazish

**Files:** Hech qanday fayl o'zgartirilmaydi — Play Console da bir martalik setup.

**Muhim:** `r0adkll/upload-google-play` faqat Play Console da allaqachon mavjud app uchun ishlaydi. Birinchi AAB qo'lda yuklanishi shart.

- [ ] **Step 1: Lokal release AAB build qilish**

```powershell
.\gradlew bundleRelease
```

Expected: `BUILD SUCCESSFUL`, fayl: `app\build\outputs\bundle\release\app-release.aab`

- [ ] **Step 2: Play Console — yangi app yaratish**

1. [play.google.com/console](https://play.google.com/console) → **Create app**
2. App name: `KidZone`
3. Default language: `English (United States)`
4. App or game: **Game**
5. Free or paid: **Free**
6. Declarations: ikkalasini belgilash → **Create app**

- [ ] **Step 3: Store listing to'ldirish**

Play Console → **Store presence** → **Main store listing**:
- Title: `store_listing/en-US/title.txt` mazmuni
- Short description: `store_listing/en-US/short_description.txt` mazmuni
- Full description: `store_listing/en-US/full_description.txt` mazmuni
- App icon: 512×512 PNG (KidZone ikonkasi)
- Feature graphic: 1024×500 PNG
- Screenshots: kamida 2 ta telefon screenshot (PNG, min 1080×1920)

- [ ] **Step 4: Content rating**

Play Console → **Policy** → **App content** → **Content rating** → Questionnaire to'ldirish (bolalar uchun o'yin).

- [ ] **Step 5: Target audience**

Play Console → **Policy** → **App content** → **Target audience** → Ages 5-8 (yoki 3-12).

- [ ] **Step 6: Internal testing — birinchi AAB upload**

Play Console → **Testing** → **Internal testing** → **Create new release** → **Upload** → `app-release.aab` tanlash → **Save** → **Review release** → **Start rollout to Internal testing**

- [ ] **Step 7: Tekshirish**

Play Console → **Internal testing** → Release status: **Active** bo'lishi kerak. Internal tester qurilmasiga yuklab ko'rish.

---

### Task 7: End-to-end smoke test — tag push → Play Store

**Files:** Hech qanday fayl o'zgartirilmaydi.

- [ ] **Step 1: Barcha o'zgarishlar push qilinganini tekshirish**

```powershell
git log --oneline -5
git status
```

Expected: `nothing to commit`, oxirgi 3-4 commit ko'rinishi kerak.

- [ ] **Step 2: Tag yaratish va push qilish**

```powershell
git tag v1.3.0
git push origin v1.3.0
```

- [ ] **Step 3: GitHub Actions ni kuzatish**

GitHub → repo → **Actions** → yangi workflow run ko'rinishi kerak:
1. `debug` job: PASS
2. `release` job: PASS
3. `Upload to Play Store` step: green ✅

Agar `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` secret sozlangan bo'lsa — upload ishlaydi.
Agar sozlanmagan bo'lsa — step `if:` sharti tufayli o'tkazib yuboriladi (bu ham to'g'ri).

- [ ] **Step 4: Play Console tekshirish**

Play Console → **Testing** → **Internal testing** → yangi release (v1.3.0, versionCode 10300) ko'rinishi kerak.

- [ ] **Step 5: `workflow_dispatch` ni tekshirish**

GitHub → **Actions** → `KidZone CI/CD` → **Run workflow**:
- `version_name`: `1.3.0`
- `track`: `internal`
- `rollout_percentage`: `100`
→ **Run workflow**

Expected: release job muvaffaqiyatli ishlaydi.

- [ ] **Step 6: GitHub Release tekshirish**

GitHub → **Releases** → `KidZone v1.3.0` release yaratilgan, `app-release.aab` va `app-release.apk` biriktirilgan bo'lishi kerak.

- [ ] **Step 7: Final commit (agar kerak bo'lsa)**

Agar hech qanday o'zgartirish kerak bo'lmasa, bu step o'tkazib yuboriladi.

```powershell
git status
# nothing to commit — hammasi tayyor
```

---

## Staged rollout qo'llanmasi (ma'lumotnoma)

Har keyingi release uchun:

```
1. git tag v1.X.Y && git push --tags
   → GitHub Actions → Internal track

2. Play Console → Internal (1-2 kun test)
   → Promote to Alpha

3. Alpha (2-3 kun)
   → Promote to Beta

4. Beta (3-7 kun, Crashlytics kuzatish)
   → Promote to Production, rollout 10%

5. Production rollout: 10% → 50% → 100%
   (har bosqich 1-2 kun)

Hotfix: workflow_dispatch → track: alpha
```
