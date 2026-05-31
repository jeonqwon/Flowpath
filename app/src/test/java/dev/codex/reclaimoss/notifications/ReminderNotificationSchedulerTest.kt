package dev.codex.reclaimoss.notifications

import dev.codex.reclaimoss.domain.model.Reminder
import dev.codex.reclaimoss.domain.model.ReminderStatus
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderNotificationSchedulerTest {
    @Test
    fun `pending reminder notifications ignore past and completed reminders`() {
        val now = Instant.parse("2026-05-31T12:00:00Z")
        val reminders = listOf(
            Reminder(id = "future", title = "Future", dueAt = now.plusSeconds(600)),
            Reminder(id = "past", title = "Past", dueAt = now.minusSeconds(600)),
            Reminder(id = "done", title = "Done", dueAt = now.plusSeconds(1200), status = ReminderStatus.COMPLETED),
        )

        val pending = pendingReminderNotifications(reminders, now)

        assertEquals(listOf(PendingReminderNotification("future", now.plusSeconds(600))), pending)
    }
}
