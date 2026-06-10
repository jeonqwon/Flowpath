package dev.codex.reclaimoss.ui

import dev.codex.reclaimoss.domain.model.RecurrenceEndMode
import dev.codex.reclaimoss.domain.model.RecurrenceRule
import dev.codex.reclaimoss.domain.model.RecurrenceType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskDetailRecurrencePreviewTest {

    private val zoneId = ZoneId.of("America/New_York")

    @Test
    fun `daily recurrence previews every interval in the visible month`() {
        val dates = recurrencePreviewDates(
            rule = RecurrenceRule(type = RecurrenceType.DAILY, interval = 2),
            dueAt = Instant.parse("2026-06-01T13:00:00Z"),
            zoneId = zoneId,
            visibleMonth = YearMonth.of(2026, 6),
        )

        assertEquals(
            listOf(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 3),
                LocalDate.of(2026, 6, 5),
                LocalDate.of(2026, 6, 7),
                LocalDate.of(2026, 6, 9),
                LocalDate.of(2026, 6, 11),
                LocalDate.of(2026, 6, 13),
                LocalDate.of(2026, 6, 15),
                LocalDate.of(2026, 6, 17),
                LocalDate.of(2026, 6, 19),
                LocalDate.of(2026, 6, 21),
                LocalDate.of(2026, 6, 23),
                LocalDate.of(2026, 6, 25),
                LocalDate.of(2026, 6, 27),
                LocalDate.of(2026, 6, 29),
            ),
            dates,
        )
    }

    @Test
    fun `weekly recurrence previews selected weekdays across a month boundary`() {
        val dates = recurrencePreviewDates(
            rule = RecurrenceRule(
                type = RecurrenceType.WEEKLY,
                interval = 1,
                daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
            ),
            dueAt = Instant.parse("2026-06-29T13:00:00Z"),
            zoneId = zoneId,
            visibleMonth = YearMonth.of(2026, 7),
        )

        assertEquals(
            listOf(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 6),
                LocalDate.of(2026, 7, 8),
                LocalDate.of(2026, 7, 13),
                LocalDate.of(2026, 7, 15),
                LocalDate.of(2026, 7, 20),
                LocalDate.of(2026, 7, 22),
                LocalDate.of(2026, 7, 27),
                LocalDate.of(2026, 7, 29),
            ),
            dates,
        )
    }

    @Test
    fun `weekly recurrence falls back to the due weekday when no weekdays are selected`() {
        val dates = recurrencePreviewDates(
            rule = RecurrenceRule(type = RecurrenceType.WEEKLY, interval = 1),
            dueAt = Instant.parse("2026-06-02T13:00:00Z"),
            zoneId = zoneId,
            visibleMonth = YearMonth.of(2026, 6),
        )

        assertEquals(
            listOf(
                LocalDate.of(2026, 6, 2),
                LocalDate.of(2026, 6, 9),
                LocalDate.of(2026, 6, 16),
                LocalDate.of(2026, 6, 23),
                LocalDate.of(2026, 6, 30),
            ),
            dates,
        )
    }

    @Test
    fun `monthly recurrence clamps to the last valid day of shorter months`() {
        val dates = recurrencePreviewDates(
            rule = RecurrenceRule(type = RecurrenceType.MONTHLY, interval = 1),
            dueAt = Instant.parse("2026-01-31T14:00:00Z"),
            zoneId = zoneId,
            visibleMonth = YearMonth.of(2026, 2),
        )

        assertEquals(listOf(LocalDate.of(2026, 2, 28)), dates)
    }

    @Test
    fun `until date stops highlighted preview dates`() {
        val dates = recurrencePreviewDates(
            rule = RecurrenceRule(
                type = RecurrenceType.DAILY,
                interval = 1,
                until = Instant.parse("2026-06-03T23:59:00Z"),
                endMode = RecurrenceEndMode.ON_DATE,
            ),
            dueAt = Instant.parse("2026-06-01T13:00:00Z"),
            zoneId = zoneId,
            visibleMonth = YearMonth.of(2026, 6),
        )

        assertEquals(
            listOf(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 2),
                LocalDate.of(2026, 6, 3),
            ),
            dates,
        )
    }

    @Test
    fun `occurrence count stops highlighted preview dates`() {
        val dates = recurrencePreviewDates(
            rule = RecurrenceRule(
                type = RecurrenceType.WEEKLY,
                interval = 1,
                daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                endMode = RecurrenceEndMode.AFTER_OCCURRENCES,
                occurrenceCount = 3,
            ),
            dueAt = Instant.parse("2026-06-01T13:00:00Z"),
            zoneId = zoneId,
            visibleMonth = YearMonth.of(2026, 6),
        )

        assertEquals(
            listOf(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 3),
                LocalDate.of(2026, 6, 8),
            ),
            dates,
        )
    }
}
