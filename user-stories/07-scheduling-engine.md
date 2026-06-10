# Scheduling Engine

## US-SC-01: Automatic task placement
**As a** user  
**I want** the app to automatically find the best time for my task  
**So that** I don't have to manually figure out where it fits  

### Acceptance Criteria
- Scheduler places tasks in the earliest available time slot
- Respects all constraints: deadline, time window, fixed start, timeframe
- Avoids hard-blocked windows (sleep, locked blocks, non-overlappable tasks)
- Scans up to 14 days ahead for available slots

## US-SC-02: Task ordering by priority
**As a** user  
**I want** higher-priority tasks to be scheduled before lower-priority ones  
**So that** my most important work gets placed first  

### Acceptance Criteria
- Sleep tasks always scheduled first
- Blocker tasks scheduled second
- Normal tasks ordered by: has deadline > deadline proximity > priority score
- Within same priority, tasks with earlier deadlines go first

## US-SC-03: Task splitting
**As a** user  
**I want** long tasks to be split into multiple blocks when they don't fit in one slot  
**So that** they still get scheduled even if the day is fragmented  

### Acceptance Criteria
- Splitting controlled by per-task toggle + global setting
- When splitting: task broken into chunks at least 30 minutes each
- Split chunks maximize size (fewest pieces possible)
- Split portions can extend past the original deadline within the 14-day window
- When splitting disabled: task must fit in one continuous block

## US-SC-04: Split minimization across days
**As a** user  
**I want** the scheduler to scan future days before splitting my task  
**So that** my task stays as one continuous block whenever possible  

### Acceptance Criteria
- Scheduler scans all 14 lookahead days before committing to a split
- Prefers unsplit full-duration blocks on later days over split blocks today
- When splitting is unavoidable, uses the largest available chunks

## US-SC-05: Overlap / concurrent tasks
**As a** user  
**I want** to allow tasks to share the same time slot when appropriate  
**So that** my schedule isn't unnecessarily spread out  

### Acceptance Criteria
- Global "Concurrent tasks" setting (default: on)
- Per-task overlap policy: Inherit, Allow, No Overlap
- When both tasks allow overlap, they can share time
- Sleep and blockers never allow overlap
- Tasks with "No Overlap" get exclusive time slots

## US-SC-06: Fixed start time anchoring
**As a** user  
**I want** my task to start at exactly the time I specified  
**So that** time-sensitive activities begin when they should  

### Acceptance Criteria
- Tasks with fixed start time anchor their first block at that time
- If another task overlaps at the fixed start, the new task still starts there (if overlap allowed)
- If the window is blocked and splitting is on, first chunk starts at fixed time, remainder after blocker
- If the window is blocked and splitting is off, the task is unscheduled

## US-SC-07: Exact time tasks (blockers)
**As a** user  
**I want** to place a task at an exact fixed time window  
**So that** I can block out time for meetings or appointments  

### Acceptance Criteria
- Fixed Exact tasks occupy a precise start-end window
- If window is blocked and splitting enabled: converts to Flexible with fixed start
- If window is blocked and splitting disabled: reports "blocked by another event"
- Blockers with zero duration are not splittable

## US-SC-08: Fast scheduling for single tasks
**As a** user  
**I want** creating a single task to be fast  
**So that** I'm not waiting for the entire schedule to recalculate  

### Acceptance Criteria
- Non-recurring normal tasks use fast path: slot into existing gaps
- Existing blocks are preserved (not disturbed)
- Sleep and blocker tasks always use full rebuild (priority placement)
- If fast path fails (window too full), falls back to full rebuild
