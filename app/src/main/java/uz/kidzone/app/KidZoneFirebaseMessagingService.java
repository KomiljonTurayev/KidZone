package uz.kidzone.app;

import android.app.Notification;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class KidZoneFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "KzFCM";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onMessageReceived(RemoteMessage message) {
        String title = null;
        String body  = null;

        if (message.getNotification() != null) {
            title = message.getNotification().getTitle();
            body  = message.getNotification().getBody();
        }
        if (title == null || title.isEmpty()) title = message.getData().get("title");
        if (body  == null || body.isEmpty())  body  = message.getData().get("body");

        showNotification(
            title != null && !title.isEmpty() ? title : "KidZone",
            body  != null ? body : ""
        );
    }

    @Override
    public void onNewToken(String token) {
        String uid = FirebaseManager.getInstance().getUid();
        if (uid != null) {
            FirestoreSync.getInstance().updateFcmToken(uid, token);
        } else {
            Log.d(TAG, "onNewToken: uid null, token not synced yet");
        }
    }

    void showNotification(String title, String body) {
        Notification n = buildNotification(title, body);
        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, n);
        } catch (SecurityException e) {
            Log.w(TAG, "Notification permission not granted: " + e.getMessage());
        }
    }

    Notification buildNotification(String title, String body) {
        return new NotificationCompat.Builder(this, KidZoneApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build();
    }
}
