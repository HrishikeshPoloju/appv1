package com.schoolapp.android.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.schoolapp.android.MainActivity
import com.schoolapp.android.R

object NotificationTopicManager {
    const val CHANNEL_ID_NOTICES = "school_notices"
    private const val PREFS_FCM = "genesis_fcm_prefs"
    private const val KEY_SUBSCRIBED_TOPIC = "subscribed_school_topic"

    fun ensureNoticeChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_NOTICES,
                "School Notices & Events",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Official circulars, announcements, and events posted by your school."
                enableVibration(true)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    fun topicNameForSchool(schoolId: String): String {
        val clean = schoolId.trim().lowercase().replace("-", "_")
            .ifBlank { "00000000_0000_0000_0000_000000000001" }
        return "school_$clean"
    }

    fun syncSchoolNoticeTopic(context: Context, schoolId: String) {
        ensureNoticeChannel(context)
        val targetTopic = topicNameForSchool(schoolId)
        val prefs = context.getSharedPreferences(PREFS_FCM, Context.MODE_PRIVATE)
        val previousTopic = prefs.getString(KEY_SUBSCRIBED_TOPIC, null)

        val fcm = FirebaseMessaging.getInstance()
        if (!previousTopic.isNullOrBlank() && previousTopic != targetTopic) {
            fcm.unsubscribeFromTopic(previousTopic)
                .addOnCompleteListener {
                    Log.i("GenesisFCM", "Unsubscribed from previous topic: $previousTopic")
                }
        }

        fcm.subscribeToTopic(targetTopic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    prefs.edit().putString(KEY_SUBSCRIBED_TOPIC, targetTopic).apply()
                    Log.i("GenesisFCM", "Subscribed to FREE school notice topic: $targetTopic")
                } else {
                    Log.w("GenesisFCM", "Failed to subscribe to topic $targetTopic", task.exception)
                }
            }
    }
}

class GenesisFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i("GenesisFCM", "New FCM registration token generated: ${token.take(18)}...")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "New School Notice"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: "Tap to open the notice in your school app."

        showNoticeNotification(title, body)
    }

    private fun showNoticeNotification(title: String, body: String) {
        NotificationTopicManager.ensureNoticeChannel(this)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_tab", "notices")
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, NotificationTopicManager.CHANNEL_ID_NOTICES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(this)
                .notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), builder.build())
        } catch (se: SecurityException) {
            Log.w("GenesisFCM", "Notification permission not granted yet: ${se.message}")
        }
    }
}
