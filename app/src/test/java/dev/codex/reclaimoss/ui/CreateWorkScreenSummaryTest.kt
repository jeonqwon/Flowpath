package dev.codex.reclaimoss.ui

import dev.codex.reclaimoss.domain.model.RecurrenceType
import dev.codex.reclaimoss.domain.model.TaskContinuationMode
import dev.codex.reclaimoss.domain.model.TaskOverlapPolicy
import dev.codex.reclaimoss.domain.model.TaskPriority
import dev.codex.reclaimoss.domain.model.TaskSchedulingMode
import dev.codex.reclaimoss.domain.model.ScheduleTask
import dev.codex.reclaimoss.domain.model.Timeframe
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateWorkScreenSummaryTest {

    @Test
    fun `duration wheel selection clamps below minimum`() {
        val minutes = durationFromWheelSelection(
            selectedHours = 0,
            selectedMinute = 0,
            minMinutes = 15,
            maxMinutes = 360,
        )

        assertEquals(15, minutes)
    }

    @Test
    fun `duration wheel selection clamps above maximum`() {
        val minutes = durationFromWheelSelection(
            selectedHours = 6,
            selectedMinute = 45,
            minMinutes = 15,
            maxMinutes = 360,
        )

        assertEquals(360, minutes)
    }

    @Test
    fun `duration wheel state snaps current duration into hours and minutes`() {
        val state = durationWheelState(
            minutes = 135,
            minMinutes = 15,
            maxMinutes = 360,
        )

        assertEquals(2, state.selectedHours)
        assertEquals(15, state.selectedMinute)
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6), state.hourOptions)
        assertEquals(listOf(0, 15, 30, 45), state.minuteOptions)
    }

    @Test
    fun `task schedule summary shows no deadline flexible tasks compactly`() {
        val summary = withLocale(Locale.US) {
            taskScheduleSummary(
                TaskDraft(
                    hasDeadline = false,
                    estimatedMinutes = 60,
                    schedulingMode = TaskSchedulingMode.FLEXIBLE,
                ),
            )
        }

        assertEquals("Flexible | No deadline | 1h", summary)
    }

    @Test
    fun `task schedule row summary shows no deadline flexible tasks cleanly`() {
        val summary = withLocale(Locale.US) {
            taskScheduleRowSummary(
                TaskDraft(
                    hasDeadline = false,
                    schedulingMode = TaskSchedulingMode.FLEXIBLE,
                ),
            )
        }

        assertEquals("Flexible, No deadline", summary)
    }

    @Test
    fun `default create task draft starts as flexible no deadline`() {
        val draft = defaultCreateTaskDraft(defaultTaskReminder = true)

        assertEquals(TaskSchedulingMode.FLEXIBLE, draft.schedulingMode)
        assertEquals(false, draft.hasDeadline)
        assertEquals(true, draft.addReminder)
    }

    @Test
    fun `applying schedule editor only copies schedule fields`() {
        val base = TaskDraft(
            title = "Essay",
            description = "Draft chapter",
            priority = TaskPriority.URGENT,
            timeframeId = "tf-1",
            overlapPolicy = TaskOverlapPolicy.DISALLOW,
            continuationParentTaskId = "task-2",
            continuationMode = TaskContinuationMode.AFTER_PARENT_DUE_AT,
            addReminder = true,
        )
        val editedSchedule = TaskDraft(
            title = "Ignored",
            description = "Ignored",
            priority = TaskPriority.MEDIUM,
            schedulingMode = TaskSchedulingMode.FIXED_EXACT,
            hasDeadline = true,
            deadline = LocalDateTime.of(2026, 6, 3, 13, 0),
            startDate = LocalDate.of(2026, 6, 2),
            fixedDate = LocalDate.of(2026, 6, 3),
            fixedStartAt = LocalDateTime.of(2026, 6, 3, 12, 0),
            fixedEndAt = LocalDateTime.of(2026, 6, 3, 13, 0),
        )

        val updated = base.applyScheduleEditor(editedSchedule)

        assertEquals("Essay", updated.title)
        assertEquals(TaskPriority.URGENT, updated.priority)
        assertEquals("tf-1", updated.timeframeId)
        assertEquals(TaskSchedulingMode.FIXED_EXACT, updated.schedulingMode)
        assertEquals(LocalDateTime.of(2026, 6, 3, 13, 0), updated.deadline)
        assertEquals(LocalDateTime.of(2026, 6, 3, 12, 0), updated.fixedStartAt)
    }

    @Test
    fun `task window row summary formats overnight windows compactly`() {
        val summary = withLocale(Locale.US) {
            taskWindowSummary(
                TaskDraft(
                    hasWindow = true,
                    schedulingMode = TaskSchedulingMode.FLEXIBLE,
                    fixedStartAt = LocalDateTime.of(2026, 6, 3, 22, 0),
                    fixedEndAt = LocalDateTime.of(2026, 6, 4, 2, 0),
                ),
            )
        }

        assertEquals("10:00 PM-2:00 AM", summary)
    }

    @Test
    fun `applying window editor copies timing fields including timeframe`() {
        val base = TaskDraft(
            title = "Read",
            schedulingMode = TaskSchedulingMode.FIXED_DAY,
            fixedDate = LocalDate.of(2026, 6, 6),
            timeframeId = "tf-2",
        )
        val editedWindow = TaskDraft(
            title = "Ignored",
            hasWindow = true,
            schedulingMode = TaskSchedulingMode.FLEXIBLE,
            fixedDate = LocalDate.of(2026, 6, 10),
            timeframeId = "tf-3",
            fixedStartAt = LocalDateTime.of(2026, 6, 6, 18, 0),
            fixedEndAt = LocalDateTime.of(2026, 6, 7, 1, 0),
        )

        val updated = base.applyWindowEditor(editedWindow)

        assertEquals("Read", updated.title)
        assertEquals(TaskSchedulingMode.FIXED_DAY, updated.schedulingMode)
        assertEquals(LocalDate.of(2026, 6, 6), updated.fixedDate)
        assertEquals(LocalDateTime.of(2026, 6, 6, 18, 0), updated.fixedStartAt)
        assertEquals(LocalDateTime.of(2026, 6, 7, 1, 0), updated.fixedEndAt)
        assertEquals("tf-3", updated.timeframeId)
    }

    @Test
    fun `window slider state keeps overnight as a separate flag on a 24 hour range`() {
        val state = windowSliderState(
            start = LocalTime.of(22, 0),
            end = LocalTime.of(2, 0),
            overnight = true,
        )

        assertEquals(22 * 60f, state.startMinutes)
        assertEquals(2 * 60f, state.endMinutes)
        assertEquals(true, state.endsNextDay)
    }

    @Test
    fun `task repeat summary reflects weekly recurrence`() {
        val summary = withLocale(Locale.US) {
            taskRepeatSummary(
                TaskDraft(
                    recurrenceType = RecurrenceType.WEEKLY,
                    recurrenceInterval = 1,
                    recurrenceDays = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
                ),
            )
        }

        assertTrue(summary.startsWith("Every 1 week: "))
        assertTrue(summary.contains("Tue"))
        assertTrue(summary.contains("Thu"))
    }

    @Test
    fun `task repeat summary uses deadline as end for recurring deadline tasks`() {
        val summary = withLocale(Locale.US) {
            taskRepeatSummary(
                TaskDraft(
                    hasDeadline = true,
                    repeatsForever = true,
                    recurrenceType = RecurrenceType.WEEKLY,
                    recurrenceInterval = 1,
                    recurrenceDays = setOf(DayOfWeek.TUESDAY),
                    deadline = LocalDateTime.of(2026, 6, 2, 17, 0),
                ),
            )
        }

        assertTrue(summary.contains("until Jun 2"))
    }

    @Test
    fun `task repeat summary keeps forever recurrence for no deadline tasks`() {
        val summary = withLocale(Locale.US) {
            taskRepeatSummary(
                TaskDraft(
                    hasDeadline = false,
                    repeatsForever = true,
                    recurrenceType = RecurrenceType.WEEKLY,
                    recurrenceInterval = 1,
                    recurrenceDays = setOf(DayOfWeek.TUESDAY),
                ),
            )
        }

        assertTrue(summary.startsWith("Every 1 week: "))
        assertTrue(!summary.contains("until "))
    }

    @Test
    fun `task timing summary combines window and timeframe`() {
        val summary = withLocale(Locale.US) {
            taskTimingSummary(
                taskDraft = TaskDraft(
                    hasWindow = true,
                    fixedStartAt = LocalDateTime.of(2026, 6, 3, 18, 0),
                    fixedEndAt = LocalDateTime.of(2026, 6, 3, 21, 0),
                    timeframeId = "timeframe-1",
                ),
                timeframes = listOf(
                    Timeframe(
                        id = "timeframe-1",
                        name = "Finals",
                        startDate = LocalDate.of(2026, 6, 1),
                        endDate = LocalDate.of(2026, 6, 7),
                        colorHex = "#F4B6D2",
                    ),
                ),
            )
        }

        assertEquals("6:00 PM-9:00 PM, Finals", summary)
    }

    @Test
    fun `task rules summary lists active optional rules without timeframe`() {
        val summary = taskRulesSummary(
            taskDraft = TaskDraft(
                continuationParentTaskId = "task-2",
                continuationMode = TaskContinuationMode.AFTER_PARENT_SCHEDULED_END,
                overlapPolicy = TaskOverlapPolicy.DISALLOW,
                addReminder = true,
                priority = TaskPriority.URGENT,
            ),
        )

        assertEquals("Dependency, No overlap, Reminder, Urgent", summary)
    }

    @Test
    fun `dependency picker groups tasks by their start day`() {
        val zone = ZoneId.of("America/New_York")
        val june3 = LocalDate.of(2026, 6, 3)
        val june4 = LocalDate.of(2026, 6, 4)
        val tasks = listOf(
            dependencyTask(
                id = "a",
                title = "Morning class",
                start = LocalDateTime.of(2026, 6, 3, 9, 0),
            ),
            dependencyTask(
                id = "b",
                title = "Essay block",
                start = LocalDateTime.of(2026, 6, 3, 18, 0),
            ),
            dependencyTask(
                id = "c",
                title = "Gym",
                start = LocalDateTime.of(2026, 6, 4, 7, 30),
            ),
        )

        assertEquals(listOf(june3, june4), dependencyDateOptions(tasks, zone))
        assertEquals(
            listOf("Morning class", "Essay block"),
            tasksForDependencyDate(tasks, june3, zone).map { it.title },
        )
        assertEquals(june4, initialDependencyDate(tasks, "c", zone))
        assertEquals(june3, initialDependencyDate(tasks, null, zone))
    }

    @Test
    fun `dependency mode labels stay concise and clearer`() {
        assertEquals("After task ends", TaskContinuationMode.AFTER_PARENT_SCHEDULED_END.labelForDependency())
        assertEquals("After deadline", TaskContinuationMode.AFTER_PARENT_DUE_AT.labelForDependency())
        assertEquals("Before task starts", TaskContinuationMode.BEFORE_PARENT_START.labelForDependency())
    }

    private fun dependencyTask(
        id: String,
        title: String,
        start: LocalDateTime,
    ): ScheduleTask = ScheduleTask(
        id = id,
        title = title,
        priority = TaskPriority.MEDIUM,
        dueAt = start.plusHours(1).atZone(ZoneId.of("America/New_York")).toInstant(),
        estimatedMinutes = 60,
        remainingMinutes = 60,
        schedulingMode = TaskSchedulingMode.FIXED_EXACT,
        fixedStartAt = start.atZone(ZoneId.of("America/New_York")).toInstant(),
        fixedEndAt = start.plusHours(1).atZone(ZoneId.of("America/New_York")).toInstant(),
    )

    @Test
    fun `reminder summary shows due time and repeat state`() {
        val summary = withLocale(Locale.US) {
            reminderSummary(
                ReminderDraft(
                    dueAt = LocalDateTime.of(2026, 6, 2, 18, 0),
                    recurrenceType = RecurrenceType.NONE,
                ),
            )
        }

        assertEquals("Tue, Jun 2 6:00 PM | Once", summary)
    }

    private fun <T> withLocale(locale: Locale, block: () -> T): T {
        val previous = Locale.getDefault()
        Locale.setDefault(locale)
        return try {
            block()
        } finally {
            Locale.setDefault(previous)
        }
    }
}
