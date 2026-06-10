# Reminders

## US-RM-01: Create a reminder for a task
**As a** user  
**I want** to add a notification reminder to my task  
**So that** I get alerted when the task is due or starting  

### Acceptance Criteria
- "Add Reminder" option in the task's More menu
- Reminder due time follows global reminder timing settings
- Reminder appears in the Upcoming Reminders sheet
- Dismissing a reminder removes the notification

## US-RM-02: View upcoming reminders
**As a** user  
**I want** to see all my upcoming reminders in one list  
**So that** I can prepare for what's coming up  

### Acceptance Criteria
- Reminders icon in Tasks header opens a bottom sheet
- Sheet lists all active reminders sorted by due time
- Each card shows title, due time, and linked task name
- Tapping opens the reminder or linked task detail

## US-RM-03: Create a standalone reminder
**As a** user  
**I want** to create a reminder that isn't tied to a specific task  
**So that** I can get notified about events without scheduling work time  

### Acceptance Criteria
- "Add Reminder" option in the Add chooser sheet
- Standalone reminders have title, description, due date
- Can be linked to a task optionally
- Appear in the reminders list

## US-RM-04: Configure reminder timing
**As a** user  
**I want** to control when reminders fire relative to the task  
**So that** I get notified at the right moment  

### Acceptance Criteria
- Global setting: At task time, or X minutes before
- Lead time configurable (default 15 minutes)
- Per-task: reminders can be added or dismissed
