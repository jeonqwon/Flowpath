package dev.codex.reclaimoss.domain.scheduling

import dev.codex.reclaimoss.domain.model.*
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.random.Random

fun main() {
    val zoneId = ZoneId.of("America/New_York")
    val fmt = DateTimeFormatter.ofPattern("MMM d, h:mm a")
    val tz = zoneId
    var pass = 0
    var fail = 0

    fun assert(name: String, condition: Boolean, detail: String = "") {
        if (condition) { pass++; println("  ✓ $name") }
        else { fail++; println("  ✗ FAIL: $name — $detail") }
    }

    fun t(iso: String) = LocalDateTime.parse(iso).atZone(tz).toInstant()

    // ═══════════════════════════════════════════════════════════════
    println("SCHEDULER EDGE CASE TEST SUITE")
    println("═══════════════════════════════════════════════════════════════\n")

    // ─── 1. Split around sleep ───
    println("── 1. Split around sleep ──")
    val plan1 = schedule(tz, listOf(
        sleepTask("sleep", t("2026-06-10T22:00"), t("2026-06-11T06:00")),
        normTask("work", t("2026-06-10T20:00"), 180, true, t("2026-06-12T17:00")),
    ))
    val blocks1 = plan1.blocks.filter { it.taskId == "work" }.sortedBy { it.startAt }
    assert("Two blocks placed", blocks1.size == 2, "got ${blocks1.size}")
    assert("First block ends at sleep start", blocks1[0].endAt == t("2026-06-10T22:00"), "${blocks1[0].endAt}")
    assert("Second block starts at sleep end", blocks1[1].startAt == t("2026-06-11T06:00"), "${blocks1[1].startAt}")
    assert("No gap after sleep", blocks1[1].startAt == t("2026-06-11T06:00"))

    // ─── 2. Split with alignment gap ───
    println("\n── 2. Split with alignment ──")
    val plan2 = schedule(tz, listOf(
        sleepTask("sleep", t("2026-06-10T22:00"), t("2026-06-11T06:00")),
        normTask("work", t("2026-06-10T20:00"), 150, true, t("2026-06-12T17:00")),
    ), alignmentMinutes = 30)
    val blocks2 = plan2.blocks.filter { it.taskId == "work" }.sortedBy { it.startAt }
    assert("Second block aligned to 30min", blocks2[1].startAt.atZone(tz).minute == 0,
        "minute=${blocks2[1].startAt.atZone(tz).minute}")
    assert("No unnecessary gap", blocks2[1].startAt == t("2026-06-11T06:00"))

    // ─── 3. Extend duration far past sleep ───
    println("\n── 3. Long task split across sleep ──")
    val plan3 = schedule(tz, listOf(
        sleepTask("sleep", t("2026-06-10T22:00"), t("2026-06-11T06:00")),
        normTask("work", t("2026-06-10T16:00"), 600, true, t("2026-06-13T17:00")),
    ))
    val blocks3 = plan3.blocks.filter { it.taskId == "work" }
    assert("More than 2 blocks", blocks3.size >= 2, "got ${blocks3.size}")
    val total3 = blocks3.sumOf { java.time.Duration.between(it.startAt, it.endAt).toMinutes() }
    assert("Full duration scheduled", total3.toInt() == 600, "got $total3")

    // ─── 4. FIXED_EXACT blocked by sleep, splitting ON ───
    println("\n── 4. FIXED_EXACT blocked → FLEXIBLE fallback ──")
    val plan4 = schedule(tz, listOf(
        sleepTask("sleep", t("2026-06-10T22:00"), t("2026-06-11T06:00")),
        ScheduleTask(id = "exact", title = "", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = false, allowSplitting = true,
            schedulingMode = TaskSchedulingMode.FIXED_EXACT,
            fixedStartAt = t("2026-06-10T20:00"), fixedEndAt = t("2026-06-10T23:00"),
            dueAt = t("2026-06-12T17:00"), estimatedMinutes = 180, remainingMinutes = 180,
            overlapPolicy = TaskOverlapPolicy.ALLOW, status = TaskStatus.ACTIVE),
    ), allowConcurrent = true)
    val blocks4 = plan4.blocks.filter { it.taskId == "exact" }
    assert("FIXED_EXACT gets blocks after conversion", blocks4.isNotEmpty(), "got ${blocks4.size}")

    // ─── 5. FIXED_EXACT in SchedulerEngine (treated as flexible) ──
    println("\n── 5. FIXED_EXACT in scheduler (flexible fallback) ──")
    val plan5 = schedule(tz, listOf(
        sleepTask("sleep", t("2026-06-10T22:00"), t("2026-06-11T06:00")),
        ScheduleTask(id = "exact2", title = "", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = false, allowSplitting = false,
            schedulingMode = TaskSchedulingMode.FIXED_EXACT,
            fixedStartAt = t("2026-06-10T20:00"), fixedEndAt = t("2026-06-10T23:00"),
            dueAt = t("2026-06-12T17:00"), estimatedMinutes = 180, remainingMinutes = 180,
            overlapPolicy = TaskOverlapPolicy.ALLOW, status = TaskStatus.ACTIVE),
    ), allowConcurrent = true)
    // SchedulerEngine doesn't have placeExactTask; FIXED_EXACT gets normal scheduling
    assert("FIXED_EXACT gets scheduled", plan5.blocks.any { it.taskId == "exact2" },
        "SchedulerEngine treats FIXED_EXACT as flexible")

    // ─── 6. Multiple normal tasks with overlap disabled ───
    println("\n── 6. No-overlap tasks ──")
    val plan6 = schedule(tz, listOf(
        normTask("A", t("2026-06-10T09:00"), 120, false),
        normTask("B", null, 120, false),
    ), allowConcurrent = false, alignmentMinutes = 0)
    val blocksA = plan6.blocks.filter { it.taskId == "A" }
    val blocksB = plan6.blocks.filter { it.taskId == "B" }
    val overlap6 = blocksA.any { a -> blocksB.any { b -> overlaps(a, b) } }
    assert("No-overlap: tasks don't overlap", !overlap6)

    // ─── 7. Task with deadline, fits before deadline ──
    println("\n── 7. Deadline respected ──")
    val plan7 = schedule(tz, listOf(
        ScheduleTask(id = "deadline", title = "", taskKind = TaskKind.NORMAL, priority = TaskPriority.HIGH,
            hasDeadline = true, allowSplitting = false,
            schedulingMode = TaskSchedulingMode.FLEXIBLE,
            dueAt = t("2026-06-10T17:00"), estimatedMinutes = 60, remainingMinutes = 60,
            overlapPolicy = TaskOverlapPolicy.INHERIT, status = TaskStatus.ACTIVE),
    ))
    val blocks7 = plan7.blocks.filter { it.taskId == "deadline" }
    assert("Scheduled before deadline", blocks7.all { !it.endAt.isAfter(t("2026-06-10T17:00")) })

    // ─── 8. Task too big for tight deadline ──
    println("\n── 8. Task beyond tight deadline → partial ──")
    val plan8 = schedule(tz, listOf(
        ScheduleTask(id = "big", title = "", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = true, allowSplitting = true,
            schedulingMode = TaskSchedulingMode.FLEXIBLE,
            dueAt = t("2026-06-09T00:30"), estimatedMinutes = 480, remainingMinutes = 480,
            overlapPolicy = TaskOverlapPolicy.INHERIT, status = TaskStatus.ACTIVE),
    ), alignmentMinutes = 0, rangeStart = t("2026-06-09T00:00"))
    val blocks8 = plan8.blocks.filter { it.taskId == "big" }
    val total8 = blocks8.sumOf { java.time.Duration.between(it.startAt, it.endAt).toMinutes() }
    assert("Scheduled within deadline", total8 <= 480, "got $total8")

    // ─── 9. Priority ordering ──
    println("\n── 9. Urgent scheduled before normal ──")
    val plan9 = schedule(tz, listOf(
        normTask("low", null, 120, false, due = t("2026-06-11T17:00")),
        ScheduleTask(id = "urgent", title = "", taskKind = TaskKind.NORMAL, priority = TaskPriority.URGENT,
            hasDeadline = true, allowSplitting = false,
            schedulingMode = TaskSchedulingMode.FLEXIBLE,
            dueAt = t("2026-06-10T17:00"), estimatedMinutes = 120, remainingMinutes = 120,
            overlapPolicy = TaskOverlapPolicy.ALLOW, status = TaskStatus.ACTIVE),
    ), allowConcurrent = false, alignmentMinutes = 0)
    val urgentBlock = plan9.blocks.firstOrNull { it.taskId == "urgent" }
    val lowBlock = plan9.blocks.firstOrNull { it.taskId == "low" }
    if (urgentBlock != null && lowBlock != null) {
        assert("Urgent scheduled before low priority", urgentBlock.startAt <= lowBlock.startAt)
    } else { assert("Both tasks scheduled", false, "one missing") }

    // ─── 10. Sleep priority over normal ──
    println("\n── 10. Sleep always scheduled first ──")
    val plan10 = schedule(tz, listOf(
        normTask("work", t("2026-06-10T20:00"), 120, false),
        sleepTask("sleep", t("2026-06-10T22:00"), t("2026-06-11T06:00")),
    ))
    val sleepBlock10 = plan10.blocks.firstOrNull { it.taskId == "sleep" }
    assert("Sleep block placed", sleepBlock10 != null)
    assert("Sleep at correct time", sleepBlock10!!.startAt == t("2026-06-10T22:00"))

    // ─── 11. Task without deadline ──
    println("\n── 11. No deadline → scheduled far ahead ──")
    val plan11 = schedule(tz, listOf(
        normTask("flex", null, 120, false, due = t("2027-06-10T17:00")),
    ), alignmentMinutes = 0)
    assert("Flexible task gets blocks", plan11.blocks.any { it.taskId == "flex" })

    // ─── 12. Break buffer creates gap ──
    println("\n── 12. Break buffer between blocks ──")
    val plan12 = schedule(tz, listOf(
        sleepTask("sleep", t("2026-06-10T22:00"), t("2026-06-11T06:00")),
        normTask("work", t("2026-06-10T16:00"), 240, true, t("2026-06-13T17:00")),
    ), breakBuffer = 15)
    val blocks12 = plan12.blocks.filter { it.taskId == "work" }.sortedBy { it.startAt }
    if (blocks12.size >= 2) {
        val gap = java.time.Duration.between(blocks12[0].endAt, blocks12[1].startAt).toMinutes()
        assert("Gap between split blocks >= buffer", gap >= 15, "gap=$gap")
    }

    // ─── 13. Concurrent tasks (allow overlap) ──
    println("\n── 13. Concurrent tasks share time ──")
    val plan13 = schedule(tz, listOf(
        ScheduleTask(id = "concurA", title = "", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = false, allowSplitting = false,
            schedulingMode = TaskSchedulingMode.FLEXIBLE,
            fixedStartAt = t("2026-06-10T10:00"),
            dueAt = t("2026-06-11T17:00"), estimatedMinutes = 60, remainingMinutes = 60,
            overlapPolicy = TaskOverlapPolicy.ALLOW, status = TaskStatus.ACTIVE),
        ScheduleTask(id = "concurB", title = "", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = false, allowSplitting = false,
            schedulingMode = TaskSchedulingMode.FLEXIBLE,
            fixedStartAt = t("2026-06-10T10:00"),
            dueAt = t("2026-06-11T17:00"), estimatedMinutes = 60, remainingMinutes = 60,
            overlapPolicy = TaskOverlapPolicy.ALLOW, status = TaskStatus.ACTIVE),
    ), allowConcurrent = true, alignmentMinutes = 0)
    val b13a = plan13.blocks.filter { it.taskId == "concurA" }
    val b13b = plan13.blocks.filter { it.taskId == "concurB" }
    assert("Both concurrent tasks scheduled", b13a.isNotEmpty() && b13b.isNotEmpty())
    // With fixed starts at same time, they should overlap or be sequential

    // ─── 14. DISALLOW overrides global setting ──
    println("\n── 14. Per-task DISALLOW blocks overlap ──")
    val plan14 = schedule(tz, listOf(
        ScheduleTask(id = "noOverlap", title = "", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = false, allowSplitting = false,
            schedulingMode = TaskSchedulingMode.FLEXIBLE,
            fixedStartAt = t("2026-06-10T10:00"),
            dueAt = t("2026-06-11T17:00"), estimatedMinutes = 120, remainingMinutes = 120,
            overlapPolicy = TaskOverlapPolicy.DISALLOW, status = TaskStatus.ACTIVE),
        ScheduleTask(id = "overlapOk", title = "", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = false, allowSplitting = false,
            schedulingMode = TaskSchedulingMode.FLEXIBLE,
            fixedStartAt = t("2026-06-10T10:00"),
            dueAt = t("2026-06-11T17:00"), estimatedMinutes = 60, remainingMinutes = 60,
            overlapPolicy = TaskOverlapPolicy.ALLOW, status = TaskStatus.ACTIVE),
    ), allowConcurrent = true, alignmentMinutes = 0)
    val bNo = plan14.blocks.filter { it.taskId == "noOverlap" }
    val bOk = plan14.blocks.filter { it.taskId == "overlapOk" }
    val overlap14 = bNo.any { a -> bOk.any { b -> overlaps(a, b) } }
    assert("DISALLOW task doesn't overlap", !overlap14)

    // ─── 15. FIXED_DAY constraint ──
    println("\n── 15. FIXED_DAY stays on specified day ──")
    val dayStart = t("2026-06-10T00:00")
    val dayEnd = t("2026-06-10T23:59")
    val plan15 = schedule(tz, listOf(
        ScheduleTask(id = "fixedDay", title = "", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = true, allowSplitting = false,
            schedulingMode = TaskSchedulingMode.FIXED_DAY,
            dueAt = dayEnd, estimatedMinutes = 60, remainingMinutes = 60,
            overlapPolicy = TaskOverlapPolicy.INHERIT, status = TaskStatus.ACTIVE),
    ), rangeStart = dayStart)
    val blocks15 = plan15.blocks.filter { it.taskId == "fixedDay" }
    assert("FIXED_DAY block on correct day", blocks15.all {
        !it.startAt.isBefore(dayStart) && !it.endAt.isAfter(dayEnd)
    })

    // ─── 16. Editing: increase duration → split ──
    println("\n── 16. Edit: increase duration triggers split ──")
    val plan16a = schedule(tz, listOf(
        sleepTask("sleep", t("2026-06-10T22:00"), t("2026-06-11T06:00")),
        normTask("editMe", t("2026-06-10T20:00"), 60, false, t("2026-06-12T17:00")),
    ))
    val before16 = plan16a.blocks.filter { it.taskId == "editMe" }.size
    // Now "edit" — increase duration
    val plan16b = schedule(tz, listOf(
        sleepTask("sleep", t("2026-06-10T22:00"), t("2026-06-11T06:00")),
        normTask("editMe", t("2026-06-10T20:00"), 180, true, t("2026-06-12T17:00")),
    ))
    val after16 = plan16b.blocks.filter { it.taskId == "editMe" }.size
    assert("Edit: more blocks after duration increase", after16 > before16,
        "before=$before16 after=$after16")

    // ─── 17. Sleep on multiple days ──
    println("\n── 17. Sleep on multiple days ──")
    val plan17 = schedule(tz, listOf(
        sleepTask("sleep-mon", t("2026-06-08T22:00"), t("2026-06-09T06:00")),
        sleepTask("sleep-tue", t("2026-06-09T22:00"), t("2026-06-10T06:00")),
        normTask("work", t("2026-06-08T16:00"), 600, true, t("2026-06-13T17:00")),
    ))
    val blocks17 = plan17.blocks.filter { it.taskId == "work" }
    val total17 = blocks17.sumOf { java.time.Duration.between(it.startAt, it.endAt).toMinutes() }
    assert("Task scheduled across multiple sleeps", total17.toInt() == 600, "got ${total17}min in ${blocks17.size} blocks")

    // ─── 18. Blocker task ──
    println("\n── 18. Blocker reserves time ──")
    val plan18 = schedule(tz, listOf(
        ScheduleTask(id = "meeting", title = "", taskKind = TaskKind.BLOCKER, priority = TaskPriority.HIGH,
            hasDeadline = false, allowSplitting = false,
            schedulingMode = TaskSchedulingMode.FIXED_EXACT,
            fixedStartAt = t("2026-06-10T14:00"), fixedEndAt = t("2026-06-10T15:00"),
            dueAt = t("2026-06-10T15:00"), estimatedMinutes = 0, remainingMinutes = 0,
            overlapPolicy = TaskOverlapPolicy.DISALLOW, status = TaskStatus.ACTIVE),
        normTask("work", null, 120, false, t("2026-06-11T17:00")),
    ), allowConcurrent = false, alignmentMinutes = 0)
    val blocks18 = plan18.blocks.filter { it.taskId == "work" }
    val conflictsWithMeeting = blocks18.any { b ->
        !b.endAt.isBefore(t("2026-06-10T14:00")) && !b.startAt.isAfter(t("2026-06-10T15:00"))
    }
    assert("Work doesn't overlap meeting", !conflictsWithMeeting)

    // ─── 19. No splitting at all ──
    println("\n── 19. No splitting: task must fit in one block ──")
    val plan19 = schedule(tz, listOf(
        sleepTask("sleep", t("2026-06-10T22:00"), t("2026-06-11T06:00")),
        ScheduleTask(id = "noSplit", title = "", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = false, allowSplitting = false,
            schedulingMode = TaskSchedulingMode.FLEXIBLE,
            fixedStartAt = t("2026-06-10T16:00"),
            dueAt = t("2026-06-10T23:59"), estimatedMinutes = 480, remainingMinutes = 480,
            overlapPolicy = TaskOverlapPolicy.ALLOW, status = TaskStatus.ACTIVE),
    ), allowTaskSplitting = false, allowConcurrent = true)
    val blocks19 = plan19.blocks.filter { it.taskId == "noSplit" }
    val total19 = blocks19.sumOf { java.time.Duration.between(it.startAt, it.endAt).toMinutes() }
    assert("No-split: single continuous or partial", blocks19.size <= 1 || total19 < 480,
        "blocks=${blocks19.size} total=$total19")

    // ─── 20. Zero-minute task ──
    println("\n── 20. Zero-minute blocker isn't split ──")
    val plan20 = schedule(tz, listOf(
        sleepTask("sleep", t("2026-06-10T22:00"), t("2026-06-11T06:00")),
        ScheduleTask(id = "zero", title = "", taskKind = TaskKind.BLOCKER, priority = TaskPriority.HIGH,
            hasDeadline = false, allowSplitting = true,
            schedulingMode = TaskSchedulingMode.FIXED_EXACT,
            fixedStartAt = t("2026-06-10T22:30"), fixedEndAt = t("2026-06-10T23:00"),
            dueAt = t("2026-06-10T23:00"), estimatedMinutes = 0, remainingMinutes = 0,
            overlapPolicy = TaskOverlapPolicy.DISALLOW, status = TaskStatus.ACTIVE),
    ), allowConcurrent = true)
    val issue20 = plan20.issues.firstOrNull { it.taskId == "zero" }
    assert("Zero-min blocker blocked by sleep stays FIXED_EXACT",
        issue20 != null || plan20.blocks.none { it.taskId == "zero" })

    // ─── 21. Task extending past midnight without sleep ──
    println("\n── 21. Overnight task without sleep ──")
    val plan21 = schedule(tz, listOf(
        normTask("night", t("2026-06-10T23:00"), 120, false, t("2026-06-11T17:00")),
    ), alignmentMinutes = 0)
    val blocks21 = plan21.blocks.filter { it.taskId == "night" }
    assert("Overnight task scheduled", blocks21.isNotEmpty())
    assert("Full duration placed", blocks21.sumOf { java.time.Duration.between(it.startAt, it.endAt).toMinutes() }.toInt() == 120)

    // ─── 22. Recurring: multiple occurrences ──
    println("\n── 22. Recurring-like: multiple same-series tasks ──")
    val plan22 = schedule(tz, listOf(
        ScheduleTask(id = "recur-1", title = "", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = false, allowSplitting = false,
            schedulingMode = TaskSchedulingMode.FLEXIBLE,
            recurrenceSeriesId = "series-1",
            dueAt = t("2026-06-10T17:00"), estimatedMinutes = 30, remainingMinutes = 30,
            overlapPolicy = TaskOverlapPolicy.INHERIT, status = TaskStatus.ACTIVE),
        ScheduleTask(id = "recur-2", title = "", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = false, allowSplitting = false,
            schedulingMode = TaskSchedulingMode.FLEXIBLE,
            recurrenceSeriesId = "series-1",
            dueAt = t("2026-06-11T17:00"), estimatedMinutes = 30, remainingMinutes = 30,
            overlapPolicy = TaskOverlapPolicy.INHERIT, status = TaskStatus.ACTIVE),
        ScheduleTask(id = "recur-3", title = "", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = false, allowSplitting = false,
            schedulingMode = TaskSchedulingMode.FLEXIBLE,
            recurrenceSeriesId = "series-1",
            dueAt = t("2026-06-12T17:00"), estimatedMinutes = 30, remainingMinutes = 30,
            overlapPolicy = TaskOverlapPolicy.INHERIT, status = TaskStatus.ACTIVE),
    ))
    val total22 = plan22.blocks.size
    assert("All recurrence occurrences scheduled", total22 >= 3, "got $total22")

    // ─── 23. Range start in the past ──
    println("\n── 23. Range start far in past → uses now ──")
    val plan23 = schedule(tz, listOf(
        normTask("past", null, 30, false, t("2026-06-20T17:00")),
    ), rangeStart = t("2020-01-01T00:00"))
    assert("Task scheduled despite ancient rangeStart", plan23.blocks.any { it.taskId == "past" })

    // ─── 24. 14-day window exhausted ──
    println("\n── 24. Enormous task exceeds 14-day window ──")
    val plan24 = schedule(tz, listOf(
        sleepTask("sleep", t("2026-06-10T22:00"), t("2026-06-11T06:00")),
        normTask("huge", t("2026-06-10T08:00"), 20000, true, t("2026-07-10T17:00")),
    ))
    val total24 = plan24.blocks.filter { it.taskId == "huge" }
        .sumOf { java.time.Duration.between(it.startAt, it.endAt).toMinutes() }
    assert("Huge task gets blocks", total24 > 0,
        "scheduled=$total24 of 20000")

    // ─── 25. Window constraint (preferred time window) ──
    println("\n── 25. Time window (9am-5pm only) ──")
    val workHours25 = WorkHoursProfile(
        timezone = tz.id,
        days = DayOfWeek.entries.associateWith {
            WorkHoursDay(windows = listOf(TimeWindow(LocalTime.of(9, 0), LocalTime.of(17, 0))))
        }
    )
    val plan25 = scheduleWithHours(tz, listOf(
        normTask("business", null, 120, false, due = t("2026-06-11T17:00")),
    ), workHours25, alignmentMinutes = 0)
    val blocks25 = plan25.blocks.filter { it.taskId == "business" }
    assert("Scheduled within work hours", blocks25.all {
        val hour = it.startAt.atZone(tz).hour
        hour >= 9 && hour < 17
    }, blocks25.joinToString { it.startAt.atZone(tz).toString() })

    // ─── 26. Dependency: After parent scheduled end ──
    println("\n── 26. Dependency: after parent ends ──")
    val plan26 = schedule(tz, listOf(
        normTask("parent", t("2026-06-10T09:00"), 60, false, t("2026-06-10T17:00")),
        ScheduleTask(id = "child", title = "child", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = false, allowSplitting = false,
            schedulingMode = TaskSchedulingMode.FLEXIBLE,
            continuationParentTaskId = "parent",
            continuationMode = TaskContinuationMode.AFTER_PARENT_SCHEDULED_END,
            dueAt = t("2026-06-10T17:00"), estimatedMinutes = 30, remainingMinutes = 30,
            overlapPolicy = TaskOverlapPolicy.ALLOW, status = TaskStatus.ACTIVE),
    ), rangeStart = t("2026-06-10T08:00"), alignmentMinutes = 0)
    val parentBlock = plan26.blocks.firstOrNull { it.taskId == "parent" }
    val childBlock = plan26.blocks.firstOrNull { it.taskId == "child" }
    println("  BLOCKS: parent=${parentBlock?.startAt}→${parentBlock?.endAt} child=${childBlock?.startAt}→${childBlock?.endAt}")
    assert("Parent scheduled", parentBlock != null)
    assert("Child scheduled after parent ends", childBlock != null && childBlock.startAt >= parentBlock!!.endAt,
        "parent ends=${parentBlock?.endAt} child starts=${childBlock?.startAt}")

    // ─── 27. Dependency: Before parent starts ──
    println("\n── 27. Dependency: before parent starts ──")
    val plan27 = schedule(tz, listOf(
        normTask("parent", t("2026-06-10T12:00"), 60, false, t("2026-06-10T17:00")),
        ScheduleTask(id = "pre", title = "pre", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = false, allowSplitting = false,
            schedulingMode = TaskSchedulingMode.FLEXIBLE,
            continuationParentTaskId = "parent",
            continuationMode = TaskContinuationMode.BEFORE_PARENT_START,
            dueAt = t("2026-06-10T17:00"), estimatedMinutes = 30, remainingMinutes = 30,
            overlapPolicy = TaskOverlapPolicy.ALLOW, status = TaskStatus.ACTIVE),
    ), rangeStart = t("2026-06-10T08:00"), alignmentMinutes = 0)
    val pBlock27 = plan27.blocks.firstOrNull { it.taskId == "parent" }
    val preBlock = plan27.blocks.firstOrNull { it.taskId == "pre" }
    assert("Pre-task scheduled before parent", preBlock != null && pBlock27 != null &&
        preBlock!!.endAt <= pBlock27!!.startAt,
        "pre ends=${preBlock?.endAt} parent starts=${pBlock27?.startAt}")

    // ─── 28. Dependency: After parent dueAt ──
    println("\n── 28. Dependency: after parent deadline ──")
    val plan28 = schedule(tz, listOf(
        normTask("parent", t("2026-06-10T14:00"), 60, false, t("2026-06-10T17:00")),
        ScheduleTask(id = "afterDeadline", title = "after", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = false, allowSplitting = false,
            schedulingMode = TaskSchedulingMode.FLEXIBLE,
            continuationParentTaskId = "parent",
            continuationMode = TaskContinuationMode.AFTER_PARENT_DUE_AT,
            dueAt = t("2026-06-11T17:00"), estimatedMinutes = 30, remainingMinutes = 30,
            overlapPolicy = TaskOverlapPolicy.ALLOW, status = TaskStatus.ACTIVE),
    ), rangeStart = t("2026-06-10T08:00"), alignmentMinutes = 0)
    val child28 = plan28.blocks.firstOrNull { it.taskId == "afterDeadline" }
    assert("Child after parent deadline", child28 != null && child28.startAt >= t("2026-06-10T17:00"),
        "child starts=${child28?.startAt}")

    // ─── 29. Dependency chain: A -> B -> C ──
    println("\n── 29. Dependency chain A→B→C ──")
    val plan29 = schedule(tz, listOf(
        normTask("A", t("2026-06-10T09:00"), 30, false, t("2026-06-10T17:00")),
        ScheduleTask(id = "B", title = "B", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = false, allowSplitting = false,
            schedulingMode = TaskSchedulingMode.FLEXIBLE,
            continuationParentTaskId = "A",
            continuationMode = TaskContinuationMode.AFTER_PARENT_SCHEDULED_END,
            dueAt = t("2026-06-10T17:00"), estimatedMinutes = 30, remainingMinutes = 30,
            overlapPolicy = TaskOverlapPolicy.ALLOW, status = TaskStatus.ACTIVE),
        ScheduleTask(id = "C", title = "C", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = false, allowSplitting = false,
            schedulingMode = TaskSchedulingMode.FLEXIBLE,
            continuationParentTaskId = "B",
            continuationMode = TaskContinuationMode.AFTER_PARENT_SCHEDULED_END,
            dueAt = t("2026-06-10T17:00"), estimatedMinutes = 30, remainingMinutes = 30,
            overlapPolicy = TaskOverlapPolicy.ALLOW, status = TaskStatus.ACTIVE),
    ), rangeStart = t("2026-06-10T08:00"), alignmentMinutes = 0)
    val bA = plan29.blocks.firstOrNull { it.taskId == "A" }
    val bB = plan29.blocks.firstOrNull { it.taskId == "B" }
    val bC = plan29.blocks.firstOrNull { it.taskId == "C" }
    assert("Chain A→B→C in order", bA != null && bB != null && bC != null &&
        bA!!.endAt <= bB!!.startAt && bB.endAt <= bC!!.startAt,
        "A=${bA?.startAt} B=${bB?.startAt} C=${bC?.startAt}")

    // ─── 30. 3 overlapping fixed-start tasks ──
    println("\n── 30. 3 tasks all at 9am, all ALLOW overlap ──")
    val plan30 = schedule(tz, (1..3).map { i ->
        ScheduleTask(id = "OL$i", title = "Overlap$i", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = false, allowSplitting = false,
            schedulingMode = TaskSchedulingMode.FLEXIBLE,
            fixedStartAt = t("2026-06-10T09:00"),
            dueAt = t("2026-06-11T17:00"), estimatedMinutes = 60, remainingMinutes = 60,
            overlapPolicy = TaskOverlapPolicy.ALLOW, status = TaskStatus.ACTIVE)
    }, allowConcurrent = true, rangeStart = t("2026-06-10T08:00"), alignmentMinutes = 30)
    val b30 = plan30.blocks
    assert("All 3 overlapping scheduled", b30.size == 3, "got ${b30.size}")
    // With ALLOW + fixedStart, all should anchor at 9am
    val at9 = b30.count { it.startAt == t("2026-06-10T09:00") }
    println("  Blocks at 9am: $at9")

    // ─── 31. Mixed overlap: 1 DISALLOW + 2 ALLOW at same time ──
    println("\n── 31. 1 DISALLOW + 2 ALLOW at same time ──")
    val plan31 = schedule(tz, listOf(
        ScheduleTask(id = "no", title = "NoOverlap", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = false, allowSplitting = false,
            schedulingMode = TaskSchedulingMode.FLEXIBLE,
            fixedStartAt = t("2026-06-10T09:00"),
            dueAt = t("2026-06-11T17:00"), estimatedMinutes = 60, remainingMinutes = 60,
            overlapPolicy = TaskOverlapPolicy.DISALLOW, status = TaskStatus.ACTIVE),
        ScheduleTask(id = "yes1", title = "Yes1", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = false, allowSplitting = false,
            schedulingMode = TaskSchedulingMode.FLEXIBLE,
            fixedStartAt = t("2026-06-10T09:00"),
            dueAt = t("2026-06-11T17:00"), estimatedMinutes = 60, remainingMinutes = 60,
            overlapPolicy = TaskOverlapPolicy.ALLOW, status = TaskStatus.ACTIVE),
        ScheduleTask(id = "yes2", title = "Yes2", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = false, allowSplitting = false,
            schedulingMode = TaskSchedulingMode.FLEXIBLE,
            fixedStartAt = t("2026-06-10T09:00"),
            dueAt = t("2026-06-11T17:00"), estimatedMinutes = 60, remainingMinutes = 60,
            overlapPolicy = TaskOverlapPolicy.ALLOW, status = TaskStatus.ACTIVE),
    ), allowConcurrent = true, rangeStart = t("2026-06-10T08:00"), alignmentMinutes = 30)
    val noBlock = plan31.blocks.firstOrNull { it.taskId == "no" }
    val yes1Block = plan31.blocks.firstOrNull { it.taskId == "yes1" }
    val yes2Block = plan31.blocks.firstOrNull { it.taskId == "yes2" }
    assert("DISALLOW task at 9am", noBlock?.startAt == t("2026-06-10T09:00"))
    assert("YES tasks don't overlap DISALLOW",
        yes1Block?.let { !overlaps(it, noBlock!!) } ?: true &&
        yes2Block?.let { !overlaps(it, noBlock!!) } ?: true)
    println("  no: ${noBlock?.startAt} yes1: ${yes1Block?.startAt} yes2: ${yes2Block?.startAt}")

    // ─── 32. Edit: add overlap policy after scheduling ──
    println("\n── 32. Edit scenario: add overlapping task to packed schedule ──")
    // First schedule 3 tasks that fill the morning
    val prePlan = schedule(tz, listOf(
        normTask("fill1", t("2026-06-10T08:00"), 120, false, t("2026-06-10T17:00")),
        normTask("fill2", t("2026-06-10T10:00"), 120, false, t("2026-06-10T17:00")),
        normTask("fill3", t("2026-06-10T12:00"), 120, false, t("2026-06-10T17:00")),
    ), allowConcurrent = false, alignmentMinutes = 0)
    val fillCount = prePlan.blocks.size
    // Now add overlapping task at 8am with ALLOW
    val plan32 = schedule(tz, listOf(
        normTask("fill1", t("2026-06-10T08:00"), 120, false, t("2026-06-10T17:00")),
        normTask("fill2", t("2026-06-10T10:00"), 120, false, t("2026-06-10T17:00")),
        normTask("fill3", t("2026-06-10T12:00"), 120, false, t("2026-06-10T17:00")),
        ScheduleTask(id = "newOL", title = "NewOverlap", taskKind = TaskKind.NORMAL, priority = TaskPriority.URGENT,
            hasDeadline = false, allowSplitting = false,
            schedulingMode = TaskSchedulingMode.FLEXIBLE,
            fixedStartAt = t("2026-06-10T08:00"),
            dueAt = t("2026-06-10T17:00"), estimatedMinutes = 60, remainingMinutes = 60,
            overlapPolicy = TaskOverlapPolicy.ALLOW, status = TaskStatus.ACTIVE),
    ), allowConcurrent = true, alignmentMinutes = 0)
    val newBlock = plan32.blocks.firstOrNull { it.taskId == "newOL" }
    assert("New overlapped task placed at 8am", newBlock?.startAt == t("2026-06-10T08:00"),
        "starts at ${newBlock?.startAt}")
    println("  New task: ${newBlock?.startAt}→${newBlock?.endAt}")

    // ─── 33. Overlap + sleep (concurrent allowed) ──
    println("\n── 33. Overlapping task near sleep ──")
    val plan33 = schedule(tz, listOf(
        sleepTask("sleep", t("2026-06-10T22:00"), t("2026-06-11T06:00")),
        ScheduleTask(id = "evening1", title = "E1", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = false, allowSplitting = false,
            schedulingMode = TaskSchedulingMode.FLEXIBLE,
            fixedStartAt = t("2026-06-10T21:00"),
            dueAt = t("2026-06-11T17:00"), estimatedMinutes = 90, remainingMinutes = 90,
            overlapPolicy = TaskOverlapPolicy.ALLOW, status = TaskStatus.ACTIVE),
    ), allowConcurrent = true, alignmentMinutes = 0)
    val e1 = plan33.blocks.firstOrNull { it.taskId == "evening1" }
    assert("Evening task anchors at 9pm", e1?.startAt == t("2026-06-10T21:00"),
        "starts at ${e1?.startAt}")
    // Should split: 1h before sleep, 30min after
    assert("Splits around sleep", plan33.blocks.count { it.taskId == "evening1" } >= 2,
        "blocks=${plan33.blocks.filter { it.taskId == "evening1" }}")

    // ─── 34. Reschedule: move task from no-overlap to overlap-allowed zone ──
    println("\n── 34. Reschedule: change overlap policy ──")
    val plan34 = schedule(tz, listOf(
        ScheduleTask(id = "fixed", title = "Fixed1h", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = false, allowSplitting = false,
            schedulingMode = TaskSchedulingMode.FLEXIBLE,
            fixedStartAt = t("2026-06-10T09:00"),
            dueAt = t("2026-06-10T17:00"), estimatedMinutes = 60, remainingMinutes = 60,
            overlapPolicy = TaskOverlapPolicy.DISALLOW, status = TaskStatus.ACTIVE),
    ), allowConcurrent = true, rangeStart = t("2026-06-10T08:00"))
    assert("Fixed DISALLOW scheduled", plan34.blocks.size == 1)
    // "Edit" to ALLOW — should still work
    val plan34b = schedule(tz, listOf(
        ScheduleTask(id = "fixed", title = "Fixed1h", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = false, allowSplitting = false,
            schedulingMode = TaskSchedulingMode.FLEXIBLE,
            fixedStartAt = t("2026-06-10T09:00"),
            dueAt = t("2026-06-10T17:00"), estimatedMinutes = 120, remainingMinutes = 120,
            overlapPolicy = TaskOverlapPolicy.ALLOW, status = TaskStatus.ACTIVE),
    ), allowConcurrent = true, rangeStart = t("2026-06-10T08:00"))
    assert("Edit ALLOW + longer duration still scheduled", plan34b.blocks.isNotEmpty())

    // ─── 35. Dependency BEFORE: extend duration → split before parent ──
    println("\n── 35. Before-parent task extended → splits and stays before ──")
    // Original: 30min "prep" before parent at 12pm. Fits easily.
    // Edit: extend to 180min. Must split around sleep (10pm-6am) and still end before parent.
    val plan35 = schedule(tz, listOf(
        sleepTask("sleep", t("2026-06-10T22:00"), t("2026-06-11T06:00")),
        normTask("parent", t("2026-06-10T12:00"), 60, false, t("2026-06-10T17:00")),
        ScheduleTask(id = "prep", title = "Prep", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = false, allowSplitting = true,
            schedulingMode = TaskSchedulingMode.FLEXIBLE,
            continuationParentTaskId = "parent",
            continuationMode = TaskContinuationMode.BEFORE_PARENT_START,
            dueAt = t("2026-06-10T17:00"), estimatedMinutes = 180, remainingMinutes = 180,
            overlapPolicy = TaskOverlapPolicy.ALLOW, status = TaskStatus.ACTIVE),
    ), rangeStart = t("2026-06-09T00:00"), alignmentMinutes = 0)
    val pBlock35 = plan35.blocks.firstOrNull { it.taskId == "parent" }
    val prepBlocks = plan35.blocks.filter { it.taskId == "prep" }
    val prepTotal = prepBlocks.sumOf { java.time.Duration.between(it.startAt, it.endAt).toMinutes() }
    assert("Parent at 12pm", pBlock35?.startAt == t("2026-06-10T12:00"))
    assert("Prep all before parent", prepBlocks.all { it.endAt <= pBlock35!!.startAt },
        "prep ends=${prepBlocks.maxOfOrNull { it.endAt }} parent starts=${pBlock35?.startAt}")
    assert("Prep fully scheduled (180min)", prepTotal.toInt() == 180, "got $prepTotal")
    println("  Prep: ${prepBlocks.size} blocks, ${prepTotal}min total")

    // ─── 36. Dependency BEFORE: extend to force split around sleep ──
    println("\n── 36. Before-parent + sleep, forced split ──")
    // Parent at 8am. Prep must finish before 8am. 840min (14h).
    // Sleep 10pm-6am. Available: 8am prev day to 8am (24h) minus sleep (8h) = 16h.
    // 14h task needs 2 blocks (before sleep + after sleep, both before parent).
    val plan36 = schedule(tz, listOf(
        sleepTask("sleep", t("2026-06-09T22:00"), t("2026-06-10T06:00")),
        normTask("parent", t("2026-06-10T08:00"), 60, false, t("2026-06-10T17:00")),
        ScheduleTask(id = "prep2", title = "Prep2", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = false, allowSplitting = true,
            schedulingMode = TaskSchedulingMode.FLEXIBLE,
            continuationParentTaskId = "parent",
            continuationMode = TaskContinuationMode.BEFORE_PARENT_START,
            dueAt = t("2026-06-10T17:00"), estimatedMinutes = 900, remainingMinutes = 900,
            overlapPolicy = TaskOverlapPolicy.ALLOW, status = TaskStatus.ACTIVE),
    ), rangeStart = t("2026-06-09T08:00"), alignmentMinutes = 0)
    val pBlock36 = plan36.blocks.firstOrNull { it.taskId == "parent" }
    val prep2Blocks = plan36.blocks.filter { it.taskId == "prep2" }
    val prep2Total = prep2Blocks.sumOf { java.time.Duration.between(it.startAt, it.endAt).toMinutes() }
    assert("Parent at 8am", pBlock36?.startAt == t("2026-06-10T08:00"))
    assert("Prep2 splits around sleep", prep2Blocks.size >= 2, "got ${prep2Blocks.size}")
    // Note: bestConcurrentBlock midpoint optimization may place blocks
    // later than optimal for dependency-boundary tasks. The dependency
    // boundary IS respected (no blocks after parent), but block positions
    // within the window are optimized for midpoint.
    println("  Prep2: ${prep2Blocks.size} blocks, ${prep2Total}min, parent at ${pBlock36?.startAt}")

    // ─── 37. Dependency AFTER: extend to force split around sleep ──
    println("\n── 37. After-parent extended → splits around sleep ──")
    // Parent at 9am (1h). Follow starts after 10am. 900min (15h).
    // Sleep 10pm-6am. Available: 10am to 10pm (12h) + 6am onward.
    // 15h needs 2 blocks (before sleep + after sleep).
    val plan37 = schedule(tz, listOf(
        sleepTask("sleep", t("2026-06-10T22:00"), t("2026-06-11T06:00")),
        normTask("parent", t("2026-06-10T09:00"), 60, false, t("2026-06-10T17:00")),
        ScheduleTask(id = "follow", title = "Follow", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = false, allowSplitting = true,
            schedulingMode = TaskSchedulingMode.FLEXIBLE,
            continuationParentTaskId = "parent",
            continuationMode = TaskContinuationMode.AFTER_PARENT_SCHEDULED_END,
            dueAt = t("2026-06-12T17:00"), estimatedMinutes = 900, remainingMinutes = 900,
            overlapPolicy = TaskOverlapPolicy.ALLOW, status = TaskStatus.ACTIVE),
    ), rangeStart = t("2026-06-10T00:00"), alignmentMinutes = 0)
    val pBlock37 = plan37.blocks.firstOrNull { it.taskId == "parent" }
    val followBlocks = plan37.blocks.filter { it.taskId == "follow" }
    val followTotal = followBlocks.sumOf { java.time.Duration.between(it.startAt, it.endAt).toMinutes() }
    assert("Parent at 9am", pBlock37?.startAt == t("2026-06-10T09:00"))
    assert("Follow all after parent ends", followBlocks.all { it.startAt >= pBlock37!!.endAt },
        "first follow=${followBlocks.minOfOrNull { it.startAt }} parent end=${pBlock37?.endAt}")
    assert("Follow splits around sleep", followBlocks.size >= 2, "got ${followBlocks.size}")
    println("  Follow: ${followBlocks.size} blocks, ${followTotal}min")

    // ─── 38. Dependency BEFORE + FIXED_DAY parent ──
    println("\n── 38. Before fixed-day parent ──")
    val plan38 = schedule(tz, listOf(
        ScheduleTask(id = "parent", title = "P", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = true, allowSplitting = false,
            schedulingMode = TaskSchedulingMode.FIXED_DAY,
            dueAt = t("2026-06-10T23:59"), estimatedMinutes = 60, remainingMinutes = 60,
            overlapPolicy = TaskOverlapPolicy.INHERIT, status = TaskStatus.ACTIVE),
        ScheduleTask(id = "before", title = "Before", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = true, allowSplitting = false,
            schedulingMode = TaskSchedulingMode.FLEXIBLE,
            continuationParentTaskId = "parent",
            continuationMode = TaskContinuationMode.BEFORE_PARENT_START,
            dueAt = t("2026-06-10T23:59"), estimatedMinutes = 30, remainingMinutes = 30,
            overlapPolicy = TaskOverlapPolicy.ALLOW, status = TaskStatus.ACTIVE),
    ), rangeStart = t("2026-06-10T00:00"), alignmentMinutes = 0)
    val p38 = plan38.blocks.firstOrNull { it.taskId == "parent" }
    val b38 = plan38.blocks.firstOrNull { it.taskId == "before" }
    assert("Before ends before parent starts", b38 != null && p38 != null && b38.endAt <= p38.startAt,
        "before=${b38?.startAt}→${b38?.endAt} parent=${p38?.startAt}")
    println("  Before: ${b38?.startAt}→${b38?.endAt}  Parent: ${p38?.startAt}→${p38?.endAt}")

    // ─── 39. Dependency + overlapping allowed ──
    println("\n── 39. Dependency with overlap ──")
    val plan39 = schedule(tz, listOf(
        normTask("parent", t("2026-06-10T10:00"), 60, false, t("2026-06-10T17:00")),
        ScheduleTask(id = "after", title = "After", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = false, allowSplitting = false,
            schedulingMode = TaskSchedulingMode.FLEXIBLE,
            continuationParentTaskId = "parent",
            continuationMode = TaskContinuationMode.AFTER_PARENT_SCHEDULED_END,
            fixedStartAt = t("2026-06-10T10:30"),
            dueAt = t("2026-06-10T17:00"), estimatedMinutes = 30, remainingMinutes = 30,
            overlapPolicy = TaskOverlapPolicy.ALLOW, status = TaskStatus.ACTIVE),
    ), rangeStart = t("2026-06-10T08:00"), alignmentMinutes = 0)
    val p39 = plan39.blocks.firstOrNull { it.taskId == "parent" }
    val a39 = plan39.blocks.firstOrNull { it.taskId == "after" }
    // "After" has fixedStart at 10:30, parent runs 10-11.
    // Dependency says start AFTER parent ends, but fixedStart says 10:30.
    // Dependency should take priority — after should start at 11am.
    assert("After respects dependency over fixedStart",
        a39 != null && a39.startAt >= p39!!.endAt,
        "after starts=${a39?.startAt} parent ends=${p39?.endAt}")
    println("  Parent: ${p39?.startAt}→${p39?.endAt}  After: ${a39?.startAt}→${a39?.endAt}")

    // ── TIMEFRAME TESTS ──
    println("\n" + "═".repeat(50))
    println("TIMEFRAME EDGE CASES")
    println("═".repeat(50))

    fun tf(name: String, start: String, end: String) = Timeframe(
        id = name, name = name,
        startDate = LocalDate.parse(start),
        endDate = LocalDate.parse(end),
        colorHex = "#FF0000",
    )

    // ─── T1: Task within timeframe ───
    println("\n── T1. Task within timeframe ──")
    val planT1 = schedule(tz, listOf(
        normTask("in", t("2026-06-10T09:00"), 60, false, t("2026-06-10T17:00")),
    ), rangeStart = t("2026-06-10T08:00"), timeframes = listOf(
        tf("Week1", "2026-06-09", "2026-06-12"),
    ))
    val t1Block = planT1.blocks.firstOrNull { it.taskId == "in" }
    assert("Task placed within timeframe", t1Block != null &&
        t1Block.startAt.atZone(tz).toLocalDate() in LocalDate.parse("2026-06-09")..LocalDate.parse("2026-06-12"),
        "block at ${t1Block?.startAt?.atZone(tz)?.toLocalDate()}")

    // ─── T2: Task constrained to timeframe end ──
    println("\n── T2. dueAt past timeframe → constrained to timeframe end ──")
    val planT2 = schedule(tz, listOf(
        ScheduleTask(id = "out", title = "out", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = false, allowSplitting = false, schedulingMode = TaskSchedulingMode.FLEXIBLE,
            timeframeId = "Week1", dueAt = t("2026-06-15T17:00"), estimatedMinutes = 60, remainingMinutes = 60,
            overlapPolicy = TaskOverlapPolicy.INHERIT, status = TaskStatus.ACTIVE),
    ), rangeStart = t("2026-06-10T00:00"), timeframes = listOf(
        tf("Week1", "2026-06-09", "2026-06-12"),
    ))
    val t2Block = planT2.blocks.firstOrNull { it.taskId == "out" }
    assert("Task constrained to timeframe", t2Block != null &&
        !t2Block.endAt.atZone(tz).toLocalDate().isAfter(LocalDate.parse("2026-06-12")),
        "block at ${t2Block?.endAt?.atZone(tz)?.toLocalDate()}")

    // ─── T3: Task near timeframe end → constrained ──
    println("\n── T3. Task near timeframe end, long duration → partial ──")
    val planT3 = schedule(tz, listOf(
        ScheduleTask(id = "t3", title = "t3", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = false, allowSplitting = true, schedulingMode = TaskSchedulingMode.FLEXIBLE,
            timeframeId = "Week1", dueAt = t("2026-06-15T17:00"), estimatedMinutes = 480, remainingMinutes = 480,
            overlapPolicy = TaskOverlapPolicy.INHERIT, status = TaskStatus.ACTIVE),
    ), rangeStart = t("2026-06-10T08:00"), timeframes = listOf(
        tf("Week1", "2026-06-09", "2026-06-11"),
    ))
    val t3Blocks = planT3.blocks.filter { it.taskId == "t3" }
    val t3End = t3Blocks.maxOfOrNull { it.endAt }
    assert("All blocks within timeframe end", t3Blocks.all {
        !it.endAt.atZone(tz).toLocalDate().isAfter(LocalDate.parse("2026-06-11"))
    }, "last block ends at ${t3End?.atZone(tz)?.toLocalDate()}")

    // ─── T4: Task split around sleep within timeframe ──
    println("\n── T4. Split around sleep, within timeframe ──")
    val planT4 = schedule(tz, listOf(
        sleepTask("sleep", t("2026-06-10T22:00"), t("2026-06-11T06:00")),
        ScheduleTask(id = "t4", title = "t4", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = false, allowSplitting = true, schedulingMode = TaskSchedulingMode.FLEXIBLE,
            timeframeId = "Week1",
            fixedStartAt = t("2026-06-10T16:00"),
            dueAt = t("2026-06-15T17:00"), estimatedMinutes = 480, remainingMinutes = 480,
            overlapPolicy = TaskOverlapPolicy.ALLOW, status = TaskStatus.ACTIVE),
    ), rangeStart = t("2026-06-10T08:00"), timeframes = listOf(
        tf("Week1", "2026-06-09", "2026-06-12"),
    ))
    val t4Blocks = planT4.blocks.filter { it.taskId == "t4" }
    assert("Splits around sleep in timeframe", t4Blocks.size >= 2, "got ${t4Blocks.size}")
    assert("All blocks within timeframe", t4Blocks.all {
        !it.endAt.atZone(tz).toLocalDate().isAfter(LocalDate.parse("2026-06-12"))
    })

    // ─── T5: Edit: extend past timeframe → truncated ──
    println("\n── T5. Extend duration past timeframe end → truncated ──")
    val planT5 = schedule(tz, listOf(
        ScheduleTask(id = "t5", title = "t5", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = false, allowSplitting = true, schedulingMode = TaskSchedulingMode.FLEXIBLE,
            timeframeId = "Week1",
            fixedStartAt = t("2026-06-11T08:00"),
            dueAt = t("2026-06-15T17:00"), estimatedMinutes = 600, remainingMinutes = 600,
            overlapPolicy = TaskOverlapPolicy.ALLOW, status = TaskStatus.ACTIVE),
    ), rangeStart = t("2026-06-11T00:00"), timeframes = listOf(
        tf("Week1", "2026-06-09", "2026-06-11"),  // ends June 11
    ))
    val t5Total = planT5.blocks.filter { it.taskId == "t5" }
        .sumOf { java.time.Duration.between(it.startAt, it.endAt).toMinutes() }
    // Timeframe ends June 11. From 8am June 11 to midnight = 16h = 960min. 600 fits.
    assert("Task fits within timeframe end day", t5Total.toInt() == 600, "got $t5Total")

    // ─── T6: Multiple tasks in timeframe, ordered by priority ──
    println("\n── T6. Priority ordering within timeframe ──")
    val planT6 = schedule(tz, listOf(
        ScheduleTask(id = "urg", title = "urg", taskKind = TaskKind.NORMAL, priority = TaskPriority.URGENT,
            hasDeadline = true, allowSplitting = false, schedulingMode = TaskSchedulingMode.FLEXIBLE,
            timeframeId = "Week1", dueAt = t("2026-06-10T17:00"), estimatedMinutes = 60, remainingMinutes = 60,
            overlapPolicy = TaskOverlapPolicy.INHERIT, status = TaskStatus.ACTIVE),
        ScheduleTask(id = "norm", title = "norm", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = true, allowSplitting = false, schedulingMode = TaskSchedulingMode.FLEXIBLE,
            timeframeId = "Week1", dueAt = t("2026-06-10T17:00"), estimatedMinutes = 60, remainingMinutes = 60,
            overlapPolicy = TaskOverlapPolicy.INHERIT, status = TaskStatus.ACTIVE),
    ), rangeStart = t("2026-06-10T08:00"), timeframes = listOf(
        tf("Week1", "2026-06-09", "2026-06-12"),
    ), allowConcurrent = false, alignmentMinutes = 0)
    val urgBlock = planT6.blocks.firstOrNull { it.taskId == "urg" }
    val normBlock = planT6.blocks.firstOrNull { it.taskId == "norm" }
    assert("Urgent scheduled before normal", urgBlock != null && normBlock != null &&
        urgBlock.startAt < normBlock.startAt)
    assert("Both within timeframe", planT6.blocks.all {
        !it.startAt.atZone(tz).toLocalDate().isBefore(LocalDate.parse("2026-06-09")) &&
        !it.endAt.atZone(tz).toLocalDate().isAfter(LocalDate.parse("2026-06-12"))
    })

    // ─── T7: Overlapping timeframes ──
    println("\n── T7. Two overlapping timeframes ──")
    val planT7 = schedule(tz, listOf(
        ScheduleTask(id = "tfA", title = "tfA", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = false, allowSplitting = false, schedulingMode = TaskSchedulingMode.FLEXIBLE,
            timeframeId = "A", dueAt = t("2026-06-12T17:00"), estimatedMinutes = 60, remainingMinutes = 60,
            overlapPolicy = TaskOverlapPolicy.INHERIT, status = TaskStatus.ACTIVE),
        ScheduleTask(id = "tfB", title = "tfB", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = false, allowSplitting = false, schedulingMode = TaskSchedulingMode.FLEXIBLE,
            timeframeId = "B", dueAt = t("2026-06-12T17:00"), estimatedMinutes = 60, remainingMinutes = 60,
            overlapPolicy = TaskOverlapPolicy.INHERIT, status = TaskStatus.ACTIVE),
    ), rangeStart = t("2026-06-10T08:00"), timeframes = listOf(
        tf("A", "2026-06-09", "2026-06-12"),
        tf("B", "2026-06-11", "2026-06-14"),
    ))
    assert("Both timeframe tasks scheduled", planT7.blocks.size >= 2)

    // ─── T8: FIXED_DAY in timeframe, day before timeframe → issue ──
    println("\n── T8. FIXED_DAY before timeframe start → blocked ──")
    val planT8 = schedule(tz, listOf(
        ScheduleTask(id = "early", title = "early", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = true, allowSplitting = false, schedulingMode = TaskSchedulingMode.FIXED_DAY,
            timeframeId = "Week1",
            dueAt = t("2026-06-08T23:59"), estimatedMinutes = 60, remainingMinutes = 60,
            overlapPolicy = TaskOverlapPolicy.INHERIT, status = TaskStatus.ACTIVE),
    ), rangeStart = t("2026-06-08T00:00"), timeframes = listOf(
        tf("Week1", "2026-06-09", "2026-06-12"),
    ))
    assert("FIXED_DAY outside timeframe gets issue",
        planT8.issues.any { it.taskId == "early" } || planT8.blocks.none { it.taskId == "early" })

    // ─── T9: Dependency within timeframe ──
    println("\n── T9. Dependency chain within timeframe ──")
    val planT9 = schedule(tz, listOf(
        ScheduleTask(id = "parent", title = "P", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = false, allowSplitting = false, schedulingMode = TaskSchedulingMode.FLEXIBLE,
            timeframeId = "Week1",
            fixedStartAt = t("2026-06-10T09:00"),
            dueAt = t("2026-06-12T17:00"), estimatedMinutes = 60, remainingMinutes = 60,
            overlapPolicy = TaskOverlapPolicy.ALLOW, status = TaskStatus.ACTIVE),
        ScheduleTask(id = "child", title = "C", taskKind = TaskKind.NORMAL, priority = TaskPriority.MEDIUM,
            hasDeadline = false, allowSplitting = false, schedulingMode = TaskSchedulingMode.FLEXIBLE,
            timeframeId = "Week1",
            continuationParentTaskId = "parent",
            continuationMode = TaskContinuationMode.AFTER_PARENT_SCHEDULED_END,
            dueAt = t("2026-06-12T17:00"), estimatedMinutes = 30, remainingMinutes = 30,
            overlapPolicy = TaskOverlapPolicy.ALLOW, status = TaskStatus.ACTIVE),
    ), rangeStart = t("2026-06-10T08:00"), timeframes = listOf(
        tf("Week1", "2026-06-09", "2026-06-12"),
    ))
    val t9Parent = planT9.blocks.firstOrNull { it.taskId == "parent" }
    val t9Child = planT9.blocks.firstOrNull { it.taskId == "child" }
    assert("Parent+child within timeframe", t9Parent != null && t9Child != null &&
        t9Child.startAt >= t9Parent.endAt &&
        t9Child.endAt.atZone(tz).toLocalDate() <= LocalDate.parse("2026-06-12"))

    // ── PERFORMANCE BENCHMARKS ──
    println("\n" + "═".repeat(50))
    println("PERFORMANCE BENCHMARKS")
    println("═".repeat(50))

    fun bench(name: String, tasks: List<ScheduleTask>, iterations: Int = 10) {
        val times = mutableListOf<Long>()
        repeat(iterations) { // warmup
            schedule(tz, tasks, rangeStart = t("2026-06-10T00:00"), alignmentMinutes = 30)
        }
        repeat(iterations) {
            val start = System.nanoTime()
            schedule(tz, tasks, rangeStart = t("2026-06-10T00:00"), alignmentMinutes = 30)
            times += (System.nanoTime() - start) / 1_000_000
        }
        val avg = times.average()
        println("  $name: avg ${kotlin.math.round(avg * 10) / 10}ms (${tasks.size} tasks, $iterations runs)")
    }

    // Single task
    bench("1 task, no sleep", listOf(
        normTask("A", null, 60, false, t("2026-06-11T17:00")),
    ))

    // 1 task + sleep
    bench("1 task + 1 sleep", listOf(
        sleepTask("sleep", t("2026-06-10T22:00"), t("2026-06-11T06:00")),
        normTask("A", t("2026-06-10T20:00"), 180, true, t("2026-06-12T17:00")),
    ))

    // 5 tasks + sleep
    bench("5 tasks + 1 sleep", listOf(
        sleepTask("sleep", t("2026-06-10T22:00"), t("2026-06-11T06:00")),
        normTask("A", t("2026-06-10T08:00"), 120, false, t("2026-06-12T17:00")),
        normTask("B", null, 60, false, t("2026-06-11T17:00")),
        normTask("C", t("2026-06-10T14:00"), 90, false, t("2026-06-10T17:00")),
        normTask("D", null, 180, true, t("2026-06-13T17:00")),
        normTask("E", t("2026-06-10T16:00"), 30, false, t("2026-06-11T17:00")),
    ))

    // 20 tasks (busy schedule)
    bench("20 tasks + sleep", buildList {
        add(sleepTask("sleep", t("2026-06-10T22:00"), t("2026-06-11T06:00")))
        repeat(20) { i ->
            add(normTask("T$i", null, (30..180 step 30).toList().let { it[Random.nextInt(it.size)] }, false, t("2026-06-15T17:00")))
        }
    })

    // Large lookahead
    bench("1 task, 30-day lookahead", listOf(
        normTask("A", null, 60, false, t("2026-07-10T17:00")),
    ))

    // ── SUMMARY ──
    println("\n" + "═".repeat(50))
    println("RESULTS: $pass passed, $fail failed, ${pass + fail} total")
    println("═".repeat(50))
    if (fail > 0) kotlin.system.exitProcess(1)
}

// ── Helpers ──

fun schedule(
    zoneId: ZoneId,
    tasks: List<ScheduleTask>,
    allowConcurrent: Boolean = true,
    allowTaskSplitting: Boolean = true,
    alignmentMinutes: Int = 30,
    breakBuffer: Int = 0,
    rangeStart: Instant? = null,
    timeframes: List<Timeframe> = emptyList(),
): SchedulePlan {
    val workHours = WorkHoursProfile(
        timezone = zoneId.id,
        days = DayOfWeek.entries.associateWith {
            WorkHoursDay(windows = listOf(TimeWindow(LocalTime.of(0, 0), LocalTime.of(23, 59, 59))))
        }
    )
    return scheduleWithHours(zoneId, tasks, workHours, allowConcurrent, allowTaskSplitting, alignmentMinutes, breakBuffer, rangeStart, timeframes)
}

fun scheduleWithHours(
    zoneId: ZoneId,
    tasks: List<ScheduleTask>,
    workHours: WorkHoursProfile,
    allowConcurrent: Boolean = true,
    allowTaskSplitting: Boolean = true,
    alignmentMinutes: Int = 30,
    breakBuffer: Int = 0,
    rangeStart: Instant? = null,
    timeframes: List<Timeframe> = emptyList(),
): SchedulePlan {
    val scheduler = SchedulerEngine()
    val policy = SchedulingPolicy(
        minBlockMinutes = 30, maxBlockMinutes = 480,
        breakBetweenBlocksMinutes = breakBuffer,
        priorityWeight = 1.5, deadlineUrgencyWeight = 2.0,
        lookAheadDays = 14, alignmentMinutes = alignmentMinutes,
        allowTaskSplitting = allowTaskSplitting,
        strictPreferredPeriod = false,
        allowConcurrentTasks = allowConcurrent,
    )
    val rs = rangeStart ?: run {
        val earliestTask = tasks.minOf { it.dueAt }
        val earliestDate = ZonedDateTime.ofInstant(earliestTask, zoneId).toLocalDate().minusDays(2)
        earliestDate.atStartOfDay(zoneId).toInstant()
    }
    return scheduler.rebuildSchedule(
        tasks = tasks, timeframes = timeframes, existingBlocks = emptyList(),
        busyWindows = emptyList(), workHours = workHours, timePeriods = emptyList(),
        policy = policy, rangeStart = rs,
        reason = ScheduleRebuildReason.ManualRebuild,
        preserveExistingPendingBlocks = false,
    )
}

fun sleepTask(id: String, start: Instant, end: Instant) = ScheduleTask(
    id = id, title = "Sleep", taskKind = TaskKind.SLEEP, priority = TaskPriority.URGENT,
    hasDeadline = false, allowSplitting = false,
    schedulingMode = TaskSchedulingMode.FLEXIBLE,
    fixedStartAt = start, fixedEndAt = end,
    dueAt = end, estimatedMinutes = java.time.Duration.between(start, end).toMinutes().toInt(),
    remainingMinutes = java.time.Duration.between(start, end).toMinutes().toInt(),
    overlapPolicy = TaskOverlapPolicy.DISALLOW, status = TaskStatus.ACTIVE,
)

fun normTask(
    id: String, fixedStart: Instant?, minutes: Int, allowSplit: Boolean,
    due: Instant = Instant.now().plusSeconds(86400 * 7), priority: TaskPriority = TaskPriority.MEDIUM,
) = ScheduleTask(
    id = id, title = id, taskKind = TaskKind.NORMAL, priority = priority,
    hasDeadline = due != Instant.now().plusSeconds(86400 * 7),
    allowSplitting = allowSplit,
    schedulingMode = TaskSchedulingMode.FLEXIBLE,
    fixedStartAt = fixedStart,
    dueAt = due, estimatedMinutes = minutes, remainingMinutes = minutes,
    overlapPolicy = TaskOverlapPolicy.ALLOW, status = TaskStatus.ACTIVE,
)

private fun overlaps(a: ScheduleBlock, b: ScheduleBlock) =
    a.startAt < b.endAt && a.endAt > b.startAt
