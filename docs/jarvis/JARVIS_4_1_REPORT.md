# Jarvis 4.1 engineering checkpoint

Status: **local verification complete for a debug preview; handset and live-account acceptance remain outstanding.** This report records verification before GitHub publication. The starting published checkpoint is `62d3e8e86146157b6f35d648977acb971d0985d7`; local rollback `checkpoint/jarvis-4.1-start-62d3e8e` is retained along with the earlier recovery checkpoints.

The supplied 4.1 directive is preserved in [JARVIS_4_1_DIRECTIVE.md](JARVIS_4_1_DIRECTIVE.md). The pre-change assessment remains in [JARVIS_4_1_AUDIT.md](JARVIS_4_1_AUDIT.md). Existing releases and audit/reference material have not been replaced.

## Repairs and additions

| Feature group | Implemented behavior | Verification boundary |
| --- | --- | --- |
| Provider persistence | Status refresh uses saved metadata without renewing OAuth repeatedly. Explicit selected-plan questions can restore inference after restart. Bounded reconnect reacts to foreground/validated network events without paid inference loops. | JVM lifecycle and mocked HTTP; no live credential in this workspace |
| Rotating OAuth grants / account controls | Started token rotation commits despite caller background cancellation; account/generation fences prevent resurrection after removal. Saved connections can be removed locally without deleting actual provider accounts. | Mock authorization server and encrypted-storage contract tests; no real browser login here |
| App discovery and dispatch | Wake-prefix commands and canonical aliases use actual visible installed packages; disabled/missing launchers produce precise results. Background handoff remains subject to Android. | Pure matching tests; actual Redmi launch unverified |
| Android controls | Typed operations, capability classification, actual readbacks where supported, Android permission/settings handoffs and RAM-only diagnostics. | Parser/policy tests and SDK compilation; physical controls NOT RUN |
| Screen recording | Fresh consent, visible mediaProjection foreground service, video-only bounded MP4, exclusive screenshot/recording ownership, pending-file validation and truthful finalization. | Recording-policy tests; real MediaProjection/encoder/storage operation NOT RUN |
| Google accounts | Native Android account picker and per-service Google AuthorizationClient consent. Tokens remain transient in Jarvis; Play services owns renewable grants. Account/service/epoch fences and successful-read verification are separate from saved metadata. | Owner has no Google Cloud Android OAuth project yet; [setup required](GOOGLE_SETUP.md) |
| Gmail and Google reads | Actual read-only authorized REST requests, Gmail profile binding, metadata/previews and real UNREAD-label count. Teacher queries use a configured address; importance uses Gmail's label. Docs/Sheets/Slides/Forms require a matching explicitly selected resource ID. Unsupported private-mail queries do not fall through to general AI. | Mock REST tests; all live account reads NOT RUN |
| Project snapshots | Explicit bounded encrypted snapshots with account separation, timestamps and removal; separate from general AI retrieval. | Store/contract tests; no automatic historical indexing or cloud upload |
| Notifications | Explicit per-source consent, current active previews, lock/code/protected-app filters, distinct unavailable/disconnected/empty outcomes. | Policy tests; real notification listener on Redmi NOT RUN |
| Reminders and local calendar | Persistent approximate WorkManager reminders with revision/claim/removal fences and generic notifications. Accurate local tomorrow/DST window, optional permission-gated farewell context. Existing calendar/check-in stores are retained. | Store, parsing, concurrency and window tests; actual delivery/Calendar provider NOT RUN |
| Farewells and listening closure | 510 authored entries, 170 per language (English/Spanish/Persian), 17 categories, repetition limits, selectable style/courtesy and optional factual context. Speech completion/failure closes capture; delayed callbacks cannot resume listening after farewell. | Intent/catalog/lifecycle policy tests; physical microphone release NOT RUN |
| Command lab | Real typed pipeline submission, native-control tests, intent/route/access display, actual errors/launch results, actual recognition and pending approvals. | 24 classification cases include all 12 directive examples; typed tests remain distinct from ASR |
| 20 computers | Encrypted registry, additive legacy migration, distinct names/aliases, explicit selected target and reviewed device/session binding. No implicit replacement when the registry is full. | Registry/protocol tests; controlled Linux HTTPS/Chromium fixture; native Windows/macOS/phone round trips NOT RUN |
| Perplexity | Official Agent endpoint, explicit vendor/model, public live-catalog request without account credentials, strict completed responses, separate encrypted API access and no provider fallback. | Mock HTTP/protocol; cloud public-catalog request was network-blocked; live inference NOT RUN |

## Android capability boundaries

The dashboard uses SUCCESS, USER_ACTION_REQUIRED, PERMISSION_REQUIRED, UNSUPPORTED, FAILED and TIMEOUT. A historical successful operation does not establish current device state indefinitely.

- Torch, media volume and battery operations use actual Android APIs/readback where available.
- Ringer/DND changes require the applicable Android policy access. Locking uses the existing lock-only administrator; secure unlock uses Android authentication.
- Wi-Fi/Bluetooth changes use official panels where direct mutation is restricted. Returning from a panel requires state verification before success can be reported.
- Hotspot, Airplane Mode, global rotation/theme, reading mode, cast, sharing interfaces, focus, bedtime and wallet depend on official screens/device capabilities. Opening a screen does not establish that a setting changed or a file was delivered. Unsupported Xiaomi screens are reported.
- TV control requires an actual supported paired protocol; no working television remote is claimed.
- Google Photos is an official content-picker/app boundary; private Maps history and consumer Keep data access are not assumed. Chat spaces are distinct from message bodies; Classroom courses are distinct from grades/coursework; Meet access depends on Google's eligibility.
- The companion still implements only its approved Chrome open/search actions. Native Windows power/media/file automation, unattended administrator actions and an internet relay are not implemented.
- The original iPad Safari companion milestone and Android-to-iPad target switching are not implemented by the desktop Chrome companion. No iPadOS-restricted function is claimed.

## Preserved application and security

The original MainActivity, holographic rendering sources, native core, original resources/assets and Room message/memory schema are retained. Changes add handlers and settings entries around the existing interface. No application data is uninstalled, reset, migrated destructively or cleared. Existing published artifacts remain available.

Only one justified direct dependency was added: official `play-services-auth:22.0.0`, checked against the existing SDK 35 / AGP 8.7.3 configuration. JDK/Gradle/framework versions and existing application dependencies were not upgraded. No emulator was installed.

The new permission is Android Notification Policy access, granted only through Android. New owned activities/services/receivers are nonexported. Google's SDK revocation service retains its Google-protected revocation permission. No broad app visibility, account enumeration or unrestricted storage permission was added. Models do not authorize shell commands, grant permissions, bypass locks or choose a different paid connection automatically.

## Actual verification evidence

The first complete JVM run passed 2,033 cases with zero failures and one optional private-export fixture skip. A later run after additional acceptance checks passed 2,088 with zero failures and the same one skip. The latter release gate then failed lint on two command-lab state reads; those were corrected to use lifecycle-collected state. These are intermediate results, not a completed release claim.

The companion's first concurrent 36-case run had 35 passes and one real-browser PAGE_LOAD_TIMEOUT. An instrumented idle browser/HTTPS run passed in 3.369 seconds without changing companion production code, followed by a serial complete 36-case pass in 2.657 seconds. The timeout is retained in the evidence. Resource/timing contention is a plausible contributor, not a proven product root cause. No Windows/macOS or actual Google page success is inferred from that controlled local fixture.

Other failed compilations and interrupted gates are retained in the committed evidence history; no interrupted run is counted as passed. The final Android gate completed successfully in 13m19s. Actual results: 2,109 JVM cases, 2,108 passed, 0 failed, 1 optional private-export fixture skip; lint 0 errors / 56 warnings / 1 informational item; APK assembly and Android-test compilation passed. Android instrumented tests executed: 0. The latest complete companion suite passed 36 cases. All 30 SDK/ZIP/binary checks passed. See [raw results](TEST_EVIDENCE/JARVIS_4_1/) and the [feature-group counts](TEST_EVIDENCE/JARVIS_4_1/feature-test-summary.json). Each of the 16 listed repaired/added feature groups has at least 21 meaningful JVM cases; shared suites are not multiplied into independent device runs. [Physical acceptance procedures](JARVIS_4_1_DEVICE_ACCEPTANCE.md) are all NOT RUN here. Suite-level counts are reported directly; shared policy cases are not multiplied into per-device or per-product execution claims.

## Installation and distribution boundary

The candidate retains package `com.jarvis.next.phoenix`, the known Phoenix signer and ARM64 / minimum API 29 / target API 35, with version code 32 and name `4.1.0-preview.1-debug`. The artifact checker confirmed these binary values. APK size: 56,644,226 bytes. SHA-256: `361872bb26f569aabb9bb13a686de97c6dbdc97fec839f625f552dd2abc68152`. Signing certificate SHA-256: `f9f13af1f84515b787b89c0f3abc8695f1d0d45e8a647d1192966c4c810964c7`. All 134 previous native/asset files, including 14 native libraries, are byte-identical. The retained Vosk library still uses 4 KB ELF alignment; thirteen other native libraries support 16 KB or greater alignment. No large-page Play readiness claim is made.

The retained signer is compatible with the known previous Phoenix code 31 APK. The certificate of the application actually installed on the user's phone has not been measured, and physical update acceptance/data preservation has not been executed. No uninstall or data-clear workaround is offered. The original production package requires its original signing identity; this debug preview does not solve an unavailable original production key.

A production Google Play AAB is not prepared. Original production signing access, release hardening, native large-page compatibility and current Play policy/device requirements still need a separate verified release workflow. A debug APK is not Play approval.

GitHub publication requires the owner's confirmation under the explicit instruction in [PHOENIX_DIRECTIVE.md](PHOENIX_DIRECTIVE.md): “Request my confirmation before pushing or publishing to GitHub.” The verified local source and artifact checkpoints will be presented with the exact normal push command. A new HTTPS APK URL is reported only after an authorized push and anonymous HTTP/body/checksum verification.
