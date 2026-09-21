package com.andy.englishcoach.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.andy.englishcoach.MainActivity
import com.andy.englishcoach.R

/**
 * BroadcastReceiver triggered by AlarmManager to post the daily reminder notification.
 */
class DailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        // Create channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                DailyReminderScheduler.NOTIFICATION_CHANNEL_ID,
                "每日學習提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "每天提醒你回來學習商務英文單字"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Tap opens MainActivity
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, DailyReminderScheduler.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("設定每日學習提醒")
            .setContentText("每天提醒你回來學英文，選一個適合你的時間背單字！")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("每天提醒你回來學英文！選一個適合你的時間，讓 EnglishCoach 每天提醒你背單字。")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(tapPendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(DailyReminderScheduler.NOTIFICATION_ID, notification)
    }
}
