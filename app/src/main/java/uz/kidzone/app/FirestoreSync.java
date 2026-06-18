package uz.kidzone.app;

import android.content.Context;
import android.util.Log;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FirestoreSync {

    private static final String TAG = "FirestoreSync";
    private static FirestoreSync instance;
    private final FirebaseFirestore db;

    /** Package-private for unit testing. */
    FirestoreSync(FirebaseFirestore db) {
        this.db = db;
    }

    public static synchronized FirestoreSync init(Context ctx) {
        if (instance == null) {
            FirebaseFirestore db;
            try {
                db = FirebaseFirestore.getInstance();
            } catch (Exception e) {
                Log.e(TAG, "Firestore init FAILED: " + e.getMessage());
                db = null;
            }
            instance = new FirestoreSync(db);
            Log.d(TAG, "init complete, available=" + (db != null));
        }
        return instance;
    }

    public static synchronized FirestoreSync getInstance() {
        return instance != null ? instance : new FirestoreSync(null);
    }

    public boolean isAvailable() {
        return db != null;
    }

    /** Package-private for BanChecker. */
    FirebaseFirestore getDb() {
        return db;
    }

    public void syncUserProfile(String uid, String displayName, String email, String ageGroup) {
        Log.d(TAG, "syncUserProfile: uid=" + uid + " available=" + isAvailable());
        if (!isAvailable() || uid == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("displayName", displayName != null ? displayName : "");
        data.put("email", email != null ? email : "");
        data.put("ageGroup", normalizeAgeGroup(ageGroup));
        data.put("status", "active");
        data.put("lastActiveAt", FieldValue.serverTimestamp());

        DocumentReference ref = db.collection("users").document(uid);
        ref.get()
            .addOnSuccessListener(snap -> {
                if (!snap.exists() || !snap.contains("createdAt")) {
                    data.put("createdAt", FieldValue.serverTimestamp());
                }
                ref.set(data, SetOptions.merge())
                   .addOnFailureListener(e -> Log.w(TAG, "syncUserProfile failed: " + e));
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "syncUserProfile get() failed, skipping createdAt: " + e);
                ref.set(data, SetOptions.merge())
                   .addOnFailureListener(e2 -> Log.w(TAG, "syncUserProfile set failed: " + e2));
            });
    }

    public void updateFcmToken(String uid, String token) {
        if (!isAvailable() || uid == null || token == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("fcmToken", token);
        db.collection("users").document(uid).set(data, SetOptions.merge());
    }

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
        db.collection("users").document(uid).set(data, SetOptions.merge())
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

    public void recordSession(String uid, long sessionMinutes, Map<String, String> gamePlays, boolean isFirstSession) {
        if (!isAvailable() || uid == null || sessionMinutes <= 0) {
            Log.w(TAG, "recordSession skipped: available=" + isAvailable()
                + " uid=" + uid + " minutes=" + sessionMinutes);
            return;
        }
        String dateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        Log.d(TAG, "recordSession: date=" + dateKey + " uid=" + uid
            + " minutes=" + sessionMinutes + " firstSession=" + isFirstSession);
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMinutes", FieldValue.increment(sessionMinutes));
        stats.put("totalSessions", FieldValue.increment(1));
        if (isFirstSession) {
            stats.put("dau", FieldValue.increment(1));
        }
        if (gamePlays != null) {
            for (Map.Entry<String, String> entry : gamePlays.entrySet()) {
                String gameId   = entry.getKey();
                String gameName = entry.getValue();
                stats.put("gameBreakdown." + gameId + ".gameName", gameName);
                stats.put("gameBreakdown." + gameId + ".playCount", FieldValue.increment(1));
            }
        }
        db.collection("stats").document(dateKey).set(stats, SetOptions.merge())
            .addOnSuccessListener(v -> Log.d(TAG, "recordSession write OK: " + dateKey))
            .addOnFailureListener(e -> Log.e(TAG, "recordSession write FAILED: " + e.getMessage()));
    }

    static String normalizeAgeGroup(String ageGroup) {
        if (ageGroup == null) return "3-5";
        switch (ageGroup) {
            case "2-4": return "3-5";
            case "3-5":
            case "5-7":
            case "7+":  return ageGroup;
            default:    return "3-5";
        }
    }
}
