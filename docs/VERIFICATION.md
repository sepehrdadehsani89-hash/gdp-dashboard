# Jarvis 0.1.3 verification

Results describe the built source and API29 x86_64 software emulator, not the owner's physical Redmi 9. Exact phone APK identity is in [build-info.json](../downloads/build-info.json); [verification.json](../downloads/verification.json) lists individual Android cases.

| Check | Final result |
| --- | --- |
| ARM64 APK assembly | Passed; Android 10+ / target35 / arm64-v8a only |
| JVM tests | 95 passed, zero failures/errors/skips |
| Android UI/runtime tests | 10 passed: three provider-dialog cases, six existing UI/privacy flows, one real screen-capture flow |
| Native Android OCR | Passed on actual rendered pixels |
| Native three-language neural voice rerun | Skipped: downloaded models absent from the runner's fresh installation. Historical 0.1.1 synthesis passed for English/Spanish/Persian; not a new 0.1.3 pass |
| Android total | 12 cases: 11 passed, zero failures/errors, one skipped |
| Android lint | Zero errors; 17 warnings retained, not suppressed |
| APK signature | Android apksigner v2 verification passed; same development certificate as 0.1.1/0.1.2 |
| Sensitive capabilities | Actual APK manifest retains contacts, calendar, coarse location, Accessibility and Notification Listener services |
| Live Google account | Not tested: no real API key or authenticated Gemini access available in this workspace |
| Physical Redmi 9 | Microphone, audible quality, wake accuracy, latency, battery and background endurance remain phone checks |

## What the new checks establish

The provider test makes a small non-streamed request and displays its actual sample, model and elapsed time. Fixtures verify paths/headers, empty-response rejection, known error classification without secret echo, and cancellation after the 15-second test deadline. Another test cancels ordinary AI generation if no answer text begins within 12 seconds. Virtual deadlines verify control flow, not Google's real response speed.

A throttled local HTTP response proves the first text callback arrives before the stream ends and cancellation closes the pending request. A sentence-buffer check verifies text can be queued for speech before the complete answer exists. Direct UTF-8 SSE decoding covers Persian, Spanish and CR/LF boundaries. The mock server throttles request bodies too, so its fixture keeps the request below one period; an earlier fixture delayed the request itself and was corrected.

The screenshot test enables Accessibility on the dedicated emulator after the runner installs the app, accepts Jarvis's disclosure and Android's actual MediaProjection dialog, obtains one real JPEG, decodes it, checks pixels with OCR, verifies the consent dialog is absent, and clears the retained image. It never uploads the image. An initial test enabled access before the runner's reinstall; moving that step afterward fixed test setup. Production APK code cannot grant Accessibility or capture consent silently.

Memory checks cover bounded complete general turns, durable explicit-memory UI behavior, screenshot expiry/zeroing, multilingual capture/follow-up phrases, locked-request guards, omission of saved identity from locked general requests, and private-memory speech staying on-device with cloud speech enabled. Phone-tool, screenshot and attachment turns are excluded from general cloud history.

## Limits

Gemini endpoints and sample parsing have protocol/HTTP fixture coverage; account quota, geographic eligibility and actual Google answers still require the phone's Test connection and a screen question. No successful Google connection, immediate spoken response, or Play Protect approval is claimed from an emulator run.

Screen-off support uses a visible user-started microphone foreground service and a bounded, renewed partial CPU wake lock. Locked general chat is a separate disabled-by-default option without identity/private context. Android/MIUI can stop listening; reboot and force-stop require reopening the app. A powered-off device cannot run Jarvis. Background endurance is not physically validated.

Lint warnings include deliberate ARM64 phone-only packaging, storage guidance, checked synchronous credential commits, version-catalog suggestions, an obsolete API29 guard, license text concatenation, and unused Bouncy Castle TLS helpers from PDFBox's dependency. App HTTP clients retain platform TLS validation; no trust-all manager is configured.
