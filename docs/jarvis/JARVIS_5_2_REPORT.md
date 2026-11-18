# Jarvis 5.2 preview — implementation and verification

This is an additive continuation of the existing Android application, not a replacement or a claim that the complete master specification is finished. The [original directive](JARVIS_5_2_DIRECTIVE.md), [pre-change audit](JARVIS_5_2_AUDIT.md), all earlier release artifacts and rollback branches are retained. The default original composition remains selected until the user deliberately applies a Studio profile.

## A. Repository audit — IMPLEMENTED

Repository `sepehrdadehsani89-hash/gdp-dashboard`, branch `jarvis-next`, starting commit `c00e8c9c89c8fea534666dab3781faeece57016a`. Rollback branch `checkpoint/jarvis-5.2-start-c00e8c9` preserves that published 4.1 state. The audited APK was `com.jarvis.next.phoenix`, code 32. The installed Redmi package/version/certificate could not be measured. No original production signing key, owner Google project or live provider credential was available here. The previously reported private computer address was not probed or hardcoded.

## B. Existing bug root causes — IMPLEMENTED source repairs

Source inspection established silent scanner busy/launch failures, strict unwrapped invitation input with silent truncation, pairing work cancelled on ordinary activity pause, transient activity-only challenge state, non-reconcilable lost responses after consuming invitations, silent empty `pending` output and a public-health check used instead of authenticated live proof. Those are real source defects. They do **not** establish the exact camera, TLS or network failure on the user's particular Redmi/Windows session; physical diagnostics are still required.

The repair provides visible typed outcomes, complete input normalization/validation, an in-memory retained coordinator, bounded request/finish reconciliation, explicit operator status and a separately authenticated capability check. TLS pin, certificate dates/SAN, private endpoint restrictions, mutual approval, revocation, command authorization and no-redirect/no-retry network policy remain enforced.

## C. QR scanner repair — IMPLEMENTED; software coverage available; USER ACTION REQUIRED

Primary scanning invokes actual Google Code Scanner 16.1.0, QR-only. Its failure/unavailability permits a bounded real CameraX 1.4.1 + bundled ML Kit 17.3.0 fallback after Android Camera permission. The fallback owns a real preview/analyzer, bounded in-flight decoding, camera lifecycle cleanup, Retry, Cancel and supported flashlight readback. Cancellation does not automatically reopen a scanner. Gallery decoding uses actual local images; manual/paste/clear remain available. No QR image is uploaded.

New Android tests declare 20 parameterized bundled-decoder cases and 24 actual software-bitmap Studio cases. Compilation and execution are distinct. **Android tests executed: 0.** Physical scanner initialization, camera focus/flashlight/permission behavior and real phone-to-Windows acceptance have not been verified.

## D. Pairing protocol — AUTOMATED TEST PASSED

The full JVM run reports **2,569 passed, 0 failures/errors, 1 skipped** out of 2,570 cases in 142 suites. The skip is the optional private user-export fixture (`JARVIS_PRIVATE_EXPORT_TEST` was not supplied). All **28 actual production Kotlin-client → Python loopback HTTPS cases passed**, including positive approval/finalization and real pin/SAN/date failures, denial, retry, revocation, wrong signature/nonce/device/session, stale manifest, redirect, oversize/malformed response and cancellation. These are host interoperability tests, not Android/Windows device runs.

Persistent pairing, current online proof and actual command completion are distinct. Signed capability proof is bound to the current device/session and expires after 60 seconds. Temporary invitations/tickets/secrets stay in RAM and are excluded from redacted exports. Rotation retains the coordinator; actual background/Stop All clears transient work. Saved registry changes propagate after successful atomic persistence. Up to 20 explicitly approved computers remain supported.

## E. Windows companion — IMPLEMENTED; native verification USER ACTION REQUIRED

Agent 1.2 retains protocol 1, existing TLS files, database identity and the two reviewed Chrome actions. `pending` now prints actual tickets/codes/age/expiry/client suffix or an explicit empty result. `approve` distinguishes failures and duplicate approval; `deny` is available. Request retries cannot hijack a ticket; finish retries reconcile only the same original session/lifetime. Revocation invalidates pending work and temporary cached responses.

The Windows launcher reuses an existing private Python environment even without `py`, and installs only missing/mismatched pinned requirements. Best-effort `_jarvis-pc._tcp.local.` discovery contains public identity hints, never authorization. Android NSD cannot replace pins, rewrite endpoints or silently select computers. Only actual same-endpoint authenticated checks establish current proof.

The full companion suite passed all 103 tests in 9.402 seconds. The first run found a real Chromium timeout. The protocol trace showed the navigation acknowledgment and HTTP 200 arriving before Chromium acknowledged Page and Network subscriptions. The adapter now waits for subscription acknowledgments and correlates the load event and response with the requested document's frame and loader IDs. It checks the final document URL and ready state before issuing a successful receipt. The suite includes 22 browser event-ordering and error cases, plus a real Chromium fixture for opening and reopening Chrome, search on a controlled page, duplicate handling and a failed HTTP 404. Controlled Linux HTTPS, mDNS and Chromium fixtures are distinct from native Windows/macOS execution, which was **NOT RUN**. The delivery is a compatible **source ZIP**, not a fabricated native EXE. Windows files/media/power/arbitrary-app/shell actions remain **UNSUPPORTED** and are explicitly blocked from fictional AI execution.

## F. Google Cloud configuration — IMPLEMENTED; USER ACTION REQUIRED

New encrypted configuration stores Project ID, Android OAuth registration metadata and an optional restricted API key. The application displays the actual installed APK's certificate SHA-1. Native Google registration still depends on package/certificate configuration; a text field cannot replace Google registration or safely embed a web client secret. The public key test calls a fixed permitted YouTube endpoint with Android restriction headers and validates the real bounded response. Saving fields does not create green connection proof.

## G. Google OAuth — existing implementation preserved; USER ACTION REQUIRED

Official native account selection, per-service consent, Google-managed renewable grants and bounded real REST reads remain. Service tests, account switching/removal/revoke and stale grant/account fencing remain separate. No live Google account request was performed here. The owner has not configured a Cloud project/client; [the exact setup guide](JARVIS_GOOGLE_CLOUD_GUIDE.md) gives the package/SHA values and consent/API steps. Private Gmail/Calendar/Drive access cannot be granted by an API key.

## H. Gmail — IMPLEMENTED read/summary boundaries; push backend BLOCKED

Typed unread count comes only from the actual Gmail UNREAD-label response. Existing bounded search, sender/subject/previews and account-isolated local project snapshots remain. Google wallpaper observations are RAM-only, account/grant-bound, dated and expire after five minutes; locked/quiet/revoked/account-changed states hide or invalidate them. Notification counts never substitute for Gmail unread count.

An optional summary reviews one exact bounded read (8K maximum), selected provider/model/mode and separate Android authentication. Consent binds account, grant epoch, source digest and provider credential; expiration/change requires another review. No automatic cloud send, tools, general conversation/memory, fallback, persistence or auto-speech occurs. The output is explicitly an **AI draft**. Voice mail aliases route to actual reads; cloud summarization requires this separate review.

No Gmail watch, Pub/Sub topic, history synchronization or deployed backend has been created. Automatic per-email API cards, deduplication/history push and background semantic mail summaries remain unimplemented/blocked by that infrastructure and owner setup. Actual Android notification events are a different supported source.

## I. AI providers — preserved; software tests passed; live verification USER ACTION REQUIRED

OpenAI, Gemini, Claude, Perplexity and Local Brain remain. API billing is distinguished from consumer ChatGPT login. Credential/configuration fencing was extended for explicitly reviewed private Google summaries. Provider tests use controlled HTTP fixtures, not owner credentials. Current official Anthropic SDK metadata/header inspection is recorded, without installing that SDK or claiming live inference. Local Brain keeps its existing explicit local model and no cloud workspace is added. No provider's live model invocation was verified in this workspace.

## J. Android tools — preserved; physical verification USER ACTION REQUIRED

Existing typed intent/authorization/confirmation/execution/readback paths remain. New typed navigation opens actual owned configuration/Studio/mode/setup/pairing/lab screens; opening a screen does not mutate a protected setting or prove completion. Restricted Android actions use legitimate system UI. No device lock bypass, silent protected toggle, unrestricted AI shell or iPadOS capability is claimed. Existing actual flashlight/volume/battery/app/permission paths need the Redmi acceptance run.

## K. Wallpaper — IMPLEMENTED; software policies passed; physical verification USER ACTION REQUIRED

An opt-in real Studio wraps the retained WallpaperService/renderer: eight encrypted profiles, 24 owned layers, preview drag/X/Y, rotation/scale/opacity/order/visibility/alignment/snap, HEX/RGB/HSV/HSL, two-color gradients, glow/text scale, eight themes and bounded quality. Default inactive Studio delegates to the original renderer; original geometry/assets/native content are preserved. Restore Original keeps saved profiles and existing preferences.

Visible wallpapers sample local widget values once per second and observe actual source events; no millisecond cloud polling is introduced. Hidden rendering stops, Reduced Motion remains enforced, and private fields require fresh unlocked/active/source-permission checks plus explicit profile opt-in. Unknown sources are unavailable. No fabricated music titles, Gmail counts, computer metrics or unsupported telemetry are rendered. Actual Redmi FPS, thermal/battery effects, home-screen persistence and Android bitmap test execution remain unverified.

## L. Voice/microphone — preserved; physical verification USER ACTION REQUIRED

Existing recognition/voice configuration, real tool results, speech-delivery closure and the 510-entry English/Spanish/Persian farewell catalogue remain. A farewell finishes permitted speech before closing conversation capture and blocking restart. Optional wake capture is separate; Stop All Listening stops both. The real MIUI microphone indicator, audibility, recognition and resource release have **NOT RUN** here. Microphone permission cannot be granted automatically on installation.

## M. Silent Mode — IMPLEMENTED; policy tests passed

Independent preferences dim/simplify owned presentation, hide private previews and suppress optional Jarvis briefing/battery activity without overwriting saved wallpaper. Wake/reminder exceptions are explicit. A permitted wake can trigger a four-second visual awakening without delaying command processing; Reduced Motion wins. Android DND is a separate permission/request/readback flow; outside messages continue arriving. Physical DND/wake/reminder behavior remains unverified.

## N. Deep Shutdown — IMPLEMENTED; policy tests passed

Explicit confirmation and normal Android authentication gate entry. It cancels Jarvis-owned active capture/wake recovery/current AI/modules/companion requests/downloads and optional work, preserving accounts, permanent pairings, layouts, files and reminder data. Reminder exceptions follow the selected policy. It neither powers off Android nor stops unrelated apps. Manual main-app opening reactivates without automatic microphone restart. Corrupt mode preferences fail closed and explicit recovery preserves the prior raw value privately. Actual hardware release remains unverified.

## O. Performance and environment

Existing JDK 21/Gradle 8.9/SDK 35 toolchain was reused, with one Gradle worker, in-process Kotlin and a 2 GiB JVM heap on a two-CPU/eight-GiB cloud quota. No emulator or unrelated framework upgrade was installed. Device FPS, inference latency, audio round-trip latency, memory/thermal behavior and native Windows performance were **NOT MEASURED**. Cloud build durations are not phone performance metrics.

The Android build passed in 11m 26s. JVM testing, lint, debug assembly and instrumented-test compilation completed. Lint reported zero errors, 74 warnings and one informational finding. Historical failed checks are retained, including fixed scanner resource, source/test compile ordering, missing timeout qualification, Studio syntax, DNS-SD service-name length and the test harness's unavailable `Redirect.DISCARD` API. ADB enumeration was blocked by the managed executable attempting to create read-only `/home/agent/.android`; this is not an installer error from a phone and no device acceptance is claimed. A workspace-specific Android user-home override did not change that executable behavior; both errors are retained, with no further automatic attempts.

## P. APK build details

All 35 SDK and archive checks passed. The APK is 60,514,307 bytes and its SHA-256 is `cd1a5f9718763e0f2cf6d35402d5f9e503c41b100fa80fb402fb95175c832c76`. The signed package is `com.jarvis.next.phoenix`, version `5.2.0-preview.1-debug`, code 33, ARM64, min API 29 and target 35. Its certificate SHA-256 is `f9f13af1f84515b787b89c0f3abc8695f1d0d45e8a647d1192966c4c810964c7`, matching the published 4.1 preview. All 135 prior asset and native entries matched byte for byte. The new APK contains three bundled ML Kit barcode models and the ARM64 barcode library. The merged manifest adds no permissions relative to 4.1.

The preview retains the known Phoenix package/signer and advances code 32 → 33. This establishes compatibility with the known packaged 4.1 preview, not with an unmeasured installed package on the user's phone. No uninstall/data-clear occurs in this work. Actual update acceptance and retained on-device settings still require physical verification. Production `com.jarvis.next` signing identity is different and unavailable; a publication-ready production AAB is **BLOCKED**. Existing Vosk native page alignment also needs a separately reviewed Google Play compatibility milestone.

## Q. Download and publication — USER ACTION REQUIRED

Local artifacts are prepared under `downloads/jarvis-5.2-preview/` after verification. No new public 5.2 link is claimed before actual authorized GitHub publication and complete anonymous HTTPS download/hash verification. The older 4.1 link is not relabeled. The saved recovery contract [PHOENIX_DIRECTIVE.md:781](PHOENIX_DIRECTIVE.md) requires: **“Request my confirmation before pushing or publishing to GitHub.”** A concrete local source/artifact checkpoint and exact normal push command are prepared for that final confirmation.

## R. Installation instructions

Follow [setup](JARVIS_SETUP_GUIDE.md) and the artifact README/checksum. Download the actual published new APK, open it and approve Android's normal installer. Keep the existing app installed. On rejection preserve storage and obtain the actual Package Manager error using [troubleshooting](JARVIS_TROUBLESHOOTING.md); never evade signature checks by changing packages or using uninstall as a default fix. Android 12 is within the minimum API 29 range, but SDK range alone is not physical install acceptance.

## S. Remaining owner setup

Approve the exact GitHub push when the verified local checkpoint is ready; then use the verified new URL. Perform the [18-step physical pairing acceptance and negative matrix](JARVIS_5_2_ACCEPTANCE.md). Start the source companion on the actual Windows private IP while preserving its state. Configure the owner Google project/Android OAuth client, enable intended APIs and consent, then run actual service reads. Enter provider credentials only privately in the application and test actual inference/audio. Choose Android permissions, live wallpaper, notification sources, Studio profiles and mode exceptions deliberately. Production key recovery, Google push/backend and native Windows packaging require separate owner/platform work.

## T. Limits and evidence

No feature is labeled LIVE API VERIFIED or PHYSICAL DEVICE VERIFIED from these cloud tests. No native Windows installer, deployed Gmail backend, unrestricted Windows controls, production Play release or measured Redmi install success is supplied. The four guides, full raw JVM XML, per-feature test mapping, real companion logs, lint output, SDK/ZIP/signature checks and failed-run history are retained under [5.2 evidence](TEST_EVIDENCE/JARVIS_5_2/README.md). Tests are counted once; shared policy coverage is not multiplied into hardware results. Unsupported/backend-blocked parts remain explicitly recorded rather than simulated.
