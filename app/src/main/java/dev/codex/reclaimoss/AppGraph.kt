package dev.codex.reclaimoss

import android.content.Context
import androidx.room.Room
import dev.codex.reclaimoss.data.calendar.NoOpGoogleCalendarGateway
import dev.codex.reclaimoss.data.local.OpenReclaimDatabase
import dev.codex.reclaimoss.data.repository.PlannerRepository
import dev.codex.reclaimoss.data.repository.PlannerRepositoryImpl
import dev.codex.reclaimoss.domain.scheduling.SchedulerEngine
import dev.codex.reclaimoss.domain.service.PlannerCoordinator
import dev.codex.reclaimoss.settings.AppSettingsRepository

class AppGraph(context: Context) {
    private val database = Room.databaseBuilder(
        context,
        OpenReclaimDatabase::class.java,
        "open-reclaim.db",
    ).addMigrations(
        OpenReclaimDatabase.MIGRATION_5_6,
        OpenReclaimDatabase.MIGRATION_6_7,
        OpenReclaimDatabase.MIGRATION_7_8,
        OpenReclaimDatabase.MIGRATION_8_9,
    ).fallbackToDestructiveMigrationOnDowngrade().build()

    private val calendarGateway = NoOpGoogleCalendarGateway()
    private val schedulerEngine = SchedulerEngine()
    val appSettingsRepository = AppSettingsRepository(context)

    val plannerRepository: PlannerRepository = PlannerRepositoryImpl(
        projectDao = database.projectDao(),
        taskDao = database.taskDao(),
        scheduleBlockDao = database.scheduleBlockDao(),
        timePeriodDao = database.timePeriodDao(),
        reminderDao = database.reminderDao(),
        schedulingIssueDao = database.schedulingIssueDao(),
    )

    val plannerCoordinator = PlannerCoordinator(
        repository = plannerRepository,
        scheduler = schedulerEngine,
        calendarGateway = calendarGateway,
        getSettings = { appSettingsRepository.current() },
    )
}
