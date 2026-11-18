# Jarvis 5.2 pre-change audit

Baseline: repository `sepehrdadehsani89-hash/gdp-dashboard`, branch `jarvis-next`, clean HEAD `c00e8c9c89c8fea534666dab3781faeece57016a`. Rollback branch `checkpoint/jarvis-5.2-start-c00e8c9` preserves it. The supplied directive is preserved verbatim in `JARVIS_5_2_DIRECTIVE.md`.

## Actual identities and environment

The last published debug APK is `4.1.0-preview.1-debug`, versionCode 32, package `com.jarvis.next.phoenix`, minimum API 29, target API 35, ARM64. Retained Phoenix certificate SHA-256 is `f9f13af1f84515b787b89c0f3abc8695f1d0d45e8a647d1192966c4c810964c7`. The application/version/certificate actually installed on the Redmi cannot be measured without an authorized device transport. Production `com.jarvis.next` has a different required signing identity; its private signing key remains unavailable. No replacement signing identity is authorized.

The cloud has an existing JDK 21 / Gradle 8.9 / SDK 35 toolchain, about 50 GiB free disk, an 8 GiB memory limit and two CPU quota. No connected physical Android, native Windows desktop or emulator is available. The previously observed `192.168.100.74:8765` is user-specific, unverified and must not be probed or hardcoded from this cloud. No Google project or live provider credential has been provided here. The existing baseline APK build is being checked before major source changes.

## Existing implementation

| Area | Actual source / behavior |
| --- | --- |
| QR screen | `companion/CompanionActivity.kt`; registered JourneyApps `ScanContract`, QR-only scanning, manual field |
| Camera | JourneyApps/ZXing 4.3.0 bundled decoding; manifest has CAMERA and merged CaptureActivity. Default activity is landscape and stateNotNeeded. No Google Code Scanner integration yet |
| Invitation | `companion/CompanionPolicy.kt`; JARVIS-PAIR-1 base64url JSON, strict private IPv4 HTTPS and certificate pin, 120-second companion lifetime |
| Android network | `companion/CompanionClient.kt`; dedicated certificate-pinned OkHttp client retains validity and hostname/SAN verification, no proxies/redirects/retry; request/complete, registry, health and signed capabilities |
| Computer | `companion/jarvis_companion.py`; source Windows/Unix launchers, retained private TLS files and SQLite identity, proof/approval protocol, only two approved Chrome actions |
| AI | `BrainProviderManager`, `OpenAiAuthManager`, `LlmClient`; API access and scoped ChatGPT plan grants separated, real model/inference verification, local brain retained |
| Google | `google/GoogleAuthorization`, `GoogleConnectionsStore`, `GoogleServicesClient`; native per-service grants and bounded actual REST reads, encrypted snapshots. Owner setup required; no Gmail Pub/Sub backend exists |
| Android tools | `controls/AndroidControlManager`, `AssistantRuntime`, existing typed command/confirmation paths and real readbacks |
| Visuals | `sentinel/JarvisWallpaperService`, `WallpaperRenderer`, `ReactorGeometry`, `HudPreferences`; native original geometry, actual telemetry and reduced motion. No complete arbitrary-layer studio exists |
| Voice | Existing delivery tracker, farewell closure gate and 510-entry catalogue; physical microphone release remains unverified |
| Tests | Published 4.1 evidence: 2,108 JVM passes, one optional fixture skip; 36 companion passes, 30 APK inspections. These are historical results, not new 5.2 verification |

## Confirmed code defects and limits

1. Scanner launch silently returns if exclusive capture is busy; launch exceptions and scanner cancellation/errors do not get a useful screen result. Manifest activity exists, so a missing declaration has not been established as the handset cause.
2. Manual invitations require exact prefix at byte zero; wrapped or surrounding whitespace is rejected. The field truncates silently at 8,192 characters instead of explaining invalid size.
3. The private module cancels its operation on every pause. Pairing secrets and ticket state are activity fields and disappear on recreation; no state model or retry/reconciliation is provided.
4. A pairing request consumes the invitation before a response reaches Android. A lost response cannot be retried safely with the same request in the existing implementation.
5. The operator's empty `pending` command prints nothing. Approval lacks distinct invalid/expired/already-approved outcomes and no explicit denial command exists.
6. The visible connection button checks a public health route; it establishes protocol reachability, not an authenticated current session. Signed capability verification already exists elsewhere and must be used for this screen.

These are established by source inspection. They do not establish which exact Android camera/TLS/network failure occurred on the user's phone. A browser self-signed-certificate warning is expected for a private identity and must not be 'fixed' by weakening Android TLS validation.

Preserve all existing working code, data stores, certificates, pairing state and prior releases. Prioritize the scanner/normalization/state/request/approval/authenticated verification chain, then integrate the remaining requested functionality according to actual platform support. Keep live-account, physical Android and native Windows acceptance separate from automated fixtures. Never represent missing backend deployment or Google project setup as completed.
