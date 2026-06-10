# Task Creation & Editing

## US-CR-01: Create a task
**As a** user  
**I want** to create a new task with a title, duration, priority, and due date  
**So that** the app can schedule it into my timeline  

### Acceptance Criteria
- "Add Task" opens the create screen
- Title field accepts text input
- Duration can be set (default 60 minutes)
- Priority can be set (Low, Normal, High, Urgent)
- Due date can be set or toggled off ("No deadline")
- Save schedules the task and returns to the timeline
- Cancel discards the task

## US-CR-02: Set a deadline
**As a** user  
**I want** to set or remove a deadline for my task  
**So that** the scheduler knows when the task must be completed  

### Acceptance Criteria
- Deadline toggle enables/disables the deadline
- Date and time pickers let me choose when it's due
- When "No deadline" is set, the task can be scheduled flexibly

## US-CR-03: Configure scheduling mode
**As a** user  
**I want** to choose how my task is scheduled (Flexible, Fixed Day, Fixed Start Time, Exact Time)  
**So that** I can control when and how rigidly the task is placed  

### Acceptance Criteria
- Flexible: scheduler picks any available time before the deadline
- Fixed Day: task must be on a specific day
- Fixed Start: task starts at a specific time but can extend
- Fixed Exact: task occupies an exact start-end window
- Schedule availability section opens an editor sheet

## US-CR-04: Set a time window (Availability)
**As a** user  
**I want** to restrict my task to specific hours of the day (Morning, Noon, Afternoon, Night)  
**So that** the task is only scheduled during appropriate time windows  

### Acceptance Criteria
- Window options: Anytime, Morning, Noon, Afternoon, Night
- The scheduler respects the window when placing the task
- Changing the window updates the schedule

## US-CR-05: Configure task splitting
**As a** user  
**I want** to allow or disallow my task being split into multiple blocks  
**So that** long tasks can be broken around obstacles or kept continuous  

### Acceptance Criteria
- Splitting toggle in Rules editor
- When enabled: task can be split into multiple time blocks
- When disabled: task must be placed as one continuous block
- Split portions extend past deadline within reason

## US-CR-06: Configure task overlap policy
**As a** user  
**I want** to control whether my task can overlap with other tasks  
**So that** I can allow flexible co-existence or enforce exclusive time slots  

### Acceptance Criteria
- Overlap options: Inherit (follow global setting), Allow, No Overlap
- "No Overlap" ensures the task gets exclusive time
- "Allow" lets other tasks share the same time slot
- Global Concurrent Tasks setting acts as default

## US-CR-07: Add a reminder to a task
**As a** user  
**I want** to attach a reminder notification to my task  
**So that** I get alerted before or at the task's scheduled time  

### Acceptance Criteria
- "Add Reminder" toggle in Rules editor
- Reminder timing follows global settings (at task time, or N minutes before)
- Reminder appears in the reminders list
- Removing reminder from task also removes the notification

## US-CR-08: Edit an existing task
**As a** user  
**I want** to modify any property of an existing task  
**So that** I can update its details, timing, or scheduling rules  

### Acceptance Criteria
- Edit button on task detail screen opens edit mode
- All creation fields are editable
- Save reschedules the task with new parameters
- Cancel leaves the task unchanged
- Changing duration updates the scheduled blocks

## US-CR-09: Delete a task
**As a** user  
**I want** to delete a task I no longer need  
**So that** it's removed from my schedule  

### Acceptance Criteria
- Delete button on task detail shows confirmation dialog
- Confirming deletes the task and its scheduled blocks
- Cancelling keeps the task
