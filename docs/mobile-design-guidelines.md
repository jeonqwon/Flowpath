# Flowpath Mobile Design Guidelines

Flowpath uses Jetpack Compose Material 3 as its UI foundation. These rules are the app-wide source of truth for new UI work and refactors.

## Foundations

- Use `MaterialTheme` color scheme, typography, and shapes as the only design tokens.
- Prefer Material 3 components before creating custom surfaces or controls.
- Keep interactive targets at least `48.dp` tall or wide where practical.
- Use tonal surfaces and semantic color roles instead of hard-coded screen-specific colors.
- Light and dark mode should communicate the same meaning with the same role mapping.

## Color Roles

- `primary`: selected state, primary actions, active pills, emphasis accents
- `primaryContainer`: softer selected backgrounds and supportive emphasis
- `secondary` / `secondaryContainer`: secondary accents and informational emphasis
- `surface`: cards, sheets, dialogs, and elevated content
- `surfaceVariant`: subdued surfaces, grouped controls, inactive pills
- `background`: page background
- `outline` / `outlineVariant`: borders and dividers
- `onSurface` and `onSurfaceVariant`: default and secondary text

Do not introduce screen-specific blues, greys, or dark-mode-only overrides unless a semantic role cannot express the need.

## Typography

- `headlineMedium`: page titles
- `titleLarge`: primary card titles and section-leading emphasis
- `titleMedium`: row titles and important controls
- `titleSmall`: section labels, segmented controls, dense labels
- `bodyLarge`: primary body copy
- `bodyMedium`: standard field content and rows
- `bodySmall`: metadata and rare helper text
- `labelLarge`: button labels and compact action text

Do not set ad hoc text sizes in screens unless a Material typography role is clearly insufficient.

## Spacing and Shape

- Use an 8dp spacing rhythm.
- Common gaps:
  - `8.dp` for tight related items
  - `12.dp` for dense control groups
  - `16.dp` for standard section spacing
  - `24.dp` for major section breaks
- Use rounded corners consistently:
  - large cards/sheets: `28.dp`
  - medium cards and panels: `20.dp`
  - pills/chips/segmented controls: fully rounded
  - small inline elements: `16.dp`

## Component Patterns

- Page header: back/action row plus one headline title style
- Form card: single surface style with consistent padding and border/elevation treatment
- Action buttons: use one primary action treatment and one secondary/outlined treatment
- Segmented choices: use one consistent chip/segmented control pattern
- Inline settings rows: title first, short optional helper text second, control aligned consistently
- Empty/error states: concise title + short supporting line only when necessary

## Copy Rules

- Do not add helper descriptions by default.
- If explanation is necessary, keep it to one short sentence.
- Prefer direct labels like `No deadline`, `Fixed time`, `Add reminder`, `Allow overlap`.

## Screen-Specific Direction

- `Tasks`, `Planner`, and `Reminders` should feel like one system:
  - same top header language
  - same selected-date treatment
  - same card vocabulary
- `Create Task` should be compact and mode-driven:
  - only show fields relevant to the chosen scheduling mode
  - keep advanced controls grouped and collapsible when appropriate
- `Settings` should use the same row, section, and control patterns as the rest of the app rather than a separate visual system.
