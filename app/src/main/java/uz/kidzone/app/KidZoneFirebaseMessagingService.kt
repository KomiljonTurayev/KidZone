package uz.kidzone.app

import android.app.Notification
import android.content.SharedPreferences
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class KidZoneFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "KzFCM"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onMessageReceived(message: RemoteMessage) {
        var title: String? = null
        var body: String? = null

        if (message.notification != null) {
            title = message.notification?.title
            body  = message.notification?.body
        }
        if (title.isNullOrEmpty()) title = message.data["title"]
        if (body.isNullOrEmpty())  body  = message.data["body"]

        val pushUrl = message.data["url"]
        if (!pushUrl.isNullOrEmpty()) {
            getSharedPreferences("kz_prefs", MODE_PRIVATE).edit()
                .putString("kz_pending_url", pushUrl)
                .apply()
        }

        val finalTitle = if (!title.isNullOrEmpty()) title else "KidZone"
        val finalBody  = body ?: ""
        saveLastNotification(finalTitle, finalBody)
        showNotification(finalTitle, finalBody)
    }

    private fun saveLastNotification(title: String, body: String) {
        getSharedPreferences("kz_prefs", MODE_PRIVATE).edit()
            .putString("kz_last_notif_title", title)
            .putString("kz_last_notif_body", body)
            .putLong("kz_last_notif_time", System.currentTimeMillis())
            .apply()
    }

    override fun onNewToken(token: String) {
        val uid = FirebaseManager.getInstance().getUid()
        if (uid != null) {
            FirestoreSync.getInstance().updateFcmToken(uid, token)
        } else {
            Log.d(TAG, "onNewToken: uid null, token not synced yet")
        }
    }

    fun showNotification(title: String, body: String) {
        val n = buildNotification(title, body)
        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, n)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission not granted: ${e.message}")
        }
    }

    fun buildNotification(title: String, body: String): Notification {
        return NotificationCompat.Builder(this, KidZoneApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
    }
}
