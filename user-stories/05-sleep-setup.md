# Sleep Setup

## US-SL-01: Configure sleep schedule
**As a** user  
**I want** to set up my sleeping hours for each day of the week  
**So that** the scheduler protects my sleep time from other tasks  

### Acceptance Criteria
- Sleep setup accessible from the Tasks header (bed icon)
- Can configure sleep per day of the week
- Default sleep is 8 PM to 8 AM overnight
- Sleep uses weekly recurrence by default
- Can switch to daily, monthly, or one-time sleep
- All recurrence types are selectable (not restricted)

## US-SL-02: See sleep coverage status
**As a** user  
**I want** to know how many days of the week have sleep configured  
**So that** I can ensure all nights are covered  

### Acceptance Criteria
- Banner shows when sleep is incomplete
- "X of 7 days configured" progress display
- Banner offers "Set up" or "Continue" action
- Banner can be dismissed
- When all 7 days covered, banner shows "Ready to create tasks"

## US-SL-03: Overwrite existing sleep with warning
**As a** user  
**I want** to be warned if I'm about to replace existing sleep on overlapping days  
**So that** I don't accidentally overwrite my sleep schedule  

### Acceptance Criteria
- Warning card appears when creating sleep that overlaps with existing
- Daily new sleep warns: "Saving will replace it for all days"
- Weekly warns with specific day names: "Sleep already set for Mon, Wed"
- Monthly warns: "Saving will replace all monthly occurrences"
- One-time warns: "Sleep is already scheduled for this day"

## US-SL-04: Sleep blocks protect from task overlap
**As a** user  
**I want** my sleep time to be respected by the scheduler  
**So that** no work tasks are scheduled during my sleeping hours  

### Acceptance Criteria
- Sleep blocks are always hard obstacles (never overlapped)
- New tasks are scheduled around sleep blocks
- Tasks with splitting enabled split before and after sleep
- Editing a task that extends into sleep triggers a split
- Split portions after sleep are placed the next morning

## US-SL-05: Configure sleep as recurring or one-time
**As a** user  
**I want** to choose whether my sleep repeats or is a one-time block  
**So that** I can handle irregular sleep schedules  

### Acceptance Criteria
- Daily sleep: repeats every day
- Weekly sleep: repeats on selected days of the week
- Monthly sleep: repeats monthly on the same day
- One-time (None): single occurrence, no repeat
