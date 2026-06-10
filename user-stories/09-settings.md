# Settings

## US-ST-01: Configure scheduling defaults
**As a** user  
**I want** to set global defaults for how tasks are scheduled  
**So that** I don't have to configure every task individually  

### Acceptance Criteria
- Concurrent tasks toggle (default: on)
- Task splitting toggle (default: on)
- Maximum chunk minutes (default: 480)
- Break buffer between blocks (default: 0 minutes)
- Alignment minutes for grid snapping (default: 30)

## US-ST-02: Customize appearance
**As a** user  
**I want** to adjust the app's appearance to my preference  
**So that** it's comfortable and readable  

### Acceptance Criteria
- Theme mode: System, Light, Dark
- Font size scale: Small, Default, Large
- Task hour height (timeline zoom): adjustable via slider
- Date format: Month-Day-Year or Day-Month-Year
- Week start day: Sunday or Monday

## US-ST-03: Configure reminders globally
**As a** user  
**I want** to set default reminder behavior for all tasks  
**So that** I get consistent notifications  

### Acceptance Criteria
- Default task reminder toggle (on/off)
- Reminder timing: at task time, or N minutes before
- Lead time in minutes (default: 15)

## US-ST-04: Configure urgent reschedule behavior
**As a** user  
**I want** to decide how the app handles urgent tasks that don't fit  
**So that** I control whether other tasks can be moved to make room  

### Acceptance Criteria
- "Move other flexible tasks if needed" mode
- "Only use available gaps" mode
- Setting affects how aggressive the scheduler is when rescheduling

## US-ST-05: Configure history retention
**As a** user  
**I want** to control how long completed tasks are kept  
**So that** I can balance history vs. clutter  

### Acceptance Criteria
- Retention: 7 days, 30 days, 90 days, or Forever
- Completed tasks older than the retention period are cleaned up

## US-ST-06: View and manage time periods
**As a** user  
**I want** to define custom time periods (e.g., "work hours", "study time")  
**So that** tasks can be assigned to specific periods of the day  

### Acceptance Criteria
- Can create named time periods with start/end times
- Time periods appear as options when setting task availability
