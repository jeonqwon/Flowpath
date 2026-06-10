# Blockers & Follow-ups

## US-BL-01: Create a blocker
**As a** user  
**I want** to place a fixed-time blocker in my schedule  
**So that** I can reserve time for meetings, appointments, or non-task activities  

### Acceptance Criteria
- "Add Blocker" in the Add chooser sheet
- Blocker has title, start time, and end time
- Blocker appears on the timeline as a locked time window
- Other tasks cannot overlap or move into blocker time

## US-BL-02: Follow up on a completed task
**As a** user  
**I want** to create a follow-up task after completing another  
**So that** I can chain dependent work together  

### Acceptance Criteria
- Follow-up option in task's More menu
- Opens create screen with context from source task
- Source task is completed only when follow-up is fully scheduled
- Partial follow-up (splitting) does not complete the source task

## US-BL-03: Set task dependencies (continuation)
**As a** user  
**I want** to make one task depend on another  
**So that** the dependent task is scheduled after the first one finishes  

### Acceptance Criteria
- Continuation parent can be set during task creation
- Dependent task starts after the parent's scheduled blocks end
- Continuation mode controls whether the parent is marked complete
