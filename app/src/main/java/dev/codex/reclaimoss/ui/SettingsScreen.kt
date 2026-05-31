package dev.codex.reclaimoss.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
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
import dev.codex.reclaimoss.settings.DateFormatPreference
import dev.codex.reclaimoss.settings.FontSizeScale
import dev.codex.reclaimoss.settings.ReminderTimingMode
import dev.codex.reclaimoss.settings.ThemeMode
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

private enum class SettingsSection(val title: String) {
    Appearance("Appearance"),
    TaskRules("Task Rules"),
    Reminders("Reminders"),
    History("History"),
    DailyFlow("Daily Flow"),
}

private enum class SettingsHubGroup(val title: String, val sections: List<SettingsSection>) {
    Schedule(
        "Schedule",
        listOf(SettingsSection.DailyFlow, SettingsSection.TaskRules),
    ),
    Preferences(
        "Preferences",
        listOf(SettingsSection.Appearance, SettingsSection.Reminders, SettingsSection.History),
    ),
}

@Composable
fun SettingsScreen(
    padding: PaddingValues,
    periods: List<TimePeriod>,
    settings: AppSettings,
    isActive: Boolean = true,
    startInDailyFlow: Boolean = false,
    startInEditFlow: Boolean = false,
    showDailyFlowOnboardingPrompt: Boolean = false,
    onSavePeriod: (TimePeriodDraft) -> Unit,
    onDeletePeriod: (String) -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onFontSizeScaleChanged: (FontSizeScale) -> Unit,
    onDateFormatPreferenceChanged: (DateFormatPreference) -> Unit,
    onWeekStartChanged: (WeekStart) -> Unit,
    onBreakBufferChanged: (Int) -> Unit,
    onAlignmentChanged: (Int) -> Unit,
    onAllowTaskSplittingChanged: (Boolean) -> Unit,
    onAllowConcurrentTasksChanged: (Boolean) -> Unit,
    onMaxTaskChunkChanged: (Int) -> Unit,
    onDefaultTaskReminderChanged: (Boolean) -> Unit,
    onReminderTimingModeChanged: (ReminderTimingMode) -> Unit,
    onReminderLeadMinutesChanged: (Int) -> Unit,
    onHistoryRetentionChanged: (HistoryRetention) -> Unit,
    onFinishDailyFlowOnboarding: () -> Unit,
) {
    val isDarkSettings = MaterialTheme.colorScheme.background.luminance() < 0.5f
    var editingPeriod by remember { mutableStateOf<TimePeriodDraft?>(null) }
    var editingPeriodError by remember { mutableStateOf<String?>(null) }
    var editFlow by rememberSaveable { mutableStateOf(startInEditFlow) }
    var section by rememberSaveable { mutableStateOf<SettingsSection?>(if (startInDailyFlow) SettingsSection.DailyFlow else null) }

    LaunchedEffect(startInDailyFlow) {
        if (startInDailyFlow && section == null) {
            section = SettingsSection.DailyFlow
        }
    }
    LaunchedEffect(startInEditFlow, section) {
        if (startInEditFlow && section == SettingsSection.DailyFlow) {
            editFlow = true
        }
    }
    LaunchedEffect(isActive, startInDailyFlow) {
        if (!isActive) {
            section = if (startInDailyFlow) SettingsSection.DailyFlow else null
            editFlow = false
        }
    }
    BackHandler(enabled = section != null) {
        section = null
    }

    if (editingPeriod != null) {
        TimePeriodDialog(
            initial = editingPeriod!!,
            errorMessage = editingPeriodError,
            onDismiss = {
                editingPeriod = null
                editingPeriodError = null
            },
            onDraftChanged = { editingPeriodError = null },
            onSave = {
                val candidate = TimePeriod(
                    id = it.id.ifBlank { "draft-period" },
                    label = it.label.trim(),
                    start = it.start,
                    end = it.end,
                    type = it.type,
                    sortOrder = it.sortOrder,
                )
                val overlap = findOverlappingTimePeriod(candidate, periods)
                if (overlap != null) {
                    editingPeriodError = timePeriodOverlapMessage(candidate.label, overlap)
                } else {
                    onSavePeriod(it)
                    editingPeriod = null
                    editingPeriodError = null
                }
            },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (section == null) {
                Text(
                    "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = settingsPrimaryTextColor(isDarkSettings),
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 120.dp),
                ) {
                    listOf(
                        SettingsSection.DailyFlow,
                        SettingsSection.TaskRules,
                        SettingsSection.Appearance,
                        SettingsSection.Reminders,
                        SettingsSection.History,
                    ).forEach { item ->
                        item {
                            SettingsNavigationRow(
                                title = item.title,
                                icon = settingsSectionIcon(item),
                                isDarkSettings = isDarkSettings,
                                onClick = { section = item },
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        IconButton(onClick = { section = null }) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                                tint = settingsPrimaryTextColor(isDarkSettings),
                            )
                        }
                        Text(
                            section!!.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = settingsPrimaryTextColor(isDarkSettings),
                        )
                    }
                    if (section == SettingsSection.DailyFlow) {
                        HeaderActionButton(
                            label = if (editFlow) "Done" else "Edit Flow",
                            onClick = { editFlow = !editFlow },
                            width = 156.dp,
                        )
                    }
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 120.dp),
                ) {
                    item {
                        when (section) {
                        SettingsSection.Appearance -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SettingsControlRow(
                                title = "Theme",
                            ) {
                                SegmentedEnumRow(
                                    options = ThemeMode.entries,
                                    selected = settings.themeMode,
                                    labelFor = {
                                        when (it) {
                                            ThemeMode.SYSTEM -> "System"
                                            ThemeMode.LIGHT -> "Light"
                                            ThemeMode.DARK -> "Dark"
                                        }
                                    },
                                    onSelected = onThemeModeChanged,
                                )
                            }
                            SettingsControlRow(
                                title = "Font size",
                            ) {
                                SegmentedEnumRow(
                                    options = FontSizeScale.entries,
                                    selected = settings.fontSizeScale,
                                    labelFor = {
                                        when (it) {
                                            FontSizeScale.SMALL -> "90%"
                                            FontSizeScale.DEFAULT -> "100%"
                                            FontSizeScale.LARGE -> "115%"
                                            FontSizeScale.EXTRA_LARGE -> "130%"
                                        }
                                    },
                                    onSelected = onFontSizeScaleChanged,
                                )
                            }
                            SettingsControlRow(
                                title = "Date format",
                            ) {
                                SegmentedEnumRow(
                                    options = DateFormatPreference.entries,
                                    selected = settings.dateFormatPreference,
                                    labelFor = {
                                        when (it) {
                                            DateFormatPreference.MONTH_DAY_YEAR -> "MM/DD/YY"
                                            DateFormatPreference.DAY_MONTH_YEAR -> "DD/MM/YY"
                                        }
                                    },
                                    onSelected = onDateFormatPreferenceChanged,
                                )
                            }
                        }

                        SettingsSection.TaskRules -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            SettingsControlRow(
                                title = "Break buffer",
                            ) {
                                DurationSlider(
                                    minutes = settings.breakBufferMinutes,
                                    minMinutes = 0,
                                    maxMinutes = 60,
                                    onMinutesChanged = onBreakBufferChanged,
                                )
                            }
                            SettingsControlRow(
                                title = "Task alignment",
                            ) {
                                SegmentedEnumRow(
                                    options = listOf(15, 30, 60),
                                    selected = settings.alignmentMinutes,
                                    labelFor = {
                                        when (it) {
                                            15 -> "15 min"
                                            30 -> "30 min"
                                            else -> "60 min"
                                        }
                                    },
                                    onSelected = onAlignmentChanged,
                                )
                            }
                            SettingsInlineSwitchRow(
                                title = "Allow task splitting",
                                checked = settings.allowTaskSplitting,
                                onCheckedChange = onAllowTaskSplittingChanged,
                            )
                            SettingsInlineSwitchRow(
                                title = "Allow concurrent tasks",
                                checked = settings.allowConcurrentTasks,
                                onCheckedChange = onAllowConcurrentTasksChanged,
                            )
                            SettingsControlRow(
                                title = "Max task chunk",
                            ) {
                                DurationSlider(
                                    minutes = settings.maxTaskChunkMinutes,
                                    maxMinutes = 360,
                                    onMinutesChanged = onMaxTaskChunkChanged,
                                )
                            }
                        }

                        SettingsSection.Reminders -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SettingsInlineSwitchRow(
                                title = "Default reminder for tasks",
                                checked = settings.defaultTaskReminder,
                                onCheckedChange = onDefaultTaskReminderChanged,
                            )
                            SettingsControlRow(
                                title = "Reminder timing for tasks",
                            ) {
                                SegmentedEnumRow(
                                    options = ReminderTimingMode.entries,
                                    selected = settings.reminderTimingMode,
                                    labelFor = {
                                        when (it) {
                                            ReminderTimingMode.AT_DUE_DATE -> "At due date"
                                            ReminderTimingMode.AT_TASK_TIME -> "At task time"
                                        }
                                    },
                                    onSelected = onReminderTimingModeChanged,
                                )
                            }
                        }

                        SettingsSection.History -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SettingsControlRow(
                                title = "Keep completed tasks",
                            ) {
                                EnumDropdownRow(
                                    title = "Retention",
                                    selected = settings.historyRetention,
                                    options = HistoryRetention.entries,
                                    labelFor = {
                                        when (it) {
                                            HistoryRetention.SEVEN_DAYS -> "7 days"
                                            HistoryRetention.THIRTY_DAYS -> "30 days"
                                            HistoryRetention.FOREVER -> "Forever"
                                        }
                                    },
                                    onSelected = onHistoryRetentionChanged,
                                )
                            }
                        }

                        SettingsSection.DailyFlow -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (showDailyFlowOnboardingPrompt) {
                                DailyFlowOnboardingCard(
                                    hasProductivePeriods = periods.any { it.type == TimePeriodType.PRODUCTIVE },
                                    onContinue = onFinishDailyFlowOnboarding,
                                )
                            }
                            SettingsFullDayTimeline(
                                periods = periods,
                                editFlow = editFlow,
                                onEditPeriod = { period ->
                                    val bounds = editableBoundsForPeriod(period, periods)
                                    editingPeriod = TimePeriodDraft(
                                        id = period.id,
                                        label = period.label,
                                        start = period.start,
                                        end = period.end,
                                        boundStart = minutesToLocalTime(bounds.startMinutes),
                                        boundEnd = minutesToLocalTime(bounds.endMinutes),
                                        type = period.type,
                                        sortOrder = period.sortOrder,
                                    )
                                },
                                onSelectFreeGap = { gap ->
                                    editingPeriod = TimePeriodDraft(
                                        sortOrder = periods.size,
                                        start = minutesToLocalTime(gap.startMinutes),
                                        end = minutesToLocalTime(gap.endMinutes),
                                        boundStart = minutesToLocalTime(gap.startMinutes),
                                        boundEnd = minutesToLocalTime(gap.endMinutes),
                                    )
                                },
                                onDeletePeriod = onDeletePeriod,
                            )
                        }

                            null -> Unit
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyFlowOnboardingCard(
    hasProductivePeriods: Boolean,
    onContinue: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (hasProductivePeriods) "You are ready to start using Flowpath"
                else "Now add your productive times",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (hasProductivePeriods) {
                    "Your life periods are set. You can keep editing here, or continue to Tasks and start adding work."
                } else {
                    "Tap an empty gap to create a productive period. Flowpath only schedules tasks inside productive time."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 14.dp),
            ) {
                Text(if (hasProductivePeriods) "Continue to Tasks" else "Skip for now")
            }
        }
    }
}

@Composable
fun SettingsFullDayTimeline(
    periods: List<TimePeriod>,
    editFlow: Boolean,
    onEditPeriod: (TimePeriod) -> Unit,
    onSelectFreeGap: (FreeGap) -> Unit,
    onDeletePeriod: (String) -> Unit,
) {
    val hourHeight = 144.dp
    val labelWidth = 92.dp
    val timelineHeight = timelineOffset(minutes = 24 * 60, hourHeight = hourHeight)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(timelineHeight),
    ) {
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
        periods
            .sortedWith(compareBy<TimePeriod> { minutesFromStart(it.start) }.thenBy { it.sortOrder })
            .forEach { period ->
                periodSegments(period).forEach { segment ->
                    SettingsPeriodBlock(
                        period = period,
                        start = segment.start,
                        minutes = segment.minutes,
                        labelWidth = labelWidth,
                        hourHeight = hourHeight,
                        editFlow = editFlow,
                        onEdit = { onEditPeriod(period) },
                        onDelete = { onDeletePeriod(period.id) },
                    )
                }
            }
        if (editFlow) {
            freeGapsForPeriods(periods).forEach { gap ->
                SettingsFreeGapBlock(
                    gap = gap,
                    labelWidth = labelWidth,
                    hourHeight = hourHeight,
                    onClick = { onSelectFreeGap(gap) },
                )
            }
        }
    }
}

@Composable
fun SettingsPeriodBlock(
    period: TimePeriod,
    start: LocalTime,
    minutes: Int,
    labelWidth: Dp,
    hourHeight: Dp,
    editFlow: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val isLife = period.type == TimePeriodType.LIFE
    val top = timelineOffset(minutes = minutesFromStart(start), hourHeight = hourHeight)
    val height = timelineBlockHeight(minutes = minutes, hourHeight = hourHeight, minHeight = 64.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = labelWidth + 8.dp)
            .height(height)
            .offset(y = top),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLife) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isLife) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 18.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    period.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    period.type.name.titlecase(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (editFlow) {
                Column {
                    IconButton(onClick = onEdit, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit period")
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete period")
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsFreeGapBlock(
    gap: FreeGap,
    labelWidth: Dp,
    hourHeight: Dp,
    onClick: () -> Unit,
) {
    val top = timelineOffset(minutes = gap.startMinutes, hourHeight = hourHeight)
    val height = timelineBlockHeight(minutes = gap.endMinutes - gap.startMinutes, hourHeight = hourHeight, minHeight = 64.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = labelWidth + 8.dp)
            .height(height)
            .offset(y = top)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.52f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            contentAlignment = Alignment.TopStart,
        ) {
            Text(
                "+ Select Time Slot",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun SettingsOptionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
fun SettingsGroupSurface(
    isDarkSettings: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), content = content)
}

@Composable
fun SettingsNavigationRow(
    title: String,
    icon: ImageVector,
    isDarkSettings: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = settingsPrimaryTextColor(isDarkSettings),
                modifier = Modifier.size(24.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = settingsPrimaryTextColor(isDarkSettings),
                )
            }
        }
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = title,
            tint = settingsSecondaryTextColor(isDarkSettings),
        )
    }
}

@Composable
fun SettingsControlRow(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isDarkSettings = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = settingsPrimaryTextColor(isDarkSettings),
        )
        content()
    }
}

@Composable
fun SettingsInlineSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val isDarkSettings = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = settingsPrimaryTextColor(isDarkSettings),
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsDivider(
    isDarkSettings: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .height(1.dp)
            .background(settingsDividerColor(isDarkSettings)),
    )
}

@Composable
fun <T> SegmentedEnumRow(
    options: List<T>,
    selected: T,
    labelFor: (T) -> String,
    onSelected: (T) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            options.forEach { option ->
                val isSelected = selected == option
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clickable { onSelected(option) },
                    shape = RoundedCornerShape(999.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            labelFor(option),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun <T> EnumDropdownRow(
    title: String,
    selected: T,
    options: List<T>,
    labelFor: (T) -> String,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    TaskSectionTitle(title)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
            Text(
                labelFor(selected),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        options.forEach { option ->
            DropdownMenuItem(
                text = { Text(labelFor(option)) },
                onClick = {
                    onSelected(option)
                    expanded = false
                },
            )
        }
    }
}

@Composable
fun TimePeriodDialog(
    initial: TimePeriodDraft,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onDraftChanged: () -> Unit,
    onSave: (TimePeriodDraft) -> Unit,
) {
    var draft by remember(initial) { mutableStateOf(initial) }
    val minMinutes = minutesFromStart(draft.boundStart)
    val maxMinutes = endBoundaryMinutes(draft.boundStart, draft.boundEnd)
    val startMinutes = minutesFromStart(draft.start).coerceIn(minMinutes, maxMinutes - 30)
    val endMinutes = endBoundaryMinutes(draft.start, draft.end).coerceIn(startMinutes + 30, maxMinutes)
    val sliderSteps = ((maxMinutes - minMinutes) / 30 - 1).coerceAtLeast(0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id.isBlank()) "Add time period" else "Edit time period") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = draft.label,
                    onValueChange = {
                        draft = draft.copy(label = it)
                        onDraftChanged()
                    },
                    label = { Text("Label") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                TaskSectionTitle("Type")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = draft.type == TimePeriodType.PRODUCTIVE,
                        onClick = {
                            draft = draft.copy(type = TimePeriodType.PRODUCTIVE)
                            onDraftChanged()
                        },
                        label = { Text("Productive") },
                    )
                    FilterChip(
                        selected = draft.type == TimePeriodType.LIFE,
                        onClick = {
                            draft = draft.copy(type = TimePeriodType.LIFE)
                            onDraftChanged()
                        },
                        label = { Text("Meal / rest") },
                    )
                }
                TaskSectionTitle("Time")
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(minutesToLocalTime(startMinutes).formatAsClock(), fontWeight = FontWeight.SemiBold)
                            Text(minutesToLocalTime(endMinutes).formatAsClock(), fontWeight = FontWeight.SemiBold)
                        }
                        RangeSlider(
                            value = startMinutes.toFloat()..endMinutes.toFloat(),
                            onValueChange = { range ->
                                val snappedStart = snapToStep(range.start.toInt(), 30).coerceIn(minMinutes, maxMinutes - 30)
                                val snappedEnd = snapToStep(range.endInclusive.toInt(), 30).coerceIn(snappedStart + 30, maxMinutes)
                                draft = draft.copy(
                                    start = minutesToLocalTime(snappedStart),
                                    end = minutesToLocalTime(snappedEnd),
                                )
                                onDraftChanged()
                            },
                            valueRange = minMinutes.toFloat()..maxMinutes.toFloat(),
                            steps = sliderSteps,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(minutesToLocalTime(minMinutes).formatAsClock(), style = MaterialTheme.typography.bodySmall)
                            Text(minutesToLocalTime(maxMinutes).formatAsClock(), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (errorMessage != null) {
                    Text(
                        errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(draft.copy(label = draft.label.trim())) },
                enabled = draft.label.isNotBlank() && draft.end != draft.start,
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun EmptyCard(message: String) {
    val isDarkSettings = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = settingsSurfaceColor(isDarkSettings)),
        border = androidx.compose.foundation.BorderStroke(1.dp, settingsDividerColor(isDarkSettings)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Text(
            message,
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = settingsSecondaryTextColor(isDarkSettings),
        )
    }
}

@Composable
private fun SettingsSectionLabel(
    title: String,
    isDarkSettings: Boolean,
) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = settingsPrimaryTextColor(isDarkSettings),
    )
}

private fun settingsSectionIcon(section: SettingsSection): ImageVector =
    when (section) {
        SettingsSection.Appearance -> Icons.Outlined.Settings
        SettingsSection.TaskRules -> Icons.Outlined.Checklist
        SettingsSection.Reminders -> Icons.Outlined.Notifications
        SettingsSection.History -> Icons.Outlined.MoreHoriz
        SettingsSection.DailyFlow -> Icons.Outlined.CalendarMonth
    }

@Composable
private fun settingsPageBackground(isDarkSettings: Boolean): Color =
    MaterialTheme.colorScheme.background

@Composable
private fun settingsSurfaceColor(isDarkSettings: Boolean): Color =
    MaterialTheme.colorScheme.surface

@Composable
private fun settingsPrimaryTextColor(isDarkSettings: Boolean): Color =
    MaterialTheme.colorScheme.onSurface

@Composable
private fun settingsSecondaryTextColor(isDarkSettings: Boolean): Color =
    MaterialTheme.colorScheme.onSurfaceVariant

@Composable
private fun settingsDividerColor(isDarkSettings: Boolean): Color =
    MaterialTheme.colorScheme.outlineVariant

fun recurrenceSummary(rule: RecurrenceRule): String =
    when (rule.type) {
        RecurrenceType.NONE -> "One-time"
        RecurrenceType.DAILY -> buildString {
            append("Every ${rule.interval} day")
            if (rule.interval != 1) append("s")
            rule.until?.let {
                append(" until ")
                append(it.atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("MMM d")))
            }
        }
        RecurrenceType.WEEKLY -> buildString {
            append("Every ${rule.interval} week")
            if (rule.interval != 1) append("s")
            append(": ")
            append(rule.daysOfWeek.sortedBy { it.value }.joinToString(", ") { it.shortLabel() })
            rule.until?.let {
                append(" until ")
                append(it.atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("MMM d")))
            }
        }
        RecurrenceType.MONTHLY -> buildString {
            append("Every ${rule.interval} month")
            if (rule.interval != 1) append("s")
            rule.until?.let {
                append(" until ")
                append(it.atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("MMM d")))
            }
        }
    }

fun periodDropdownLabel(period: TimePeriod): String =
    "${period.label} - ${period.start.formatAsClock()} - ${period.end.formatAsClock()}"

fun RecurrenceType.displayName(): String =
    when (this) {
        RecurrenceType.NONE -> "Once"
        RecurrenceType.DAILY -> "Daily"
        RecurrenceType.WEEKLY -> "Weekly"
        RecurrenceType.MONTHLY -> "Monthly"
    }

fun DayOfWeek.shortLabel(): String = getDisplayName(TextStyle.SHORT, Locale.getDefault())

fun Set<DayOfWeek>.toggle(day: DayOfWeek): Set<DayOfWeek> =
    if (day in this) this - day else this + day

fun TimePeriod?.toPreferredTimeOfDay(): PreferredTimeOfDay {
    if (this == null) return PreferredTimeOfDay.ANYTIME
    return when {
        start < LocalTime.NOON -> PreferredTimeOfDay.MORNING
        start < LocalTime.of(14, 0) -> PreferredTimeOfDay.NOON
        start < LocalTime.of(18, 0) -> PreferredTimeOfDay.AFTERNOON
        else -> PreferredTimeOfDay.NIGHT
    }
}

fun LocalTime.formatAsClock(): String =
    format(DateTimeFormatter.ofPattern("h:mm a"))

fun LocalTime.formatHourLabel(): String =
    format(DateTimeFormatter.ofPattern("HH:mm"))

fun minutesFromStart(time: LocalTime): Int =
    time.hour * 60 + time.minute

fun periodDurationMinutes(start: LocalTime, end: LocalTime): Int {
    val startMinutes = minutesFromStart(start)
    val endMinutes = minutesFromStart(end)
    val raw = if (endMinutes > startMinutes) endMinutes - startMinutes else (24 * 60 - startMinutes) + endMinutes
    return raw.coerceAtLeast(30)
}

data class PeriodSegment(
    val start: LocalTime,
    val minutes: Int,
)

data class FreeGap(
    val startMinutes: Int,
    val endMinutes: Int,
)

fun findOverlappingTimePeriod(
    candidate: TimePeriod,
    periods: List<TimePeriod>,
): TimePeriod? {
    val candidateRanges = periodSegments(candidate).map { segment ->
        minutesFromStart(segment.start) to (minutesFromStart(segment.start) + segment.minutes).coerceAtMost(24 * 60)
    }
    return periods
        .asSequence()
        .filterNot { it.id == candidate.id }
        .firstOrNull { existing ->
            periodSegments(existing).any { segment ->
                val existingStart = minutesFromStart(segment.start)
                val existingEnd = (existingStart + segment.minutes).coerceAtMost(24 * 60)
                candidateRanges.any { (candidateStart, candidateEnd) ->
                    candidateStart < existingEnd && existingStart < candidateEnd
                }
            }
        }
}

fun timePeriodOverlapMessage(
    candidateLabel: String,
    overlappingPeriod: TimePeriod,
): String {
    val subject = candidateLabel.ifBlank { "This period" }
    return "$subject overlaps with ${overlappingPeriod.label} (${overlappingPeriod.start.formatAsClock()} - ${overlappingPeriod.end.formatAsClock()}). Choose a different time."
}

fun periodSegments(period: TimePeriod): List<PeriodSegment> {
    val startMinutes = minutesFromStart(period.start)
    val endMinutes = minutesFromStart(period.end)
    return if (endMinutes > startMinutes) {
        listOf(PeriodSegment(period.start, endMinutes - startMinutes))
    } else {
        listOf(
            PeriodSegment(period.start, 24 * 60 - startMinutes),
            PeriodSegment(LocalTime.MIDNIGHT, endMinutes),
        ).filter { it.minutes > 0 }
    }
}

fun freeGapsForPeriods(periods: List<TimePeriod>): List<FreeGap> {
    val occupied = periods
        .flatMap { period ->
            periodSegments(period).map { segment ->
                minutesFromStart(segment.start) to (minutesFromStart(segment.start) + segment.minutes).coerceAtMost(24 * 60)
            }
        }
        .filter { (start, end) -> end > start }
        .sortedBy { it.first }

    val merged = mutableListOf<Pair<Int, Int>>()
    occupied.forEach { range ->
        val last = merged.lastOrNull()
        if (last == null || range.first > last.second) {
            merged.add(range)
        } else {
            merged[merged.lastIndex] = last.first to maxOf(last.second, range.second)
        }
    }

    val gaps = mutableListOf<FreeGap>()
    var cursor = 0
    merged.forEach { (start, end) ->
        if (start - cursor >= 30) gaps.add(FreeGap(cursor, start))
        cursor = maxOf(cursor, end)
    }
    if (24 * 60 - cursor >= 30) gaps.add(FreeGap(cursor, 24 * 60))
    return gaps
}

fun editableBoundsForPeriod(period: TimePeriod, periods: List<TimePeriod>): FreeGap {
    val occupied = periods
        .filterNot { it.id == period.id }
        .flatMap { other ->
            periodSegments(other).map { segment ->
                minutesFromStart(segment.start) to (minutesFromStart(segment.start) + segment.minutes).coerceAtMost(24 * 60)
            }
        }
        .sortedBy { it.first }
    val start = minutesFromStart(period.start)
    val end = minutesFromStart(period.end).let { if (it <= start) 24 * 60 else it }
    val lower = occupied.filter { it.second <= start }.maxOfOrNull { it.second } ?: 0
    val upper = occupied.filter { it.first >= end }.minOfOrNull { it.first } ?: 24 * 60
    return FreeGap(lower, upper)
}

fun minutesToLocalTime(minutes: Int): LocalTime {
    val normalized = minutes.coerceIn(0, 24 * 60)
    if (normalized == 24 * 60) return LocalTime.MIDNIGHT
    return LocalTime.of(normalized / 60, normalized % 60)
}

fun endBoundaryMinutes(start: LocalTime, end: LocalTime): Int {
    val startMinutes = minutesFromStart(start)
    val endMinutes = minutesFromStart(end)
    return if (endMinutes <= startMinutes) 24 * 60 else endMinutes
}

fun snapToStep(value: Int, step: Int): Int =
    ((value + step / 2) / step) * step

fun timelineOffset(minutes: Int, hourHeight: Dp): Dp =
    (hourHeight.value * minutes.toFloat() / 60f).dp

fun timelineBlockHeight(minutes: Int, hourHeight: Dp, minHeight: Dp): Dp {
    val proportional = timelineOffset(minutes.coerceAtLeast(30), hourHeight)
    return if (proportional < minHeight) minHeight else proportional
}

fun headerDateLabel(date: LocalDate, formatPreference: DateFormatPreference): String {
    val order = when (formatPreference) {
        DateFormatPreference.MONTH_DAY_YEAR -> "${date.monthValue}/${date.dayOfMonth}"
        DateFormatPreference.DAY_MONTH_YEAR -> "${date.dayOfMonth}/${date.monthValue}"
    }
    return "${date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())} $order"
}

fun reminderDateTimeFormatter(formatPreference: DateFormatPreference): DateTimeFormatter =
    when (formatPreference) {
        DateFormatPreference.MONTH_DAY_YEAR -> DateTimeFormatter.ofPattern("MMM d, h:mm a")
        DateFormatPreference.DAY_MONTH_YEAR -> DateTimeFormatter.ofPattern("d MMM, h:mm a")
    }

fun Int.durationLabel(): String =
    if (this < 60) "${this}m" else "${this / 60}h${if (this % 60 == 0) "" else " ${this % 60}m"}"

fun taskDotColor(count: Int): Color =
    when {
        count <= 1 -> Color(0xFF9FD0B6)
        count == 2 -> Color(0xFF74AE92)
        count == 3 -> Color(0xFF4E8C70)
        else -> Color(0xFF2E6A52)
    }

fun String.titlecase(): String =
    lowercase().replaceFirstChar { it.titlecase() }
