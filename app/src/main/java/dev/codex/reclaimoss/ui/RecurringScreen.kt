package dev.codex.reclaimoss.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.codex.reclaimoss.domain.model.RecurrenceType
import dev.codex.reclaimoss.domain.model.ScheduleTask
import dev.codex.reclaimoss.domain.model.TaskKind
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

// -- helpers (keep existing) --

fun sleepCoverageByWeekday(tasks: List<ScheduleTask>): Set<DayOfWeek> =
    tasks.filter { it.taskKind == TaskKind.SLEEP && it.recurrenceRule.type == RecurrenceType.WEEKLY }
        .flatMap { it.recurrenceRule.daysOfWeek }
        .toSet()

fun sleepTimeForDay(tasks: List<ScheduleTask>, day: DayOfWeek): Pair<LocalTime, LocalTime>? {
    val task = tasks.firstOrNull {
        it.taskKind == TaskKind.SLEEP && day in it.recurrenceRule.daysOfWeek
    } ?: return null
    val zoneId = ZoneId.systemDefault()
    val start = task.fixedStartAt?.atZone(zoneId)?.toLocalTime() ?: return null
    val end = task.fixedEndAt?.atZone(zoneId)?.toLocalTime() ?: return null
    return start to end
}

private fun weekdayLabel(day: DayOfWeek): String =
    day.getDisplayName(TextStyle.SHORT, Locale.getDefault())

private fun weekdayLabelShort(day: DayOfWeek): String =
    day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).first().toString()

private fun recurrenceSummary(task: ScheduleTask): String {
    val rule = task.recurrenceRule
    if (rule.type == RecurrenceType.NONE) return "One-time"
    val interval = if (rule.interval > 1) "Every ${rule.interval} " else "Every "
    val unit = when (rule.type) {
        RecurrenceType.DAILY -> if (rule.interval > 1) "days" else "day"
        RecurrenceType.WEEKLY -> if (rule.interval > 1) "weeks" else "week"
        RecurrenceType.MONTHLY -> if (rule.interval > 1) "months" else "month"
        RecurrenceType.NONE -> ""
    }
    val days = if (rule.type == RecurrenceType.WEEKLY && rule.daysOfWeek.isNotEmpty()) {
        " on " + rule.daysOfWeek.sortedBy { it.value }.joinToString(", ") { weekdayLabel(it) }
    } else ""
    val until = when (rule.endMode) {
        dev.codex.reclaimoss.domain.model.RecurrenceEndMode.NEVER -> ", forever"
        dev.codex.reclaimoss.domain.model.RecurrenceEndMode.ON_DATE -> ", until ${rule.until?.atZone(ZoneId.systemDefault())?.toLocalDate()?.toString() ?: "?"}"
        dev.codex.reclaimoss.domain.model.RecurrenceEndMode.AFTER_OCCURRENCES -> ", ${rule.occurrenceCount ?: "?"} times"
    }
    return "$interval$unit$days$until"
}

// -- screen --

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    existingSleepTasks: List<ScheduleTask>,
    allTasks: List<ScheduleTask>,
    onBack: () -> Unit,
    onAddSleep: () -> Unit,
    onOpenTask: (String) -> Unit,
) {
    val zoneId = remember { ZoneId.systemDefault() }
    val covered = remember(existingSleepTasks) { sleepCoverageByWeekday(existingSleepTasks) }
    val coveredCount = covered.size
    val sleepConfigured = coveredCount == 7

    // Other recurring tasks (non-sleep, non-blocker, with recurrence), ordered by next due date
    val recurringTasks = remember(allTasks) {
        allTasks.filter {
            it.taskKind != TaskKind.SLEEP &&
                it.taskKind != TaskKind.BLOCKER &&
                it.recurrenceRule.type != RecurrenceType.NONE
        }.sortedBy { it.dueAt }
    }

    var expandedTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var sleepExpanded by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recurring", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // --- SLEEP CARD (always first) ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { sleepExpanded = !sleepExpanded },
                            ) {
                                Text("Sleep", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Icon(
                                    if (sleepExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                    null, Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = onAddSleep,
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                ) {
                                    Text("Add/change", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        // Expanded detail
                        AnimatedVisibility(
                            visible = sleepExpanded,
                            enter = expandVertically(),
                            exit = shrinkVertically(),
                        ) {
                            Column(
                                modifier = Modifier.padding(top = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("Coverage", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = if (sleepConfigured) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                    ) {
                                        Text(
                                            "$coveredCount / 7 days",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (sleepConfigured) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }

                                // Weekday row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    DayOfWeek.entries.forEach { day ->
                                        Surface(
                                            modifier = Modifier.size(34.dp),
                                            shape = RoundedCornerShape(999.dp),
                                            color = if (day in covered) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                if (day in covered) {
                                                    Icon(Icons.Outlined.Check, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimary)
                                                } else {
                                                    Text(weekdayLabelShort(day), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Per-day times
                                if (coveredCount > 0) {
                                    covered.sortedBy { it.value }.forEach { day ->
                                        val sleepTask = existingSleepTasks.firstOrNull {
                                            it.taskKind == TaskKind.SLEEP && day in it.recurrenceRule.daysOfWeek
                                        }
                                        val time = sleepTimeForDay(existingSleepTasks, day) ?: return@forEach
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .then(
                                                    if (sleepTask != null) Modifier.clickable { onOpenTask(sleepTask.id) }
                                                    else Modifier
                                                ),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Text(weekdayLabel(day), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    "${time.first.formatAsClock()} – ${time.second.formatAsClock()}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                                if (sleepTask != null) {
                                                    Icon(
                                                        Icons.Outlined.ChevronRight, null, Modifier.size(14.dp),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- RECURRING TASK CARDS ---
            if (recurringTasks.isEmpty()) {
                item {
                    Text(
                        "No recurring tasks yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            } else {
                items(recurringTasks, key = { it.id }) { task ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenTask(task.id) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(task.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1)
                            Icon(
                                Icons.Outlined.ChevronRight, null, Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
