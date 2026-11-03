# Jarvis OMNISYSTEM beta 1

[Download ARM64 APK](Jarvis-10.0.0-beta1-arm64-reinstall.apk) · [SHA-256](SHA256SUMS) · [Build identity](build-info.json) · [Public certificate](signing-certificate.pem)

**This APK cannot update original Jarvis 7.0 in place.** It keeps `com.jarvis.next` and the previously retained reinstall certificate. It can update the earlier code-10 reinstall development build with that certificate. Android rejects the original signature mismatch; do not uninstall the installed original just to try this link.

Version code 11, `10.0.0-beta1-reinstall`, ARM64, Android 10+. This beta adds real optional music/Spotify, location/maps/destinations, selected-image analysis, local speaker profiles and app-owned timers. Existing data stores and core features are retained in source. Service account access, optional downloads and explicit permissions/consents are required. It is not the complete master directive or a commercial acceptance sign-off.

Before any separately chosen reinstall, privately save the old memory export, both independent signing-backup files and the APK outside the cloud workspace. Uninstalling erases app-private data. The legacy export restores document text, approved memories, tasks and eligible aliases only; it does not restore conversations, credentials, settings, models, signing identity, speaker profiles, saved destinations or timer state. Import via Settings → Neural Core → Import memories & approved documents, review the preview, then confirm. Full importer picker UI and actual-phone migration remain unverified.

See [current evidence](../../docs/omnisystem/BETA_VERIFICATION.md), [feature acceptance boundaries](../../docs/omnisystem/ACCEPTANCE.md), [service/model setup and licenses](../../docs/omnisystem/OPTIONAL_SERVICES.md), and [private migration/signing instructions](../../docs/omnisystem/REINSTALL.md). Never publish private signing backups, their unlock file, provider credentials or memory exports.
