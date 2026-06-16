package uz.kidzone.app;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import com.chuckerteam.chucker.api.ChuckerInterceptor;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import uz.kidzone.app.BuildConfig;

public class KidZoneApplication extends Application {

    public static final String CHANNEL_ID = "kidzone_push";

    private static OkHttpClient httpClient;

    public static OkHttpClient getHttpClient() { return httpClient; }

    @Override
    public void onCreate() {
        super.onCreate();
        httpClient = new OkHttpClient.Builder()
            .addInterceptor(new ChuckerInterceptor.Builder(this).build())
            .build();
        if (BuildConfig.DEBUG) {
            FirebaseFirestore.setLoggingEnabled(true);
        }
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            Log.d("KZ_DEBUG", "Auth OK: uid=" + user.getUid());
            syncToFirestore(user.getUid());
        } else {
            auth.signInAnonymously()
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    Log.d("KZ_DEBUG", "signInAnonymously OK: uid=" + uid);
                    syncToFirestore(uid);
                })
                .addOnFailureListener(e ->
                    Log.e("KZ_DEBUG", "signInAnonymously FAILED: " + e.getMessage()));
        }
        createNotificationChannel();
    }

    private void syncToFirestore(String uid) {
        SharedPreferences prefs = getSharedPreferences("kz_prefs", MODE_PRIVATE);
        prefs.edit().putString("kz_uid", uid).apply();
        String ageGroup = prefs.getString("kz_age_filter", "3-5");
        FirestoreSync.init(this).syncUserProfile(uid, null, null, ageGroup);
        FirebaseMessaging.getInstance().getToken()
            .addOnSuccessListener(token -> {
                prefs.edit().putString("kz_fcm_token", token).apply();
                FirestoreSync.getInstance().updateFcmToken(uid, token);
                registerTokenWithBackend(token);
            })
            .addOnFailureListener(e ->
                Log.w("KZ_DEBUG", "FCM token fetch failed: " + e.getMessage()));
    }

    private void registerTokenWithBackend(String fcmToken) {
        FirebaseAuth.getInstance().getCurrentUser()
            .getIdToken(false)
            .addOnSuccessListener(result -> {
                String idToken = result.getToken();
                String json = "{\"fcmToken\":\"" + fcmToken + "\"}";
                RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
                Request request = new Request.Builder()
                    .url("https://kidzone-backend-s7to.onrender.com/push/register-token")
                    .addHeader("Authorization", "Bearer " + idToken)
                    .post(body)
                    .build();
                httpClient.newCall(request).enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) {
                        Log.w("KZ_DEBUG", "registerToken failed: " + e.getMessage());
                    }
                    @Override public void onResponse(Call call, Response response) {
                        Log.d("KZ_DEBUG", "registerToken HTTP " + response.code());
                        response.close();
                    }
                });
            })
            .addOnFailureListener(e ->
                Log.w("KZ_DEBUG", "getIdToken failed: " + e.getMessage()));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "KidZone Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }
}
