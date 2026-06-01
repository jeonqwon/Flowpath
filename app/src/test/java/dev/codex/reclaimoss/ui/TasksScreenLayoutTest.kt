package dev.codex.reclaimoss.ui

import dev.codex.reclaimoss.domain.model.BlockCompletionState
import dev.codex.reclaimoss.domain.model.BlockLockState
import dev.codex.reclaimoss.domain.model.BlockSource
import dev.codex.reclaimoss.domain.model.ScheduleBlock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
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

    private fun scheduleBlock(
        startAt: Instant,
        endAt: Instant,
    ) = ScheduleBlock(
        id = "block-1",
        taskId = "task-1",
        startAt = startAt,
        endAt = endAt,
        source = BlockSource.AUTO,
        lockState = BlockLockState.FLEXIBLE,
        completionState = BlockCompletionState.PENDING,
        externalCalendarEventId = null,
    )
}
