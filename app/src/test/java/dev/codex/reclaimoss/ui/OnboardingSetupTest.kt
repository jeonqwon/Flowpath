package dev.codex.reclaimoss.ui

import java.time.DayOfWeek
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingSetupTest {
    @Test
    fun `covered sleep weekdays unions all configured entries`() {
        val entries = listOf(
            SleepOnboardingEntryDraft(
                weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY),
                windowStart = LocalTime.of(22, 0),
                windowEnd = LocalTime.of(7, 0),
                durationMinutes = 8 * 60,
            ),
            SleepOnboardingEntryDraft(
                weekdays = setOf(DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                windowStart = LocalTime.of(23, 0),
                windowEnd = LocalTime.of(8, 0),
                durationMinutes = 8 * 60,
            ),
        )

        assertEquals(
            setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
            ),
            coveredSleepWeekdays(entries),
        )
    }

    @Test
    fun `missing sleep weekdays reports remaining uncovered days`() {
        val entries = listOf(
            SleepOnboardingEntryDraft(
                weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            ),
            SleepOnboardingEntryDraft(
                weekdays = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
            ),
        )

        assertEquals(
            linkedSetOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
            missingSleepWeekdays(entries),
        )
    }

    @Test
    fun `missing sleep weekdays is empty once all seven days are covered`() {
        val entries = listOf(
            SleepOnboardingEntryDraft(
                weekdays = DayOfWeek.entries.toSet(),
                windowStart = LocalTime.of(22, 0),
                windowEnd = LocalTime.of(7, 0),
                durationMinutes = 8 * 60,
            ),
        )

        assertTrue(missingSleepWeekdays(entries).isEmpty())
    }

    @Test
    fun `unavailable sleep weekdays excludes the draft being edited`() {
        val entries = listOf(
            SleepOnboardingEntryDraft(
                weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
            ),
            SleepOnboardingEntryDraft(
                weekdays = setOf(DayOfWeek.FRIDAY),
            ),
        )

        assertEquals(
            linkedSetOf(DayOfWeek.FRIDAY),
            unavailableSleepWeekdays(
                entries = entries,
                selectedDays = entries.first().weekdays,
                editingIndex = 0,
            ),
        )
    }
}
