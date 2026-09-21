package com.andy.englishcoach.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * Scheduler for daily learning reminder using Android AlarmManager.
 * Uses a single deterministic RequestCode and PendingIntent identity to prevent duplicate alarms.
 */
object DailyReminderScheduler {

    const val REMINDER_REQUEST_CODE = 2001
    const val NOTIFICATION_CHANNEL_ID = "englishcoach_daily_reminder"
    const val NOTIFICATION_ID = 1001

    /**
     * Schedules or updates the daily reminder alarm at the specified [hour] and [minute].
     * Replaces any preexisting alarm with the same PendingIntent identity.
     */
    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = createPendingIntent(context)

        // Cancel any existing alarm with this PendingIntent first to guarantee single alarm
        alarmManager.cancel(pendingIntent)

        // Calculate next trigger time
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If scheduled time has already passed today, advance to tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } catch (_: SecurityException) {
            // Gracefully handled if alarms are restricted
        }
    }

    /**
     * Cancels the daily reminder alarm if scheduled.
     */
    fun cancelDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = createPendingIntent(context)
        alarmManager.cancel(pendingIntent)
    }

    private fun createPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = "com.andy.englishcoach.action.DAILY_REMINDER"
        }
        return PendingIntent.getBroadcast(
            context,
            REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
