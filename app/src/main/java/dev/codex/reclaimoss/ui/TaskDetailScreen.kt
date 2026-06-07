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
fun TaskDetailScreen(
    padding: PaddingValues,
    task: ScheduleTask,
    blocks: List<ScheduleBlock>,
    linkedReminder: Reminder?,
    onBack: () -> Unit,
    onAddReminder: () -> Unit,
    onFollowUp: () -> Unit,
    onEdit: () -> Unit,
    onReschedule: () -> Unit,
    onDone: (ScheduleBlock) -> Unit,
    onDoneAllRecurring: () -> Unit,
    onDelete: () -> Unit,
) {
    val zoneId = remember { ZoneId.systemDefault() }
    val formatter = remember { DateTimeFormatter.ofPattern("MMM d, h:mm a") }
    val dateOnlyFormatter = remember { DateTimeFormatter.ofPattern("MMM d") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val firstBlock = blocks.firstOrNull()
    var showActionsMenu by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var showDoneAllConfirm by rememberSaveable { mutableStateOf(false) }
    val isRecurringTask = task.recurrenceSeriesId != null || task.recurrenceRule.type != RecurrenceType.NONE

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (task.priority == TaskPriority.URGENT) {
                        Text(
                            "Urgent",
                            modifier = Modifier.align(Alignment.CenterVertically),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit task")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete task")
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text(task.title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text(
                        firstBlock?.let {
                            "${timeFormatter.format(it.startAt.atZone(zoneId))} - ${timeFormatter.format(it.endAt.atZone(zoneId))}"
                        } ?: "Not scheduled yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (task.description.isNotBlank()) {
                        Text(task.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    DetailRow("Due", task.dueDisplayText(formatter, zoneId))
                    DetailRow("Duration", task.estimatedMinutes.durationLabel())
                    if (task.recurrenceRule.type != RecurrenceType.NONE) {
                        RepeatDetailRow(task.recurrenceRule)
                    }
                    if (linkedReminder != null) {
                        DetailRow("Reminder", linkedReminder.dueDisplayText(formatter, dateOnlyFormatter, zoneId))
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            firstBlock?.let { onDone(it) }
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(if (isRecurringTask) "Done this task" else "Done")
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { showActionsMenu = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Icon(Icons.Outlined.MoreHoriz, contentDescription = "More options")
                            Spacer(Modifier.width(8.dp))
                            Text("More")
                        }
                        DropdownMenu(
                            expanded = showActionsMenu,
                            onDismissRequest = { showActionsMenu = false },
                            offset = DpOffset(x = 0.dp, y = 8.dp),
                            shape = RoundedCornerShape(24.dp),
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp,
                            shadowElevation = 12.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.widthIn(min = 220.dp, max = 240.dp),
                        ) {
                            if (isRecurringTask) {
                                ActionMenuItem(
                                label = "Done all recurring",
                                icon = Icons.Outlined.Checklist,
                                onClick = {
                                    showActionsMenu = false
                                    showDoneAllConfirm = true
                                },
                            )
                            }
                            ActionMenuItem(
                                label = if (linkedReminder == null) "Add Reminder" else "Dismiss Reminder",
                                icon = Icons.Outlined.Notifications,
                                onClick = {
                                    showActionsMenu = false
                                    onAddReminder()
                                },
                            )
                            ActionMenuItem(
                                label = "Follow up",
                                icon = Icons.Outlined.Add,
                                onClick = {
                                    showActionsMenu = false
                                    onFollowUp()
                                },
                            )
                            ActionMenuItem(
                                label = "Reschedule",
                                icon = Icons.Outlined.CalendarMonth,
                                onClick = {
                                    showActionsMenu = false
                                    onReschedule()
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Delete task?") },
            text = { Text("Are you sure?") },
        )
    }

    if (showDoneAllConfirm) {
        AlertDialog(
            onDismissRequest = { showDoneAllConfirm = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDoneAllConfirm = false
                        onDoneAllRecurring()
                    },
                ) {
                    Text("Done all")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDoneAllConfirm = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Done all recurring?") },
            text = { Text("Are you sure?") },
        )
    }
}

@Composable
fun ReminderDetailScreen(
    padding: PaddingValues,
    reminder: Reminder,
    linkedTask: ScheduleTask?,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
) {
    val zoneId = remember { ZoneId.systemDefault() }
    val formatter = remember { DateTimeFormatter.ofPattern("MMM d, h:mm a") }
    val dateOnlyFormatter = remember { DateTimeFormatter.ofPattern("MMM d") }
    val description = linkedTask?.description?.takeIf { it.isNotBlank() } ?: reminder.description

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.width(48.dp))
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text(reminder.title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text(
                        reminder.dueDisplayText(formatter, dateOnlyFormatter, zoneId),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (!description.isNullOrBlank()) {
                        Text(description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    DetailRow("Due", reminder.dueDisplayText(formatter, dateOnlyFormatter, zoneId))
                    if (linkedTask != null) {
                        DetailRow("Task", linkedTask.title)
                    }
                    if (reminder.recurrenceRule.type != RecurrenceType.NONE) {
                        DetailRow("Repeat", recurrenceSummary(reminder.recurrenceRule))
                    }
                }
            }
        }
        item {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(999.dp),
            ) {
                Text("Dismiss Reminder")
            }
        }
    }
}

@Composable
fun ActionMenuItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
        },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        onClick = onClick,
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}

@Composable
fun RepeatDetailRow(rule: RecurrenceRule) {
    val primary = when (rule.type) {
        RecurrenceType.DAILY -> "Every ${rule.interval} day" + if (rule.interval == 1) "" else "s"
        RecurrenceType.WEEKLY -> "Every ${rule.interval} week" + if (rule.interval == 1) "" else "s"
        RecurrenceType.MONTHLY -> "Every ${rule.interval} month" + if (rule.interval == 1) "" else "s"
        RecurrenceType.NONE -> "Once"
    }
    val secondary = if (rule.type == RecurrenceType.WEEKLY && rule.daysOfWeek.isNotEmpty()) {
        rule.daysOfWeek
            .sortedBy { it.value }
            .joinToString(" · ") { day ->
                day.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            }
    } else {
        null
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "Repeat",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            primary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (secondary != null) {
            Text(
                secondary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}


