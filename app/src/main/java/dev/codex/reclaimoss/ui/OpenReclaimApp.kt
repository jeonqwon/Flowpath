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
fun OpenReclaimApp(appGraph: AppGraph) {
    val viewModel: PlannerViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(PlannerViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return PlannerViewModel(appGraph.plannerCoordinator, appGraph.appSettingsRepository) as T
                }
                throw IllegalArgumentException("Unsupported ViewModel class: ${modelClass.name}")
            }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Tasks) }
    var onboardingStep by rememberSaveable { mutableStateOf<OnboardingStep?>(null) }
    var onboardingDismissedThisSession by rememberSaveable { mutableStateOf(false) }
    var showingCreate by rememberSaveable { mutableStateOf(false) }
    var selectedTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedReminderId by rememberSaveable { mutableStateOf<String?>(null) }
    var followUpSourceTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var createTaskDraftOverride by remember { mutableStateOf<TaskDraft?>(null) }

    LaunchedEffect(Unit) {
        viewModel.seedIfNeeded()
    }

    if (!state.settingsLoaded) {
        InitialLoadingScreen()
        return
    }

    val shouldShowOnboarding = state.settingsLoaded &&
        !state.settings.hasCompletedOnboarding &&
        !onboardingDismissedThisSession

    LaunchedEffect(shouldShowOnboarding, onboardingStep) {
        if (shouldShowOnboarding) {
            selectedTab = AppTab.Settings
            if (onboardingStep == null) onboardingStep = OnboardingStep.Sleep
        }
    }

    if (shouldShowOnboarding && onboardingStep != null && onboardingStep != OnboardingStep.DailyFlow) {
        val step = onboardingStep!!
        val initialRange = existingOnboardingRange(step, state.snapshot.timePeriods)
        OnboardingSetupScreen(
            step = step,
            initialRange = initialRange,
            onNext = { range ->
                scope.launch {
                    val period = onboardingPeriodForStep(step, range)
                    val overlap = findOverlappingTimePeriod(period, state.snapshot.timePeriods)
                    if (overlap != null) {
                        snackbarHostState.showSnackbar(timePeriodOverlapMessage(period.label, overlap))
                    } else {
                        viewModel.saveTimePeriod(
                            TimePeriodDraft(
                                id = period.id,
                                label = period.label,
                                start = period.start,
                                end = period.end,
                                boundStart = LocalTime.MIDNIGHT,
                                boundEnd = LocalTime.MIDNIGHT,
                                type = period.type,
                                sortOrder = period.sortOrder,
                            ),
                        )
                        onboardingStep = step.nextStep()
                    }
                }
            },
            onSkip = if (step == OnboardingStep.Sleep) {
                null
            } else {
                {
                    scope.launch {
                        val periodId = onboardingPeriodForStep(step, initialRange).id
                        viewModel.deleteTimePeriod(periodId)
                        onboardingStep = step.nextStep()
                    }
                }
            },
        )
        return
    }

    if (showingCreate) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = CreateScreenSnackbarBottomOffset,
                    ),
                )
            },
        ) { padding ->
            CreateWorkScreen(
                padding = padding,
                periods = state.snapshot.timePeriods,
                initialMode = CreateMode.Task,
                initialTaskDraft = createTaskDraftOverride ?: TaskDraft(
                    preferredTimePeriodId = state.snapshot.timePeriods.firstOrNull { it.type == TimePeriodType.PRODUCTIVE }?.id,
                    addReminder = state.settings.defaultTaskReminder,
                ),
                followUpMode = followUpSourceTaskId != null,
                onBack = {
                    showingCreate = false
                    followUpSourceTaskId = null
                    createTaskDraftOverride = null
                },
                onSaveTask = { draft ->
                    scope.launch {
                        val sourceTaskId = followUpSourceTaskId
                        val result = if (sourceTaskId != null) {
                            viewModel.addFollowUpTask(sourceTaskId, draft)
                        } else {
                            viewModel.addTask(draft, state.snapshot.timePeriods)
                        }
                        if (result == null) {
                            snackbarHostState.showSnackbar("Unable to schedule task")
                            return@launch
                        }
                        if (!result.scheduled) {
                            snackbarHostState.showSnackbar(
                                if (result.partial) {
                                    "Unable to fully schedule task. Try another time, period, or shorter duration."
                                } else {
                                    "Unable to schedule task. Try another time, period, or shorter duration."
                                },
                            )
                            return@launch
                        }
                        showingCreate = false
                        followUpSourceTaskId = null
                        createTaskDraftOverride = null
                        selectedTab = AppTab.Tasks
                        snackbarHostState.showSnackbar(
                            when {
                                result.partial -> "Task partially scheduled"
                                sourceTaskId != null -> "Follow-up task created"
                                else -> "Task scheduled"
                            },
                        )
                    }
                },
                onSaveReminder = { draft ->
                    scope.launch {
                        viewModel.addReminder(draft)
                        showingCreate = false
                        followUpSourceTaskId = null
                        createTaskDraftOverride = null
                        selectedTab = AppTab.Reminders
                        snackbarHostState.showSnackbar("Reminder saved")
                    }
                },
            )
        }
        return
    }

    selectedReminderId?.let { reminderId ->
        val reminder = state.snapshot.reminders.firstOrNull { it.id == reminderId }
        if (reminder != null) {
            val linkedTask = reminder.linkedTaskId?.let { taskId -> state.snapshot.tasks.firstOrNull { it.id == taskId } }
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                containerColor = MaterialTheme.colorScheme.background,
            ) { padding ->
                ReminderDetailScreen(
                    padding = padding,
                    reminder = reminder,
                    linkedTask = linkedTask,
                    onBack = { selectedReminderId = null },
                    onDismiss = {
                        scope.launch {
                            viewModel.dismissReminder(reminder.id)
                            selectedReminderId = null
                            snackbarHostState.showSnackbar("Reminder dismissed")
                        }
                    },
                )
            }
            return
        } else {
            selectedReminderId = null
        }
    }

    selectedTaskId?.let { taskId ->
        val task = state.snapshot.tasks.firstOrNull { it.id == taskId }
        if (task != null) {
            val blocks = state.snapshot.blocks.filter { it.taskId == taskId }.sortedBy { it.startAt }
            val reminder = state.snapshot.reminders.firstOrNull { it.linkedTaskId == taskId && it.status != ReminderStatus.COMPLETED }
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                containerColor = MaterialTheme.colorScheme.background,
            ) { padding ->
                TaskDetailScreen(
                    padding = padding,
                    task = task,
                    blocks = blocks,
                    linkedReminder = reminder,
                    onBack = { selectedTaskId = null },
                    onAddReminder = {
                        scope.launch {
                            if (reminder == null) {
                                viewModel.addReminderForTask(task.id)
                                snackbarHostState.showSnackbar("Reminder added")
                            } else {
                                viewModel.dismissReminder(reminder.id)
                                snackbarHostState.showSnackbar("Reminder dismissed")
                            }
                        }
                    },
                    onFollowUp = {
                        followUpSourceTaskId = task.id
                        createTaskDraftOverride = task.toFollowUpDraft()
                        selectedTaskId = null
                        showingCreate = true
                    },
                    onRescheduleUrgently = { dueAt ->
                        scope.launch {
                            val success = viewModel.rescheduleUrgently(task.id, dueAt)
                            if (success) {
                                selectedTaskId = null
                                snackbarHostState.showSnackbar("Task rescheduled")
                            } else {
                                snackbarHostState.showSnackbar("Couldn't reschedule urgently.")
                            }
                        }
                    },
                    onRescheduleLater = { dueAt ->
                        scope.launch {
                            val success = viewModel.rescheduleNextAvailable(task.id, dueAt)
                            if (success) {
                                selectedTaskId = null
                                snackbarHostState.showSnackbar("Task rescheduled")
                            } else {
                                snackbarHostState.showSnackbar("No other free slot before the due date. Try Urgent or change the deadline.")
                            }
                        }
                    },
                    onDone = {
                        scope.launch {
                            viewModel.completeTask(task.id)
                            selectedTaskId = null
                            snackbarHostState.showSnackbar("Task done")
                        }
                    },
                    onDoneAllRecurring = {
                        scope.launch {
                            viewModel.completeRecurringSeries(task.id)
                            selectedTaskId = null
                            snackbarHostState.showSnackbar("Recurring task series done")
                        }
                    },
                    onDelete = {
                        scope.launch {
                            viewModel.deleteTask(task.id)
                            selectedTaskId = null
                            snackbarHostState.showSnackbar("Task deleted")
                        }
                    },
                )
            }
            return
        } else {
            selectedTaskId = null
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
            ) {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            val icon = when (tab) {
                                AppTab.Tasks -> Icons.Outlined.Checklist
                                AppTab.Planner -> Icons.Outlined.CalendarMonth
                                AppTab.Reminders -> Icons.Outlined.Notifications
                                AppTab.Settings -> Icons.Outlined.Settings
                            }
                            Icon(icon, contentDescription = tab.label)
                        },
                        label = {
                            Text(
                                tab.label,
                                color = if (selectedTab == tab) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when (selectedTab) {
            AppTab.Tasks -> TasksScreen(
                padding = padding,
                state = state,
                settings = state.settings,
                onAddTask = {
                    followUpSourceTaskId = null
                    createTaskDraftOverride = null
                    showingCreate = true
                },
                onDeleteTask = { taskId ->
                    scope.launch {
                        viewModel.deleteTask(taskId)
                        snackbarHostState.showSnackbar("Task deleted")
                    }
                },
                onOpenTask = { selectedTaskId = it },
            )

            AppTab.Planner -> PlannerScreen(
                padding = padding,
                state = state,
                settings = state.settings,
                onRebuild = {
                    scope.launch {
                        viewModel.rebuildSchedule()
                        snackbarHostState.showSnackbar("Schedule rebuilt")
                    }
                },
                onToggleLock = { block ->
                    scope.launch { viewModel.toggleLock(block) }
                },
                onMarkDone = { block ->
                    scope.launch {
                        viewModel.completeBlock(block, state.snapshot.tasks)
                        snackbarHostState.showSnackbar("Task updated")
                    }
                },
                onReschedule = { taskId ->
                    scope.launch { viewModel.rescheduleMissed(taskId) }
                },
                onAddTask = {
                    followUpSourceTaskId = null
                    createTaskDraftOverride = null
                    showingCreate = true
                },
                onOpenTask = { selectedTaskId = it },
            )

            AppTab.Reminders -> RemindersScreen(
                padding = padding,
                reminders = state.snapshot.reminders,
                tasksById = state.snapshot.tasks.associateBy { it.id },
                onAddTask = {
                    followUpSourceTaskId = null
                    createTaskDraftOverride = null
                    showingCreate = true
                },
                onOpenReminder = { reminder ->
                    val linkedTaskId = reminder.linkedTaskId
                    if (linkedTaskId != null && state.snapshot.tasks.any { it.id == linkedTaskId }) {
                        selectedTaskId = linkedTaskId
                    } else {
                        selectedReminderId = reminder.id
                    }
                },
            )

            AppTab.Settings -> SettingsScreen(
                padding = padding,
                periods = state.snapshot.timePeriods,
                settings = state.settings,
                startInDailyFlow = shouldShowOnboarding || onboardingStep == OnboardingStep.DailyFlow,
                startInEditFlow = shouldShowOnboarding || onboardingStep == OnboardingStep.DailyFlow,
                showDailyFlowOnboardingPrompt = shouldShowOnboarding,
                onSavePeriod = { draft ->
                    scope.launch {
                        val isFirstProductiveOnboardingPeriod =
                            shouldShowOnboarding &&
                                draft.type == TimePeriodType.PRODUCTIVE &&
                                state.snapshot.timePeriods.none { it.type == TimePeriodType.PRODUCTIVE }
                        viewModel.saveTimePeriod(draft)
                        if (isFirstProductiveOnboardingPeriod) {
                            onboardingDismissedThisSession = true
                            onboardingStep = null
                            selectedTab = AppTab.Tasks
                            viewModel.setHasCompletedOnboarding(true)
                            snackbarHostState.showSnackbar("Productive time added. You can start adding tasks.")
                        } else {
                            snackbarHostState.showSnackbar("Time period saved")
                        }
                    }
                },
                onDeletePeriod = { periodId ->
                    scope.launch {
                        viewModel.deleteTimePeriod(periodId)
                        snackbarHostState.showSnackbar("Time period deleted")
                    }
                },
                onThemeModeChanged = { value -> scope.launch { viewModel.setThemeMode(value) } },
                onWeekStartChanged = { value -> scope.launch { viewModel.setWeekStart(value) } },
                onBreakBufferChanged = { value -> scope.launch { viewModel.setBreakBufferMinutes(value) } },
                onAlignmentChanged = { value -> scope.launch { viewModel.setAlignmentMinutes(value) } },
                onAllowTaskSplittingChanged = { value -> scope.launch { viewModel.setAllowTaskSplitting(value) } },
                onMaxTaskChunkChanged = { value -> scope.launch { viewModel.setMaxTaskChunkMinutes(value) } },
                onPreferredFallbackChanged = { value -> scope.launch { viewModel.setPreferredPeriodFallbackMode(value) } },
                onUrgentRescheduleChanged = { value -> scope.launch { viewModel.setUrgentRescheduleMode(value) } },
                onDefaultTaskReminderChanged = { value -> scope.launch { viewModel.setDefaultTaskReminder(value) } },
                onReminderTimingModeChanged = { value -> scope.launch { viewModel.setReminderTimingMode(value) } },
                onReminderLeadMinutesChanged = { value -> scope.launch { viewModel.setReminderLeadMinutes(value) } },
                onHistoryRetentionChanged = { value -> scope.launch { viewModel.setHistoryRetention(value) } },
                onFinishDailyFlowOnboarding = {
                    onboardingDismissedThisSession = true
                    onboardingStep = null
                    selectedTab = AppTab.Tasks
                    scope.launch { viewModel.setHasCompletedOnboarding(true) }
                },
            )
        }
    }
}

@Composable
private fun InitialLoadingScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Flowpath",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Loading your schedule...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

