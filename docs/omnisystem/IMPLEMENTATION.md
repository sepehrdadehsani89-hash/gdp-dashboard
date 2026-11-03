> Historical checkpoint. The later beta implementation and evidence are described in [ACCEPTANCE.md](ACCEPTANCE.md), [CURRENT_UPGRADE.md](CURRENT_UPGRADE.md) and [BETA_VERIFICATION.md](BETA_VERIFICATION.md). Earlier unavailable-feature statements are not the current source inventory.

# OMNISYSTEM development update (not a signed release)

Baseline: `jarvis-next` at `f110e56d30dfcc3c1a6153f19888be10fa2bb79b`. Application ID remains `com.jarvis.next`; development version is `10.0.0-dev`, code 10. This is an incremental continuation of Jarvis 7.0. It is not a claim that the full master directive is complete.

## Implemented source changes

- Presentation-only module routing for explicit English, Spanish and Persian requests. Direct app-launch requests retain their existing tool path. Module selection never grants tool permission, starts a scan, records sound or authorizes a transfer. New module panels share existing actual battery/clock/weather/reactor state, retain Assistant/Memory/Settings navigation, and request immersive system bars through ordinary Android APIs.
- Dedicated weather panel with actual current measurements, up to twelve returned future hourly entries and eight returned daily entries. Open-Meteo fields are requested and parsed, not synthesized. Missing values remain unavailable; stale source observations cannot be made fresh merely by updating their retrieval timestamp. Source timezone and retrieval time are shown. Weather-code-driven bounded atmosphere effects stop while invisible and respect reduced motion. Existing city/approximate-location consent paths remain.
- Battery, Wi-Fi, Bluetooth, network and system panels connect to existing Android readers and reviewed controls. Module-specific Infinity controls expose the appropriate existing operations. Bluetooth discovery, BLE battery reads and the roughly 5 MiB Cloudflare transfer still require explicit interaction/approval. Actual discovery RSSI is plotted as signal strength, without implying distance or direction. The speed-test panel charts measured download byte/time samples (including HTTP setup), explicitly distinguishing partial observations from a completed test. No fake radar positions, throughput chart points or connection successes are created. A clock dial uses the actual device time; system telemetry adds actual app-volume storage and Android thermal status.
- Fresh on-demand location acquisition replaces the old saved-fix-only phone answer. It requires permission and enabled providers, checks monotonic age, coordinate range and reported accuracy, times out after twenty seconds, and removes listeners on cancellation/completion. It does not substitute an old location or make a geocoding request. No separate location history is created; ordinary conversation-history settings still apply to answers.
- A private CameraX front/rear preview activity, permission gate, explicit still capture, decoded thumbnail and explicit attachment to the existing Assistant/photo pipeline. Selecting the Camera panel alone does not start it. The camera temporarily pauses voice capture without cancelling model downloads or disabling the user’s wake preference; only already-running wake listening is resumed on close. The camera stops with its visible activity; unused captures are removed on close. Explicitly attached cache captures remain available to the next request; captures older than 24 hours are cleaned on the next capture. Existing provider/privacy controls determine subsequent processing. No automatic upload, video recording, face identity or medical diagnosis is implemented.
- Removed SYSTEM FREE, SYSTEM TOTAL and APP PSS readings from the normal HUD. No RAM refusal, admission threshold or low-memory cancellation was restored. Existing local model files, selection/installation separation, thermal/integrity/storage/ABI protections, ten-second voice windows and data stores remain.
- Release signing now requires the exact original certificate fingerprint, not merely the presence of signing files. No replacement release key is generated.

## Still unfinished or unverified

This is not the completed Jarvis 10.0 release. In particular:

- Licensed built-in music fingerprinting, Spotify OAuth/library saving and six-person speaker embeddings/enrollment are not implemented. The music panel explicitly reports unavailable and records nothing. These require verified provider/model licensing, configuration and testing; no credentials or enrollment samples were supplied.
- Location has no embedded map, reverse geocoding, saved destinations or location-history UI. The phone answer reports an actual permitted fix or an unavailable result.
- Directional Bluetooth visualization, verified timer countdown state from an external Clock app and comprehensive new module sound assets are not implemented. Existing real reactor/audio effects and real transfer results remain; no decorative measurements are substituted.
- Camera stills connect to the existing selected-image/OCR workflow. A new general visual-description/comparison engine is not implemented.
- Not every new technical control is translated. Existing language handling and Persian RTL are preserved.
- No local weight inference, multilingual model-quality, speaker/acoustic, sustained battery/thermal or Redmi 9 performance benchmark is claimed. No physical phone or real Bluetooth accessory is attached. Existing `libvosk.so` 16 KiB-page compatibility remains unresolved.
- The original private signing key/backup is absent. No upgrade-compatible signed update or new release/download URL can be delivered until private recovery and verification. See [private recovery](SIGNING_RECOVERY.md). No uninstall or data reset is a workaround.

## Reproducing current-machine checks

A tested local build helper is retained outside the repository at `/workspace/android-toolchain/build-jarvis.sh`. It selects the restored full Temurin 21 JDK, Gradle home, Android SDK and inherited proxy/CA trust. Use the existing isolated checkout; do not create a worktree unless explicitly requested.

The repository pins Gradle 8.9 (wrapper SHA-256 checked), SDK/build-tools 35, NDK 26.3.11579264 and CMake 3.22.1. New CameraX dependencies are 1.4.1 (AndroidX Apache-2.0). The helper uses two Gradle workers and in-process Kotlin compilation so the software emulator can share the four available CPUs. Never disable checksum or TLS checks.

Current-machine validation results are recorded separately in `VERIFICATION.md`; historical Jarvis 7.0 results do not establish this update's readiness. Debug artifacts are for isolated emulator/development checks, not an upgrade for the installed signed phone app.

The validated current-instance command is:

```sh
/workspace/android-toolchain/build-jarvis.sh :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

For emulator artifacts, add `-PjarvisAbis=x86_64`. Keep the ARM64 phone artifact separate before that command overwrites the debug output path. UI tests used the retained `jarvis-api29` AVD, software acceleration, `-gpu swiftshader_indirect`, `-cores 1`, `-memory 2048`, and emulated front/rear cameras. Live emulator processes are not part of a filesystem snapshot and must be restarted for later UI checks. Keep TLS/signature/checksum checks enabled and never use a debug APK to replace the installed signed phone app.
