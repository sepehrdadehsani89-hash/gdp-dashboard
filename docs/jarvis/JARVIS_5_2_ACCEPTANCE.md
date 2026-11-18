# Jarvis 5.2 physical acceptance record

**Execution status: NOT RUN.** This cloud workspace has no connected Redmi 9, Android emulator, native Windows host or owner live provider/Google credentials. Every row below is a pending acceptance procedure, not a passed test. Compiled Android tests, JVM policies, local TLS fixtures and Linux Chromium are reported separately. Do not fill these rows from software fixture results.

## Required real Redmi 9 → Windows pairing sequence

| Step | Procedure | Actual result |
|---|---|---|
| 1 | Start the actual Windows companion with its preserved private identity | NOT RUN |
| 2 | Generate a fresh private invitation on Windows | NOT RUN |
| 3 | Display its actual QR on Windows | NOT RUN |
| 4 | Open the existing Jarvis installation on the Redmi 9 | NOT RUN |
| 5 | Tap Scan computer's one-time QR | NOT RUN |
| 6 | Observe a real scanner opening | NOT RUN |
| 7 | Scan the displayed QR | NOT RUN |
| 8 | Verify full decoded invitation length without exposing its secret | NOT RUN |
| 9 | Verify actual name/address/pin/protocol/expiration | NOT RUN |
| 10 | Tap Request Pairing and observe explicit progress | NOT RUN |
| 11 | Observe the real matching pending request in the Windows terminal | NOT RUN |
| 12 | Compare the actual six-character code on both devices | NOT RUN |
| 13 | Approve the actual ticket on Windows | NOT RUN |
| 14 | Finish pairing on Android | NOT RUN |
| 15 | Verify Saved Computers becomes 1/20 without losing other saved entries | NOT RUN |
| 16 | Perform the authenticated signed capability check | NOT RUN |
| 17 | Review and run one authorized browser command | NOT RUN |
| 18 | Inspect the actual Windows browser result and verified receipt | NOT RUN |

## Negative pairing acceptance

These are at least 20 distinct real-device failure/recovery cases. Some have additional automated policy/HTTPS coverage; that does not execute this physical procedure.

| Case | Procedure | Physical result |
|---|---|---|
| N01 | Primary scanner initialization and real camera output | NOT RUN |
| N02 | Scanner user cancellation without automatic retry | NOT RUN |
| N03 | Unavailable/busy physical camera and retry | NOT RUN |
| N04 | Unavailable Google Play services with bundled fallback | NOT RUN |
| N05 | Camera permission denied and legitimate settings assistance | NOT RUN |
| N06 | Non-Jarvis QR format | NOT RUN |
| N07 | Expired invitation | NOT RUN |
| N08 | Malformed encoding/JSON/schema | NOT RUN |
| N09 | Empty/missing/truncated payload | NOT RUN |
| N10 | Incorrect certificate pin | NOT RUN |
| N11 | Offline companion | NOT RUN |
| N12 | Unreachable LAN address | NOT RUN |
| N13 | Different subnet/guest Wi-Fi isolation | NOT RUN |
| N14 | Unsupported protocol version | NOT RUN |
| N15 | Invitation replay by a different requester | NOT RUN |
| N16 | Identical legitimate request retry and duplicate-pair reconciliation | NOT RUN |
| N17 | Desktop approval timeout | NOT RUN |
| N18 | Explicit desktop denial | NOT RUN |
| N19 | App backgrounding cancels temporary work | NOT RUN |
| N20 | Activity recreation without saved-state secrets | NOT RUN |
| N21 | Screen rotation preserves active coordinator | NOT RUN |
| N22 | Connectivity lost during request/finish | NOT RUN |
| N23 | Client process restart reuses saved permanent trust | NOT RUN |
| N24 | Wrong hostname or expired/not-yet-valid pinned TLS certificate | NOT RUN |
| N25 | Stop All Listening cancels stale camera-permission grant callbacks | NOT RUN |
| N26 | Flashlight capability/readback on actual Redmi camera | NOT RUN |
| N27 | Gallery decoding a real selected QR image | NOT RUN |
| N28 | Unpaired health succeeds without an authenticated indicator | NOT RUN |
| N29 | Saved pairing while offline is not green live proof | NOT RUN |
| N30 | Revocation removes actual authorization | NOT RUN |

## Wider physical and owner-account acceptance

| Case | Procedure | Actual result |
|---|---|---|
| P01 | Update the actually installed package without uninstall/data-clear and inspect retained settings | NOT RUN |
| P02 | Inspect installed package/version/signing certificate on Redmi | NOT RUN |
| P03 | Launch without startup crash | NOT RUN |
| P04 | Hear the selected TTS voice and actual AI answer | NOT RUN |
| P05 | Observe microphone release after farewell and Stop All Listening | NOT RUN |
| P06 | Test actual wake/foreground permissions on MIUI | NOT RUN |
| P07 | Select and persist the actual live wallpaper across restart | NOT RUN |
| P08 | Measure original/default composition visually | NOT RUN |
| P09 | Apply Studio drag/rotation/scale/z/gradient profile on home screen | NOT RUN |
| P10 | Verify private widgets disappear immediately on lock/permission revocation | NOT RUN |
| P11 | Measure Balanced/Saver/High frame timing, memory, CPU and thermal behavior | NOT RUN |
| P12 | Test Silent wake exception and reduced-motion awakening | NOT RUN |
| P13 | Confirm Deep cancels work and does not automatically restart wake | NOT RUN |
| P14 | Confirm manual opening reactivates without automatically capturing microphone | NOT RUN |
| P15 | Verify configured reminder exception and DND readback | NOT RUN |
| P16 | Perform actual OpenAI inference with owner credential | NOT RUN |
| P17 | Perform actual Gemini/Claude/Perplexity inference with their authorized credentials | NOT RUN |
| P18 | Choose Google account with configured owner Android OAuth client | NOT RUN |
| P19 | Perform actual Gmail unread/search/previews using granted scope | NOT RUN |
| P20 | Perform actual Calendar/Drive/Tasks reads and account switch/revocation | NOT RUN |
| P21 | Review one bounded private AI summary and verify selected destination | NOT RUN |
| P22 | Verify real Android notification events with explicit source consent | NOT RUN |
| P23 | Run actual flashlight/volume/app/system-settings controls | NOT RUN |
| P24 | Run and inspect the native Windows PowerShell launcher | NOT RUN |

Keep private invitations, comparison codes, tokens, API keys and personal content out of public evidence. Record timestamps, exact redacted errors, versions and the physical platform used. For a failed update preserve the existing installed application and its storage; capture the actual Package Manager rejection code as described in [troubleshooting](JARVIS_TROUBLESHOOTING.md).
