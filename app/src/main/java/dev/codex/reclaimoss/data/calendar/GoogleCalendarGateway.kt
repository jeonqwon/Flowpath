package dev.codex.reclaimoss.data.calendar

import dev.codex.reclaimoss.domain.model.ScheduleBlock
import dev.codex.reclaimoss.domain.scheduling.SchedulerEngine
import java.time.Instant

interface GoogleCalendarGateway {
    suspend fun syncBusyEvents(rangeStart: Instant, rangeEnd: Instant): List<SchedulerEngine.BusyWindow>
    suspend fun syncPlannedBlocks(blocks: List<ScheduleBlock>)
}

class NoOpGoogleCalendarGateway : GoogleCalendarGateway {
    override suspend fun syncBusyEvents(
        rangeStart: Instant,
        rangeEnd: Instant,
    ): List<SchedulerEngine.BusyWindow> = emptyList()

    override suspend fun syncPlannedBlocks(blocks: List<ScheduleBlock>) = Unit
}
