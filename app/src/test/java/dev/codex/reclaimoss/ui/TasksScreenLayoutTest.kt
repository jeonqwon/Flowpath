package dev.codex.reclaimoss.ui

import dev.codex.reclaimoss.domain.model.BlockCompletionState
import dev.codex.reclaimoss.domain.model.BlockLockState
import dev.codex.reclaimoss.domain.model.BlockSource
import dev.codex.reclaimoss.domain.model.RecurrenceRule
import dev.codex.reclaimoss.domain.model.Reminder
import dev.codex.reclaimoss.domain.model.ScheduleBlock
import dev.codex.reclaimoss.domain.model.ScheduleTask
import dev.codex.reclaimoss.domain.model.TaskPriority
import dev.codex.reclaimoss.domain.model.TaskStatus
import dev.codex.reclaimoss.domain.model.Timeframe
import dev.codex.reclaimoss.settings.TasksViewMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TasksScreenLayoutTest {

    @Test
    fun `visible blocks for day split overnight block at midnight`() {
        val zoneId = ZoneId.of("America/New_York")
        val block = scheduleBlock(
            startAt = Instant.parse("2026-06-02T02:00:00Z"),
            endAt = Instant.parse("2026-06-02T10:00:00Z"),
        )

        val firstDay = visibleBlocksForDay(
            blocks = listOf(block),
            day = LocalDate.of(2026, 6, 1),
            zoneId = zoneId,
        )
        val secondDay = visibleBlocksForDay(
            blocks = listOf(block),
            day = LocalDate.of(2026, 6, 2),
            zoneId = zoneId,
        )

        assertEquals(1, firstDay.size)
        assertEquals("2026-06-02T02:00:00Z", firstDay.single().startAt.toString())
        assertEquals("2026-06-02T04:00:00Z", firstDay.single().endAt.toString())

        assertEquals(1, secondDay.size)
        assertEquals("2026-06-02T04:00:00Z", secondDay.single().startAt.toString())
        assertEquals("2026-06-02T10:00:00Z", secondDay.single().endAt.toString())
    }

    @Test
    fun `visible blocks for day excludes blocks outside selected day`() {
        val zoneId = ZoneId.of("America/New_York")
        val block = scheduleBlock(
            startAt = Instant.parse("2026-06-02T14:00:00Z"),
            endAt = Instant.parse("2026-06-02T15:00:00Z"),
        )

        val visible = visibleBlocksForDay(
            blocks = listOf(block),
            day = LocalDate.of(2026, 6, 1),
            zoneId = zoneId,
        )

        assertEquals(emptyList<ScheduleBlock>(), visible)
    }

    @Test
    fun `active timeframes for day returns overlapping ranges in sorted order`() {
        val timeframes = listOf(
            timeframe(id = "tf-2", name = "Exams", startDate = LocalDate.of(2026, 6, 1), endDate = LocalDate.of(2026, 6, 5)),
            timeframe(id = "tf-1", name = "Sprint", startDate = LocalDate.of(2026, 5, 31), endDate = LocalDate.of(2026, 6, 3)),
            timeframe(id = "tf-3", name = "Later", startDate = LocalDate.of(2026, 6, 7), endDate = LocalDate.of(2026, 6, 9)),
        )

        val active = activeTimeframesForDay(timeframes, LocalDate.of(2026, 6, 2))

        assertEquals(listOf("tf-1", "tf-2"), active.map { it.id })
    }

    @Test
    fun `build task day section counts unique tasks reminders and timeframes`() {
        val zoneId = ZoneId.of("America/New_York")
        val date = LocalDate.of(2026, 6, 2)
        val taskOne = scheduleTask("task-1", "Deep Work")
        val taskTwo = scheduleTask("task-2", "Review")
        val tasksById = listOf(taskOne, taskTwo).associateBy { it.id }
        val blocks = listOf(
            scheduleBlock(
                id = "block-1",
                taskId = taskOne.id,
                startAt = Instant.parse("2026-06-02T14:00:00Z"),
                endAt = Instant.parse("2026-06-02T15:00:00Z"),
            ),
            scheduleBlock(
                id = "block-2",
                taskId = taskOne.id,
                startAt = Instant.parse("2026-06-02T16:00:00Z"),
                endAt = Instant.parse("2026-06-02T17:00:00Z"),
            ),
            scheduleBlock(
                id = "block-3",
                taskId = taskTwo.id,
                startAt = Instant.parse("2026-06-02T18:00:00Z"),
                endAt = Instant.parse("2026-06-02T19:00:00Z"),
            ),
        )
        val reminders = listOf(
            reminder("rem-1", "Standup", Instant.parse("2026-06-02T15:00:00Z")),
            reminder("rem-2", "Tomorrow", Instant.parse("2026-06-03T15:00:00Z")),
        )
        val timeframes = listOf(
            timeframe(id = "tf-1", name = "Sprint", startDate = LocalDate.of(2026, 6, 1), endDate = LocalDate.of(2026, 6, 3)),
            timeframe(id = "tf-2", name = "Later", startDate = LocalDate.of(2026, 6, 4), endDate = LocalDate.of(2026, 6, 7)),
        )

        val section = buildTaskDaySection(
            date = date,
            blocks = blocks,
            tasksById = tasksById,
            reminders = reminders,
            timeframes = timeframes,
            zoneId = zoneId,
        )

        assertEquals(date, section.date)
        assertEquals(2, section.taskCount)
        assertEquals(1, section.reminderCount)
        assertEquals(listOf("Sprint"), section.timeframes.map { it.name })
        assertEquals(listOf("Deep Work", "Review"), section.tasks.map { it.title })
        assertEquals(listOf("Standup"), section.reminders.map { it.title })
    }

    @Test
    fun `tasks view mode defaults to expanded`() {
        assertEquals(TasksViewMode.EXPANDED, dev.codex.reclaimoss.settings.AppSettings().tasksViewMode)
    }

    @Test
    fun `timeframe rail metadata is continuous across day boundaries`() {
        val current = timeframe(id = "tf-1", name = "Sprint", startDate = LocalDate.of(2026, 6, 1), endDate = LocalDate.of(2026, 6, 3))
        val next = timeframe(id = "tf-1", name = "Sprint", startDate = LocalDate.of(2026, 6, 1), endDate = LocalDate.of(2026, 6, 3))

        val metadata = buildTimeframeRailMetadata(
            currentDayTimeframes = listOf(current),
            nextDayTimeframes = listOf(next),
        )

        assertEquals(1, metadata.size)
        assertTrue(metadata.single().continuesIntoNextDay)
    }

    @Test
    fun `timeframe rail metadata keeps parallel rails touching in order`() {
        val sprint = timeframe(id = "tf-1", name = "Sprint", startDate = LocalDate.of(2026, 6, 1), endDate = LocalDate.of(2026, 6, 5))
        val finals = timeframe(id = "tf-2", name = "Finals", startDate = LocalDate.of(2026, 6, 2), endDate = LocalDate.of(2026, 6, 4))

        val metadata = buildTimeframeRailMetadata(
            currentDayTimeframes = listOf(sprint, finals),
            nextDayTimeframes = emptyList(),
        )

        assertEquals(listOf("tf-1", "tf-2"), metadata.map { it.id })
        assertEquals(listOf(0, 1), metadata.map { it.laneIndex })
    }

    @Test
    fun `sticky timeframe header text joins active names in a single line`() {
        val metadata = listOf(
            TimeframeRailMetadata(
                id = "tf-1",
                name = "Sprint",
                colorHex = "#F4B6D2",
                laneIndex = 0,
                continuesFromPreviousDay = false,
                continuesIntoNextDay = true,
            ),
            TimeframeRailMetadata(
                id = "tf-2",
                name = "Finals",
                colorHex = "#9BCB72",
                laneIndex = 1,
                continuesFromPreviousDay = false,
                continuesIntoNextDay = false,
            ),
        )

        assertEquals("Sprint · Finals", stickyTimeframeHeaderNames(metadata))
    }

    @Test
    fun `sticky timeframe header text disappears when no rails are active`() {
        assertEquals(null, stickyTimeframeHeaderNames(emptyList()))
    }

    private fun scheduleBlock(
        id: String = "block-1",
        taskId: String = "task-1",
        startAt: Instant,
        endAt: Instant,
    ) = ScheduleBlock(
        id = id,
        taskId = taskId,
        startAt = startAt,
        endAt = endAt,
        source = BlockSource.AUTO,
        lockState = BlockLockState.FLEXIBLE,
        completionState = BlockCompletionState.PENDING,
        externalCalendarEventId = null,
    )

    private fun scheduleTask(
        id: String,
        title: String,
    ) = ScheduleTask(
        id = id,
        title = title,
        dueAt = Instant.parse("2026-06-03T17:00:00Z"),
        estimatedMinutes = 60,
        remainingMinutes = 60,
        priority = TaskPriority.MEDIUM,
        status = TaskStatus.ACTIVE,
        recurrenceRule = RecurrenceRule(),
    )

    private fun reminder(
        id: String,
        title: String,
        dueAt: Instant,
    ) = Reminder(
        id = id,
        title = title,
        dueAt = dueAt,
    )

    private fun timeframe(
        id: String,
        name: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ) = Timeframe(
        id = id,
        name = name,
        startDate = startDate,
        endDate = endDate,
        colorHex = "#F4B6D2",
    )
}
