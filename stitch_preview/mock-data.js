(function () {
  const STORAGE_KEY = 'focus-preview-periods-v2';
  const TASKS_KEY = 'focus-preview-tasks-v2';
  const REMINDERS_KEY = 'focus-preview-reminders-v3';
  const HIDDEN_TASKS_KEY = 'focus-preview-hidden-tasks-v2';
  const HIDDEN_REMINDERS_KEY = 'focus-preview-hidden-reminders-v2';
  const SNOOZED_REMINDERS_KEY = 'focus-preview-snoozed-reminders-v2';
  const DEFAULT_SCHEDULE_DATE = '2026-10-06';

  const defaultPeriods = [
    { id: 'p1', name: 'Morning', start: '08:00', end: '12:00', type: 'productive' },
    { id: 'p2', name: 'Lunch Break', start: '12:00', end: '13:00', type: 'life' },
    { id: 'p3', name: 'Midday Focus', start: '13:00', end: '14:00', type: 'productive' },
    { id: 'p4', name: 'Afternoon', start: '14:00', end: '17:00', type: 'productive' },
    { id: 'p5', name: 'Rest', start: '17:00', end: '18:00', type: 'life' },
    { id: 'p6', name: 'Dinner', start: '19:00', end: '20:00', type: 'life' },
    { id: 'p7', name: 'Night', start: '20:00', end: '22:00', type: 'productive' },
    { id: 'p8', name: 'Sleep', start: '22:00', end: '06:00', type: 'life' }
  ];

  const defaultTasks = [
    {
      id: 't1',
      title: 'Q3 Review Presentation',
      description: 'Finalize slides 12 to 15 and review financial projections for accuracy.',
      priority: 'urgent',
      dueDate: '2026-10-05',
      dueTime: '10:30',
      preferredPeriod: 'Morning',
      scheduleDate: '2026-10-06',
      repeatMode: 'none',
      repeatDays: [],
      repeatStartDate: '2026-10-06',
      start: '09:00',
      end: '10:30'
    },
    {
      id: 't2',
      title: 'Midday Focus Task',
      description: 'Finish the core focus block before the afternoon handoff.',
      priority: 'normal',
      dueDate: '2026-10-06',
      dueTime: '15:30',
      preferredPeriod: 'Afternoon',
      scheduleDate: '2026-10-06',
      repeatMode: 'none',
      repeatDays: [],
      repeatStartDate: '2026-10-06',
      start: '14:00',
      end: '15:30'
    },
    {
      id: 't3',
      title: 'Take vitamins',
      description: 'Take the evening vitamins before winding down for the night.',
      priority: 'normal',
      dueDate: '2026-10-07',
      dueTime: '18:30',
      preferredPeriod: 'Night',
      scheduleDate: '2026-10-07',
      repeatMode: 'none',
      repeatDays: [],
      repeatStartDate: '2026-10-07',
      start: '20:00',
      end: '21:00'
    }
  ];

  const defaultReminders = [
    { id: 'r1', title: 'Slides follow-up', date: '2026-10-05', time: '11:30', repeatMode: 'none', repeatDays: [], repeatStartDate: '2026-10-05', linkedTaskId: 't1' },
    { id: 'r2', title: 'Prep focus block', date: '2026-10-06', time: '16:30', repeatMode: 'none', repeatDays: [], repeatStartDate: '2026-10-06', linkedTaskId: 't2' },
    { id: 'r3', title: 'Take vitamins', date: '2026-10-07', time: '18:30', repeatMode: 'none', repeatDays: [], repeatStartDate: '2026-10-07', linkedTaskId: 't3' }
  ];

  const DEFAULT_REPEAT_DAY = new Date(`${DEFAULT_SCHEDULE_DATE}T00:00:00`).getDay();

  function toMinutes(time) {
    const [h, m] = time.split(':').map(Number);
    return h * 60 + m;
  }

  function minutesToTime(totalMinutes) {
    const normalized = ((totalMinutes % 1440) + 1440) % 1440;
    const h = String(Math.floor(normalized / 60)).padStart(2, '0');
    const m = String(normalized % 60).padStart(2, '0');
    return `${h}:${m}`;
  }

  function addMinutes(time, minutes) {
    return minutesToTime(toMinutes(time) + minutes);
  }

  function formatTime(time) {
    const [rawH, rawM] = time.split(':').map(Number);
    if (rawH === 24) return `12:${String(rawM).padStart(2, '0')} AM`;
    const suffix = rawH >= 12 ? 'PM' : 'AM';
    const h = rawH % 12 || 12;
    return `${h}:${String(rawM).padStart(2, '0')} ${suffix}`;
  }

  function formatRange(start, end) {
    return `${formatTime(start)} - ${formatTime(end)}`;
  }

  function formatShortDate(dateIso) {
    const date = new Date(`${dateIso}T00:00:00`);
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  }

  function parseDate(dateIso) {
    return new Date(`${dateIso}T00:00:00`);
  }

  function dayOfWeek(dateIso) {
    return parseDate(dateIso).getDay();
  }

  function normalizeRepeatMode(value) {
    return value === 'daily' || value === 'weekly' ? value : 'none';
  }

  function normalizeRepeatDays(value) {
    const source = Array.isArray(value) ? value : [];
    const days = source
      .map((item) => Number(item))
      .filter((item) => Number.isInteger(item) && item >= 0 && item <= 6);
    return [...new Set(days)].sort((a, b) => a - b);
  }

  function occursOnDate(dateIso, repeatMode, repeatDays, repeatStartDate, fallbackDate) {
    const anchor = repeatStartDate || fallbackDate || DEFAULT_SCHEDULE_DATE;
    if (dateIso < anchor) return false;
    if (repeatMode === 'daily') return true;
    if (repeatMode === 'weekly') return repeatDays.includes(dayOfWeek(dateIso));
    return dateIso === (fallbackDate || anchor);
  }

  function nextOccurrenceOnOrAfter(referenceDateIso, repeatMode, repeatDays, repeatStartDate, fallbackDate) {
    const anchor = repeatStartDate || fallbackDate || DEFAULT_SCHEDULE_DATE;
    if (repeatMode === 'none') {
      const dueDate = fallbackDate || anchor;
      return dueDate >= referenceDateIso ? dueDate : null;
    }
    const start = referenceDateIso > anchor ? referenceDateIso : anchor;
    for (let offset = 0; offset < 30; offset += 1) {
      const candidate = addDays(start, offset);
      if (occursOnDate(candidate, repeatMode, repeatDays, anchor, fallbackDate)) {
        return candidate;
      }
    }
    return null;
  }

  function normalizePeriods(periods) {
    return periods.map((period) => period.name === 'Sleep'
      ? { ...period, start: '22:00', end: '06:00', type: 'life' }
      : period);
  }

  function productivePeriodsFrom(periods) {
    return periods.filter((period) => period.type === 'productive');
  }

  function apiRequest(method, pathname, body) {
    const request = new XMLHttpRequest();
    request.open(method, pathname, false);
    request.setRequestHeader('Accept', 'application/json');
    if (body !== undefined) {
      request.setRequestHeader('Content-Type', 'application/json');
    }
    request.send(body === undefined ? null : JSON.stringify(body));
    if (request.status < 200 || request.status >= 300) {
      throw new Error(`API request failed: ${method} ${pathname} (${request.status})`);
    }
    return request.responseText ? JSON.parse(request.responseText) : null;
  }

  function loadState() {
    const state = apiRequest('GET', '/api/state');
    const periods = normalizePeriods(Array.isArray(state.periods) ? state.periods : defaultPeriods);
    return {
      defaultScheduleDate: state.defaultScheduleDate || DEFAULT_SCHEDULE_DATE,
      periods,
      tasks: Array.isArray(state.tasks) ? state.tasks.map((task) => normalizeTask(task, periods)) : defaultTasks.map((task) => normalizeTask(task, periods)),
      reminders: Array.isArray(state.reminders) ? state.reminders.map((reminder) => normalizeReminder(reminder)) : defaultReminders.map((reminder) => normalizeReminder(reminder)),
      uiState: {
        hiddenTaskIds: Array.isArray(state.uiState?.hiddenTaskIds) ? state.uiState.hiddenTaskIds : [],
        hiddenReminderIds: Array.isArray(state.uiState?.hiddenReminderIds) ? state.uiState.hiddenReminderIds : [],
        snoozedReminders: state.uiState?.snoozedReminders && typeof state.uiState.snoozedReminders === 'object'
          ? state.uiState.snoozedReminders
          : {}
      }
    };
  }

  function saveState(state) {
    return apiRequest('PUT', '/api/state', {
      defaultScheduleDate: state.defaultScheduleDate || DEFAULT_SCHEDULE_DATE,
      periods: normalizePeriods(state.periods || []),
      tasks: (state.tasks || []).map((task) => normalizeTask(task)),
      reminders: (state.reminders || []).map((reminder) => normalizeReminder(reminder)),
      uiState: {
        hiddenTaskIds: Array.isArray(state.uiState?.hiddenTaskIds) ? state.uiState.hiddenTaskIds : [],
        hiddenReminderIds: Array.isArray(state.uiState?.hiddenReminderIds) ? state.uiState.hiddenReminderIds : [],
        snoozedReminders: state.uiState?.snoozedReminders && typeof state.uiState.snoozedReminders === 'object'
          ? state.uiState.snoozedReminders
          : {}
      }
    });
  }

  function loadPeriods() {
    return loadState().periods;
  }

  function savePeriods(periods) {
    const state = loadState();
    state.periods = normalizePeriods(periods);
    saveState(state);
  }

  function upsertPeriod(period) {
    const periods = loadPeriods();
    const normalized = { ...period, id: period.id || `p${Date.now()}` };
    const withoutExisting = periods.filter((item) => item.id !== normalized.id);
    const next = [...withoutExisting, normalized]
      .sort((a, b) => toMinutes(a.start) - toMinutes(b.start));
    savePeriods(next);
    return next;
  }

  function deletePeriod(periodId) {
    const next = loadPeriods().filter((period) => period.id !== periodId);
    savePeriods(next);
    return next;
  }

  function productivePeriods() {
    return productivePeriodsFrom(loadPeriods());
  }

  function periodByName(name, periodList = loadPeriods()) {
    return periodList.find((period) => period.name === name);
  }

  function clampTaskDuration(start, end) {
    const startMinutes = toMinutes(start);
    const endMinutes = end === '24:00' ? 1440 : toMinutes(end);
    return minutesToTime(Math.min(startMinutes + 90, endMinutes));
  }

  function inferDurationMinutes(start, end) {
    if (!start || !end) return 60;
    const startMinutes = toMinutes(start);
    let endMinutes = end === '24:00' ? 1440 : toMinutes(end);
    if (endMinutes <= startMinutes) {
      endMinutes += 1440;
    }
    return Math.max(15, endMinutes - startMinutes);
  }

  function normalizeDurationMinutes(value, start, end) {
    const numeric = Number(value);
    if (Number.isFinite(numeric) && numeric > 0) {
      return Math.max(15, Math.round(numeric));
    }
    return inferDurationMinutes(start, end);
  }

  function computeTaskEnd(start, periodEnd, durationMinutes) {
    const startMinutes = toMinutes(start);
    let periodEndMinutes = periodEnd === '24:00' ? 1440 : toMinutes(periodEnd);
    if (periodEndMinutes <= startMinutes) {
      periodEndMinutes = startMinutes + durationMinutes;
    }
    return minutesToTime(Math.min(startMinutes + durationMinutes, periodEndMinutes));
  }

  function endMinutesForTime(time) {
    return time === '24:00' ? 1440 : toMinutes(time);
  }

  function productiveSchedulingOrder(preferredPeriod, periodList = loadPeriods()) {
    const productive = productivePeriodsFrom(periodList);
    const preferredIndex = productive.findIndex((period) => period.name === preferredPeriod);
    if (preferredIndex <= 0) return productive;
    return [...productive.slice(preferredIndex), ...productive.slice(0, preferredIndex)];
  }

  function scheduleTaskSlot(task, periodList = loadPeriods(), existingTasks = loadTasks()) {
    if (task.start) return task;

    const preferredPeriod = task.preferredPeriod || 'Afternoon';
    const durationMinutes = normalizeDurationMinutes(task.durationMinutes, task.start, task.end);
    const scheduleDate = task.scheduleDate || task.dueDate || DEFAULT_SCHEDULE_DATE;
    const candidatePeriods = productiveSchedulingOrder(preferredPeriod, periodList);
    const sameDayTasks = existingTasks
      .filter((item) => (item.scheduleDate || item.dueDate || DEFAULT_SCHEDULE_DATE) === scheduleDate)
      .sort((a, b) => toMinutes(a.start) - toMinutes(b.start));

    for (const period of candidatePeriods) {
      const periodStartMinutes = toMinutes(period.start);
      const periodEndMinutes = endMinutesForTime(period.end);
      let cursor = periodStartMinutes;
      const blockers = sameDayTasks
        .filter((item) => {
          const itemStart = toMinutes(item.start);
          const itemEnd = endMinutesForTime(item.end);
          return itemEnd > periodStartMinutes && itemStart < periodEndMinutes;
        })
        .sort((a, b) => toMinutes(a.start) - toMinutes(b.start));

      for (const blocker of blockers) {
        const blockerStart = toMinutes(blocker.start);
        const blockerEnd = endMinutesForTime(blocker.end);
        if (cursor + durationMinutes <= blockerStart) {
          return {
            ...task,
            preferredPeriod: period.name,
            scheduleDate,
            start: minutesToTime(cursor),
            end: minutesToTime(cursor + durationMinutes)
          };
        }
        cursor = Math.max(cursor, blockerEnd);
      }

      if (cursor + durationMinutes <= periodEndMinutes) {
        return {
          ...task,
          preferredPeriod: period.name,
          scheduleDate,
          start: minutesToTime(cursor),
          end: minutesToTime(cursor + durationMinutes)
        };
      }
    }

    const fallbackPeriod = candidatePeriods[0] || { name: preferredPeriod, start: '09:00', end: '10:30' };
    const fallbackStart = fallbackPeriod.start;
    return {
      ...task,
      preferredPeriod: fallbackPeriod.name,
      scheduleDate,
      start: fallbackStart,
      end: computeTaskEnd(fallbackStart, fallbackPeriod.end, durationMinutes)
    };
  }

  function normalizeTask(task, periodList = loadPeriods()) {
    const preferredPeriod = task.preferredPeriod || 'Afternoon';
    const productive = productivePeriodsFrom(periodList);
    const period = periodByName(preferredPeriod, periodList) || productive[0] || { start: '09:00', end: '10:30', name: preferredPeriod };
    const start = task.start || period.start;
    const durationMinutes = normalizeDurationMinutes(task.durationMinutes, start, task.end);
    const end = task.end && task.durationMinutes == null
      ? task.end
      : computeTaskEnd(start, period.end, durationMinutes);
    const repeatMode = normalizeRepeatMode(task.repeatMode);
    const repeatDays = repeatMode === 'weekly'
      ? normalizeRepeatDays(task.repeatDays).length ? normalizeRepeatDays(task.repeatDays) : [DEFAULT_REPEAT_DAY]
      : [];
    const repeatStartDate = task.repeatStartDate || task.scheduleDate || task.dueDate || DEFAULT_SCHEDULE_DATE;
    return {
      id: task.id || `t${Date.now()}`,
      title: task.title || 'New task',
      description: task.description || '',
      priority: task.priority === 'urgent' ? 'urgent' : 'normal',
      dueDate: task.dueDate || DEFAULT_SCHEDULE_DATE,
      dueTime: task.dueTime || end,
      preferredPeriod,
      durationMinutes,
      scheduleDate: task.scheduleDate || task.dueDate || DEFAULT_SCHEDULE_DATE,
      repeatMode,
      repeatDays,
      repeatStartDate,
      start,
      end
    };
  }

  function normalizeReminder(reminder) {
    const repeatMode = normalizeRepeatMode(reminder.repeatMode);
    const repeatDays = repeatMode === 'weekly'
      ? normalizeRepeatDays(reminder.repeatDays).length ? normalizeRepeatDays(reminder.repeatDays) : [DEFAULT_REPEAT_DAY]
      : [];
    return {
      id: reminder.id || `r${Date.now()}`,
      title: reminder.title || 'Reminder',
      description: reminder.description || '',
      date: reminder.date || DEFAULT_SCHEDULE_DATE,
      time: reminder.time || '09:00',
      repeatMode,
      repeatDays,
      repeatStartDate: reminder.repeatStartDate || reminder.date || DEFAULT_SCHEDULE_DATE,
      linkedTaskId: reminder.linkedTaskId || null
    };
  }

  function loadTasks() {
    return loadState().tasks;
  }

  function saveTasks(tasks) {
    const state = loadState();
    state.tasks = tasks.map((task) => normalizeTask(task));
    saveState(state);
  }

  function addTask(task) {
    const periods = loadPeriods();
    const scheduledTask = scheduleTaskSlot(task, periods, loadTasks());
    const nextTask = normalizeTask(scheduledTask, periods);
    saveTasks([...loadTasks(), nextTask]);
    return nextTask;
  }

  function updateTask(taskId, updates) {
    const tasks = loadTasks();
    const existingTask = tasks.find((task) => task.id === taskId);
    if (!existingTask) return null;
    const nextTask = normalizeTask({ ...existingTask, ...updates });
    saveTasks(tasks.map((task) => task.id === taskId ? nextTask : task));
    return nextTask;
  }

  function loadReminders() {
    return loadState().reminders;
  }

  function saveReminders(reminders) {
    const state = loadState();
    state.reminders = reminders.map((reminder) => normalizeReminder(reminder));
    saveState(state);
  }

  function addReminder(reminder) {
    const nextReminder = normalizeReminder(reminder);
    saveReminders([...loadReminders(), nextReminder]);
    return nextReminder;
  }

  function syncLinkedReminderDates(taskId, updates = {}) {
    const reminders = loadReminders();
    const nextReminders = reminders.map((reminder) => {
      if (reminder.linkedTaskId !== taskId) return reminder;
      return normalizeReminder({
        ...reminder,
        date: updates.date || reminder.date,
        time: updates.time || reminder.time,
        repeatStartDate: updates.date || reminder.repeatStartDate || reminder.date
      });
    });
    saveReminders(nextReminders);
  }

  function loadJsonArrayFromSession(key) {
    const state = loadState();
    if (key === HIDDEN_TASKS_KEY) return state.uiState.hiddenTaskIds;
    if (key === HIDDEN_REMINDERS_KEY) return state.uiState.hiddenReminderIds;
    return [];
  }

  function saveJsonArrayToSession(key, values) {
    const state = loadState();
    if (key === HIDDEN_TASKS_KEY) {
      state.uiState.hiddenTaskIds = values;
    }
    if (key === HIDDEN_REMINDERS_KEY) {
      state.uiState.hiddenReminderIds = values;
    }
    saveState(state);
  }

  function loadJsonObjectFromSession(key) {
    const state = loadState();
    if (key === SNOOZED_REMINDERS_KEY) {
      return state.uiState.snoozedReminders;
    }
    return {};
  }

  function loadHiddenTaskIds() {
    return loadJsonArrayFromSession(HIDDEN_TASKS_KEY);
  }

  function loadHiddenReminderIds() {
    return loadJsonArrayFromSession(HIDDEN_REMINDERS_KEY);
  }

  function hideReminder(reminderId) {
    const ids = new Set(loadHiddenReminderIds());
    ids.add(reminderId);
    saveJsonArrayToSession(HIDDEN_REMINDERS_KEY, [...ids]);
  }

  function hideTask(taskId) {
    const ids = new Set(loadHiddenTaskIds());
    ids.add(taskId);
    saveJsonArrayToSession(HIDDEN_TASKS_KEY, [...ids]);
    linkedRemindersForTask(taskId).forEach((reminder) => hideReminder(reminder.id));
  }

  function loadSnoozedReminders() {
    return loadJsonObjectFromSession(SNOOZED_REMINDERS_KEY);
  }

  function saveSnoozedReminders(value) {
    const state = loadState();
    state.uiState.snoozedReminders = value;
    saveState(state);
  }

  function dateParts(dateIso) {
    const [year, month, day] = dateIso.split('-').map(Number);
    return { year, month, day };
  }

  function addDays(dateIso, amount) {
    const { year, month, day } = dateParts(dateIso);
    const next = new Date(year, month - 1, day);
    next.setDate(next.getDate() + amount);
    const y = next.getFullYear();
    const m = String(next.getMonth() + 1).padStart(2, '0');
    const d = String(next.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }

  function linkedRemindersForTask(taskId) {
    const hiddenReminderIds = new Set(loadHiddenReminderIds());
    return loadReminders().filter((reminder) => reminder.linkedTaskId === taskId && !hiddenReminderIds.has(reminder.id));
  }

  function reminderStatus(reminder) {
    const snoozed = loadSnoozedReminders();
    return {
      dueDate: snoozed[reminder.id]?.date || reminder.date,
      dueTime: snoozed[reminder.id]?.time || reminder.time
    };
  }

  function remindersForDate(dateIso) {
    const hiddenReminderIds = new Set(loadHiddenReminderIds());
    const hiddenTaskIds = new Set(loadHiddenTaskIds());

    return loadReminders()
      .filter((reminder) => !hiddenReminderIds.has(reminder.id))
      .filter((reminder) => !reminder.linkedTaskId || !hiddenTaskIds.has(reminder.linkedTaskId))
      .flatMap((reminder) => {
        const status = reminderStatus(reminder);
        if (!occursOnDate(
          dateIso,
          reminder.repeatMode,
          reminder.repeatDays,
          reminder.repeatStartDate,
          status.dueDate
        )) return [];
        return [{
          kind: 'reminder',
          id: reminder.id,
          title: reminder.title,
          description: reminder.description || '',
          linkedTaskId: reminder.linkedTaskId,
          start: status.dueTime,
          end: addMinutes(status.dueTime, 30),
          displayTime: formatTime(status.dueTime),
          date: status.dueDate
        }];
      })
      .sort((a, b) => toMinutes(a.start) - toMinutes(b.start));
  }

  function tasksForDate(dateIso) {
    const hiddenTaskIds = new Set(loadHiddenTaskIds());
    return loadTasks()
      .filter((task) => !hiddenTaskIds.has(task.id))
      .filter((task) => occursOnDate(
        dateIso,
        task.repeatMode,
        task.repeatDays,
        task.repeatStartDate,
        task.scheduleDate
      ))
      .sort((a, b) => toMinutes(a.start) - toMinutes(b.start))
      .map((task) => ({ kind: 'task', ...task }));
  }

  function taskCountForDate(dateIso) {
    return tasksForDate(dateIso).length;
  }

  function taskAttentionSections(dateIso = DEFAULT_SCHEDULE_DATE) {
    const hiddenTaskIds = new Set(loadHiddenTaskIds());
    const dueTasks = loadTasks()
      .filter((task) => !hiddenTaskIds.has(task.id))
      .map((task) => ({
        kind: 'task',
        id: task.id,
        title: task.title,
        priority: task.priority,
        dueDate: task.dueDate || task.scheduleDate || DEFAULT_SCHEDULE_DATE,
        dueTime: task.dueTime || task.end || '23:59',
        reminderCount: linkedRemindersForTask(task.id).length
      }))
      .sort((a, b) => {
        if (a.dueDate === b.dueDate) return toMinutes(a.dueTime) - toMinutes(b.dueTime);
        return a.dueDate.localeCompare(b.dueDate);
      });

    const sections = { Overdue: [], Today: [], Upcoming: [] };
    dueTasks.forEach((task) => {
      if (task.dueDate < dateIso) sections.Overdue.push(task);
      else if (task.dueDate === dateIso) sections.Today.push(task);
      else sections.Upcoming.push(task);
    });

    return Object.entries(sections)
      .filter(([, items]) => items.length)
      .map(([label, items]) => ({ label, items }));
  }

  function reminderRepeatNote(reminder, dueDate) {
    if (reminder.repeatMode === 'daily') {
      return 'Repeats daily';
    }
    if (reminder.repeatMode === 'weekly') {
      const currentDay = dayOfWeek(dueDate);
      const others = reminder.repeatDays
        .filter((day) => day !== currentDay)
        .map((day) => weekdayNames(day));
      if (others.length) {
        return `Also repeats ${others.join(', ')}`;
      }
      return 'Repeats weekly';
    }
    return '';
  }

  function weekdayNames(dayIndex) {
    return ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'][dayIndex] || '';
  }

  function reminderSections(dateIso = DEFAULT_SCHEDULE_DATE) {
    const hiddenReminderIds = new Set(loadHiddenReminderIds());
    const hiddenTaskIds = new Set(loadHiddenTaskIds());

    const items = loadReminders()
      .filter((reminder) => !hiddenReminderIds.has(reminder.id))
      .filter((reminder) => !reminder.linkedTaskId || !hiddenTaskIds.has(reminder.linkedTaskId))
      .map((reminder) => {
        const status = reminderStatus(reminder);
        const dueDate = reminder.repeatMode === 'none'
          ? status.dueDate
          : nextOccurrenceOnOrAfter(
            dateIso,
            reminder.repeatMode,
            reminder.repeatDays,
            reminder.repeatStartDate,
            status.dueDate
          );
        if (!dueDate) return null;
        return {
          kind: 'reminder',
          id: reminder.id,
          title: reminder.title,
          dueDate,
          dueTime: status.dueTime,
          repeatNote: reminderRepeatNote(reminder, dueDate),
          linkedTaskId: reminder.linkedTaskId
        };
      })
      .filter(Boolean)
      .sort((a, b) => {
        if (a.dueDate === b.dueDate) return toMinutes(a.dueTime) - toMinutes(b.dueTime);
        return a.dueDate.localeCompare(b.dueDate);
      });

    const sections = { Overdue: [], Today: [], Upcoming: [] };
    items.forEach((item) => {
      if (item.dueDate < dateIso) sections.Overdue.push(item);
      else if (item.dueDate === dateIso) sections.Today.push(item);
      else sections.Upcoming.push(item);
    });

    return Object.entries(sections)
      .filter(([, sectionItems]) => sectionItems.length)
      .map(([label, sectionItems]) => ({ label, items: sectionItems }));
  }

  function createReminderForTask(taskId, reminderFields = {}) {
    const task = loadTasks().find((item) => item.id === taskId);
    if (!task) return null;
    return addReminder({
      title: reminderFields.title || task.title,
      description: reminderFields.description || task.description || '',
      date: reminderFields.date || task.dueDate || task.scheduleDate || DEFAULT_SCHEDULE_DATE,
      time: reminderFields.time || task.dueTime || task.end || '09:00',
      repeatMode: reminderFields.repeatMode || task.repeatMode || 'none',
      repeatDays: reminderFields.repeatDays || task.repeatDays || [],
      repeatStartDate: reminderFields.repeatStartDate || task.repeatStartDate || task.scheduleDate || DEFAULT_SCHEDULE_DATE,
      linkedTaskId: taskId
    });
  }

  function promoteReminderToTask(reminderId, taskFields = {}) {
    const reminders = loadReminders();
    const reminder = reminders.find((item) => item.id === reminderId);
    if (!reminder) return null;
    if (reminder.linkedTaskId) {
      return loadTasks().find((task) => task.id === reminder.linkedTaskId) || null;
    }
    const task = addTask({
      title: taskFields.title || reminder.title,
      description: taskFields.description || reminder.description || '',
      priority: taskFields.priority || 'normal',
      dueDate: taskFields.dueDate || reminder.date,
      dueTime: taskFields.dueTime || reminder.time,
      preferredPeriod: taskFields.preferredPeriod || 'Afternoon',
      scheduleDate: taskFields.scheduleDate || reminder.date,
      repeatMode: taskFields.repeatMode || reminder.repeatMode || 'none',
      repeatDays: taskFields.repeatDays || reminder.repeatDays || [],
      repeatStartDate: taskFields.repeatStartDate || reminder.repeatStartDate || reminder.date
    });
    saveReminders(reminders.map((item) => item.id === reminderId ? { ...item, linkedTaskId: task.id } : item));
    return task;
  }

  function createWorkItem(input) {
    const task = addTask({
      title: (input.taskFields && input.taskFields.title) || input.title || 'New task',
      description: (input.taskFields && input.taskFields.description) || input.description || '',
      priority: (input.taskFields && input.taskFields.priority) || 'normal',
      dueDate: (input.taskFields && input.taskFields.dueDate) || input.date || DEFAULT_SCHEDULE_DATE,
      dueTime: (input.taskFields && input.taskFields.dueTime) || input.time || '09:00',
      preferredPeriod: (input.taskFields && input.taskFields.preferredPeriod) || 'Afternoon',
      scheduleDate: (input.taskFields && input.taskFields.scheduleDate) || input.date || DEFAULT_SCHEDULE_DATE,
      repeatMode: (input.taskFields && input.taskFields.repeatMode) || input.repeatMode || 'none',
      repeatDays: (input.taskFields && input.taskFields.repeatDays) || input.repeatDays || [],
      repeatStartDate: (input.taskFields && input.taskFields.repeatStartDate) || input.repeatStartDate || DEFAULT_SCHEDULE_DATE
    });
    const reminder = input.addReminder ? createReminderForTask(task.id, {
      title: input.reminderTitle || input.title || task.title,
      description: input.reminderDescription || input.description || '',
      date: input.reminderDate || input.date || task.dueDate,
      time: input.reminderTime || input.time || task.dueTime,
      repeatMode: input.reminderRepeatMode || input.repeatMode || task.repeatMode,
      repeatDays: input.reminderRepeatDays || input.repeatDays || task.repeatDays,
      repeatStartDate: input.reminderRepeatStartDate || input.repeatStartDate || task.repeatStartDate
    }) : null;
    return { reminder, task };
  }

  function snoozeReminder(reminderId, currentDateIso) {
    const reminder = loadReminders().find((item) => item.id === reminderId);
    if (!reminder) return;
    const snoozed = loadSnoozedReminders();
    snoozed[reminderId] = {
      date: addDays(currentDateIso, 1),
      time: reminder.time
    };
    saveSnoozedReminders(snoozed);
  }

  function rescheduleTask(taskId, mode, options = {}) {
    const task = loadTasks().find((item) => item.id === taskId);
    if (!task) return null;

    let nextDate = task.scheduleDate || task.dueDate || DEFAULT_SCHEDULE_DATE;
    let nextPreferredPeriod = task.preferredPeriod;

    if (mode === 'urgent') {
      nextDate = addDays(DEFAULT_SCHEDULE_DATE, 1);
      nextPreferredPeriod = 'Morning';
    } else if (mode === 'week') {
      nextDate = addDays(DEFAULT_SCHEDULE_DATE, 3);
    } else if (mode === 'due-date' && options.dueDate) {
      nextDate = options.dueDate;
    } else {
      return task;
    }

    const nextTask = updateTask(taskId, {
      dueDate: nextDate,
      scheduleDate: nextDate,
      preferredPeriod: nextPreferredPeriod,
      repeatStartDate: task.repeatMode === 'none' ? task.repeatStartDate : nextDate
    });

    if (nextTask) {
      syncLinkedReminderDates(taskId, {
        date: nextTask.dueDate,
        time: nextTask.dueTime
      });
    }

    return nextTask;
  }

  function expandLifePeriodsForWindow(dayStart, dayEnd) {
    const startWindow = toMinutes(dayStart);
    const endWindow = toMinutes(dayEnd);
    return loadPeriods()
      .filter((period) => period.type === 'life')
      .flatMap((period) => {
        const start = toMinutes(period.start);
        const end = toMinutes(period.end);
        if (end <= start) {
          return [
            { title: period.name, start: '00:00', end: period.end, fullRange: formatRange(period.start, period.end) },
            { title: period.name, start: period.start, end: dayEnd === '24:00' ? '24:00' : dayEnd, fullRange: formatRange(period.start, period.end) }
          ];
        }
        return [{ title: period.name, start: period.start, end: period.end, fullRange: formatRange(period.start, period.end) }];
      })
      .filter((segment) => {
        const segmentStart = toMinutes(segment.start);
        const segmentEnd = segment.end === '24:00' ? 1440 : toMinutes(segment.end);
        return segmentEnd > startWindow && segmentStart < endWindow;
      })
      .map((segment) => ({
        ...segment,
        start: toMinutes(segment.start) < startWindow ? dayStart : segment.start,
        end: (segment.end === '24:00' ? 1440 : toMinutes(segment.end)) > endWindow ? dayEnd : segment.end
      }))
      .sort((a, b) => toMinutes(a.start) - toMinutes(b.start));
  }

  function daySegmentsForSchedule(dateIso = DEFAULT_SCHEDULE_DATE, options = {}) {
    const { includeReminders = true, dayStart = '08:00', dayEnd = '23:00' } = options;
    const taskItems = tasksForDate(dateIso).map((task) => ({
      ...task,
      reminderCount: linkedRemindersForTask(task.id).length
    }));
    const lifeItems = expandLifePeriodsForWindow(dayStart, dayEnd).map((period) => ({
      kind: 'life',
      title: period.title,
      start: period.start,
      end: period.end
    }));
    const reminderItems = includeReminders ? remindersForDate(dateIso) : [];
    return [...taskItems, ...lifeItems, ...reminderItems].sort((a, b) => toMinutes(a.start) - toMinutes(b.start));
  }

  function plannerAgendaItems(dateIso) {
    return tasksForDate(dateIso).map((task) => ({
      title: task.title,
      description: task.description || '',
      time: formatRange(task.start, task.end),
      kind: 'task',
      id: task.id
    }))
      .filter((item, index, list) => list.findIndex((candidate) => candidate.title === item.title && candidate.time === item.time && candidate.kind === item.kind) === index)
      .sort((a, b) => {
        const startA = a.time.split(' - ')[0];
        const startB = b.time.split(' - ')[0];
        return toMinutes(to24Hour(startA)) - toMinutes(to24Hour(startB));
      });
  }

  function to24Hour(timeText) {
    const [time, suffix] = timeText.split(' ');
    if (!suffix) return time;
    const [rawHour, minute] = time.split(':').map(Number);
    const hour = suffix === 'PM' && rawHour !== 12 ? rawHour + 12 : (suffix === 'AM' && rawHour === 12 ? 0 : rawHour);
    return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
  }

  window.FocusPreviewData = {
    STORAGE_KEY,
    TASKS_KEY,
    REMINDERS_KEY,
    defaultPeriods,
    defaultTasks,
    defaultReminders,
    DEFAULT_SCHEDULE_DATE,
    normalizePeriods,
    normalizeTask,
    normalizeReminder,
    loadPeriods,
    savePeriods,
    upsertPeriod,
    deletePeriod,
    loadTasks,
    saveTasks,
    addTask,
    updateTask,
    loadReminders,
    saveReminders,
    addReminder,
    productivePeriods,
    taskCountForDate,
    tasksForDate,
    remindersForDate,
    reminderSections,
    taskAttentionSections,
    linkedRemindersForTask,
    createReminderForTask,
    promoteReminderToTask,
    createWorkItem,
    rescheduleTask,
    plannerAgendaItems,
    daySegmentsForSchedule,
    hideTask,
    hideReminder,
    snoozeReminder,
    addDays,
    toMinutes,
    addMinutes,
    formatTime,
    formatRange,
    formatShortDate,
    to24Hour
  };
})();
