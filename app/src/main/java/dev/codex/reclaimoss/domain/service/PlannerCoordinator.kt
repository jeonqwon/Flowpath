package dev.codex.reclaimoss.domain.service

import dev.codex.reclaimoss.data.calendar.GoogleCalendarGateway
import dev.codex.reclaimoss.data.repository.PlannerRepository
import dev.codex.reclaimoss.domain.model.BlockCompletionState
import dev.codex.reclaimoss.domain.model.BlockLockState
import dev.codex.reclaimoss.domain.model.PreferredTimeOfDay
import dev.codex.reclaimoss.domain.model.RecurrenceEndMode
import dev.codex.reclaimoss.domain.model.RecurrenceRule
import dev.codex.reclaimoss.domain.model.RecurrenceType
import dev.codex.reclaimoss.domain.model.Reminder
import dev.codex.reclaimoss.domain.model.ReminderPolicy
import dev.codex.reclaimoss.domain.model.ReminderStatus
import dev.codex.reclaimoss.domain.model.ScheduleTask
import dev.codex.reclaimoss.domain.model.SchedulingIssue
import dev.codex.reclaimoss.domain.model.SchedulingPolicy
import dev.codex.reclaimoss.domain.model.SchedulingIssueType
import dev.codex.reclaimoss.domain.model.TaskContinuationMode
import dev.codex.reclaimoss.domain.model.TaskPriority
import dev.codex.reclaimoss.domain.model.TaskSchedulingMode
import dev.codex.reclaimoss.domain.model.TaskStatus
import dev.codex.reclaimoss.domain.model.TimePeriod
import dev.codex.reclaimoss.domain.model.TimePeriodType
import dev.codex.reclaimoss.domain.model.TimeWindow
import dev.codex.reclaimoss.domain.model.WorkHoursDay
import dev.codex.reclaimoss.domain.model.WorkHoursProfile
import java.time.Clock
import dev.codex.reclaimoss.domain.scheduling.ScheduleRebuildReason
import dev.codex.reclaimoss.domain.scheduling.SchedulerEngine
import dev.codex.reclaimoss.settings.AppSettings
import dev.codex.reclaimoss.settings.PreferredPeriodFallbackMode
import dev.codex.reclaimoss.settings.ReminderTimingMode
import dev.codex.reclaimoss.settings.UrgentRescheduleMode
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class TaskCreationResult(
    val taskId: String,
    val scheduled: Boolean,
    val partial: Boolean,
    val reason: String? = null,
)

private data class TaskOccurrence(
    val dueAt: Instant,
    val fixedStartAt: Instant?,
    val fixedEndAt: Instant?,
)

class PlannerCoordinator(
    private val repository: PlannerRepository,
    private val scheduler: SchedulerEngine,
    private val calendarGateway: GoogleCalendarGateway,
    private val getSettings: suspend () -> AppSettings = { AppSettings() },
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    val snapshot = repository.observeSnapshot()
    private val recurrenceMaterializationDays = 180

    private val emptyWorkHours = WorkHoursProfile(
        timezone = clock.zone.id,
        days = DayOfWeek.entries.associateWith {
            WorkHoursDay(
                windows = emptyList(),
            )
        },
    )

    suspend fun ensureSeedData() {
        repository.seedDemoDataIfEmpty()
        rebuildSchedule()
    }

    suspend fun createTask(
        title: String,
        description: String,
        priority: TaskPriority,
        dueAt: Instant,
        preferredTimePeriodId: String?,
        hasDeadline: Boolean = true,
        continuationParentTaskId: String? = null,
        continuationMode: TaskContinuationMode? = null,
        recurrenceRule: RecurrenceRule,
        estimatedMinutes: Int,
        addReminder: Boolean,
        schedulingMode: TaskSchedulingMode = TaskSchedulingMode.FLEXIBLE,
        fixedStartAt: Instant? = null,
        fixedEndAt: Instant? = null,
    ): TaskCreationResult {
        requireValidContinuationParent(continuationParentTaskId)
        val isRecurringSeries = recurrenceRule.type != RecurrenceType.NONE
        val seriesId = if (isRecurringSeries) newId("series") else null
        val occurrences = materializedOccurrences(
            initialDueAt = dueAt,
            recurrenceRule = recurrenceRule,
            schedulingMode = schedulingMode,
            fixedStartAt = fixedStartAt,
            fixedEndAt = fixedEndAt,
        )
        val taskIdsNeedingReminder = mutableListOf<String>()
        val createdTaskIds = occurrences.mapIndexed { index, occurrence ->
            val taskId = "${newId("task")}-$index"
            repository.upsertTask(
                ScheduleTask(
                    id = taskId,
                    recurrenceSeriesId = seriesId,
                    projectId = "project-default",
                    title = title,
                    description = description,
                    priority = priority,
                    preferredTimeOfDay = PreferredTimeOfDay.ANYTIME,
                    preferredTimePeriodId = preferredTimePeriodId,
                    hasDeadline = hasDeadline,
                    continuationParentTaskId = continuationParentTaskId,
                    continuationMode = continuationMode,
                    schedulingMode = schedulingMode,
                    fixedStartAt = occurrence.fixedStartAt,
                    fixedEndAt = occurrence.fixedEndAt,
                    dueAt = occurrence.dueAt,
                    estimatedMinutes = estimatedMinutes,
                    remainingMinutes = estimatedMinutes,
                    recurrenceRule = recurrenceRule,
                    reminderPolicy = ReminderPolicy(10, 5, 0),
                    status = TaskStatus.ACTIVE,
                ),
            )
            if (addReminder) taskIdsNeedingReminder += taskId
            taskId
        }
        if (schedulingMode == TaskSchedulingMode.FIXED_EXACT) {
            createdTaskIds.forEach { taskId ->
                placeExactTask(taskId)
            }
        } else if (createdTaskIds.size == 1) {
            scheduleTask(createdTaskIds.first())
        } else {
            rebuildSchedule()
        }
        taskIdsNeedingReminder.forEach { createReminderForTask(it) }
        val primaryTaskId = createdTaskIds.first()
        val result = taskResultFor(primaryTaskId)
        val failedToFullySchedule = !result.scheduled
        if (failedToFullySchedule && recurrenceRule.type == RecurrenceType.NONE) {
            repository.getReminders()
                .filter { it.linkedTaskId == primaryTaskId }
                .forEach { repository.deleteReminder(it.id) }
            deleteTask(primaryTaskId)
        }
        return result
    }

    suspend fun createFollowUpTask(
        sourceTaskId: String,
        title: String,
        description: String,
        priority: TaskPriority,
        dueAt: Instant,
        preferredTimePeriodId: String?,
        hasDeadline: Boolean = true,
        continuationParentTaskId: String? = null,
        continuationMode: TaskContinuationMode? = null,
        recurrenceRule: RecurrenceRule,
        estimatedMinutes: Int,
        addReminder: Boolean,
        schedulingMode: TaskSchedulingMode = TaskSchedulingMode.FLEXIBLE,
        fixedStartAt: Instant? = null,
        fixedEndAt: Instant? = null,
    ): TaskCreationResult? {
        val sourceTask = repository.getTasks().firstOrNull { it.id == sourceTaskId } ?: return null
        val result = createTask(
            title = title,
            description = description,
            priority = priority,
            dueAt = dueAt,
            preferredTimePeriodId = preferredTimePeriodId,
            hasDeadline = hasDeadline,
            continuationParentTaskId = continuationParentTaskId,
            continuationMode = continuationMode,
            recurrenceRule = recurrenceRule,
            estimatedMinutes = estimatedMinutes,
            addReminder = addReminder,
            schedulingMode = schedulingMode,
            fixedStartAt = fixedStartAt,
            fixedEndAt = fixedEndAt,
        )
        if (!result.scheduled) return result
        repository.clearAllPendingBlocks(sourceTaskId)
        repository.upsertTask(sourceTask.copy(remainingMinutes = 0, status = TaskStatus.COMPLETED, updatedAt = now()))
        return result
    }

    suspend fun createReminder(
        title: String,
        description: String,
        dueAt: Instant,
        recurrenceRule: RecurrenceRule = RecurrenceRule(),
        linkedTaskId: String? = null,
    ): String {
        val reminderId = newId("reminder")
        repository.upsertReminder(
            Reminder(
                id = reminderId,
                title = title,
                description = description,
                dueAt = dueAt,
                recurrenceRule = recurrenceRule,
                linkedTaskId = linkedTaskId,
            ),
        )
        return reminderId
    }

    suspend fun createReminderForTask(taskId: String): String? {
        val task = repository.getTasks().firstOrNull { it.id == taskId } ?: return null
        val reminderDueAt = reminderDueAtForTask(task)
        repository.getReminders()
            .firstOrNull { it.linkedTaskId == task.id && it.status != ReminderStatus.COMPLETED }
            ?.let { existing ->
                repository.upsertReminder(
                    existing.copy(
                        title = task.title,
                        description = task.description,
                        dueAt = reminderDueAt,
                        recurrenceRule = task.recurrenceRule,
                        updatedAt = now(),
                    ),
                )
                return existing.id
            }
        return createReminder(
            title = task.title,
            description = task.description,
            dueAt = reminderDueAt,
            recurrenceRule = task.recurrenceRule,
            linkedTaskId = task.id,
        )
    }

    suspend fun completeReminder(reminderId: String) {
        val reminder = repository.getReminders().firstOrNull { it.id == reminderId } ?: return
        repository.upsertReminder(reminder.copy(status = ReminderStatus.COMPLETED, updatedAt = now()))
    }

    suspend fun dismissReminder(reminderId: String) {
        repository.deleteReminder(reminderId)
    }

    suspend fun snoozeReminder(reminderId: String) {
        val reminder = repository.getReminders().firstOrNull { it.id == reminderId } ?: return
        repository.upsertReminder(
            reminder.copy(
                dueAt = reminder.dueAt.plus(1, ChronoUnit.DAYS),
                status = ReminderStatus.SNOOZED,
                updatedAt = now(),
            ),
        )
    }

    suspend fun scheduleTask(taskId: String) {
        rebuildSchedule(ScheduleRebuildReason.ManualRebuild, taskId)
    }

    suspend fun rescheduleTask(taskId: String, reason: ScheduleRebuildReason) {
        repository.clearAllPendingBlocks(taskId)
        rebuildSchedule(reason, taskId)
    }

    suspend fun rescheduleUrgently(taskId: String, dueAt: Instant? = null): Boolean {
        val task = repository.getTasks().firstOrNull { it.id == taskId } ?: return false
        val updatedTask = task.copy(
            priority = TaskPriority.URGENT,
            dueAt = effectiveRescheduleDueAt(task, dueAt),
            updatedAt = now(),
        )
        val settings = getSettings()
        return performReschedule(
            originalTask = task,
            updatedTask = updatedTask,
            allowMovingOtherTasks = settings.urgentRescheduleMode == UrgentRescheduleMode.MOVE_OTHER_FLEXIBLE_IF_NEEDED,
        )
    }

    suspend fun rescheduleNextAvailable(taskId: String, dueAt: Instant? = null): Boolean {
        val task = repository.getTasks().firstOrNull { it.id == taskId } ?: return false
        val updatedTask = task.copy(
            priority = TaskPriority.MEDIUM,
            dueAt = effectiveRescheduleDueAt(task, dueAt),
            updatedAt = now(),
        )
        return performReschedule(
            originalTask = task,
            updatedTask = updatedTask,
            allowMovingOtherTasks = false,
        )
    }

    suspend fun rescheduleToDueDate(taskId: String, dueAt: Instant) {
        repository.clearAllPendingBlocks(taskId)
        repository.updateTaskDueDate(taskId, dueAt)
        rebuildSchedule(ScheduleRebuildReason.ManualRebuild, taskId)
    }

    suspend fun rescheduleTaskWithUpdate(
        taskId: String,
        title: String,
        description: String,
        priority: TaskPriority,
        dueAt: Instant,
        preferredTimePeriodId: String?,
        hasDeadline: Boolean = true,
        continuationParentTaskId: String? = null,
        continuationMode: TaskContinuationMode? = null,
        recurrenceRule: RecurrenceRule,
        estimatedMinutes: Int,
        schedulingMode: TaskSchedulingMode,
        fixedStartAt: Instant? = null,
        fixedEndAt: Instant? = null,
    ): TaskCreationResult {
        val existingTask = repository.getTasks().firstOrNull { it.id == taskId } ?: return TaskCreationResult(
                taskId = taskId,
                scheduled = false,
                partial = false,
                reason = "Task no longer exists.",
            )
        requireValidContinuationParent(
            continuationParentTaskId = continuationParentTaskId,
            currentTaskId = taskId,
        )
        val originalPendingBlocks = repository.getBlocks()
            .filter { it.taskId == taskId && it.completionState != BlockCompletionState.COMPLETED }
        val originalIssues = repository.getSchedulingIssues().filter { it.taskId == taskId }
        val updatedTask = existingTask.copy(
            title = title,
            description = description,
            priority = priority,
            preferredTimePeriodId = preferredTimePeriodId,
            hasDeadline = hasDeadline,
            continuationParentTaskId = continuationParentTaskId,
            continuationMode = continuationMode,
            schedulingMode = schedulingMode,
            fixedStartAt = fixedStartAt,
            fixedEndAt = fixedEndAt,
            dueAt = dueAt,
            estimatedMinutes = estimatedMinutes,
            remainingMinutes = estimatedMinutes,
            recurrenceRule = recurrenceRule,
            updatedAt = now(),
        )
        repository.clearAllPendingBlocks(taskId)
        repository.upsertTask(updatedTask)
        if (schedulingMode == TaskSchedulingMode.FIXED_EXACT) {
            placeExactTask(taskId)
        } else {
            rebuildSchedule(ScheduleRebuildReason.ManualRebuild, taskId)
        }
        val result = taskResultFor(taskId)
        if (!result.scheduled) {
            repository.upsertTask(existingTask)
            repository.replaceFlexibleBlocks(taskId, originalPendingBlocks)
            repository.replaceSchedulingIssuesForTask(taskId, originalIssues)
            syncLinkedReminderForTask(taskId)
            return result
        }
        syncLinkedReminderForTask(taskId)
        return result
    }

    suspend fun rebuildSchedule(
        reason: ScheduleRebuildReason = ScheduleRebuildReason.ManualRebuild,
        onlyTaskId: String? = null,
    ) {
        val tasks = repository.getTasks()
        val filteredTasks = onlyTaskId?.let { id -> tasks.filter { it.id == id } } ?: tasks
        if (filteredTasks.isEmpty()) return

        val exactTasks = filteredTasks.filter { it.schedulingMode == TaskSchedulingMode.FIXED_EXACT }
        val schedulableTasks = filteredTasks.filter { it.schedulingMode != TaskSchedulingMode.FIXED_EXACT }

        val existingBlocks = repository.getBlocks()
        val timePeriods = repository.getTimePeriods()
        val rangeStart = now()
        val policy = schedulingPolicy(getSettings())
        val rangeEnd = rangeStart.plusSeconds(60L * 60L * 24L * policy.lookAheadDays)
        val busyEvents = calendarGateway.syncBusyEvents(rangeStart, rangeEnd) + lifePeriodBusyWindows(
            timePeriods = timePeriods,
            rangeStart = rangeStart,
            rangeEnd = rangeEnd,
        )
        if (schedulableTasks.isNotEmpty()) {
            val plan = scheduler.rebuildSchedule(
                tasks = schedulableTasks,
                existingBlocks = existingBlocks,
                busyWindows = busyEvents,
                workHours = workHoursFromProductivePeriods(timePeriods),
                timePeriods = timePeriods.filter { it.type == TimePeriodType.PRODUCTIVE },
                policy = policy,
                rangeStart = rangeStart,
                reason = reason,
            )
            schedulableTasks.forEach { task ->
                repository.replaceFlexibleBlocks(task.id, plan.blocks.filter { it.taskId == task.id })
                repository.replaceSchedulingIssuesForTask(task.id, plan.issues.filter { it.taskId == task.id })
                syncLinkedReminderForTask(task.id)
            }
            calendarGateway.syncPlannedBlocks(repository.getBlocks())
        }
        exactTasks.forEach { task ->
            placeExactTask(task.id)
        }
    }

    suspend fun lockBlock(blockId: String) {
        repository.updateBlockLock(blockId, BlockLockState.LOCKED)
    }

    suspend fun unlockBlock(blockId: String) {
        repository.updateBlockLock(blockId, BlockLockState.FLEXIBLE)
    }

    suspend fun markBlockDone(blockId: String, taskId: String, blockMinutes: Int) {
        repository.updateBlockCompletion(blockId, BlockCompletionState.COMPLETED)
        val task = repository.getTasks().firstOrNull { it.id == taskId } ?: return
        val newRemaining = (task.remainingMinutes - blockMinutes).coerceAtLeast(0)
        repository.updateTaskRemaining(taskId, newRemaining)
        if (newRemaining > 0) return

        if (task.recurrenceSeriesId != null) {
            completeRecurringOccurrence(task)
            return
        }

        if (task.recurrenceRule.type == RecurrenceType.NONE) {
            repository.upsertTask(task.copy(remainingMinutes = 0, status = TaskStatus.COMPLETED))
            return
        }

        repository.clearAllPendingBlocks(taskId)
        repository.upsertTask(
            task.copy(
                dueAt = nextFutureOccurrence(task),
                remainingMinutes = task.estimatedMinutes,
                status = TaskStatus.ACTIVE,
                updatedAt = now(),
            ),
        )
        rebuildSchedule(ScheduleRebuildReason.ManualRebuild, taskId)
    }

    suspend fun markTaskDone(taskId: String) {
        val task = repository.getTasks().firstOrNull { it.id == taskId } ?: return
        repository.clearAllPendingBlocks(taskId)
        if (task.recurrenceSeriesId != null) {
            completeRecurringOccurrence(task)
            return
        }
        if (task.recurrenceRule.type == RecurrenceType.NONE) {
            repository.upsertTask(task.copy(remainingMinutes = 0, status = TaskStatus.COMPLETED, updatedAt = now()))
            return
        }

        repository.upsertTask(
            task.copy(
                dueAt = nextFutureOccurrence(task),
                remainingMinutes = task.estimatedMinutes,
                status = TaskStatus.ACTIVE,
                updatedAt = now(),
            ),
        )
        rebuildSchedule(ScheduleRebuildReason.ManualRebuild, taskId)
    }

    suspend fun markRecurringSeriesDone(taskId: String) {
        val task = repository.getTasks().firstOrNull { it.id == taskId } ?: return
        val seriesId = task.recurrenceSeriesId
        if (seriesId == null) {
            markTaskDone(taskId)
            return
        }
        repository.getTasks()
            .filter { it.recurrenceSeriesId == seriesId && it.status == TaskStatus.ACTIVE }
            .forEach { occurrence ->
                repository.clearAllPendingBlocks(occurrence.id)
                repository.upsertTask(
                    occurrence.copy(
                        remainingMinutes = 0,
                        status = TaskStatus.COMPLETED,
                        updatedAt = now(),
                    ),
                )
            }
    }

    suspend fun deleteTask(taskId: String) {
        repository.deleteTask(taskId)
    }

    suspend fun upsertTimePeriod(period: TimePeriod) {
        repository.upsertTimePeriod(period)
        rebuildSchedule()
    }

    suspend fun deleteTimePeriod(periodId: String) {
        repository.deleteTimePeriod(periodId)
        rebuildSchedule()
    }

    fun projectNames(): Flow<Map<String, String>> =
        snapshot.map { state -> state.projects.associate { it.id to it.name } }

    private suspend fun performReschedule(
        originalTask: ScheduleTask,
        updatedTask: ScheduleTask,
        allowMovingOtherTasks: Boolean,
    ): Boolean {
        val timePeriods = repository.getTimePeriods()
        val existingBlocks = repository.getBlocks()
        val currentTaskBlocks = existingBlocks
            .filter { it.taskId == originalTask.id && it.completionState == BlockCompletionState.PENDING }
            .sortedBy { it.startAt }
        val blockedOldWindows = currentTaskBlocks.map { SchedulerEngine.BusyWindow(it.startAt, it.endAt) }
        val rangeStart = listOfNotNull(
            currentTaskBlocks.maxOfOrNull { it.endAt },
            now(),
        ).maxOrNull() ?: now()

        val targetedPlan = buildSchedulePlan(
            tasks = listOf(updatedTask),
            existingBlocks = existingBlocks.filter { it.taskId != originalTask.id },
            timePeriods = timePeriods,
            rangeStart = rangeStart,
            extraBusyWindows = blockedOldWindows,
            preserveExistingPendingBlocks = true,
        )
        if (planSchedulesTaskCleanly(targetedPlan, updatedTask.id)) {
            repository.upsertTask(updatedTask)
            applyPlanForTasks(targetedPlan, listOf(updatedTask))
            return true
        }

        if (!allowMovingOtherTasks) {
            return false
        }

        val allTasks = repository.getTasks()
            .map { task -> if (task.id == updatedTask.id) updatedTask else task }
        val rebuildPlan = buildSchedulePlan(
            tasks = allTasks,
            existingBlocks = existingBlocks.filter { it.taskId != originalTask.id },
            timePeriods = timePeriods,
            rangeStart = now(),
            extraBusyWindows = blockedOldWindows,
            preserveExistingPendingBlocks = false,
        )
        if (!planSchedulesTaskCleanly(rebuildPlan, updatedTask.id)) {
            return false
        }

        repository.upsertTask(updatedTask)
        applyPlanForTasks(
            plan = rebuildPlan,
            tasks = allTasks.filter { it.status == TaskStatus.ACTIVE && it.remainingMinutes > 0 },
        )
        return true
    }

    private suspend fun buildSchedulePlan(
        tasks: List<ScheduleTask>,
        existingBlocks: List<dev.codex.reclaimoss.domain.model.ScheduleBlock>,
        timePeriods: List<TimePeriod>,
        rangeStart: Instant,
        extraBusyWindows: List<SchedulerEngine.BusyWindow>,
        preserveExistingPendingBlocks: Boolean,
    ): dev.codex.reclaimoss.domain.model.SchedulePlan {
        val policy = schedulingPolicy(getSettings())
        return scheduler.rebuildSchedule(
            tasks = tasks,
            existingBlocks = existingBlocks,
            busyWindows = calendarGateway.syncBusyEvents(
                rangeStart,
                rangeStart.plusSeconds(60L * 60L * 24L * policy.lookAheadDays),
            ) + lifePeriodBusyWindows(
                timePeriods = timePeriods,
                rangeStart = rangeStart,
                rangeEnd = rangeStart.plusSeconds(60L * 60L * 24L * policy.lookAheadDays),
            ) + extraBusyWindows,
            workHours = workHoursFromProductivePeriods(timePeriods),
            timePeriods = timePeriods.filter { it.type == TimePeriodType.PRODUCTIVE },
            policy = policy,
            rangeStart = rangeStart,
            reason = ScheduleRebuildReason.ManualRebuild,
            preserveExistingPendingBlocks = preserveExistingPendingBlocks,
        )
    }

    private suspend fun applyPlanForTasks(
        plan: dev.codex.reclaimoss.domain.model.SchedulePlan,
        tasks: List<ScheduleTask>,
    ) {
        tasks.forEach { task ->
            repository.replaceFlexibleBlocks(task.id, plan.blocks.filter { it.taskId == task.id })
            repository.replaceSchedulingIssuesForTask(task.id, plan.issues.filter { it.taskId == task.id })
            syncLinkedReminderForTask(task.id)
        }
        calendarGateway.syncPlannedBlocks(plan.blocks)
    }

    private suspend fun syncLinkedReminderForTask(taskId: String) {
        val task = repository.getTasks().firstOrNull { it.id == taskId } ?: return
        val reminder = repository.getReminders()
            .firstOrNull { it.linkedTaskId == taskId && it.status != ReminderStatus.COMPLETED }
            ?: return
        repository.upsertReminder(
            reminder.copy(
                title = task.title,
                description = task.description,
                dueAt = reminderDueAtForTask(task),
                recurrenceRule = task.recurrenceRule,
                updatedAt = now(),
            ),
        )
    }

    private suspend fun placeExactTask(taskId: String) {
        val task = repository.getTasks().firstOrNull { it.id == taskId } ?: return
        if (task.schedulingMode != TaskSchedulingMode.FIXED_EXACT) return
        repository.clearAllPendingBlocks(taskId)
        val startAt = task.fixedStartAt
        val endAt = task.fixedEndAt
        if (startAt == null || endAt == null || !endAt.isAfter(startAt)) {
            repository.replaceSchedulingIssuesForTask(
                taskId,
                listOf(
                    dev.codex.reclaimoss.domain.model.SchedulingIssue(
                        taskId = taskId,
                        type = SchedulingIssueType.UNSCHEDULED,
                        unscheduledMinutes = task.remainingMinutes,
                        reason = "Exact-time task is missing a valid start and end time.",
                    ),
                ),
            )
            return
        }
        val rangeStart = startAt.minus(1, ChronoUnit.DAYS)
        val rangeEnd = endAt.plus(1, ChronoUnit.DAYS)
        val timePeriods = repository.getTimePeriods()
        val hardBusyWindows = calendarGateway.syncBusyEvents(rangeStart, rangeEnd) +
            lifePeriodBusyWindows(timePeriods, rangeStart, rangeEnd) +
            repository.getBlocks()
                .filter { it.taskId != taskId }
                .filter { it.lockState == BlockLockState.LOCKED || it.completionState == BlockCompletionState.COMPLETED }
                .map { SchedulerEngine.BusyWindow(it.startAt, it.endAt) }
        val otherTaskBusyWindows = repository.getBlocks()
            .filter { it.taskId != taskId }
            .filter { it.lockState != BlockLockState.LOCKED && it.completionState != BlockCompletionState.COMPLETED }
            .map { SchedulerEngine.BusyWindow(it.startAt, it.endAt) }
        val allowConcurrent = getSettings().allowConcurrentTasks
        val overlapsHardBlock = hardBusyWindows.any { it.startAt < endAt && it.endAt > startAt }
        val overlapsOtherTask = otherTaskBusyWindows.any { it.startAt < endAt && it.endAt > startAt }
        if (overlapsHardBlock || (!allowConcurrent && overlapsOtherTask)) {
            repository.replaceSchedulingIssuesForTask(
                taskId,
                listOf(
                    SchedulingIssue(
                        taskId = taskId,
                        type = SchedulingIssueType.UNSCHEDULED,
                        unscheduledMinutes = task.remainingMinutes,
                        reason = when {
                            overlapsHardBlock -> "This fixed time overlaps a break, sleep, or other blocked time."
                            else -> "This fixed time overlaps another task."
                        },
                    ),
                ),
            )
            return
        }
        repository.replaceFlexibleBlocks(
            taskId,
            listOf(
                dev.codex.reclaimoss.domain.model.ScheduleBlock(
                    id = "exact-$taskId",
                    taskId = taskId,
                    startAt = startAt,
                    endAt = endAt,
                    source = dev.codex.reclaimoss.domain.model.BlockSource.MANUAL,
                    lockState = BlockLockState.LOCKED,
                    completionState = BlockCompletionState.PENDING,
                    externalCalendarEventId = null,
                ),
            ),
        )
        repository.replaceSchedulingIssuesForTask(taskId, emptyList())
    }

    private fun planSchedulesTaskCleanly(
        plan: dev.codex.reclaimoss.domain.model.SchedulePlan,
        taskId: String,
    ): Boolean {
        val hasBlock = plan.blocks.any { it.taskId == taskId }
        val hasIssue = plan.issues.any { it.taskId == taskId }
        return hasBlock && !hasIssue
    }

    private suspend fun completeRecurringOccurrence(task: ScheduleTask) {
        repository.upsertTask(
            task.copy(
                remainingMinutes = 0,
                status = TaskStatus.COMPLETED,
                updatedAt = now(),
            ),
        )
        materializeFutureOccurrences(task)
        rebuildSchedule()
    }

    private fun materializedOccurrences(
        initialDueAt: Instant,
        recurrenceRule: RecurrenceRule,
        schedulingMode: TaskSchedulingMode,
        fixedStartAt: Instant?,
        fixedEndAt: Instant?,
    ): List<TaskOccurrence> {
        if (recurrenceRule.type == RecurrenceType.NONE) {
            return listOf(TaskOccurrence(initialDueAt, fixedStartAt, fixedEndAt))
        }

        val zoneId = zoneId()
        val horizonEnd = recurrenceHorizonEnd(recurrenceRule)
        val initial = initialDueAt.atZone(zoneId)
        val fixedStartLocal = fixedStartAt?.atZone(zoneId)
        val fixedEndLocal = fixedEndAt?.atZone(zoneId)
        var emitted = 0
        return when (recurrenceRule.type) {
            RecurrenceType.NONE -> listOf(TaskOccurrence(initialDueAt, fixedStartAt, fixedEndAt))
            RecurrenceType.DAILY -> {
                buildList {
                    var candidate = initial
                    while (!candidate.toInstant().isAfter(horizonEnd) && withinOccurrenceLimit(recurrenceRule, emitted)) {
                        if (!candidate.toInstant().isBefore(initialDueAt)) {
                            add(
                                TaskOccurrence(
                                    dueAt = candidate.toInstant(),
                                    fixedStartAt = if ((schedulingMode == TaskSchedulingMode.FIXED_EXACT || schedulingMode == TaskSchedulingMode.FLEXIBLE_WINDOW) && fixedStartLocal != null) {
                                        ZonedDateTime.of(candidate.toLocalDate(), fixedStartLocal.toLocalTime(), zoneId).toInstant()
                                    } else {
                                        null
                                    },
                                    fixedEndAt = if ((schedulingMode == TaskSchedulingMode.FIXED_EXACT || schedulingMode == TaskSchedulingMode.FLEXIBLE_WINDOW) && fixedEndLocal != null) {
                                        ZonedDateTime.of(candidate.toLocalDate(), fixedEndLocal.toLocalTime(), zoneId).toInstant()
                                    } else {
                                        null
                                    },
                                ),
                            )
                            emitted += 1
                        }
                        candidate = candidate.plusDays(recurrenceRule.interval.coerceAtLeast(1).toLong())
                    }
                }
            }
            RecurrenceType.WEEKLY -> {
                val repeatDays = recurrenceRule.daysOfWeek.ifEmpty { setOf(initial.dayOfWeek) }
                buildList {
                    val intervalWeeks = recurrenceRule.interval.coerceAtLeast(1).toLong()
                    val sortedDays = repeatDays.sortedBy { it.value }
                    var weekStart = initial.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    while (!weekStart.atTime(initial.toLocalTime()).atZone(zoneId).toInstant().isAfter(horizonEnd) && withinOccurrenceLimit(recurrenceRule, emitted)) {
                        for (day in sortedDays) {
                            if (!withinOccurrenceLimit(recurrenceRule, emitted)) break
                            val candidateDate = weekStart.plusDays((day.value - DayOfWeek.MONDAY.value).toLong())
                            val candidate = ZonedDateTime.of(candidateDate, initial.toLocalTime(), zoneId).toInstant()
                            if (candidate.isBefore(initialDueAt) || candidate.isAfter(horizonEnd)) continue
                            add(
                                TaskOccurrence(
                                    dueAt = candidate,
                                    fixedStartAt = if ((schedulingMode == TaskSchedulingMode.FIXED_EXACT || schedulingMode == TaskSchedulingMode.FLEXIBLE_WINDOW) && fixedStartLocal != null) {
                                        ZonedDateTime.of(candidateDate, fixedStartLocal.toLocalTime(), zoneId).toInstant()
                                    } else {
                                        null
                                    },
                                    fixedEndAt = if ((schedulingMode == TaskSchedulingMode.FIXED_EXACT || schedulingMode == TaskSchedulingMode.FLEXIBLE_WINDOW) && fixedEndLocal != null) {
                                        ZonedDateTime.of(candidateDate, fixedEndLocal.toLocalTime(), zoneId).toInstant()
                                    } else {
                                        null
                                    },
                                ),
                            )
                            emitted += 1
                        }
                        weekStart = weekStart.plusWeeks(intervalWeeks)
                    }
                }
            }
            RecurrenceType.MONTHLY -> {
                buildList {
                    var candidateDate = initial.toLocalDate()
                    while (!candidateDate.atTime(initial.toLocalTime()).atZone(zoneId).toInstant().isAfter(horizonEnd) && withinOccurrenceLimit(recurrenceRule, emitted)) {
                        val candidate = ZonedDateTime.of(candidateDate, initial.toLocalTime(), zoneId).toInstant()
                        if (!candidate.isBefore(initialDueAt)) {
                            add(
                                TaskOccurrence(
                                    dueAt = candidate,
                                    fixedStartAt = if ((schedulingMode == TaskSchedulingMode.FIXED_EXACT || schedulingMode == TaskSchedulingMode.FLEXIBLE_WINDOW) && fixedStartLocal != null) {
                                        ZonedDateTime.of(candidateDate, fixedStartLocal.toLocalTime(), zoneId).toInstant()
                                    } else {
                                        null
                                    },
                                    fixedEndAt = if ((schedulingMode == TaskSchedulingMode.FIXED_EXACT || schedulingMode == TaskSchedulingMode.FLEXIBLE_WINDOW) && fixedEndLocal != null) {
                                        ZonedDateTime.of(candidateDate, fixedEndLocal.toLocalTime(), zoneId).toInstant()
                                    } else {
                                        null
                                    },
                                ),
                            )
                            emitted += 1
                        }
                        val nextMonth = candidateDate.plusMonths(recurrenceRule.interval.coerceAtLeast(1).toLong())
                        candidateDate = nextMonth.withDayOfMonth(minOf(initial.dayOfMonth, nextMonth.lengthOfMonth()))
                    }
                }
            }
        }
    }

    private suspend fun materializeFutureOccurrences(task: ScheduleTask) {
        val seriesId = task.recurrenceSeriesId ?: return
        if (task.recurrenceRule.type == RecurrenceType.NONE) return

        val allTasks = repository.getTasks().filter { it.recurrenceSeriesId == seriesId }
        val horizonEnd = recurrenceHorizonEnd(task.recurrenceRule)
        var latestDueAt = allTasks.maxOfOrNull { it.dueAt } ?: task.dueAt
        val existingDueAts = allTasks.map { it.dueAt }.toMutableSet()
        var nextDueAt = nextOccurrence(task.copy(dueAt = latestDueAt))
        while (!nextDueAt.isAfter(horizonEnd)) {
            if (existingDueAts.add(nextDueAt)) {
                repository.upsertTask(
                    task.copy(
                        id = "${newId("task")}-${existingDueAts.size}",
                        dueAt = nextDueAt,
                        fixedStartAt = task.fixedStartAt?.let {
                            val local = it.atZone(zoneId())
                            ZonedDateTime.of(nextDueAt.atZone(zoneId()).toLocalDate(), local.toLocalTime(), zoneId()).toInstant()
                        },
                        fixedEndAt = task.fixedEndAt?.let {
                            val local = it.atZone(zoneId())
                            ZonedDateTime.of(nextDueAt.atZone(zoneId()).toLocalDate(), local.toLocalTime(), zoneId()).toInstant()
                        },
                        remainingMinutes = task.estimatedMinutes,
                        status = TaskStatus.ACTIVE,
                        updatedAt = now(),
                    ),
                )
            }
            latestDueAt = nextDueAt
            nextDueAt = nextOccurrence(task.copy(dueAt = latestDueAt))
        }
    }

    private fun recurrenceHorizonEnd(recurrenceRule: RecurrenceRule): Instant {
        val rollingHorizon = now().plus(recurrenceMaterializationDays.toLong(), ChronoUnit.DAYS)
        val until = recurrenceRule.until ?: return rollingHorizon
        return if (until.isBefore(rollingHorizon)) until else rollingHorizon
    }

    private fun workHoursFromProductivePeriods(timePeriods: List<TimePeriod>): WorkHoursProfile {
        val productiveWindows = timePeriods
            .filter { it.type == TimePeriodType.PRODUCTIVE }
            .map { TimeWindow(it.start, it.end) }
            .sortedBy { it.start }
        if (productiveWindows.isEmpty()) return emptyWorkHours

        return WorkHoursProfile(
            timezone = zoneId().id,
            days = DayOfWeek.entries.associateWith { WorkHoursDay(productiveWindows) },
        )
    }

    private fun lifePeriodBusyWindows(
        timePeriods: List<TimePeriod>,
        rangeStart: Instant,
        rangeEnd: Instant,
    ): List<SchedulerEngine.BusyWindow> {
        val zoneId = zoneId()
        val startDate = rangeStart.atZone(zoneId).toLocalDate()
        val endDate = rangeEnd.atZone(zoneId).toLocalDate()
        val windows = mutableListOf<SchedulerEngine.BusyWindow>()
        var date: LocalDate = startDate
        while (!date.isAfter(endDate)) {
            timePeriods
                .filter { it.type == TimePeriodType.LIFE }
                .forEach { period ->
                    val start = ZonedDateTime.of(date, period.start, zoneId).toInstant()
                    val endDateTime = if (period.end > period.start) {
                        ZonedDateTime.of(date, period.end, zoneId)
                    } else {
                        ZonedDateTime.of(date.plusDays(1), period.end, zoneId)
                    }
                    val end = endDateTime.toInstant()
                    if (end > rangeStart && start < rangeEnd) {
                        windows += SchedulerEngine.BusyWindow(start, end)
                    }
                }
            date = date.plusDays(1)
        }
        return windows
    }

    private fun nextOccurrence(task: ScheduleTask): Instant {
        val zoneId = zoneId()
        val current = task.dueAt.atZone(zoneId)
        return when (task.recurrenceRule.type) {
            RecurrenceType.NONE -> task.dueAt
            RecurrenceType.DAILY -> current.plusDays(task.recurrenceRule.interval.coerceAtLeast(1).toLong()).toInstant()
            RecurrenceType.WEEKLY -> {
                val repeatDays = task.recurrenceRule.daysOfWeek.ifEmpty { setOf(current.dayOfWeek) }
                val sortedDays = repeatDays.sortedBy { it.value }
                val currentWeekStart = current.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val currentDayOrder = current.dayOfWeek.value
                sortedDays.firstOrNull { it.value > currentDayOrder }?.let { nextDay ->
                    val candidateDate = currentWeekStart.plusDays((nextDay.value - DayOfWeek.MONDAY.value).toLong())
                    return ZonedDateTime.of(candidateDate, current.toLocalTime(), zoneId).toInstant()
                }
                val nextWeekStart = currentWeekStart.plusWeeks(task.recurrenceRule.interval.coerceAtLeast(1).toLong())
                val firstDay = sortedDays.first()
                val candidateDate = nextWeekStart.plusDays((firstDay.value - DayOfWeek.MONDAY.value).toLong())
                ZonedDateTime.of(candidateDate, current.toLocalTime(), zoneId).toInstant()
            }
            RecurrenceType.MONTHLY -> {
                val nextMonth = current.toLocalDate().plusMonths(task.recurrenceRule.interval.coerceAtLeast(1).toLong())
                val nextDay = minOf(current.dayOfMonth, nextMonth.lengthOfMonth())
                ZonedDateTime.of(nextMonth.withDayOfMonth(nextDay), current.toLocalTime(), zoneId).toInstant()
            }
        }
    }

    private fun withinOccurrenceLimit(recurrenceRule: RecurrenceRule, emitted: Int): Boolean =
        recurrenceRule.endMode != RecurrenceEndMode.AFTER_OCCURRENCES ||
            emitted < (recurrenceRule.occurrenceCount ?: Int.MAX_VALUE)

    private fun nextFutureOccurrence(task: ScheduleTask): Instant {
        var candidateTask = task
        var candidate = nextOccurrence(candidateTask)
        val now = now()
        while (!candidate.isAfter(now) && candidate != candidateTask.dueAt) {
            candidateTask = candidateTask.copy(dueAt = candidate)
            candidate = nextOccurrence(candidateTask)
        }
        return candidate
    }

    private fun effectiveRescheduleDueAt(task: ScheduleTask, requestedDueAt: Instant?): Instant {
        if (requestedDueAt != null) return requestedDueAt
        val now = now()
        if (task.dueAt.isAfter(now)) return task.dueAt

        val zoneId = zoneId()
        var candidate = task.dueAt.atZone(zoneId)
        val currentTime = candidate.toLocalTime()
        val currentDate = now.atZone(zoneId).toLocalDate()
        candidate = ZonedDateTime.of(currentDate, currentTime, zoneId)
        while (!candidate.toInstant().isAfter(now)) {
            candidate = candidate.plusDays(1)
        }
        return candidate.toInstant()
    }

    private suspend fun reminderDueAtForTask(task: ScheduleTask): Instant {
        val settings = getSettings()
        val pendingBlocks = repository.getBlocks()
            .filter { it.taskId == task.id && it.completionState != BlockCompletionState.COMPLETED }
        return when (settings.reminderTimingMode) {
            ReminderTimingMode.AT_DUE_DATE -> if (task.hasDeadline) {
                task.dueAt
            } else {
                pendingBlocks.minOfOrNull { it.startAt } ?: task.dueAt
            }
            ReminderTimingMode.AT_TASK_TIME -> if (task.hasDeadline) {
                pendingBlocks.maxOfOrNull { it.endAt } ?: task.dueAt
            } else {
                pendingBlocks.minOfOrNull { it.startAt } ?: task.dueAt
            }
        }
    }

    private suspend fun taskResultFor(taskId: String): TaskCreationResult {
        val hasScheduledBlock = repository.getBlocks().any { it.taskId == taskId }
        val issue = repository.getSchedulingIssues().firstOrNull { it.taskId == taskId }
        return TaskCreationResult(
            taskId = taskId,
            scheduled = hasScheduledBlock && issue == null,
            partial = issue?.type == SchedulingIssueType.PARTIAL,
            reason = issue?.reason,
        )
    }

    private fun schedulingPolicy(settings: AppSettings) = SchedulingPolicy(
        minBlockMinutes = 30,
        maxBlockMinutes = settings.maxTaskChunkMinutes,
        breakBetweenBlocksMinutes = settings.breakBufferMinutes,
        priorityWeight = 1.5,
        deadlineUrgencyWeight = 2.0,
        lookAheadDays = 14,
        alignmentMinutes = settings.alignmentMinutes,
        allowTaskSplitting = settings.allowTaskSplitting,
        strictPreferredPeriod = settings.preferredPeriodFallbackMode == PreferredPeriodFallbackMode.STRICT,
        allowConcurrentTasks = settings.allowConcurrentTasks,
    )

    private fun newId(prefix: String): String = "$prefix-${UUID.randomUUID()}"

    private fun now(): Instant = clock.instant()

    private fun zoneId(): ZoneId = clock.zone

    private suspend fun requireValidContinuationParent(
        continuationParentTaskId: String?,
        currentTaskId: String? = null,
    ) {
        if (continuationParentTaskId == null) return
        require(continuationParentTaskId != currentTaskId) { "A task cannot continue after itself." }
        val parentExists = repository.getTasks().any { it.id == continuationParentTaskId }
        require(parentExists) { "The selected parent task no longer exists." }
    }
}
