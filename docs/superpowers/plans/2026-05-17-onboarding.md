# Onboarding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show a 3-step native onboarding (language → age → Kidzo intro with TTS) on first app launch only, persist choices to SharedPreferences, then inject them into WebView localStorage.

**Architecture:** Single `OnboardingActivity` with one XML layout containing 3 step sections shown/hidden via `setVisibility`. `MainActivity.onCreate` checks `kz_onboarding_done` flag and redirects if needed. `KidWebViewManager` gets a one-shot `onPageReadyCallback` to inject `kz-lang`/`kz-age` into `localStorage` after first page load.

**Tech Stack:** Android Java, TextToSpeech, ObjectAnimator, SharedPreferences, Material Components

---

## File Map

| Action | File |
|--------|------|
| Modify | `app/src/main/res/values/strings.xml` |
| Modify | `app/src/main/AndroidManifest.xml` |
| Create | `app/src/main/res/layout/activity_onboarding.xml` |
| Create | `app/src/main/java/uz/kidzone/app/OnboardingActivity.java` |
| Modify | `app/src/main/java/uz/kidzone/app/KidWebViewManager.java` |
| Modify | `app/src/main/java/uz/kidzone/app/MainActivity.java` |

---

## Task 1: Strings + Manifest

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Add onboarding strings to strings.xml**

Open `app/src/main/res/values/strings.xml`. Add before the closing `</resources>` tag:

```xml
    <!-- Onboarding -->
    <string name="onb_lang_hint">🇺🇿 Tilni tanlang · 🇷🇺 Выберите язык · 🇬🇧 Choose language</string>
    <string name="onb_lang_uz">🇺🇿  O\'zbek</string>
    <string name="onb_lang_ru">🇷🇺  Русский</string>
    <string name="onb_lang_en">🇬🇧  English</string>
    <string name="onb_next">Keyingi →</string>
    <string name="onb_back">← Orqaga</string>
    <string name="onb_age_title">Yosh guruhini tanlang</string>
    <string name="onb_age_24">👶\n2–4</string>
    <string name="onb_age_57">🧒\n5–7</string>
    <string name="onb_age_8plus">👦\n8+</string>
```

- [ ] **Step 2: Register OnboardingActivity in AndroidManifest.xml**

Open `app/src/main/AndroidManifest.xml`. Add after the `</activity>` closing tag for MainActivity (before `</application>`):

```xml
        <!-- ONBOARDING ACTIVITY -->
        <activity
            android:name=".OnboardingActivity"
            android:exported="false"
            android:screenOrientation="portrait"
            android:theme="@style/Theme.KidZone"/>
```

- [ ] **Step 3: Commit**

```
git add app/src/main/res/values/strings.xml app/src/main/AndroidManifest.xml
git commit -m "feat(onboarding): add strings and register OnboardingActivity"
```

---

## Task 2: Layout

**Files:**
- Create: `app/src/main/res/layout/activity_onboarding.xml`

- [ ] **Step 1: Create activity_onboarding.xml**

Create `app/src/main/res/layout/activity_onboarding.xml` with this full content:

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#FFF8F0">

    <!-- ── Step 0: Language ── -->
    <LinearLayout
        android:id="@+id/step_lang"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:gravity="center"
        android:padding="32dp"
        android:visibility="gone">

        <ImageView
            android:layout_width="120dp"
            android:layout_height="120dp"
            android:src="@drawable/splash_icon"
            android:contentDescription="@null"
            android:layout_marginBottom="24dp"/>

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/onb_lang_hint"
            android:textSize="13sp"
            android:textColor="#888888"
            android:textAlignment="center"
            android:layout_marginBottom="28dp"/>

        <Button
            android:id="@+id/btn_lang_uz"
            style="@style/Widget.MaterialComponents.Button.OutlinedButton"
            android:layout_width="match_parent"
            android:layout_height="60dp"
            android:text="@string/onb_lang_uz"
            android:textSize="18sp"
            android:textColor="#222222"
            android:layout_marginBottom="12dp"/>

        <Button
            android:id="@+id/btn_lang_ru"
            style="@style/Widget.MaterialComponents.Button.OutlinedButton"
            android:layout_width="match_parent"
            android:layout_height="60dp"
            android:text="@string/onb_lang_ru"
            android:textSize="18sp"
            android:textColor="#222222"
            android:layout_marginBottom="12dp"/>

        <Button
            android:id="@+id/btn_lang_en"
            style="@style/Widget.MaterialComponents.Button.OutlinedButton"
            android:layout_width="match_parent"
            android:layout_height="60dp"
            android:text="@string/onb_lang_en"
            android:textSize="18sp"
            android:textColor="#222222"
            android:layout_marginBottom="32dp"/>

        <Button
            android:id="@+id/btn_next_lang"
            android:layout_width="match_parent"
            android:layout_height="56dp"
            android:text="@string/onb_next"
            android:textSize="16sp"
            android:textColor="#FFFFFF"
            android:backgroundTint="#FF6B35"
            android:enabled="false"/>

    </LinearLayout>

    <!-- ── Step 1: Age ── -->
    <LinearLayout
        android:id="@+id/step_age"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:gravity="center"
        android:padding="32dp"
        android:visibility="gone">

        <Button
            android:id="@+id/btn_back_age"
            style="@style/Widget.MaterialComponents.Button.TextButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="start"
            android:text="@string/onb_back"
            android:textColor="#FF6B35"
            android:layout_marginBottom="16dp"/>

        <TextView
            android:id="@+id/tv_age_title"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/onb_age_title"
            android:textSize="20sp"
            android:textStyle="bold"
            android:textColor="#222222"
            android:layout_marginBottom="28dp"/>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:layout_marginBottom="32dp">

            <Button
                android:id="@+id/btn_age_24"
                style="@style/Widget.MaterialComponents.Button.OutlinedButton"
                android:layout_width="0dp"
                android:layout_height="100dp"
                android:layout_weight="1"
                android:text="@string/onb_age_24"
                android:textSize="15sp"
                android:textColor="#222222"
                android:layout_marginEnd="8dp"/>

            <Button
                android:id="@+id/btn_age_57"
                style="@style/Widget.MaterialComponents.Button.OutlinedButton"
                android:layout_width="0dp"
                android:layout_height="100dp"
                android:layout_weight="1"
                android:text="@string/onb_age_57"
                android:textSize="15sp"
                android:textColor="#222222"
                android:layout_marginHorizontal="4dp"/>

            <Button
                android:id="@+id/btn_age_8plus"
                style="@style/Widget.MaterialComponents.Button.OutlinedButton"
                android:layout_width="0dp"
                android:layout_height="100dp"
                android:layout_weight="1"
                android:text="@string/onb_age_8plus"
                android:textSize="15sp"
                android:textColor="#222222"
                android:layout_marginStart="8dp"/>

        </LinearLayout>

        <Button
            android:id="@+id/btn_next_age"
            android:layout_width="match_parent"
            android:layout_height="56dp"
            android:text="@string/onb_next"
            android:textSize="16sp"
            android:textColor="#FFFFFF"
            android:backgroundTint="#FF6B35"
            android:enabled="false"/>

    </LinearLayout>

    <!-- ── Step 2: Kidzo Intro ── -->
    <LinearLayout
        android:id="@+id/step_kidzo"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:gravity="center"
        android:padding="32dp"
        android:visibility="gone">

        <ImageView
            android:id="@+id/img_kidzo"
            android:layout_width="160dp"
            android:layout_height="160dp"
            android:src="@drawable/splash_icon"
            android:contentDescription="@null"
            android:layout_marginBottom="32dp"/>

        <TextView
            android:id="@+id/tv_greeting"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Salom! Men Kidzo 🐥\nKidZone\'ga xush kelibsiz!"
            android:textSize="22sp"
            android:textStyle="bold"
            android:textColor="#222222"
            android:textAlignment="center"
            android:lineSpacingMultiplier="1.3"
            android:layout_marginBottom="48dp"/>

        <Button
            android:id="@+id/btn_start"
            android:layout_width="match_parent"
            android:layout_height="64dp"
            android:text="Boshlash 🚀"
            android:textSize="18sp"
            android:textColor="#FFFFFF"
            android:backgroundTint="#FF6B35"/>

    </LinearLayout>

</FrameLayout>
```

- [ ] **Step 2: Commit**

```
git add app/src/main/res/layout/activity_onboarding.xml
git commit -m "feat(onboarding): add activity_onboarding layout"
```

---

## Task 3: OnboardingActivity

**Files:**
- Create: `app/src/main/java/uz/kidzone/app/OnboardingActivity.java`

- [ ] **Step 1: Create OnboardingActivity.java**

Create `app/src/main/java/uz/kidzone/app/OnboardingActivity.java`:

```java
package uz.kidzone.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import java.util.Locale;

public class OnboardingActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    static final String PREFS   = "kz_prefs";
    static final String KEY_DONE = "kz_onboarding_done";
    static final String KEY_LANG = "kz_lang";
    static final String KEY_AGE  = "kz_age";

    private int currentStep = 0;
    private String selectedLang = null;
    private String selectedAge  = null;
    private TextToSpeech tts;
    private boolean ttsReady = false;

    private View stepLang, stepAge, stepKidzo;
    private Button btnUz, btnRu, btnEn, btnNextLang;
    private Button btnAge24, btnAge57, btnAge8plus, btnNextAge, btnBackAge;
    private ImageView imgKidzo;
    private TextView tvGreeting;
    private Button btnStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_onboarding);

        tts = new TextToSpeech(this, this);
        bindViews();
        setupClickListeners();
        showStep(0);
    }

    private void bindViews() {
        stepLang  = findViewById(R.id.step_lang);
        stepAge   = findViewById(R.id.step_age);
        stepKidzo = findViewById(R.id.step_kidzo);

        btnUz       = findViewById(R.id.btn_lang_uz);
        btnRu       = findViewById(R.id.btn_lang_ru);
        btnEn       = findViewById(R.id.btn_lang_en);
        btnNextLang = findViewById(R.id.btn_next_lang);

        btnAge24    = findViewById(R.id.btn_age_24);
        btnAge57    = findViewById(R.id.btn_age_57);
        btnAge8plus = findViewById(R.id.btn_age_8plus);
        btnNextAge  = findViewById(R.id.btn_next_age);
        btnBackAge  = findViewById(R.id.btn_back_age);

        imgKidzo   = findViewById(R.id.img_kidzo);
        tvGreeting = findViewById(R.id.tv_greeting);
        btnStart   = findViewById(R.id.btn_start);
    }

    private void setupClickListeners() {
        btnUz.setOnClickListener(v -> selectLang("uz"));
        btnRu.setOnClickListener(v -> selectLang("ru"));
        btnEn.setOnClickListener(v -> selectLang("en"));
        btnNextLang.setOnClickListener(v -> { if (selectedLang != null) showStep(1); });

        btnAge24.setOnClickListener(v -> selectAge("2-4"));
        btnAge57.setOnClickListener(v -> selectAge("5-7"));
        btnAge8plus.setOnClickListener(v -> selectAge("8+"));
        btnNextAge.setOnClickListener(v -> { if (selectedAge != null) showStep(2); });
        btnBackAge.setOnClickListener(v -> showStep(0));

        btnStart.setOnClickListener(v -> finishOnboarding());
    }

    private void selectLang(String lang) {
        selectedLang = lang;
        btnUz.setSelected("uz".equals(lang));
        btnRu.setSelected("ru".equals(lang));
        btnEn.setSelected("en".equals(lang));
        btnNextLang.setEnabled(true);
    }

    private void selectAge(String age) {
        selectedAge = age;
        btnAge24.setSelected("2-4".equals(age));
        btnAge57.setSelected("5-7".equals(age));
        btnAge8plus.setSelected("8+".equals(age));
        btnNextAge.setEnabled(true);
    }

    void showStep(int step) {
        currentStep = step;
        stepLang.setVisibility(step == 0 ? View.VISIBLE : View.GONE);
        stepAge.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        stepKidzo.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        if (step == 2) {
            animateKidzo();
            updateGreeting();
            speakGreeting();
        }
    }

    private void animateKidzo() {
        imgKidzo.setScaleX(0f);
        imgKidzo.setScaleY(0f);
        imgKidzo.animate()
            .scaleX(1.15f).scaleY(1.15f).setDuration(400)
            .withEndAction(() ->
                imgKidzo.animate().scaleX(1f).scaleY(1f).setDuration(200).start())
            .start();
    }

    void updateGreeting() {
        String greeting, startLabel;
        if ("ru".equals(selectedLang)) {
            greeting   = "Привет! Я Кидзо 🐥\nДобро пожаловать в KidZone!";
            startLabel = "Начать 🚀";
        } else if ("en".equals(selectedLang)) {
            greeting   = "Hi! I'm Kidzo 🐥\nWelcome to KidZone!";
            startLabel = "Let's go 🚀";
        } else {
            greeting   = "Salom! Men Kidzo 🐥\nKidZone'ga xush kelibsiz!";
            startLabel = "Boshlash 🚀";
        }
        tvGreeting.setText(greeting);
        btnStart.setText(startLabel);
    }

    private void speakGreeting() {
        if (!ttsReady) return;
        Locale locale = "ru".equals(selectedLang) ? new Locale("ru", "RU")
                      : "en".equals(selectedLang) ? Locale.ENGLISH
                      : new Locale("uz", "UZ");
        int result = tts.setLanguage(locale);
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.setLanguage(Locale.getDefault());
        }
        String spoken = "ru".equals(selectedLang) ? "Привет! Я Кидзо. Добро пожаловать в KidZone!"
                      : "en".equals(selectedLang) ? "Hi! I'm Kidzo. Welcome to KidZone!"
                      : "Salom! Men Kidzo. KidZone'ga xush kelibsiz!";
        tts.speak(spoken, TextToSpeech.QUEUE_FLUSH, null, "kidzo_greeting");
    }

    private void finishOnboarding() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putBoolean(KEY_DONE, true)
            .putString(KEY_LANG, selectedLang != null ? selectedLang : "uz")
            .putString(KEY_AGE,  selectedAge  != null ? selectedAge  : "2-4")
            .apply();
        if (tts != null) tts.stop();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = true;
            if (currentStep == 2) speakGreeting();
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
```

- [ ] **Step 2: Commit**

```
git add app/src/main/java/uz/kidzone/app/OnboardingActivity.java
git commit -m "feat(onboarding): implement OnboardingActivity (3 steps + TTS)"
```

---

## Task 4: Wire MainActivity + KidWebViewManager

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/KidWebViewManager.java`
- Modify: `app/src/main/java/uz/kidzone/app/MainActivity.java`

- [ ] **Step 1: Add onPageReadyCallback to KidWebViewManager**

In `KidWebViewManager.java`, add two fields after `private String currentLanguage = "en";`:

```java
    private Runnable onPageReadyCallback;
    private boolean pageReadyCalled = false;
```

Add a public setter after `getLanguage()`:

```java
    public void setOnPageReadyCallback(Runnable callback) {
        this.onPageReadyCallback = callback;
    }
```

Replace the static inner class `InternalWebViewClient` with a non-static one that fires the callback on first `onPageFinished`:

```java
    private class InternalWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            if (url.startsWith("file://")) return false;
            try {
                android.content.Intent intent = new android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(url)
                );
                view.getContext().startActivity(intent);
            } catch (Exception e) {
                Log.w(TAG, "Cannot open external URL: " + url);
            }
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (!pageReadyCalled && onPageReadyCallback != null) {
                pageReadyCalled = true;
                view.post(onPageReadyCallback);
            }
        }
    }
```

- [ ] **Step 2: Add first-launch check and lang injection to MainActivity**

In `MainActivity.java`, add a field after `private int gameLaunchCount = 0;`:

```java
    private android.content.SharedPreferences kzPrefs;
```

Replace the `onCreate` method:

```java
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        kzPrefs = getSharedPreferences(OnboardingActivity.PREFS, MODE_PRIVATE);
        if (!kzPrefs.getBoolean(OnboardingActivity.KEY_DONE, false)) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }

        initializeUI();
        initializeManagers();
        setupKidzoFab();
    }
```

In `initializeManagers()`, add these two lines right after `webViewManager.loadUrl(INDEX_PATH);`:

```java
        final String lang = kzPrefs.getString(OnboardingActivity.KEY_LANG, "uz");
        final String age  = kzPrefs.getString(OnboardingActivity.KEY_AGE,  "2-4");
        webViewManager.setOnPageReadyCallback(() ->
            webViewManager.evaluateJavascript(
                "localStorage.setItem('kz-lang','" + lang + "');" +
                "localStorage.setItem('kz-age','"  + age  + "');")
        );
```

Also add this import at the top of MainActivity.java if not present:

```java
import android.content.Intent;
```

- [ ] **Step 3: Commit**

```
git add app/src/main/java/uz/kidzone/app/KidWebViewManager.java \
        app/src/main/java/uz/kidzone/app/MainActivity.java
git commit -m "feat(onboarding): wire first-launch check and localStorage injection"
```

---

## Task 5: Build + Install + Verify

**Files:** none (verification only)

- [ ] **Step 1: Build**

```
.\gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Install**

```
"C:\Users\Komiljon\AppData\Local\Android\Sdk\platform-tools\adb.exe" install -r -t app\build\outputs\apk\debug\app-debug.apk
```

Expected: `Success`

- [ ] **Step 3: Clear app data (simulate fresh install)**

```
"C:\Users\Komiljon\AppData\Local\Android\Sdk\platform-tools\adb.exe" shell pm clear uz.kidzone.app.debug
```

Expected: `Success`

- [ ] **Step 4: Launch and smoke test**

```
"C:\Users\Komiljon\AppData\Local\Android\Sdk\platform-tools\adb.exe" shell am start -n "uz.kidzone.app.debug/uz.kidzone.app.MainActivity"
```

Manual checks:
1. OnboardingActivity opens (not MainActivity)
2. Step 0: "Keyingi" button is disabled until a language is tapped
3. Tap 🇺🇿 O'zbek → button highlights, "Keyingi" enabled
4. Tap "Keyingi" → Step 1 (age) appears
5. Tap back → Step 0 reappears with 🇺🇿 still highlighted
6. Tap 🧒 5–7 → "Keyingi" enabled → Step 2
7. Kidzo image animates in (spring scale 0→1.15→1.0)
8. TTS plays Uzbek greeting
9. Tap "Boshlash 🚀" → MainActivity opens normally
10. Relaunch app → MainActivity opens directly (onboarding skipped)
11. Verify: O'yinlar tab loads; language is Uzbek in WebView (check tab labels)

- [ ] **Step 5: Commit build verification**

```
git commit --allow-empty -m "chore: onboarding verified on device"
```
