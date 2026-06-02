package dev.codex.reclaimoss.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
import dev.codex.reclaimoss.domain.model.Timeframe
import dev.codex.reclaimoss.domain.scheduling.ScheduleRebuildReason
import dev.codex.reclaimoss.domain.service.PlannerCoordinator
import dev.codex.reclaimoss.domain.service.TaskCreationResult
import dev.codex.reclaimoss.settings.AppSettings
import dev.codex.reclaimoss.settings.TasksViewMode
import dev.codex.reclaimoss.ui.HeaderActionHeight
import dev.codex.reclaimoss.ui.HeaderActionShape
import dev.codex.reclaimoss.ui.HeaderActionSlotWidth
import dev.codex.reclaimoss.ui.HeaderActionWidth
import dev.codex.reclaimoss.ui.dueDisplayText
import dev.codex.reclaimoss.ui.formatHourLabel
import dev.codex.reclaimoss.ui.minutesFromStart
import dev.codex.reclaimoss.ui.parseTimeframeColor
import dev.codex.reclaimoss.ui.reminderDateTimeFormatter
import dev.codex.reclaimoss.ui.timelineBlockHeight
import dev.codex.reclaimoss.ui.timelineOffset
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class TaskDaySection(
    val date: LocalDate,
    val blocks: List<ScheduleBlock>,
    val tasks: List<ScheduleTask>,
    val reminders: List<Reminder>,
    val timeframes: List<Timeframe>,
    val taskCount: Int,
    val reminderCount: Int,
)

internal data class TimeframeRailMetadata(
    val id: String,
    val name: String,
    val colorHex: String,
    val laneIndex: Int,
    val continuesFromPreviousDay: Boolean,
    val continuesIntoNextDay: Boolean,
)

internal data class TimeframeHeaderLabel(
    val name: String,
    val colorHex: String,
)

internal fun activeTimeframesForDay(
    timeframes: List<Timeframe>,
    day: LocalDate,
): List<Timeframe> = timeframes
    .filter { !day.isBefore(it.startDate) && !day.isAfter(it.endDate) }
    .sortedWith(compareBy<Timeframe> { it.startDate }.thenBy { it.endDate }.thenBy { it.name })

internal fun buildTaskDaySection(
    date: LocalDate,
    blocks: List<ScheduleBlock>,
    tasksById: Map<String, ScheduleTask>,
    reminders: List<Reminder>,
    timeframes: List<Timeframe>,
    zoneId: ZoneId,
): TaskDaySection {
    val visibleBlocks = visibleBlocksForDay(blocks, date, zoneId)
        .filter { it.completionState != dev.codex.reclaimoss.domain.model.BlockCompletionState.COMPLETED }
        .sortedBy { it.startAt }
    val tasks = visibleBlocks
        .mapNotNull { tasksById[it.taskId] }
        .distinctBy { it.id }
    val dayReminders = reminders
        .filter { it.status != ReminderStatus.COMPLETED }
        .filter { it.dueAt.atZone(zoneId).toLocalDate() == date }
        .sortedBy { it.dueAt }
    val dayTimeframes = activeTimeframesForDay(timeframes, date)
    return TaskDaySection(
        date = date,
        blocks = visibleBlocks,
        tasks = tasks,
        reminders = dayReminders,
        timeframes = dayTimeframes,
        taskCount = tasks.size,
        reminderCount = dayReminders.size,
    )
}

internal fun buildTimeframeRailMetadata(
    currentDayTimeframes: List<Timeframe>,
    previousDayTimeframes: List<Timeframe> = emptyList(),
    nextDayTimeframes: List<Timeframe> = emptyList(),
): List<TimeframeRailMetadata> {
    val previousIds = previousDayTimeframes.map { it.id }.toSet()
    val nextIds = nextDayTimeframes.map { it.id }.toSet()
    return currentDayTimeframes.mapIndexed { index, timeframe ->
        TimeframeRailMetadata(
            id = timeframe.id,
            name = timeframe.name,
            colorHex = timeframe.colorHex,
            laneIndex = index,
            continuesFromPreviousDay = timeframe.id in previousIds,
            continuesIntoNextDay = timeframe.id in nextIds,
        )
    }
}

private const val TaskFeedDayCount = 20001
private const val TaskFeedCenterIndex = TaskFeedDayCount / 2
private val ExpandedStickyHeaderMinHeight = 44.dp

private enum class TasksSheetType {
    ADD_CHOOSER,
    UPCOMING_REMINDERS,
    DAY_SUMMARY,
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TasksScreen(
    padding: PaddingValues,
    state: PlannerUiState,
    settings: AppSettings,
    isActive: Boolean,
    selectedDate: LocalDate,
    onSelectedDateChange: (LocalDate) -> Unit,
    onTasksViewModeChanged: (TasksViewMode) -> Unit,
    onAddTask: () -> Unit,
    onAddReminder: () -> Unit,
    onDeleteTask: (String) -> Unit,
    onOpenTask: (String) -> Unit,
    onOpenReminder: (Reminder) -> Unit,
) {
    val zoneId = remember { ZoneId.systemDefault() }
    val today = remember(zoneId) { LocalDate.now(zoneId) }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val collapsedListState = rememberLazyListState(initialFirstVisibleItemIndex = taskFeedIndexForDate(today, selectedDate))
    val expandedListState = rememberLazyListState(initialFirstVisibleItemIndex = expandedListIndexForDate(today, selectedDate))
    val tasksById = remember(state.snapshot.tasks) { state.snapshot.tasks.associateBy { it.id } }
    val activeReminders = remember(state.snapshot.reminders) { state.snapshot.reminders.filter { it.status != ReminderStatus.COMPLETED } }
    val dateFormatter = remember(settings.dateFormatPreference) {
        when (settings.dateFormatPreference) {
            dev.codex.reclaimoss.settings.DateFormatPreference.MONTH_DAY_YEAR -> DateTimeFormatter.ofPattern("EEE, MMM d")
            dev.codex.reclaimoss.settings.DateFormatPreference.DAY_MONTH_YEAR -> DateTimeFormatter.ofPattern("EEE, d MMM")
        }
    }
    val reminderFormatter = remember(settings.dateFormatPreference) { reminderDateTimeFormatter(settings.dateFormatPreference) }
    val hourHeight = 144.dp
    var showingSheet by rememberSaveable { mutableStateOf<TasksSheetType?>(null) }
    var selectedDaySummaryEpoch by rememberSaveable { mutableStateOf<Long?>(null) }
    val selectedDaySummary = selectedDaySummaryEpoch?.let { epoch ->
        buildTaskDaySection(
            date = LocalDate.ofEpochDay(epoch),
            blocks = state.snapshot.blocks,
            tasksById = tasksById,
            reminders = activeReminders,
            timeframes = state.snapshot.timeframes,
            zoneId = zoneId,
        )
    }

    LaunchedEffect(isActive, settings.tasksViewMode) {
        if (!isActive) return@LaunchedEffect
        when (settings.tasksViewMode) {
            TasksViewMode.COLLAPSED -> {
                collapsedListState.scrollToItem(taskFeedIndexForDate(today, selectedDate))
            }
            TasksViewMode.EXPANDED -> {
                expandedListState.scrollToItem(expandedListIndexForDate(today, selectedDate))
            }
        }
    }
    LaunchedEffect(isActive, settings.tasksViewMode, collapsedListState, expandedListState) {
        if (!isActive) return@LaunchedEffect
        val stateToWatch = if (settings.tasksViewMode == TasksViewMode.COLLAPSED) collapsedListState else expandedListState
        snapshotFlow { stateToWatch.firstVisibleItemIndex }
            .collect { index ->
                onSelectedDateChange(
                    when (settings.tasksViewMode) {
                        TasksViewMode.COLLAPSED -> taskFeedDateForIndex(today, index)
                        TasksViewMode.EXPANDED -> taskFeedDateForExpandedListIndex(today, index)
                    },
                )
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                IconButton(
                    onClick = {
                        onTasksViewModeChanged(
                            if (settings.tasksViewMode == TasksViewMode.COLLAPSED) TasksViewMode.EXPANDED else TasksViewMode.COLLAPSED,
                        )
                    },
                ) {
                    Text(
                        if (settings.tasksViewMode == TasksViewMode.COLLAPSED) "+" else "-",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                IconButton(
                    onClick = {
                        val targetDate = today
                        onSelectedDateChange(targetDate)
                        val targetIndex = when (settings.tasksViewMode) {
                            TasksViewMode.COLLAPSED -> taskFeedIndexForDate(today, targetDate)
                            TasksViewMode.EXPANDED -> expandedListIndexForDate(today, targetDate)
                        }
                        val offsetPx = with(density) {
                            if (settings.tasksViewMode == TasksViewMode.EXPANDED) {
                                (ExpandedStickyHeaderMinHeight + timelineOffset((minutesFromStart(LocalTime.now(zoneId)) - 60).coerceAtLeast(0), hourHeight)).roundToPx()
                            } else {
                                0
                            }
                        }
                        val listState = if (settings.tasksViewMode == TasksViewMode.COLLAPSED) collapsedListState else expandedListState
                        scope.launch {
                            listState.animateScrollToItem(targetIndex, offsetPx)
                        }
                    },
                ) {
                    Icon(Icons.Outlined.Home, contentDescription = "Go to today")
                }
                IconButton(onClick = { showingSheet = TasksSheetType.UPCOMING_REMINDERS }) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "Upcoming reminders")
                }
            }
            HeaderActionSlot {
                HeaderActionButton(label = "Add", icon = Icons.Outlined.Add, onClick = { showingSheet = TasksSheetType.ADD_CHOOSER })
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (settings.tasksViewMode) {
                TasksViewMode.COLLAPSED -> {
                    LazyColumn(
                        state = collapsedListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 260.dp),
                    ) {
                        items(TaskFeedDayCount, key = { index -> taskFeedDateForIndex(today, index).toEpochDay() }) { index ->
                            val date = taskFeedDateForIndex(today, index)
                            val section = buildTaskDaySection(
                                date = date,
                                blocks = state.snapshot.blocks,
                                tasksById = tasksById,
                                reminders = activeReminders,
                                timeframes = state.snapshot.timeframes,
                                zoneId = zoneId,
                            )
                            CollapsedTaskDayRow(
                                section = section,
                                railMetadata = railMetadataForDate(state.snapshot.timeframes, date),
                                formatter = dateFormatter,
                                today = today,
                                onClick = {
                                    selectedDaySummaryEpoch = section.date.toEpochDay()
                                    showingSheet = TasksSheetType.DAY_SUMMARY
                                },
                            )
                        }
                    }
                }
                TasksViewMode.EXPANDED -> {
                    LazyColumn(
                        state = expandedListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        contentPadding = PaddingValues(bottom = 260.dp),
                    ) {
                        for (index in 0 until TaskFeedDayCount) {
                            val date = taskFeedDateForIndex(today, index)
                            val section = buildTaskDaySection(
                                date = date,
                                blocks = state.snapshot.blocks,
                                tasksById = tasksById,
                                reminders = activeReminders,
                                timeframes = state.snapshot.timeframes,
                                zoneId = zoneId,
                            )
                            val railMetadata = railMetadataForDate(state.snapshot.timeframes, date)
                            stickyHeader(key = "tasks-header-${date.toEpochDay()}") {
                                ExpandedTaskDayHeader(
                                    section = section,
                                    railMetadata = railMetadata,
                                    formatter = dateFormatter,
                                )
                            }
                            item(key = "tasks-body-${date.toEpochDay()}") {
                                ExpandedTaskDaySection(
                                    section = section,
                                    railMetadata = railMetadata,
                                    tasksById = tasksById,
                                    zoneId = zoneId,
                                    hourHeight = hourHeight,
                                    allowConcurrentTasks = settings.allowConcurrentTasks,
                                    onOpenTask = onOpenTask,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    when (showingSheet) {
        TasksSheetType.ADD_CHOOSER -> {
            ModalBottomSheet(
                onDismissRequest = { showingSheet = null },
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                TasksSheetActionList(
                    title = "Add",
                    actions = listOf(
                        "Add Task" to onAddTask,
                        "Add Reminder" to onAddReminder,
                    ),
                    onDone = { showingSheet = null },
                )
            }
        }
        TasksSheetType.UPCOMING_REMINDERS -> {
            ModalBottomSheet(
                onDismissRequest = { showingSheet = null },
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                UpcomingRemindersSheet(
                    reminders = activeReminders.sortedBy { it.dueAt },
                    tasksById = tasksById,
                    formatter = reminderFormatter,
                    zoneId = zoneId,
                    onOpenReminder = { reminder ->
                        showingSheet = null
                        val linkedTaskId = reminder.linkedTaskId
                        if (linkedTaskId != null && tasksById.containsKey(linkedTaskId)) {
                            onOpenTask(linkedTaskId)
                        } else {
                            onOpenReminder(reminder)
                        }
                    },
                )
            }
        }
        TasksSheetType.DAY_SUMMARY -> {
            val section = selectedDaySummary
            if (section != null) {
                ModalBottomSheet(
                    onDismissRequest = { showingSheet = null },
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    DaySummarySheet(
                        section = section,
                        formatter = dateFormatter,
                        reminderFormatter = reminderFormatter,
                        zoneId = zoneId,
                        onAddTask = {
                            showingSheet = null
                            onAddTask()
                        },
                        onAddReminder = {
                            showingSheet = null
                            onAddReminder()
                        },
                        onExpand = {
                            showingSheet = null
                            onSelectedDateChange(section.date)
                            onTasksViewModeChanged(TasksViewMode.EXPANDED)
                        },
                        onOpenTask = {
                            showingSheet = null
                            onOpenTask(it)
                        },
                        onOpenReminder = { reminder ->
                            showingSheet = null
                            onOpenReminder(reminder)
                        },
                    )
                }
            }
        }
        null -> Unit
    }
}

private fun taskFeedIndexForDate(today: LocalDate, date: LocalDate): Int =
    (TaskFeedCenterIndex + ChronoUnit.DAYS.between(today, date).toInt()).coerceIn(0, TaskFeedDayCount - 1)

private fun expandedListIndexForDate(today: LocalDate, date: LocalDate): Int =
    taskFeedIndexForDate(today, date) * 2

private fun taskFeedDateForExpandedListIndex(today: LocalDate, index: Int): LocalDate =
    taskFeedDateForIndex(today, (index / 2).coerceIn(0, TaskFeedDayCount - 1))

private fun taskFeedDateForIndex(today: LocalDate, index: Int): LocalDate =
    today.plusDays((index - TaskFeedCenterIndex).toLong())

private fun railMetadataForDate(
    timeframes: List<Timeframe>,
    date: LocalDate,
): List<TimeframeRailMetadata> = buildTimeframeRailMetadata(
    currentDayTimeframes = activeTimeframesForDay(timeframes, date),
    previousDayTimeframes = activeTimeframesForDay(timeframes, date.minusDays(1)),
    nextDayTimeframes = activeTimeframesForDay(timeframes, date.plusDays(1)),
)

@Composable
private fun CollapsedTaskDayRow(
    section: TaskDaySection,
    railMetadata: List<TimeframeRailMetadata>,
    formatter: DateTimeFormatter,
    today: LocalDate,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 88.dp)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimeframeRailStrip(
                rails = railMetadata,
                modifier = Modifier.height(68.dp),
                compact = true,
                segment = TimeframeRailSegment.COMPACT,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    formatter.format(section.date) + if (section.date == today) " · Today" else "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    collapsedDaySummaryText(section),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun collapsedDaySummaryText(section: TaskDaySection): String = buildString {
    append(if (section.taskCount == 1) "1 task" else "${section.taskCount} tasks")
    append(" · ")
    append(if (section.reminderCount == 1) "1 reminder" else "${section.reminderCount} reminders")
}

@Composable
private fun ExpandedTaskDaySection(
    section: TaskDaySection,
    railMetadata: List<TimeframeRailMetadata>,
    tasksById: Map<String, ScheduleTask>,
    zoneId: ZoneId,
    hourHeight: Dp,
    allowConcurrentTasks: Boolean,
    onOpenTask: (String) -> Unit,
) {
    val timelineHeight = timelineOffset(minutes = 24 * 60, hourHeight = hourHeight)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(timelineHeight),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TimeframeRailStrip(
            rails = railMetadata,
            modifier = Modifier.height(timelineHeight),
            compact = false,
            segment = TimeframeRailSegment.BODY,
        )
        Box(modifier = Modifier.weight(1f)) {
            FullDayTimeline(
                blocks = section.blocks,
                tasksById = tasksById,
                zoneId = zoneId,
                day = section.date,
                hourHeight = hourHeight,
                allowConcurrentTasks = allowConcurrentTasks,
                onOpenTask = onOpenTask,
                onDeleteTask = {},
                showMidnightLabel = false,
            )
        }
    }
}

@Composable
private fun ExpandedTaskDayHeader(
    section: TaskDaySection,
    railMetadata: List<TimeframeRailMetadata>,
    formatter: DateTimeFormatter,
) {
    val timeframeLabels = stickyTimeframeHeaderLabels(railMetadata)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .heightIn(min = ExpandedStickyHeaderMinHeight)
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        TimeframeRailStrip(
            rails = railMetadata,
            modifier = Modifier
                .fillMaxHeight()
                .padding(top = 4.dp),
            compact = true,
            segment = TimeframeRailSegment.HEADER,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 100.dp, top = 4.dp, end = 12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    formatter.format(section.date),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                timeframeLabels.forEach { label ->
                    Text(
                        text = label.name,
                        modifier = Modifier
                            .background(
                                parseTimeframeColor(label.colorHex).copy(alpha = 0.22f),
                                RoundedCornerShape(10.dp),
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private enum class TimeframeRailSegment {
    COMPACT,
    HEADER,
    BODY,
}

internal fun stickyTimeframeHeaderLabels(rails: List<TimeframeRailMetadata>): List<TimeframeHeaderLabel> =
    rails.distinctBy { it.id }
        .map { TimeframeHeaderLabel(name = it.name, colorHex = it.colorHex) }

internal fun stickyTimeframeHeaderNames(rails: List<TimeframeRailMetadata>): String? =
    rails.map { it.name }
        .distinct()
        .takeIf { it.isNotEmpty() }
        ?.joinToString(" · ")

internal fun stickyTimeframeHeaderText(rails: List<TimeframeRailMetadata>): String? =
    rails.map { it.name }
        .distinct()
        .takeIf { it.isNotEmpty() }
        ?.joinToString(" · ")

@Composable
private fun TimeframeRailStrip(
    rails: List<TimeframeRailMetadata>,
    modifier: Modifier = Modifier,
    compact: Boolean,
    segment: TimeframeRailSegment = TimeframeRailSegment.COMPACT,
) {
    val visible = if (rails.isEmpty()) listOf<TimeframeRailMetadata?>(null) else rails.map { it }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        visible.forEach { rail ->
            if (rail == null) {
                Spacer(Modifier.width(if (compact) 4.dp else 5.dp))
            } else {
                val color = parseTimeframeColor(rail.colorHex)
                val topConnected = when (segment) {
                    TimeframeRailSegment.COMPACT -> rail.continuesFromPreviousDay
                    TimeframeRailSegment.HEADER -> rail.continuesFromPreviousDay
                    TimeframeRailSegment.BODY -> true
                }
                val bottomConnected = when (segment) {
                    TimeframeRailSegment.COMPACT -> rail.continuesIntoNextDay
                    TimeframeRailSegment.HEADER -> true
                    TimeframeRailSegment.BODY -> rail.continuesIntoNextDay
                }
                val shape = RoundedCornerShape(
                    topStart = if (topConnected) 0.dp else 10.dp,
                    topEnd = if (topConnected) 0.dp else 10.dp,
                    bottomStart = if (bottomConnected) 0.dp else 10.dp,
                    bottomEnd = if (bottomConnected) 0.dp else 10.dp,
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(if (compact) 4.dp else 5.dp)
                        .clip(shape)
                        .background(color.copy(alpha = 0.88f)),
                )
            }
        }
    }
}

@Composable
private fun TasksSheetActionList(
    title: String,
    actions: List<Pair<String, () -> Unit>>,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        actions.forEach { (label, action) ->
            FilledTonalButton(
                onClick = action,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(20.dp),
            ) {
                Text(label)
            }
        }
        TextButton(onClick = onDone, modifier = Modifier.align(Alignment.End)) {
            Text("Done")
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun UpcomingRemindersSheet(
    reminders: List<Reminder>,
    tasksById: Map<String, ScheduleTask>,
    formatter: DateTimeFormatter,
    zoneId: ZoneId,
    onOpenReminder: (Reminder) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Upcoming notifications", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (reminders.isEmpty()) {
            Text("No upcoming reminders.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(reminders, key = { it.id }) { reminder ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenReminder(reminder) },
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(reminder.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                reminder.dueDisplayText(formatter, DateTimeFormatter.ofPattern("MMM d"), zoneId),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            tasksById[reminder.linkedTaskId]?.let { linkedTask ->
                                Text(
                                    linkedTask.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DaySummarySheet(
    section: TaskDaySection,
    formatter: DateTimeFormatter,
    reminderFormatter: DateTimeFormatter,
    zoneId: ZoneId,
    onAddTask: () -> Unit,
    onAddReminder: () -> Unit,
    onExpand: () -> Unit,
    onOpenTask: (String) -> Unit,
    onOpenReminder: (Reminder) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(formatter.format(section.date), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(collapsedDaySummaryText(section), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (section.timeframes.isNotEmpty()) {
            Text(
                section.timeframes.joinToString(", ") { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (section.tasks.isNotEmpty()) {
            Text("Tasks", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            section.tasks.forEach { task ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenTask(task.id) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(task.status.name.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (section.reminders.isNotEmpty()) {
            Text("Reminders", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            section.reminders.forEach { reminder ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenReminder(reminder) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(reminder.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            reminder.dueDisplayText(reminderFormatter, DateTimeFormatter.ofPattern("MMM d"), zoneId),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        FilledTonalButton(onClick = onAddTask, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
            Text("Add Task")
        }
        FilledTonalButton(onClick = onAddReminder, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
            Text("Add Reminder")
        }
        TextButton(onClick = onExpand, modifier = Modifier.align(Alignment.End)) {
            Text("Expand Day")
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun HeaderActionSlot(
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.width(HeaderActionSlotWidth),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun HeaderActionButton(
    label: String,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    width: Dp = HeaderActionWidth,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .width(width)
            .height(HeaderActionHeight),
        shape = HeaderActionShape,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
        colors = ButtonDefaults.textButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, maxLines = 1)
    }
}

@Composable
fun FullDayTimeline(
    blocks: List<ScheduleBlock>,
    tasksById: Map<String, ScheduleTask>,
    zoneId: ZoneId,
    day: LocalDate,
    hourHeight: Dp,
    allowConcurrentTasks: Boolean = false,
    showMidnightLabel: Boolean = true,
    onOpenTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
) {
    val labelWidth = 92.dp
    val timelineHeight = timelineOffset(minutes = 24 * 60, hourHeight = hourHeight)
    val now = remember { LocalTime.now(zoneId) }
    val showNowIndicator = day == LocalDate.now(zoneId)

    val positionedBlocks = remember(blocks, allowConcurrentTasks) {
        if (allowConcurrentTasks) {
            computeTaskBlockLayout(blocks)
        } else {
            blocks.sortedBy { it.startAt }.map { PositionedTaskBlock(it, laneIndex = 0, totalLanes = 1) }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(timelineHeight),
    ) {
        val contentStart = labelWidth + 8.dp
        val contentWidth = maxWidth - contentStart
        for (hour in 0..24) {
            val top = timelineOffset(minutes = hour * 60, hourHeight = hourHeight)
            if (!(hour == 0 && !showMidnightLabel)) {
                Text(
                    LocalTime.of(hour % 24, 0).formatHourLabel(),
                    modifier = Modifier
                        .width(labelWidth)
                        .offset(y = if (hour == 0) top else top - 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = labelWidth)
                    .height(1.dp)
                    .offset(y = top)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)),
            )
        }
        Box(
            modifier = Modifier
                .offset(x = labelWidth - 6.dp)
                .width(1.dp)
                .height(timelineHeight)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        )
        positionedBlocks.forEach { positioned ->
            FullDayTaskBlock(
                positionedBlock = positioned,
                task = tasksById[positioned.block.taskId],
                zoneId = zoneId,
                contentStart = contentStart,
                contentWidth = contentWidth,
                hourHeight = hourHeight,
                onOpen = { onOpenTask(positioned.block.taskId) },
            )
        }
        if (showNowIndicator) {
            val nowTop = timelineOffset(minutes = minutesFromStart(now), hourHeight = hourHeight)
            Box(
                modifier = Modifier
                    .offset(x = labelWidth - 13.dp, y = nowTop - 7.dp)
                    .size(14.dp)
                    .zIndex(3f)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = labelWidth)
                    .height(2.dp)
                    .offset(y = nowTop)
                    .zIndex(3f)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)),
            )
        }
    }
}

data class PositionedTaskBlock(
    val block: ScheduleBlock,
    val laneIndex: Int,
    val totalLanes: Int,
)

fun visibleBlocksForDay(
    blocks: List<ScheduleBlock>,
    day: LocalDate,
    zoneId: ZoneId,
): List<ScheduleBlock> {
    val dayStart = day.atStartOfDay(zoneId).toInstant()
    val nextDayStart = day.plusDays(1).atStartOfDay(zoneId).toInstant()
    return blocks.mapNotNull { block ->
        val segmentStart = if (block.startAt >= dayStart) block.startAt else dayStart
        val segmentEnd = if (block.endAt <= nextDayStart) block.endAt else nextDayStart
        if (!segmentEnd.isAfter(segmentStart)) {
            null
        } else {
            block.copy(startAt = segmentStart, endAt = segmentEnd)
        }
    }
}

private fun computeTaskBlockLayout(blocks: List<ScheduleBlock>): List<PositionedTaskBlock> {
    data class ActiveLane(val endAt: Instant, val laneIndex: Int)
    data class AssignedBlock(val block: ScheduleBlock, val laneIndex: Int, val groupId: Int)

    val sorted = blocks.sortedBy { it.startAt }
    val active = mutableListOf<ActiveLane>()
    val assigned = mutableListOf<AssignedBlock>()
    var groupId = -1

    for (block in sorted) {
        active.removeAll { !it.endAt.isAfter(block.startAt) }
        if (active.isEmpty()) groupId += 1
        val usedLanes = active.map { it.laneIndex }.toSet()
        var laneIndex = 0
        while (laneIndex in usedLanes) laneIndex += 1
        active += ActiveLane(block.endAt, laneIndex)
        assigned += AssignedBlock(block, laneIndex, groupId)
    }

    val groupLaneCounts = assigned.groupBy { it.groupId }.mapValues { (_, group) ->
        group.maxOf { it.laneIndex } + 1
    }
    return assigned.map { PositionedTaskBlock(it.block, it.laneIndex, groupLaneCounts.getValue(it.groupId)) }
}

@Composable
fun CompletedTaskHistoryCard(task: ScheduleTask, zoneId: ZoneId) {
    val completedFormatter = remember { DateTimeFormatter.ofPattern("MMM d, h:mm a") }
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
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                task.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "Completed ${task.updatedAt.atZone(zoneId).format(completedFormatter)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun FullDayTaskBlock(
    positionedBlock: PositionedTaskBlock,
    task: ScheduleTask?,
    zoneId: ZoneId,
    contentStart: Dp,
    contentWidth: Dp,
    hourHeight: Dp,
    onOpen: () -> Unit,
) {
    val block = positionedBlock.block
    val start = block.startAt.atZone(zoneId).toLocalTime()
    val top = timelineOffset(minutes = minutesFromStart(start), hourHeight = hourHeight)
    val durationMinutes = java.time.Duration.between(block.startAt, block.endAt).toMinutes().toInt().coerceAtLeast(30)
    val height = timelineBlockHeight(minutes = durationMinutes, hourHeight = hourHeight, minHeight = 64.dp)
    val laneGap = 8.dp
    val laneCount = positionedBlock.totalLanes.coerceAtLeast(1)
    val laneWidth = (contentWidth - laneGap * (laneCount - 1)) / laneCount
    val xOffset = contentStart + (laneWidth + laneGap) * positionedBlock.laneIndex
    Card(
        modifier = Modifier
            .width(laneWidth)
            .height(height)
            .offset(x = xOffset, y = top)
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                task?.title ?: block.taskId,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

