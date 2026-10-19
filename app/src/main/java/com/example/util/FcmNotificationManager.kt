package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Handles simulated local push notifications for previewing admin broadcasts.
 * NOTE: This is a local on-device notification simulation. Real production FCM
 * requires registering Firebase Cloud Messaging server-side keys and tokens.
 */
object FcmNotificationManager {
    const val CHANNEL_ID = "creator_simulated_alerts_channel"
    private const val CHANNEL_NAME = "Simulated Creator Announcements (Preview)"

    fun setupNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Simulated preview alerts sent by Creator Admin (Local Notification Mode)"
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendLocalPushNotification(context: Context, title: String, message: String) {
        try {
            setupNotificationChannel(context)
            val notificationId = (1000..9999).random()

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)

            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, builder.build())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
