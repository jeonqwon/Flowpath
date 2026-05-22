---
name: Linear Productivity System
colors:
  surface: '#FFFFFF'
  surface-dim: '#d9dadb'
  surface-bright: '#f8f9fa'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f3f4f5'
  surface-container: '#edeeef'
  surface-container-high: '#e7e8e9'
  surface-container-highest: '#e1e3e4'
  on-surface: '#191c1d'
  on-surface-variant: '#464555'
  inverse-surface: '#2e3132'
  inverse-on-surface: '#f0f1f2'
  outline: '#777587'
  outline-variant: '#c7c4d8'
  surface-tint: '#4d44e3'
  primary: '#3525cd'
  on-primary: '#ffffff'
  primary-container: '#4f46e5'
  on-primary-container: '#dad7ff'
  inverse-primary: '#c3c0ff'
  secondary: '#5a5e69'
  on-secondary: '#ffffff'
  secondary-container: '#dee2ef'
  on-secondary-container: '#60646f'
  tertiary: '#5c00ca'
  on-tertiary: '#ffffff'
  tertiary-container: '#7531e6'
  on-tertiary-container: '#e4d4ff'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#e2dfff'
  primary-fixed-dim: '#c3c0ff'
  on-primary-fixed: '#0f0069'
  on-primary-fixed-variant: '#3323cc'
  secondary-fixed: '#dee2ef'
  secondary-fixed-dim: '#c2c6d3'
  on-secondary-fixed: '#171c25'
  on-secondary-fixed-variant: '#424751'
  tertiary-fixed: '#eaddff'
  tertiary-fixed-dim: '#d2bbff'
  on-tertiary-fixed: '#25005a'
  on-tertiary-fixed-variant: '#5a00c6'
  background: '#f8f9fa'
  on-background: '#191c1d'
  surface-variant: '#e1e3e4'
  text-main: '#111827'
  text-muted: '#6B7280'
  border-subtle: '#E5E7EB'
  priority-low: '#10B981'
  priority-medium: '#F59E0B'
  priority-high: '#EF4444'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: -0.01em
  subhead-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  label-xs:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
  button-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 20px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  touch-target: 48px
  gutter: 16px
  margin-screen: 16px
---

## Brand & Style

This design system is built on the principles of **intentionality, clarity, and focus**. It targets high-performance individuals who require a tool that feels both powerful and calming. The brand personality is professional and "intelligent," avoiding unnecessary decoration in favor of structural elegance and functional density.

The chosen style is **Modern Corporate Minimalism with Tactile Layering**. It draws heavy inspiration from high-end desktop productivity tools like Linear, translating that refined "pro-tool" aesthetic into a mobile-first Android environment. Key characteristics include:
- **Soft Neutrals:** A base of off-whites and cool grays to reduce eye strain and provide a "paper-like" canvas.
- **Precision:** Thin, purposeful borders replace heavy shadows to define structure.
- **Strategic Vitality:** Vibrant accent colors are used sparingly, reserved exclusively for primary actions and critical status signaling (priorities).
- **Depth through Layering:** A hierarchy of surfaces that feels physical yet lightweight, utilizing subtle shifts in tonal values to denote importance.

## Colors

The color strategy centers on **functional chromaticity**. The background is not pure white but a subtle gray (`#F8F9FA`), which makes the white surface cards (`#FFFFFF`) appear to float slightly above the canvas.

- **Primary & Secondary:** The Indigo pair is used for the "active" state. The primary indigo is for high-contrast actions, while the soft indigo is for backgrounds of selected items or chips.
- **Status/Priority:** These colors are semantically mapped. Use the Emerald, Amber, Rose, and Violet shades only for indicating task urgency. Do not use these for decorative elements.
- **Typography:** Two distinct levels of gray are used to maintain a clear information hierarchy without the harshness of pure black.

## Typography

The system utilizes **Inter** for its neutral, systematic character and exceptional legibility at small sizes—essential for data-dense productivity views.

- **Scale:** The scale is tight, avoiding massive display sizes to maintain a "pro" feel. 
- **Tracking:** Headlines use slight negative letter spacing to feel more cohesive and "set," while labels and small subheads use positive tracking to ensure readability at 12px and 14px.
- **Hierarchy:** Use weight (Semi-Bold/Bold) rather than just size to differentiate titles from body text. Task titles in cards should always use `headline-md` for immediate scannability.

## Layout & Spacing

This design system uses a **Fluid Grid with Fixed Gutters**, based on an 8px rhythmic increment. 

- **The 8px Rule:** All margins, paddings, and component heights must be multiples of 8 (or 4 for extremely tight internal spacing).
- **Safe Margins:** A standard 16px (`md`) margin is applied to the left and right of all screens.
- **Vertical Rhythm:** Task lists should utilize 12px to 16px of vertical spacing between cards. Calendar views (Day/Week) should align to a strict vertical grid where each hour block is a multiple of 48px.
- **Mobile Adaptations:** On smaller screens, the 16px gutter is maintained, but card internal padding may drop to 12px to maximize content area.

## Elevation & Depth

Depth is conveyed through **Tonal Layering and Low-Contrast Outlines**. 

1.  **Base Layer (Level 0):** The app background (`neutral_color_hex`).
2.  **Surface Layer (Level 1):** Main content cards and calendar cells. These use a pure white background and a 1px solid border (`border-subtle`). Use an extremely soft shadow (blur 4px, 5% opacity) to give a subtle lift.
3.  **Floating Layer (Level 2):** Floating Action Buttons (FABs) and Bottom Sheets. These require higher contrast. Use a more pronounced shadow (blur 15px, 10% opacity) and no border, or a semi-transparent glass effect for top bars with an 8px backdrop blur.

Avoid using heavy, dark shadows. The goal is to make the UI feel like stacked sheets of premium paper rather than floating blocks.

## Shapes

The shape language is **distinctly rounded** to soften the systematic grid and make the app feel approachable. 

- **Cards & Inputs:** Use a 12px (`rounded-lg`) corner radius for standard task cards and text input fields.
- **Bottom Sheets:** The top corners of modal sheets should be 24px (`rounded-xl`) to emphasize their "overlay" nature.
- **Interactive Elements:** Buttons and priority tags should be pill-shaped (fully rounded) to differentiate them from the structural containers.
- **Calendar Chips:** Smaller elements like calendar event blocks use a 4px (Soft) radius to maintain precision in tight spaces.

## Components

### Task Cards
Cards are the primary unit of the UI. They feature a white surface, a 1px subtle border, and 16px internal padding. 
- **Priority Indicator:** A 4px vertical bar or a small pill-shaped dot on the left edge of the card, using the `priority` color tokens.
- **Metadata:** Timestamps and icons should use `label-xs` in `text-muted`.

### Buttons
- **Primary:** High-contrast Indigo background with white text. Pill-shaped.
- **Secondary:** Soft Indigo background with Primary Indigo text. Used for "Add Subtask" or "Reschedule."
- **Ghost:** No background, `text-muted` label. Used for less frequent actions like "Delete."

### Input Fields
Outlined style with a 1px border. When focused, the border weight increases to 2px and changes to the Primary Indigo color.

### Calendar Cells
In the Day View, work blocks should be treated as "soft cards." They use the `secondary_color_hex` (Soft Indigo) as a fill, with a 2px left-accent border of the `primary_color_hex`. This creates a "Linear-inspired" timeline feel.

### Lists
Lists should be "unbound" (no background for the list itself) with 16px spacing between the cards to let the background breathe. Use a 48px touch target for all list item interactions (checkboxes, drag handles).