# Flowpath

Flowpath is a local-first Android planner for scheduling tasks, reminders, and routines into your day.

## Features

- Day timeline with scheduled blocks and a live now indicator
- Task creation with duration, priority, deadline, and scheduling controls
- Recurring tasks and reminders
- Planner month view with day summaries
- Daily Flow time periods for productive and unavailable time
- Schedule locking, rescheduling, and reminder linking
- Local-first storage with no account requirement

## Tech Stack

- Kotlin
- Jetpack Compose with Material 3
- Room
- Coroutines and Flow
- Multi-module structure: `app` and `engine`

## Project Structure

```text
app/     Android application
engine/  Pure Kotlin scheduling engine
docs/    Mobile app and release documentation
```

## User Guide

See [docs/USER_GUIDE.md](docs/USER_GUIDE.md).

## Build

Requires Android Studio and JDK 17+.

```bash
./gradlew assembleDebug
```

## License

MIT
