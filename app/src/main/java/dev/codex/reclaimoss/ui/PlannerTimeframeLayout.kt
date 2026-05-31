package dev.codex.reclaimoss.ui

import dev.codex.reclaimoss.domain.model.Timeframe
import java.time.LocalDate

internal data class TimeframeRowDrawSpec(
    val timeframeId: String,
    val rowIndex: Int,
    val startColumn: Int,
    val endColumn: Int,
    val lane: Int,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val radius: Float,
    val colorHex: String,
)

private data class TimeframeRowInterval(
    val timeframe: Timeframe,
    val rowIndex: Int,
    val startColumn: Int,
    val endColumn: Int,
) {
    val visibleLength: Int
        get() = endColumn - startColumn + 1
}

internal fun buildTimeframeRowDrawSpecs(
    weeks: List<List<LocalDate?>>,
    timeframes: List<Timeframe>,
    cellWidthPx: Float,
    rowHeightPx: Float,
    strokeWidthPx: Float,
    baseInsetPx: Float,
    edgeOverhangPx: Float,
): List<TimeframeRowDrawSpec> {
    if (weeks.isEmpty() || timeframes.isEmpty()) return emptyList()

    return buildList {
        weeks.forEachIndexed { rowIndex, row ->
            val visibleColumns = row.withIndex()
                .filter { it.value != null }
                .map { it.index }
            if (visibleColumns.isEmpty()) return@forEachIndexed
            val visibleFirstColumn = visibleColumns.first()
            val visibleLastColumn = visibleColumns.last()
            val baseCornerRadiusPx = (
                (rowHeightPx - (baseInsetPx * 2f)) / 4f
            ).coerceAtLeast(strokeWidthPx * 3f)

            val intervals = timeframes.mapNotNull { timeframe ->
                val matchingColumns = row.withIndex()
                    .filter { (_, date) -> date != null && !date.isBefore(timeframe.startDate) && !date.isAfter(timeframe.endDate) }
                    .map { it.index }
                if (matchingColumns.isEmpty()) {
                    null
                } else {
                    TimeframeRowInterval(
                        timeframe = timeframe,
                        rowIndex = rowIndex,
                        startColumn = matchingColumns.first(),
                        endColumn = matchingColumns.last(),
                    )
                }
            }
            if (intervals.isEmpty()) return@forEachIndexed

            buildIntervalClusters(intervals).forEach { cluster ->
                val laneAssignments = assignClusterLanes(cluster)
                cluster.forEach { interval ->
                    val lane = laneAssignments.getValue(interval.timeframe.id)
                    val laneOffset = strokeWidthPx * lane
                    val startOverhang = if (interval.startColumn == visibleFirstColumn) edgeOverhangPx else 0f
                    val endOverhang = if (interval.endColumn == visibleLastColumn) edgeOverhangPx else 0f
                    val left = interval.startColumn * cellWidthPx + baseInsetPx - laneOffset -
                        startOverhang
                    val right = (interval.endColumn + 1) * cellWidthPx - baseInsetPx + laneOffset +
                        endOverhang
                    val top = baseInsetPx - laneOffset
                    val bottom = rowHeightPx - baseInsetPx + laneOffset
                    add(
                        TimeframeRowDrawSpec(
                            timeframeId = interval.timeframe.id,
                            rowIndex = rowIndex,
                            startColumn = interval.startColumn,
                            endColumn = interval.endColumn,
                            lane = lane,
                            left = left,
                            top = top,
                            right = right,
                            bottom = bottom,
                            radius = baseCornerRadiusPx + laneOffset,
                            colorHex = interval.timeframe.colorHex,
                        ),
                    )
                }
            }
        }
    }.sortedWith(
        compareBy<TimeframeRowDrawSpec> { it.rowIndex }
            .thenByDescending { it.lane }
            .thenBy { it.startColumn },
    )
}

private fun buildIntervalClusters(intervals: List<TimeframeRowInterval>): List<List<TimeframeRowInterval>> {
    if (intervals.isEmpty()) return emptyList()
    val sorted = intervals.sortedWith(
        compareBy<TimeframeRowInterval> { it.startColumn }
            .thenBy { it.endColumn }
            .thenBy { it.timeframe.createdAt }
            .thenBy { it.timeframe.id },
    )
    val clusters = mutableListOf<MutableList<TimeframeRowInterval>>()
    var currentCluster = mutableListOf(sorted.first())
    var currentEnd = sorted.first().endColumn
    for (interval in sorted.drop(1)) {
        if (interval.startColumn <= currentEnd) {
            currentCluster += interval
            currentEnd = maxOf(currentEnd, interval.endColumn)
        } else {
            clusters += currentCluster
            currentCluster = mutableListOf(interval)
            currentEnd = interval.endColumn
        }
    }
    clusters += currentCluster
    return clusters
}

private fun assignClusterLanes(cluster: List<TimeframeRowInterval>): Map<String, Int> {
    val intervalsByLane = mutableMapOf<Int, MutableList<TimeframeRowInterval>>()
    val assignments = mutableMapOf<String, Int>()
    val ordered = cluster.sortedWith(
        compareBy<TimeframeRowInterval> { it.visibleLength }
            .thenBy { it.timeframe.createdAt }
            .thenBy { it.timeframe.id },
    )
    ordered.forEach { interval ->
        var lane = 0
        while (true) {
            val laneIntervals = intervalsByLane[lane].orEmpty()
            val overlapsExisting = laneIntervals.any { existing ->
                existing.startColumn <= interval.endColumn && interval.startColumn <= existing.endColumn
            }
            if (!overlapsExisting) break
            lane += 1
        }
        assignments[interval.timeframe.id] = lane
        intervalsByLane.getOrPut(lane) { mutableListOf() } += interval
    }
    return assignments
}
