package dev.codex.reclaimoss.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
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
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
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
import kotlin.math.roundToInt

internal data class TaskDaySection(
    val date: LocalDate,
    val segments: List<VisibleTaskSegment>,
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
    val startDate: LocalDate,
    val laneIndex: Int,
    val continuesFromPreviousDay: Boolean,
    val continuesIntoNextDay: Boolean,
)

internal data class TimeframeHeaderLabel(
    val name: String,
    val colorHex: String,
)

internal data class ExpandedHeaderTransition(
    val pinnedDayIndex: Int,
    val incomingDayIndex: Int? = null,
    val progress: Float = 0f,
)

internal enum class StickyHeaderTimeframeChipMotion {
    PINNED,
    EXITING,
    ENTERING,
}

internal data class TimeframeChipPlacement(
    val id: String,
    val name: String,
    val colorHex: String,
    val motion: StickyHeaderTimeframeChipMotion,
    val fromSlot: Int,
    val toSlot: Int,
    val progress: Float,
)

private data class ExpandedOverlayFragment(
    val segment: VisibleTaskSegment,
    val laneIndex: Int,
    val totalLanes: Int,
    val dayOffsetPx: Int,
)

private data class HeaderChipVerticalOffsets(
    val outgoingDateYPx: Int,
    val incomingDateYPx: Int?,
)

data class VisibleTaskSegment(
    val block: ScheduleBlock,
    val continuesFromPreviousDay: Boolean,
    val continuesIntoNextDay: Boolean,
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
    val visibleSegments = visibleTaskSegmentsForDay(blocks, date, zoneId)
        .filter { it.block.completionState != dev.codex.reclaimoss.domain.model.BlockCompletionState.COMPLETED }
        .sortedBy { it.block.startAt }
    val tasks = visibleSegments
        .mapNotNull { tasksById[it.block.taskId] }
        .distinctBy { it.id }
    val dayReminders = reminders
        .filter { it.status != ReminderStatus.COMPLETED }
        .filter { it.dueAt.atZone(zoneId).toLocalDate() == date }
        .sortedBy { it.dueAt }
    val dayTimeframes = activeTimeframesForDay(timeframes, date)
    return TaskDaySection(
        date = date,
        segments = visibleSegments,
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
            startDate = timeframe.startDate,
            laneIndex = index,
            continuesFromPreviousDay = timeframe.id in previousIds,
            continuesIntoNextDay = timeframe.id in nextIds,
        )
    }
}

private const val TaskFeedDayCount = 20001
private const val TaskFeedCenterIndex = TaskFeedDayCount / 2
private const val MaxOverlappingTimeframeRails = 5
private val ExpandedDayHeaderHeight = 48.dp
private val ExpandedDayHeaderTopInset = 10.dp
private val ExpandedDateChipSlotHeight = 30.dp
private val ExpandedDateChipGap = 16.dp
private val ExpandedDateChipAboveMidnightOffset = 38.dp
private val ExpandedTaskStickyTitleTopInset = 8.dp
private val TimeframeHeaderChipVerticalAdjustment = 2.dp
private val TimeframeHeaderChipMaxWidth = 96.dp
private val TimeframeHeaderChipSlotStep = 38.dp
private val TaskTimelineLabelWidth = 52.dp
private val TaskTimelineContentInset = 6.dp
private val TaskTimelineCompactRailWidth = 2.dp
private val TaskTimelineExpandedRailWidth = 2.dp
private val TaskTimelineDividerWidth = 0.75.dp
private val TaskTimelineRailGap = 4.dp
private val TaskTimelineBoundaryOverlap = 2.dp

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
    selectedDateScrollOffset: Int,
    autoScrollToNow: Boolean,
    onAutoScrollToNowConsumed: () -> Unit,
    onSelectedDateChange: (LocalDate) -> Unit,
    onScrollPositionChange: (LocalDate, Int) -> Unit,
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
    val initialTaskFeedIndex = taskFeedIndexForDate(today, selectedDate)
    val collapsedListState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialTaskFeedIndex,
        initialFirstVisibleItemScrollOffset = selectedDateScrollOffset,
    )
    val expandedListState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialTaskFeedIndex,
        initialFirstVisibleItemScrollOffset = selectedDateScrollOffset,
    )
    val tasksById = remember(state.snapshot.tasks) { state.snapshot.tasks.associateBy { it.id } }
    val activeReminders = remember(state.snapshot.reminders) { state.snapshot.reminders.filter { it.status != ReminderStatus.COMPLETED } }
    val dateFormatter = remember(settings.dateFormatPreference) {
        when (settings.dateFormatPreference) {
            dev.codex.reclaimoss.settings.DateFormatPreference.MONTH_DAY_YEAR -> DateTimeFormatter.ofPattern("EEE, MMM d")
            dev.codex.reclaimoss.settings.DateFormatPreference.DAY_MONTH_YEAR -> DateTimeFormatter.ofPattern("EEE, d MMM")
        }
    }
    val reminderFormatter = remember(settings.dateFormatPreference) { reminderDateTimeFormatter(settings.dateFormatPreference) }
    val hourHeight = settings.taskHourHeightDp.dp
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
        if (autoScrollToNow) return@LaunchedEffect
        when (settings.tasksViewMode) {
            TasksViewMode.COLLAPSED -> {
                collapsedListState.scrollToItem(taskFeedIndexForDate(today, selectedDate), selectedDateScrollOffset)
            }
            TasksViewMode.EXPANDED -> {
                expandedListState.scrollToItem(taskFeedIndexForDate(today, selectedDate), selectedDateScrollOffset)
            }
        }
    }
    LaunchedEffect(isActive, settings.tasksViewMode, autoScrollToNow, hourHeight) {
        if (!isActive || !autoScrollToNow) return@LaunchedEffect
        val targetIndex = taskFeedIndexForDate(today, today)
        val offsetPx = if (settings.tasksViewMode == TasksViewMode.EXPANDED) {
            with(density) {
                val nowMinutes = minutesFromStart(LocalTime.now(zoneId))
                val scrollMinutes = (nowMinutes - 60).coerceAtLeast(0)
                timelineOffset(scrollMinutes, hourHeight).roundToPx()
            }
        } else {
            0
        }
        val listState = if (settings.tasksViewMode == TasksViewMode.COLLAPSED) collapsedListState else expandedListState
        listState.scrollToItem(targetIndex, offsetPx)
        onAutoScrollToNowConsumed()
    }
    LaunchedEffect(isActive, settings.tasksViewMode, collapsedListState, expandedListState) {
        if (!isActive) return@LaunchedEffect
        val stateToWatch = if (settings.tasksViewMode == TasksViewMode.COLLAPSED) collapsedListState else expandedListState
        snapshotFlow { stateToWatch.firstVisibleItemIndex to stateToWatch.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                onScrollPositionChange(
                    taskFeedDateForIndex(today, index),
                    offset,
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
                        when (settings.tasksViewMode) {
                            TasksViewMode.COLLAPSED -> {
                                val targetIndex = collapsedListState.firstVisibleItemIndex
                                val targetDate = taskFeedDateForIndex(today, targetIndex)
                                val expandedDate = taskFeedDateForIndex(today, expandedListState.firstVisibleItemIndex)
                                val expandedOffset = if (expandedDate == targetDate) {
                                    expandedListState.firstVisibleItemScrollOffset
                                } else {
                                    0
                                }
                                if (expandedDate != targetDate) {
                                    scope.launch {
                                        expandedListState.scrollToItem(targetIndex, 0)
                                    }
                                }
                                onScrollPositionChange(targetDate, expandedOffset)
                                onTasksViewModeChanged(TasksViewMode.EXPANDED)
                            }
                            TasksViewMode.EXPANDED -> {
                                val targetIndex = expandedListState.firstVisibleItemIndex
                                val targetDate = taskFeedDateForIndex(today, targetIndex)
                                onScrollPositionChange(targetDate, 0)
                                onTasksViewModeChanged(TasksViewMode.COLLAPSED)
                            }
                        }
                    },
                ) {
                    Icon(
                        if (settings.tasksViewMode == TasksViewMode.COLLAPSED) Icons.Outlined.ZoomIn else Icons.Outlined.ZoomOut,
                        contentDescription = if (settings.tasksViewMode == TasksViewMode.COLLAPSED) "Expand tasks view" else "Collapse tasks view",
                    )
                }
                IconButton(
                    onClick = {
                        val targetDate = today
                        onSelectedDateChange(targetDate)
                        val targetIndex = taskFeedIndexForDate(today, targetDate)
                        val offsetPx = if (settings.tasksViewMode == TasksViewMode.EXPANDED) {
                            with(density) {
                                val nowMinutes = minutesFromStart(LocalTime.now(zoneId))
                                val scrollMinutes = (nowMinutes - 60).coerceAtLeast(0)
                                timelineOffset(scrollMinutes, hourHeight).roundToPx()
                            }
                        } else 0
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
            AnimatedContent(
                targetState = settings.tasksViewMode,
                transitionSpec = {
                    if (targetState == TasksViewMode.EXPANDED) {
                        (fadeIn(tween(300)) + expandVertically(tween(300), expandFrom = Alignment.Top))
                            .togetherWith(fadeOut(tween(200)) + shrinkVertically(tween(200), shrinkTowards = Alignment.Top))
                    } else {
                        (fadeIn(tween(300)) + expandVertically(tween(300), expandFrom = Alignment.Bottom))
                            .togetherWith(fadeOut(tween(200)) + shrinkVertically(tween(200), shrinkTowards = Alignment.Bottom))
                    }
                },
                label = "tasks-view-mode",
            ) { mode ->
                when (mode) {
                    TasksViewMode.COLLAPSED -> {
                        LazyColumn(
                            state = collapsedListState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(0.dp),
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
                                    onClick = {
                                        selectedDaySummaryEpoch = section.date.toEpochDay()
                                        showingSheet = TasksSheetType.DAY_SUMMARY
                                    },
                                )
                            }
                        }
                    }
                    TasksViewMode.EXPANDED -> {
                        val timelineHeight = timelineOffset(minutes = 24 * 60, hourHeight = hourHeight)
                        val timelineHeightPx = with(density) { timelineHeight.roundToPx() }

                        val headerHeightPx = with(density) { ExpandedDayHeaderHeight.roundToPx() }
                        val headerTransition by remember(expandedListState, timelineHeightPx, headerHeightPx) {
                            derivedStateOf {
                                val scrollPx = (
                                    expandedListState.firstVisibleItemIndex * timelineHeightPx +
                                        expandedListState.firstVisibleItemScrollOffset
                                    ).coerceAtLeast(0)
                                resolveExpandedHeaderTransition(
                                    scrollPx = scrollPx,
                                    dayHeightPx = timelineHeightPx,
                                    maxIndex = TaskFeedDayCount - 1,
                                    handoffHeightPx = headerHeightPx,
                                )
                            }
                        }
                        val pinnedDayIndex = headerTransition.pinnedDayIndex
                        val incomingDayIndex = headerTransition.incomingDayIndex
                        val pinnedDate = taskFeedDateForIndex(today, pinnedDayIndex)
                        val incomingDate = incomingDayIndex?.let { taskFeedDateForIndex(today, it) }
                        val pinnedRailMetadata = remember(pinnedDate, state.snapshot.timeframes) {
                            railMetadataForDate(state.snapshot.timeframes, pinnedDate)
                        }
                        val incomingRailMetadata = remember(incomingDate, state.snapshot.timeframes) {
                            incomingDate?.let { railMetadataForDate(state.snapshot.timeframes, it) }
                        }
                        val stickyHeaderTopPx = with(density) { ExpandedDayHeaderTopInset.roundToPx() }
                        val dateChipHeightPx = with(density) { ExpandedDateChipSlotHeight.roundToPx() }
                        val dateChipGapPx = with(density) { ExpandedDateChipGap.roundToPx() }
                        val dateChipAboveMidnightOffsetPx = with(density) { ExpandedDateChipAboveMidnightOffset.roundToPx() }
                        val headerDateChipOffsets by remember(
                            expandedListState,
                            pinnedDayIndex,
                            incomingDayIndex,
                            stickyHeaderTopPx,
                            dateChipHeightPx,
                            dateChipGapPx,
                            dateChipAboveMidnightOffsetPx,
                        ) {
                            derivedStateOf {
                                val visibleItems = expandedListState.layoutInfo.visibleItemsInfo.sortedBy { it.index }
                                fun chipYForIndex(index: Int): Int? {
                                    val position = visibleItems.indexOfFirst { it.index == index }
                                    if (position < 0) return null
                                    val nextOffsetPx = visibleItems.getOrNull(position + 1)?.offset ?: Int.MAX_VALUE
                                    return resolveExpandedDateChipY(
                                        bodyOffsetPx = visibleItems[position].offset - dateChipAboveMidnightOffsetPx,
                                        nextBodyOffsetPx = nextOffsetPx - dateChipAboveMidnightOffsetPx,
                                        stickyYPx = stickyHeaderTopPx,
                                        chipHeightPx = dateChipHeightPx,
                                        chipGapPx = dateChipGapPx,
                                    )
                                }

                                HeaderChipVerticalOffsets(
                                    outgoingDateYPx = chipYForIndex(pinnedDayIndex) ?: stickyHeaderTopPx,
                                    incomingDateYPx = incomingDayIndex?.let { chipYForIndex(it) },
                                )
                            }
                        }
                        val timeframeChipPlacements = remember(
                            pinnedRailMetadata,
                            incomingRailMetadata,
                            headerTransition.progress,
                        ) {
                            buildTimeframeChipPlacements(
                                currentRails = pinnedRailMetadata,
                                incomingRails = incomingRailMetadata,
                                progress = headerTransition.progress,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clipToBounds(),
                        ) {
                            LazyColumn(
                                state = expandedListState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(0.dp),
                                contentPadding = PaddingValues(bottom = 260.dp),
                            ) {
                                items(TaskFeedDayCount, key = { index -> taskFeedDateForIndex(today, index).toEpochDay() }) { index ->
                                    val date = taskFeedDateForIndex(today, index)
                                    val railMetadata = railMetadataForDate(state.snapshot.timeframes, date)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(timelineHeight),
                                        horizontalArrangement = Arrangement.spacedBy(TaskTimelineRailGap),
                                    ) {
                                        TimeframeRailStrip(
                                            rails = railMetadata,
                                            modifier = Modifier
                                                .width(timelineRailStripWidth(railMetadata, compact = false))
                                                .fillMaxHeight(),
                                            compact = false,
                                            segment = TimeframeRailSegment.BODY,
                                        )

                                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                            Box(
                                                modifier = Modifier
                                                    .offset(x = TaskTimelineLabelWidth - TaskTimelineDividerWidth)
                                                    .width(TaskTimelineDividerWidth)
                                                    .fillMaxHeight()
                                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                                            )

                                            FullDayTimeline(
                                                segments = emptyList(),
                                                tasksById = tasksById,
                                                zoneId = zoneId,
                                                day = date,
                                                hourHeight = hourHeight,
                                                allowConcurrentTasks = settings.allowConcurrentTasks,
                                                showTaskCards = false,
                                                showMidnightLabel = true,
                                                drawVerticalDivider = false,
                                                onOpenTask = onOpenTask,
                                                onDeleteTask = onDeleteTask,
                                            )

                                        }
                                    }
                                }
                            }

                            ExpandedTaskOverlay(
                                listState = expandedListState,
                                blocks = state.snapshot.blocks,
                                tasksById = tasksById,
                                today = today,
                                zoneId = zoneId,
                                hourHeight = hourHeight,
                                allowConcurrentTasks = settings.allowConcurrentTasks,
                                onOpenTask = onOpenTask,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .fillMaxSize()
                                    .zIndex(10f),
                            )
                            ExpandedNowIndicatorOverlay(
                                listState = expandedListState,
                                today = today,
                                zoneId = zoneId,
                                hourHeight = hourHeight,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .fillMaxSize()
                                    .zIndex(20f),
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .fillMaxWidth()
                                    .height(ExpandedDayHeaderHeight)
                                    .clipToBounds()
                                    .background(MaterialTheme.colorScheme.background)
                                    .zIndex(30f),
                            ) {
                                PinnedExpandedTimelineHeader(
                                    timeframePlacements = timeframeChipPlacements,
                                    outgoingDateYPx = headerDateChipOffsets.outgoingDateYPx,
                                    incomingDateYPx = headerDateChipOffsets.incomingDateYPx,
                                    modifier = Modifier,
                                )
                            }
                            ExpandedTimelineDateOverlay(
                                listState = expandedListState,
                                today = today,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .fillMaxSize()
                                    .zIndex(40f),
                            )
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

internal fun resolveExpandedHeaderTransition(
    scrollPx: Int,
    dayHeightPx: Int,
    maxIndex: Int,
    handoffHeightPx: Int = (dayHeightPx / 10).coerceAtLeast(1),
): ExpandedHeaderTransition {
    val safeDayHeightPx = dayHeightPx.coerceAtLeast(1)
    val safeHandoffHeightPx = handoffHeightPx.coerceIn(1, safeDayHeightPx)
    val safeScrollPx = scrollPx.coerceAtLeast(0)
    val baseIndex = (safeScrollPx / safeDayHeightPx).coerceIn(0, maxIndex)
    if (baseIndex >= maxIndex) {
        return ExpandedHeaderTransition(pinnedDayIndex = maxIndex)
    }

    val dayRemainderPx = safeScrollPx % safeDayHeightPx
    val handoffStartPx = safeDayHeightPx - safeHandoffHeightPx
    if (dayRemainderPx < handoffStartPx) {
        return ExpandedHeaderTransition(pinnedDayIndex = baseIndex)
    }

    val progress = ((dayRemainderPx - handoffStartPx).toFloat() / safeHandoffHeightPx)
        .coerceIn(0f, 1f)
    return ExpandedHeaderTransition(
        pinnedDayIndex = baseIndex,
        incomingDayIndex = (baseIndex + 1).coerceAtMost(maxIndex),
        progress = progress,
    )
}

internal fun buildTimeframeChipPlacements(
    currentRails: List<TimeframeRailMetadata>,
    incomingRails: List<TimeframeRailMetadata>? = null,
    progress: Float,
): List<TimeframeChipPlacement> {
    val orderedCurrent = currentRails.distinctBy { it.id }
    val orderedIncoming = incomingRails?.distinctBy { it.id }
    val clampedProgress = progress.coerceIn(0f, 1f)
    if (orderedIncoming == null) {
        return orderedCurrent.mapIndexed { slot, rail ->
            TimeframeChipPlacement(
                id = rail.id,
                name = rail.name,
                colorHex = rail.colorHex,
                motion = StickyHeaderTimeframeChipMotion.PINNED,
                fromSlot = slot,
                toSlot = slot,
                progress = 0f,
            )
        }
    }

    val incomingIds = orderedIncoming.map { it.id }.toSet()
    val currentIds = orderedCurrent.map { it.id }.toSet()
    val incomingSlotById = orderedIncoming
        .mapIndexed { slot, rail -> rail.id to slot }
        .toMap()

    return buildList {
        orderedCurrent.forEachIndexed { currentSlot, rail ->
            val incomingSlot = incomingSlotById[rail.id]
            add(
                TimeframeChipPlacement(
                    id = rail.id,
                    name = rail.name,
                    colorHex = rail.colorHex,
                    motion = if (rail.id in incomingIds) {
                        StickyHeaderTimeframeChipMotion.PINNED
                    } else {
                        StickyHeaderTimeframeChipMotion.EXITING
                    },
                    fromSlot = currentSlot,
                    toSlot = incomingSlot ?: currentSlot,
                    progress = clampedProgress,
                ),
            )
        }
        orderedIncoming.forEachIndexed { incomingSlot, rail ->
            if (rail.id !in currentIds) {
                add(
                    TimeframeChipPlacement(
                        id = rail.id,
                        name = rail.name,
                        colorHex = rail.colorHex,
                        motion = StickyHeaderTimeframeChipMotion.ENTERING,
                        fromSlot = incomingSlot,
                        toSlot = incomingSlot,
                        progress = clampedProgress,
                    ),
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
private fun CollapsedTaskDayRow(
    section: TaskDaySection,
    railMetadata: List<TimeframeRailMetadata>,
    onClick: () -> Unit,
) {
    val rowHeight = 88.dp

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight)
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Timeframe rails — same max-5 lane logic, same x-position as expanded
            TimeframeRailStrip(
                rails = railMetadata,
                modifier = Modifier
                    .width(timelineRailStripWidth(railMetadata, compact = true))
                    .fillMaxHeight(),
                compact = true,
                segment = TimeframeRailSegment.COMPACT,
            )

            Spacer(Modifier.width(TaskTimelineRailGap))

            // Date chip — centered in the label column, same slot as expanded
            TimelineDateChipSlot(
                date = section.date,
                modifier = Modifier.fillMaxHeight(),
            )

            // Vertical divider — continuous across rows (same x, flush rows)
            Box(
                modifier = Modifier
                    .width(TaskTimelineDividerWidth)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
            )

            Spacer(Modifier.width(12.dp))

            // Content — no card wrapper, just text
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = collapsedDaySummaryText(section),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }

        // Horizontal divider — only in content area to preserve continuous timeline
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .widthIn(min = 0.dp)
                .fillMaxWidth(1f)
                .height(TaskTimelineDividerWidth)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        )
    }
}

@Composable
private fun TimelineDateChipSlot(
    date: LocalDate,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.width(TaskTimelineLabelWidth),
        contentAlignment = Alignment.CenterStart,
    ) {
        DateChip(text = compactStickyDateText(date))
    }
}

@Composable
private fun ExpandedTaskOverlay(
    listState: LazyListState,
    blocks: List<ScheduleBlock>,
    tasksById: Map<String, ScheduleTask>,
    today: LocalDate,
    zoneId: ZoneId,
    hourHeight: Dp,
    allowConcurrentTasks: Boolean,
    onOpenTask: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val contentStart = maxTimelineRailStripWidth(compact = false) + TaskTimelineRailGap +
        TaskTimelineLabelWidth + TaskTimelineContentInset
    val isScrolling = listState.isScrollInProgress
    val scrollingState by rememberUpdatedState(isScrolling)
    val onOpenState by rememberUpdatedState(onOpenTask)

    BoxWithConstraints(
        modifier = modifier,
    ) {
        val contentWidth = maxWidth - contentStart
        val overlayHeightPx = with(density) { maxHeight.roundToPx() }
        val layoutInfo = listState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo
        if (visibleItems.isEmpty()) return@BoxWithConstraints

        val visibleDays = visibleItems.map { item ->
            item.index to (taskFeedDateForIndex(today, item.index) to item.offset)
        }
        val firstVisible = visibleItems.first()
        val firstIndex = firstVisible.index
        val firstOffsetPx = firstVisible.offset
        val firstVisibleDate = taskFeedDateForIndex(today, firstIndex)
        val firstVisibleDayStart = firstVisibleDate.atStartOfDay(zoneId)

        val viewportHeightPx = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).coerceAtLeast(0)
        val visibleTimelineTopPx = (-firstOffsetPx).coerceAtLeast(0)
        val visibleTimelineBottomPx = visibleTimelineTopPx + viewportHeightPx
        val hourHeightPx = with(density) { hourHeight.toPx() }
        val visibleTimelineTopMinutes =
            ((visibleTimelineTopPx / hourHeightPx) * 60f).toLong()
        val visibleTimelineBottomMinutes =
            kotlin.math.ceil((visibleTimelineBottomPx / hourHeightPx) * 60f).toLong()
        val viewportStartInstant = firstVisibleDayStart.plusSeconds(visibleTimelineTopMinutes * 60).toInstant()
        val viewportEndInstant = firstVisibleDayStart.plusSeconds(visibleTimelineBottomMinutes * 60).toInstant()

        val candidateSegments = remember(blocks, viewportStartInstant, viewportEndInstant) {
            blocks
                .asSequence()
                .filter { it.completionState != dev.codex.reclaimoss.domain.model.BlockCompletionState.COMPLETED }
                .filter { it.endAt > viewportStartInstant && it.startAt < viewportEndInstant }
                .sortedBy { it.startAt }
                .map {
                    VisibleTaskSegment(
                        block = it,
                        continuesFromPreviousDay = it.startAt < viewportStartInstant,
                        continuesIntoNextDay = it.endAt > viewportEndInstant,
                    )
                }
                .toList()
        }

        val positioned = remember(candidateSegments, allowConcurrentTasks) {
            if (allowConcurrentTasks) {
                computeTaskBlockLayout(candidateSegments)
            } else {
                candidateSegments.map { PositionedTaskBlock(it, laneIndex = 0, totalLanes = 1) }
            }
        }

        val fragments = remember(positioned, visibleDays, zoneId) {
            buildList {
                visibleDays.forEach { (_, dayAndOffset) ->
                    val (dayDate, dayOffsetPx) = dayAndOffset
                    positioned.forEach { positionedBlock ->
                        expandedTaskSegmentsForDay(
                            blocks = listOf(positionedBlock.segment.block),
                            day = dayDate,
                            zoneId = zoneId,
                        ).forEach { segment ->
                            add(
                                ExpandedOverlayFragment(
                                    segment = segment,
                                    laneIndex = positionedBlock.laneIndex,
                                    totalLanes = positionedBlock.totalLanes,
                                    dayOffsetPx = dayOffsetPx,
                                ),
                            )
                        }
                    }
                }
            }.sortedBy { fragment -> fragment.dayOffsetPx + minutesFromStart(fragment.segment.block.startAt.atZone(zoneId).toLocalTime()) }
        }

        val topmostFragmentByTaskId = remember(fragments, zoneId) {
            buildMap<String, ExpandedOverlayFragment> {
                fragments.forEach { fragment ->
                    val existing = get(fragment.segment.block.taskId)
                    val fragmentStart = fragment.dayOffsetPx + minutesFromStart(fragment.segment.block.startAt.atZone(zoneId).toLocalTime())
                    val existingStart = existing?.let {
                        it.dayOffsetPx + minutesFromStart(it.segment.block.startAt.atZone(zoneId).toLocalTime())
                    }
                    if (existing == null || fragmentStart < existingStart!!) {
                        put(fragment.segment.block.taskId, fragment)
                    }
                }
            }
        }

        fragments.forEach { fragment ->
            val block = fragment.segment.block
            val durationMinutes = java.time.Duration.between(block.startAt, block.endAt).toMinutes().toInt().coerceAtLeast(1)
            val topExtension = if (fragment.segment.continuesFromPreviousDay) TaskTimelineBoundaryOverlap else 0.dp
            val bottomExtension = if (fragment.segment.continuesIntoNextDay) TaskTimelineBoundaryOverlap else 0.dp
            val blockTopPx = fragment.dayOffsetPx + with(density) {
                (timelineOffset(minutesFromStart(block.startAt.atZone(zoneId).toLocalTime()), hourHeight) - topExtension).roundToPx()
            }
            val blockHeightPx = with(density) {
                (timelineOffset(durationMinutes, hourHeight) + topExtension + bottomExtension).roundToPx()
            }
            val absoluteYPx = blockTopPx
            val heightPx = blockHeightPx
            val visibleEnough =
                absoluteYPx + heightPx > 0 &&
                    absoluteYPx < overlayHeightPx
            if (visibleEnough) {
                val laneGap = 8.dp
                val laneCount = fragment.totalLanes.coerceAtLeast(1)
                val laneWidth = (contentWidth - laneGap * (laneCount - 1)) / laneCount
                val laneXOffset = contentStart + (laneWidth + laneGap) * fragment.laneIndex
                val titleReservePx = with(density) { 88.dp.roundToPx() }
                val titleTopInsetPx = with(density) {
                    if (fragment.segment.continuesFromPreviousDay || absoluteYPx < 0) {
                        6.dp.roundToPx()
                    } else {
                        14.dp.roundToPx()
                    }
                }
                val stickyTitleMinYPx = with(density) {
                    (ExpandedDayHeaderHeight + ExpandedTaskStickyTitleTopInset).roundToPx()
                }
                val titleNaturalYPx = absoluteYPx + titleTopInsetPx
                val titleMaxYPx = absoluteYPx + (heightPx - titleReservePx).coerceAtLeast(0) + titleTopInsetPx
                val stickyTitleYPx = titleNaturalYPx
                    .coerceAtLeast(stickyTitleMinYPx)
                    .coerceAtMost(titleMaxYPx)
                FullDayTaskBlock(
                    positionedBlock = PositionedTaskBlock(fragment.segment, fragment.laneIndex, fragment.totalLanes),
                    task = tasksById[block.taskId],
                    zoneId = zoneId,
                    contentStart = contentStart,
                    contentWidth = contentWidth,
                    hourHeight = hourHeight,
                    onOpen = { onOpenState(block.taskId) },
                    absoluteY = with(density) { absoluteYPx.toDp() },
                    absoluteHeight = with(density) { heightPx.toDp() },
                    renderContinuesFromPrevious = fragment.segment.continuesFromPreviousDay,
                    renderContinuesIntoNext = fragment.segment.continuesIntoNextDay,
                    showTitle = false,
                    useTapGesture = true,
                    isScrollInProgress = scrollingState,
                    onScrollBy = { deltaY ->
                        listState.dispatchRawDelta(-deltaY)
                    },
                )
                if (topmostFragmentByTaskId[block.taskId] == fragment) {
                    StickyOverlayTaskTitle(
                        title = tasksById[block.taskId]?.title ?: block.taskId,
                        xOffset = laneXOffset + 18.dp,
                        yOffset = with(density) { stickyTitleYPx.toDp() },
                        width = (laneWidth - 36.dp).coerceAtLeast(0.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StickyOverlayTaskTitle(
    title: String,
    xOffset: Dp,
    yOffset: Dp,
    width: Dp,
) {
    Text(
        text = title,
        modifier = Modifier
            .offset(x = xOffset, y = yOffset)
            .width(width)
            .zIndex(12f),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun PinnedExpandedTimelineHeader(
    timeframePlacements: List<TimeframeChipPlacement>,
    outgoingDateYPx: Int,
    incomingDateYPx: Int?,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val headerTopInsetPx = with(density) { ExpandedDayHeaderTopInset.roundToPx() }
    val timeframeVerticalAdjustmentPx = with(density) { TimeframeHeaderChipVerticalAdjustment.roundToPx() }
    val chipStartPx = with(density) {
        (maxTimelineRailStripWidth(compact = false) +
            TaskTimelineRailGap +
            TaskTimelineLabelWidth +
            TaskTimelineContentInset).roundToPx()
    }
    val chipStridePx = with(density) { TimeframeHeaderChipSlotStep.roundToPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ExpandedDayHeaderHeight),
    ) {
        timeframePlacements.forEach { placement ->
            val slotProgress = placement.progress.coerceIn(0f, 1f)
            val slot = placement.fromSlot + ((placement.toSlot - placement.fromSlot) * slotProgress)
            val chipOffsetY = when (placement.motion) {
                StickyHeaderTimeframeChipMotion.PINNED -> headerTopInsetPx
                StickyHeaderTimeframeChipMotion.EXITING -> outgoingDateYPx
                StickyHeaderTimeframeChipMotion.ENTERING -> incomingDateYPx ?: outgoingDateYPx
            } + timeframeVerticalAdjustmentPx
            TimeframeNameChip(
                text = placement.name,
                borderColor = parseTimeframeColor(placement.colorHex),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset {
                        IntOffset(
                            chipStartPx + (chipStridePx * slot).roundToInt(),
                            chipOffsetY,
                        )
                    }
                    .zIndex(1f)
                    .widthIn(max = TimeframeHeaderChipMaxWidth),
            )
        }
    }
}

@Composable
private fun ExpandedTimelineDateOverlay(
    listState: LazyListState,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val railStripOffsetPx = with(density) {
        (maxTimelineRailStripWidth(compact = false) + TaskTimelineRailGap).roundToPx()
    }
    val stickyYPx = with(density) { ExpandedDayHeaderTopInset.roundToPx() }
    val chipHeightPx = with(density) { ExpandedDateChipSlotHeight.roundToPx() }
    val chipGapPx = with(density) { ExpandedDateChipGap.roundToPx() }
    val dateChipAboveMidnightOffsetPx = with(density) { ExpandedDateChipAboveMidnightOffset.roundToPx() }
    val visibleItems = listState.layoutInfo.visibleItemsInfo.sortedBy { it.index }

    Box(modifier = modifier.clipToBounds()) {
        visibleItems.forEachIndexed { position, item ->
            val nextOffsetPx = visibleItems.getOrNull(position + 1)?.offset ?: Int.MAX_VALUE
            val chipYPx = resolveExpandedDateChipY(
                bodyOffsetPx = item.offset - dateChipAboveMidnightOffsetPx,
                nextBodyOffsetPx = nextOffsetPx - dateChipAboveMidnightOffsetPx,
                stickyYPx = stickyYPx,
                chipHeightPx = chipHeightPx,
                chipGapPx = chipGapPx,
            )
            TimelineDateChipSlot(
                date = taskFeedDateForIndex(today, item.index),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset { IntOffset(railStripOffsetPx, chipYPx) }
                    .height(ExpandedDateChipSlotHeight),
            )
        }
    }
}

@Composable
private fun ExpandedNowIndicatorOverlay(
    listState: LazyListState,
    today: LocalDate,
    zoneId: ZoneId,
    hourHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val todayIndex = taskFeedIndexForDate(today, today)
    val todayItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == todayIndex } ?: return
    val now = remember(zoneId) { LocalTime.now(zoneId) }
    val nowTop = with(density) { todayItem.offset.toDp() } +
        timelineOffset(minutes = minutesFromStart(now), hourHeight = hourHeight)
    val railStripOffset = maxTimelineRailStripWidth(compact = false) + TaskTimelineRailGap
    val lineStart = railStripOffset + TaskTimelineLabelWidth

    Box(modifier = modifier.clipToBounds()) {
        Box(
            modifier = Modifier
                .offset(x = lineStart - 13.dp, y = nowTop - 7.dp)
                .size(14.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.primary),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = lineStart)
                .height(2.dp)
                .offset(y = nowTop)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)),
        )
    }
}

internal fun resolveExpandedDateChipY(
    bodyOffsetPx: Int,
    nextBodyOffsetPx: Int,
    stickyYPx: Int,
    chipHeightPx: Int,
    chipGapPx: Int = 0,
): Int {
    val pinnedY = bodyOffsetPx.coerceAtLeast(stickyYPx)
    return pinnedY.coerceAtMost(nextBodyOffsetPx - chipHeightPx - chipGapPx)
}

@Composable
private fun DateChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun TimeframeNameChip(
    text: String,
    borderColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = borderColor.copy(alpha = 0.88f),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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

internal fun compactStickyDateText(date: LocalDate): String = "${date.dayOfMonth}/${date.monthValue}"

internal fun shouldShowTimelineHourLabel(hour: Int): Boolean = hour != 24

internal fun shouldShowTimelineHourDivider(hour: Int): Boolean = hour in 0..24

internal fun stickyTimeframeCardText(rails: List<TimeframeRailMetadata>): String? =
    rails.map { it.name }
        .distinct()
        .takeIf { it.isNotEmpty() }
        ?.joinToString(" \u00B7 ")

internal fun stickyTimeframeHeaderNames(rails: List<TimeframeRailMetadata>): String? =
    rails.map { it.name }
        .distinct()
        .takeIf { it.isNotEmpty() }
        ?.joinToString(" \u00B7 ")

internal fun stickyTimeframeHeaderText(rails: List<TimeframeRailMetadata>): String? =
    rails.map { it.name }
        .distinct()
        .takeIf { it.isNotEmpty() }
        ?.joinToString(" \u00B7 ")

private fun timelineRailStripWidth(rails: List<TimeframeRailMetadata>, compact: Boolean): Dp {
    val railWidth = if (compact) TaskTimelineCompactRailWidth else TaskTimelineExpandedRailWidth
    return railWidth * MaxOverlappingTimeframeRails
}

private fun maxTimelineRailStripWidth(compact: Boolean): Dp {
    val railWidth = if (compact) TaskTimelineCompactRailWidth else TaskTimelineExpandedRailWidth
    return railWidth * MaxOverlappingTimeframeRails
}

private fun orderedTimeframeRailsForDisplay(
    rails: List<TimeframeRailMetadata>,
): List<TimeframeRailMetadata> {
    return rails
        .distinctBy { it.id }
        .sortedWith(
            compareBy<TimeframeRailMetadata> { it.startDate }
                .thenBy { it.name }
                .thenBy { it.id }
        )
        .take(MaxOverlappingTimeframeRails)
        .asReversed()
}

@Composable
private fun TimeframeRailStrip(
    rails: List<TimeframeRailMetadata>,
    modifier: Modifier = Modifier,
    compact: Boolean,
    segment: TimeframeRailSegment = TimeframeRailSegment.COMPACT,
) {
    val railWidth = if (compact) TaskTimelineCompactRailWidth else TaskTimelineExpandedRailWidth
    val orderedRails = orderedTimeframeRailsForDisplay(rails)
    val visible: List<TimeframeRailMetadata?> =
        if (orderedRails.isEmpty()) {
            List(MaxOverlappingTimeframeRails) { null }
        } else {
            val blanks = List((MaxOverlappingTimeframeRails - orderedRails.size).coerceAtLeast(0)) { null }
            blanks + orderedRails
        }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        visible.forEach { rail ->
            if (rail == null) {
                Spacer(Modifier.width(railWidth))
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
                        .width(railWidth)
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
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        actions.forEach { (label, action) ->
            FilledTonalButton(
                onClick = action,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(20.dp),
            ) {
                Text(label)
            }
        }
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
    segments: List<VisibleTaskSegment>,
    tasksById: Map<String, ScheduleTask>,
    zoneId: ZoneId,
    day: LocalDate,
    hourHeight: Dp,
    allowConcurrentTasks: Boolean = false,
    showTaskCards: Boolean = true,
    showMidnightLabel: Boolean = true,
    drawVerticalDivider: Boolean = true,
    onOpenTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
) {
    val labelWidth = TaskTimelineLabelWidth
    val timelineHeight = timelineOffset(minutes = 24 * 60, hourHeight = hourHeight)
    val now = remember { LocalTime.now(zoneId) }
    val showNowIndicator = day == LocalDate.now(zoneId)

    val positionedBlocks = remember(segments, allowConcurrentTasks, showTaskCards) {
        if (!showTaskCards) emptyList()
        else if (allowConcurrentTasks) {
            computeTaskBlockLayout(segments)
        } else {
            segments.sortedBy { it.block.startAt }.map { PositionedTaskBlock(it, laneIndex = 0, totalLanes = 1) }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(timelineHeight),
    ) {
        val contentStart = labelWidth + TaskTimelineContentInset
        val contentWidth = maxWidth - contentStart
        for (hour in 0..24) {
            val top = timelineOffset(minutes = hour * 60, hourHeight = hourHeight)
            if (shouldShowTimelineHourLabel(hour) && (hour != 0 || showMidnightLabel)) {
                Text(
                    LocalTime.of(hour, 0).formatHourLabel(),
                    modifier = Modifier
                        .width(labelWidth)
                        .offset(y = if (hour == 0) top else top - 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (shouldShowTimelineHourDivider(hour)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = labelWidth)
                        .height(TaskTimelineDividerWidth)
                        .offset(y = top)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)),
                )
            }
        }
        if (drawVerticalDivider) {
            Box(
                modifier = Modifier
                    .offset(x = labelWidth - TaskTimelineDividerWidth)
                    .width(TaskTimelineDividerWidth)
                    .height(timelineHeight)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
            )
        }
        positionedBlocks.forEach { positioned ->
            FullDayTaskBlock(
                positionedBlock = positioned,
                task = tasksById[positioned.segment.block.taskId],
                zoneId = zoneId,
                contentStart = contentStart,
                contentWidth = contentWidth,
                hourHeight = hourHeight,
                onOpen = { onOpenTask(positioned.segment.block.taskId) },
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
    val segment: VisibleTaskSegment,
    val laneIndex: Int,
    val totalLanes: Int,
)

fun visibleBlocksForDay(
    blocks: List<ScheduleBlock>,
    day: LocalDate,
    zoneId: ZoneId,
): List<ScheduleBlock> {
    return visibleTaskSegmentsForDay(blocks, day, zoneId).map { it.block }
}

fun visibleTaskSegmentsForDay(
    blocks: List<ScheduleBlock>,
    day: LocalDate,
    zoneId: ZoneId,
): List<VisibleTaskSegment> {
    val dayStart = day.atStartOfDay(zoneId).toInstant()
    val nextDayStart = day.plusDays(1).atStartOfDay(zoneId).toInstant()
    return blocks.mapNotNull { block ->
        val segmentStart = if (block.startAt >= dayStart) block.startAt else dayStart
        val segmentEnd = if (block.endAt <= nextDayStart) block.endAt else nextDayStart
        if (!segmentEnd.isAfter(segmentStart)) {
            null
        } else {
            VisibleTaskSegment(
                block = block.copy(startAt = segmentStart, endAt = segmentEnd),
                continuesFromPreviousDay = block.startAt < dayStart,
                continuesIntoNextDay = block.endAt > nextDayStart,
            )
        }
    }
}

internal fun expandedTaskSegmentsForDay(
    blocks: List<ScheduleBlock>,
    day: LocalDate,
    zoneId: ZoneId,
): List<VisibleTaskSegment> = visibleTaskSegmentsForDay(blocks, day, zoneId)

private fun computeTaskBlockLayout(segments: List<VisibleTaskSegment>): List<PositionedTaskBlock> {
    data class ActiveLane(val endAt: Instant, val laneIndex: Int)
    data class AssignedBlock(val segment: VisibleTaskSegment, val laneIndex: Int, val groupId: Int)

    val sorted = segments.sortedBy { it.block.startAt }
    val active = mutableListOf<ActiveLane>()
    val assigned = mutableListOf<AssignedBlock>()
    var groupId = -1

    for (segment in sorted) {
        active.removeAll { !it.endAt.isAfter(segment.block.startAt) }
        if (active.isEmpty()) groupId += 1
        val usedLanes = active.map { it.laneIndex }.toSet()
        var laneIndex = 0
        while (laneIndex in usedLanes) laneIndex += 1
        active += ActiveLane(segment.block.endAt, laneIndex)
        assigned += AssignedBlock(segment, laneIndex, groupId)
    }

    val groupLaneCounts = assigned.groupBy { it.groupId }.mapValues { (_, group) ->
        group.maxOf { it.laneIndex } + 1
    }
    return assigned.map { PositionedTaskBlock(it.segment, it.laneIndex, groupLaneCounts.getValue(it.groupId)) }
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
    absoluteY: Dp? = null,
    absoluteHeight: Dp? = null,
    renderContinuesFromPrevious: Boolean = positionedBlock.segment.continuesFromPreviousDay,
    renderContinuesIntoNext: Boolean = positionedBlock.segment.continuesIntoNextDay,
    showTitle: Boolean = true,
    stickyTitleOffset: Dp = 0.dp,
    useTapGesture: Boolean = false,
    isScrollInProgress: Boolean = false,
    onScrollBy: ((Float) -> Unit)? = null,
) {
    val block = positionedBlock.segment.block
    val density = LocalDensity.current
    val start = block.startAt.atZone(zoneId).toLocalTime()
    val topExtension = if (renderContinuesFromPrevious) TaskTimelineBoundaryOverlap else 0.dp
    val bottomExtension = if (renderContinuesIntoNext) TaskTimelineBoundaryOverlap else 0.dp
    val computedTop = timelineOffset(minutes = minutesFromStart(start), hourHeight = hourHeight) - topExtension
    val durationMinutes = java.time.Duration.between(block.startAt, block.endAt).toMinutes().toInt().coerceAtLeast(30)
    val computedHeight = timelineBlockHeight(minutes = durationMinutes, hourHeight = hourHeight, minHeight = 64.dp) + topExtension + bottomExtension
    val top = absoluteY ?: computedTop
    val height = absoluteHeight ?: computedHeight
    val laneGap = 8.dp
    val laneCount = positionedBlock.totalLanes.coerceAtLeast(1)
    val laneWidth = (contentWidth - laneGap * (laneCount - 1)) / laneCount
    val xOffset = contentStart + (laneWidth + laneGap) * positionedBlock.laneIndex
    val shape = RoundedCornerShape(
        topStart = if (renderContinuesFromPrevious) 0.dp else 22.dp,
        topEnd = if (renderContinuesFromPrevious) 0.dp else 22.dp,
        bottomStart = if (renderContinuesIntoNext) 0.dp else 22.dp,
        bottomEnd = if (renderContinuesIntoNext) 0.dp else 22.dp,
    )
    val showsBoundaryContinuation =
        renderContinuesFromPrevious || renderContinuesIntoNext
    val cardModifier = Modifier
        .requiredWidth(laneWidth)
        .requiredHeight(height)
        .offset(x = xOffset, y = top)
        .zIndex(1f)
    val scrolling by rememberUpdatedState(isScrollInProgress)
    val tapCallback by rememberUpdatedState(onOpen)
    val scrollCallback by rememberUpdatedState(onScrollBy)
    val cardScrollableState = rememberScrollableState { delta ->
        scrollCallback?.invoke(delta)
        delta
    }
    val positionedModifier = if (useTapGesture) {
        cardModifier
            .scrollable(
                state = cardScrollableState,
                orientation = Orientation.Vertical,
            )
            .clickable(
                enabled = !scrolling,
                onClick = { if (!scrolling) tapCallback() },
            )
    } else {
        cardModifier.clickable(
            enabled = !scrolling,
            onClick = { if (!scrolling) tapCallback() },
        )
    }
    Card(
        modifier = positionedModifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            width = if (showsBoundaryContinuation) 0.dp else 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (showsBoundaryContinuation) 0.dp else 4.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            if (showTitle) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(horizontal = 18.dp)
                        .offset(y = stickyTitleOffset + 14.dp),
                ) {
                    Text(
                        text = task?.title ?: block.taskId,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

