# Jarvis Next for Android

**Separate installation:** Jarvis Next uses `com.jarvis.next` and a new signing identity. Keep your existing Jarvis installed; its data and permissions are untouched. This is based on published Jarvis 0.1.3, not recovered unfinished 0.1.4 work.

Jarvis Next adds explicit live tap/draft controls, direct installed-app opening, WhatsApp draft and selected-photo handoffs, persistent read-only connected folders, and a visible switch to free on-device AI. See [setup and limitations](docs/JARVIS_NEXT.md).

A new app identity does not establish Play Protect approval or fix Google's HTTP 429 quota. Keep Play Protect enabled. Local AI is a separate provider with an optional ~400 MB model download, no API key, and small-model performance/quality limits.

## Phone workflow

1. Install the signed ARM64 Jarvis Next APK after reviewing the build verification. Open **Jarvis Next**, not the original Jarvis.
2. Complete onboarding. Optional permissions are separate from the original app; choose only what you need.
3. Tap **Use free on-device AI** on Home and download the optional Qwen model in Settings for general answers without a cloud key. Initial model download requires Internet; inference runs locally.
4. Use **Folders** to connect/browse a user-selected directory read-only. Choose documents/photos to attach to your next question. **Share selected photos…** opens Android's recipient chooser; sending stays manual.
5. For live controls, enable Screen access in Settings, download offline speech, explicitly start listening, and open the target app. Say `tap Search` or `type Hello Sofia`, then confirm by voice while the same screen remains open. Sending, payment, destructive and permission controls stay manual. `open WhatsApp` directly opens an unambiguous installed app. `whatsapp draft +593991234567: Hello Sofia` proposes a recipient-owned draft, never a silent send.

[Full Jarvis Next guide and signing backup instructions](docs/JARVIS_NEXT.md) · [Pinned toolchain](docs/COMPATIBILITY.md) · [Local model](docs/LOCAL_MODEL.md) · [Third-party notices](THIRD_PARTY_NOTICES.md)

## Build and updates

Use the pinned JDK/SDK/NDK/CMake versions and Gradle wrapper. Set `JARVIS_NEXT_SIGNING_DIR` to your private signing directory outside the checkout, containing `jarvis-next.p12` and `store-password`. Never commit either file. Future updates need the same key. Save a private backup outside the cloud environment as described in the guide.

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:lintRelease :app:assembleRelease
```

Debug builds are for the dedicated test emulator only; the downloadable phone build is signed release ARM64. Older verification documents and existing `Jarvis-0.1.x` artifacts describe the original app and are historical, not verification of Jarvis Next.
