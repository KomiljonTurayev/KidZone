package uz.kidzone.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class OnboardingActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    static final String PREFS    = "kz_prefs";
    static final String KEY_DONE = "kz_onboarding_done";
    static final String KEY_LANG = "kz_lang";
    static final String KEY_AGE  = "kz_age";

    private int currentStep = 0;
    private String selectedLang = null;
    private String selectedAge  = null;
    private TextToSpeech tts;
    private boolean ttsReady = false;

    private View stepLang, stepAge, stepKidzo;
    private MaterialButton btnUz, btnRu, btnEn, btnNextLang;
    private MaterialButton btnAge24, btnAge57, btnAge8plus, btnNextAge, btnBackAge;
    private ImageView imgKidzo;
    private TextView tvGreeting;
    private MaterialButton btnStart;

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
        highlightBtn(btnUz, "uz".equals(lang));
        highlightBtn(btnRu, "ru".equals(lang));
        highlightBtn(btnEn, "en".equals(lang));
        btnNextLang.setEnabled(true);
    }

    private void selectAge(String age) {
        selectedAge = age;
        highlightBtn(btnAge24,    "2-4".equals(age));
        highlightBtn(btnAge57,    "5-7".equals(age));
        highlightBtn(btnAge8plus, "8+".equals(age));
        btnNextAge.setEnabled(true);
    }

    private void highlightBtn(MaterialButton btn, boolean selected) {
        if (selected) {
            btn.setBackgroundTintList(ColorStateList.valueOf(0xFFFF6B35));
            btn.setStrokeColor(ColorStateList.valueOf(0xFFFF6B35));
            btn.setTextColor(0xFFFFFFFF);
        } else {
            btn.setBackgroundTintList(ColorStateList.valueOf(0xFFFFF8F0));
            btn.setStrokeColor(ColorStateList.valueOf(0xFFCCCCCC));
            btn.setTextColor(0xFF222222);
        }
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
