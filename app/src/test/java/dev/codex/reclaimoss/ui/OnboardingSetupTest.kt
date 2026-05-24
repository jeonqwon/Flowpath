package dev.codex.reclaimoss.ui

import dev.codex.reclaimoss.domain.model.TimePeriodType
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class OnboardingSetupTest {
    @Test
    fun `sleep onboarding period keeps overnight range and life metadata`() {
        val period = onboardingPeriodForStep(
            step = OnboardingStep.Sleep,
            range = OnboardingTimeRange(
                start = LocalTime.of(22, 0),
                end = LocalTime.of(7, 0),
            ),
        )

        assertEquals(ONBOARDING_SLEEP_PERIOD_ID, period.id)
        assertEquals("Sleep", period.label)
        assertEquals(LocalTime.of(22, 0), period.start)
        assertEquals(LocalTime.of(7, 0), period.end)
        assertEquals(TimePeriodType.LIFE, period.type)
        assertEquals(0, period.sortOrder)
    }

    @Test
    fun `meal onboarding periods use stable ids and ordering`() {
        val breakfast = onboardingPeriodForStep(
            step = OnboardingStep.Breakfast,
            range = OnboardingTimeRange(LocalTime.of(8, 0), LocalTime.of(8, 30)),
        )
        val lunch = onboardingPeriodForStep(
            step = OnboardingStep.Lunch,
            range = OnboardingTimeRange(LocalTime.of(12, 0), LocalTime.of(13, 0)),
        )
        val dinner = onboardingPeriodForStep(
            step = OnboardingStep.Dinner,
            range = OnboardingTimeRange(LocalTime.of(19, 0), LocalTime.of(20, 0)),
        )

        assertEquals(ONBOARDING_BREAKFAST_PERIOD_ID, breakfast.id)
        assertEquals("Breakfast", breakfast.label)
        assertEquals(1, breakfast.sortOrder)

        assertEquals(ONBOARDING_LUNCH_PERIOD_ID, lunch.id)
        assertEquals("Lunch", lunch.label)
        assertEquals(2, lunch.sortOrder)

        assertEquals(ONBOARDING_DINNER_PERIOD_ID, dinner.id)
        assertEquals("Dinner", dinner.label)
        assertEquals(3, dinner.sortOrder)
    }

    @Test
    fun `existing onboarding period falls back to matching life period label`() {
        val existingSleep = dev.codex.reclaimoss.domain.model.TimePeriod(
            id = "user-sleep",
            label = "Sleep",
            start = LocalTime.of(23, 0),
            end = LocalTime.of(7, 30),
            type = TimePeriodType.LIFE,
            sortOrder = 7,
        )

        val found = existingOnboardingPeriod(OnboardingStep.Sleep, listOf(existingSleep))
        val updated = onboardingPeriodForStep(
            step = OnboardingStep.Sleep,
            range = OnboardingTimeRange(LocalTime.of(22, 0), LocalTime.of(7, 0)),
            existingPeriod = found,
        )

        assertNotNull(found)
        assertEquals("user-sleep", updated.id)
        assertEquals(7, updated.sortOrder)
        assertEquals("Sleep", updated.label)
    }
}
