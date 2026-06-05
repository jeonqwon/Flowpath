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
    fun `compact sticky date text uses day slash month`() {
        assertEquals("2/6", compactStickyDateText(LocalDate.of(2026, 6, 2)))
    }

    @Test
    fun `timeline shows hour 0 label`() {
        assertEquals(true, shouldShowTimelineHourLabel(0))
    }

    @Test
    fun `timeline hides hour 24 label`() {
        assertEquals(false, shouldShowTimelineHourLabel(24))
    }

    @Test
    fun `timeline shows hour 1 label`() {
        assertEquals(true, shouldShowTimelineHourLabel(1))
    }

    @Test
    fun `timeline dividers always show for hours 0 through 24`() {
        assertEquals(true, shouldShowTimelineHourDivider(0))
        assertEquals(true, shouldShowTimelineHourDivider(24))
        assertEquals(true, shouldShowTimelineHourDivider(1))
        assertEquals(true, shouldShowTimelineHourDivider(12))
        assertEquals(false, shouldShowTimelineHourDivider(-1))
        assertEquals(false, shouldShowTimelineHourDivider(25))
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
                startDate = LocalDate.of(2026, 5, 18),
                laneIndex = 0,
                continuesFromPreviousDay = false,
                continuesIntoNextDay = true,
            ),
            TimeframeRailMetadata(
                id = "tf-2",
                name = "Finals",
                colorHex = "#9BCB72",
                startDate = LocalDate.of(2026, 5, 18),
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

    @Test
    fun `sticky timeframe card text joins active names in one card`() {
        val metadata = listOf(
            TimeframeRailMetadata(
                id = "tf-1",
                name = "Sprint",
                colorHex = "#F4B6D2",
                startDate = LocalDate.of(2026, 5, 18),
                laneIndex = 0,
                continuesFromPreviousDay = false,
                continuesIntoNextDay = true,
            ),
            TimeframeRailMetadata(
                id = "tf-2",
                name = "Finals",
                colorHex = "#9BCB72",
                startDate = LocalDate.of(2026, 5, 18),
                laneIndex = 1,
                continuesFromPreviousDay = false,
                continuesIntoNextDay = false,
            ),
        )

        assertEquals("Sprint · Finals", stickyTimeframeCardText(metadata))
    }

    @Test
    fun `task segment continuity marks overnight joins across midnight`() {
        val zoneId = ZoneId.of("America/New_York")
        val block = scheduleBlock(
            startAt = Instant.parse("2026-06-02T02:00:00Z"),
            endAt = Instant.parse("2026-06-02T10:00:00Z"),
        )

        val firstDay = visibleTaskSegmentsForDay(
            blocks = listOf(block),
            day = LocalDate.of(2026, 6, 1),
            zoneId = zoneId,
        ).single()
        val secondDay = visibleTaskSegmentsForDay(
            blocks = listOf(block),
            day = LocalDate.of(2026, 6, 2),
            zoneId = zoneId,
        ).single()

        assertEquals(false, firstDay.continuesFromPreviousDay)
        assertEquals(true, firstDay.continuesIntoNextDay)
        assertEquals(true, secondDay.continuesFromPreviousDay)
        assertEquals(false, secondDay.continuesIntoNextDay)
    }

    @Test
    fun `expanded task segments split overnight block across both days`() {
        val zoneId = ZoneId.of("America/New_York")
        val block = scheduleBlock(
            startAt = Instant.parse("2026-06-02T02:00:00Z"),
            endAt = Instant.parse("2026-06-02T10:00:00Z"),
        )

        val firstDay = expandedTaskSegmentsForDay(
            blocks = listOf(block),
            day = LocalDate.of(2026, 6, 1),
            zoneId = zoneId,
        )
        val secondDay = expandedTaskSegmentsForDay(
            blocks = listOf(block),
            day = LocalDate.of(2026, 6, 2),
            zoneId = zoneId,
        )

        assertEquals(1, firstDay.size)
        assertEquals(true, firstDay.single().continuesIntoNextDay)
        assertEquals("2026-06-02T02:00:00Z", firstDay.single().block.startAt.toString())
        assertEquals("2026-06-02T04:00:00Z", firstDay.single().block.endAt.toString())

        assertEquals(1, secondDay.size)
        assertEquals(true, secondDay.single().continuesFromPreviousDay)
        assertEquals("2026-06-02T04:00:00Z", secondDay.single().block.startAt.toString())
        assertEquals("2026-06-02T10:00:00Z", secondDay.single().block.endAt.toString())
    }

    @Test
    fun `sticky timeframe header labels preserve active order with colors`() {
        val metadata = listOf(
            TimeframeRailMetadata(
                id = "tf-1",
                name = "Sprint",
                colorHex = "#F4B6D2",
                startDate = LocalDate.of(2026, 5, 18),
                laneIndex = 0,
                continuesFromPreviousDay = false,
                continuesIntoNextDay = true,
            ),
            TimeframeRailMetadata(
                id = "tf-2",
                name = "Finals",
                colorHex = "#9BCB72",
                startDate = LocalDate.of(2026, 5, 18),
                laneIndex = 1,
                continuesFromPreviousDay = false,
                continuesIntoNextDay = false,
            ),
        )

        assertEquals(
            listOf(
                TimeframeHeaderLabel(name = "Sprint", colorHex = "#F4B6D2"),
                TimeframeHeaderLabel(name = "Finals", colorHex = "#9BCB72"),
            ),
            stickyTimeframeHeaderLabels(metadata),
        )
    }

    @Test
    fun `sticky timeframe header labels de duplicate repeated timeframe ids`() {
        val metadata = listOf(
            TimeframeRailMetadata(
                id = "tf-1",
                name = "Sprint",
                colorHex = "#F4B6D2",
                startDate = LocalDate.of(2026, 5, 18),
                laneIndex = 0,
                continuesFromPreviousDay = true,
                continuesIntoNextDay = true,
            ),
            TimeframeRailMetadata(
                id = "tf-1",
                name = "Sprint",
                colorHex = "#F4B6D2",
                startDate = LocalDate.of(2026, 5, 18),
                laneIndex = 1,
                continuesFromPreviousDay = true,
                continuesIntoNextDay = true,
            ),
        )

        assertEquals(
            listOf(TimeframeHeaderLabel(name = "Sprint", colorHex = "#F4B6D2")),
            stickyTimeframeHeaderLabels(metadata),
        )
    }

    @Test
    fun `expanded header handoff moves forward into next day`() {
        val transition = resolveExpandedHeaderTransition(
            scrollPx = 940,
            dayHeightPx = 1000,
            maxIndex = 20,
        )

        assertEquals(0, transition.pinnedDayIndex)
        assertEquals(1, transition.incomingDayIndex)
        assertEquals(0.4f, transition.progress, 0.001f)
    }

    @Test
    fun `expanded header handoff mirrors when reversed before boundary`() {
        val transition = resolveExpandedHeaderTransition(
            scrollPx = 999,
            dayHeightPx = 1000,
            maxIndex = 20,
        )

        assertEquals(0, transition.pinnedDayIndex)
        assertEquals(1, transition.incomingDayIndex)
        assertEquals(0.99f, transition.progress, 0.001f)
    }

    @Test
    fun `expanded header handoff snaps after transition completes`() {
        val transition = resolveExpandedHeaderTransition(
            scrollPx = 1000,
            dayHeightPx = 1000,
            maxIndex = 20,
        )

        assertEquals(1, transition.pinnedDayIndex)
        assertEquals(null, transition.incomingDayIndex)
        assertEquals(0f, transition.progress, 0.001f)
    }

    @Test
    fun `expanded date chip stays in timestamp gutter before reaching sticky bar`() {
        assertEquals(
            120,
            resolveExpandedDateChipY(
                bodyOffsetPx = 120,
                nextBodyOffsetPx = 1000,
                stickyYPx = 10,
                chipHeightPx = 30,
            ),
        )
    }

    @Test
    fun `expanded date chip pins at sticky bar after boundary scrolls past`() {
        assertEquals(
            10,
            resolveExpandedDateChipY(
                bodyOffsetPx = -400,
                nextBodyOffsetPx = 1000,
                stickyYPx = 10,
                chipHeightPx = 30,
            ),
        )
    }

    @Test
    fun `expanded date chip is pushed away by next boundary chip`() {
        assertEquals(
            -5,
            resolveExpandedDateChipY(
                bodyOffsetPx = -900,
                nextBodyOffsetPx = 25,
                stickyYPx = 10,
                chipHeightPx = 30,
            ),
        )
    }

    @Test
    fun `continuing timeframe placement stays vertically pinned`() {
        val placements = buildTimeframeChipPlacements(
            currentRails = listOf(testRail("tf-1", "Sprint")),
            incomingRails = listOf(testRail("tf-1", "Sprint")),
            progress = 0.75f,
        )

        assertEquals(StickyHeaderTimeframeChipMotion.PINNED, placements.single().motion)
        assertEquals(0, placements.single().fromSlot)
        assertEquals(0, placements.single().toSlot)
        assertEquals(0.75f, placements.single().progress, 0.001f)
    }

    @Test
    fun `ending timeframe placement exits upward`() {
        val placements = buildTimeframeChipPlacements(
            currentRails = listOf(testRail("tf-1", "Sprint")),
            incomingRails = emptyList(),
            progress = 0.5f,
        )

        assertEquals(StickyHeaderTimeframeChipMotion.EXITING, placements.single().motion)
        assertEquals(0, placements.single().fromSlot)
        assertEquals(0, placements.single().toSlot)
        assertEquals(0.5f, placements.single().progress, 0.001f)
    }

    @Test
    fun `starting timeframe placement enters from below`() {
        val placements = buildTimeframeChipPlacements(
            currentRails = emptyList(),
            incomingRails = listOf(testRail("tf-2", "Exams")),
            progress = 0.25f,
        )

        assertEquals(
            TimeframeChipPlacement(
                id = "tf-2",
                name = "Exams",
                colorHex = "#F4B6D2",
                motion = StickyHeaderTimeframeChipMotion.ENTERING,
                fromSlot = 0,
                toSlot = 0,
                progress = 0.25f,
            ),
            placements.single(),
        )
    }

    @Test
    fun `continuing timeframe shifts left when an earlier timeframe ends`() {
        val current = listOf(
            testRail("tf-1", "Sprint"),
            testRail("tf-2", "Finals"),
            testRail("tf-3", "Reading"),
        )
        val incoming = listOf(
            testRail("tf-1", "Sprint"),
            testRail("tf-3", "Reading"),
        )

        val placements = buildTimeframeChipPlacements(
            currentRails = current,
            incomingRails = incoming,
            progress = 0.5f,
        )

        assertEquals(
            TimeframeChipPlacement(
                id = "tf-3",
                name = "Reading",
                colorHex = "#F4B6D2",
                motion = StickyHeaderTimeframeChipMotion.PINNED,
                fromSlot = 2,
                toSlot = 1,
                progress = 0.5f,
            ),
            placements.single { it.id == "tf-3" },
        )
    }

    private fun testRail(
        id: String,
        name: String,
    ) = TimeframeRailMetadata(
        id = id,
        name = name,
        colorHex = "#F4B6D2",
        startDate = LocalDate.of(2026, 5, 18),
        laneIndex = 0,
        continuesFromPreviousDay = true,
        continuesIntoNextDay = true,
    )

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
