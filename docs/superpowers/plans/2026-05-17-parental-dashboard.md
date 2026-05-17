# Parental Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a PIN-protected parental dashboard reachable by long-pressing the music button, showing daily/weekly play-time stats, age group and category content filtering, and a hard daily time limit with a lock overlay.

**Architecture:** `ParentalStatsManager` owns all tracking logic and SharedPreferences I/O. `PinDialogHelper` is a stateless utility that shows PIN create/enter dialogs. `ParentalDashboardActivity` is a single-screen Activity with four card sections. `MainActivity` wires everything: stats lifecycle, AdMobBridge entry point, time-lock handler, and lock overlay.

**Tech Stack:** Android Java, JUnit 4 + Robolectric 4.13 (unit tests), Material Components, SwitchCompat, ObjectAnimator.

---

## File Map

| Action | File |
|--------|------|
| Modify | `app/build.gradle` |
| Create | `app/src/test/java/uz/kidzone/app/ParentalStatsManagerTest.java` |
| Create | `app/src/main/java/uz/kidzone/app/ParentalStatsManager.java` |
| Create | `app/src/main/java/uz/kidzone/app/PinDialogHelper.java` |
| Modify | `app/src/main/res/values/strings.xml` |
| Modify | `app/src/main/AndroidManifest.xml` |
| Create | `app/src/main/res/drawable/pin_dot_empty.xml` |
| Create | `app/src/main/res/drawable/pin_dot_filled.xml` |
| Create | `app/src/main/res/layout/dialog_pin.xml` |
| Create | `app/src/main/res/layout/activity_parental_dashboard.xml` |
| Create | `app/src/main/res/layout/view_lock_overlay.xml` |
| Create | `app/src/main/java/uz/kidzone/app/ParentalDashboardActivity.java` |
| Modify | `app/src/main/java/uz/kidzone/app/MainActivity.java` |
| Modify | `app/src/main/assets/www/index.html` |

---

## Task 1: ParentalStatsManager (TDD)

**Files:**
- Modify: `app/build.gradle`
- Create: `app/src/test/java/uz/kidzone/app/ParentalStatsManagerTest.java`
- Create: `app/src/main/java/uz/kidzone/app/ParentalStatsManager.java`

- [ ] **Step 1: Add Robolectric to app/build.gradle**

In the `android { }` block, add after `compileOptions { ... }`:
```groovy
testOptions {
    unitTests {
        includeAndroidResources = true
    }
}
```

In the `dependencies { }` block, add:
```groovy
testImplementation 'org.robolectric:robolectric:4.13'
```

- [ ] **Step 2: Write the failing tests**

Create `app/src/test/java/uz/kidzone/app/ParentalStatsManagerTest.java`:
```java
package uz.kidzone.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class ParentalStatsManagerTest {

    private SharedPreferences prefs;
    private ParentalStatsManager mgr;

    @Before
    public void setUp() {
        prefs = RuntimeEnvironment.getApplication()
                .getSharedPreferences("test_prefs", Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
        mgr = new ParentalStatsManager(prefs);
    }

    @Test
    public void getTodayMinutes_noData_returnsZero() {
        assertEquals(0, mgr.getTodayMinutes());
    }

    @Test
    public void getTodayMinutes_savedData_returnsSaved() {
        prefs.edit().putInt(todayKey(), 20).commit();
        assertEquals(20, mgr.getTodayMinutes());
    }

    @Test
    public void isTimeLimitReached_limitZero_alwaysFalse() {
        mgr.setTimeLimitMinutes(0);
        prefs.edit().putInt(todayKey(), 999).commit();
        assertFalse(mgr.isTimeLimitReached());
    }

    @Test
    public void isTimeLimitReached_belowLimit_false() {
        mgr.setTimeLimitMinutes(30);
        prefs.edit().putInt(todayKey(), 29).commit();
        assertFalse(mgr.isTimeLimitReached());
    }

    @Test
    public void isTimeLimitReached_atLimit_true() {
        mgr.setTimeLimitMinutes(30);
        prefs.edit().putInt(todayKey(), 30).commit();
        assertTrue(mgr.isTimeLimitReached());
    }

    @Test
    public void onGameLaunched_noDuplicatesWithinDay() {
        mgr.onGameLaunched("story-001");
        mgr.onGameLaunched("story-001");
        assertEquals(1, mgr.getTodayGames().size());
    }

    @Test
    public void onGameLaunched_multipleDistinct_countsAll() {
        mgr.onGameLaunched("story-001");
        mgr.onGameLaunched("song-001");
        assertEquals(2, mgr.getTodayGames().size());
    }

    @Test
    public void getWeeklyMinutes_returns7Elements() {
        assertEquals(7, mgr.getWeeklyMinutes().length);
    }

    @Test
    public void getWeeklyMinutes_todayAtIndex6() {
        prefs.edit().putInt(todayKey(), 45).commit();
        assertEquals(45, mgr.getWeeklyMinutes()[6]);
    }

    @Test
    public void setTimeLimitMinutes_negativeClampedToZero() {
        mgr.setTimeLimitMinutes(-10);
        assertEquals(0, mgr.getTimeLimitMinutes());
    }

    @Test
    public void onSessionEnd_calledBeforeStart_doesNothing() {
        mgr.onSessionEnd();
        assertEquals(0, mgr.getTodayMinutes());
    }

    private static String todayKey() {
        return "kz_pt_" + new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US)
                .format(new java.util.Date());
    }
}
```

- [ ] **Step 3: Run tests — verify they all FAIL**

```
.\gradlew test --tests "uz.kidzone.app.ParentalStatsManagerTest" -i 2>&1 | Select-String "FAIL|ERROR|PASS|BUILD"
```
Expected: `BUILD FAILED` — `ParentalStatsManager` does not exist yet.

- [ ] **Step 4: Implement ParentalStatsManager**

Create `app/src/main/java/uz/kidzone/app/ParentalStatsManager.java`:
```java
package uz.kidzone.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ParentalStatsManager {

    private static final String KEY_TIME_LIMIT = "kz_time_limit";
    private final SharedPreferences prefs;
    private long sessionStartMs = 0;

    public ParentalStatsManager(Context ctx) {
        this(ctx.getSharedPreferences("kz_prefs", Context.MODE_PRIVATE));
    }

    ParentalStatsManager(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public void onSessionStart() {
        sessionStartMs = System.currentTimeMillis();
    }

    public void onSessionEnd() {
        if (sessionStartMs == 0) return;
        int elapsed = (int) ((System.currentTimeMillis() - sessionStartMs) / 60_000L);
        sessionStartMs = 0;
        if (elapsed <= 0) return;
        String key = todayPtKey();
        prefs.edit().putInt(key, prefs.getInt(key, 0) + elapsed).apply();
    }

    public void onGameLaunched(String gameId) {
        if (gameId == null || gameId.isEmpty()) return;
        String key = todayGlKey();
        String existing = prefs.getString(key, "");
        List<String> list = parseList(existing);
        if (!list.contains(gameId)) {
            list.add(gameId);
            prefs.edit().putString(key, joinList(list)).apply();
        }
    }

    public int getTodayMinutes() {
        int saved = prefs.getInt(todayPtKey(), 0);
        int current = sessionStartMs > 0
                ? (int) ((System.currentTimeMillis() - sessionStartMs) / 60_000L) : 0;
        return saved + current;
    }

    public int[] getWeeklyMinutes() {
        int[] result = new int[7];
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.US);
        Calendar cal = Calendar.getInstance();
        for (int i = 6; i >= 0; i--) {
            String key = "kz_pt_" + sdf.format(cal.getTime());
            result[i] = prefs.getInt(key, 0);
            if (i == 6 && sessionStartMs > 0)
                result[i] += (int) ((System.currentTimeMillis() - sessionStartMs) / 60_000L);
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }
        return result;
    }

    public List<String> getTodayGames() {
        return parseList(prefs.getString(todayGlKey(), ""));
    }

    public int getTimeLimitMinutes() {
        return prefs.getInt(KEY_TIME_LIMIT, 0);
    }

    public void setTimeLimitMinutes(int minutes) {
        prefs.edit().putInt(KEY_TIME_LIMIT, Math.max(0, minutes)).apply();
    }

    public boolean isTimeLimitReached() {
        int limit = getTimeLimitMinutes();
        return limit > 0 && getTodayMinutes() >= limit;
    }

    private static String todayPtKey() {
        return "kz_pt_" + new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
    }

    private static String todayGlKey() {
        return "kz_gl_" + new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
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
```

- [ ] **Step 5: Run tests — verify all 11 PASS**

```
.\gradlew test --tests "uz.kidzone.app.ParentalStatsManagerTest" 2>&1 | Select-String "tests|PASS|FAIL|BUILD"
```
Expected: `BUILD SUCCESSFUL`, 11 tests passing.

- [ ] **Step 6: Commit**

```
git add app/build.gradle app/src/test/java/uz/kidzone/app/ParentalStatsManagerTest.java app/src/main/java/uz/kidzone/app/ParentalStatsManager.java
git commit -m "feat(parental): ParentalStatsManager — session tracking, game log, time limit (TDD)"
```

---

## Task 2: PinDialogHelper

**Files:**
- Create: `app/src/main/java/uz/kidzone/app/PinDialogHelper.java`

- [ ] **Step 1: Implement PinDialogHelper**

Create `app/src/main/java/uz/kidzone/app/PinDialogHelper.java`:
```java
package uz.kidzone.app;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class PinDialogHelper {

    public interface OnPinResult {
        void onResult(String pin); // 4-digit string, or "" if skipped
    }

    public interface OnPinVerified {
        void onVerified();
    }

    /** Show "Create PIN" flow: enter, confirm, skip option. Calls cb.onResult(pin) or cb.onResult("") on skip. */
    public static void showCreate(Context ctx, OnPinResult cb) {
        View view = LayoutInflater.from(ctx).inflate(R.layout.dialog_pin, null);
        TextView tvTitle = view.findViewById(R.id.pin_title);
        tvTitle.setText("Create a parent PIN");

        final StringBuilder entered = new StringBuilder();
        final String[] firstPin = {null};

        AlertDialog dialog = new MaterialAlertDialogBuilder(ctx)
                .setView(view).setCancelable(false).create();

        wireKeypad(view, entered, () -> {
            String pin = entered.toString();
            if (firstPin[0] == null) {
                firstPin[0] = pin;
                entered.setLength(0);
                updateDots(view, 0);
                tvTitle.setText("Confirm PIN");
            } else if (pin.equals(firstPin[0])) {
                dialog.dismiss();
                cb.onResult(pin);
            } else {
                shakeDots(view);
                entered.setLength(0);
                firstPin[0] = null;
                updateDots(view, 0);
                tvTitle.setText("Create a parent PIN");
            }
        });

        view.findViewById(R.id.pin_skip).setVisibility(View.VISIBLE);
        view.findViewById(R.id.pin_skip).setOnClickListener(v -> {
            dialog.dismiss();
            cb.onResult("");
        });

        dialog.show();
    }

    /** Show "Enter PIN" flow — no skip button. Calls cb.onVerified() on correct PIN. */
    public static void showEnter(Context ctx, String expectedPin, OnPinVerified cb) {
        View view = LayoutInflater.from(ctx).inflate(R.layout.dialog_pin, null);
        TextView tvTitle = view.findViewById(R.id.pin_title);
        tvTitle.setText("Enter parent PIN");
        view.findViewById(R.id.pin_skip).setVisibility(View.GONE);

        final StringBuilder entered = new StringBuilder();

        AlertDialog dialog = new MaterialAlertDialogBuilder(ctx)
                .setView(view).setCancelable(true).create();

        wireKeypad(view, entered, () -> {
            if (entered.toString().equals(expectedPin)) {
                dialog.dismiss();
                cb.onVerified();
            } else {
                shakeDots(view);
                entered.setLength(0);
                updateDots(view, 0);
            }
        });

        dialog.show();
    }

    private static void wireKeypad(View view, StringBuilder entered, Runnable onFourDigits) {
        int[] keyIds = {R.id.pin_key_1, R.id.pin_key_2, R.id.pin_key_3,
                        R.id.pin_key_4, R.id.pin_key_5, R.id.pin_key_6,
                        R.id.pin_key_7, R.id.pin_key_8, R.id.pin_key_9,
                        R.id.pin_key_0};
        String[] digits = {"1","2","3","4","5","6","7","8","9","0"};

        for (int i = 0; i < keyIds.length; i++) {
            final String d = digits[i];
            view.findViewById(keyIds[i]).setOnClickListener(v -> {
                if (entered.length() >= 4) return;
                entered.append(d);
                updateDots(view, entered.length());
                if (entered.length() == 4) onFourDigits.run();
            });
        }

        view.findViewById(R.id.pin_backspace).setOnClickListener(v -> {
            if (entered.length() > 0) {
                entered.deleteCharAt(entered.length() - 1);
                updateDots(view, entered.length());
            }
        });
    }

    static void updateDots(View root, int count) {
        int[] dotIds = {R.id.dot1, R.id.dot2, R.id.dot3, R.id.dot4};
        for (int i = 0; i < dotIds.length; i++) {
            root.findViewById(dotIds[i]).setBackgroundResource(
                    i < count ? R.drawable.pin_dot_filled : R.drawable.pin_dot_empty);
        }
    }

    static void shakeDots(View root) {
        View container = root.findViewById(R.id.pin_dots);
        ObjectAnimator anim = ObjectAnimator.ofFloat(container, "translationX",
                0f, -10f, 10f, -10f, 10f, -5f, 5f, 0f);
        anim.setDuration(400);
        anim.start();
    }
}
```

- [ ] **Step 2: Build to verify no compile errors**

```
.\gradlew compileDebugJavaSources 2>&1 | Select-String "error:|BUILD"
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```
git add app/src/main/java/uz/kidzone/app/PinDialogHelper.java
git commit -m "feat(parental): PinDialogHelper — create/enter PIN dialogs"
```

---

## Task 3: String Resources + Manifest

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Add dashboard strings to strings.xml**

Append before `</resources>`:
```xml
<!-- Parental Dashboard -->
<string name="pd_title">👨‍👩‍👧 Parent Zone</string>
<string name="pd_stats_header">📊 STATS</string>
<string name="pd_content_header">🎮 CONTENT FILTER</string>
<string name="pd_limit_header">⏰ TIME LIMIT</string>
<string name="pd_security_header">🔒 SECURITY</string>
<string name="pd_no_limit">No limit</string>
<string name="pd_set_pin">Set PIN</string>
<string name="pd_change_pin">Change PIN</string>
```

- [ ] **Step 2: Register ParentalDashboardActivity in AndroidManifest.xml**

After the `</activity>` tag for OnboardingActivity, add:
```xml
<!-- PARENTAL DASHBOARD -->
<activity
    android:name=".ParentalDashboardActivity"
    android:exported="false"
    android:screenOrientation="portrait"
    android:theme="@style/Theme.KidZone"/>
```

- [ ] **Step 3: Commit**

```
git add app/src/main/res/values/strings.xml app/src/main/AndroidManifest.xml
git commit -m "feat(parental): strings + manifest registration for ParentalDashboardActivity"
```

---

## Task 4: Drawables + Layouts

**Files:**
- Create: `app/src/main/res/drawable/pin_dot_empty.xml`
- Create: `app/src/main/res/drawable/pin_dot_filled.xml`
- Create: `app/src/main/res/layout/dialog_pin.xml`
- Create: `app/src/main/res/layout/activity_parental_dashboard.xml`
- Create: `app/src/main/res/layout/view_lock_overlay.xml`

- [ ] **Step 1: Create pin_dot_empty.xml**

`app/src/main/res/drawable/pin_dot_empty.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <stroke android:width="2dp" android:color="#CCCCCC"/>
    <size android:width="16dp" android:height="16dp"/>
</shape>
```

- [ ] **Step 2: Create pin_dot_filled.xml**

`app/src/main/res/drawable/pin_dot_filled.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="#FF6B35"/>
    <size android:width="16dp" android:height="16dp"/>
</shape>
```

- [ ] **Step 3: Create dialog_pin.xml**

`app/src/main/res/layout/dialog_pin.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="24dp"
    android:background="#FFF8F0">

    <TextView
        android:id="@+id/pin_title"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center_horizontal"
        android:text="PIN kiriting"
        android:textSize="18sp"
        android:textStyle="bold"
        android:textColor="#222222"
        android:layout_marginBottom="24dp"/>

    <LinearLayout
        android:id="@+id/pin_dots"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center_horizontal"
        android:orientation="horizontal"
        android:layout_marginBottom="24dp">
        <View android:id="@+id/dot1" android:layout_width="16dp" android:layout_height="16dp"
            android:background="@drawable/pin_dot_empty" android:layout_marginHorizontal="8dp"/>
        <View android:id="@+id/dot2" android:layout_width="16dp" android:layout_height="16dp"
            android:background="@drawable/pin_dot_empty" android:layout_marginHorizontal="8dp"/>
        <View android:id="@+id/dot3" android:layout_width="16dp" android:layout_height="16dp"
            android:background="@drawable/pin_dot_empty" android:layout_marginHorizontal="8dp"/>
        <View android:id="@+id/dot4" android:layout_width="16dp" android:layout_height="16dp"
            android:background="@drawable/pin_dot_empty" android:layout_marginHorizontal="8dp"/>
    </LinearLayout>

    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="horizontal" android:layout_marginBottom="8dp">
        <Button style="@style/Widget.MaterialComponents.Button.OutlinedButton"
            android:id="@+id/pin_key_1" android:layout_width="0dp" android:layout_height="52dp"
            android:layout_weight="1" android:text="1" android:textSize="18sp" android:layout_marginEnd="4dp"/>
        <Button style="@style/Widget.MaterialComponents.Button.OutlinedButton"
            android:id="@+id/pin_key_2" android:layout_width="0dp" android:layout_height="52dp"
            android:layout_weight="1" android:text="2" android:textSize="18sp" android:layout_marginHorizontal="2dp"/>
        <Button style="@style/Widget.MaterialComponents.Button.OutlinedButton"
            android:id="@+id/pin_key_3" android:layout_width="0dp" android:layout_height="52dp"
            android:layout_weight="1" android:text="3" android:textSize="18sp" android:layout_marginStart="4dp"/>
    </LinearLayout>

    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="horizontal" android:layout_marginBottom="8dp">
        <Button style="@style/Widget.MaterialComponents.Button.OutlinedButton"
            android:id="@+id/pin_key_4" android:layout_width="0dp" android:layout_height="52dp"
            android:layout_weight="1" android:text="4" android:textSize="18sp" android:layout_marginEnd="4dp"/>
        <Button style="@style/Widget.MaterialComponents.Button.OutlinedButton"
            android:id="@+id/pin_key_5" android:layout_width="0dp" android:layout_height="52dp"
            android:layout_weight="1" android:text="5" android:textSize="18sp" android:layout_marginHorizontal="2dp"/>
        <Button style="@style/Widget.MaterialComponents.Button.OutlinedButton"
            android:id="@+id/pin_key_6" android:layout_width="0dp" android:layout_height="52dp"
            android:layout_weight="1" android:text="6" android:textSize="18sp" android:layout_marginStart="4dp"/>
    </LinearLayout>

    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="horizontal" android:layout_marginBottom="8dp">
        <Button style="@style/Widget.MaterialComponents.Button.OutlinedButton"
            android:id="@+id/pin_key_7" android:layout_width="0dp" android:layout_height="52dp"
            android:layout_weight="1" android:text="7" android:textSize="18sp" android:layout_marginEnd="4dp"/>
        <Button style="@style/Widget.MaterialComponents.Button.OutlinedButton"
            android:id="@+id/pin_key_8" android:layout_width="0dp" android:layout_height="52dp"
            android:layout_weight="1" android:text="8" android:textSize="18sp" android:layout_marginHorizontal="2dp"/>
        <Button style="@style/Widget.MaterialComponents.Button.OutlinedButton"
            android:id="@+id/pin_key_9" android:layout_width="0dp" android:layout_height="52dp"
            android:layout_weight="1" android:text="9" android:textSize="18sp" android:layout_marginStart="4dp"/>
    </LinearLayout>

    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="horizontal">
        <Button style="@style/Widget.MaterialComponents.Button.TextButton"
            android:id="@+id/pin_skip" android:layout_width="0dp" android:layout_height="52dp"
            android:layout_weight="1" android:text="Skip" android:textSize="13sp"
            android:textColor="#FF6B35" android:layout_marginEnd="4dp" android:visibility="gone"/>
        <Button style="@style/Widget.MaterialComponents.Button.OutlinedButton"
            android:id="@+id/pin_key_0" android:layout_width="0dp" android:layout_height="52dp"
            android:layout_weight="1" android:text="0" android:textSize="18sp" android:layout_marginHorizontal="2dp"/>
        <Button style="@style/Widget.MaterialComponents.Button.OutlinedButton"
            android:id="@+id/pin_backspace" android:layout_width="0dp" android:layout_height="52dp"
            android:layout_weight="1" android:text="⌫" android:textSize="18sp" android:layout_marginStart="4dp"/>
    </LinearLayout>

</LinearLayout>
```

- [ ] **Step 4: Create activity_parental_dashboard.xml**

`app/src/main/res/layout/activity_parental_dashboard.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="#FFF8F0">

    <!-- Toolbar -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="56dp"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingHorizontal="16dp"
        android:background="#FFF8F0">

        <com.google.android.material.button.MaterialButton
            android:id="@+id/pd_back"
            style="@style/Widget.MaterialComponents.Button.TextButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="← Back"
            android:textColor="#FF6B35"/>

        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="@string/pd_title"
            android:textSize="18sp"
            android:textStyle="bold"
            android:textColor="#222222"
            android:gravity="center"/>

        <Space android:layout_width="80dp" android:layout_height="1dp"/>
    </LinearLayout>

    <View android:layout_width="match_parent" android:layout_height="1dp" android:background="#EEEEEE"/>

    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:padding="16dp"
        android:fillViewport="true">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical">

            <!-- STATS -->
            <TextView
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="@string/pd_stats_header"
                android:textSize="13sp" android:textStyle="bold"
                android:textColor="#FF6B35" android:layout_marginBottom="8dp"/>

            <androidx.cardview.widget.CardView
                android:layout_width="match_parent" android:layout_height="wrap_content"
                app:cardCornerRadius="16dp" app:cardElevation="2dp"
                android:layout_marginBottom="16dp">
                <LinearLayout
                    android:layout_width="match_parent" android:layout_height="wrap_content"
                    android:orientation="vertical" android:padding="16dp">

                    <TextView
                        android:id="@+id/pd_today_minutes"
                        android:layout_width="wrap_content" android:layout_height="wrap_content"
                        android:text="Today: 0 min"
                        android:textSize="16sp" android:textColor="#222222"
                        android:layout_marginBottom="12dp"/>

                    <LinearLayout
                        android:id="@+id/pd_chart"
                        android:layout_width="match_parent" android:layout_height="80dp"
                        android:orientation="horizontal" android:gravity="bottom"
                        android:layout_marginBottom="4dp"/>

                    <LinearLayout
                        android:id="@+id/pd_chart_labels"
                        android:layout_width="match_parent" android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:layout_marginBottom="12dp"/>

                    <TextView
                        android:layout_width="wrap_content" android:layout_height="wrap_content"
                        android:text="Today\'s games:"
                        android:textSize="12sp" android:textColor="#888888"
                        android:layout_marginBottom="4dp"/>

                    <HorizontalScrollView
                        android:layout_width="match_parent" android:layout_height="wrap_content">
                        <LinearLayout
                            android:id="@+id/pd_games_row"
                            android:layout_width="wrap_content" android:layout_height="wrap_content"
                            android:orientation="horizontal"/>
                    </HorizontalScrollView>

                </LinearLayout>
            </androidx.cardview.widget.CardView>

            <!-- CONTENT FILTER -->
            <TextView
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="@string/pd_content_header"
                android:textSize="13sp" android:textStyle="bold"
                android:textColor="#FF6B35" android:layout_marginBottom="8dp"/>

            <androidx.cardview.widget.CardView
                android:layout_width="match_parent" android:layout_height="wrap_content"
                app:cardCornerRadius="16dp" app:cardElevation="2dp"
                android:layout_marginBottom="16dp">
                <LinearLayout
                    android:layout_width="match_parent" android:layout_height="wrap_content"
                    android:orientation="vertical" android:padding="16dp">

                    <TextView
                        android:layout_width="wrap_content" android:layout_height="wrap_content"
                        android:text="Age group"
                        android:textSize="12sp" android:textColor="#888888"
                        android:layout_marginBottom="8dp"/>

                    <LinearLayout
                        android:layout_width="match_parent" android:layout_height="wrap_content"
                        android:orientation="horizontal" android:layout_marginBottom="16dp">
                        <com.google.android.material.button.MaterialButton
                            android:id="@+id/pd_age_24"
                            style="@style/Widget.MaterialComponents.Button.OutlinedButton"
                            android:layout_width="0dp" android:layout_height="48dp"
                            android:layout_weight="1" android:text="👶 2–4"
                            android:textSize="13sp" android:layout_marginEnd="4dp"/>
                        <com.google.android.material.button.MaterialButton
                            android:id="@+id/pd_age_57"
                            style="@style/Widget.MaterialComponents.Button.OutlinedButton"
                            android:layout_width="0dp" android:layout_height="48dp"
                            android:layout_weight="1" android:text="🧒 5–7"
                            android:textSize="13sp" android:layout_marginHorizontal="2dp"/>
                        <com.google.android.material.button.MaterialButton
                            android:id="@+id/pd_age_8plus"
                            style="@style/Widget.MaterialComponents.Button.OutlinedButton"
                            android:layout_width="0dp" android:layout_height="48dp"
                            android:layout_weight="1" android:text="👦 8+"
                            android:textSize="13sp" android:layout_marginStart="4dp"/>
                    </LinearLayout>

                    <View android:layout_width="match_parent" android:layout_height="1dp"
                        android:background="#EEEEEE" android:layout_marginBottom="12dp"/>

                    <TextView
                        android:layout_width="wrap_content" android:layout_height="wrap_content"
                        android:text="Categories"
                        android:textSize="12sp" android:textColor="#888888"
                        android:layout_marginBottom="8dp"/>

                    <LinearLayout
                        android:id="@+id/pd_categories"
                        android:layout_width="match_parent" android:layout_height="wrap_content"
                        android:orientation="vertical"/>

                </LinearLayout>
            </androidx.cardview.widget.CardView>

            <!-- TIME LIMIT -->
            <TextView
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="@string/pd_limit_header"
                android:textSize="13sp" android:textStyle="bold"
                android:textColor="#FF6B35" android:layout_marginBottom="8dp"/>

            <androidx.cardview.widget.CardView
                android:layout_width="match_parent" android:layout_height="wrap_content"
                app:cardCornerRadius="16dp" app:cardElevation="2dp"
                android:layout_marginBottom="16dp">
                <LinearLayout
                    android:layout_width="match_parent" android:layout_height="wrap_content"
                    android:orientation="horizontal" android:gravity="center_vertical"
                    android:padding="16dp">

                    <com.google.android.material.button.MaterialButton
                        android:id="@+id/pd_limit_minus"
                        style="@style/Widget.MaterialComponents.Button.OutlinedButton"
                        android:layout_width="48dp" android:layout_height="48dp"
                        android:text="−" android:textSize="20sp" android:padding="0dp"/>

                    <TextView
                        android:id="@+id/pd_limit_val"
                        android:layout_width="0dp" android:layout_height="wrap_content"
                        android:layout_weight="1" android:text="@string/pd_no_limit"
                        android:textSize="16sp" android:textStyle="bold"
                        android:textColor="#222222" android:gravity="center"/>

                    <com.google.android.material.button.MaterialButton
                        android:id="@+id/pd_limit_plus"
                        style="@style/Widget.MaterialComponents.Button.OutlinedButton"
                        android:layout_width="48dp" android:layout_height="48dp"
                        android:text="+" android:textSize="20sp" android:padding="0dp"/>

                </LinearLayout>
            </androidx.cardview.widget.CardView>

            <!-- PIN SETTINGS -->
            <TextView
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="@string/pd_security_header"
                android:textSize="13sp" android:textStyle="bold"
                android:textColor="#FF6B35" android:layout_marginBottom="8dp"/>

            <androidx.cardview.widget.CardView
                android:layout_width="match_parent" android:layout_height="wrap_content"
                app:cardCornerRadius="16dp" app:cardElevation="2dp"
                android:layout_marginBottom="32dp">
                <LinearLayout
                    android:layout_width="match_parent" android:layout_height="wrap_content"
                    android:padding="16dp">
                    <com.google.android.material.button.MaterialButton
                        android:id="@+id/pd_change_pin"
                        android:layout_width="match_parent" android:layout_height="48dp"
                        android:text="@string/pd_set_pin"
                        android:backgroundTint="#FF6B35"/>
                </LinearLayout>
            </androidx.cardview.widget.CardView>

        </LinearLayout>
    </ScrollView>

</LinearLayout>
```

- [ ] **Step 5: Create view_lock_overlay.xml**

`app/src/main/res/layout/view_lock_overlay.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#CC000000">

    <LinearLayout
        android:layout_width="300dp"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:orientation="vertical"
        android:gravity="center"
        android:padding="24dp"
        android:background="#FFF8F0"
        android:elevation="8dp">

        <TextView
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="⏰" android:textSize="56sp"
            android:layout_marginBottom="12dp"/>

        <TextView
            android:id="@+id/lock_title"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="Vaqt tugadi!"
            android:textSize="20sp" android:textStyle="bold"
            android:textColor="#222222" android:textAlignment="center"
            android:layout_marginBottom="8dp"/>

        <TextView
            android:id="@+id/lock_subtitle"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="Davom etish uchun PIN kiriting"
            android:textSize="13sp" android:textColor="#888888"
            android:textAlignment="center" android:layout_marginBottom="24dp"/>

        <!-- PIN section (shown when PIN is set) -->
        <LinearLayout
            android:id="@+id/lock_pin_section"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:orientation="vertical">

            <LinearLayout
                android:id="@+id/lock_dots"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:layout_gravity="center_horizontal"
                android:orientation="horizontal" android:layout_marginBottom="20dp">
                <View android:id="@+id/lock_dot1" android:layout_width="14dp" android:layout_height="14dp"
                    android:background="@drawable/pin_dot_empty" android:layout_marginHorizontal="6dp"/>
                <View android:id="@+id/lock_dot2" android:layout_width="14dp" android:layout_height="14dp"
                    android:background="@drawable/pin_dot_empty" android:layout_marginHorizontal="6dp"/>
                <View android:id="@+id/lock_dot3" android:layout_width="14dp" android:layout_height="14dp"
                    android:background="@drawable/pin_dot_empty" android:layout_marginHorizontal="6dp"/>
                <View android:id="@+id/lock_dot4" android:layout_width="14dp" android:layout_height="14dp"
                    android:background="@drawable/pin_dot_empty" android:layout_marginHorizontal="6dp"/>
            </LinearLayout>

            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="horizontal" android:layout_marginBottom="6dp">
                <Button style="@style/Widget.MaterialComponents.Button.OutlinedButton"
                    android:id="@+id/lock_key_1" android:layout_width="0dp" android:layout_height="48dp"
                    android:layout_weight="1" android:text="1" android:textSize="16sp" android:layout_marginEnd="3dp"/>
                <Button style="@style/Widget.MaterialComponents.Button.OutlinedButton"
                    android:id="@+id/lock_key_2" android:layout_width="0dp" android:layout_height="48dp"
                    android:layout_weight="1" android:text="2" android:textSize="16sp" android:layout_marginHorizontal="2dp"/>
                <Button style="@style/Widget.MaterialComponents.Button.OutlinedButton"
                    android:id="@+id/lock_key_3" android:layout_width="0dp" android:layout_height="48dp"
                    android:layout_weight="1" android:text="3" android:textSize="16sp" android:layout_marginStart="3dp"/>
            </LinearLayout>
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="horizontal" android:layout_marginBottom="6dp">
                <Button style="@style/Widget.MaterialComponents.Button.OutlinedButton"
                    android:id="@+id/lock_key_4" android:layout_width="0dp" android:layout_height="48dp"
                    android:layout_weight="1" android:text="4" android:textSize="16sp" android:layout_marginEnd="3dp"/>
                <Button style="@style/Widget.MaterialComponents.Button.OutlinedButton"
                    android:id="@+id/lock_key_5" android:layout_width="0dp" android:layout_height="48dp"
                    android:layout_weight="1" android:text="5" android:textSize="16sp" android:layout_marginHorizontal="2dp"/>
                <Button style="@style/Widget.MaterialComponents.Button.OutlinedButton"
                    android:id="@+id/lock_key_6" android:layout_width="0dp" android:layout_height="48dp"
                    android:layout_weight="1" android:text="6" android:textSize="16sp" android:layout_marginStart="3dp"/>
            </LinearLayout>
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="horizontal" android:layout_marginBottom="6dp">
                <Button style="@style/Widget.MaterialComponents.Button.OutlinedButton"
                    android:id="@+id/lock_key_7" android:layout_width="0dp" android:layout_height="48dp"
                    android:layout_weight="1" android:text="7" android:textSize="16sp" android:layout_marginEnd="3dp"/>
                <Button style="@style/Widget.MaterialComponents.Button.OutlinedButton"
                    android:id="@+id/lock_key_8" android:layout_width="0dp" android:layout_height="48dp"
                    android:layout_weight="1" android:text="8" android:textSize="16sp" android:layout_marginHorizontal="2dp"/>
                <Button style="@style/Widget.MaterialComponents.Button.OutlinedButton"
                    android:id="@+id/lock_key_9" android:layout_width="0dp" android:layout_height="48dp"
                    android:layout_weight="1" android:text="9" android:textSize="16sp" android:layout_marginStart="3dp"/>
            </LinearLayout>
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="horizontal">
                <View android:layout_width="0dp" android:layout_height="48dp" android:layout_weight="1" android:layout_marginEnd="3dp"/>
                <Button style="@style/Widget.MaterialComponents.Button.OutlinedButton"
                    android:id="@+id/lock_key_0" android:layout_width="0dp" android:layout_height="48dp"
                    android:layout_weight="1" android:text="0" android:textSize="16sp" android:layout_marginHorizontal="2dp"/>
                <Button style="@style/Widget.MaterialComponents.Button.OutlinedButton"
                    android:id="@+id/lock_key_bsp" android:layout_width="0dp" android:layout_height="48dp"
                    android:layout_weight="1" android:text="⌫" android:textSize="16sp" android:layout_marginStart="3dp"/>
            </LinearLayout>

        </LinearLayout>

        <!-- No-PIN section (shown when PIN was skipped) -->
        <com.google.android.material.button.MaterialButton
            android:id="@+id/lock_continue"
            android:layout_width="match_parent" android:layout_height="48dp"
            android:text="Continue"
            android:backgroundTint="#FF6B35"
            android:visibility="gone"/>

    </LinearLayout>
</FrameLayout>
```

- [ ] **Step 6: Build to verify layouts compile**

```
.\gradlew compileDebugJavaSources 2>&1 | Select-String "error:|BUILD"
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```
git add app/src/main/res/drawable/pin_dot_empty.xml app/src/main/res/drawable/pin_dot_filled.xml app/src/main/res/layout/dialog_pin.xml app/src/main/res/layout/activity_parental_dashboard.xml app/src/main/res/layout/view_lock_overlay.xml
git commit -m "feat(parental): drawables and layout files for dashboard, PIN dialog, lock overlay"
```

---

## Task 5: ParentalDashboardActivity

**Files:**
- Create: `app/src/main/java/uz/kidzone/app/ParentalDashboardActivity.java`

- [ ] **Step 1: Implement ParentalDashboardActivity**

Create `app/src/main/java/uz/kidzone/app/ParentalDashboardActivity.java`:
```java
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_parental_dashboard);

        prefs = getSharedPreferences(OnboardingActivity.PREFS, MODE_PRIVATE);
        stats = new ParentalStatsManager(this);

        bindViews();
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
    }

    // ── Stats ────────────────────────────────────────────────────────────────

    private void loadStats() {
        tvToday.setText("Today: " + stats.getTodayMinutes() + " min");
        drawChart(stats.getWeeklyMinutes());
        populateGames(stats.getTodayGames());
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
                prefs.edit().putString("kz_pin", newPin).apply();
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
```

- [ ] **Step 2: Build to verify no compile errors**

```
.\gradlew compileDebugJavaSources 2>&1 | Select-String "error:|BUILD"
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```
git add app/src/main/java/uz/kidzone/app/ParentalDashboardActivity.java
git commit -m "feat(parental): ParentalDashboardActivity — stats, age filter, categories, time limit, PIN"
```

---

## Task 6: Wire MainActivity

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/MainActivity.java`

- [ ] **Step 1: Add fields at the top of the class (after existing fields)**

After `private int gameLaunchCount = 0;`, add:
```java
private ParentalStatsManager statsManager;
private android.os.Handler timeLockHandler;
private View lockOverlay;
static java.lang.ref.WeakReference<MainActivity> instance;

private final Runnable timeLockRunnable = new Runnable() {
    @Override public void run() {
        if (statsManager != null && statsManager.isTimeLimitReached()) {
            showLockOverlay();
        } else {
            timeLockHandler.postDelayed(this, 60_000);
        }
    }
};
```

- [ ] **Step 2: Initialize in onCreate, after the kzPrefs/onboarding block**

After the `if (!kzPrefs.getBoolean(...)) { ... return; }` block and before `initializeUI()`, add:
```java
instance = new java.lang.ref.WeakReference<>(this);
statsManager = new ParentalStatsManager(this);
timeLockHandler = new android.os.Handler(android.os.Looper.getMainLooper());
```

- [ ] **Step 3: Add lifecycle hooks**

In `onResume()`, after the existing three lines, add:
```java
if (statsManager != null) {
    statsManager.onSessionStart();
    timeLockHandler.postDelayed(timeLockRunnable, 60_000);
}
```

In `onPause()`, before `super.onPause()`, add:
```java
if (statsManager != null) {
    statsManager.onSessionEnd();
    timeLockHandler.removeCallbacks(timeLockRunnable);
}
```

In `onDestroy()`, before `super.onDestroy()`, add:
```java
instance = null;
```

- [ ] **Step 4: Add injectJs() package-private method**

Add after `onDestroy()`:
```java
void injectJs(String script) {
    if (webViewManager != null) webViewManager.evaluateJavascript(script);
}
```

- [ ] **Step 5: Add showPinDialog() and openDashboard() private methods**

```java
private void showPinDialog() {
    String savedPin = kzPrefs.getString("kz_pin", null);
    if (savedPin == null) {
        PinDialogHelper.showCreate(this, pin -> {
            kzPrefs.edit().putString("kz_pin", pin).apply();
            openDashboard();
        });
    } else if (savedPin.isEmpty()) {
        openDashboard();
    } else {
        PinDialogHelper.showEnter(this, savedPin, this::openDashboard);
    }
}

private void openDashboard() {
    startActivity(new android.content.Intent(this, ParentalDashboardActivity.class));
}
```

- [ ] **Step 6: Add showLockOverlay() and hideLockOverlay() private methods**

```java
private void showLockOverlay() {
    if (lockOverlay != null) return;
    timeLockHandler.removeCallbacks(timeLockRunnable);

    lockOverlay = getLayoutInflater().inflate(R.layout.view_lock_overlay, null);

    String lang = kzPrefs.getString(OnboardingActivity.KEY_LANG, "uz");
    TextView tvTitle = lockOverlay.findViewById(R.id.lock_title);
    TextView tvSub   = lockOverlay.findViewById(R.id.lock_subtitle);
    if ("ru".equals(lang)) {
        tvTitle.setText("Время вышло! ⏰");
        tvSub.setText("Введите PIN для продолжения");
    } else if ("en".equals(lang)) {
        tvTitle.setText("Time's up! ⏰");
        tvSub.setText("Enter parent PIN to continue");
    }

    String savedPin = kzPrefs.getString("kz_pin", "");
    View pinSection = lockOverlay.findViewById(R.id.lock_pin_section);
    View continueBtn = lockOverlay.findViewById(R.id.lock_continue);

    if (savedPin == null || savedPin.isEmpty()) {
        pinSection.setVisibility(View.GONE);
        continueBtn.setVisibility(View.VISIBLE);
        continueBtn.setOnClickListener(v -> hideLockOverlay());
    } else {
        pinSection.setVisibility(View.VISIBLE);
        continueBtn.setVisibility(View.GONE);
        wireLockPad(lockOverlay, savedPin);
    }

    android.widget.FrameLayout decor =
        (android.widget.FrameLayout) getWindow().getDecorView();
    decor.addView(lockOverlay, new android.widget.FrameLayout.LayoutParams(
        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
        android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
}

private void hideLockOverlay() {
    if (lockOverlay == null) return;
    if (statsManager != null) statsManager.setTimeLimitMinutes(0);
    ((android.widget.FrameLayout) getWindow().getDecorView()).removeView(lockOverlay);
    lockOverlay = null;
    timeLockHandler.postDelayed(timeLockRunnable, 60_000);
}

private void wireLockPad(View overlay, String expectedPin) {
    View[] dots = {
        overlay.findViewById(R.id.lock_dot1), overlay.findViewById(R.id.lock_dot2),
        overlay.findViewById(R.id.lock_dot3), overlay.findViewById(R.id.lock_dot4)
    };
    View dotsContainer = overlay.findViewById(R.id.lock_dots);
    StringBuilder entered = new StringBuilder();

    int[] keyIds = {R.id.lock_key_1, R.id.lock_key_2, R.id.lock_key_3,
                    R.id.lock_key_4, R.id.lock_key_5, R.id.lock_key_6,
                    R.id.lock_key_7, R.id.lock_key_8, R.id.lock_key_9,
                    R.id.lock_key_0};
    String[] digits = {"1","2","3","4","5","6","7","8","9","0"};

    for (int i = 0; i < keyIds.length; i++) {
        final String d = digits[i];
        overlay.findViewById(keyIds[i]).setOnClickListener(v -> {
            if (entered.length() >= 4) return;
            entered.append(d);
            updateLockDots(dots, entered.length());
            if (entered.length() == 4) {
                if (entered.toString().equals(expectedPin)) {
                    hideLockOverlay();
                } else {
                    android.animation.ObjectAnimator anim =
                        android.animation.ObjectAnimator.ofFloat(dotsContainer, "translationX",
                            0f, -10f, 10f, -10f, 10f, -5f, 5f, 0f);
                    anim.setDuration(400);
                    anim.start();
                    entered.setLength(0);
                    updateLockDots(dots, 0);
                }
            }
        });
    }

    overlay.findViewById(R.id.lock_key_bsp).setOnClickListener(v -> {
        if (entered.length() > 0) {
            entered.deleteCharAt(entered.length() - 1);
            updateLockDots(dots, entered.length());
        }
    });
}

private static void updateLockDots(View[] dots, int count) {
    for (int i = 0; i < dots.length; i++) {
        dots[i].setBackgroundResource(
            i < count ? R.drawable.pin_dot_filled : R.drawable.pin_dot_empty);
    }
}
```

- [ ] **Step 7: Add openParentalDashboard() and gameLaunched() to AdMobBridge (inner class)**

Inside `private class AdMobBridge`, add:
```java
@JavascriptInterface
public void openParentalDashboard() {
    runOnUiThread(() -> showPinDialog());
}

@JavascriptInterface
public void gameLaunched(String gameId) {
    if (statsManager != null) statsManager.onGameLaunched(gameId);
}
```

- [ ] **Step 8: Build to verify no compile errors**

```
.\gradlew compileDebugJavaSources 2>&1 | Select-String "error:|BUILD"
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 9: Commit**

```
git add app/src/main/java/uz/kidzone/app/MainActivity.java
git commit -m "feat(parental): wire MainActivity — stats lifecycle, PIN dialog, lock overlay, AdMobBridge"
```

---

## Task 7: index.html Long-Press + Build + Verify

**Files:**
- Modify: `app/src/main/assets/www/index.html`

- [ ] **Step 1: Add long-press detection to #music-pill**

Find `id="music-pill"` in `index.html`. It will be on a `<button>` element. Add inside the `<script>` section (after DOMContentLoaded fires, near where other button event handlers are set up), or inline in a `<script>` at the bottom of `<body>`:

```javascript
(function() {
  var _pt;
  var pill = document.getElementById('music-pill');
  if (!pill) return;
  pill.addEventListener('touchstart', function() {
    _pt = setTimeout(function() {
      if (window.AndroidAdMob) AndroidAdMob.openParentalDashboard();
    }, 3000);
  }, { passive: true });
  pill.addEventListener('touchend', function() { clearTimeout(_pt); }, { passive: true });
  pill.addEventListener('touchcancel', function() { clearTimeout(_pt); }, { passive: true });
})();
```

Place this script block just before `</body>`.

- [ ] **Step 2: Commit**

```
git add app/src/main/assets/www/index.html
git commit -m "feat(parental): long-press music button 3s opens parental dashboard"
```

- [ ] **Step 3: Run all unit tests**

```
.\gradlew test 2>&1 | Select-String "tests|PASS|FAIL|BUILD"
```
Expected: `BUILD SUCCESSFUL`, all existing tests still pass.

- [ ] **Step 4: Build and install APK**

```
.\gradlew assembleDebug
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install -r app/build/outputs/apk/debug/app-debug.apk
```
Expected: `Success`.

- [ ] **Step 5: Smoke test checklist**

Test on device:
1. Long-press music button < 3 s → nothing opens
2. Long-press ≥ 3 s → PIN dialog appears with "Create a parent PIN" title
3. Enter 4 digits, confirm same → "Set PIN" saved, dashboard opens
4. Dashboard shows today's minutes and 7-day chart
5. Change age to 5–7 → refreshes WebView localStorage
6. Toggle "dance" OFF → `kz-cat-off` contains "dance" in localStorage
7. Set time limit to 15 → "15 min" shown
8. Press Back → returns to main app
9. Long-press again → "Enter parent PIN" shown, correct PIN opens dashboard
10. Wrong PIN → dots shake, reset
11. From dashboard → "Change PIN" → sets new PIN

- [ ] **Step 6: Final commit if any fixes were needed**

```
git add -p
git commit -m "fix(parental): smoke test fixes"
```
