package com.example.taskly.firebase

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.taskly.MainActivity
import com.example.taskly.R
import com.example.taskly.network.RetrofitInstance
import com.example.taskly.network.TokenUpdate
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Token baru: $token")

        val sharedPreferences = getSharedPreferences("TasklyPrefs", MODE_PRIVATE)
        val userId = sharedPreferences.getInt("userId", -1)
        if (userId != -1) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    RetrofitInstance.ApiServiceFactory.apiService.updateToken(TokenUpdate(userId, token))
                } catch (e: Exception) {
                    Log.e("FCM", "Error saat memperbarui token", e)
                }
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"]
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"]
        val channelId = remoteMessage.data["channelId"] ?: "default_channel"

        if (title.isNullOrBlank() || body.isNullOrBlank() || channelId.isNullOrBlank()) {
            Log.e("FCM", "Payload notifikasi tidak valid.")
            return
        }

        showNotification(title, body, channelId)
    }

    private fun showNotification(title: String, body: String, channelId: String) {
        val notificationId = System.currentTimeMillis().toInt()
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            with(NotificationManagerCompat.from(this)) {
                notify(notificationId, builder.build())
            }
        } else {
            Log.e("FCM", "Izin notifikasi tidak diberikan.")
        }
    }
}
