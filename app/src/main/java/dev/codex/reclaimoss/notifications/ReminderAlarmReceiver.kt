package dev.codex.reclaimoss.notifications

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.codex.reclaimoss.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Reminder" }
        val description = intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty()
        val linkedTaskId = intent.getStringExtra(EXTRA_LINKED_TASK_ID)
        val dueAtMillis = intent.getLongExtra(EXTRA_DUE_AT_MILLIS, 0L)
        val isAllDay = intent.getBooleanExtra(EXTRA_IS_ALL_DAY, false)
        val fallbackLine = dueSummaryText(
            dueAtMillis = dueAtMillis,
            isAllDay = isAllDay,
            linkedTaskId = linkedTaskId,
        )
        val body = description.ifBlank { fallbackLine }

        val contentIntent = androidx.core.app.TaskStackBuilder.create(context)
            .addNextIntentWithParentStack(launcherIntent(context))
            .getPendingIntent(
                reminderId.hashCode(),
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
            )

        val notification = NotificationCompat.Builder(context, ReminderNotificationScheduler.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context).notify(reminderId.hashCode(), notification)
    }

    private fun dueSummaryText(
        dueAtMillis: Long,
        isAllDay: Boolean,
        linkedTaskId: String?,
    ): String {
        val zonedDateTime = Instant.ofEpochMilli(dueAtMillis).atZone(ZoneId.systemDefault())
        return when {
            isAllDay -> "All-day reminder for ${zonedDateTime.format(DATE_FORMATTER)}"
            linkedTaskId != null -> "Task reminder for ${zonedDateTime.format(DATE_TIME_FORMATTER)}"
            else -> "Reminder for ${zonedDateTime.format(DATE_TIME_FORMATTER)}"
        }
    }

    companion object {
        private const val ACTION_SHOW_REMINDER = "dev.codex.reclaimoss.SHOW_REMINDER"
        private const val EXTRA_REMINDER_ID = "extra_reminder_id"
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_DESCRIPTION = "extra_description"
        private const val EXTRA_DUE_AT_MILLIS = "extra_due_at_millis"
        private const val EXTRA_IS_ALL_DAY = "extra_is_all_day"
        private const val EXTRA_LINKED_TASK_ID = "extra_linked_task_id"
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d")
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM d, h:mm a")

        fun createIntent(
            context: Context,
            reminderId: String,
            title: String,
            description: String,
            dueAtEpochMillis: Long,
            isAllDay: Boolean,
            linkedTaskId: String?,
        ): Intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_SHOW_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_DESCRIPTION, description)
            putExtra(EXTRA_DUE_AT_MILLIS, dueAtEpochMillis)
            putExtra(EXTRA_IS_ALL_DAY, isAllDay)
            putExtra(EXTRA_LINKED_TASK_ID, linkedTaskId)
        }
    }
}
