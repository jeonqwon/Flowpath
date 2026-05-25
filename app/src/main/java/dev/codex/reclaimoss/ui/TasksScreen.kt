package dev.codex.reclaimoss.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import dev.codex.reclaimoss.domain.model.TimePeriod
import dev.codex.reclaimoss.domain.model.TimePeriodType
import dev.codex.reclaimoss.domain.scheduling.ScheduleRebuildReason
import dev.codex.reclaimoss.domain.service.PlannerCoordinator
import dev.codex.reclaimoss.domain.service.TaskCreationResult
import dev.codex.reclaimoss.settings.AppSettings
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
fun TasksScreen(
    padding: PaddingValues,
    state: PlannerUiState,
    settings: AppSettings,
    selectedDate: LocalDate,
    savedScrollOffset: Int,
    autoPositionNonce: Int,
    onSelectedDateChange: (LocalDate) -> Unit,
    onScrollOffsetChange: (Int) -> Unit,
    onAddTask: () -> Unit,
    onDeleteTask: (String) -> Unit,
    onOpenTask: (String) -> Unit,
) {
    val zoneId = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zoneId) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = 0, initialFirstVisibleItemScrollOffset = savedScrollOffset)
    val density = LocalDensity.current
    val blocksToday = remember(state.snapshot.blocks, selectedDate) {
        state.snapshot.blocks
            .filter { it.startAt.atZone(zoneId).toLocalDate() == selectedDate }
            .filter { it.completionState != dev.codex.reclaimoss.domain.model.BlockCompletionState.COMPLETED }
    }
    val tasksById = remember(state.snapshot.tasks) { state.snapshot.tasks.associateBy { it.id } }
    val lifePeriods = remember(state.snapshot.timePeriods) {
        state.snapshot.timePeriods.filter { it.type == TimePeriodType.LIFE }
    }
    val hourHeight = 144.dp
    val darkThemeHeader = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val todayHeaderColor = if (darkThemeHeader) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
    }
    val todayHeaderTextColor = if (darkThemeHeader) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    LaunchedEffect(autoPositionNonce) {
        val offsetMinutes = if (selectedDate == today) {
            (minutesFromStart(LocalTime.now(zoneId)) - 60).coerceAtLeast(0)
        } else {
            0
        }
        val offsetPx = with(density) { timelineOffset(offsetMinutes, hourHeight).roundToPx() }
        listState.scrollToItem(index = 0, scrollOffset = offsetPx)
        onScrollOffsetChange(offsetPx)
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemScrollOffset }
            .collect { offset -> onScrollOffsetChange(offset) }
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
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                IconButton(onClick = { onSelectedDateChange(selectedDate.minusDays(1)) }) {
                    Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous day")
                }
                Text(
                    headerDateLabel(selectedDate, settings.dateFormatPreference),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (selectedDate == today) {
                                todayHeaderColor
                            } else {
                                Color.Transparent
                            },
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (selectedDate == today) todayHeaderTextColor else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = { onSelectedDateChange(selectedDate.plusDays(1)) }) {
                    Icon(Icons.Outlined.ChevronRight, contentDescription = "Next day")
                }
            }
            HeaderActionButton(label = "Add Task", icon = Icons.Outlined.Add, onClick = onAddTask)
        }

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 260.dp),
        ) {
            item {
                FullDayTimeline(
                    blocks = blocksToday,
                    lifePeriods = lifePeriods,
                    tasksById = tasksById,
                    zoneId = zoneId,
                    day = selectedDate,
                    hourHeight = hourHeight,
                    allowConcurrentTasks = settings.allowConcurrentTasks,
                    onOpenTask = onOpenTask,
                    onDeleteTask = onDeleteTask,
                )
            }
        }
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
            .height(HeaderActionHeight)
            .wrapContentWidth(Alignment.End),
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
    lifePeriods: List<TimePeriod>,
    tasksById: Map<String, ScheduleTask>,
    zoneId: ZoneId,
    day: LocalDate,
    hourHeight: Dp,
    allowConcurrentTasks: Boolean = false,
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
            Text(
                LocalTime.of(hour % 24, 0).formatHourLabel(),
                modifier = Modifier
                    .width(labelWidth)
                    .offset(y = if (hour == 0) top else top - 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
        lifePeriods.forEach { period ->
            FullDayLifeBlock(
                period = period,
                labelWidth = labelWidth,
                hourHeight = hourHeight,
            )
        }
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

@Composable
fun FullDayLifeBlock(
    period: TimePeriod,
    labelWidth: Dp,
    hourHeight: Dp,
) {
    val top = timelineOffset(minutes = minutesFromStart(period.start), hourHeight = hourHeight)
    val height = timelineBlockHeight(
        minutes = periodDurationMinutes(period.start, period.end),
        hourHeight = hourHeight,
        minHeight = 64.dp,
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = labelWidth + 8.dp)
            .height(height)
            .offset(y = top),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                period.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

