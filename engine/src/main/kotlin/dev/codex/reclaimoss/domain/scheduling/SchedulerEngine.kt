package dev.codex.reclaimoss.domain.scheduling

import dev.codex.reclaimoss.domain.model.BlockCompletionState
import dev.codex.reclaimoss.domain.model.BlockLockState
import dev.codex.reclaimoss.domain.model.BlockSource
import dev.codex.reclaimoss.domain.model.ScheduleBlock
import dev.codex.reclaimoss.domain.model.SchedulePlan
import dev.codex.reclaimoss.domain.model.ScheduleTask
import dev.codex.reclaimoss.domain.model.SchedulingIssue
import dev.codex.reclaimoss.domain.model.SchedulingIssueType
import dev.codex.reclaimoss.domain.model.SchedulingPolicy
import dev.codex.reclaimoss.domain.model.TaskContinuationMode
import dev.codex.reclaimoss.domain.model.TaskSchedulingMode
import dev.codex.reclaimoss.domain.model.TaskStatus
import dev.codex.reclaimoss.domain.model.TimePeriod
import dev.codex.reclaimoss.domain.model.WorkHoursProfile
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min

sealed interface ScheduleRebuildReason {
    data object ManualRebuild : ScheduleRebuildReason
    data class TaskMissed(val taskId: String) : ScheduleRebuildReason
    data class CalendarConflict(val taskId: String) : ScheduleRebuildReason
}

class SchedulerEngine {
    data class BusyWindow(
        val startAt: Instant,
        val endAt: Instant,
    )

    private data class ScoredBusyWindow(
        val window: BusyWindow,
        val overlapCount: Int,
        val midpointDistanceMinutes: Long,
        val startAt: Instant,
    )

    fun rebuildSchedule(
        tasks: List<ScheduleTask>,
        existingBlocks: List<ScheduleBlock>,
        busyWindows: List<BusyWindow>,
        workHours: WorkHoursProfile,
        timePeriods: List<TimePeriod>,
        policy: SchedulingPolicy,
        rangeStart: Instant,
        reason: ScheduleRebuildReason,
        preserveExistingPendingBlocks: Boolean = true,
    ): SchedulePlan {
        val zoneId = ZoneId.of(workHours.timezone)
        val existingPendingByTaskId = if (preserveExistingPendingBlocks) {
            existingBlocks
                .filter { it.completionState == BlockCompletionState.PENDING && it.lockState != BlockLockState.LOCKED }
                .groupBy { it.taskId }
        } else {
            emptyMap()
        }
        val existingAnchorByTaskId = existingPendingByTaskId
            .mapValues { (_, blocks) -> blocks.minOf { it.startAt } }
        val baseTasksToSchedule = tasks
            .filter { it.status == TaskStatus.ACTIVE && it.remainingMinutes > 0 }
            .sortedWith(
                compareBy<ScheduleTask> { !it.hasDeadline }
                    .thenBy { existingAnchorByTaskId[it.id] == null }
                    .thenBy { existingAnchorByTaskId[it.id] ?: Instant.MAX }
                    .thenByDescending { taskScore(it, rangeStart, policy) }
                    .thenBy { it.dueAt },
            )
        val activeTasksById = baseTasksToSchedule.associateBy { it.id }
        val allTasksById = tasks.associateBy { it.id }
        val tasksToSchedule = orderTasksWithDependencies(baseTasksToSchedule, activeTasksById)
        val lockedOrCompleted = existingBlocks.filter { block ->
            block.lockState == BlockLockState.LOCKED ||
                block.completionState == BlockCompletionState.COMPLETED
        }
        val pendingBlocks = if (preserveExistingPendingBlocks) {
            existingBlocks.filter { it.completionState == BlockCompletionState.PENDING }
        } else {
            emptyList()
        }
        val occupied = (
            busyWindows +
                lockedOrCompleted.map { BusyWindow(it.startAt, it.endAt) } +
                if (policy.allowConcurrentTasks) emptyList() else pendingBlocks.map { BusyWindow(it.startAt, it.endAt) }
            )
            .sortedBy { it.startAt }
            .toMutableList()
        val softOccupied = pendingBlocks
            .map { BusyWindow(it.startAt, it.endAt) }
            .toMutableList()
        val results = lockedOrCompleted.sortedBy { it.startAt }.toMutableList()
        val unscheduled = mutableListOf<String>()
        val issues = mutableListOf<SchedulingIssue>()

        for (task in tasksToSchedule) {
            val existingTaskBlocks = existingPendingByTaskId[task.id].orEmpty().sortedBy { it.startAt }
            var remaining = task.remainingMinutes
            val startingRemaining = remaining
            val dependencyBoundary = dependencyBoundary(
                task = task,
                allTasksById = allTasksById,
                existingBlocks = existingBlocks,
                scheduledBlocks = results,
            )
            val firstBlockStart = when {
                task.schedulingMode == TaskSchedulingMode.FIXED_DAY -> {
                    val occurrenceStart = task.dueAt.atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant()
                    maxInstant(maxInstant(rangeStart, occurrenceStart), dependencyBoundary ?: Instant.MIN)
                }
                task.schedulingMode == TaskSchedulingMode.FLEXIBLE_WINDOW && task.fixedStartAt != null -> {
                    maxInstant(maxInstant(rangeStart, task.fixedStartAt), dependencyBoundary ?: Instant.MIN)
                }
                task.schedulingMode == TaskSchedulingMode.FLEXIBLE && task.fixedStartAt != null -> {
                    maxInstant(maxInstant(rangeStart, task.fixedStartAt), dependencyBoundary ?: Instant.MIN)
                }
                task.recurrenceRule.type != dev.codex.reclaimoss.domain.model.RecurrenceType.NONE -> {
                    val occurrenceStart = task.dueAt.atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant()
                    maxInstant(maxInstant(rangeStart, occurrenceStart), dependencyBoundary ?: Instant.MIN)
                }
                else -> maxInstant(maxInstant(rangeStart, Instant.now().minus(3650, ChronoUnit.DAYS)), dependencyBoundary ?: Instant.MIN)
            }
            var cursor = firstBlockStart

            if (existingTaskBlocks.isNotEmpty()) {
                val occupiedWithoutSelf = occupied.filterNot { busy ->
                    existingTaskBlocks.any { existing -> existing.startAt == busy.startAt && existing.endAt == busy.endAt }
                }
                val canKeepExisting = existingTaskBlocks.all { existingBlock ->
                    blockCanStayScheduled(
                        block = existingBlock,
                        task = task,
                        occupiedWithoutSelf = occupiedWithoutSelf,
                        zoneId = zoneId,
                        workHours = workHours,
                        timePeriods = timePeriods,
                        rangeStart = firstBlockStart,
                        dependencyBoundary = dependencyBoundary,
                    )
                }
                if (canKeepExisting) {
                    results += existingTaskBlocks
                    val keptMinutes = existingTaskBlocks.sumOf { Duration.between(it.startAt, it.endAt).toMinutes().toInt() }
                    remaining = (remaining - keptMinutes).coerceAtLeast(0)
                    if (remaining == 0) {
                        continue
                    }
                    cursor = existingTaskBlocks.maxOf { it.endAt }
                        .plus(policy.breakBetweenBlocksMinutes.toLong(), ChronoUnit.MINUTES)
                } else {
                    existingTaskBlocks.forEach { existingBlock ->
                        occupied.removeAll { it.startAt == existingBlock.startAt && it.endAt == existingBlock.endAt }
                        softOccupied.removeAll { it.startAt == existingBlock.startAt && it.endAt == existingBlock.endAt }
                    }
                }
            }

            if (cursor == firstBlockStart) {
                cursor = existingAnchorByTaskId[task.id]?.let { maxInstant(firstBlockStart, it) } ?: firstBlockStart
            }

            while (remaining > 0 && cursor <= task.dueAt) {
                val candidate = nextCandidate(
                    task = task,
                    cursor = cursor,
                    dueAt = task.dueAt,
                    zoneId = zoneId,
                    workHours = workHours,
                    occupied = occupied,
                    softOccupied = softOccupied,
                    timePeriods = timePeriods,
                    policy = policy,
                    remainingMinutes = remaining,
                    preferredTimePeriodId = task.preferredTimePeriodId,
                ) ?: break

                val minutes = Duration.between(candidate.startAt, candidate.endAt).toMinutes().toInt()
                val block = ScheduleBlock(
                    id = "block-${task.id}-${results.count { it.taskId == task.id } + 1}",
                    taskId = task.id,
                    startAt = candidate.startAt,
                    endAt = candidate.endAt,
                    source = BlockSource.AUTO,
                    lockState = BlockLockState.FLEXIBLE,
                    completionState = BlockCompletionState.PENDING,
                    externalCalendarEventId = null,
                )
                results += block
                val busyWindow = BusyWindow(block.startAt, block.endAt)
                if (!policy.allowConcurrentTasks) {
                    occupied += busyWindow
                    occupied.sortBy { it.startAt }
                }
                softOccupied += busyWindow
                remaining -= minutes
                cursor = block.endAt.plus(policy.breakBetweenBlocksMinutes.toLong(), ChronoUnit.MINUTES)
            }

            if (remaining > 0) {
                unscheduled += task.id
                val scheduledMinutes = startingRemaining - remaining
                issues += SchedulingIssue(
                    taskId = task.id,
                    type = if (scheduledMinutes > 0) SchedulingIssueType.PARTIAL else SchedulingIssueType.UNSCHEDULED,
                    unscheduledMinutes = remaining,
                    reason = if (scheduledMinutes > 0) {
                        if (task.schedulingMode == TaskSchedulingMode.FIXED_DAY) {
                            "Only part of this task fits on the selected date."
                        } else if (task.schedulingMode == TaskSchedulingMode.FLEXIBLE_WINDOW) {
                            "Only part of this task fits inside its time window."
                        } else {
                            "Only part of this task fits before its deadline."
                        }
                    } else {
                        if (task.schedulingMode == TaskSchedulingMode.FIXED_DAY) {
                            "No productive time is available on the selected date."
                        } else if (task.schedulingMode == TaskSchedulingMode.FLEXIBLE_WINDOW) {
                            "No productive time is available inside this time window."
                        } else {
                            "No valid productive slot is available before the deadline."
                        }
                    },
                )
            }
        }

        return SchedulePlan(
            blocks = results.sortedBy { it.startAt },
            unscheduledTaskIds = unscheduled.distinct(),
            issues = issues,
        )
    }

    private fun blockCanStayScheduled(
        block: ScheduleBlock,
        task: ScheduleTask,
        occupiedWithoutSelf: List<BusyWindow>,
        zoneId: ZoneId,
        workHours: WorkHoursProfile,
        timePeriods: List<TimePeriod>,
        rangeStart: Instant,
        dependencyBoundary: Instant?,
    ): Boolean {
        if (block.endAt > task.dueAt) return false
        if (block.startAt < rangeStart) return false
        if (dependencyBoundary != null && block.startAt < dependencyBoundary) return false
        if (task.schedulingMode == TaskSchedulingMode.FLEXIBLE && task.fixedStartAt != null && block.startAt < task.fixedStartAt) return false
        if (task.schedulingMode == TaskSchedulingMode.FLEXIBLE_WINDOW) {
            val windowStart = task.fixedStartAt ?: return false
            val windowEnd = task.fixedEndAt ?: task.dueAt
            if (block.startAt < windowStart || block.endAt > windowEnd) return false
        }
        if (occupiedWithoutSelf.any { it.startAt < block.endAt && it.endAt > block.startAt }) return false
        if (!isInsideWorkingWindow(block, zoneId, workHours)) return false
        if (!matchesPreference(BusyWindow(block.startAt, block.endAt), zoneId, task.preferredTimePeriodId, timePeriods)) return false
        return true
    }

    private fun isInsideWorkingWindow(
        block: ScheduleBlock,
        zoneId: ZoneId,
        workHours: WorkHoursProfile,
    ): Boolean {
        val start = block.startAt.atZone(zoneId)
        val end = block.endAt.atZone(zoneId)
        if (start.toLocalDate() != end.toLocalDate()) return false
        val day = workHours.days[start.dayOfWeek] ?: return false
        return day.windows.any { window ->
            start.toLocalTime() >= window.start && end.toLocalTime() <= window.end
        }
    }

    private fun nextCandidate(
        task: ScheduleTask,
        cursor: Instant,
        dueAt: Instant,
        zoneId: ZoneId,
        workHours: WorkHoursProfile,
        occupied: List<BusyWindow>,
        softOccupied: List<BusyWindow>,
        timePeriods: List<TimePeriod>,
        policy: SchedulingPolicy,
        remainingMinutes: Int,
        preferredTimePeriodId: String?,
    ): BusyWindow? {
        val fixedDayEnd = if (task.schedulingMode == TaskSchedulingMode.FIXED_DAY) {
            task.dueAt.atZone(zoneId).toLocalDate().plusDays(1).atStartOfDay(zoneId).toInstant().minusSeconds(1)
        } else {
            dueAt
        }
        val flexibleWindowStart = if (task.schedulingMode == TaskSchedulingMode.FLEXIBLE_WINDOW) task.fixedStartAt else null
        val flexibleWindowEnd = if (task.schedulingMode == TaskSchedulingMode.FLEXIBLE_WINDOW) (task.fixedEndAt ?: dueAt) else null
        val lookAheadEnd = minInstant(
            fixedDayEnd,
            cursor.plus(policy.lookAheadDays.toLong(), ChronoUnit.DAYS),
        )
        var date = cursor.atZone(zoneId).toLocalDate()
        val endDate = lookAheadEnd.atZone(zoneId).toLocalDate()
        var earliestFallback: BusyWindow? = null
        var bestConcurrentCandidate: ScoredBusyWindow? = null

        while (!date.isAfter(endDate)) {
            val day = workHours.days[date.dayOfWeek] ?: return null
            val preferredSegments = mutableListOf<BusyWindow>()
            val fallbackSegments = mutableListOf<BusyWindow>()
            for (window in day.windows) {
                val windowStart = ZonedDateTime.of(date, window.start, zoneId).toInstant()
                val windowEnd = ZonedDateTime.of(date, window.end, zoneId).toInstant()
                if (windowEnd <= cursor) continue
                val segmentStart = maxInstant(windowStart, maxInstant(cursor, flexibleWindowStart ?: cursor))
                val segmentEnd = minInstant(windowEnd, minInstant(dueAt, flexibleWindowEnd ?: dueAt))
                if (segmentEnd <= segmentStart) continue
                val freeSegments = subtractBusy(
                    start = segmentStart,
                    end = segmentEnd,
                    occupied = occupied,
                )
                for (segment in freeSegments) {
                    val capacity = Duration.between(segment.startAt, segment.endAt).toMinutes().toInt()
                    if (capacity < policy.minBlockMinutes) continue
                    if (matchesPreference(segment, zoneId, preferredTimePeriodId, timePeriods)) {
                        preferredSegments += segment
                    } else {
                        fallbackSegments += segment
                    }
                }
            }
            if (policy.allowConcurrentTasks) {
                val preferredCandidate = preferredSegments.bestConcurrentBlock(
                    remainingMinutes = remainingMinutes,
                    maxBlockMinutes = policy.maxBlockMinutes,
                    minBlockMinutes = policy.minBlockMinutes,
                    zoneId = zoneId,
                    alignmentMinutes = policy.alignmentMinutes,
                    allowTaskSplitting = policy.allowTaskSplitting,
                    softOccupied = softOccupied,
                )
                val fallbackCandidate = if (!policy.strictPreferredPeriod && preferredCandidate == null) {
                    fallbackSegments.bestConcurrentBlock(
                        remainingMinutes = remainingMinutes,
                        maxBlockMinutes = policy.maxBlockMinutes,
                        minBlockMinutes = policy.minBlockMinutes,
                        zoneId = zoneId,
                        alignmentMinutes = policy.alignmentMinutes,
                        allowTaskSplitting = policy.allowTaskSplitting,
                        softOccupied = softOccupied,
                    )
                } else {
                    null
                }
                bestConcurrentCandidate = preferredCandidate
                    ?.let { candidate -> minByNullable(bestConcurrentCandidate, candidate) }
                    ?: fallbackCandidate?.let { candidate -> minByNullable(bestConcurrentCandidate, candidate) }
                    ?: bestConcurrentCandidate
            } else {
                val preferredCandidate = if (task.schedulingMode == TaskSchedulingMode.FLEXIBLE_WINDOW) {
                    preferredSegments.bestWindowedBlock(
                        remainingMinutes = remainingMinutes,
                        maxBlockMinutes = policy.maxBlockMinutes,
                        minBlockMinutes = policy.minBlockMinutes,
                        zoneId = zoneId,
                        alignmentMinutes = policy.alignmentMinutes,
                        allowTaskSplitting = policy.allowTaskSplitting,
                    )
                } else {
                    preferredSegments.firstBlock(
                        remainingMinutes = remainingMinutes,
                        maxBlockMinutes = policy.maxBlockMinutes,
                        minBlockMinutes = policy.minBlockMinutes,
                        zoneId = zoneId,
                        alignmentMinutes = policy.alignmentMinutes,
                        allowTaskSplitting = policy.allowTaskSplitting,
                    )
                }
                preferredCandidate?.let { return it }
            }
            if (!policy.strictPreferredPeriod && earliestFallback == null) {
                earliestFallback = if (task.schedulingMode == TaskSchedulingMode.FLEXIBLE_WINDOW) {
                    fallbackSegments.bestWindowedBlock(
                        remainingMinutes = remainingMinutes,
                        maxBlockMinutes = policy.maxBlockMinutes,
                        minBlockMinutes = policy.minBlockMinutes,
                        zoneId = zoneId,
                        alignmentMinutes = policy.alignmentMinutes,
                        allowTaskSplitting = policy.allowTaskSplitting,
                    )
                } else {
                    fallbackSegments.firstBlock(
                        remainingMinutes = remainingMinutes,
                        maxBlockMinutes = policy.maxBlockMinutes,
                        minBlockMinutes = policy.minBlockMinutes,
                        zoneId = zoneId,
                        alignmentMinutes = policy.alignmentMinutes,
                        allowTaskSplitting = policy.allowTaskSplitting,
                    )
                }
            }

            date = date.plusDays(1)
        }
        if (policy.allowConcurrentTasks) return bestConcurrentCandidate?.window
        return earliestFallback
    }

    private fun List<BusyWindow>.firstBlock(
        remainingMinutes: Int,
        maxBlockMinutes: Int,
        minBlockMinutes: Int,
        zoneId: ZoneId,
        alignmentMinutes: Int,
        allowTaskSplitting: Boolean,
    ): BusyWindow? {
        firstOrNull { segment ->
            val alignedStart = alignToNextAlignment(segment.startAt, zoneId, alignmentMinutes)
            Duration.between(alignedStart, segment.endAt).toMinutes().toInt() >= remainingMinutes
        }?.let { segment ->
            val alignedStart = alignToNextAlignment(segment.startAt, zoneId, alignmentMinutes)
            return BusyWindow(
                startAt = alignedStart,
                endAt = alignedStart.plus(remainingMinutes.toLong(), ChronoUnit.MINUTES),
            )
        }
        if (!allowTaskSplitting) return null
        val preferredBlockMinutes = min(remainingMinutes, maxBlockMinutes)
        firstOrNull { segment ->
            val alignedStart = alignToNextAlignment(segment.startAt, zoneId, alignmentMinutes)
            Duration.between(alignedStart, segment.endAt).toMinutes().toInt() >= preferredBlockMinutes
        }?.let { segment ->
            val alignedStart = alignToNextAlignment(segment.startAt, zoneId, alignmentMinutes)
            return BusyWindow(
                startAt = alignedStart,
                endAt = alignedStart.plus(preferredBlockMinutes.toLong(), ChronoUnit.MINUTES),
            )
        }
        firstOrNull { segment ->
            val alignedStart = alignToNextAlignment(segment.startAt, zoneId, alignmentMinutes)
            Duration.between(alignedStart, segment.endAt).toMinutes().toInt() >= minBlockMinutes
        }?.let { segment ->
            val alignedStart = alignToNextAlignment(segment.startAt, zoneId, alignmentMinutes)
            val partialBlockMinutes = min(
                min(
                    Duration.between(alignedStart, segment.endAt).toMinutes().toInt(),
                    remainingMinutes,
                ),
                maxBlockMinutes,
            )
            return BusyWindow(
                startAt = alignedStart,
                endAt = alignedStart.plus(partialBlockMinutes.toLong(), ChronoUnit.MINUTES),
            )
        }
        return null
    }

    private fun List<BusyWindow>.bestConcurrentBlock(
        remainingMinutes: Int,
        maxBlockMinutes: Int,
        minBlockMinutes: Int,
        zoneId: ZoneId,
        alignmentMinutes: Int,
        allowTaskSplitting: Boolean,
        softOccupied: List<BusyWindow>,
    ): ScoredBusyWindow? {
        var best: ScoredBusyWindow? = null
        for (segment in this) {
            val preferredBlockMinutes = when {
                Duration.between(segment.startAt, segment.endAt).toMinutes().toInt() >= remainingMinutes -> remainingMinutes
                !allowTaskSplitting -> continue
                Duration.between(segment.startAt, segment.endAt).toMinutes().toInt() >= min(remainingMinutes, maxBlockMinutes) -> min(remainingMinutes, maxBlockMinutes)
                else -> min(
                    min(Duration.between(alignToNextAlignment(segment.startAt, zoneId, alignmentMinutes), segment.endAt).toMinutes().toInt(), remainingMinutes),
                    maxBlockMinutes,
                )
            }
            if (preferredBlockMinutes < minBlockMinutes) continue
            val alignedStart = alignToNextAlignment(segment.startAt, zoneId, alignmentMinutes)
            var candidateStart = alignedStart
            val windowMidpoint = Duration.between(Instant.EPOCH, segment.startAt).toMinutes() +
                Duration.between(segment.startAt, segment.endAt).toMinutes() / 2
            while (!candidateStart.plus(preferredBlockMinutes.toLong(), ChronoUnit.MINUTES).isAfter(segment.endAt)) {
                val candidate = BusyWindow(
                    startAt = candidateStart,
                    endAt = candidateStart.plus(preferredBlockMinutes.toLong(), ChronoUnit.MINUTES),
                )
                val overlapCount = softOccupied.count { it.startAt < candidate.endAt && it.endAt > candidate.startAt }
                val candidateMidpoint = Duration.between(Instant.EPOCH, candidate.startAt).toMinutes() + preferredBlockMinutes / 2
                val score = ScoredBusyWindow(
                    window = candidate,
                    overlapCount = overlapCount,
                    midpointDistanceMinutes = kotlin.math.abs(candidateMidpoint - windowMidpoint),
                    startAt = candidate.startAt,
                )
                best = minByNullable(best, score)
                candidateStart = candidateStart.plus(alignmentMinutes.toLong(), ChronoUnit.MINUTES)
            }
        }
        return best
    }

    private fun List<BusyWindow>.bestWindowedBlock(
        remainingMinutes: Int,
        maxBlockMinutes: Int,
        minBlockMinutes: Int,
        zoneId: ZoneId,
        alignmentMinutes: Int,
        allowTaskSplitting: Boolean,
    ): BusyWindow? {
        var best: ScoredBusyWindow? = null
        for (segment in this) {
            val preferredBlockMinutes = when {
                Duration.between(segment.startAt, segment.endAt).toMinutes().toInt() >= remainingMinutes -> remainingMinutes
                !allowTaskSplitting -> continue
                Duration.between(segment.startAt, segment.endAt).toMinutes().toInt() >= min(remainingMinutes, maxBlockMinutes) -> min(remainingMinutes, maxBlockMinutes)
                else -> min(
                    min(Duration.between(alignToNextAlignment(segment.startAt, zoneId, alignmentMinutes), segment.endAt).toMinutes().toInt(), remainingMinutes),
                    maxBlockMinutes,
                )
            }
            if (preferredBlockMinutes < minBlockMinutes) continue
            val alignedStart = alignToNextAlignment(segment.startAt, zoneId, alignmentMinutes)
            var candidateStart = alignedStart
            val windowMidpoint = Duration.between(Instant.EPOCH, segment.startAt).toMinutes() +
                Duration.between(segment.startAt, segment.endAt).toMinutes() / 2
            while (!candidateStart.plus(preferredBlockMinutes.toLong(), ChronoUnit.MINUTES).isAfter(segment.endAt)) {
                val candidate = BusyWindow(
                    startAt = candidateStart,
                    endAt = candidateStart.plus(preferredBlockMinutes.toLong(), ChronoUnit.MINUTES),
                )
                val candidateMidpoint = Duration.between(Instant.EPOCH, candidate.startAt).toMinutes() + preferredBlockMinutes / 2
                val score = ScoredBusyWindow(
                    window = candidate,
                    overlapCount = 0,
                    midpointDistanceMinutes = kotlin.math.abs(candidateMidpoint - windowMidpoint),
                    startAt = candidate.startAt,
                )
                best = minByNullable(best, score)
                candidateStart = candidateStart.plus(alignmentMinutes.toLong(), ChronoUnit.MINUTES)
            }
        }
        return best?.window
    }

    private fun minByNullable(current: ScoredBusyWindow?, candidate: ScoredBusyWindow): ScoredBusyWindow =
        when {
            current == null -> candidate
            candidate.overlapCount < current.overlapCount -> candidate
            candidate.overlapCount > current.overlapCount -> current
            candidate.midpointDistanceMinutes < current.midpointDistanceMinutes -> candidate
            candidate.midpointDistanceMinutes > current.midpointDistanceMinutes -> current
            candidate.startAt < current.startAt -> candidate
            else -> current
        }

    private fun matchesPreference(
        segment: BusyWindow,
        zoneId: ZoneId,
        preferredTimePeriodId: String?,
        timePeriods: List<TimePeriod>,
    ): Boolean {
        if (preferredTimePeriodId == null) return true
        val preferredPeriod = timePeriods.firstOrNull { it.id == preferredTimePeriodId } ?: return false
        val localStart = segment.startAt.atZone(zoneId).toLocalTime()
        return localStart >= preferredPeriod.start && localStart < preferredPeriod.end
    }

    private fun subtractBusy(
        start: Instant,
        end: Instant,
        occupied: List<BusyWindow>,
    ): List<BusyWindow> {
        if (end <= start) return emptyList()
        val free = mutableListOf<BusyWindow>()
        var cursor = start
        occupied
            .filter { it.endAt > start && it.startAt < end }
            .sortedBy { it.startAt }
            .forEach { busy ->
                if (busy.startAt > cursor) {
                    free += BusyWindow(cursor, minInstant(busy.startAt, end))
                }
                if (busy.endAt > cursor) {
                    cursor = maxInstant(cursor, busy.endAt)
                }
            }
        if (cursor < end) {
            free += BusyWindow(cursor, end)
        }
        return free.filter { it.endAt > it.startAt }
    }

    private fun orderTasksWithDependencies(
        baseTasks: List<ScheduleTask>,
        tasksById: Map<String, ScheduleTask>,
    ): List<ScheduleTask> {
        val ordered = mutableListOf<ScheduleTask>()
        val visiting = mutableSetOf<String>()
        val visited = mutableSetOf<String>()

        fun visit(task: ScheduleTask) {
            if (task.id in visited) return
            if (!visiting.add(task.id)) return
            val parentId = task.continuationParentTaskId
            if (parentId != null) {
                tasksById[parentId]?.let(::visit)
            }
            visiting.remove(task.id)
            visited += task.id
            ordered += task
        }

        baseTasks.forEach(::visit)
        return ordered
    }

    private fun dependencyBoundary(
        task: ScheduleTask,
        allTasksById: Map<String, ScheduleTask>,
        existingBlocks: List<ScheduleBlock>,
        scheduledBlocks: List<ScheduleBlock>,
    ): Instant? {
        val parentId = task.continuationParentTaskId ?: return null
        val parentTask = allTasksById[parentId]
        return when (task.continuationMode ?: TaskContinuationMode.AFTER_PARENT_SCHEDULED_END) {
            TaskContinuationMode.AFTER_PARENT_SCHEDULED_END -> {
                val latestParentEnd = (existingBlocks.asSequence() + scheduledBlocks.asSequence())
                    .filter { it.taskId == parentId }
                    .maxOfOrNull { it.endAt }
                latestParentEnd ?: parentTask?.dueAt
            }
            TaskContinuationMode.AFTER_PARENT_DUE_AT -> parentTask?.dueAt
        }
    }

    private fun taskScore(
        task: ScheduleTask,
        rangeStart: Instant,
        policy: SchedulingPolicy,
    ): Double {
        val urgency = if (task.hasDeadline) {
            val minutesToDeadline = max(1, Duration.between(rangeStart, task.dueAt).toMinutes().toInt())
            policy.deadlineUrgencyWeight * (1440.0 / minutesToDeadline.toDouble())
        } else {
            0.0
        }
        val priority = policy.priorityWeight * task.priority.score.toDouble()
        return urgency + priority
    }

    private fun minInstant(a: Instant, b: Instant): Instant = if (a <= b) a else b

    private fun maxInstant(a: Instant, b: Instant): Instant = if (a >= b) a else b

    private fun alignToNextAlignment(instant: Instant, zoneId: ZoneId, alignmentMinutes: Int): Instant {
        val local = instant.atZone(zoneId)
        val minute = local.minute
        val hasSubMinute = local.second != 0 || local.nano != 0
        val effectiveAlignment = alignmentMinutes.coerceAtLeast(1)
        val remainder = minute % effectiveAlignment
        val minutesToAdd = when {
            remainder == 0 && !hasSubMinute -> 0
            remainder == 0 -> effectiveAlignment
            else -> effectiveAlignment - remainder
        }
        return local
            .truncatedTo(ChronoUnit.MINUTES)
            .plusMinutes(minutesToAdd.toLong())
            .toInstant()
    }
}
