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
