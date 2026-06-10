# Project Timeframes

## US-TF-01: Create a project timeframe
**As a** user  
**I want** to define a date range for a project (timeframe) with a name and color  
**So that** I can group tasks by project and see them visually on the timeline  

### Acceptance Criteria
- Can create a timeframe with name, start date, end date, and color
- Multiple timeframes can overlap
- Timeframe appears as a colored rail on the timeline

## US-TF-02: Assign a task to a timeframe
**As a** user  
**I want** to associate my task with a project timeframe  
**So that** the task is visible within that project's context on the timeline  

### Acceptance Criteria
- Task creation/edit includes timeframe assignment
- Tasks inherit timeframe color on the timeline
- Tasks outside their timeframe's date range show a scheduling issue

## US-TF-03: Edit or delete a timeframe
**As a** user  
**I want** to modify or remove a project timeframe  
**So that** I can keep my projects up to date  

### Acceptance Criteria
- Edit changes the timeframe's name, dates, or color
- Delete removes the timeframe
- Tasks assigned to a deleted timeframe lose the association
