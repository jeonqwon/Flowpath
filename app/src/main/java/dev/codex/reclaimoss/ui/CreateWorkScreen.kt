package dev.codex.reclaimoss.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.animateContentSize
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
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
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
import dev.codex.reclaimoss.domain.model.RecurrenceRule
import dev.codex.reclaimoss.domain.model.RecurrenceType
import dev.codex.reclaimoss.domain.model.Reminder
import dev.codex.reclaimoss.domain.model.ReminderStatus
import dev.codex.reclaimoss.domain.model.ScheduleBlock
import dev.codex.reclaimoss.domain.model.ScheduleTask
import dev.codex.reclaimoss.domain.model.TaskContinuationMode
import dev.codex.reclaimoss.domain.model.TaskOverlapPolicy
import dev.codex.reclaimoss.domain.model.TaskSchedulingMode
import dev.codex.reclaimoss.domain.model.TaskPriority
import dev.codex.reclaimoss.domain.model.TaskStatus
import dev.codex.reclaimoss.domain.model.Timeframe
import dev.codex.reclaimoss.domain.model.TimePeriod
import dev.codex.reclaimoss.domain.scheduling.ScheduleRebuildReason
import dev.codex.reclaimoss.domain.service.PlannerCoordinator
import dev.codex.reclaimoss.domain.service.TaskCreationResult
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

@Composable
fun TaskSectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun CreateFormCard(
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

@Composable
fun CreateModeSwitch(
    mode: CreateMode,
    onModeChanged: (CreateMode) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CreateMode.entries.forEach { option ->
                val selected = option == mode
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clickable { onModeChanged(option) },
                    shape = RoundedCornerShape(999.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            option.label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateWorkScreen(
    padding: PaddingValues,
    timeframes: List<Timeframe>,
    availableTasks: List<ScheduleTask>,
    currentTaskId: String? = null,
    sessionKey: Int,
    initialMode: CreateMode = CreateMode.Task,
    initialTaskDraft: TaskDraft? = null,
    initialReminderDraft: ReminderDraft? = null,
    followUpMode: Boolean = false,
    rescheduleMode: Boolean = false,
    onBack: () -> Unit,
    onSaveTask: (TaskDraft) -> Unit,
    onSaveReminder: (ReminderDraft) -> Unit,
) {
    val context = LocalContext.current
    var mode by rememberSaveable(sessionKey) { mutableStateOf(if (followUpMode || rescheduleMode) CreateMode.Task else initialMode) }
    var taskDraft by rememberSaveable(
        sessionKey,
        stateSaver = listSaver(
            save = {
                listOf(
                    it.title,
                    it.description,
                    it.priority.name,
                    it.preferredTimePeriodId ?: "",
                    it.timeframeId ?: "",
                    it.hasDeadline,
                    it.continuationParentTaskId ?: "",
                    it.continuationMode?.name ?: "",
                    it.overlapPolicy.name,
                    it.deadline.toString(),
                    it.schedulingMode.name,
                    it.startDate?.toString() ?: "",
                    it.fixedDate.toString(),
                    it.fixedStartAt.toString(),
                    it.fixedEndAt.toString(),
                    it.repeatsForever,
                    it.estimatedMinutes,
                    it.addReminder,
                    it.recurrenceType.name,
                    it.recurrenceInterval,
                    it.recurrenceDays.joinToString(",") { day -> day.name },
                )
            },
            restore = { saved ->
                TaskDraft(
                    title = saved[0] as String,
                    description = saved[1] as String,
                    priority = TaskPriority.valueOf(saved[2] as String),
                    preferredTimePeriodId = (saved[3] as String).ifBlank { null },
                    timeframeId = (saved[4] as String).ifBlank { null },
                    hasDeadline = saved[5] as Boolean,
                    continuationParentTaskId = (saved[6] as String).ifBlank { null },
                    continuationMode = (saved[7] as String).ifBlank { null }?.let(TaskContinuationMode::valueOf),
                    overlapPolicy = TaskOverlapPolicy.valueOf(saved[8] as String),
                    deadline = LocalDateTime.parse(saved[9] as String),
                    schedulingMode = TaskSchedulingMode.valueOf(saved[10] as String),
                    startDate = (saved[11] as String).ifBlank { null }?.let(LocalDate::parse),
                    fixedDate = LocalDate.parse(saved[12] as String),
                    fixedStartAt = LocalDateTime.parse(saved[13] as String),
                    fixedEndAt = LocalDateTime.parse(saved[14] as String),
                    repeatsForever = saved[15] as Boolean,
                    estimatedMinutes = saved[16] as Int,
                    addReminder = saved[17] as Boolean,
                    recurrenceType = RecurrenceType.valueOf(saved[18] as String),
                    recurrenceInterval = saved[19] as Int,
                    recurrenceDays = (saved[20] as String)
                        .takeIf { it.isNotBlank() }
                        ?.split(",")
                        ?.map { DayOfWeek.valueOf(it) }
                        ?.toSet()
                        ?: emptySet(),
                )
            },
        ),
    ) {
        mutableStateOf(initialTaskDraft ?: TaskDraft())
    }
    var reminderDraft by rememberSaveable(
        sessionKey,
        stateSaver = listSaver(
            save = {
                listOf(
                    it.title,
                    it.description,
                    it.dueAt.toString(),
                    it.isAllDay,
                    it.recurrenceType.name,
                    it.recurrenceInterval,
                    it.recurrenceDays.joinToString(",") { day -> day.name },
                )
            },
            restore = { saved ->
                ReminderDraft(
                    title = saved[0] as String,
                    description = saved[1] as String,
                    dueAt = LocalDateTime.parse(saved[2] as String),
                    isAllDay = saved[3] as Boolean,
                    recurrenceType = RecurrenceType.valueOf(saved[4] as String),
                    recurrenceInterval = saved[5] as Int,
                    recurrenceDays = (saved[6] as String)
                        .takeIf { it.isNotBlank() }
                        ?.split(",")
                        ?.map { DayOfWeek.valueOf(it) }
                        ?.toSet()
                        ?: emptySet(),
                )
            },
        ),
    ) { mutableStateOf(initialReminderDraft ?: ReminderDraft()) }
    var showAdvancedTiming by rememberSaveable(sessionKey) { mutableStateOf(false) }
    val noDeadlineEligible = taskDraft.schedulingMode == TaskSchedulingMode.FLEXIBLE && taskDraft.recurrenceType == RecurrenceType.NONE
    val continuationTasks = remember(availableTasks, currentTaskId, taskDraft.continuationParentTaskId) {
        availableTasks
            .filter { it.status == TaskStatus.ACTIVE }
            .filterNot { it.id == currentTaskId }
            .sortedBy { it.dueAt }
    }
    LaunchedEffect(continuationTasks, taskDraft.continuationParentTaskId) {
        if (taskDraft.continuationParentTaskId != null && continuationTasks.none { it.id == taskDraft.continuationParentTaskId }) {
            taskDraft = taskDraft.copy(
                continuationParentTaskId = null,
                continuationMode = null,
            )
        } else if (taskDraft.continuationParentTaskId != null && taskDraft.continuationMode == null) {
            taskDraft = taskDraft.copy(continuationMode = TaskContinuationMode.AFTER_PARENT_SCHEDULED_END)
        }
    }
    LaunchedEffect(followUpMode, rescheduleMode) {
        if (followUpMode || rescheduleMode) {
            mode = CreateMode.Task
            taskDraft = taskDraft.copy(
                addReminder = false,
                recurrenceType = if (followUpMode) RecurrenceType.NONE else taskDraft.recurrenceType,
                recurrenceInterval = if (followUpMode) 1 else taskDraft.recurrenceInterval,
                recurrenceDays = if (followUpMode) emptySet() else taskDraft.recurrenceDays,
            )
        }
    }
    val availableTimeframes = remember(timeframes, taskDraft) {
        val cutoffDate = taskDraft.timeframeCutoffDate()
        timeframes
            .filter { cutoffDate == null || !it.startDate.isAfter(cutoffDate) }
            .sortedWith(compareBy<Timeframe> { it.startDate }.thenBy { it.endDate }.thenBy { it.name })
    }
    LaunchedEffect(availableTimeframes, taskDraft.timeframeId) {
        if (taskDraft.timeframeId != null && availableTimeframes.none { it.id == taskDraft.timeframeId }) {
            taskDraft = taskDraft.copy(timeframeId = null)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
            }
            Text(
                when {
                    followUpMode -> "Create Follow-up"
                    rescheduleMode -> "Reschedule Task"
                    mode == CreateMode.Task -> "Create Task"
                    else -> "Create Reminder"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        if (!followUpMode && !rescheduleMode) {
            CreateModeSwitch(mode = mode, onModeChanged = { mode = it })
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            if (mode == CreateMode.Task) {
                item {
                    CreateFormCard {
                        OutlinedTextField(
                            value = taskDraft.title,
                            onValueChange = { taskDraft = taskDraft.copy(title = it) },
                            label = { Text("Task title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = taskDraft.description,
                            onValueChange = { taskDraft = taskDraft.copy(description = it) },
                            label = { Text("Notes") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                        )
                    }
                }
                item {
                    CreateFormCard {
                        DurationSlider(
                            minutes = taskDraft.estimatedMinutes,
                            maxMinutes = 360,
                            onMinutesChanged = {
                                taskDraft = if (taskDraft.schedulingMode == TaskSchedulingMode.FIXED_EXACT) {
                                    val updatedEnd = taskDraft.fixedStartAt.plusMinutes(it.toLong())
                                    taskDraft.copy(
                                        estimatedMinutes = it,
                                        fixedEndAt = updatedEnd,
                                        deadline = updatedEnd,
                                    )
                                } else {
                                    taskDraft.copy(estimatedMinutes = it)
                                }
                            },
                        )
                        if (taskDraft.schedulingMode == TaskSchedulingMode.FLEXIBLE) {
                            if (noDeadlineEligible) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    TaskSectionTitle("No deadline")
                                    Switch(
                                        checked = !taskDraft.hasDeadline,
                                        onCheckedChange = { enabled ->
                                            taskDraft = taskDraft.copy(hasDeadline = !enabled)
                                        },
                                    )
                                }
                            }
                            if (taskDraft.hasDeadline) {
                                DateTimeSection(
                                    title = "Deadline",
                                    dateTime = taskDraft.deadline,
                                    onDateTimeChanged = { selected ->
                                        taskDraft = taskDraft.copy(deadline = selected)
                                    },
                                    context = context,
                                )
                            }
                        }
                        TaskSectionTitle("Timeframe")
                        TimeframeDropdown(
                            timeframes = availableTimeframes,
                            selectedTimeframeId = taskDraft.timeframeId,
                            onSelected = { timeframeId ->
                                taskDraft = taskDraft.copy(timeframeId = timeframeId)
                            },
                        )
                    }
                }
                item {
                    CreateFormCard {
                        AdvancedTimingRow(
                            expanded = showAdvancedTiming,
                            onToggle = { showAdvancedTiming = !showAdvancedTiming },
                        )
                        if (showAdvancedTiming) {
                            TaskSectionTitle("Priority")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(TaskPriority.MEDIUM to "Normal", TaskPriority.URGENT to "Urgent").forEach { (priority, label) ->
                                    FilterChip(
                                        selected = taskDraft.priority == priority,
                                        onClick = { taskDraft = taskDraft.copy(priority = priority) },
                                        label = { Text(label) },
                                    )
                                }
                            }
                            TaskSectionTitle("Overlap")
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                listOf(
                                    TaskOverlapPolicy.INHERIT to "Inherit",
                                    TaskOverlapPolicy.ALLOW to "Allow",
                                    TaskOverlapPolicy.DISALLOW to "No overlap",
                                ).forEach { (policy, label) ->
                                    FilterChip(
                                        selected = taskDraft.overlapPolicy == policy,
                                        onClick = { taskDraft = taskDraft.copy(overlapPolicy = policy) },
                                        label = { Text(label) },
                                    )
                                }
                            }
                            SchedulingModeSection(
                                schedulingMode = taskDraft.schedulingMode,
                                onModeChanged = { newMode ->
                                    taskDraft = taskDraft.copy(
                                        schedulingMode = newMode,
                                        hasDeadline = if (newMode == TaskSchedulingMode.FLEXIBLE) taskDraft.hasDeadline else true,
                                        startDate = when (newMode) {
                                            TaskSchedulingMode.FLEXIBLE_WINDOW -> taskDraft.fixedStartAt.toLocalDate()
                                            else -> null
                                        },
                                        recurrenceType = if (newMode == TaskSchedulingMode.FIXED_DAY) RecurrenceType.NONE else taskDraft.recurrenceType,
                                        recurrenceDays = if (newMode == TaskSchedulingMode.FIXED_DAY) emptySet() else taskDraft.recurrenceDays,
                                        fixedDate = taskDraft.deadline.toLocalDate(),
                                        fixedStartAt = when (newMode) {
                                            TaskSchedulingMode.FIXED_EXACT -> taskDraft.deadline.minusMinutes(taskDraft.estimatedMinutes.toLong()).withSecond(0).withNano(0)
                                            TaskSchedulingMode.FLEXIBLE_WINDOW -> taskDraft.fixedStartAt.withSecond(0).withNano(0)
                                            else -> taskDraft.fixedStartAt.withSecond(0).withNano(0)
                                        },
                                        fixedEndAt = when (newMode) {
                                            TaskSchedulingMode.FIXED_EXACT -> taskDraft.deadline.withSecond(0).withNano(0)
                                            TaskSchedulingMode.FLEXIBLE_WINDOW -> taskDraft.fixedEndAt.withSecond(0).withNano(0)
                                            else -> taskDraft.fixedEndAt.withSecond(0).withNano(0)
                                        },
                                    )
                                },
                            )
                            when (taskDraft.schedulingMode) {
                                TaskSchedulingMode.FLEXIBLE -> Unit
                                TaskSchedulingMode.FLEXIBLE_WINDOW -> {
                                    FlexibleWindowSection(
                                        recurrenceType = taskDraft.recurrenceType,
                                        startDate = taskDraft.startDate ?: taskDraft.fixedStartAt.toLocalDate(),
                                        windowStart = taskDraft.fixedStartAt,
                                        windowEnd = taskDraft.fixedEndAt,
                                        onStartDateChanged = {
                                            taskDraft = taskDraft.copy(startDate = it)
                                        },
                                        onWindowStartChanged = {
                                            taskDraft = taskDraft.copy(fixedStartAt = it)
                                        },
                                        onWindowEndChanged = {
                                            taskDraft = taskDraft.copy(fixedEndAt = it)
                                        },
                                        context = context,
                                    )
                                }
                                TaskSchedulingMode.FIXED_DAY -> {
                                    FixedDaySection(
                                        date = taskDraft.fixedDate,
                                        onDateChanged = {
                                            taskDraft = taskDraft.copy(
                                                fixedDate = it,
                                                deadline = taskDraft.deadline.withYear(it.year).withMonth(it.monthValue).withDayOfMonth(it.dayOfMonth),
                                                fixedStartAt = taskDraft.fixedStartAt.withYear(it.year).withMonth(it.monthValue).withDayOfMonth(it.dayOfMonth),
                                                fixedEndAt = taskDraft.fixedEndAt.withYear(it.year).withMonth(it.monthValue).withDayOfMonth(it.dayOfMonth),
                                            )
                                        },
                                        context = context,
                                    )
                                }
                                TaskSchedulingMode.FIXED_EXACT -> {
                                    FixedTimeSection(
                                        recurrenceType = taskDraft.recurrenceType,
                                        dateTime = taskDraft.fixedStartAt,
                                        onDateTimeChanged = {
                                            val adjustedEnd = it.plusMinutes(taskDraft.estimatedMinutes.toLong())
                                            taskDraft = taskDraft.copy(
                                                fixedStartAt = it,
                                                fixedEndAt = adjustedEnd,
                                                deadline = adjustedEnd,
                                            )
                                        },
                                        onTimeChanged = { selectedTime ->
                                            val adjustedStart = taskDraft.fixedStartAt
                                                .withHour(selectedTime.hour)
                                                .withMinute(selectedTime.minute)
                                                .withSecond(0)
                                                .withNano(0)
                                            val adjustedEnd = adjustedStart.plusMinutes(taskDraft.estimatedMinutes.toLong())
                                            taskDraft = taskDraft.copy(
                                                fixedStartAt = adjustedStart,
                                                fixedEndAt = adjustedEnd,
                                                deadline = adjustedEnd,
                                            )
                                        },
                                        context = context,
                                    )
                                }
                            }
                            if (!followUpMode && !rescheduleMode) {
                                RecurrenceSection(
                                    recurrenceType = taskDraft.recurrenceType,
                                    recurrenceDays = taskDraft.recurrenceDays,
                                    recurrenceInterval = taskDraft.recurrenceInterval,
                                    onTypeChanged = {
                                        taskDraft = taskDraft.copy(
                                            recurrenceType = it,
                                            hasDeadline = if (it == RecurrenceType.NONE) taskDraft.hasDeadline else true,
                                            recurrenceDays = if (it == RecurrenceType.WEEKLY) taskDraft.recurrenceDays else emptySet(),
                                        )
                                    },
                                    onIntervalChanged = { taskDraft = taskDraft.copy(recurrenceInterval = it) },
                                    disabledTypes = if (taskDraft.schedulingMode == TaskSchedulingMode.FIXED_DAY) {
                                        setOf(RecurrenceType.DAILY, RecurrenceType.WEEKLY)
                                    } else {
                                        emptySet()
                                    },
                                    onDayToggle = { taskDraft = taskDraft.copy(recurrenceDays = taskDraft.recurrenceDays.toggleForCreate(it)) },
                                )
                                if (taskDraft.recurrenceType != RecurrenceType.NONE) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        TaskSectionTitle("Repeats forever")
                                        Switch(
                                            checked = taskDraft.repeatsForever,
                                            onCheckedChange = { taskDraft = taskDraft.copy(repeatsForever = it) },
                                        )
                                    }
                                    if (!taskDraft.repeatsForever) {
                                        DateTimeSection(
                                            title = "Repeat until",
                                            dateTime = taskDraft.deadline,
                                            onDateTimeChanged = { taskDraft = taskDraft.copy(deadline = it) },
                                            context = context,
                                        )
                                    }
                                }
                            }
                            if (continuationTasks.isNotEmpty()) {
                                ContinuationSection(
                                    tasks = continuationTasks,
                                    selectedParentTaskId = taskDraft.continuationParentTaskId,
                                    selectedMode = taskDraft.continuationMode,
                                    onParentSelected = { taskId ->
                                        taskDraft = taskDraft.copy(
                                            continuationParentTaskId = taskId,
                                            continuationMode = if (taskId == null) null else (taskDraft.continuationMode ?: TaskContinuationMode.AFTER_PARENT_SCHEDULED_END),
                                        )
                                    },
                                    onModeSelected = { mode ->
                                        taskDraft = taskDraft.copy(continuationMode = mode)
                                    },
                                )
                            }
                            if (!followUpMode && !rescheduleMode) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    TaskSectionTitle("Add as reminder")
                                    Switch(
                                        checked = taskDraft.addReminder,
                                        onCheckedChange = { taskDraft = taskDraft.copy(addReminder = it) },
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    CreateFormCard {
                        OutlinedTextField(
                            value = reminderDraft.title,
                            onValueChange = { reminderDraft = reminderDraft.copy(title = it) },
                            label = { Text("Reminder title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = reminderDraft.description,
                            onValueChange = { reminderDraft = reminderDraft.copy(description = it) },
                            label = { Text("Notes") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                        )
                    }
                }
                item {
                    CreateFormCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TaskSectionTitle("Whole day")
                            Switch(
                                checked = reminderDraft.isAllDay,
                                onCheckedChange = { enabled ->
                                    reminderDraft = reminderDraft.copy(
                                        isAllDay = enabled,
                                        dueAt = if (enabled) {
                                            reminderDraft.dueAt.withHour(9).withMinute(0).withSecond(0).withNano(0)
                                        } else {
                                            reminderDraft.dueAt.withSecond(0).withNano(0)
                                        },
                                    )
                                },
                            )
                        }
                        DateTimeSection(
                            title = if (reminderDraft.recurrenceType == RecurrenceType.NONE) "Remind me" else "First reminder time",
                            dateTime = reminderDraft.dueAt,
                            onDateTimeChanged = { reminderDraft = reminderDraft.copy(dueAt = it) },
                            context = context,
                            dateOnly = reminderDraft.isAllDay,
                        )
                        RecurrenceSection(
                            recurrenceType = reminderDraft.recurrenceType,
                            recurrenceDays = reminderDraft.recurrenceDays,
                            recurrenceInterval = reminderDraft.recurrenceInterval,
                            onTypeChanged = {
                                reminderDraft = reminderDraft.copy(
                                    recurrenceType = it,
                                    recurrenceDays = if (it == RecurrenceType.WEEKLY) reminderDraft.recurrenceDays else emptySet(),
                                )
                            },
                            onIntervalChanged = { reminderDraft = reminderDraft.copy(recurrenceInterval = it) },
                            onDayToggle = { reminderDraft = reminderDraft.copy(recurrenceDays = reminderDraft.recurrenceDays.toggleForCreate(it)) },
                        )
                    }
                }
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(999.dp),
            enabled = if (mode == CreateMode.Task) {
                taskDraft.title.isNotBlank() &&
                    ((taskDraft.schedulingMode != TaskSchedulingMode.FIXED_EXACT && taskDraft.schedulingMode != TaskSchedulingMode.FLEXIBLE_WINDOW) || taskDraft.fixedEndAt.isAfter(taskDraft.fixedStartAt)) &&
                    (taskDraft.schedulingMode != TaskSchedulingMode.FLEXIBLE_WINDOW || taskDraft.recurrenceType != RecurrenceType.NONE || taskDraft.fixedStartAt.toLocalDate() == taskDraft.fixedEndAt.toLocalDate()) &&
                    (taskDraft.recurrenceType != RecurrenceType.WEEKLY || taskDraft.recurrenceDays.isNotEmpty())
            } else {
                reminderDraft.title.isNotBlank() &&
                    (reminderDraft.recurrenceType != RecurrenceType.WEEKLY || reminderDraft.recurrenceDays.isNotEmpty())
            },
            onClick = {
                if (mode == CreateMode.Task) onSaveTask(taskDraft) else onSaveReminder(reminderDraft)
            },
        ) {
            Text(
                when {
                    mode != CreateMode.Task -> "Save Reminder"
                    rescheduleMode -> "Save Reschedule"
                    else -> "Schedule Task"
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SchedulingModeSection(
    schedulingMode: TaskSchedulingMode,
    onModeChanged: (TaskSchedulingMode) -> Unit,
) {
    TaskSectionTitle("Schedule")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            TaskSchedulingMode.FLEXIBLE to "Flexible",
            TaskSchedulingMode.FLEXIBLE_WINDOW to "Window",
            TaskSchedulingMode.FIXED_DAY to "Fixed date",
            TaskSchedulingMode.FIXED_EXACT to "Fixed time",
        ).forEach { (mode, label) ->
            FilterChip(
                selected = schedulingMode == mode,
                onClick = { onModeChanged(mode) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
fun FixedDaySection(
    date: LocalDate,
    onDateChanged: (LocalDate) -> Unit,
    context: android.content.Context,
) {
    val dateLabel = remember(date) { DateTimeFormatter.ofPattern("EEE, MMM d").format(date) }
    TaskSectionTitle("Date")
    DateTimePickerCard(
        label = "Date",
        value = dateLabel,
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    onDateChanged(LocalDate.of(year, month + 1, dayOfMonth))
                },
                date.year,
                date.monthValue - 1,
                date.dayOfMonth,
            ).show()
        },
    )
}

@Composable
fun FixedTimeSection(
    recurrenceType: RecurrenceType,
    dateTime: LocalDateTime,
    onDateTimeChanged: (LocalDateTime) -> Unit,
    onTimeChanged: (LocalTime) -> Unit,
    context: android.content.Context,
) {
    if (recurrenceType == RecurrenceType.NONE) {
        DateTimeSection(
            title = "Time",
            dateTime = dateTime,
            onDateTimeChanged = onDateTimeChanged,
            context = context,
        )
    } else {
        TimeOnlySection(
            title = "Time",
            time = dateTime.toLocalTime(),
            onTimeChanged = onTimeChanged,
            context = context,
        )
    }
}

@Composable
fun FlexibleWindowSection(
    recurrenceType: RecurrenceType,
    startDate: LocalDate,
    windowStart: LocalDateTime,
    windowEnd: LocalDateTime,
    onStartDateChanged: (LocalDate) -> Unit,
    onWindowStartChanged: (LocalDateTime) -> Unit,
    onWindowEndChanged: (LocalDateTime) -> Unit,
    context: android.content.Context,
) {
    if (recurrenceType == RecurrenceType.NONE) {
        DateTimeSection(
            title = "Window start",
            dateTime = windowStart,
            onDateTimeChanged = onWindowStartChanged,
            context = context,
        )
        DateTimeSection(
            title = "Window end",
            dateTime = windowEnd,
            onDateTimeChanged = onWindowEndChanged,
            context = context,
        )
    } else {
        FixedDaySection(
            date = startDate,
            onDateChanged = onStartDateChanged,
            context = context,
        )
        TimeOnlySection(
            title = "Window start",
            time = windowStart.toLocalTime(),
            onTimeChanged = { selectedTime ->
                onWindowStartChanged(
                    windowStart
                        .withHour(selectedTime.hour)
                        .withMinute(selectedTime.minute)
                        .withSecond(0)
                        .withNano(0),
                )
            },
            context = context,
        )
        TimeOnlySection(
            title = "Window end",
            time = windowEnd.toLocalTime(),
            onTimeChanged = { selectedTime ->
                onWindowEndChanged(
                    windowEnd
                        .withHour(selectedTime.hour)
                        .withMinute(selectedTime.minute)
                        .withSecond(0)
                        .withNano(0),
                )
            },
            context = context,
        )
    }
}

@Composable
fun AdvancedTimingRow(
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("More options", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = if (expanded) "Collapse options" else "Expand options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer(rotationZ = if (expanded) 90f else 0f),
            )
        }
    }
}

@Composable
fun StartDateSection(
    date: LocalDate?,
    onDateChanged: (LocalDate?) -> Unit,
    onClear: () -> Unit,
    context: android.content.Context,
) {
    val dateLabel = remember(date) { date?.let { DateTimeFormatter.ofPattern("EEE, MMM d").format(it) } ?: "Not set" }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DateTimePickerCard(
            label = "Start date",
            value = dateLabel,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val initial = date ?: LocalDate.now()
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        onDateChanged(LocalDate.of(year, month + 1, dayOfMonth))
                    },
                    initial.year,
                    initial.monthValue - 1,
                    initial.dayOfMonth,
                ).show()
            },
        )
        if (date != null) {
            OutlinedButton(
                onClick = onClear,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text("Clear start date")
            }
        }
    }
}

@Composable
fun DeadlineSummaryCard(taskDraft: TaskDraft) {
    val value = remember(taskDraft.hasDeadline, taskDraft.deadline, taskDraft.fixedDate, taskDraft.fixedStartAt, taskDraft.fixedEndAt, taskDraft.schedulingMode) {
        if (!taskDraft.hasDeadline && taskDraft.schedulingMode == TaskSchedulingMode.FLEXIBLE) {
            return@remember "No deadline"
        }
        when (taskDraft.schedulingMode) {
            TaskSchedulingMode.FLEXIBLE -> DateTimeFormatter.ofPattern("EEE, MMM d • h:mm a").format(taskDraft.deadline)
            TaskSchedulingMode.FLEXIBLE_WINDOW -> {
                val dayFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
                val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
                "${dayFormatter.format(taskDraft.fixedStartAt)} • ${timeFormatter.format(taskDraft.fixedStartAt)}-${timeFormatter.format(taskDraft.fixedEndAt)}"
            }
            TaskSchedulingMode.FIXED_DAY -> DateTimeFormatter.ofPattern("EEE, MMM d").format(taskDraft.fixedDate)
            TaskSchedulingMode.FIXED_EXACT -> DateTimeFormatter.ofPattern("EEE, MMM d • h:mm a").format(taskDraft.fixedEndAt)
        }
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
    ) {
        Text(
            value,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun DateTimeSection(
    title: String,
    dateTime: LocalDateTime,
    onDateTimeChanged: (LocalDateTime) -> Unit,
    context: android.content.Context,
    dateOnly: Boolean = false,
) {
    TaskSectionTitle(title)
    DateTimePickerRows(
        dateTime = dateTime,
        onDateTimeChanged = onDateTimeChanged,
        context = context,
        dateOnly = dateOnly,
    )
}

@Composable
fun DurationSlider(
    minutes: Int,
    minMinutes: Int = 15,
    maxMinutes: Int = 360,
    onMinutesChanged: (Int) -> Unit,
) {
    val stepMinutes = 15
    val effectiveMax = maxMinutes.coerceAtLeast(minMinutes)
    val snapped = snapToStepForCreate(minutes.coerceIn(minMinutes, effectiveMax), stepMinutes).coerceIn(minMinutes, effectiveMax)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Duration", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(snapped.durationLabelForCreate(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = snapped.toFloat(),
                onValueChange = { raw ->
                    onMinutesChanged(snapToStepForCreate(raw.toInt(), stepMinutes).coerceIn(minMinutes, effectiveMax))
                },
                valueRange = minMinutes.toFloat()..effectiveMax.toFloat(),
                steps = ((effectiveMax - minMinutes) / stepMinutes - 1).coerceAtLeast(0),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(minMinutes.durationLabelForCreate(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(effectiveMax.durationLabelForCreate(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun DateTimePickerRows(
    dateTime: LocalDateTime,
    onDateTimeChanged: (LocalDateTime) -> Unit,
    context: android.content.Context,
    dateOnly: Boolean = false,
) {
    val dateLabel = remember(dateTime) { DateTimeFormatter.ofPattern("EEE, MMM d").format(dateTime) }
    val timeLabel = remember(dateTime) { DateTimeFormatter.ofPattern("h:mm a").format(dateTime) }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        DateTimePickerCard(
            label = "Date",
            value = dateLabel,
            modifier = Modifier.weight(1f),
            onClick = {
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        onDateTimeChanged(dateTime.withYear(year).withMonth(month + 1).withDayOfMonth(dayOfMonth))
                    },
                    dateTime.year,
                    dateTime.monthValue - 1,
                    dateTime.dayOfMonth,
                ).show()
            },
        )
        if (!dateOnly) {
            DateTimePickerCard(
                label = "Time",
                value = timeLabel,
                modifier = Modifier.weight(1f),
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            onDateTimeChanged(
                                dateTime
                                    .withHour(hour)
                                    .withMinute(minute)
                                    .withSecond(0)
                                    .withNano(0),
                            )
                        },
                        dateTime.hour,
                        dateTime.minute,
                        false,
                    ).show()
                },
            )
        }
    }
}

@Composable
fun DateTimePickerCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun TimeframeDropdown(
    timeframes: List<Timeframe>,
    selectedTimeframeId: String?,
    onSelected: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedTimeframe = timeframes.firstOrNull { it.id == selectedTimeframeId }
    val formatter = remember { DateTimeFormatter.ofPattern("MMM d") }
    val selectedLabel = selectedTimeframe?.let {
        "${it.name} · ${formatter.format(it.startDate)} - ${formatter.format(it.endDate)}"
    } ?: "None"

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    selectedLabel,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text("v", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp)),
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        "None",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (selectedTimeframeId == null) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                onClick = {
                    onSelected(null)
                    expanded = false
                },
            )
            timeframes.forEach { timeframe ->
                DropdownMenuItem(
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                timeframe.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (selectedTimeframeId == timeframe.id) FontWeight.Bold else FontWeight.Normal,
                            )
                            Text(
                                "${formatter.format(timeframe.startDate)} - ${formatter.format(timeframe.endDate)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = {
                        onSelected(timeframe.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContinuationSection(
    tasks: List<ScheduleTask>,
    selectedParentTaskId: String?,
    selectedMode: TaskContinuationMode?,
    onParentSelected: (String?) -> Unit,
    onModeSelected: (TaskContinuationMode) -> Unit,
) {
    TaskSectionTitle("Dependency")
    var expanded by remember { mutableStateOf(false) }
    val selectedTask = tasks.firstOrNull { it.id == selectedParentTaskId }
    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    selectedTask?.title ?: "None",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text("v", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp)),
        ) {
            DropdownMenuItem(
                text = { Text("None", style = MaterialTheme.typography.titleMedium) },
                onClick = {
                    onParentSelected(null)
                    expanded = false
                },
            )
            tasks.forEach { task ->
                DropdownMenuItem(
                    text = {
                        Text(
                            task.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (task.id == selectedParentTaskId) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    onClick = {
                        onParentSelected(task.id)
                        expanded = false
                    },
                )
            }
        }
    }
    if (selectedParentTaskId != null) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                TaskContinuationMode.AFTER_PARENT_SCHEDULED_END to "After task time",
                TaskContinuationMode.AFTER_PARENT_DUE_AT to "After due time",
                TaskContinuationMode.BEFORE_PARENT_START to "Finish first",
            ).forEach { (mode, label) ->
                FilterChip(
                    selected = selectedMode == mode,
                    onClick = { onModeSelected(mode) },
                    label = { Text(label) },
                )
            }
        }
    }
}

private fun TaskDraft.timeframeCutoffDate(): LocalDate? = when {
    !hasDeadline -> null
    schedulingMode == TaskSchedulingMode.FIXED_DAY -> fixedDate
    schedulingMode == TaskSchedulingMode.FIXED_EXACT -> fixedStartAt.toLocalDate()
    schedulingMode == TaskSchedulingMode.FLEXIBLE_WINDOW -> fixedEndAt.toLocalDate()
    else -> deadline.toLocalDate()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecurrenceSection(
    recurrenceType: RecurrenceType,
    recurrenceDays: Set<DayOfWeek>,
    onTypeChanged: (RecurrenceType) -> Unit,
    recurrenceInterval: Int,
    onIntervalChanged: (Int) -> Unit,
    disabledTypes: Set<RecurrenceType> = emptySet(),
    onDayToggle: (DayOfWeek) -> Unit,
) {
    TaskSectionTitle("Repeat")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RecurrenceType.entries.forEach { type ->
            FilterChip(
                selected = recurrenceType == type,
                onClick = { onTypeChanged(type) },
                enabled = type !in disabledTypes,
                label = { Text(type.displayNameForCreate()) },
            )
        }
    }
    if (recurrenceType != RecurrenceType.NONE) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Every", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = { if (recurrenceInterval > 1) onIntervalChanged(recurrenceInterval - 1) }) { Text("-") }
            Text(recurrenceInterval.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedButton(onClick = { onIntervalChanged((recurrenceInterval + 1).coerceAtMost(30)) }) { Text("+") }
            Text(
                when (recurrenceType) {
                    RecurrenceType.DAILY -> "day(s)"
                    RecurrenceType.WEEKLY -> "week(s)"
                    RecurrenceType.MONTHLY -> "month(s)"
                    RecurrenceType.NONE -> ""
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (recurrenceType == RecurrenceType.WEEKLY) {
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            DayOfWeek.entries.forEach { day ->
                Surface(
                    modifier = Modifier
                        .size(42.dp)
                        .clickable { onDayToggle(day) },
                    shape = RoundedCornerShape(999.dp),
                    color = if (day in recurrenceDays) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            day.shortLabelForCreate().first().toString(),
                            color = if (day in recurrenceDays) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimeOnlySection(
    title: String,
    time: LocalTime,
    onTimeChanged: (LocalTime) -> Unit,
    context: android.content.Context,
) {
    val timeLabel = remember(time) { DateTimeFormatter.ofPattern("h:mm a").format(time) }
    TaskSectionTitle(title)
    DateTimePickerCard(
        label = "Time",
        value = timeLabel,
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            TimePickerDialog(
                context,
                { _, hour, minute -> onTimeChanged(LocalTime.of(hour, minute)) },
                time.hour,
                time.minute,
                false,
            ).show()
        },
    )
}

private fun RecurrenceType.displayNameForCreate(): String =
    when (this) {
        RecurrenceType.NONE -> "Once"
        RecurrenceType.DAILY -> "Daily"
        RecurrenceType.WEEKLY -> "Weekly"
        RecurrenceType.MONTHLY -> "Monthly"
    }

private fun DayOfWeek.shortLabelForCreate(): String =
    getDisplayName(TextStyle.SHORT, Locale.getDefault())

private fun Set<DayOfWeek>.toggleForCreate(day: DayOfWeek): Set<DayOfWeek> =
    if (day in this) this - day else this + day

private fun LocalTime.formatAsClockForCreate(): String =
    format(DateTimeFormatter.ofPattern("h:mm a"))

private fun snapToStepForCreate(value: Int, step: Int): Int =
    ((value + step / 2) / step) * step

private fun Int.durationLabelForCreate(): String =
    if (this < 60) "${this}m" else "${this / 60}h${if (this % 60 == 0) "" else " ${this % 60}m"}"

