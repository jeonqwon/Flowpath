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
import dev.codex.reclaimoss.domain.model.RecurrenceRule
import dev.codex.reclaimoss.domain.model.RecurrenceType
import dev.codex.reclaimoss.domain.model.Reminder
import dev.codex.reclaimoss.domain.model.ReminderStatus
import dev.codex.reclaimoss.domain.model.ScheduleBlock
import dev.codex.reclaimoss.domain.model.ScheduleTask
import dev.codex.reclaimoss.domain.model.TaskPriority
import dev.codex.reclaimoss.domain.model.TaskStatus
import dev.codex.reclaimoss.domain.model.TimePeriod
import dev.codex.reclaimoss.domain.model.TimePeriodType
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
        text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
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
            modifier = Modifier.padding(18.dp),
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
        color = MaterialTheme.colorScheme.surface,
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
                            fontWeight = FontWeight.Bold,
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
    periods: List<TimePeriod>,
    initialMode: CreateMode = CreateMode.Task,
    initialTaskDraft: TaskDraft? = null,
    followUpMode: Boolean = false,
    onBack: () -> Unit,
    onSaveTask: (TaskDraft) -> Unit,
    onSaveReminder: (ReminderDraft) -> Unit,
) {
    val context = LocalContext.current
    var mode by rememberSaveable(initialMode, followUpMode) { mutableStateOf(if (followUpMode) CreateMode.Task else initialMode) }
    var taskDraft by remember(initialTaskDraft, periods) {
        mutableStateOf(initialTaskDraft ?: TaskDraft(preferredTimePeriodId = periods.firstOrNull { it.type == TimePeriodType.PRODUCTIVE }?.id))
    }
    var reminderDraft by remember { mutableStateOf(ReminderDraft()) }
    LaunchedEffect(followUpMode) {
        if (followUpMode) {
            mode = CreateMode.Task
            taskDraft = taskDraft.copy(
                addReminder = false,
                recurrenceType = RecurrenceType.NONE,
                recurrenceDays = emptySet(),
            )
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
            Column {
                Text(
                    when {
                        followUpMode -> "Create Follow-up"
                        mode == CreateMode.Task -> "Create Task"
                        else -> "Create Reminder"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    when {
                        followUpMode -> "Schedule the next follow-up step"
                        mode == CreateMode.Task -> "Add work to your schedule"
                        else -> "Add something to remember"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (!followUpMode) {
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
                        TaskSectionTitle("Duration")
                        DurationSlider(
                            minutes = taskDraft.estimatedMinutes,
                            maxMinutes = 360,
                            onMinutesChanged = { taskDraft = taskDraft.copy(estimatedMinutes = it) },
                        )
                        TaskSectionTitle("Preferred period")
                        PreferredPeriodDropdown(
                            periods = periods,
                            selectedPeriodId = taskDraft.preferredTimePeriodId,
                            onSelected = { periodId ->
                                taskDraft = taskDraft.copy(preferredTimePeriodId = periodId)
                            },
                        )
                    }
                }
                item {
                    CreateFormCard {
                        if (!followUpMode) {
                            RecurrenceSection(
                                recurrenceType = taskDraft.recurrenceType,
                                recurrenceDays = taskDraft.recurrenceDays,
                                onTypeChanged = {
                                    taskDraft = taskDraft.copy(
                                        recurrenceType = it,
                                        recurrenceDays = if (it == RecurrenceType.WEEKLY) taskDraft.recurrenceDays else emptySet(),
                                    )
                                },
                                onDayToggle = { taskDraft = taskDraft.copy(recurrenceDays = taskDraft.recurrenceDays.toggle(it)) },
                            )
                            if (taskDraft.recurrenceType != RecurrenceType.NONE) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    TaskSectionTitle("No due date")
                                    Switch(
                                        checked = taskDraft.repeatsForever,
                                        onCheckedChange = { taskDraft = taskDraft.copy(repeatsForever = it) },
                                    )
                                }
                            }
                        }
                        val showDeadline = followUpMode || taskDraft.recurrenceType == RecurrenceType.NONE || !taskDraft.repeatsForever
                        if (showDeadline) {
                            DateTimeSection(
                                title = if (followUpMode) "Follow-up due date" else "Deadline",
                                dateTime = taskDraft.deadline,
                                onDateTimeChanged = { taskDraft = taskDraft.copy(deadline = it) },
                                context = context,
                            )
                        }
                        if (!followUpMode) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                TaskSectionTitle("Add also as reminder")
                                Switch(
                                    checked = taskDraft.addReminder,
                                    onCheckedChange = { taskDraft = taskDraft.copy(addReminder = it) },
                                )
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
                        DateTimeSection(
                            title = if (reminderDraft.recurrenceType == RecurrenceType.NONE) "Remind me" else "First reminder time",
                            dateTime = reminderDraft.dueAt,
                            onDateTimeChanged = { reminderDraft = reminderDraft.copy(dueAt = it) },
                            context = context,
                        )
                        RecurrenceSection(
                            recurrenceType = reminderDraft.recurrenceType,
                            recurrenceDays = reminderDraft.recurrenceDays,
                            onTypeChanged = {
                                reminderDraft = reminderDraft.copy(
                                    recurrenceType = it,
                                    recurrenceDays = if (it == RecurrenceType.WEEKLY) reminderDraft.recurrenceDays else emptySet(),
                                )
                            },
                            onDayToggle = { reminderDraft = reminderDraft.copy(recurrenceDays = reminderDraft.recurrenceDays.toggle(it)) },
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
                    (taskDraft.recurrenceType != RecurrenceType.WEEKLY || taskDraft.recurrenceDays.isNotEmpty())
            } else {
                reminderDraft.title.isNotBlank() &&
                    (reminderDraft.recurrenceType != RecurrenceType.WEEKLY || reminderDraft.recurrenceDays.isNotEmpty())
            },
            onClick = {
                if (mode == CreateMode.Task) onSaveTask(taskDraft) else onSaveReminder(reminderDraft)
            },
        ) {
            Text(if (mode == CreateMode.Task) "Schedule Task" else "Save Reminder")
        }
    }
}

@Composable
fun DateTimeSection(
    title: String,
    dateTime: LocalDateTime,
    onDateTimeChanged: (LocalDateTime) -> Unit,
    context: android.content.Context,
) {
    TaskSectionTitle(title)
    DateTimePickerRows(
        dateTime = dateTime,
        onDateTimeChanged = onDateTimeChanged,
        context = context,
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
    val snapped = snapToStep(minutes.coerceIn(minMinutes, effectiveMax), stepMinutes).coerceIn(minMinutes, effectiveMax)

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
                Text("Estimated time", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(snapped.durationLabel(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = snapped.toFloat(),
                onValueChange = { raw ->
                    onMinutesChanged(snapToStep(raw.toInt(), stepMinutes).coerceIn(minMinutes, effectiveMax))
                },
                valueRange = minMinutes.toFloat()..effectiveMax.toFloat(),
                steps = ((effectiveMax - minMinutes) / stepMinutes - 1).coerceAtLeast(0),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(minMinutes.durationLabel(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(effectiveMax.durationLabel(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun DateTimePickerRows(
    dateTime: LocalDateTime,
    onDateTimeChanged: (LocalDateTime) -> Unit,
    context: android.content.Context,
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
        DateTimePickerCard(
            label = "Time",
            value = timeLabel,
            modifier = Modifier.weight(1f),
            onClick = {
                TimePickerDialog(
                    context,
                    { _, hour, minute -> onDateTimeChanged(dateTime.withHour(hour).withMinute(minute)) },
                    dateTime.hour,
                    dateTime.minute,
                    false,
                ).show()
            },
        )
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
fun PreferredPeriodDropdown(
    periods: List<TimePeriod>,
    selectedPeriodId: String?,
    onSelected: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val productivePeriods = remember(periods) {
        periods
            .filter { it.type == TimePeriodType.PRODUCTIVE }
            .sortedWith(compareBy<TimePeriod> { minutesFromStart(it.start) }.thenBy { it.sortOrder })
    }
    val selectedPeriod = productivePeriods.firstOrNull { it.id == selectedPeriodId }
    val selectedLabel = selectedPeriod?.let { periodDropdownLabel(it) } ?: "Anytime"

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
                        "Anytime",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (selectedPeriodId == null) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                onClick = {
                    onSelected(null)
                    expanded = false
                },
            )
            productivePeriods.forEach { period ->
                DropdownMenuItem(
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                period.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (selectedPeriodId == period.id) FontWeight.Bold else FontWeight.Normal,
                            )
                            Text(
                                "${period.start.formatAsClock()} - ${period.end.formatAsClock()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = {
                        onSelected(period.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecurrenceSection(
    recurrenceType: RecurrenceType,
    recurrenceDays: Set<DayOfWeek>,
    onTypeChanged: (RecurrenceType) -> Unit,
    onDayToggle: (DayOfWeek) -> Unit,
) {
    TaskSectionTitle("Repeat")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RecurrenceType.entries.forEach { type ->
            FilterChip(
                selected = recurrenceType == type,
                onClick = { onTypeChanged(type) },
                label = { Text(type.displayName()) },
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
                            day.shortLabel().first().toString(),
                            color = if (day in recurrenceDays) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

