package dev.codex.reclaimoss.data.repository

import dev.codex.reclaimoss.data.local.ProjectDao
import dev.codex.reclaimoss.data.local.ProjectEntity
import dev.codex.reclaimoss.data.local.ReminderDao
import dev.codex.reclaimoss.data.local.ReminderEntity
import dev.codex.reclaimoss.data.local.ScheduleBlockDao
import dev.codex.reclaimoss.data.local.ScheduleBlockEntity
import dev.codex.reclaimoss.data.local.SchedulingIssueDao
import dev.codex.reclaimoss.data.local.SchedulingIssueEntity
import dev.codex.reclaimoss.data.local.TaskDao
import dev.codex.reclaimoss.data.local.TaskEntity
import dev.codex.reclaimoss.data.local.TimePeriodDao
import dev.codex.reclaimoss.data.local.TimePeriodEntity
import dev.codex.reclaimoss.domain.model.BlockCompletionState
import dev.codex.reclaimoss.domain.model.BlockLockState
import dev.codex.reclaimoss.domain.model.PreferredTimeOfDay
import dev.codex.reclaimoss.domain.model.Project
import dev.codex.reclaimoss.domain.model.RecurrenceRule
import dev.codex.reclaimoss.domain.model.RecurrenceType
import dev.codex.reclaimoss.domain.model.Reminder
import dev.codex.reclaimoss.domain.model.ReminderStatus
import dev.codex.reclaimoss.domain.model.ScheduleBlock
import dev.codex.reclaimoss.domain.model.ScheduleTask
import dev.codex.reclaimoss.domain.model.SchedulingIssue
import dev.codex.reclaimoss.domain.model.TaskPriority
import dev.codex.reclaimoss.domain.model.TaskStatus
import dev.codex.reclaimoss.domain.model.TimePeriod
import dev.codex.reclaimoss.domain.model.TimePeriodType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class PlannerSnapshot(
    val projects: List<Project>,
    val tasks: List<ScheduleTask>,
    val blocks: List<ScheduleBlock>,
    val timePeriods: List<TimePeriod>,
    val reminders: List<Reminder> = emptyList(),
    val schedulingIssues: List<SchedulingIssue> = emptyList(),
)

interface PlannerRepository {
    fun observeSnapshot(): Flow<PlannerSnapshot>
    suspend fun upsertProject(project: Project)
    suspend fun upsertTask(task: ScheduleTask)
    suspend fun getTasks(): List<ScheduleTask>
    suspend fun updateTaskDueDate(taskId: String, dueAt: Instant)
    suspend fun updateTaskPriority(taskId: String, priority: TaskPriority)
    suspend fun updateTaskEstimatedMinutes(taskId: String, estimatedMinutes: Int)
    suspend fun getBlocks(): List<ScheduleBlock>
    suspend fun replaceFlexibleBlocks(taskId: String, blocks: List<ScheduleBlock>)
    suspend fun updateBlockLock(blockId: String, lockState: BlockLockState)
    suspend fun updateBlockCompletion(blockId: String, completionState: BlockCompletionState)
    suspend fun updateTaskRemaining(taskId: String, remainingMinutes: Int)
    suspend fun deleteTask(taskId: String)
    suspend fun clearAllPendingBlocks(taskId: String)
    suspend fun getTimePeriods(): List<TimePeriod>
    suspend fun upsertTimePeriod(period: TimePeriod)
    suspend fun deleteTimePeriod(periodId: String)
    suspend fun upsertReminder(reminder: Reminder)
    suspend fun deleteReminder(reminderId: String)
    suspend fun getReminders(): List<Reminder>
    fun observeReminders(): Flow<List<Reminder>>
    suspend fun getSchedulingIssues(): List<SchedulingIssue>
    suspend fun replaceSchedulingIssuesForTask(taskId: String, issues: List<SchedulingIssue>)
    suspend fun seedDemoDataIfEmpty()
}

class PlannerRepositoryImpl(
    private val projectDao: ProjectDao,
    private val taskDao: TaskDao,
    private val scheduleBlockDao: ScheduleBlockDao,
    private val timePeriodDao: TimePeriodDao,
    private val reminderDao: ReminderDao,
    private val schedulingIssueDao: SchedulingIssueDao,
) : PlannerRepository {
    override fun observeSnapshot(): Flow<PlannerSnapshot> =
        combine(
            projectDao.observeProjects().map { items -> items.map { it.toDomain() } },
            taskDao.observeTasks().map { items -> items.map { it.toDomain() } },
            scheduleBlockDao.observeBlocks().map { items -> items.map { it.toDomain() } },
            timePeriodDao.observeTimePeriods().map { items -> items.map { it.toDomain() } },
            reminderDao.observeReminders().map { items -> items.map { it.toDomain() } },
            schedulingIssueDao.observeIssues().map { items -> items.map { it.toDomain() } },
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            PlannerSnapshot(
                projects = values[0] as List<Project>,
                tasks = values[1] as List<ScheduleTask>,
                blocks = values[2] as List<ScheduleBlock>,
                timePeriods = values[3] as List<TimePeriod>,
                reminders = values[4] as List<Reminder>,
                schedulingIssues = values[5] as List<SchedulingIssue>,
            )
        }

    override suspend fun upsertProject(project: Project) {
        projectDao.upsert(
            ProjectEntity(
                id = project.id,
                name = project.name,
                colorHex = project.colorHex,
                defaultPriority = project.defaultPriority,
                archived = project.archived,
            ),
        )
    }

    override suspend fun upsertTask(task: ScheduleTask) {
        taskDao.upsert(task.toEntity())
    }

    override suspend fun getTasks(): List<ScheduleTask> = taskDao.getAll().map { it.toDomain() }

    override suspend fun updateTaskDueDate(taskId: String, dueAt: Instant) {
        val task = getTasks().firstOrNull { it.id == taskId } ?: return
        upsertTask(task.copy(dueAt = dueAt, updatedAt = Instant.now()))
    }

    override suspend fun updateTaskPriority(taskId: String, priority: TaskPriority) {
        val task = getTasks().firstOrNull { it.id == taskId } ?: return
        upsertTask(task.copy(priority = priority, updatedAt = Instant.now()))
    }

    override suspend fun updateTaskEstimatedMinutes(taskId: String, estimatedMinutes: Int) {
        val task = getTasks().firstOrNull { it.id == taskId } ?: return
        upsertTask(
            task.copy(
                estimatedMinutes = estimatedMinutes,
                remainingMinutes = estimatedMinutes,
                updatedAt = Instant.now(),
            ),
        )
    }

    override suspend fun getBlocks(): List<ScheduleBlock> = scheduleBlockDao.getAll().map { it.toDomain() }

    override suspend fun replaceFlexibleBlocks(taskId: String, blocks: List<ScheduleBlock>) {
        scheduleBlockDao.deleteFlexiblePendingBlocksForTask(taskId)
        scheduleBlockDao.upsertAll(blocks.map { it.toEntity() })
    }

    override suspend fun updateBlockLock(blockId: String, lockState: BlockLockState) {
        scheduleBlockDao.updateLockState(blockId, lockState)
    }

    override suspend fun updateBlockCompletion(blockId: String, completionState: BlockCompletionState) {
        scheduleBlockDao.updateCompletionState(blockId, completionState)
    }

    override suspend fun updateTaskRemaining(taskId: String, remainingMinutes: Int) {
        taskDao.updateRemainingMinutes(taskId, remainingMinutes, Instant.now().toEpochMilli())
    }

    override suspend fun deleteTask(taskId: String) {
        scheduleBlockDao.deleteAllForTask(taskId)
        taskDao.deleteTask(taskId)
    }

    override suspend fun clearAllPendingBlocks(taskId: String) {
        scheduleBlockDao.deletePendingForTask(taskId)
    }

    override suspend fun getTimePeriods(): List<TimePeriod> = timePeriodDao.getAll().map { it.toDomain() }

    override suspend fun upsertTimePeriod(period: TimePeriod) {
        timePeriodDao.upsert(period.toEntity())
    }

    override suspend fun deleteTimePeriod(periodId: String) {
        taskDao.clearPreferredTimePeriod(periodId)
        timePeriodDao.delete(periodId)
    }

    override suspend fun upsertReminder(reminder: Reminder) {
        reminderDao.upsert(reminder.toEntity())
    }

    override suspend fun deleteReminder(reminderId: String) {
        reminderDao.delete(reminderId)
    }

    override suspend fun getReminders(): List<Reminder> = reminderDao.getAll().map { it.toDomain() }

    override fun observeReminders(): Flow<List<Reminder>> =
        reminderDao.observeReminders().map { items -> items.map { it.toDomain() } }

    override suspend fun getSchedulingIssues(): List<SchedulingIssue> =
        schedulingIssueDao.getAll().map { it.toDomain() }

    override suspend fun replaceSchedulingIssuesForTask(taskId: String, issues: List<SchedulingIssue>) {
        schedulingIssueDao.deleteForTask(taskId)
        schedulingIssueDao.upsertAll(issues.map { it.toEntity() })
    }

    override suspend fun seedDemoDataIfEmpty() {
        if (timePeriodDao.getAll().isEmpty()) {
            listOf(
                TimePeriod("period-sleep-start", "Sleep", LocalTime.MIDNIGHT, LocalTime.of(6, 0), type = TimePeriodType.LIFE, sortOrder = 0),
                TimePeriod("period-morning", "Morning", LocalTime.of(8, 0), LocalTime.of(12, 0), type = TimePeriodType.PRODUCTIVE, sortOrder = 1),
                TimePeriod("period-lunch", "Lunch", LocalTime.of(12, 0), LocalTime.of(13, 0), type = TimePeriodType.LIFE, sortOrder = 2),
                TimePeriod("period-midday", "Midday Focus", LocalTime.of(13, 0), LocalTime.of(14, 0), type = TimePeriodType.PRODUCTIVE, sortOrder = 3),
                TimePeriod("period-afternoon", "Afternoon", LocalTime.of(14, 0), LocalTime.of(17, 0), type = TimePeriodType.PRODUCTIVE, sortOrder = 4),
                TimePeriod("period-rest", "Rest", LocalTime.of(17, 0), LocalTime.of(18, 0), type = TimePeriodType.LIFE, sortOrder = 5),
                TimePeriod("period-dinner", "Dinner", LocalTime.of(19, 0), LocalTime.of(20, 0), type = TimePeriodType.LIFE, sortOrder = 6),
                TimePeriod("period-night", "Night", LocalTime.of(20, 0), LocalTime.of(22, 0), type = TimePeriodType.PRODUCTIVE, sortOrder = 7),
                TimePeriod("period-sleep-end", "Sleep", LocalTime.of(22, 0), LocalTime.MAX, type = TimePeriodType.LIFE, sortOrder = 8),
            ).forEachIndexed { index, period ->
                timePeriodDao.upsert(period.copy(sortOrder = index).toEntity())
            }
        }
        if (taskDao.getAll().isNotEmpty()) return

        upsertProject(
            Project(
                id = "project-default",
                name = "Personal Ops",
                colorHex = "#2B5566",
                defaultPriority = TaskPriority.MEDIUM,
            ),
        )
        upsertTask(
            ScheduleTask(
                id = "task-demo-1",
                projectId = "project-default",
                title = "Ship v1 scheduler",
                description = "Finish the deterministic planner and reminders.",
                priority = TaskPriority.HIGH,
                preferredTimeOfDay = PreferredTimeOfDay.MORNING,
                preferredTimePeriodId = "period-morning",
                dueAt = Instant.now().plusSeconds(60L * 60L * 36L),
                estimatedMinutes = 180,
                remainingMinutes = 180,
                recurrenceRule = RecurrenceRule(RecurrenceType.NONE),
                status = TaskStatus.ACTIVE,
            ),
        )
        upsertTask(
            ScheduleTask(
                id = "task-demo-2",
                projectId = "project-default",
                title = "Review project notes",
                description = "Clean up requirements and backlog items.",
                priority = TaskPriority.MEDIUM,
                preferredTimeOfDay = PreferredTimeOfDay.AFTERNOON,
                preferredTimePeriodId = "period-afternoon",
                dueAt = Instant.now().plusSeconds(60L * 60L * 72L),
                estimatedMinutes = 90,
                remainingMinutes = 90,
                recurrenceRule = RecurrenceRule(RecurrenceType.WEEKLY, setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)),
                status = TaskStatus.ACTIVE,
            ),
        )
    }
}

private fun ProjectEntity.toDomain() = Project(
    id = id,
    name = name,
    colorHex = colorHex,
    defaultPriority = defaultPriority,
    archived = archived,
)

private fun TaskEntity.toDomain() = ScheduleTask(
    id = id,
    recurrenceSeriesId = recurrenceSeriesId,
    projectId = projectId,
    title = title,
    description = description,
    priority = priority,
    preferredTimeOfDay = preferredTimeOfDay,
    preferredTimePeriodId = preferredTimePeriodId,
    dueAt = Instant.ofEpochMilli(dueAtEpochMillis),
    estimatedMinutes = estimatedMinutes,
    remainingMinutes = remainingMinutes,
    recurrenceRule = RecurrenceRule(
        type = recurrenceType,
        daysOfWeek = recurrenceDaysCsv
            .split(',')
            .filter { it.isNotBlank() }
            .map { DayOfWeek.valueOf(it) }
            .toSet(),
    ),
    status = status,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
)

private fun ScheduleTask.toEntity() = TaskEntity(
    id = id,
    recurrenceSeriesId = recurrenceSeriesId,
    projectId = projectId,
    title = title,
    description = description,
    priority = priority,
    preferredTimeOfDay = preferredTimeOfDay,
    preferredTimePeriodId = preferredTimePeriodId,
    dueAtEpochMillis = dueAt.toEpochMilli(),
    estimatedMinutes = estimatedMinutes,
    remainingMinutes = remainingMinutes,
    recurrenceType = recurrenceRule.type,
    recurrenceDaysCsv = recurrenceRule.daysOfWeek.joinToString(",") { it.name },
    status = status,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
)

private fun TimePeriodEntity.toDomain() = TimePeriod(
    id = id,
    label = label,
    start = startTime,
    end = endTime,
    type = type,
    sortOrder = sortOrder,
)

private fun TimePeriod.toEntity() = TimePeriodEntity(
    id = id,
    label = label,
    startTime = start,
    endTime = end,
    type = type,
    sortOrder = sortOrder,
)

private fun ReminderEntity.toDomain() = Reminder(
    id = id,
    title = title,
    description = description,
    dueAt = Instant.ofEpochMilli(dueAtEpochMillis),
    recurrenceRule = RecurrenceRule(
        type = recurrenceType,
        daysOfWeek = recurrenceDaysCsv
            .split(',')
            .filter { it.isNotBlank() }
            .map { DayOfWeek.valueOf(it) }
            .toSet(),
    ),
    linkedTaskId = linkedTaskId,
    status = status,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
)

private fun Reminder.toEntity() = ReminderEntity(
    id = id,
    title = title,
    description = description,
    dueAtEpochMillis = dueAt.toEpochMilli(),
    recurrenceType = recurrenceRule.type,
    recurrenceDaysCsv = recurrenceRule.daysOfWeek.joinToString(",") { it.name },
    linkedTaskId = linkedTaskId,
    status = status,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
)

private fun SchedulingIssueEntity.toDomain() = SchedulingIssue(
    taskId = taskId,
    type = type,
    unscheduledMinutes = unscheduledMinutes,
    reason = reason,
)

private fun SchedulingIssue.toEntity() = SchedulingIssueEntity(
    id = taskId,
    taskId = taskId,
    type = type,
    unscheduledMinutes = unscheduledMinutes,
    reason = reason,
)

private fun ScheduleBlockEntity.toDomain() = ScheduleBlock(
    id = id,
    taskId = taskId,
    startAt = Instant.ofEpochMilli(startAtEpochMillis),
    endAt = Instant.ofEpochMilli(endAtEpochMillis),
    source = source,
    lockState = lockState,
    completionState = completionState,
    externalCalendarEventId = externalCalendarEventId,
)

private fun ScheduleBlock.toEntity() = ScheduleBlockEntity(
    id = id,
    taskId = taskId,
    startAtEpochMillis = startAt.toEpochMilli(),
    endAtEpochMillis = endAt.toEpochMilli(),
    source = source,
    lockState = lockState,
    completionState = completionState,
    externalCalendarEventId = externalCalendarEventId,
)
