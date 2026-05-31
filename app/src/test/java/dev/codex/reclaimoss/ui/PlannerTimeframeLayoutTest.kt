package dev.codex.reclaimoss.ui

import dev.codex.reclaimoss.domain.model.Timeframe
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlannerTimeframeLayoutTest {
    @Test
    fun `non overlapping short spans reuse smallest lane while long span stays outer`() {
        val weeks = listOf(
            listOf(
                LocalDate.of(2026, 5, 24),
                LocalDate.of(2026, 5, 25),
                LocalDate.of(2026, 5, 26),
                LocalDate.of(2026, 5, 27),
                LocalDate.of(2026, 5, 28),
                LocalDate.of(2026, 5, 29),
                LocalDate.of(2026, 5, 30),
            ),
        )
        val createdAt = Instant.parse("2026-05-01T00:00:00Z")
        val yellow = timeframe("yellow", "Yellow", LocalDate.of(2026, 5, 24), LocalDate.of(2026, 5, 30), createdAt)
        val pink = timeframe("pink", "Pink", LocalDate.of(2026, 5, 27), LocalDate.of(2026, 5, 27), createdAt.plusSeconds(60))
        val cyan = timeframe("cyan", "Cyan", LocalDate.of(2026, 5, 30), LocalDate.of(2026, 5, 30), createdAt.plusSeconds(120))

        val specs = buildTimeframeRowDrawSpecs(
            weeks = weeks,
            timeframes = listOf(yellow, pink, cyan),
            cellWidthPx = 100f,
            rowHeightPx = 64f,
            strokeWidthPx = 4f,
            baseInsetPx = 10f,
            edgeOverhangPx = 6f,
        )

        assertEquals(1, specs.single { it.timeframeId == "yellow" }.lane)
        assertEquals(0, specs.single { it.timeframeId == "pink" }.lane)
        assertEquals(0, specs.single { it.timeframeId == "cyan" }.lane)
    }

    @Test
    fun `outer lane expands evenly around inner lane`() {
        val weeks = listOf(
            listOf(
                LocalDate.of(2026, 5, 24),
                LocalDate.of(2026, 5, 25),
                LocalDate.of(2026, 5, 26),
                LocalDate.of(2026, 5, 27),
                LocalDate.of(2026, 5, 28),
                LocalDate.of(2026, 5, 29),
                LocalDate.of(2026, 5, 30),
            ),
        )
        val outer = timeframe("outer", "Outer", LocalDate.of(2026, 5, 24), LocalDate.of(2026, 5, 30))
        val inner = timeframe("inner", "Inner", LocalDate.of(2026, 5, 27), LocalDate.of(2026, 5, 27))
        val strokeWidthPx = 4f

        val specs = buildTimeframeRowDrawSpecs(
            weeks = weeks,
            timeframes = listOf(outer, inner),
            cellWidthPx = 100f,
            rowHeightPx = 64f,
            strokeWidthPx = strokeWidthPx,
            baseInsetPx = 10f,
            edgeOverhangPx = 0f,
        )

        val outerSpec = specs.single { it.timeframeId == "outer" }
        val innerSpec = specs.single { it.timeframeId == "inner" }
        assertEquals(1, outerSpec.lane)
        assertEquals(0, innerSpec.lane)
        assertEquals(strokeWidthPx, innerSpec.top - outerSpec.top, 0.001f)
        assertEquals(strokeWidthPx, outerSpec.bottom - innerSpec.bottom, 0.001f)
        assertEquals(strokeWidthPx, outerSpec.radius - innerSpec.radius, 0.001f)
        assertTrue(outerSpec.left < innerSpec.left)
        assertTrue(outerSpec.right > innerSpec.right)
    }

    @Test
    fun `timeframe spanning multiple rows emits one segment per visible row`() {
        val weeks = listOf(
            listOf(
                LocalDate.of(2026, 5, 24),
                LocalDate.of(2026, 5, 25),
                LocalDate.of(2026, 5, 26),
                LocalDate.of(2026, 5, 27),
                LocalDate.of(2026, 5, 28),
                LocalDate.of(2026, 5, 29),
                LocalDate.of(2026, 5, 30),
            ),
            listOf(
                LocalDate.of(2026, 5, 31),
                null,
                null,
                null,
                null,
                null,
                null,
            ),
        )

        val specs = buildTimeframeRowDrawSpecs(
            weeks = weeks,
            timeframes = listOf(timeframe("cyan", "Cyan", LocalDate.of(2026, 5, 30), LocalDate.of(2026, 5, 31))),
            cellWidthPx = 100f,
            rowHeightPx = 64f,
            strokeWidthPx = 4f,
            baseInsetPx = 10f,
            edgeOverhangPx = 6f,
        )

        assertEquals(2, specs.size)
        assertEquals(0, specs[0].rowIndex)
        assertEquals(6, specs[0].startColumn)
        assertEquals(6, specs[0].endColumn)
        assertEquals(1, specs[1].rowIndex)
        assertEquals(0, specs[1].startColumn)
        assertEquals(0, specs[1].endColumn)
        assertEquals(4f, specs[1].left, 0.001f)
        assertEquals(96f, specs[1].right, 0.001f)
    }

    private fun timeframe(
        id: String,
        name: String,
        startDate: LocalDate,
        endDate: LocalDate,
        createdAt: Instant = Instant.parse("2026-05-01T00:00:00Z"),
    ) = Timeframe(
        id = id,
        name = name,
        startDate = startDate,
        endDate = endDate,
        colorHex = "#FF88AA",
        createdAt = createdAt,
        updatedAt = createdAt,
    )
}
