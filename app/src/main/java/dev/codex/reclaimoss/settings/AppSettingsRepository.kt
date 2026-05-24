package dev.codex.reclaimoss.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "flowpath_settings")

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class DateFormatPreference {
    MONTH_DAY_YEAR,
    DAY_MONTH_YEAR,
}

enum class WeekStart {
    SUNDAY,
    MONDAY,
}

enum class PreferredPeriodFallbackMode {
    STRICT,
    USE_OTHER_PRODUCTIVE_PERIODS,
}

enum class UrgentRescheduleMode {
    MOVE_THIS_TASK_FIRST,
    MOVE_OTHER_FLEXIBLE_IF_NEEDED,
}

enum class ReminderTimingMode {
    AT_DUE_DATE,
    AT_TASK_TIME,
}

enum class HistoryRetention {
    SEVEN_DAYS,
    THIRTY_DAYS,
    FOREVER,
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dateFormatPreference: DateFormatPreference = DateFormatPreference.MONTH_DAY_YEAR,
    val weekStart: WeekStart = WeekStart.SUNDAY,
    val breakBufferMinutes: Int = 0,
    val alignmentMinutes: Int = 30,
    val allowTaskSplitting: Boolean = true,
    val maxTaskChunkMinutes: Int = 120,
    val preferredPeriodFallbackMode: PreferredPeriodFallbackMode = PreferredPeriodFallbackMode.USE_OTHER_PRODUCTIVE_PERIODS,
    val urgentRescheduleMode: UrgentRescheduleMode = UrgentRescheduleMode.MOVE_OTHER_FLEXIBLE_IF_NEEDED,
    val defaultTaskReminder: Boolean = false,
    val reminderTimingMode: ReminderTimingMode = ReminderTimingMode.AT_TASK_TIME,
    val reminderLeadMinutes: Int = 15,
    val historyRetention: HistoryRetention = HistoryRetention.THIRTY_DAYS,
    val hasCompletedOnboarding: Boolean = false,
)

class AppSettingsRepository(
    private val context: Context,
) {
    val settings: Flow<AppSettings> = context.settingsDataStore.data.map(::toSettings)

    suspend fun current(): AppSettings = settings.first()

    suspend fun setThemeMode(value: ThemeMode) = updateString(THEME_MODE, value.name)
    suspend fun setDateFormatPreference(value: DateFormatPreference) = updateString(DATE_FORMAT_PREFERENCE, value.name)
    suspend fun setWeekStart(value: WeekStart) = updateString(WEEK_START, value.name)
    suspend fun setBreakBufferMinutes(value: Int) = updateInt(BREAK_BUFFER_MINUTES, value.coerceIn(0, 60))
    suspend fun setAlignmentMinutes(value: Int) = updateInt(
        ALIGNMENT_MINUTES,
        when {
            value <= 15 -> 15
            value <= 30 -> 30
            else -> 60
        },
    )
    suspend fun setAllowTaskSplitting(value: Boolean) = updateBoolean(ALLOW_TASK_SPLITTING, value)
    suspend fun setMaxTaskChunkMinutes(value: Int) = updateInt(MAX_TASK_CHUNK_MINUTES, value.coerceIn(30, 360))
    suspend fun setPreferredPeriodFallbackMode(value: PreferredPeriodFallbackMode) = updateString(PREFERRED_PERIOD_FALLBACK_MODE, value.name)
    suspend fun setUrgentRescheduleMode(value: UrgentRescheduleMode) = updateString(URGENT_RESCHEDULE_MODE, value.name)
    suspend fun setDefaultTaskReminder(value: Boolean) = updateBoolean(DEFAULT_TASK_REMINDER, value)
    suspend fun setReminderTimingMode(value: ReminderTimingMode) = updateString(REMINDER_TIMING_MODE, value.name)
    suspend fun setReminderLeadMinutes(value: Int) = updateInt(REMINDER_LEAD_MINUTES, value.coerceIn(5, 120))
    suspend fun setHistoryRetention(value: HistoryRetention) = updateString(HISTORY_RETENTION, value.name)
    suspend fun setHasCompletedOnboarding(value: Boolean) = updateBoolean(HAS_COMPLETED_ONBOARDING, value)

    private suspend fun updateString(key: Preferences.Key<String>, value: String) {
        context.settingsDataStore.edit { prefs -> prefs[key] = value }
    }

    private suspend fun updateInt(key: Preferences.Key<Int>, value: Int) {
        context.settingsDataStore.edit { prefs -> prefs[key] = value }
    }

    private suspend fun updateBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[key] = value }
    }

    private fun toSettings(prefs: Preferences): AppSettings =
        AppSettings(
            themeMode = prefs[THEME_MODE].safeEnumOrDefault(AppSettings().themeMode),
            dateFormatPreference = prefs[DATE_FORMAT_PREFERENCE].safeEnumOrDefault(AppSettings().dateFormatPreference),
            weekStart = prefs[WEEK_START].safeEnumOrDefault(AppSettings().weekStart),
            breakBufferMinutes = prefs[BREAK_BUFFER_MINUTES] ?: AppSettings().breakBufferMinutes,
            alignmentMinutes = prefs[ALIGNMENT_MINUTES] ?: AppSettings().alignmentMinutes,
            allowTaskSplitting = prefs[ALLOW_TASK_SPLITTING] ?: AppSettings().allowTaskSplitting,
            maxTaskChunkMinutes = prefs[MAX_TASK_CHUNK_MINUTES] ?: AppSettings().maxTaskChunkMinutes,
            preferredPeriodFallbackMode = prefs[PREFERRED_PERIOD_FALLBACK_MODE].safeEnumOrDefault(AppSettings().preferredPeriodFallbackMode),
            urgentRescheduleMode = prefs[URGENT_RESCHEDULE_MODE].safeEnumOrDefault(AppSettings().urgentRescheduleMode),
            defaultTaskReminder = prefs[DEFAULT_TASK_REMINDER] ?: AppSettings().defaultTaskReminder,
            reminderTimingMode = prefs[REMINDER_TIMING_MODE].safeEnumOrDefault(AppSettings().reminderTimingMode),
            reminderLeadMinutes = prefs[REMINDER_LEAD_MINUTES] ?: AppSettings().reminderLeadMinutes,
            historyRetention = prefs[HISTORY_RETENTION].safeEnumOrDefault(AppSettings().historyRetention),
            hasCompletedOnboarding = prefs[HAS_COMPLETED_ONBOARDING] ?: AppSettings().hasCompletedOnboarding,
        )

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DATE_FORMAT_PREFERENCE = stringPreferencesKey("date_format_preference")
        val WEEK_START = stringPreferencesKey("week_start")
        val BREAK_BUFFER_MINUTES = intPreferencesKey("break_buffer_minutes")
        val ALIGNMENT_MINUTES = intPreferencesKey("alignment_minutes")
        val ALLOW_TASK_SPLITTING = booleanPreferencesKey("allow_task_splitting")
        val MAX_TASK_CHUNK_MINUTES = intPreferencesKey("max_task_chunk_minutes")
        val PREFERRED_PERIOD_FALLBACK_MODE = stringPreferencesKey("preferred_period_fallback_mode")
        val URGENT_RESCHEDULE_MODE = stringPreferencesKey("urgent_reschedule_mode")
        val DEFAULT_TASK_REMINDER = booleanPreferencesKey("default_task_reminder")
        val REMINDER_TIMING_MODE = stringPreferencesKey("reminder_timing_mode")
        val REMINDER_LEAD_MINUTES = intPreferencesKey("reminder_lead_minutes")
        val HISTORY_RETENTION = stringPreferencesKey("history_retention")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
    }
}

private inline fun <reified T : Enum<T>> String?.safeEnumOrDefault(default: T): T =
    this?.let { runCatching { enumValueOf<T>(it) }.getOrDefault(default) } ?: default
