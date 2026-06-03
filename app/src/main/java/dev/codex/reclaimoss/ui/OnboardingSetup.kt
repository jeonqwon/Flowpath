package dev.codex.reclaimoss.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

data class SleepOnboardingEntryDraft(
    val weekdays: Set<DayOfWeek> = emptySet(),
    val windowStart: LocalTime = LocalTime.of(22, 0),
    val windowEnd: LocalTime = LocalTime.of(8, 0),
    val durationMinutes: Int = 8 * 60,
)

private fun encodeSleepOnboardingEntry(entry: SleepOnboardingEntryDraft): String = listOf(
    entry.weekdays.sortedBy { it.value }.joinToString(",") { it.value.toString() },
    entry.windowStart.toSecondOfDay().toString(),
    entry.windowEnd.toSecondOfDay().toString(),
    entry.durationMinutes.toString(),
).joinToString("|")

private fun decodeSleepOnboardingEntry(encoded: String): SleepOnboardingEntryDraft {
    val parts = encoded.split("|")
    val weekdays = parts.getOrNull(0)
        .orEmpty()
        .split(",")
        .filter { it.isNotBlank() }
        .map { DayOfWeek.of(it.toInt()) }
        .toSet()
    val windowStart = LocalTime.ofSecondOfDay(parts.getOrNull(1)?.toLongOrNull() ?: LocalTime.of(22, 0).toSecondOfDay().toLong())
    val windowEnd = LocalTime.ofSecondOfDay(parts.getOrNull(2)?.toLongOrNull() ?: LocalTime.of(8, 0).toSecondOfDay().toLong())
    val durationMinutes = parts.getOrNull(3)?.toIntOrNull() ?: 8 * 60
    return SleepOnboardingEntryDraft(
        weekdays = weekdays,
        windowStart = windowStart,
        windowEnd = windowEnd,
        durationMinutes = durationMinutes,
    )
}

private val SleepOnboardingEntryDraftSaver = listSaver<SleepOnboardingEntryDraft, String>(
    save = { listOf(encodeSleepOnboardingEntry(it)) },
    restore = { saved -> decodeSleepOnboardingEntry(saved.single()) },
)

private val SleepOnboardingEntryDraftListSaver = listSaver<List<SleepOnboardingEntryDraft>, String>(
    save = { entries -> entries.map(::encodeSleepOnboardingEntry) },
    restore = { saved -> saved.map(::decodeSleepOnboardingEntry) },
)

fun coveredSleepWeekdays(entries: List<SleepOnboardingEntryDraft>): Set<DayOfWeek> =
    entries.flatMapTo(linkedSetOf()) { it.weekdays }

fun missingSleepWeekdays(entries: List<SleepOnboardingEntryDraft>): Set<DayOfWeek> =
    DayOfWeek.entries.filterNotTo(linkedSetOf()) { it in coveredSleepWeekdays(entries) }

fun unavailableSleepWeekdays(
    entries: List<SleepOnboardingEntryDraft>,
    selectedDays: Set<DayOfWeek>,
    editingIndex: Int,
): Set<DayOfWeek> =
    entries
        .filterIndexed { index, _ -> index != editingIndex }
        .flatMapTo(linkedSetOf()) { it.weekdays }
        .minus(selectedDays)

private fun availableWindowMinutes(entry: SleepOnboardingEntryDraft): Int {
    val startMinutes = minutesFromStart(entry.windowStart)
    val endMinutes = minutesFromStart(entry.windowEnd)
    return if (endMinutes > startMinutes) {
        endMinutes - startMinutes
    } else {
        (24 * 60) - startMinutes + endMinutes
    }
}

private fun weekdaySummary(weekdays: Set<DayOfWeek>): String =
    weekdays.sortedBy { it.value }.joinToString(", ") {
        it.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    }

@Composable
fun SleepOnboardingScreen(
    initialEntries: List<SleepOnboardingEntryDraft> = emptyList(),
    errorMessage: String?,
    onComplete: (List<SleepOnboardingEntryDraft>) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) snackbarHostState.showSnackbar(errorMessage)
    }
    var entries by rememberSaveable(stateSaver = SleepOnboardingEntryDraftListSaver) { mutableStateOf(initialEntries) }
    var editingIndex by rememberSaveable { mutableIntStateOf(-1) }
    var draft by rememberSaveable(stateSaver = SleepOnboardingEntryDraftSaver) {
        mutableStateOf(initialEntries.firstOrNull() ?: SleepOnboardingEntryDraft())
    }
    val covered = remember(entries) { coveredSleepWeekdays(entries) }
    val missing = remember(entries) { missingSleepWeekdays(entries) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(localError) {
        if (!localError.isNullOrBlank()) snackbarHostState.showSnackbar(localError!!)
    }

    fun resetDraft() {
        editingIndex = -1
        draft = SleepOnboardingEntryDraft()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { onComplete(entries) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = missing.isEmpty(),
                    contentPadding = PaddingValues(vertical = 16.dp),
                ) {
                    Text("Finish")
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(bottom = 160.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Sleep schedule",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (missing.isEmpty()) {
                            "All 7 days are covered."
                        } else {
                            "Cover every day of the week before finishing."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (missing.isEmpty()) {
                            "You can edit entries below or finish."
                        } else {
                            "Missing: ${weekdaySummary(missing)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (entries.isNotEmpty()) {
                itemsIndexed(entries) { index, entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = HeaderActionShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    weekdaySummary(entry.weekdays),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "${entry.windowStart.formatAsClock()} - ${entry.windowEnd.formatAsClock()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    "${entry.durationMinutes / 60}h ${(entry.durationMinutes % 60).toString().padStart(2, '0')}m",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        editingIndex = index
                                        draft = entry
                                        localError = null
                                    },
                                ) {
                                    Icon(Icons.Outlined.Edit, contentDescription = "Edit sleep entry")
                                }
                                IconButton(
                                    onClick = {
                                        entries = entries.filterIndexed { itemIndex, _ -> itemIndex != index }
                                        if (editingIndex == index) resetDraft()
                                    },
                                ) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete sleep entry")
                                }
                            }
                        }
                    }
                }
            }
            item {
                CreateFormCard {
                    TaskSectionTitle(if (editingIndex >= 0) "Edit sleep entry" else "Add sleep entry")
                    WeekdayPicker(
                        selectedDays = draft.weekdays,
                        unavailableDays = remember(entries, draft.weekdays, editingIndex) {
                            unavailableSleepWeekdays(
                                entries = entries,
                                selectedDays = draft.weekdays,
                                editingIndex = editingIndex,
                            )
                        },
                        onSelectionChanged = {
                            draft = draft.copy(weekdays = it)
                            localError = null
                        },
                    )
                    DailyWindowConfigurator(
                        hasWindow = true,
                        canDisable = false,
                        startTime = draft.windowStart,
                        endTime = draft.windowEnd,
                        endsNextDay = draft.windowEnd <= draft.windowStart,
                        minimumWindowMinutes = draft.durationMinutes,
                        onWindowEnabledChanged = {},
                        onWindowChanged = { startTime, endTime, _ ->
                            draft = draft.copy(
                                windowStart = startTime,
                                windowEnd = endTime,
                            )
                            localError = null
                        },
                        context = context,
                    )
                    DurationSlider(
                        minutes = draft.durationMinutes,
                        minMinutes = 240,
                        maxMinutes = 720,
                        onMinutesChanged = {
                            draft = draft.copy(durationMinutes = it)
                            localError = null
                        },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                val overlappingDays = entries
                                    .filterIndexed { index, _ -> index != editingIndex }
                                    .flatMap { it.weekdays }
                                    .toSet()
                                    .intersect(draft.weekdays)
                                when {
                                    draft.weekdays.isEmpty() -> localError = "Choose at least one day."
                                    draft.durationMinutes > availableWindowMinutes(draft) -> {
                                        localError = "Duration must fit inside the selected sleep window."
                                    }
                                    overlappingDays.isNotEmpty() -> {
                                        localError = "Each day can only use one sleep entry."
                                    }
                                    else -> {
                                        entries = if (editingIndex >= 0) {
                                            entries.toMutableList().also { it[editingIndex] = draft }
                                        } else {
                                            entries + draft
                                        }
                                        resetDraft()
                                        localError = null
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 14.dp),
                        ) {
                            Text(if (editingIndex >= 0) "Update" else "Add")
                        }
                        if (editingIndex >= 0) {
                            OutlinedButton(
                                onClick = { resetDraft() },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 14.dp),
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekdayPicker(
    selectedDays: Set<DayOfWeek>,
    unavailableDays: Set<DayOfWeek>,
    onSelectionChanged: (Set<DayOfWeek>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TaskSectionTitle("Weekdays")
        Text(
            "Pick uncovered days for this sleep window.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            DayOfWeek.entries.forEach { day ->
                val isSelected = day in selectedDays
                val isUnavailable = day in unavailableDays
                Surface(
                    modifier = Modifier
                        .size(42.dp)
                        .then(
                            if (isUnavailable) {
                                Modifier
                            } else {
                                Modifier.clickable {
                                    onSelectionChanged(
                                        if (isSelected) selectedDays - day else selectedDays + day,
                                    )
                                }
                            },
                        ),
                    shape = RoundedCornerShape(999.dp),
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isUnavailable -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    border = if (isUnavailable) {
                        androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                        )
                    } else {
                        null
                    },
                ) {
                    Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text(
                            day.shortLabelForOnboarding(),
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                isUnavailable -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

private fun DayOfWeek.shortLabelForOnboarding(): String =
    getDisplayName(TextStyle.SHORT, Locale.getDefault()).first().toString()

@Composable
private fun TimeOfDaySliderCard(
    title: String,
    minutes: Int,
    onMinutesChanged: (Int) -> Unit,
) {
    val snappedMinutes = snapToStep(minutes.coerceIn(0, (24 * 60) - 30), 30).coerceIn(0, (24 * 60) - 30)
    Card(
        shape = HeaderActionShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(minutesToLocalTime(snappedMinutes).formatAsClock(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = snappedMinutes.toFloat(),
                onValueChange = { raw ->
                    onMinutesChanged(snapToStep(raw.toInt(), 30).coerceIn(0, (24 * 60) - 30))
                },
                valueRange = 0f..((24 * 60) - 30).toFloat(),
                steps = ((24 * 60 - 30) / 30 - 1).coerceAtLeast(0),
            )
        }
    }
}
