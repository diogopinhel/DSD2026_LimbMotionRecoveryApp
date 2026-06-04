package com.example.limbmotionrecoveryapp.notifications

import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.limbmotionrecoveryapp.R
import com.dsd.m1.api.V2ApiClient
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FcmService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FcmService"
        private const val CHANNEL_ID = "default_channel"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed: $token")
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()
        registerTokenToBackend(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val title = remoteMessage.notification?.title ?: "Notification"
        val body = remoteMessage.notification?.body ?: ""
        showNotification(title, body)
    }

    private fun registerTokenToBackend(token: String) {
        val authPrefs = getSharedPreferences("auth", MODE_PRIVATE)
        val userId = authPrefs.getInt("userId", 0)
        val authToken = authPrefs.getString("token", null)
        if (userId > 0 && authToken != null) {
            Thread {
                try {
                    V2ApiClient().registerPushToken(userId, token, "android", authToken)
                    Log.d(TAG, "Push token registered to backend")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to register push token: ${e.message}")
                }
            }.start()
        }
    }

    private fun showNotification(title: String, body: String) {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
        NotificationManagerCompat.from(this).notify(0, builder.build())
    }
}
