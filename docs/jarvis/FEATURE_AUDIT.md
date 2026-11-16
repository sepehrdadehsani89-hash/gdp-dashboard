# Feature audit — incremental, incomplete

222 owned Kotlin/Java files existed at the pre-repair checkpoint. Two new repair/test files do not replace any module. SOURCE_INVENTORY.json inventories every owned Kotlin/Java path (including the new helper) and manifest activity/service/receiver/provider. Inventory is not a completed semantic/security audit. Vendor C++, assets, release binaries and prior documents remain intact.

Inspected closely this milestone: foreground and offline capture, microphone lease/effects, recognition confidence/finality, inactivity/utterance assembly, wake service paths, TTS dispatch/playback, core motion policies and frame loops, paired-computer test coverage. REQUIREMENTS_MATRIX.md gives reviewed feature classifications. No physical feature is marked IMPLEMENTED_AND_VERIFIED from source presence alone.

The baseline exercise passed 201 JVM cases across voice, sentinel and power. The companion suite passed 36 cases, including real controlled Linux Chromium/local HTTPS. Source code exists for the additional packages below, but full per-feature reconciliation is pending.

## Complete owned source-package inventory

| Package | Files | Review state |
|---|---:|---|
| app | 3 | Inventoried; semantic feature audit pending |
| call | 3 | Inventoried; semantic feature audit pending |
| checkin | 3 | Inventoried; semantic feature audit pending |
| companion | 3 | Targeted source review; runtime acceptance pending |
| contacts | 3 | Inventoried; semantic feature audit pending |
| core | 11 | Inventoried; semantic feature audit pending |
| data | 3 | Inventoried; semantic feature audit pending |
| finishpolish | 15 | Inventoried; semantic feature audit pending |
| hud | 7 | Targeted source review; runtime acceptance pending |
| infinity | 8 | Inventoried; semantic feature audit pending |
| local | 5 | Inventoried; semantic feature audit pending |
| neural | 22 | Inventoried; semantic feature audit pending |
| omnisystem | 21 | Inventoried; semantic feature audit pending |
| phantom | 5 | Inventoried; semantic feature audit pending |
| pickup | 2 | Inventoried; semantic feature audit pending |
| power | 9 | Targeted source review; runtime acceptance pending |
| quantum | 2 | Inventoried; semantic feature audit pending |
| rebirth | 14 | Targeted source review; runtime acceptance pending |
| screen | 2 | Inventoried; semantic feature audit pending |
| security | 8 | Inventoried; semantic feature audit pending |
| sentinel | 13 | Targeted source review; runtime acceptance pending |
| titan | 11 | Inventoried; semantic feature audit pending |
| tools | 10 | Inventoried; semantic feature audit pending |
| ui | 5 | Inventoried; semantic feature audit pending |
| voice | 32 | Targeted source review; runtime acceptance pending |
| widgets | 3 | Inventoried; semantic feature audit pending |

## Specific defect and preservation

OfflineVoiceManager used a wall-clock-bounded drain loop followed by unconditional success whenever the request token remained current. An undrained track could therefore report completion after the wait expired. The repair gates completion on the cursor, reports timeout separately, uses elapsedRealtime, honors cancellation, and leaves cleanup in the original finally block. This is a source-level confirmed defect; it has not been reproduced on a phone. General conversation currently uses the configured Fish Audio path; this repair does not change provider selection or claim to enable a general offline fallback.

No HUD geometry/colors/particles, UI navigation, model assets, signing configuration, permissions, schemas, stored profiles or companion protocol changed.
