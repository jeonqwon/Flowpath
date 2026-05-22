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
        val tasksToSchedule = tasks
            .filter { it.status == TaskStatus.ACTIVE && it.remainingMinutes > 0 }
            .sortedWith(
                compareBy<ScheduleTask> { existingAnchorByTaskId[it.id] == null }
                    .thenBy { existingAnchorByTaskId[it.id] ?: Instant.MAX }
                    .thenByDescending { taskScore(it, rangeStart, policy) }
                    .thenBy { it.dueAt },
            )
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
                pendingBlocks.map { BusyWindow(it.startAt, it.endAt) }
            )
            .sortedBy { it.startAt }
            .toMutableList()
        val results = lockedOrCompleted.sortedBy { it.startAt }.toMutableList()
        val unscheduled = mutableListOf<String>()
        val issues = mutableListOf<SchedulingIssue>()

        for (task in tasksToSchedule) {
            val existingTaskBlocks = existingPendingByTaskId[task.id].orEmpty().sortedBy { it.startAt }
            var remaining = task.remainingMinutes
            val startingRemaining = remaining
            val firstBlockStart = task.takeIf { it.recurrenceRule.type != dev.codex.reclaimoss.domain.model.RecurrenceType.NONE }?.let {
                val occurrenceStart = task.dueAt.atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant()
                maxInstant(rangeStart, occurrenceStart)
            } ?: maxInstant(rangeStart, Instant.now().minus(3650, ChronoUnit.DAYS))
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
                    }
                }
            }

            if (cursor == firstBlockStart) {
                cursor = existingAnchorByTaskId[task.id]?.let { maxInstant(firstBlockStart, it) } ?: firstBlockStart
            }

            while (remaining > 0 && cursor <= task.dueAt) {
                val candidate = nextCandidate(
                    cursor = cursor,
                    dueAt = task.dueAt,
                    zoneId = zoneId,
                    workHours = workHours,
                    occupied = occupied,
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
                occupied += BusyWindow(block.startAt, block.endAt)
                occupied.sortBy { it.startAt }
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
                        "Only part of this task fits before its deadline."
                    } else {
                        "No valid productive slot is available before the deadline."
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
    ): Boolean {
        if (block.endAt > task.dueAt) return false
        if (block.startAt < rangeStart) return false
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
        cursor: Instant,
        dueAt: Instant,
        zoneId: ZoneId,
        workHours: WorkHoursProfile,
        occupied: List<BusyWindow>,
        timePeriods: List<TimePeriod>,
        policy: SchedulingPolicy,
        remainingMinutes: Int,
        preferredTimePeriodId: String?,
    ): BusyWindow? {
        val lookAheadEnd = minInstant(
            dueAt,
            cursor.plus(policy.lookAheadDays.toLong(), ChronoUnit.DAYS),
        )
        var date = cursor.atZone(zoneId).toLocalDate()
        val endDate = lookAheadEnd.atZone(zoneId).toLocalDate()
        var earliestFallback: BusyWindow? = null

        while (!date.isAfter(endDate)) {
            val day = workHours.days[date.dayOfWeek] ?: return null
            val preferredSegments = mutableListOf<BusyWindow>()
            val fallbackSegments = mutableListOf<BusyWindow>()
            for (window in day.windows) {
                val windowStart = ZonedDateTime.of(date, window.start, zoneId).toInstant()
                val windowEnd = ZonedDateTime.of(date, window.end, zoneId).toInstant()
                if (windowEnd <= cursor) continue
                val freeSegments = subtractBusy(
                    start = maxInstant(windowStart, cursor),
                    end = minInstant(windowEnd, dueAt),
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
            preferredSegments.firstBlock(
                remainingMinutes = remainingMinutes,
                maxBlockMinutes = policy.maxBlockMinutes,
                minBlockMinutes = policy.minBlockMinutes,
                zoneId = zoneId,
            )?.let { return it }
            if (earliestFallback == null) {
                earliestFallback = fallbackSegments.firstBlock(
                    remainingMinutes = remainingMinutes,
                    maxBlockMinutes = policy.maxBlockMinutes,
                    minBlockMinutes = policy.minBlockMinutes,
                    zoneId = zoneId,
                )
            }

            date = date.plusDays(1)
        }
        return earliestFallback
    }

    private fun List<BusyWindow>.firstBlock(
        remainingMinutes: Int,
        maxBlockMinutes: Int,
        minBlockMinutes: Int,
        zoneId: ZoneId,
    ): BusyWindow? {
        firstOrNull { segment ->
            val alignedStart = alignToNextHalfHour(segment.startAt, zoneId)
            Duration.between(alignedStart, segment.endAt).toMinutes().toInt() >= remainingMinutes
        }?.let { segment ->
            val alignedStart = alignToNextHalfHour(segment.startAt, zoneId)
            return BusyWindow(
                startAt = alignedStart,
                endAt = alignedStart.plus(remainingMinutes.toLong(), ChronoUnit.MINUTES),
            )
        }
        val preferredBlockMinutes = min(remainingMinutes, maxBlockMinutes)
        firstOrNull { segment ->
            val alignedStart = alignToNextHalfHour(segment.startAt, zoneId)
            Duration.between(alignedStart, segment.endAt).toMinutes().toInt() >= preferredBlockMinutes
        }?.let { segment ->
            val alignedStart = alignToNextHalfHour(segment.startAt, zoneId)
            return BusyWindow(
                startAt = alignedStart,
                endAt = alignedStart.plus(preferredBlockMinutes.toLong(), ChronoUnit.MINUTES),
            )
        }
        firstOrNull { segment ->
            val alignedStart = alignToNextHalfHour(segment.startAt, zoneId)
            Duration.between(alignedStart, segment.endAt).toMinutes().toInt() >= minBlockMinutes
        }?.let { segment ->
            val alignedStart = alignToNextHalfHour(segment.startAt, zoneId)
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

    private fun taskScore(
        task: ScheduleTask,
        rangeStart: Instant,
        policy: SchedulingPolicy,
    ): Double {
        val minutesToDeadline = max(1, Duration.between(rangeStart, task.dueAt).toMinutes().toInt())
        val urgency = policy.deadlineUrgencyWeight * (1440.0 / minutesToDeadline.toDouble())
        val priority = policy.priorityWeight * task.priority.score.toDouble()
        return urgency + priority
    }

    private fun minInstant(a: Instant, b: Instant): Instant = if (a <= b) a else b

    private fun maxInstant(a: Instant, b: Instant): Instant = if (a >= b) a else b

    private fun alignToNextHalfHour(instant: Instant, zoneId: ZoneId): Instant {
        val local = instant.atZone(zoneId)
        val minute = local.minute
        val hasSubMinute = local.second != 0 || local.nano != 0
        val minutesToAdd = when {
            minute == 0 && !hasSubMinute -> 0
            minute == 30 && !hasSubMinute -> 0
            minute < 30 -> 30 - minute
            else -> 60 - minute
        }
        return local
            .truncatedTo(ChronoUnit.MINUTES)
            .plusMinutes(minutesToAdd.toLong())
            .toInstant()
    }
}
