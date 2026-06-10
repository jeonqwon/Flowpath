# Task Timeline

## US-TL-01: View daily schedule
**As a** user  
**I want** to see my day's scheduled tasks on a timeline  
**So that** I can understand when each task is scheduled and how my day is organized  

### Acceptance Criteria
- Timeline shows hours from 00:00 to 24:00 with hourly labels
- Each task appears as a card positioned at its scheduled time
- Task cards show title and time range
- Current time is marked with a red "now" indicator line
- The now indicator updates every 60 seconds

## US-TL-02: Scroll through days continuously
**As a** user  
**I want** to scroll vertically through days without seams between them  
**So that** I can browse my schedule smoothly across multiple days  

### Acceptance Criteria
- Scrolling is smooth with no visual gaps between day boundaries
- Days flow together as one continuous canvas
- Date chips move smoothly with scrolling

## US-TL-03: See overnight tasks as continuous cards
**As a** user  
**I want** tasks that span midnight (e.g., sleep) to appear as single unbroken cards  
**So that** I can see the full duration of overnight tasks at a glance  

### Acceptance Criteria
- Sleep blocks render as one card spanning across midnight
- Overnight task cards show the full duration without fragmentation
- Card corners are rounded only at the actual start/end boundaries

## US-TL-04: Switch between collapsed and expanded views
**As a** user  
**I want** to toggle between a compact day-summary view and a detailed timeline view  
**So that** I can scan many days quickly or inspect a specific day in detail  

### Acceptance Criteria
- Toggle button switches between Collapsed and Expanded modes
- Collapsed view shows one row per day with task/reminder count
- Expanded view shows full timeline with positioned task cards
- Tapping a collapsed day row opens a day-summary sheet
- Scroll position is preserved when switching views

## US-TL-05: See timeframe rails
**As a** user with project timeframes  
**I want** to see colored rails beside the timeline showing which timeframes are active  
**So that** I can identify which project each day belongs to  

### Acceptance Criteria
- Active timeframes show as colored vertical rails to the left of the timeline
- Rails span the full day height
- Timeframe names appear as chips in the sticky header
- Multiple overlapping timeframes stack side by side

## US-TL-06: Tap a task card to open details
**As a** user  
**I want** to tap any task card on the timeline to see its full details  
**So that** I can inspect or edit a task quickly  

### Acceptance Criteria
- Tapping a task card navigates to the Task Detail screen
- Scrolling does not accidentally trigger a tap
- Tapping works on overnight cards too

## US-TL-07: Go to today
**As a** user  
**I want** a button that scrolls back to the current day and time  
**So that** I can quickly return to today after browsing future days  

### Acceptance Criteria
- "Today" button scrolls the timeline to the current day
- In expanded view, scrolls to current time hour (minus one hour for context)
- In collapsed view, scrolls to today's row
