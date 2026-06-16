# Flowpath

An open-source, local-first Android planner — schedule tasks, reminders, and routines without an account.

## Features

### Timeline
- Scrollable **day view** with hour labels and a live now indicator
- Color-coded **time periods** (productive / life) as background blocks
- Task blocks show **title, duration, and priority color** at a glance

### Tasks
- **Create** tasks with title, description, due date, estimated duration (15 min – 8 h), and priority (Low / Medium / High / Urgent)
- **Preferred time of day** and **preferred period** to match tasks to your energy
- **Recurring tasks** — daily or weekly with day selection
- **Complete**, **reschedule**, **lock/unlock**, or **delete** from the task detail view
- **Add follow-up** — clone a task pre-filled with the original's data

### Reminders
- Standalone or **linked to a task** for context
- **Repeat** on a daily or weekly schedule
- **Complete, snooze, or delete** from the reminders list
- Android notification support with alarm scheduling

### Planner (Month View)
- Calendar grid with **dot indicators** on days that have tasks
- Tap a day to see its **scheduled tasks and reminders** in a side panel
- Navigate months with arrow controls

### Scheduling Engine
- Pure Kotlin engine (in `engine/`) that auto-schedules tasks into available slots
- Respects your **work hours**, **time periods**, **priority**, and **deadline urgency**
- **Locked tasks** stay put; the scheduler works around them
- Also runs as a **CLI** for testing and scripting

### Other
- **Google Calendar** sync support
- Four-tab navigation: **Tasks** · **Planner** · **Reminders** · **Settings**
- Local SQLite storage via Room — no cloud, no sign-up

## How to Use

1. **Set up time periods** — Go to *Settings* and add your daily blocks (e.g. "Morning Deep Work", "Lunch", "Evening"). Mark them *Productive* or *Life* so the scheduler knows where to place tasks.

2. **Create a task** — Tap **+** on the Tasks tab, fill in the title, duration, due date, and priority. Optionally tie it to a preferred period or make it recurring.

3. **Add reminders** — Tap **+** then switch to the *Reminder* tab. Link it to an existing task for extra context.

4. **Use the Planner** — The *Planner* tab gives you a month overview. Tap any day to review what's scheduled.

5. **Work your day** — Follow the timeline on the *Tasks* tab. Complete, reschedule, or lock tasks as you go. The scheduler rebuilds automatically.

6. **Repeat** — Set up recurring tasks and reminders for daily/weekly routines. Lock important blocks so they don't get moved.

For a full walkthrough, see the [User Guide](docs/USER_GUIDE.md).

## Tech Stack

Kotlin · Jetpack Compose · Material 3 · Room · Coroutines & Flow

## Structure

```text
app/     Android application
engine/  Pure Kotlin scheduling engine (CLI + library)
docs/    User guide and release docs
```

## Build

Requires Android Studio Hedgehog+ and JDK 17.

```bash
./gradlew assembleDebug
```

## License

MIT
