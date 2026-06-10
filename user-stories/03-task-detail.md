# Task Detail

## US-DT-01: View task details
**As a** user  
**I want** to see all relevant information about a task on its detail screen  
**So that** I can understand its configuration and scheduled time  

### Acceptance Criteria
- Card shows task name in headline style
- Block time shown directly below the name (e.g., "9:00 AM to 10:30 AM")
- If urgent, "Urgent" appears beside the time in same font/size with a gap

## US-DT-02: See non-default scheduling details
**As a** user  
**I want** to see only the scheduling details that differ from defaults  
**So that** the detail card stays clean and uncluttered  

### Acceptance Criteria
- Deadline shown only if `hasDeadline` is true
- Window (Morning/Noon/Afternoon/Night) shown only if not "Anytime"
- Timeframe shown only if one is assigned
- Repeat schedule shown only if task is recurring
- Splitting shown only if disabled (differs from default "on")

## US-DT-03: Mark a task as done
**As a** user  
**I want** to mark a task as completed  
**So that** it's removed from my active schedule  

### Acceptance Criteria
- "Done" button marks the task complete
- Completed tasks move to history
- Recurring tasks show "Done all recurring" in the More menu
- Done all recurring confirms with a dialog

## US-DT-04: Follow up on a task
**As a** user  
**I want** to create a follow-up task from an existing task  
**So that** I can chain related work together  

### Acceptance Criteria
- "Follow up" option in the More menu
- Opens create screen pre-filled with source task's context
- Source task is marked complete only when follow-up fully schedules

## US-DT-05: Reschedule a task
**As a** user  
**I want** to move a task to a different time  
**So that** I can adjust my schedule as priorities change  

### Acceptance Criteria
- "Reschedule" option in the More menu
- Opens create screen with reschedule mode
- Old blocks are cleared and new ones placed

## US-DT-06: View repeat calendar
**As a** user with a recurring task  
**I want** to see a calendar showing when the task repeats  
**So that** I know which days are covered  

### Acceptance Criteria
- Tapping the Repeat row opens a calendar sheet
- Calendar shows highlighted dates where the task occurs
- Original due date is outlined
- Month navigation with chevron buttons
- Calendar grid always shows 6 rows for consistent height
- Sheet opens fully expanded, not cut off
