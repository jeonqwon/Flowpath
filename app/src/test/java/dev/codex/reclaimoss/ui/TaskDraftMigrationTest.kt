package dev.codex.reclaimoss.ui

import dev.codex.reclaimoss.domain.model.RecurrenceRule
import dev.codex.reclaimoss.domain.model.ScheduleTask
import dev.codex.reclaimoss.domain.model.TaskPriority
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertNull
import org.junit.Test

class TaskDraftMigrationTest {

    @Test
    fun `follow up draft clears preferred period`() {
        val draft = task(preferredTimePeriodId = "period-evening").toFollowUpDraft()

        assertNull(draft.preferredTimePeriodId)
    }

    @Test
    fun `reschedule draft clears preferred period`() {
        val draft = task(preferredTimePeriodId = "period-evening").toRescheduleDraft(ZoneId.of("America/New_York"))

        assertNull(draft.preferredTimePeriodId)
    }

    private fun task(preferredTimePeriodId: String?): ScheduleTask =
        ScheduleTask(
            id = "task-1",
            title = "Task",
            priority = TaskPriority.MEDIUM,
            preferredTimePeriodId = preferredTimePeriodId,
            dueAt = Instant.parse("2026-05-31T18:00:00Z"),
            estimatedMinutes = 60,
            remainingMinutes = 60,
            recurrenceRule = RecurrenceRule(),
        )
}
