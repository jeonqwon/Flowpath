package dev.codex.reclaimoss.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import dev.codex.reclaimoss.domain.model.TimePeriod
import dev.codex.reclaimoss.domain.model.TimePeriodType
import java.time.LocalTime

internal const val ONBOARDING_SLEEP_PERIOD_ID = "onboarding-sleep"
internal const val ONBOARDING_BREAKFAST_PERIOD_ID = "onboarding-breakfast"
internal const val ONBOARDING_LUNCH_PERIOD_ID = "onboarding-lunch"
internal const val ONBOARDING_DINNER_PERIOD_ID = "onboarding-dinner"

enum class OnboardingStep {
    Sleep,
    Breakfast,
    Lunch,
    Dinner,
    DailyFlow,
}

data class OnboardingTimeRange(
    val start: LocalTime,
    val end: LocalTime,
)

private data class OnboardingPeriodSpec(
    val id: String,
    val label: String,
    val sortOrder: Int,
)

private fun onboardingPeriodSpec(step: OnboardingStep): OnboardingPeriodSpec =
    when (step) {
        OnboardingStep.Sleep -> OnboardingPeriodSpec(ONBOARDING_SLEEP_PERIOD_ID, "Sleep", 0)
        OnboardingStep.Breakfast -> OnboardingPeriodSpec(ONBOARDING_BREAKFAST_PERIOD_ID, "Breakfast", 1)
        OnboardingStep.Lunch -> OnboardingPeriodSpec(ONBOARDING_LUNCH_PERIOD_ID, "Lunch", 2)
        OnboardingStep.Dinner -> OnboardingPeriodSpec(ONBOARDING_DINNER_PERIOD_ID, "Dinner", 3)
        OnboardingStep.DailyFlow -> error("Daily Flow is not a period step")
    }

fun existingOnboardingPeriod(
    step: OnboardingStep,
    periods: List<TimePeriod>,
): TimePeriod? {
    val spec = onboardingPeriodSpec(step)
    return periods.firstOrNull { it.id == spec.id } ?: periods.firstOrNull {
        it.type == TimePeriodType.LIFE && it.label.equals(spec.label, ignoreCase = true)
    }
}

fun onboardingPeriodForStep(
    step: OnboardingStep,
    range: OnboardingTimeRange,
    existingPeriod: TimePeriod? = null,
): TimePeriod {
    val spec = onboardingPeriodSpec(step)
    return TimePeriod(
        id = existingPeriod?.id ?: spec.id,
        label = existingPeriod?.label ?: spec.label,
        start = range.start,
        end = range.end,
        type = TimePeriodType.LIFE,
        sortOrder = existingPeriod?.sortOrder ?: spec.sortOrder,
    )
}

fun defaultOnboardingRange(step: OnboardingStep): OnboardingTimeRange =
    when (step) {
        OnboardingStep.Sleep -> OnboardingTimeRange(LocalTime.of(22, 0), LocalTime.of(7, 0))
        OnboardingStep.Breakfast -> OnboardingTimeRange(LocalTime.of(8, 0), LocalTime.of(8, 30))
        OnboardingStep.Lunch -> OnboardingTimeRange(LocalTime.of(12, 0), LocalTime.of(13, 0))
        OnboardingStep.Dinner -> OnboardingTimeRange(LocalTime.of(19, 0), LocalTime.of(20, 0))
        OnboardingStep.DailyFlow -> error("Daily Flow does not have a default range")
    }

fun existingOnboardingRange(
    step: OnboardingStep,
    periods: List<TimePeriod>,
): OnboardingTimeRange {
    val existing = existingOnboardingPeriod(step, periods)
    return existing?.let { OnboardingTimeRange(it.start, it.end) } ?: defaultOnboardingRange(step)
}

fun OnboardingStep.nextStep(): OnboardingStep =
    when (this) {
        OnboardingStep.Sleep -> OnboardingStep.Breakfast
        OnboardingStep.Breakfast -> OnboardingStep.Lunch
        OnboardingStep.Lunch -> OnboardingStep.Dinner
        OnboardingStep.Dinner -> OnboardingStep.DailyFlow
        OnboardingStep.DailyFlow -> OnboardingStep.DailyFlow
    }

@Composable
fun OnboardingSetupScreen(
    step: OnboardingStep,
    initialRange: OnboardingTimeRange,
    errorMessage: String?,
    onNext: (OnboardingTimeRange) -> Unit,
    onSkip: (() -> Unit)?,
) {
    var startMinutes by rememberSaveable(step) { mutableIntStateOf(minutesFromStart(initialRange.start)) }
    var endMinutes by rememberSaveable(step) { mutableIntStateOf(minutesFromStart(initialRange.end)) }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) snackbarHostState.showSnackbar(errorMessage)
    }
    val progress = when (step) {
        OnboardingStep.Sleep -> "1 of 5"
        OnboardingStep.Breakfast -> "2 of 5"
        OnboardingStep.Lunch -> "3 of 5"
        OnboardingStep.Dinner -> "4 of 5"
        OnboardingStep.DailyFlow -> "5 of 5"
    }
    val title = when (step) {
        OnboardingStep.Sleep -> "When do you sleep?"
        OnboardingStep.Breakfast -> "When is breakfast?"
        OnboardingStep.Lunch -> "When is lunch?"
        OnboardingStep.Dinner -> "When is dinner?"
        OnboardingStep.DailyFlow -> ""
    }
    val body = when (step) {
        OnboardingStep.Sleep -> "Choose when your sleep block starts and when you wake up."
        OnboardingStep.Breakfast -> "Add breakfast if you want it blocked in your day."
        OnboardingStep.Lunch -> "Add lunch if you want it blocked in your day."
        OnboardingStep.Dinner -> "Add dinner if you want it blocked in your day."
        OnboardingStep.DailyFlow -> ""
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
                    onClick = {
                        onNext(
                            OnboardingTimeRange(
                                start = minutesToLocalTime(startMinutes),
                                end = minutesToLocalTime(endMinutes),
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = startMinutes != endMinutes,
                    contentPadding = PaddingValues(vertical = 16.dp),
                ) {
                    Text("Next")
                }
                if (onSkip != null) {
                    OutlinedButton(
                        onClick = onSkip,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 16.dp),
                    ) {
                        Text("Skip")
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(progress, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TimeOfDaySliderCard(
                title = "Start",
                minutes = startMinutes,
                onMinutesChanged = { startMinutes = it },
            )
            TimeOfDaySliderCard(
                title = "End",
                minutes = endMinutes,
                onMinutesChanged = { endMinutes = it },
            )
            Spacer(Modifier.height(if (onSkip != null) 180.dp else 112.dp))
        }
    }
}

@Composable
private fun TimeOfDaySliderCard(
    title: String,
    minutes: Int,
    onMinutesChanged: (Int) -> Unit,
) {
    val snappedMinutes = snapToStep(minutes.coerceIn(0, (24 * 60) - 30), 30).coerceIn(0, (24 * 60) - 30)
    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("12:00 AM", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("11:30 PM", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
