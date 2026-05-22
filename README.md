# OpenReclaim

Plan tasks into your day with local-first scheduling, reminders, and recurring routines.

## Features

- **Day timeline** — Full-day scrollable view with task blocks, time periods, and a live "now" indicator
- **Task creation** — Title, description, due date, estimated duration, preferred time-of-day, priority
- **Recurring tasks** — Daily or weekly repeat with series management
- **Reminders** — Standalone reminders with due times, snooze, and task linking
- **Planner month view** — Calendar grid with task density dots, day overview with scheduled items
- **Time periods** — Custom blocks (e.g. "Morning Deep Work", "Evening Routine") that constrain scheduling
- **Schedule locking & rescheduling** — Lock blocks in place or reschedule missed tasks
- **Local-first, offline** — All data on-device via Room, no account or cloud sync required

## Tech Stack

- **Kotlin** (Android)
- **Jetpack Compose** with Material 3
- **Room** for local persistence
- **Coroutines & Flow** for async
- **Multi-module**: `app` (UI) + `engine` (pure Kotlin scheduling library)

## Project Structure

```
app/          Android application (UI, ViewModels, Room, Compose)
engine/       Pure Kotlin/JVM scheduling engine (no Android dependency)
docs/         Release documentation
stitch_preview/  Web-based UI design previews
```

## User Guide

See [docs/USER_GUIDE.md](docs/USER_GUIDE.md) for a step-by-step walkthrough.

## Build

Requires Android Studio and JDK 17+.

```bash
./gradlew assembleDebug
```

## License

MIT
