package dev.codex.reclaimoss.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dev.codex.reclaimoss.MainActivity
import dev.codex.reclaimoss.domain.model.Reminder
import dev.codex.reclaimoss.domain.model.ReminderStatus
import java.time.Instant

internal data class PendingReminderNotification(
    val reminderId: String,
    val triggerAt: Instant,
)

internal fun pendingReminderNotifications(
    reminders: List<Reminder>,
    now: Instant,
): List<PendingReminderNotification> = reminders
    .filter { it.status != ReminderStatus.COMPLETED }
    .filter { it.dueAt.isAfter(now) }
    .sortedBy { it.dueAt }
    .map { PendingReminderNotification(reminderId = it.id, triggerAt = it.dueAt) }

class ReminderNotificationScheduler(
    private val context: Context,
) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)
    private val notificationManager = appContext.getSystemService(NotificationManager::class.java)
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Task and reminder alerts"
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun sync(reminders: List<Reminder>, now: Instant = Instant.now()) {
        ensureChannel()
        val desiredReminders = reminders
            .filter { it.status != ReminderStatus.COMPLETED }
            .filter { it.dueAt.isAfter(now) }
        val desiredIds = desiredReminders.map { it.id }.toSet()
        val knownIds = prefs.getStringSet(KEY_KNOWN_IDS, emptySet()).orEmpty()

        (knownIds - desiredIds).forEach(::cancel)
        desiredReminders.forEach(::schedule)

        prefs.edit().putStringSet(KEY_KNOWN_IDS, desiredIds).apply()
    }

    fun cancel(reminderId: String) {
        val pendingIntent = alarmPendingIntent(reminderId)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        notificationManager.cancel(reminderId.hashCode())
    }

    private fun schedule(reminder: Reminder) {
        val intent = ReminderAlarmReceiver.createIntent(
            context = appContext,
            reminderId = reminder.id,
            title = reminder.title,
            description = reminder.description,
            dueAtEpochMillis = reminder.dueAt.toEpochMilli(),
            isAllDay = reminder.isAllDay,
            linkedTaskId = reminder.linkedTaskId,
        )
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            reminder.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pendingIntent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.dueAt.toEpochMilli(),
                pendingIntent,
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                reminder.dueAt.toEpochMilli(),
                pendingIntent,
            )
        }
    }

    private fun alarmPendingIntent(reminderId: String): PendingIntent =
        PendingIntent.getBroadcast(
            appContext,
            reminderId.hashCode(),
            ReminderAlarmReceiver.createIntent(
                context = appContext,
                reminderId = reminderId,
                title = "",
                description = "",
                dueAtEpochMillis = 0L,
                isAllDay = false,
                linkedTaskId = null,
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        const val CHANNEL_ID = "task_reminders"
        private const val PREFS_NAME = "reminder_notification_scheduler"
        private const val KEY_KNOWN_IDS = "known_ids"
    }
}

internal fun launcherIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
