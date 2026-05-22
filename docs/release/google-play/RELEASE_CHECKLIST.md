# OpenReclaim Google Play Release Checklist

Last updated: May 22, 2026

## Code and build

- [x] Scheduler unit tests pass
- [x] App coordinator unit tests pass
- [x] Android lint passes
- [x] Release APK assembles
- [x] Release AAB builds
- [x] Android smoke test passes on emulator
- [x] Destructive Room migration removed
- [x] Fake production wiring removed from shipped app graph
- [x] Unused permissions removed from manifest

## Product validation

- [ ] Manual QA on at least one small phone profile
- [ ] Manual QA on at least one large phone profile
- [ ] Final review of task creation, recurrence, reminders, reschedule, and follow-up flows
- [ ] Final review of dark theme and light theme on device
- [ ] Accessibility pass for touch targets, content descriptions, and text truncation

## Store assets

- [x] Privacy policy draft created
- [x] Data Safety worksheet created
- [x] Store listing draft created
- [ ] Final app icon review
- [ ] Final screenshot selection and cropping
- [ ] Feature graphic if desired

## Google Play Console work

- [ ] Create or confirm support email
- [ ] Host the privacy policy at a public HTTPS URL
- [ ] Upload AAB
- [ ] Enable Play App Signing
- [ ] Complete Data Safety form
- [ ] Complete App Content section
- [ ] Complete content rating questionnaire
- [ ] Complete store listing and screenshots
- [ ] Run internal testing track
- [ ] Review pre-launch report before production rollout

## Current recommendation

Use this build for:

- internal testing
- closed testing
- open testing after one more manual multi-device pass

Do not push directly to full production until the unchecked Play Console and manual QA items above are complete.
