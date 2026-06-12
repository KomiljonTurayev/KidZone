package uz.kidzone.app;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.view.WindowCompat;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ParentalDashboardActivity extends AppCompatActivity {

    private static final String[] CATEGORIES  = {
        "alphabet","animals","dance","family","games","heroes","lullaby","nature","space"
    };
    private static final String[] CAT_EMOJIS  = {
        "🔤","🐘","💃","👨‍👩‍👧","🎮","🦸","🌙","🌿","🚀"
    };

    private SharedPreferences prefs;
    private ParentalStatsManager stats;

    private TextView tvToday;
    private LinearLayout chartLayout, chartLabels, gamesRow, categoriesLayout;
    private MaterialButton btnAge24, btnAge57, btnAge8plus;
    private MaterialButton btnLimitMinus, btnLimitPlus;
    private TextView tvLimitVal;
    private MaterialButton btnChangePIN;
    private LinearLayout profilesLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_parental_dashboard);

        prefs = getSharedPreferences(OnboardingActivity.PREFS, MODE_PRIVATE);
        stats = new ParentalStatsManager(this);

        bindViews();
        loadProfiles();
        loadStats();
        setupAgeButtons();
        setupCategoryToggles();
        setupTimeLimitControls();
        setupPinButton();

        findViewById(R.id.pd_back).setOnClickListener(v -> finish());
    }

    private void bindViews() {
        tvToday          = findViewById(R.id.pd_today_minutes);
        chartLayout      = findViewById(R.id.pd_chart);
        chartLabels      = findViewById(R.id.pd_chart_labels);
        gamesRow         = findViewById(R.id.pd_games_row);
        btnAge24         = findViewById(R.id.pd_age_24);
        btnAge57         = findViewById(R.id.pd_age_57);
        btnAge8plus      = findViewById(R.id.pd_age_8plus);
        categoriesLayout = findViewById(R.id.pd_categories);
        btnLimitMinus    = findViewById(R.id.pd_limit_minus);
        btnLimitPlus     = findViewById(R.id.pd_limit_plus);
        tvLimitVal       = findViewById(R.id.pd_limit_val);
        btnChangePIN     = findViewById(R.id.pd_change_pin);
        profilesLayout   = findViewById(R.id.pd_profiles);
    }

    // ── Stats ────────────────────────────────────────────────────────────────

    private void loadStats() {
        tvToday.setText("Today: " + stats.getTodayMinutes() + " min");
        drawChart(stats.getWeeklyMinutes());
        populateGames(stats.getTodayGames());
    }

    private void loadProfiles() {
        MainActivity main = MainActivity.instance != null ? MainActivity.instance.get() : null;
        if (main == null) return;
        main.evaluateJs(
            "JSON.stringify(window.profileManager ? profileManager.getAll() : [])",
            value -> runOnUiThread(() -> renderProfiles(value))
        );
    }

    private void renderProfiles(String json) {
        if (profilesLayout == null) return;
        profilesLayout.removeAllViews();
        if (json == null || json.equals("null")) return;
        if (json.startsWith("\"")) json = json.substring(1, json.length() - 1).replace("\\\"", "\"");
        try {
            org.json.JSONArray arr = new org.json.JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject p = arr.getJSONObject(i);
                String id   = p.getString("id");
                String name = p.getString("name");
                profilesLayout.addView(buildProfileRow(id, name));
            }
        } catch (org.json.JSONException e) { /* ignore */ }

        if (profilesLayout.getChildCount() < 5) {
            MaterialButton addBtn = new MaterialButton(this);
            addBtn.setText("+ Profil qo'shish");
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, dp(8), 0, 0);
            addBtn.setLayoutParams(lp);
            addBtn.setOnClickListener(v -> showAddProfileDialog());
            profilesLayout.addView(addBtn);
        }
    }

    private View buildProfileRow(String id, String name) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));

        TextView label = new TextView(this);
        label.setText("👤  " + name);
        label.setTextSize(14f);
        label.setTextColor(0xFF222222);
        label.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        MaterialButton delBtn = new MaterialButton(this);
        delBtn.setText("O'chirish");
        delBtn.setTextSize(12f);
        delBtn.setOnClickListener(v -> confirmDeleteProfile(id, name));

        row.addView(label);
        row.addView(delBtn);
        return row;
    }

    private void showAddProfileDialog() {
        android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(this);
        b.setTitle("Yangi profil");
        android.widget.EditText et = new android.widget.EditText(this);
        et.setHint("Ism kiriting");
        et.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        b.setView(et);
        b.setPositiveButton("Yaratish", (d, w) -> {
            String name = et.getText().toString().trim();
            if (name.isEmpty()) return;
            injectJs("if(window.profileManager) profileManager.create('" +
                    name.replace("'", "\\'") + "', 0)");
            profilesLayout.postDelayed(this::loadProfiles, 400);
        });
        b.setNegativeButton("Bekor", null);
        b.show();
    }

    private void confirmDeleteProfile(String id, String name) {
        new android.app.AlertDialog.Builder(this)
            .setTitle("Profilni o'chirish")
            .setMessage("\"" + name + "\" profilini o'chirishni tasdiqlaysizmi?")
            .setPositiveButton("O'chirish", (d, w) -> {
                injectJs("if(window.profileManager) profileManager.remove('" + id + "')");
                profilesLayout.postDelayed(this::loadProfiles, 400);
            })
            .setNegativeButton("Bekor", null)
            .show();
    }

    private void drawChart(int[] weekly) {
        chartLayout.removeAllViews();
        chartLabels.removeAllViews();

        int max = 1;
        for (int v : weekly) if (v > max) max = v;

        SimpleDateFormat sdf = new SimpleDateFormat("EEE", Locale.US);
        Calendar cal = Calendar.getInstance();
        String[] dayNames = new String[7];
        for (int i = 6; i >= 0; i--) {
            dayNames[i] = sdf.format(cal.getTime());
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }

        int maxPx  = dp(72);
        int minPx  = dp(4);

        for (int i = 0; i < 7; i++) {
            boolean isToday = (i == 6);

            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            cp.setMargins(2, 0, 2, 0);
            col.setLayoutParams(cp);

            View bar = new View(this);
            int h = minPx + (int) ((float) weekly[i] / max * (maxPx - minPx));
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, h);
            bar.setLayoutParams(bp);
            GradientDrawable gd = new GradientDrawable();
            gd.setColor(isToday ? 0xFFFF6B35 : 0xFFFFB894);
            gd.setCornerRadii(new float[]{6,6,6,6,0,0,0,0});
            bar.setBackground(gd);
            col.addView(bar);
            chartLayout.addView(col);

            TextView label = new TextView(this);
            label.setText(dayNames[i]);
            label.setTextSize(9f);
            label.setTextColor(isToday ? 0xFFFF6B35 : 0xFF888888);
            label.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            label.setLayoutParams(lp);
            chartLabels.addView(label);
        }
    }

    private void populateGames(List<String> games) {
        gamesRow.removeAllViews();
        if (games.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("—");
            tv.setTextColor(0xFF888888);
            tv.setTextSize(14f);
            gamesRow.addView(tv);
            return;
        }
        for (String id : games) {
            TextView tv = new TextView(this);
            tv.setText(emojiForId(id));
            tv.setTextSize(22f);
            tv.setPadding(4, 0, 4, 0);
            gamesRow.addView(tv);
        }
    }

    private static String emojiForId(String id) {
        if (id.startsWith("story")) return "📖";
        if (id.startsWith("song"))  return "🎵";
        return "🎮";
    }

    // ── Age group ─────────────────────────────────────────────────────────────

    private void setupAgeButtons() {
        String cur = prefs.getString(OnboardingActivity.KEY_AGE, "2-4");
        applyAgeHighlight(cur);
        btnAge24.setOnClickListener(v   -> selectAge("2-4"));
        btnAge57.setOnClickListener(v   -> selectAge("5-7"));
        btnAge8plus.setOnClickListener(v -> selectAge("8+"));
    }

    private void selectAge(String age) {
        prefs.edit().putString(OnboardingActivity.KEY_AGE, age).apply();
        applyAgeHighlight(age);
        injectJs("localStorage.setItem('kz-age','" + age + "');");
    }

    private void applyAgeHighlight(String age) {
        highlightBtn(btnAge24,    "2-4".equals(age));
        highlightBtn(btnAge57,    "5-7".equals(age));
        highlightBtn(btnAge8plus, "8+".equals(age));
    }

    private void highlightBtn(MaterialButton b, boolean on) {
        if (on) {
            b.setBackgroundTintList(ColorStateList.valueOf(0xFFFF6B35));
            b.setStrokeColor(ColorStateList.valueOf(0xFFFF6B35));
            b.setTextColor(0xFFFFFFFF);
        } else {
            b.setBackgroundTintList(ColorStateList.valueOf(0xFFFFF8F0));
            b.setStrokeColor(ColorStateList.valueOf(0xFFCCCCCC));
            b.setTextColor(0xFF222222);
        }
    }

    // ── Category toggles ──────────────────────────────────────────────────────

    private void setupCategoryToggles() {
        List<String> offList = parseList(prefs.getString("kz_cat_off", ""));
        for (int i = 0; i < CATEGORIES.length; i++) {
            categoriesLayout.addView(buildCatRow(CATEGORIES[i], CAT_EMOJIS[i],
                    !offList.contains(CATEGORIES[i])));
        }
    }

    private View buildCatRow(String cat, String emoji, boolean on) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(44)));

        TextView label = new TextView(this);
        label.setText(emoji + "  " + capitalize(cat));
        label.setTextSize(14f);
        label.setTextColor(0xFF222222);
        label.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        SwitchCompat sw = new SwitchCompat(this);
        sw.setChecked(on);
        sw.setOnCheckedChangeListener((btn, checked) -> toggleCategory(cat, checked));

        row.addView(label);
        row.addView(sw);
        return row;
    }

    private void toggleCategory(String cat, boolean enabled) {
        List<String> off = parseList(prefs.getString("kz_cat_off", ""));
        if (enabled) off.remove(cat);
        else if (!off.contains(cat)) off.add(cat);
        String val = joinList(off);
        prefs.edit().putString("kz_cat_off", val).apply();
        injectJs("localStorage.setItem('kz-cat-off','" + val + "');");
    }

    // ── Time limit ────────────────────────────────────────────────────────────

    private void setupTimeLimitControls() {
        refreshLimitDisplay();
        btnLimitMinus.setOnClickListener(v -> {
            stats.setTimeLimitMinutes(Math.max(0, stats.getTimeLimitMinutes() - 15));
            refreshLimitDisplay();
        });
        btnLimitPlus.setOnClickListener(v -> {
            stats.setTimeLimitMinutes(Math.min(180, stats.getTimeLimitMinutes() + 15));
            refreshLimitDisplay();
        });
    }

    private void refreshLimitDisplay() {
        int lim = stats.getTimeLimitMinutes();
        tvLimitVal.setText(lim == 0 ? "No limit" : lim + " min");
    }

    // ── PIN ───────────────────────────────────────────────────────────────────

    private void setupPinButton() {
        String pin = prefs.getString("kz_pin", null);
        btnChangePIN.setText((pin != null && !pin.isEmpty()) ? "Change PIN" : "Set PIN");
        btnChangePIN.setOnClickListener(v ->
            PinDialogHelper.showCreate(this, newPin -> {
                prefs.edit().putString("kz_pin", newPin.isEmpty() ? "" : PinUtil.hash(newPin)).apply();
                btnChangePIN.setText(newPin.isEmpty() ? "Set PIN" : "Change PIN");
            })
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void injectJs(String script) {
        MainActivity main = MainActivity.instance != null ? MainActivity.instance.get() : null;
        if (main != null) main.injectJs(script);
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static List<String> parseList(String csv) {
        if (csv == null || csv.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(csv.split(",")));
    }

    private static String joinList(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(list.get(i));
        }
        return sb.toString();
    }
}
