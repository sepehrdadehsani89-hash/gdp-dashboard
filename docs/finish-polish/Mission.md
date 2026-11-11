# Universal Mission Control implementation

The existing private-LAN companion and pairing are extended; the original Android HUD, wallpaper, signing identity and stored pairing are retained. The app supports **one selected, explicitly paired computer**, with an encrypted friendly label bound to that device ID. No new relay, Google identity, arbitrary remote shell or covert capture is introduced.

## Actual behavior

`MissionController` exposes a typed state stream and `execute(DesktopRequest, confirmed, expectedDeviceId, expectedSessionId)` returning `MissionOutcome.verified`, `message/detail` and `errorCode`. The shared action executor must call this path only after its plan review. The settings Activity shows actual readiness and progress, permits an explicit local nickname, opens the existing pair/revoke flow, and provides a reviewed Chrome search test. Android continues to own the microphone and TTS; the desktop needs neither.

Before a new command, the controller requests an authenticated `/v1/capabilities` probe. Its HMAC manifest is bound to the exact random request nonce, paired device, client, session and sample time. Chrome's presence is checked through the existing allowlisted OS locator without launching it; the response contains no executable path or token. Only Chrome open and Google search report `REQUIRES_CONFIRMATION` when present. Missing Chrome reports `UNAVAILABLE`; file transfer, documents, arbitrary apps, screen capture and remote shell remain explicitly `UNAVAILABLE`. This is readiness metadata, never proof an operation executed.

Older companions remain compatible: a `ROUTE_NOT_FOUND` capability response triggers the existing identity-checked health probe and displays **legacy readiness unverified**. Existing Chrome commands can still be approved and require the same original signed execution receipt. The app does not pretend the old agent checked installed applications.

The flow distinguishes:

- `CHECKING` and `CONNECTED/OFFLINE/AWAITING_PAIRING`: actual readiness checks.
- `NOT_SENT`: no usable pairing or known unavailable capability.
- `REQUESTED/AUTHORIZED`: reviewed action and local authorization, without claiming execution.
- `SENT`: OkHttp confirms the actual command request body was transmitted. Acceptance/completion is still unknown.
- `RUNNING`: the authenticated receipt endpoint reports that this command ID is processing. This is optional; fast commands may finish before it is observed.
- `VERIFIED`: only the original authenticated, identity/body-hash-bound receipt and loaded Chrome document postcondition can set this result.
- `FAILED/CANCEL_REQUESTED/CANCELLED`: actual failure or bounded cancellation. An already opened page cannot be undone. Timeout/connection loss can leave completion unknown; inspect the computer before approving a new command.

The reviewed plan supplies its device/session identity, and standalone callers snapshot the pairing at entry. A changed identity before readiness or dispatch is rejected before sending. Command IDs, nonce/expiry protection, rate limiting, idempotence, TLS certificate pin plus SAN/validity validation, desktop explicit pairing approval, dedicated Chrome profile and postcondition verification are retained. Capability probes have an additional per-client rate limit. No private query is added to a persistent mission history.

## Verification

**36 Python tests passed** in the controlled cloud run: pairing, authorization, expiry, replay, duplicate idempotence, scoped receipts, rate limiting, revocation, malformed/unauthorized capabilities, signed nonce-bound metadata and actual local HTTPS. The isolated real Chromium fixture verified capability discovery without browser launch, an actual page load receipt, duplicate no-repeat execution and an actual HTTP-404 failure. Evidence is stored in `docs/finish-polish/companion-browser-results.json`; no Google results page or personal browser profile was used.

Twelve Android JVM policy tests cover nonce/signature/identity/session/freshness validation, supported/unavailable capability decisions, malformed/duplicate manifests, reviewed device/session binding, invalid timestamp overflow, stale progress, terminal/cancelled job protection, monotonic progress and friendly-name validation. They await the root Android pipeline result; this document does not label unrun Android tests as passed. Android runtime behavior requires physical acceptance.

| Platform / capability | Status |
| --- | --- |
| Linux private HTTPS + controlled headless Chromium | Verified fixture; production retains browser sandbox |
| Real Google search and phone voice → desktop → audible phone | NOT RUN |
| Windows 10/11 Chrome locator/launcher | Existing source adapter; native acceptance NOT RUN |
| macOS Chrome locator/launcher | Existing source adapter; native acceptance NOT RUN |
| Native EXE/macOS signed installer | NOT BUILT; manual CI recipe retained |
| Multiple stored computers / selection between devices | Not implemented; existing single selected target retained |
| VS Code, arbitrary documents, approved-folder search, file transfer | Not implemented; unavailable manifest entries |
| Internet relay, remote shell, hidden screen/audio monitoring | Not implemented; prohibited by default |

## Setup / update

Use the existing companion launchers on the same reachable private network. To gain authenticated readiness discovery, stop the visible old companion, replace the source files with this release's companion source and restart with the same private state directory/address. Preserve the private TLS certificate/key and pairing database; do not reset or publish them. The dependency list and existing explicit QR/code approval remain. Old phone versions continue to use their unchanged command route.

On Android, open Mission Control, check current connection/capabilities, optionally save a friendly name and review the Chrome test. Stop requests best-effort cancellation; disconnect/revoke uses the existing companion settings. No action is automatically triggered merely by opening the page or by a prediction.
