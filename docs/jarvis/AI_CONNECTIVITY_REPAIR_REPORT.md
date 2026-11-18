# AI connectivity repair — engineering report

Status: **software verification and APK packaging passed; live-provider and handset acceptance remain pending**. Source checkpoint `00424ba4e91419be64cef9eb8167ee82a98a66bc`. No successful live authentication or physical device result is claimed.

## Root cause and limits of the evidence

The source audit at `f7182629059dae465f05fe432a9b9423bc43f573` found four concrete defects: all API-key OpenAI models used the legacy Chat Completions body with `max_tokens`; the catalog offered non-chat models without a verified default workflow; UI refresh could erase a failed connection result and restore historical proof as green; and the retained legacy stream accepted nonempty deltas without a completion marker. JSON/network errors also lost useful categories. These mechanisms are established by the code and regression fixtures. The owner's exact rejected live HTTP request was not supplied, so no particular handset HTTP error is asserted as proven.

The repair uses the official Responses contract for modern OpenAI text models, keeps legacy GPT-3.5/GPT-4 request formats, and verifies completed output on both routes. Catalog data and inference permission remain separate. Authenticated errors and interrupted/empty/malformed output cannot create inference proof. A model may resolve to a documented snapshot alias without silently requesting a substitute.

## Backend and authentication

Backend: **Not required** for the existing direct personal API-key workflow. No backend was deployed, no health endpoint was fabricated, and no shared credential was bundled. The existing Android Keystore-backed key store is retained. This protects credentials at rest; it is not equivalent to keeping a service key exclusively on an authenticated server. The optional official ChatGPT public-client/plan authorization remains separate, including its own catalog schema, bearer grant, scopes and usage limits. Claude, Gemini and Local are preserved.

OpenAI: **LIVE PROVIDER VERIFICATION NOT PERFORMED**. The owner confirms having an API key and credits, but no runtime key is available to this cloud task. Credential availability checks record presence only. Real authentication, an accessible exact model, a successful live response and actual device speech remain unverified. No model is labeled live-verified in this report. Mock fixtures are confined to test sources.

## Implementation

| File | Purpose |
| --- | --- |
| `data/LlmClient.kt` | Cancellable Responses requests/streaming, strict modern and legacy completion validation, typed safe transport errors, GET-only retry policy, separate plan credentials. |
| `data/OpenAiResponsesProtocol.kt` | Nonstored bounded input, explicit images only, exact model/snapshot validation, completed nonempty output and authoritative terminal response. |
| `data/OpenAiLegacyChatProtocol.kt` | Preserve legacy requests and historical aliases; reject truncated, empty, malformed or mismatched chat responses/streams. |
| `data/OpenAiModelCatalog.kt` | Filter actual credential-returned catalog into text candidates; choose a default only from returned IDs. Candidate routing is not an availability or inference claim. |
| `data/ProviderRetryPolicy.kt` | Bounded backoff/jitter and Retry-After parsing for safe catalog GETs. No automatic inference replay. |
| `data/ProviderHttpErrors.kt`, `data/BrainProtocol.kt` | Safe request-compatibility and missing-credential categories; bounded status/retry/request-ID metadata without raw error reflection. |
| `brain/BrainProviderManager.kt`, `brain/ProviderConnection.kt` | Preserve actual failures; distinguish current-session success, historical proof, checking and expiry; expose diagnostic state/duration/status/retry information. A metadata-only timer expires green proof even while settings are closed. |
| `brain/OpenAiDefaultSelection.kt` | Explicit discover → configure → actual inference → select sequence; retain active selection on failure and never silently try another model. |
| `brain/ProviderDiagnostics.kt` | Redacted metadata report with Android internet observation and honest direct-key backend status. |
| `brain/BrainManagerActivity.kt` | Improve the existing API card with discovery, candidate labels, explicit verified-default action, retry timing, private key handling and diagnostics. Overall application/core layout is preserved. |
| `AssistantRuntime.kt` | Validate before saving a new key, preserve same-key retry state, expire stale proof without inference, and label already opted-in Local fallback as local in spoken and displayed output. |
| `voice/AiAnswerProvenance.kt` | No canned fallback success: require actual nonempty Local output and disclose its source. Existing voice ownership/TTS engine code remains intact. |
| `core/Models.kt` | Include resolved-model/test metadata in connection evidence. |
| `ui/JarvisAppUi.kt` | Give the existing OpenAI test UI enough time for the new bounded connection test; no layout redesign. |
| `app/build.gradle.kts` | Version increment and retained Phoenix certificate guard before signing validation. Dependencies, SDK targets and application ID are retained. |

Dedicated new test files cover Responses protocol/HTTP contracts, legacy chat completion, model catalogs, retries, explicit default selection, diagnostics and fallback provenance. Existing `BrainProviderManagerTest` adds lifecycle/expiry regressions; `BrainManagerHttpTest` updates API-key fixtures to the real modern Responses contract while retaining separate plan tests. Exact changed paths and frozen hashes are recorded with the evidence.

## Verification results

- Full JVM suite: **1,516 cases; 1,515 passed; 0 failed/errors; 1 skipped**. The existing `PrivateMemoryBackupTest.validatesUserExportPrivatelyWhenProvided` is skipped because its optional private export fixture is not supplied. No owner private file was requested merely to remove the skip.
- New cases: 219 additional JVM tests compared with the 1,297-case baseline. Transport fixtures are MockWebServer simulations; orchestration/diagnostic tests use injected local fixtures.
- Lint: **0 errors/fatal items, 55 warnings, 1 informational item**. This matches the previous stabilization counts; it does not mean warning-free production readiness.
- Gradle final gate: **PASS**, exit 0 in 11m 42s: `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin`, using the retained resource-limited toolchain.
- Android test sources: compiled successfully; **0 instrumented/device/emulator tests executed**.
- APK inspection: **23 passed, 0 failed**. Signature, alignment, package/version, minimum/target SDK, ZIP integrity, ABI, manifest preservation, production fixture exclusion and bounded key-pattern scans are recorded.
- The packaged manifest differs from the previous published Phoenix APK only in versionCode/versionName. All **134 compared native/asset files**, including the 14 native libraries, are retained byte for byte.
- Application source hashes remained unchanged throughout the final gate and match the committed source checkpoint. See [evidence](TEST_EVIDENCE/AI_CONNECTIVITY/jvm-summary.json), [feature test mapping](TEST_EVIDENCE/AI_CONNECTIVITY/feature-test-map.json), [artifact checks](TEST_EVIDENCE/AI_CONNECTIVITY/artifact-verification.json) and [verification attempts](TEST_EVIDENCE/AI_CONNECTIVITY/verification-attempts.md).

No results from a failed test-compilation attempt or an intentionally interrupted verification run are counted as passing. Raw attempts and their explanations are retained. The focused run before the final legacy/expiry gate passed 277 tests. Feature groups overlap; their counts must not be summed to derive the unique suite total.

The 20 device-acceptance cases are separately recorded as **NOT RUN**. The owner reports that the previously installed APK and holographic core work; that report is not a cloud device test. No emulator was installed and no physical installation, microphone capture, model response playback or settings-retention test was executed here.

## APK and update compatibility

Actual APK: [`Jarvis-2.11.9-ai.1-debug-arm64.apk`](../../downloads/ai-connectivity-2.11.9-debug/Jarvis-2.11.9-ai.1-debug-arm64.apk).

| Field | Verified value |
| --- | --- |
| Filesystem path | `/workspace/gdp-dashboard/downloads/ai-connectivity-2.11.9-debug/Jarvis-2.11.9-ai.1-debug-arm64.apk` |
| Package | `com.jarvis.next.phoenix` |
| Version | `2.11.9-ai.1-debug` |
| versionCode | `31` (previous published Phoenix: 30) |
| Minimum Android | Android 10 / API 29 |
| Target SDK | 35 |
| Architectures | ARM64 (`arm64-v8a`) |
| Size | 55,710,259 bytes |
| SHA-256 | `42f8a693349770b6362174a81dbd4037ecd046bc499b7d530f3cd90eaa084138` |
| Certificate SHA-256 | `f9f13af1f84515b787b89c0f3abc8695f1d0d45e8a647d1192966c4c810964c7` |
| Signer comparison | Matches previous published `2.11.9-stabilize.1-debug` Phoenix APK |
| Original production signer | Different; the retained 2.11.8 production artifact uses `1094ecc1e01dfb873a8e63689b09dc156fff9d41f77ffdfae27e702991711bda` |
| Physical update/installed certificate | Not inspected or tested by this workspace |
| Direct installation | Signed preview APK prepared with static update compatibility; actual handset acceptance remains unverified |

The checksum is also saved beside the APK. SDK inspection is performed on the actual built bytes, not a renamed prior APK.

The intended update preserves `com.jarvis.next.phoenix` and the retained preview certificate; it does not rename the package or rotate a key to bypass installation checks. Static compatibility with the previous published stabilization APK is separate from identifying the certificate installed on the handset and performing a real update. No uninstall, data-clear or migration was performed. Room, Keystore storage, user settings and core assets remain unchanged by the repair.

Google Play AAB: **not ready**. Original production signing credentials remain unavailable, the retained target SDK is 35, and the existing Vosk native library's 4 KB page alignment remains a documented Play readiness blocker. The debug preview is not a production signing identity. This task does not claim an AAB or publication.

## Checkpoints, publication and next actions

Baseline rollback: `checkpoint/ai-connectivity-start-f718262`. All existing checkpoint branches are retained. The last fetched GitHub `jarvis-next` head is `83adccdb8cee8d28b9a942438783c229c2188662`; configured tracking refs are stale because the fetch refspec is main-only, so the actual fetched `FETCH_HEAD` is used. No force push, reset, rebase or branch deletion was performed.

The new source and artifact/evidence checkpoints are local. The new APK has **not** been pushed or published, so there is **no new verified public HTTPS download URL** yet. Do not reuse the old stabilization URL as the repaired download. The saved [recovery directive](PHOENIX_DIRECTIVE.md) says: “Request my confirmation before pushing or publishing to GitHub.” A concrete non-force push command will be presented after all local work is complete.

After updating the existing Phoenix app, follow [private OpenAI API setup](AI_CONNECTIVITY_SETUP.md): enter the key on the phone, use **Discover, test & select default**, inspect the actual verified model/response, then ask an ordinary question and a follow-up and hear the response. Share only the model ID, error category and HTTP status if it fails. Do not send secrets. The repair is built and software-verified only when the final evidence below says so; end-to-end live acceptance remains pending those real checks.
