# Full-feature Jarvis: Play Protect review packet

## Current status

The owner reported Google Play Protect blocking the full-feature Android preview on a Redmi 9 with this warning:

> App blocked to protect your device. This app can request access to sensitive data. This can increase the risk of identity theft or financial fraud.

This warning does not reveal Google's exact detection rule. Jarvis has **not** been cleared or approved by Google. Keep Play Protect enabled. Do not rename the package, rotate certificates, repeatedly repackage the same capabilities, or use another installer to avoid the verdict.

The owner requested that screen reading, notification reading, contacts, calendar, and location remain available. All five are preserved. The unpublished reduced-access experiment was discarded; no reduced-access APK was published.

## APK and identity

| Field | Value |
| --- | --- |
| Package | `com.jarvis.assistant` |
| Version | `0.1.1` / version code `2` |
| APK SHA-256 | `c64c5a5ef6f888c4f057e6af601854960dd67abeb06d7d605546a502580d92b1` |
| Signing certificate SHA-256 | `4a53a95a949af1d6a094a6e05794c74a1562158adf346eacbe76f02c4951e265` |
| Signing | Development debug certificate; APK v2 signature verified |
| Android | Minimum API29; target35; ARM64 |
| Source commit for application code | `cb26dfc6a3b11a44cdd7129e092d384de311316c` |

[Exact APK submitted for review](https://raw.githubusercontent.com/sepehrdadehsani89-hash/gdp-dashboard/ed1fdf2f2a53a480565d288d05cd644657bce1e1/downloads/Jarvis-0.1.1-arm64.apk)

[Source and documentation](https://github.com/sepehrdadehsani89-hash/gdp-dashboard/tree/jarvis-android-download)

No new APK is represented as a fix: the file above is the same full-feature file that was blocked. Its identity and capabilities have not been disguised.

## Capability explanation for a reviewer

- Accessibility service: `com.jarvis.assistant.tools.JarvisAccessibilityService`, requiring the system's `BIND_ACCESSIBILITY_SERVICE` binding permission. It provides user-requested screen text and limited confirmed Home/Back navigation. It requires the user to enable Android Accessibility access. It skips password nodes and a protected-package baseline; those checks do not identify every possible secret or banking app.
- Notification-listener service: `com.jarvis.assistant.tools.JarvisNotificationService`, requiring the system's `BIND_NOTIFICATION_LISTENER_SERVICE` binding permission. It reads current notifications for supported user-requested summaries after Android special access is enabled. It is distinct from the app's own listening notification.
- Contacts/calendar/location: `READ_CONTACTS`, `READ_CALENDAR`, and `ACCESS_COARSE_LOCATION` support explicit, permission-gated lookup/read requests. Location is last-known approximate location, not background tracking. This version does not request contact/calendar writes or background location.
- Voice: user-started microphone foreground service with a visible notification; local Vosk wake-prefix recognition and downloaded speech models. No hidden microphone start or boot resurrection is implemented. Android restrictions and device behavior remain applicable.
- Data: conversations and explicit memories are app-private Room storage, not advertised as encrypted SQLite. API credentials use Keystore encryption; backups are excluded. Optional cloud processing requires user selection and user-entered keys. Private tool/file/memory answers deny cloud speech by default. See the full [privacy description](PRIVACY.md).
- Automation: supported app/browser/alarm/timer intents and limited confirmed navigation. This is not a general autonomous controller of every app, and payment/password-manager automation is not implemented.

These are implementation descriptions, not a claim of Play policy eligibility or Google approval. Review may require implementation changes, clearer disclosure, identity verification, or a supported distribution channel. A debug-signed preview is not a production release.

## Suggested review request text

> Please review the Google Play Protect installation block for Jarvis Android preview0.1.1, package com.jarvis.assistant. The APK download, SHA-256, certificate fingerprint, source and privacy documentation are provided above. The intended use is a user-controlled personal voice assistant. Its sensitive integrations are declared and require Android permission or special-access consent. Please assess the APK and advise which implementation, disclosure, verification, or distribution changes are required. We are not claiming the warning is necessarily incorrect or asking users to disable Play Protect.

Verify that the description matches the implementation before submission. Attach the device warning privately through Google's process; the owner's screenshot is not published here.

## Next steps that require the developer/owner

1. Open Google's [official Play Protect developer warning guidance](https://developers.google.com/android/play-protect/warning-dev-guidance).
2. Follow its current review/appeal or verification instructions using a Google account you control. Supply the exact APK details above and the warning screenshot when requested.
3. Wait for Google's assessment and address any required changes. Do not claim that rebuilding, signing, or submitting a form guarantees clearance.

No review has been submitted on the owner's behalf. The build environment could not retrieve the guidance page because its network policy denied that host; the official link was supplied by the owner. Follow the current page's instructions rather than assuming a particular form or approval timeline.
