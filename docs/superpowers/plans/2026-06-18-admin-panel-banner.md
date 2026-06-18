# Admin Panel + Banner System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** CRM Block 4 — in-app admin panel (user list + ban/unban) va banner tizimi (Firestore overlay + FCM push + deep link URL ochish).

**Architecture:** Admin UID hardcode (`AdminConfig.java`) qilinadi; `ParentalDashboardActivity` admin UID bilan login qilinganida ikki karta ko'rsatadi: foydalanuvchilar ro'yxati va banner yaratish. `BannerChecker` `config/banner` hujjatini o'qiydi, `MainActivity` launch'da overlay ko'rsatadi. FCM push `data["url"]` ni `SharedPrefs`'ga yozadi, `onResume` uni consume qiladi.

**Tech Stack:** Java, Firebase Firestore, Firebase Auth (ID token), OkHttp 4.12, AndroidX Browser (CustomTabs), JUnit 4, injectable provider pattern (BanChecker'ga o'xshash).

## Global Constraints

- Backend base URL: `https://kidzone-backend-s7to.onrender.com`
- SharedPrefs key for FCM/UID prefs: `"kz_prefs"`
- OkHttp client: `KidZoneApplication.getHttpClient()`
- ID token: `FirebaseAuth.getInstance().getCurrentUser().getIdToken(false)`
- Test pattern: `AtomicReference` + lambda provider (Mockito ishlatilmaydi — BanCheckerTest'ga o'xshash)
- Barcha UI programmatic (XML minimal — faqat container placeholder'lar)
- min SDK 26, Java 17

---

## Task 1: AdminConfig + FirestoreSync admin methods (TDD)

**Files:**
- Create: `app/src/main/java/uz/kidzone/app/AdminConfig.java`
- Modify: `app/src/main/java/uz/kidzone/app/FirestoreSync.java`
- Create: `app/src/test/java/uz/kidzone/app/FirestoreSyncAdminTest.java`

**Interfaces:**
- Produces: `AdminConfig.ADMIN_UID` (String constant), `FirestoreSync.UserInfo` (data class), `FirestoreSync.getAllUsers(UserListCallback)`, `FirestoreSync.setUserStatus(String uid, String status, Runnable onDone)`, `FirestoreSync.setBanner(String title, String body, String url, String adminUid)`, `FirestoreSync.clearBanner()`

- [ ] **Step 1: FirestoreSyncAdminTest.java yaratish**

```java
package uz.kidzone.app;

import org.junit.Test;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

public class FirestoreSyncAdminTest {

    @Test
    public void getAllUsers_unavailable_returnsEmpty() {
        FirestoreSync sync = new FirestoreSync(null);
        AtomicReference<List<FirestoreSync.UserInfo>> result = new AtomicReference<>();
        sync.getAllUsers(result::set);
        assertNotNull(result.get());
        assertEquals(0, result.get().size());
    }

    @Test
    public void setUserStatus_unavailable_doesNothing() {
        FirestoreSync sync = new FirestoreSync(null);
        sync.setUserStatus("uid123", "banned", null);
        // must not throw
    }

    @Test
    public void setUserStatus_nullUid_doesNothing() {
        FirestoreSync sync = new FirestoreSync(null);
        sync.setUserStatus(null, "banned", null);
        // must not throw
    }

    @Test
    public void setBanner_unavailable_doesNothing() {
        FirestoreSync sync = new FirestoreSync(null);
        sync.setBanner("title", "body", "https://example.com", "uid");
        // must not throw
    }

    @Test
    public void clearBanner_unavailable_doesNothing() {
        FirestoreSync sync = new FirestoreSync(null);
        sync.clearBanner();
        // must not throw
    }

    @Test
    public void userInfo_fields_correct() {
        FirestoreSync.UserInfo info = new FirestoreSync.UserInfo("uid1", "test@mail.com", "active");
        assertEquals("uid1", info.uid);
        assertEquals("test@mail.com", info.email);
        assertEquals("active", info.status);
    }
}
```

- [ ] **Step 2: Testlarni ishlatib fail ekanini tekshirish**

```
./gradlew testDebugUnitTest --tests "uz.kidzone.app.FirestoreSyncAdminTest"
```
Expected: FAIL — `getAllUsers` metodi mavjud emas.

- [ ] **Step 3: AdminConfig.java yaratish**

```java
package uz.kidzone.app;

final class AdminConfig {
    private AdminConfig() {}
    // Firebase Console → Authentication → Users'dan o'z UID'ingizni oling
    static final String ADMIN_UID = "REPLACE_WITH_YOUR_FIREBASE_UID";
}
```

- [ ] **Step 4: FirestoreSync.java'ga UserInfo va yangi metodlar qo'shish**

`FirestoreSync.java`'ga mavjud `updateFcmToken` metodidan keyin quyidagilarni qo'shing:

```java
public interface UserListCallback {
    void onResult(java.util.List<UserInfo> users);
}

public static class UserInfo {
    public final String uid;
    public final String email;
    public final String status;
    public UserInfo(String uid, String email, String status) {
        this.uid = uid;
        this.email = email;
        this.status = status;
    }
}

public void getAllUsers(UserListCallback callback) {
    if (!isAvailable()) {
        callback.onResult(java.util.Collections.emptyList());
        return;
    }
    db.collection("users").get()
        .addOnSuccessListener(snap -> {
            java.util.List<UserInfo> users = new java.util.ArrayList<>();
            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snap) {
                String email = doc.getString("email");
                String status = doc.getString("status");
                users.add(new UserInfo(
                    doc.getId(),
                    email != null ? email : "",
                    status != null ? status : "active"
                ));
            }
            callback.onResult(users);
        })
        .addOnFailureListener(e -> {
            Log.w(TAG, "getAllUsers failed: " + e);
            callback.onResult(java.util.Collections.emptyList());
        });
}

public void setUserStatus(String uid, String status, Runnable onDone) {
    if (!isAvailable() || uid == null) return;
    java.util.Map<String, Object> data = new java.util.HashMap<>();
    data.put("status", status);
    db.collection("users").document(uid).update(data)
        .addOnSuccessListener(v -> { if (onDone != null) onDone.run(); })
        .addOnFailureListener(e -> Log.w(TAG, "setUserStatus failed: " + e));
}

public void setBanner(String title, String body, String url, String adminUid) {
    if (!isAvailable()) return;
    java.util.Map<String, Object> data = new java.util.HashMap<>();
    data.put("active", true);
    data.put("title", title != null ? title : "");
    data.put("body", body != null ? body : "");
    data.put("url", url != null ? url : "");
    data.put("createdAt", FieldValue.serverTimestamp());
    data.put("createdBy", adminUid != null ? adminUid : "");
    db.collection("config").document("banner").set(data)
        .addOnFailureListener(e -> Log.w(TAG, "setBanner failed: " + e));
}

public void clearBanner() {
    if (!isAvailable()) return;
    java.util.Map<String, Object> data = new java.util.HashMap<>();
    data.put("active", false);
    db.collection("config").document("banner").update(data)
        .addOnFailureListener(e -> Log.w(TAG, "clearBanner failed: " + e));
}
```

- [ ] **Step 5: Testlarni ishlatib pass ekanini tekshirish**

```
./gradlew testDebugUnitTest --tests "uz.kidzone.app.FirestoreSyncAdminTest"
```
Expected: 6 tests PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/AdminConfig.java \
        app/src/main/java/uz/kidzone/app/FirestoreSync.java \
        app/src/test/java/uz/kidzone/app/FirestoreSyncAdminTest.java
git commit -m "feat: AdminConfig + FirestoreSync admin/banner methods (TDD)"
```

---

## Task 2: BannerChecker (TDD)

**Files:**
- Create: `app/src/test/java/uz/kidzone/app/BannerCheckerTest.java`
- Create: `app/src/main/java/uz/kidzone/app/BannerChecker.java`

**Interfaces:**
- Consumes: `FirestoreSync.isAvailable()`, `FirestoreSync.getDb()`
- Produces: `BannerChecker.BannerData` (title, body, url), `BannerChecker.Callback`, `BannerChecker.checkAsync(FirestoreSync, Callback)`

- [ ] **Step 1: BannerCheckerTest.java yaratish**

```java
package uz.kidzone.app;

import org.junit.Test;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

public class BannerCheckerTest {

    @Test
    public void unavailableSync_returnsNull() {
        AtomicReference<BannerChecker.BannerData> result = new AtomicReference<>();
        FirestoreSync sync = new FirestoreSync(null);
        BannerChecker.checkAsync(sync, result::set);
        assertNull(result.get());
    }

    @Test
    public void activeBanner_returnsData() {
        AtomicReference<BannerChecker.BannerData> result = new AtomicReference<>();
        BannerChecker.checkAsync(
            cb -> cb.onDoc(true, true, "Sarlavha", "Matn", "https://example.com"),
            result::set
        );
        assertNotNull(result.get());
        assertEquals("Sarlavha", result.get().title);
        assertEquals("Matn", result.get().body);
        assertEquals("https://example.com", result.get().url);
    }

    @Test
    public void inactiveBanner_returnsNull() {
        AtomicReference<BannerChecker.BannerData> result = new AtomicReference<>();
        BannerChecker.checkAsync(
            cb -> cb.onDoc(true, false, "title", "body", "https://example.com"),
            result::set
        );
        assertNull(result.get());
    }

    @Test
    public void noDocument_returnsNull() {
        AtomicReference<BannerChecker.BannerData> result = new AtomicReference<>();
        BannerChecker.checkAsync(
            cb -> cb.onDoc(false, false, null, null, null),
            result::set
        );
        assertNull(result.get());
    }

    @Test
    public void missingUrl_returnsNull() {
        AtomicReference<BannerChecker.BannerData> result = new AtomicReference<>();
        BannerChecker.checkAsync(
            cb -> cb.onDoc(true, true, "title", "body", null),
            result::set
        );
        assertNull(result.get());
    }

    @Test
    public void error_returnsNull() {
        AtomicReference<BannerChecker.BannerData> result = new AtomicReference<>();
        BannerChecker.checkAsync(BannerChecker.DocCallback::onError, result::set);
        assertNull(result.get());
    }
}
```

- [ ] **Step 2: Testlarni ishlatib fail ekanini tekshirish**

```
./gradlew testDebugUnitTest --tests "uz.kidzone.app.BannerCheckerTest"
```
Expected: FAIL — `BannerChecker` mavjud emas.

- [ ] **Step 3: BannerChecker.java yaratish**

```java
package uz.kidzone.app;

import android.util.Log;

/** Reads config/banner from Firestore; returns BannerData if active, null otherwise. */
public final class BannerChecker {

    private static final String TAG = "BannerChecker";

    public static class BannerData {
        public final String title;
        public final String body;
        public final String url;
        public BannerData(String title, String body, String url) {
            this.title = title;
            this.body = body;
            this.url = url;
        }
    }

    public interface Callback {
        void onResult(BannerData banner);
    }

    interface DocCallback {
        void onDoc(boolean exists, boolean active, String title, String body, String url);
        default void onError() {}
    }

    /** Production entry point. */
    public static void checkAsync(FirestoreSync sync, Callback callback) {
        if (!sync.isAvailable()) { callback.onResult(null); return; }
        checkAsync(docCb ->
            sync.getDb().collection("config").document("banner").get()
                .addOnSuccessListener(snap ->
                    docCb.onDoc(
                        snap.exists(),
                        Boolean.TRUE.equals(snap.getBoolean("active")),
                        snap.getString("title"),
                        snap.getString("body"),
                        snap.getString("url")
                    )
                )
                .addOnFailureListener(e -> {
                    Log.w(TAG, "banner check failed: " + e.getMessage());
                    docCb.onError();
                }),
            callback
        );
    }

    /** Package-private — injectable for unit testing. */
    static void checkAsync(DocCallback provider, Callback callback) {
        provider.onDoc(false, false, null, null, null); // will be overridden by lambda
    }
}
```

Yuqoridagi `checkAsync(DocCallback, Callback)` noto'g'ri — to'g'ri versiyasi:

```java
/** Package-private — injectable for unit testing. */
static void checkAsync(java.util.function.Consumer<DocCallback> provider, Callback callback) {
    provider.accept(new DocCallback() {
        @Override
        public void onDoc(boolean exists, boolean active, String title, String body, String url) {
            if (!exists || !active || url == null || url.isEmpty()) {
                callback.onResult(null);
                return;
            }
            callback.onResult(new BannerData(
                title != null ? title : "",
                body != null ? body : "",
                url
            ));
        }
        @Override
        public void onError() {
            callback.onResult(null);
        }
    });
}
```

Va test'dagi `BannerChecker.checkAsync(cb -> cb.onDoc(...), result::set)` shakli ushbu overload'ni ishlatadi. `DocCallback::onError` lambda esa `BannerChecker.checkAsync(provider, callback)` ga mos kelishi uchun test'da o'zgartirish kerak:

`BannerCheckerTest`'dagi oxirgi test:
```java
@Test
public void error_returnsNull() {
    AtomicReference<BannerChecker.BannerData> result = new AtomicReference<>();
    BannerChecker.checkAsync(cb -> cb.onError(), result::set);
    assertNull(result.get());
}
```

Va production `checkAsync(FirestoreSync, Callback)`:
```java
public static void checkAsync(FirestoreSync sync, Callback callback) {
    if (!sync.isAvailable()) { callback.onResult(null); return; }
    checkAsync(docCb ->
        sync.getDb().collection("config").document("banner").get()
            .addOnSuccessListener(snap ->
                docCb.onDoc(
                    snap.exists(),
                    Boolean.TRUE.equals(snap.getBoolean("active")),
                    snap.getString("title"),
                    snap.getString("body"),
                    snap.getString("url")
                )
            )
            .addOnFailureListener(e -> {
                Log.w(TAG, "banner check failed: " + e.getMessage());
                docCb.onError();
            }),
        callback
    );
}
```

**To'liq BannerChecker.java:**

```java
package uz.kidzone.app;

import android.util.Log;
import java.util.function.Consumer;

public final class BannerChecker {

    private static final String TAG = "BannerChecker";

    public static class BannerData {
        public final String title;
        public final String body;
        public final String url;
        public BannerData(String title, String body, String url) {
            this.title = title; this.body = body; this.url = url;
        }
    }

    public interface Callback {
        void onResult(BannerData banner);
    }

    interface DocCallback {
        void onDoc(boolean exists, boolean active, String title, String body, String url);
        default void onError() {}
    }

    public static void checkAsync(FirestoreSync sync, Callback callback) {
        if (!sync.isAvailable()) { callback.onResult(null); return; }
        checkAsync(docCb ->
            sync.getDb().collection("config").document("banner").get()
                .addOnSuccessListener(snap ->
                    docCb.onDoc(
                        snap.exists(),
                        Boolean.TRUE.equals(snap.getBoolean("active")),
                        snap.getString("title"),
                        snap.getString("body"),
                        snap.getString("url")
                    )
                )
                .addOnFailureListener(e -> {
                    Log.w(TAG, "banner check failed: " + e);
                    docCb.onError();
                }),
            callback
        );
    }

    static void checkAsync(Consumer<DocCallback> provider, Callback callback) {
        provider.accept(new DocCallback() {
            @Override
            public void onDoc(boolean exists, boolean active,
                              String title, String body, String url) {
                if (!exists || !active || url == null || url.isEmpty()) {
                    callback.onResult(null);
                } else {
                    callback.onResult(new BannerData(
                        title != null ? title : "",
                        body != null ? body : "",
                        url
                    ));
                }
            }
            @Override public void onError() { callback.onResult(null); }
        });
    }
}
```

- [ ] **Step 4: Testlarni ishlatib pass ekanini tekshirish**

```
./gradlew testDebugUnitTest --tests "uz.kidzone.app.BannerCheckerTest"
```
Expected: 6 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/BannerChecker.java \
        app/src/test/java/uz/kidzone/app/BannerCheckerTest.java
git commit -m "feat: BannerChecker reads config/banner from Firestore (TDD)"
```

---

## Task 3: BackendClient

**Files:**
- Create: `app/src/main/java/uz/kidzone/app/BackendClient.java`

**Interfaces:**
- Consumes: `KidZoneApplication.getHttpClient()`, Firebase ID token
- Produces: `BackendClient.sendTopicPush(String title, String body, String url, Runnable onDone, Runnable onError)`

- [ ] **Step 1: BackendClient.java yaratish**

```java
package uz.kidzone.app;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public final class BackendClient {

    private static final String TAG = "BackendClient";
    private static final String BASE_URL = "https://kidzone-backend-s7to.onrender.com";

    private BackendClient() {}

    public static void sendTopicPush(String title, String body, String url,
                                     Runnable onDone, Runnable onError) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.w(TAG, "sendTopicPush: no Firebase user");
            if (onError != null) onError.run();
            return;
        }
        FirebaseAuth.getInstance().getCurrentUser()
            .getIdToken(false)
            .addOnSuccessListener(result -> doPost(result.getToken(), title, body, url, onDone, onError))
            .addOnFailureListener(e -> {
                Log.w(TAG, "getIdToken failed: " + e);
                if (onError != null) onError.run();
            });
    }

    private static void doPost(String idToken, String title, String body, String url,
                                Runnable onDone, Runnable onError) {
        try {
            org.json.JSONObject data = new org.json.JSONObject();
            data.put("url", url != null ? url : "");
            org.json.JSONObject payload = new org.json.JSONObject();
            payload.put("title", title != null ? title : "");
            payload.put("body", body != null ? body : "");
            payload.put("data", data);

            RequestBody reqBody = RequestBody.create(
                payload.toString(), MediaType.parse("application/json"));
            Request request = new Request.Builder()
                .url(BASE_URL + "/push/send-all")
                .addHeader("Authorization", "Bearer " + idToken)
                .post(reqBody)
                .build();

            KidZoneApplication.getHttpClient().newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    Log.w(TAG, "sendTopicPush failed: " + e);
                    if (onError != null) onError.run();
                }
                @Override public void onResponse(Call call, Response response) {
                    Log.d(TAG, "sendTopicPush HTTP " + response.code());
                    response.close();
                    if (response.isSuccessful()) { if (onDone != null) onDone.run(); }
                    else { if (onError != null) onError.run(); }
                }
            });
        } catch (org.json.JSONException e) {
            Log.w(TAG, "JSON error: " + e);
            if (onError != null) onError.run();
        }
    }
}
```

- [ ] **Step 2: Build qilib compile bo'lishini tekshirish**

```
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/BackendClient.java
git commit -m "feat: BackendClient.sendTopicPush — POST /push/send-all via OkHttp"
```

---

## Task 4: Admin Panel — User List UI

**Files:**
- Modify: `app/src/main/res/layout/activity_parental_dashboard.xml`
- Modify: `app/src/main/java/uz/kidzone/app/ParentalDashboardActivity.java`

**Interfaces:**
- Consumes: `AdminConfig.ADMIN_UID`, `FirebaseManager.getInstance().getUid()`, `FirestoreSync.getInstance().getAllUsers()`, `FirestoreSync.getInstance().setUserStatus()`

**Firestore security rules (qo'lda Console'da yangilanadi):**
Firebase Console → Firestore → Rules bo'limiga kiring va mavjud `users/{uid}` qoidasiga qo'shing:
```
match /users/{uid} {
  allow read, write: if request.auth.uid == uid
                     || request.auth.uid == "YOUR_ADMIN_UID";
}
match /config/{doc} {
  allow read:  if request.auth != null;
  allow write: if request.auth.uid == "YOUR_ADMIN_UID";
}
```
`YOUR_ADMIN_UID` o'rniga `AdminConfig.ADMIN_UID`'dagi qiymatni yozing.

- [ ] **Step 1: activity_parental_dashboard.xml'ga admin container qo'shish**

`activity_parental_dashboard.xml`'dagi ScrollView ichidagi ichki LinearLayout'ning **oxiriga** (boshqa kartalardan keyin) qo'shing:

```xml
<!-- Admin Panel — faqat ADMIN_UID uchun ko'rinadi -->
<LinearLayout
    android:id="@+id/pd_admin_container"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:visibility="gone"
    android:layout_marginTop="24dp"/>
```

- [ ] **Step 2: ParentalDashboardActivity.java'ga admin panel qo'shish**

`bindViews()` ichiga qo'shing:
```java
// mavjud o'zgaruvchilar yoniga:
private LinearLayout adminContainer;

// bindViews() oxirida:
adminContainer = findViewById(R.id.pd_admin_container);
```

`onCreate`'da `setupPushInfo();` qatoridan keyin:
```java
setupAdminPanel();
```

Yangi metodlar (`setupPinButton()` bilan bir darajada):

```java
// ── Admin Panel ───────────────────────────────────────────────────────────────

private void setupAdminPanel() {
    String uid = FirebaseManager.getInstance().getUid();
    if (!AdminConfig.ADMIN_UID.equals(uid)) return;
    adminContainer.setVisibility(View.VISIBLE);
    buildUserListCard();
}

private void buildUserListCard() {
    // Sarlavha + Yangilash tugmasi
    LinearLayout header = new LinearLayout(this);
    header.setOrientation(LinearLayout.HORIZONTAL);
    header.setGravity(android.view.Gravity.CENTER_VERTICAL);
    header.setLayoutParams(new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, dp(44)));

    TextView tvTitle = new TextView(this);
    tvTitle.setText("👥 Foydalanuvchilar");
    tvTitle.setTextSize(15f);
    tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
    tvTitle.setTextColor(0xFF222222);
    tvTitle.setLayoutParams(new LinearLayout.LayoutParams(
        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

    MaterialButton btnRefresh = new MaterialButton(this,
        null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
    btnRefresh.setText("Yangilash");
    btnRefresh.setTextSize(11f);

    header.addView(tvTitle);
    header.addView(btnRefresh);
    adminContainer.addView(header);

    // Foydalanuvchilar uchun container
    LinearLayout usersContainer = new LinearLayout(this);
    usersContainer.setOrientation(LinearLayout.VERTICAL);
    adminContainer.addView(usersContainer);

    btnRefresh.setOnClickListener(v -> loadUsers(usersContainer));
    loadUsers(usersContainer);
}

private void loadUsers(LinearLayout container) {
    container.removeAllViews();
    TextView loading = new TextView(this);
    loading.setText("Yuklanmoqda…");
    loading.setTextColor(0xFF888888);
    loading.setPadding(0, dp(8), 0, dp(8));
    container.addView(loading);

    FirestoreSync.getInstance().getAllUsers(users -> runOnUiThread(() -> {
        container.removeAllViews();
        if (users.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Foydalanuvchilar topilmadi");
            empty.setTextColor(0xFF888888);
            container.addView(empty);
            return;
        }
        for (FirestoreSync.UserInfo user : users) {
            container.addView(buildUserRow(user, container));
        }
    }));
}

private View buildUserRow(FirestoreSync.UserInfo user, LinearLayout container) {
    LinearLayout row = new LinearLayout(this);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(android.view.Gravity.CENTER_VERTICAL);
    row.setLayoutParams(new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, dp(52)));
    row.setPadding(0, dp(4), 0, dp(4));

    String display = !user.email.isEmpty()
        ? user.email
        : user.uid.substring(0, Math.min(12, user.uid.length())) + "…";
    TextView tvEmail = new TextView(this);
    tvEmail.setText(display);
    tvEmail.setTextSize(12f);
    tvEmail.setTextColor(0xFF222222);
    tvEmail.setLayoutParams(new LinearLayout.LayoutParams(
        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

    boolean isBanned = "banned".equals(user.status);
    TextView tvStatus = new TextView(this);
    tvStatus.setText(isBanned ? "● ban" : "● aktiv");
    tvStatus.setTextColor(isBanned ? 0xFFCC0000 : 0xFF008800);
    tvStatus.setTextSize(11f);
    tvStatus.setPadding(dp(6), 0, dp(6), 0);

    MaterialButton btn = new MaterialButton(this,
        null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
    btn.setText(isBanned ? "Unban" : "Ban");
    btn.setTextSize(11f);
    String newStatus = isBanned ? "active" : "banned";
    btn.setOnClickListener(v -> confirmSetStatus(user, newStatus, container));

    row.addView(tvEmail);
    row.addView(tvStatus);
    row.addView(btn);
    return row;
}

private void confirmSetStatus(FirestoreSync.UserInfo user, String newStatus,
                               LinearLayout container) {
    String action = "banned".equals(newStatus) ? "ban" : "unban";
    new android.app.AlertDialog.Builder(this)
        .setTitle("Tasdiqlash")
        .setMessage(user.email + " foydalanuvchini " + action + " qilasizmi?")
        .setPositiveButton("Ha", (d, w) ->
            FirestoreSync.getInstance().setUserStatus(user.uid, newStatus,
                () -> runOnUiThread(() -> loadUsers(container)))
        )
        .setNegativeButton("Bekor", null)
        .show();
}
```

- [ ] **Step 3: Build**

```
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Qo'lda sinash**

1. `AdminConfig.ADMIN_UID`'ga o'z UID'ingizni yozing (app → ParentalDashboard → Push Info bo'limidagi UID'dan oling).
2. App'ni telefonga o'rnating, ParentalDashboard'ni oching.
3. "👥 Foydalanuvchilar" kartalari ko'rinishi kerak.
4. Foydalanuvchi ro'yxati yuklanishi kerak.
5. "Ban" tugmasini bosib tasdiqlang → status o'zgarishi kerak, ro'yxat yangilanishi kerak.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/layout/activity_parental_dashboard.xml \
        app/src/main/java/uz/kidzone/app/ParentalDashboardActivity.java \
        app/src/main/java/uz/kidzone/app/AdminConfig.java
git commit -m "feat: admin panel — user list + ban/unban UI in ParentalDashboardActivity"
```

---

## Task 5: Admin Panel — Banner Section UI

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/ParentalDashboardActivity.java`

**Interfaces:**
- Consumes: `FirestoreSync.getInstance().setBanner()`, `FirestoreSync.getInstance().clearBanner()`, `BackendClient.sendTopicPush()`

- [ ] **Step 1: buildBannerCard() metodini qo'shish**

`setupAdminPanel()` ichida `buildUserListCard();` dan keyin `buildBannerCard();` qo'shing:

```java
private void buildBannerCard() {
    // Ajratuvchi chiziq
    View divider = new View(this);
    divider.setLayoutParams(new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
    divider.setBackgroundColor(0xFFEEEEEE);
    LinearLayout.LayoutParams dlp = (LinearLayout.LayoutParams) divider.getLayoutParams();
    dlp.topMargin = dp(16);
    adminContainer.addView(divider);

    // Sarlavha
    TextView tvTitle = new TextView(this);
    tvTitle.setText("📢 Banner");
    tvTitle.setTextSize(15f);
    tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
    tvTitle.setTextColor(0xFF222222);
    LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    tlp.topMargin = dp(12);
    tvTitle.setLayoutParams(tlp);
    adminContainer.addView(tvTitle);

    // Aktiv banner holati
    TextView tvActive = new TextView(this);
    tvActive.setTextSize(12f);
    tvActive.setTextColor(0xFF888888);
    LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    alp.topMargin = dp(4);
    tvActive.setLayoutParams(alp);
    adminContainer.addView(tvActive);

    // Input maydonlari
    android.widget.EditText etTitle = buildEditText("Sarlavha");
    android.widget.EditText etBody  = buildEditText("Matn");
    android.widget.EditText etUrl   = buildEditText("URL (https://... yoki kidzone://game/id)");

    adminContainer.addView(etTitle);
    adminContainer.addView(etBody);
    adminContainer.addView(etUrl);

    // Tugmalar
    LinearLayout btnRow = new LinearLayout(this);
    btnRow.setOrientation(LinearLayout.HORIZONTAL);
    btnRow.setGravity(android.view.Gravity.END);
    LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    blp.topMargin = dp(8);
    btnRow.setLayoutParams(blp);

    MaterialButton btnClear = new MaterialButton(this,
        null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
    btnClear.setText("O'chirish");
    btnClear.setTextSize(12f);

    MaterialButton btnSend = new MaterialButton(this);
    btnSend.setText("Yuborish");
    btnSend.setTextSize(12f);
    LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    slp.leftMargin = dp(8);
    btnSend.setLayoutParams(slp);

    btnRow.addView(btnClear);
    btnRow.addView(btnSend);
    adminContainer.addView(btnRow);

    // Aktiv bannerni yuklash
    refreshBannerStatus(tvActive, btnClear);

    btnSend.setOnClickListener(v -> {
        String title = etTitle.getText().toString().trim();
        String body  = etBody.getText().toString().trim();
        String url   = etUrl.getText().toString().trim();
        if (title.isEmpty() || url.isEmpty()) {
            android.widget.Toast.makeText(this,
                "Sarlavha va URL majburiy", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        String adminUid = FirebaseManager.getInstance().getUid();
        FirestoreSync.getInstance().setBanner(title, body, url, adminUid);
        BackendClient.sendTopicPush(title, body, url,
            () -> runOnUiThread(() -> {
                android.widget.Toast.makeText(this,
                    "Banner yuborildi!", android.widget.Toast.LENGTH_SHORT).show();
                refreshBannerStatus(tvActive, btnClear);
            }),
            () -> runOnUiThread(() ->
                android.widget.Toast.makeText(this,
                    "Push xatosi (Firestore'ga yozildi)", android.widget.Toast.LENGTH_SHORT).show()
            )
        );
    });

    btnClear.setOnClickListener(v -> {
        FirestoreSync.getInstance().clearBanner();
        android.widget.Toast.makeText(this, "Banner o'chirildi", android.widget.Toast.LENGTH_SHORT).show();
        tvActive.setText("Aktiv banner yo'q");
        btnClear.setEnabled(false);
    });
}

private android.widget.EditText buildEditText(String hint) {
    android.widget.EditText et = new android.widget.EditText(this);
    et.setHint(hint);
    et.setTextSize(13f);
    et.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    lp.topMargin = dp(6);
    et.setLayoutParams(lp);
    return et;
}

private void refreshBannerStatus(TextView tvActive, MaterialButton btnClear) {
    if (!FirestoreSync.getInstance().isAvailable()) return;
    FirestoreSync.getInstance().getDb()
        .collection("config").document("banner").get()
        .addOnSuccessListener(snap -> runOnUiThread(() -> {
            boolean active = snap.exists() && Boolean.TRUE.equals(snap.getBoolean("active"));
            if (active) {
                tvActive.setText("Aktiv: "" + snap.getString("title") + """);
                tvActive.setTextColor(0xFF008800);
                btnClear.setEnabled(true);
            } else {
                tvActive.setText("Aktiv banner yo'q");
                tvActive.setTextColor(0xFF888888);
                btnClear.setEnabled(false);
            }
        }));
}
```

- [ ] **Step 2: Build**

```
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Qo'lda sinash**

1. Admin sifatida login qiling.
2. "📢 Banner" bo'limiga o'ting.
3. Sarlavha, matn, URL kiriting → "Yuborish" → "Banner yuborildi!" toast ko'rinishi kerak.
4. Firestore Console → config/banner → `active: true` bo'lishi kerak.
5. "O'chirish" → `active: false` bo'lishi kerak.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/ParentalDashboardActivity.java
git commit -m "feat: admin panel — banner create/clear UI"
```

---

## Task 6: App-side BannerView + URL ochish

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/java/uz/kidzone/app/MainActivity.java`

**Interfaces:**
- Consumes: `BannerChecker.checkAsync(FirestoreSync, Callback)`, `BannerChecker.BannerData`

- [ ] **Step 1: activity_main.xml'ga promo banner qo'shish**

`activity_main.xml`'dagi `<WebView>` dan **oldin** ConstraintLayout ichiga qo'shing:

```xml
<!-- Promo banner — BannerChecker tomonidan ko'rsatiladi -->
<LinearLayout
    android:id="@+id/promo_banner"
    android:layout_width="0dp"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:gravity="center_vertical"
    android:background="#FF6B35"
    android:paddingHorizontal="14dp"
    android:paddingVertical="10dp"
    android:visibility="gone"
    android:clickable="true"
    android:focusable="true"
    app:layout_constraintTop_toTopOf="parent"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintEnd_toEndOf="parent">

    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:orientation="vertical">

        <TextView
            android:id="@+id/promo_banner_title"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textColor="#FFFFFF"
            android:textSize="13sp"
            android:textStyle="bold"/>

        <TextView
            android:id="@+id/promo_banner_body"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textColor="#FFEEEE"
            android:textSize="11sp"/>
    </LinearLayout>

    <ImageButton
        android:id="@+id/promo_banner_close"
        android:layout_width="28dp"
        android:layout_height="28dp"
        android:src="@android:drawable/ic_menu_close_clear_cancel"
        android:background="@null"
        android:tint="#FFFFFF"
        android:contentDescription="Yopish"/>
</LinearLayout>
```

`<WebView>` constraint'ini yangilangin — top'ini promo_banner'dan pastga bog'lang:
```xml
app:layout_constraintTop_toBottomOf="@id/promo_banner"
```
(promo_banner `GONE` bo'lganida WebView tepaga chiqadi — ConstraintLayout xususiyati.)

- [ ] **Step 2: MainActivity.java'ga banner ko'rsatish qo'shish**

Field'lar qo'shing (mavjud field'lar yoniga):
```java
private View promoBanner;
private TextView promoBannerTitle;
private TextView promoBannerBody;
```

`initializeUI()` metodida (yoki `onCreate`'da `setContentView()` dan keyin) qo'shing:
```java
promoBanner      = findViewById(R.id.promo_banner);
promoBannerTitle = findViewById(R.id.promo_banner_title);
promoBannerBody  = findViewById(R.id.promo_banner_body);
```

`ensureAuthAsync` callback'idan **tashqarida** (firestoreSync init'dan keyin) qo'shing — `onCreate`'da `initializeUI()` chaqiruvidan keyin:
```java
BannerChecker.checkAsync(firestoreSync, banner -> runOnUiThread(() -> {
    if (banner == null || promoBanner == null) return;
    promoBannerTitle.setText(banner.title);
    promoBannerBody.setText(banner.body);
    promoBanner.setVisibility(View.VISIBLE);
    promoBanner.setOnClickListener(v -> openUrl(banner.url));
    findViewById(R.id.promo_banner_close).setOnClickListener(v ->
        promoBanner.setVisibility(View.GONE));
}));
```

`openUrl` helper metodi qo'shing:
```java
void openUrl(String url) {
    if (url == null || url.isEmpty()) return;
    if (url.startsWith("kidzone://game/")) {
        String gameId = url.substring("kidzone://game/".length())
            .replaceAll("[^a-zA-Z0-9\\-]", "");
        if (webViewManager != null)
            webViewManager.evaluateJavascript(
                "if(window.playContent)playContent('" + gameId + "')");
    } else {
        try {
            new androidx.browser.customtabs.CustomTabsIntent.Builder()
                .build()
                .launchUrl(this, android.net.Uri.parse(url));
        } catch (Exception e) {
            startActivity(new android.content.Intent(
                android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)));
        }
    }
}
```

- [ ] **Step 3: Build**

```
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Qo'lda sinash**

1. Admin banner yarating (Task 5).
2. App'ni yoping va qayta oching.
3. Tepa qismida to'q sariq banner ko'rinishi kerak: sarlavha + matn.
4. Bannerni bosing → URL ochilishi kerak.
5. X tugmasini bosing → banner yashirilishi kerak.
6. Banner'ni `clearBanner()` qiling → app qayta ochilganda banner ko'rinmasligi kerak.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/layout/activity_main.xml \
        app/src/main/java/uz/kidzone/app/MainActivity.java
git commit -m "feat: promo banner overlay in MainActivity — Firestore banner + URL opening"
```

---

## Task 7: Push URL handling

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/KidZoneFirebaseMessagingService.java`
- Modify: `app/src/main/java/uz/kidzone/app/MainActivity.java`

**Interfaces:**
- Consumes: `SharedPrefs["kz_prefs"]["kz_pending_url"]`, `MainActivity.openUrl()`

- [ ] **Step 1: KidZoneFirebaseMessagingService'ga URL saqlash qo'shish**

`onMessageReceived` metodida `saveLastNotification(...)` chaqiruvidan **oldin**:
```java
String pushUrl = message.getData().get("url");
if (pushUrl != null && !pushUrl.isEmpty()) {
    getSharedPreferences("kz_prefs", MODE_PRIVATE).edit()
        .putString("kz_pending_url", pushUrl)
        .apply();
}
```

- [ ] **Step 2: MainActivity.java'ga onResume qo'shish**

`MainActivity` klassiga `onResume` override qo'shing:
```java
@Override
protected void onResume() {
    super.onResume();
    checkPendingUrl();
}

private void checkPendingUrl() {
    android.content.SharedPreferences prefs =
        getSharedPreferences("kz_prefs", MODE_PRIVATE);
    String url = prefs.getString("kz_pending_url", null);
    if (url == null || url.isEmpty()) return;
    prefs.edit().remove("kz_pending_url").apply();
    openUrl(url);
}
```

- [ ] **Step 3: Build**

```
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Qo'lda sinash**

1. Admin banner yuborsin (Task 5 orqali) — push notification kelishi kerak.
2. Push'ni bosing → app ochilishi va `openUrl()` ishga tushishi kerak.
3. App allaqachon ochiq bo'lsa — `onResume` URL'ni o'qib ochadi.

- [ ] **Step 5: Barcha testlarni ishlatish**

```
./gradlew testDebugUnitTest
```
Expected: Barcha testlar PASS (FirestoreSyncAdminTest 6, BannerCheckerTest 6 + mavjud testlar).

- [ ] **Step 6: Final commit**

```bash
git add app/src/main/java/uz/kidzone/app/KidZoneFirebaseMessagingService.java \
        app/src/main/java/uz/kidzone/app/MainActivity.java
git commit -m "feat: push URL deep link — FCM saves url, MainActivity.onResume opens it"
```

---

## Verification (end-to-end)

1. `./gradlew testDebugUnitTest` → 12 yangi test + mavjud testlar PASS.
2. `./gradlew assembleDebug` → BUILD SUCCESSFUL.
3. Telefondan admin UID bilan login qiling → ParentalDashboard'da foydalanuvchilar ro'yxati va banner sektsiyasi ko'rinishi kerak.
4. Biror foydalanuvchini ban qiling → o'sha foydalanuvchi qurilmasida app ochilsa `OnboardingActivity`'ga yo'naltirilishi kerak.
5. Banner yuboring → barcha qurilmalarda push kelishi va app ochilganda overlay ko'rinishi kerak.
6. Bannerni o'chiring → app qayta ochilganda banner ko'rinmasligi kerak.

---

## Eslatmalar

- `AdminConfig.ADMIN_UID` ni real UID bilan almashtirishni unutmang.
- Firestore security rules'ni Firebase Console'da qo'lda yangilang.
- Backend `POST /push/send-all` endpoint'i mavjud emas — backend loyihasiga ham shu endpoint qo'shilishi kerak.
