package dev.codex.reclaimoss.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.codex.reclaimoss.AppGraph
import dev.codex.reclaimoss.data.repository.PlannerSnapshot
import dev.codex.reclaimoss.domain.model.BlockLockState
import dev.codex.reclaimoss.domain.model.PreferredTimeOfDay
import dev.codex.reclaimoss.domain.model.RecurrenceEndMode
import dev.codex.reclaimoss.domain.model.RecurrenceRule
import dev.codex.reclaimoss.domain.model.RecurrenceType
import dev.codex.reclaimoss.domain.model.Reminder
import dev.codex.reclaimoss.domain.model.ReminderStatus
import dev.codex.reclaimoss.domain.model.ScheduleBlock
import dev.codex.reclaimoss.domain.model.ScheduleTask
import dev.codex.reclaimoss.domain.model.TaskContinuationMode
import dev.codex.reclaimoss.domain.model.TaskSchedulingMode
import dev.codex.reclaimoss.domain.model.TaskPriority
import dev.codex.reclaimoss.domain.model.TaskStatus
import dev.codex.reclaimoss.domain.model.TimePeriod
import dev.codex.reclaimoss.domain.model.TimePeriodType
import dev.codex.reclaimoss.domain.scheduling.ScheduleRebuildReason
import dev.codex.reclaimoss.domain.service.PlannerCoordinator
import dev.codex.reclaimoss.domain.service.TaskCreationResult
import dev.codex.reclaimoss.settings.AppSettings
import dev.codex.reclaimoss.settings.AppSettingsRepository
import dev.codex.reclaimoss.settings.DateFormatPreference
import dev.codex.reclaimoss.settings.HistoryRetention
import dev.codex.reclaimoss.settings.PreferredPeriodFallbackMode
import dev.codex.reclaimoss.settings.ReminderTimingMode
import dev.codex.reclaimoss.settings.ThemeMode
import dev.codex.reclaimoss.settings.UrgentRescheduleMode
import dev.codex.reclaimoss.settings.WeekStart
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppTab(val label: String) {
    Tasks("Tasks"),
    Reminders("Reminders"),
    Planner("Planner"),
    Settings("Settings"),
}

enum class CreateMode(val label: String) {
    Task("Task"),
    Reminder("Reminder"),
}

val HeaderActionShape = RoundedCornerShape(22.dp)
val HeaderActionHeight = 44.dp
val HeaderActionWidth = 176.dp
val SurfaceTintStrong = Color(0xFFE9EEF9)
val CreateScreenSnackbarBottomOffset = 108.dp

data class PlannerUiState(
    val snapshot: PlannerSnapshot = PlannerSnapshot(
        projects = emptyList(),
        tasks = emptyList(),
        blocks = emptyList(),
        timePeriods = emptyList(),
    ),
    val settings: AppSettings = AppSettings(),
    val settingsLoaded: Boolean = false,
)

data class TaskDraft(
    val title: String = "",
    val description: String = "",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val preferredTimePeriodId: String? = null,
    val hasDeadline: Boolean = true,
    val continuationParentTaskId: String? = null,
    val continuationMode: TaskContinuationMode? = null,
    val deadline: LocalDateTime = LocalDateTime.now().plusDays(1).withHour(17).withMinute(0),
    val schedulingMode: TaskSchedulingMode = TaskSchedulingMode.FLEXIBLE,
    val startDate: LocalDate? = null,
    val fixedDate: LocalDate = LocalDate.now().plusDays(1),
    val fixedStartAt: LocalDateTime = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0),
    val fixedEndAt: LocalDateTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0),
    val repeatsForever: Boolean = true,
    val estimatedMinutes: Int = 60,
    val addReminder: Boolean = false,
    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    val recurrenceInterval: Int = 1,
    val recurrenceDays: Set<DayOfWeek> = emptySet(),
    val recurrenceOccurrenceLimit: Int? = null,
)

data class ReminderDraft(
    val title: String = "",
    val description: String = "",
    val dueAt: LocalDateTime = LocalDateTime.now().plusHours(1).withMinute(0),
    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    val recurrenceInterval: Int = 1,
    val recurrenceDays: Set<DayOfWeek> = emptySet(),
    val recurrenceOccurrenceLimit: Int? = null,
)

data class TimePeriodDraft(
    val id: String = "",
    val label: String = "",
    val start: LocalTime = LocalTime.of(9, 0),
    val end: LocalTime = LocalTime.of(12, 0),
    val boundStart: LocalTime = LocalTime.MIDNIGHT,
    val boundEnd: LocalTime = LocalTime.MIDNIGHT,
    val type: TimePeriodType = TimePeriodType.PRODUCTIVE,
    val sortOrder: Int = 0,
)

fun ScheduleTask.toFollowUpDraft(zoneId: ZoneId = ZoneId.systemDefault()): TaskDraft =
    TaskDraft(
        title = if (title.endsWith(" Follow up")) title else "$title Follow up",
        description = description,
        priority = priority,
        preferredTimePeriodId = preferredTimePeriodId,
        hasDeadline = true,
        continuationParentTaskId = null,
        continuationMode = null,
        schedulingMode = TaskSchedulingMode.FLEXIBLE,
        startDate = null,
        fixedDate = dueAt.atZone(zoneId).toLocalDate().plusDays(1),
        fixedStartAt = dueAt.atZone(zoneId).toLocalDateTime(),
        fixedEndAt = dueAt.atZone(zoneId).toLocalDateTime().plusMinutes(estimatedMinutes.toLong()),
        deadline = dueAt.atZone(zoneId).toLocalDateTime().plusDays(1),
        repeatsForever = true,
        estimatedMinutes = estimatedMinutes,
        addReminder = false,
        recurrenceType = RecurrenceType.NONE,
        recurrenceInterval = 1,
        recurrenceDays = emptySet(),
        recurrenceOccurrenceLimit = null,
    )

fun ScheduleTask.toRescheduleDraft(zoneId: ZoneId = ZoneId.systemDefault()): TaskDraft {
    val localDueAt = dueAt.atZone(zoneId).toLocalDateTime()
    val localFixedStart = fixedStartAt?.atZone(zoneId)?.toLocalDateTime()
        ?: localDueAt.minusMinutes(estimatedMinutes.toLong())
    val localFixedEnd = fixedEndAt?.atZone(zoneId)?.toLocalDateTime()
        ?: localDueAt
    return TaskDraft(
        title = if (title.endsWith(" Rescheduled")) title else "$title Rescheduled",
        description = description,
        priority = priority,
        preferredTimePeriodId = preferredTimePeriodId,
        hasDeadline = hasDeadline,
        continuationParentTaskId = continuationParentTaskId,
        continuationMode = continuationMode,
        deadline = localDueAt,
        schedulingMode = schedulingMode,
        startDate = if (schedulingMode == TaskSchedulingMode.FLEXIBLE || schedulingMode == TaskSchedulingMode.FLEXIBLE_WINDOW) {
            fixedStartAt?.atZone(zoneId)?.toLocalDate()
        } else {
            null
        },
        fixedDate = localDueAt.toLocalDate(),
        fixedStartAt = localFixedStart,
        fixedEndAt = localFixedEnd,
        repeatsForever = recurrenceRule.until == null,
        estimatedMinutes = estimatedMinutes,
        addReminder = false,
        recurrenceType = recurrenceRule.type,
        recurrenceInterval = recurrenceRule.interval,
        recurrenceDays = recurrenceRule.daysOfWeek,
        recurrenceOccurrenceLimit = recurrenceRule.occurrenceCount,
    )
}

class PlannerViewModel(
    private val coordinator: PlannerCoordinator,
    private val settingsRepository: AppSettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PlannerUiState())
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            coordinator.snapshot.collect { snapshot ->
                _uiState.value = _uiState.value.copy(snapshot = snapshot)
            }
        }
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.value = _uiState.value.copy(settings = settings, settingsLoaded = true)
            }
        }
    }

    suspend fun seedIfNeeded() = coordinator.ensureSeedData()

    suspend fun addTask(draft: TaskDraft, timePeriods: List<TimePeriod>): TaskCreationResult {
        return coordinator.createTask(
            title = draft.title,
            description = draft.description,
            priority = draft.priority,
            preferredTimePeriodId = draft.preferredTimePeriodId,
            hasDeadline = draft.hasDeadline,
            continuationParentTaskId = draft.continuationParentTaskId,
            continuationMode = draft.continuationMode,
            dueAt = draft.taskDueAtInstant(),
            recurrenceRule = RecurrenceRule(
                type = draft.recurrenceType,
                interval = draft.recurrenceInterval,
                daysOfWeek = if (draft.recurrenceType == RecurrenceType.WEEKLY) draft.recurrenceDays else emptySet(),
                until = draft.repeatDeadlineOrNull(),
                endMode = if (draft.recurrenceType == RecurrenceType.NONE || draft.repeatsForever) RecurrenceEndMode.NEVER else RecurrenceEndMode.ON_DATE,
                occurrenceCount = draft.recurrenceOccurrenceLimit,
            ),
            estimatedMinutes = draft.estimatedMinutes,
            addReminder = draft.addReminder,
            schedulingMode = draft.schedulingMode,
            fixedStartAt = draft.schedulingStartInstantOrNull(),
            fixedEndAt = draft.fixedEndAtInstantOrNull(),
        )
    }

    suspend fun addFollowUpTask(sourceTaskId: String, draft: TaskDraft): TaskCreationResult? {
        return coordinator.createFollowUpTask(
            sourceTaskId = sourceTaskId,
            title = draft.title,
            description = draft.description,
            priority = draft.priority,
            dueAt = draft.taskDueAtInstant(),
            preferredTimePeriodId = draft.preferredTimePeriodId,
            hasDeadline = draft.hasDeadline,
            continuationParentTaskId = draft.continuationParentTaskId,
            continuationMode = draft.continuationMode,
            recurrenceRule = RecurrenceRule(
                type = draft.recurrenceType,
                interval = draft.recurrenceInterval,
                daysOfWeek = if (draft.recurrenceType == RecurrenceType.WEEKLY) draft.recurrenceDays else emptySet(),
                until = draft.repeatDeadlineOrNull(),
                endMode = if (draft.recurrenceType == RecurrenceType.NONE || draft.repeatsForever) RecurrenceEndMode.NEVER else RecurrenceEndMode.ON_DATE,
                occurrenceCount = draft.recurrenceOccurrenceLimit,
            ),
            estimatedMinutes = draft.estimatedMinutes,
            addReminder = draft.addReminder,
            schedulingMode = draft.schedulingMode,
            fixedStartAt = draft.schedulingStartInstantOrNull(),
            fixedEndAt = draft.fixedEndAtInstantOrNull(),
        )
    }

    suspend fun rescheduleTaskWithUpdate(taskId: String, draft: TaskDraft): TaskCreationResult {
        return coordinator.rescheduleTaskWithUpdate(
            taskId = taskId,
            title = draft.title,
            description = draft.description,
            priority = draft.priority,
            dueAt = draft.taskDueAtInstant(),
            preferredTimePeriodId = draft.preferredTimePeriodId,
            hasDeadline = draft.hasDeadline,
            continuationParentTaskId = draft.continuationParentTaskId,
            continuationMode = draft.continuationMode,
            recurrenceRule = RecurrenceRule(
                type = draft.recurrenceType,
                interval = draft.recurrenceInterval,
                daysOfWeek = if (draft.recurrenceType == RecurrenceType.WEEKLY) draft.recurrenceDays else emptySet(),
                until = draft.repeatDeadlineOrNull(),
                endMode = if (draft.recurrenceType == RecurrenceType.NONE || draft.repeatsForever) RecurrenceEndMode.NEVER else RecurrenceEndMode.ON_DATE,
                occurrenceCount = draft.recurrenceOccurrenceLimit,
            ),
            estimatedMinutes = draft.estimatedMinutes,
            schedulingMode = draft.schedulingMode,
            fixedStartAt = draft.schedulingStartInstantOrNull(),
            fixedEndAt = draft.fixedEndAtInstantOrNull(),
        )
    }

    suspend fun addReminder(draft: ReminderDraft) {
        coordinator.createReminder(
            title = draft.title,
            description = draft.description,
            dueAt = draft.dueAt.atZone(ZoneId.systemDefault()).toInstant(),
            recurrenceRule = RecurrenceRule(
                type = draft.recurrenceType,
                interval = draft.recurrenceInterval,
                daysOfWeek = if (draft.recurrenceType == RecurrenceType.WEEKLY) draft.recurrenceDays else emptySet(),
                endMode = RecurrenceEndMode.NEVER,
                occurrenceCount = draft.recurrenceOccurrenceLimit,
            ),
        )
    }

    suspend fun dismissReminder(reminderId: String) {
        coordinator.dismissReminder(reminderId)
    }

    suspend fun rebuildSchedule() {
        coordinator.rebuildSchedule()
    }

    suspend fun toggleLock(block: ScheduleBlock) {
        if (block.lockState == BlockLockState.LOCKED) {
            coordinator.unlockBlock(block.id)
        } else {
            coordinator.lockBlock(block.id)
        }
    }

    suspend fun completeBlock(block: ScheduleBlock, tasks: List<ScheduleTask>) {
        val task = tasks.firstOrNull { it.id == block.taskId } ?: return
        val minutes = java.time.Duration.between(block.startAt, block.endAt).toMinutes().toInt()
        coordinator.markBlockDone(block.id, task.id, minutes)
    }

    suspend fun completeTask(taskId: String) {
        coordinator.markTaskDone(taskId)
    }

    suspend fun completeRecurringSeries(taskId: String) {
        coordinator.markRecurringSeriesDone(taskId)
    }

    suspend fun addReminderForTask(taskId: String) {
        coordinator.createReminderForTask(taskId)
    }

    suspend fun rescheduleUrgently(taskId: String, dueAt: Instant? = null): Boolean {
        return coordinator.rescheduleUrgently(taskId, dueAt)
    }

    suspend fun rescheduleNextAvailable(taskId: String, dueAt: Instant? = null): Boolean {
        return coordinator.rescheduleNextAvailable(taskId, dueAt)
    }

    suspend fun rescheduleToDueDate(taskId: String, dueAt: Instant) {
        coordinator.rescheduleToDueDate(taskId, dueAt)
    }

    suspend fun rescheduleMissed(taskId: String) {
        coordinator.rescheduleTask(taskId, ScheduleRebuildReason.TaskMissed(taskId))
    }

    suspend fun deleteTask(taskId: String) {
        coordinator.deleteTask(taskId)
    }

    suspend fun saveTimePeriod(draft: TimePeriodDraft) {
        coordinator.upsertTimePeriod(
            TimePeriod(
                id = draft.id.ifBlank { "period-${System.currentTimeMillis()}" },
                label = draft.label,
                start = draft.start,
                end = draft.end,
                type = draft.type,
                sortOrder = draft.sortOrder,
            ),
        )
    }

    suspend fun deleteTimePeriod(periodId: String) {
        coordinator.deleteTimePeriod(periodId)
    }

    suspend fun setThemeMode(value: ThemeMode) = settingsRepository.setThemeMode(value)
    suspend fun setDateFormatPreference(value: DateFormatPreference) = settingsRepository.setDateFormatPreference(value)
    suspend fun setWeekStart(value: WeekStart) = settingsRepository.setWeekStart(value)
    suspend fun setBreakBufferMinutes(value: Int) = settingsRepository.setBreakBufferMinutes(value)
    suspend fun setAlignmentMinutes(value: Int) = settingsRepository.setAlignmentMinutes(value)
    suspend fun setAllowTaskSplitting(value: Boolean) = settingsRepository.setAllowTaskSplitting(value)
    suspend fun setAllowConcurrentTasks(value: Boolean) = settingsRepository.setAllowConcurrentTasks(value)
    suspend fun setMaxTaskChunkMinutes(value: Int) = settingsRepository.setMaxTaskChunkMinutes(value)
    suspend fun setPreferredPeriodFallbackMode(value: PreferredPeriodFallbackMode) = settingsRepository.setPreferredPeriodFallbackMode(value)
    suspend fun setUrgentRescheduleMode(value: UrgentRescheduleMode) = settingsRepository.setUrgentRescheduleMode(value)
    suspend fun setDefaultTaskReminder(value: Boolean) = settingsRepository.setDefaultTaskReminder(value)
    suspend fun setReminderTimingMode(value: ReminderTimingMode) = settingsRepository.setReminderTimingMode(value)
    suspend fun setReminderLeadMinutes(value: Int) = settingsRepository.setReminderLeadMinutes(value)
    suspend fun setHistoryRetention(value: HistoryRetention) = settingsRepository.setHistoryRetention(value)
    suspend fun setHasCompletedOnboarding(value: Boolean) = settingsRepository.setHasCompletedOnboarding(value)
}

private fun TaskDraft.taskDueAtInstant(): Instant =
    taskDueAtLocalDateTime().atZone(ZoneId.systemDefault()).toInstant()

private fun TaskDraft.repeatDeadlineOrNull(): Instant? {
    if (recurrenceType == RecurrenceType.NONE || repeatsForever) return null
    val deadlineInstant = when (schedulingMode) {
        TaskSchedulingMode.FLEXIBLE -> deadline.atZone(ZoneId.systemDefault()).toInstant()
        TaskSchedulingMode.FLEXIBLE_WINDOW -> deadline.atZone(ZoneId.systemDefault()).toInstant()
        TaskSchedulingMode.FIXED_DAY -> fixedDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusSeconds(1)
        TaskSchedulingMode.FIXED_EXACT -> fixedEndAt.atZone(ZoneId.systemDefault()).toInstant()
    }
    val firstOccurrenceInstant = taskDueAtInstant()
    return if (deadlineInstant.isBefore(firstOccurrenceInstant)) firstOccurrenceInstant else deadlineInstant
}

private fun TaskDraft.taskDueAtLocalDateTime(now: LocalDateTime = LocalDateTime.now()): LocalDateTime {
    if (schedulingMode == TaskSchedulingMode.FIXED_DAY) {
        return fixedDate.atTime(23, 59)
    }
    if (schedulingMode == TaskSchedulingMode.FLEXIBLE_WINDOW) {
        if (recurrenceType == RecurrenceType.NONE) {
            return fixedEndAt
        }
        return LocalDateTime.of(startDate ?: fixedEndAt.toLocalDate(), fixedEndAt.toLocalTime())
    }
    if (schedulingMode == TaskSchedulingMode.FIXED_EXACT) {
        return fixedEndAt
    }
    if (startDate != null && recurrenceType != RecurrenceType.NONE) {
        val startCandidate = LocalDateTime.of(startDate, deadline.toLocalTime())
        return if (startCandidate.isAfter(now)) startCandidate else deadline
    }
    if (recurrenceType == RecurrenceType.NONE) {
        return if (hasDeadline) deadline else deadline.plusYears(1)
    }
    val targetTime = deadline.toLocalTime()
    return when (recurrenceType) {
        RecurrenceType.NONE -> deadline
        RecurrenceType.DAILY -> {
            var candidate = LocalDateTime.of(now.toLocalDate(), targetTime)
            val interval = recurrenceInterval.coerceAtLeast(1).toLong()
            if (!candidate.isAfter(now)) {
                do {
                    candidate = candidate.plusDays(interval)
                } while (!candidate.isAfter(now))
            }
            candidate
        }
        RecurrenceType.WEEKLY -> {
            val repeatDays = recurrenceDays.ifEmpty { setOf(now.dayOfWeek) }
            var candidateDate = now.toLocalDate()
            var candidate = LocalDateTime.of(candidateDate, targetTime)
            while (candidate.dayOfWeek !in repeatDays || !candidate.isAfter(now)) {
                candidateDate = candidateDate.plusDays(1)
                candidate = LocalDateTime.of(candidateDate, targetTime)
            }
            candidate
        }
        RecurrenceType.MONTHLY -> {
            val interval = recurrenceInterval.coerceAtLeast(1).toLong()
            var candidate = LocalDateTime.of(now.toLocalDate().withDayOfMonth(minOf(now.toLocalDate().lengthOfMonth(), deadline.dayOfMonth)), targetTime)
            while (!candidate.isAfter(now)) {
                val nextMonth = candidate.toLocalDate().plusMonths(interval)
                val nextDay = minOf(deadline.dayOfMonth, nextMonth.lengthOfMonth())
                candidate = LocalDateTime.of(nextMonth.withDayOfMonth(nextDay), targetTime)
            }
            candidate
        }
    }
}

fun ScheduleTask.dueDisplayText(
    formatter: java.time.format.DateTimeFormatter,
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = if (hasDeadline) {
    formatter.format(dueAt.atZone(zoneId))
} else {
    "No deadline"
}

fun ScheduleTask.deadlineSummaryText(
    formatter: java.time.format.DateTimeFormatter,
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = if (hasDeadline) {
    "Deadline ${formatter.format(dueAt.atZone(zoneId))}"
} else {
    "No deadline"
}

private fun TaskDraft.schedulingStartInstantOrNull(): Instant? =
    when (schedulingMode) {
        TaskSchedulingMode.FLEXIBLE -> startDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()
        TaskSchedulingMode.FLEXIBLE_WINDOW -> LocalDateTime.of(startDate ?: fixedStartAt.toLocalDate(), fixedStartAt.toLocalTime())
            .atZone(ZoneId.systemDefault())
            .toInstant()
        TaskSchedulingMode.FIXED_DAY -> null
        TaskSchedulingMode.FIXED_EXACT -> fixedStartAt.atZone(ZoneId.systemDefault()).toInstant()
    }

private fun TaskDraft.fixedEndAtInstantOrNull(): Instant? =
    when (schedulingMode) {
        TaskSchedulingMode.FLEXIBLE_WINDOW -> LocalDateTime.of(startDate ?: fixedEndAt.toLocalDate(), fixedEndAt.toLocalTime())
            .atZone(ZoneId.systemDefault())
            .toInstant()
        TaskSchedulingMode.FIXED_EXACT -> fixedEndAt.atZone(ZoneId.systemDefault()).toInstant()
        else -> null
    }

