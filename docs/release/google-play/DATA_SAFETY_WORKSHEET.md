# OpenReclaim Data Safety Worksheet

Last updated: May 22, 2026

This worksheet is based on the current app code in this repository.

## Current release assumptions

- no `INTERNET` permission
- no account sign-in
- no analytics SDK
- no ads SDK
- no remote backend
- no push notifications
- no calendar account integration in the shipped app

## Likely Google Play Data Safety answers

### Does the app collect or share any of the required user data types?

Recommended current answer:

- `No`, assuming the release build remains local-only and does not send task/reminder data off device

Reason:

- app data is stored only in local Room storage
- no network permission is declared
- no sharing path exists in the current release code

### Does the app process user-provided data on device?

Yes, on device only:

- tasks
- reminders
- schedule metadata
- recurrence settings
- time periods

This does not count as remote data collection if it stays on device.

## Recheck required if any of these are added later

If you later add any of the following, redo the Data Safety form:

- real calendar sync
- notifications backed by a cloud service
- crash reporting
- analytics
- authentication
- cloud backup or sync
- AI API calls

## Release owner checklist

- confirm the release manifest still has no network permission
- confirm no SDK was added that transmits analytics or crash data
- confirm privacy policy matches the shipped behavior
- answer the Play Console Data Safety form from the actual release artifact, not memory
