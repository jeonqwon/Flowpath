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
import dev.codex.reclaimoss.settings.AppSettings
import dev.codex.reclaimoss.settings.HistoryRetention
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

@Composable
fun PlannerScreen(
    padding: PaddingValues,
    state: PlannerUiState,
    settings: AppSettings,
    onRebuild: () -> Unit,
    onAddTask: () -> Unit,
    onToggleLock: (ScheduleBlock) -> Unit,
    onMarkDone: (ScheduleBlock) -> Unit,
    onReschedule: (String) -> Unit,
    onOpenTask: (String) -> Unit,
) {
    var visibleMonth by rememberSaveable { mutableStateOf(YearMonth.now()) }
    val zoneId = remember { ZoneId.systemDefault() }
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now(zoneId)) }
    val activeBlocks = remember(state.snapshot.blocks) {
        state.snapshot.blocks.filter { it.completionState != dev.codex.reclaimoss.domain.model.BlockCompletionState.COMPLETED }
    }
    val blocksByDate = remember(activeBlocks) {
        activeBlocks.groupBy { it.startAt.atZone(zoneId).toLocalDate() }
    }
    val tasksById = remember(state.snapshot.tasks) { state.snapshot.tasks.associateBy { it.id } }
    val periodsById = remember(state.snapshot.timePeriods) { state.snapshot.timePeriods.associateBy { it.id } }
    val remindersByDate = remember(state.snapshot.reminders) {
        state.snapshot.reminders
            .filter { it.status != ReminderStatus.COMPLETED }
            .groupBy { it.dueAt.atZone(zoneId).toLocalDate() }
    }
    val historyCutoff = remember(settings.historyRetention) {
        when (settings.historyRetention) {
            HistoryRetention.SEVEN_DAYS -> Instant.now().minusSeconds(7L * 24L * 60L * 60L)
            HistoryRetention.THIRTY_DAYS -> Instant.now().minusSeconds(30L * 24L * 60L * 60L)
            HistoryRetention.FOREVER -> null
        }
    }
    val completedTasksByDate = remember(state.snapshot.tasks, settings.historyRetention) {
        state.snapshot.tasks
            .filter { it.status == TaskStatus.COMPLETED }
            .filter { historyCutoff == null || !it.updatedAt.isBefore(historyCutoff) }
            .groupBy { it.updatedAt.atZone(zoneId).toLocalDate() }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 120.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(todayHeader(LocalDate.now(zoneId)), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                HeaderActionButton(label = "Add Task", icon = Icons.Outlined.Add, onClick = onAddTask)
            }
        }
        item {
            CalendarCard(
                month = visibleMonth,
                selectedDate = selectedDate,
                weekStart = settings.weekStart,
                blocksByDate = blocksByDate,
                remindersByDate = remindersByDate,
                tasksById = tasksById,
                onPreviousMonth = { visibleMonth = visibleMonth.minusMonths(1) },
                onNextMonth = { visibleMonth = visibleMonth.plusMonths(1) },
                onDateSelected = { selectedDate = it },
            )
        }
        selectedDayOverview(
            selectedDate = selectedDate,
            blocks = blocksByDate[selectedDate].orEmpty().sortedBy { it.startAt },
            reminders = remindersByDate[selectedDate].orEmpty().sortedBy { it.dueAt },
            completedTasks = completedTasksByDate[selectedDate].orEmpty().sortedByDescending { it.updatedAt },
            tasksById = tasksById,
            zoneId = zoneId,
            onToggleLock = onToggleLock,
            onMarkDone = onMarkDone,
            onReschedule = onReschedule,
            onOpenTask = onOpenTask,
        )
    }
}

fun LazyListScope.selectedDayOverview(
    selectedDate: LocalDate,
    blocks: List<ScheduleBlock>,
    reminders: List<Reminder>,
    completedTasks: List<ScheduleTask>,
    tasksById: Map<String, ScheduleTask>,
    zoneId: ZoneId,
    onToggleLock: (ScheduleBlock) -> Unit,
    onMarkDone: (ScheduleBlock) -> Unit,
    onReschedule: (String) -> Unit,
    onOpenTask: (String) -> Unit,
) {
    item {
        Text(
            selectedDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()) + " " + selectedDate.dayOfMonth,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
    item {
        OverviewCard(title = "Tasks") {
            if (blocks.isEmpty()) {
                Text("No tasks scheduled.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val groupedBlocks = blocks.groupBy { it.taskId }.values.sortedBy { group -> group.minOf { it.startAt } }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    groupedBlocks.forEach { taskBlocks ->
                        val task = tasksById[taskBlocks.first().taskId]
                        CompactTaskRow(
                            blocks = taskBlocks.sortedBy { it.startAt },
                            task = task,
                            zoneId = zoneId,
                            onOpen = { onOpenTask(taskBlocks.first().taskId) },
                        )
                    }
                }
            }
        }
    }
    item {
        OverviewCard(title = "Reminders") {
            if (reminders.isEmpty()) {
                Text("No reminders.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    reminders.forEach { reminder ->
                        ReminderMiniCard(reminder = reminder, zoneId = zoneId)
                    }
                }
            }
        }
    }
    item {
        OverviewCard(title = "History") {
            if (completedTasks.isEmpty()) {
                Text("No completed tasks.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    completedTasks.forEach { task ->
                        CompletedTaskHistoryCard(task = task, zoneId = zoneId)
                    }
                }
            }
        }
    }
}

@Composable
fun PlannerDayScreen(
    padding: PaddingValues,
    date: LocalDate,
    dayBlocks: List<ScheduleBlock>,
    dayReminders: List<Reminder>,
    tasksById: Map<String, ScheduleTask>,
    periodsById: Map<String, TimePeriod>,
    zoneId: ZoneId,
    onBack: () -> Unit,
    onToggleLock: (ScheduleBlock) -> Unit,
    onMarkDone: (ScheduleBlock) -> Unit,
    onReschedule: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    Spacer(Modifier.width(8.dp))
                    Text("Back")
                }
                Column {
                    Text("Day view", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()) + ", " +
                            date.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + date.dayOfMonth,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        if (dayBlocks.isEmpty() && dayReminders.isEmpty()) {
            item { EmptyCard("No tasks or reminders scheduled for this day yet.") }
        } else {
            if (dayBlocks.isNotEmpty()) {
                item {
                    Text("Tasks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            items(dayBlocks, key = { it.id }) { block ->
                val task = tasksById[block.taskId]
                DayTaskCard(
                    block = block,
                    task = task,
                    periodLabel = task?.preferredTimePeriodId?.let { periodsById[it]?.label } ?: "Anytime",
                    zoneId = zoneId,
                    onToggleLock = { onToggleLock(block) },
                    onMarkDone = { onMarkDone(block) },
                    onReschedule = { task?.let { onReschedule(it.id) } },
                )
            }
            if (dayReminders.isNotEmpty()) {
                item {
                    Text("Reminders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(dayReminders, key = { it.id }) { reminder ->
                    ReminderMiniCard(reminder = reminder, zoneId = zoneId)
                }
            }
        }
    }
}

@Composable
fun ReminderMiniCard(reminder: Reminder, zoneId: ZoneId) {
    val formatter = remember { DateTimeFormatter.ofPattern("MMM d, h:mm a") }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(reminder.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(formatter.format(reminder.dueAt.atZone(zoneId)), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun OverviewCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
fun CompactTaskRow(
    blocks: List<ScheduleBlock>,
    task: ScheduleTask?,
    zoneId: ZoneId,
    onOpen: () -> Unit,
) {
    val firstBlock = blocks.minByOrNull { it.startAt } ?: return
    val totalMinutes = blocks.sumOf { java.time.Duration.between(it.startAt, it.endAt).toMinutes().toInt() }
    val blockSummary = if (blocks.size == 1) {
        "${firstBlock.startAt.atZone(zoneId).toLocalTime().formatAsClock()} - ${firstBlock.endAt.atZone(zoneId).toLocalTime().formatAsClock()}"
    } else {
        "${firstBlock.startAt.atZone(zoneId).toLocalTime().formatAsClock()} start • ${totalMinutes.durationLabel()} total • ${blocks.size} blocks"
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpen)
                    .padding(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(task?.title ?: firstBlock.taskId, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    blockSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun CalendarCard(
    month: YearMonth,
    selectedDate: LocalDate,
    weekStart: WeekStart,
    blocksByDate: Map<LocalDate, List<ScheduleBlock>>,
    remindersByDate: Map<LocalDate, List<Reminder>>,
    tasksById: Map<String, ScheduleTask>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val today = remember { LocalDate.now() }
    val dayLabels = remember(weekStart) {
        if (weekStart == WeekStart.MONDAY) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        } else {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        }
    }
    val daysInMonth = remember(month, weekStart) {
        buildList<LocalDate?> {
            val first = month.atDay(1)
            val leadingSlots = when (weekStart) {
                WeekStart.SUNDAY -> first.dayOfWeek.value % 7
                WeekStart.MONDAY -> first.dayOfWeek.value - 1
            }
            repeat(leadingSlots) { add(null) }
            for (day in 1..month.lengthOfMonth()) {
                add(month.atDay(day))
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous month")
                }
                Text(
                    "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = onNextMonth) {
                    Icon(Icons.Outlined.ChevronRight, contentDescription = "Next month")
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                dayLabels.forEach { dayName ->
                    Text(dayName, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.height(310.dp),
                userScrollEnabled = false,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                gridItems(daysInMonth) { date ->
                    if (date == null) {
                        Spacer(modifier = Modifier.size(40.dp))
                    } else {
                        CalendarDayCell(
                            date = date,
                            isSelected = date == selectedDate,
                            isToday = date == today,
                            tasks = blocksByDate[date].orEmpty().mapNotNull { tasksById[it.taskId] }.distinctBy { it.id },
                            hasReminders = remindersByDate[date].orEmpty().isNotEmpty(),
                            onClick = { onDateSelected(date) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarDayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    tasks: List<ScheduleTask>,
    hasReminders: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.78f)
        else -> Color.Transparent
    }
    val background = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    Box(
        modifier = Modifier
            .height(48.dp)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(background, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 3.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            )
            if (tasks.isNotEmpty() || hasReminders) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (tasks.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size((4.dp + (tasks.size.coerceAtMost(4) * 1.5f).dp).coerceAtMost(10.dp))
                                .background(taskDotColor(tasks.size), RoundedCornerShape(999.dp)),
                        )
                    }
                    if (hasReminders) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(999.dp)),
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
fun DayTaskCard(
    block: ScheduleBlock,
    task: ScheduleTask?,
    periodLabel: String,
    zoneId: ZoneId,
    onToggleLock: () -> Unit,
    onMarkDone: () -> Unit,
    onReschedule: () -> Unit,
) {
    val formatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(task?.title ?: block.taskId, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Priority ${task?.priority?.name?.titlecase() ?: "Medium"} • $periodLabel",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(onClick = onToggleLock, modifier = Modifier.weight(1f)) {
                    Text(if (block.lockState == BlockLockState.LOCKED) "Unlock" else "Lock")
                }
                FilledTonalButton(onClick = onMarkDone, modifier = Modifier.weight(1f)) {
                    Text("Done")
                }
                FilledTonalButton(onClick = onReschedule, modifier = Modifier.weight(1f)) {
                    Text("Move")
                }
            }
        }
    }
}

