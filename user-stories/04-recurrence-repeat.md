# Recurrence & Repeat

## US-RP-01: Make a task repeat
**As a** user  
**I want** to set my task to repeat daily, weekly, or monthly  
**So that** I don't have to recreate it for every occurrence  

### Acceptance Criteria
- Repeat section shows type chips: None, Daily, Weekly, Monthly
- All four types are selectable (no locking)
- Switching away from Weekly clears day selection
- The Repeat editor sheet shows the same options

## US-RP-02: Choose specific days for weekly repeat
**As a** user with a weekly repeat  
**I want** to select which days of the week the task repeats on  
**So that** I can customize my weekly schedule  

### Acceptance Criteria
- Day-of-week toggles show for Weekly repeat
- Selected days are visually highlighted
- At least one day must be selected

## US-RP-03: Set repeat interval
**As a** user  
**I want** to control how often the task repeats (every 1, 2, 3... days/weeks/months)  
**So that** I can schedule less frequent recurrences  

### Acceptance Criteria
- Interval can be set in the Repeat editor sheet
- "Every X days" / "Every X weeks" / "Every X months" display text
- Minimum interval is 1

## US-RP-04: Set repeat until date or occurrence count
**As a** user  
**I want** to specify when the repeat ends  
**So that** the task doesn't repeat indefinitely unless I want it to  

### Acceptance Criteria
- "Repeats forever" toggle when no deadline is set
- When not forever: "Repeat until" date picker available
- Occurrence count limit supported
- On-date end mode stops after a specific date

## US-RP-05: Manage recurring tasks
**As a** user  
**I want** to see all my recurring tasks in one place  
**So that** I can manage them collectively  

### Acceptance Criteria
- Recurring screen accessible from the Tasks header
- Shows each recurring series as a card
- Tapping opens the task detail for that recurrence

## US-RP-06: Complete one occurrence of a recurring task
**As a** user  
**I want** to mark one instance of a recurring task as done  
**So that** that specific occurrence is removed while future ones remain  

### Acceptance Criteria
- "Done" completes only the current occurrence
- Next occurrence is materialized automatically
- Future occurrences are unaffected
