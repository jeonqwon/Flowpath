package dev.codex.reclaimoss.ui

import dev.codex.reclaimoss.domain.model.TimePeriod
import dev.codex.reclaimoss.domain.model.TimePeriodType
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimePeriodValidationTest {
    @Test
    fun `overlap validation catches breakfast overlapping overnight sleep`() {
        val sleep = TimePeriod(
            id = "sleep",
            label = "Sleep",
            start = LocalTime.of(22, 0),
            end = LocalTime.of(7, 0),
            type = TimePeriodType.LIFE,
            sortOrder = 0,
        )
        val breakfast = TimePeriod(
            id = "breakfast",
            label = "Breakfast",
            start = LocalTime.of(6, 30),
            end = LocalTime.of(7, 30),
            type = TimePeriodType.LIFE,
            sortOrder = 1,
        )

        val overlap = findOverlappingTimePeriod(
            candidate = breakfast,
            periods = listOf(sleep),
        )

        assertEquals(sleep, overlap)
        assertEquals(
            "Breakfast overlaps with Sleep (10:00 pm - 7:00 am). Choose a different time.",
            timePeriodOverlapMessage(breakfast.label, sleep),
        )
    }

    @Test
    fun `overlap validation ignores the period being edited`() {
        val breakfast = TimePeriod(
            id = "breakfast",
            label = "Breakfast",
            start = LocalTime.of(8, 0),
            end = LocalTime.of(8, 30),
            type = TimePeriodType.LIFE,
            sortOrder = 1,
        )

        val overlap = findOverlappingTimePeriod(
            candidate = breakfast.copy(start = LocalTime.of(8, 15), end = LocalTime.of(8, 45)),
            periods = listOf(breakfast),
        )

        assertNull(overlap)
    }
}
