# Universal AI Brain Manager

## Recoverable baseline

Repository `sepehrdadehsani89-hash/gdp-dashboard`, branch `jarvis-next`, clean initial HEAD `7ac5e33efab40bb8dbfd27bacce1cc7ed65c2954`, matching GitHub. Local checkpoint `checkpoint/brain-manager-start-7ac5e33` preserves the verified voice/caption/rendering release. Earlier rollback branches/APKs remain intact.

The new directive is preserved verbatim as `BRAIN_MANAGER_DIRECTIVE.md`, SHA-256 `7f13dd26df6fbfa87a6afa7e3121e4f03163cc4ae62343226aa0bf9f33320322`. The original24-part specifications and earlier directives are retained.

Baseline `:app:testDebugUnitTest :app:compileDebugKotlin` completed **BUILD SUCCESSFUL in56s**, all26 tasks up-to-date. This reused the verified789-case baseline (788 passed/one optional private-export fixture skipped); it did not execute new tests or prove device behavior. Log: `TEST_EVIDENCE/BRAIN_MANAGER/baseline-build.txt`. Existing JDK21/Gradle8.9/AGP8.7.3/Kotlin2.0.21/SDK35 toolchain reused. Approximately51GiB available disk,8GiB memory limit. No new dependencies/models/emulator installed.

## Existing architecture

`data/LlmClient.kt` and `BrainProtocol.kt` already support OpenAI Chat Completions, Anthropic Messages and Gemini through cancellable HTTPS requests. `AssistantRuntime.generate` routes actual recognized voice through those adapters and existing Fish Audio/caption playback. Provider keys are separate AES-GCM/AndroidKeystore entries in `JarvisStore`; Firebase is absent. A separate app identity system is unnecessary for the requested local public-client architecture.

The Local Brain is native llama.cpp inference with managed downloads only under `filesDir/local-brain`, including the Balanced subdirectory. Current downloader already pins approved model metadata/revision/SHA/size. Removal currently rejects the selected profile; integrity/readiness and concurrent lifecycle need targeted improvements. No managed inference GGUF model is installed in this workspace; vendored tokenizer fixtures are not app-installed brains. Speech, speaker, OCR and native runtime assets must remain independent.

Current provider verification is transient Settings UI state and primary selection is not gated by a durable verified connection. Explicit provider disconnect and per-provider verification lifecycle need implementation. Existing Gemini support will remain available alongside the three requested manager cards.

## Current official authentication research

The official [open-source Sign in with ChatGPT flow](https://developers.openai.com/siwc/token-sharing-open-source/sign-in) documents a public-client dynamic registration entrypoint and loopback callback, separate identity/plan scopes and signed-ID-token validation. The project has a GPLv3 license and runs locally; that fits the documented client category, but Android browser/runtime compatibility and actual account eligibility still require real authorization. No first-party client ID, browser cookies, unofficial endpoint or invented credential will be reused.

Plan-authorized inference uses the documented [Responses API contract](https://developers.openai.com/siwc/token-sharing-open-source/models-and-inference): account-specific catalog, bearer OAuth, nonstored streaming input, and terminal completion validation. The API-key route remains an explicitly separate choice. Anthropic's official supported method and requirements are being checked before implementation. No real account authorization or live paid inference has been performed.

## Implemented integration

The additive `brain` module provides a private Brain Manager activity within the existing dark/cyan interface. It uses the existing holographic header and links from Settings → Artificial Intelligence. The official OpenAI SIWC monochrome mark is included with pinned provenance and brand notice; no DevKit application code is copied. The first sign-in acknowledgement and official usage-settings link explain authorized plan use.

OpenAI authorization is application-owned and survives activity pause: bound numeric-loopback callback, fresh state/nonce/PKCE, callback-issued registration, signed RS256 identity validation against fixed official JWKS, authoritative granted scopes, account/client/subject binding, encrypted per-install storage, serialized refresh and remote revocation. Failed initial exchange retains only the encrypted host-bound registration ID for a fresh attempt. Pending codes, verifier and nonce are not persisted. Healthy refresh preserves grant revision; reauthorization, scope/account changes and disconnect invalidate proof. Undocumented refresh eligibility fields are ignored rather than assigned invented timestamp semantics.

ChatGPT plan inference uses the public Responses API and account-specific `.models` catalog. It sends nonstored streaming text requests, requires a valid terminal completion, classifies known plan/API errors and rejects provisional/failed/incomplete streams. API access remains an explicitly separate mode. No identity-only state, model list or saved credential creates inference proof, and no paid fallback is automatic. Images through this plan adapter are explicitly unsupported rather than silently omitted.

Claude uses a personal supported Anthropic API credential, public Messages API, documented bearer authentication/version and actual response verification. Consumer Claude login is not reused. OpenAI API and existing Gemini routes remain; the previous `models/` Gemini ID form remains accepted without allowing additional path segments. Provider proofs bind the exact model, credential/account/grant revision and time. New primary selection requires matching real inference; existing configured API providers can continue responding and earn proof from an actual answer.

The main recognized-voice generation path now calls the manager and retains existing Fish Audio, caption, privacy, history and cancellation behavior. Provider/account changes clear transient context and cancel stale turns. Legacy local-selection controls route to verification, and old Settings primary selection shares local hardware/profile checks.

Local model management counts only catalog-owned files, verifies actual SHA-256, supports repair and the existing revision-bound resumable downloader, and tests native inference before selection. Removal closes admission to whole operations, cancels and joins hashing/loading/download/generation, takes locks in a fixed order, releases the native handle and deletes only the explicit allowlist. Selected/all model removal is supported without deleting voice packs, keys, settings, databases, user files or benchmark records. Bundled APK/native runtime bytes are not represented as independently removable.

No dependency versions, Firebase configuration, emulator, actual provider account, production signing key or managed LLM weights were added. The original assets/native/rendering source and earlier APKs/checkpoint branches remain retained.

## Verification attempts and limits

Raw logs remain outside Git in `/workspace/work/jarvis-brain-manager/`; tracked copies strip only trailing console whitespace. The first compilation caught a settings-dialog syntax error. The first suite found three invalid JUnit method-return declarations. A later run used an older compiled auth interface after its matching test changed. The third run executed1,058 cases:21 failed, one skipped. Three failures covered changes made after that main compilation snapshot (legacy Gemini prefix and two top-level Responses errors);18 negative JWT cases found an incorrect test assumption that coroutine exception recovery cannot add a sanitized cause. The corrected assertion checks every cause for the fixed safe type/message and absence of token data. Failure XML/logs are preserved. Final outcome is recorded below after the frozen-source build completes.

`DEVICE_AND_LIVE_CASES.csv` lists all30 requested end-to-end scenarios as BLOCKED. No authenticated provider inference, real local native-model inference, Android Keystore operation, microphone/speaker playback, wallpaper/core GPU run or Redmi9 compatibility/performance test has been executed here. Existing Android test sources can be compiled, but compilation is not execution. A direct ADB enumeration attempt failed because its default user directory was read-only; no authorized phone connection or emulator was supplied. No emulator installation was attempted.

## Completed software and artifact verification

The final frozen-code build completed **BUILD SUCCESSFUL in14m45s**,68 tasks (20 executed/48 up-to-date). Main compilation reused its successful result from the preceding run; the corrected tests compiled and executed. Lint, Android-test Kotlin compilation, cached Android-test Java compilation and APK assembly completed. The fourth run's one remaining failure was a duplicate no-cause assertion in the JWT redirect test; its replacement checks the actual HTTP request count while the shared helper still checks the entire sanitized cause chain. All failure reports remain preserved.

- **1,058 JVM cases:1,057 PASS,0 FAIL,1 SKIPPED**. The skip is the existing optional private-export fixture. There are269 additional cases over the789-case baseline; counts are not additive across repeated runs. New suites: manager58, auth lifecycle50, signed identity27, plan protocol37, HTTP routes32, error classification21, owned-file removal/integrity34, operation admission/join10. All269 additional cases passed using mocks/generated keys/loopback sockets/temporary files.
- Lint: **0 errors/fatals,55 warnings,1 information**. Seven new warnings concern capitalization of private Unit-returning composable names;48 previous warnings remain. No warning was represented as a pass or hidden with suppression.
- All existing Android-test sources compiled, including the20 rendering fixtures. **Zero Android tests executed**. The30 requested live/device scenarios remain BLOCKED in their own CSV.
- **20 APK artifact checks PASS**: package/version/SDK/labels/component isolation/signature/alignment/CRC/ABI/previous asset retention/size. All14 packaged native libraries are AArch64. All134 native-library/asset entries from the previous voice preview were preserved byte for byte. The compiled manifest also confirms the Brain Manager activity is not exported, backups remain disabled and cleartext traffic remains disabled.
- Existing source asset/native/rendering files and dependency versions were retained. The two terminal-blank-line-only source/test cleanups are documented separately in `EOF_FORMAT_ONLY.json`; no code or executable line changed after the final application's compilation.

Evidence is under `TEST_EVIDENCE/BRAIN_MANAGER/`: per-case CSV, original JUnit XML, actual failure logs/XML, final build log, lint report, source hashes, compiled manifests, certificate/ZIP checks, official integration research, logo provenance and blocked device acceptance cases. These are software/mock/artifact results, not authenticated service or physical-device success.

## Verified preview and publication checkpoint

Source checkpoint: `2b73bbaf576166f84049969f03bdc9f732c55ba5`. Baseline rollback: `checkpoint/brain-manager-start-7ac5e33` at `7ac5e33efab40bb8dbfd27bacce1cc7ed65c2954`; all earlier checkpoint branches and APKs remain intact. The following artifact/evidence commit records the final download folder without changing application code.

Artifact: `downloads/brain-manager-2.11.9-debug/Jarvis-2.11.9-brain.1-debug-arm64.apk`.

- Version **2.11.9-brain.1-debug**, code29; package **com.jarvis.next.phoenix**; label **Jarvis Phoenix Test**; ARM64; minimumAPI29/target35.
- **55,580,535 bytes**; SHA-256 **ecd1fd92c6360d427a4ddbf7368066a14475ab9b4c7dab4fc846a2bf36f32698**. The copied download matches the assembled APK checksum.
- Verified preview certificate SHA-256 **f9f13af1f84515b787b89c0f3abc8695f1d0d45e8a647d1192966c4c810964c7**, matching the previous Phoenix/voice preview. This is a debug-signed evaluation update for that isolated package. The unavailable original production signing key is not replaced; no production-signed upgrade or original-package data migration is claimed.

ChatGPT actual account/workspace eligibility, official Android browser return, live model inference/usage limits/revocation, Claude personal API authorization, Android Keystore behavior, real GGUF reinstall/native generation, microphone/Fish playback/captions, wallpaper/core GPU behavior and Redmi9 performance remain unverified on hardware. No real model weights were removed/reinstalled here and no phone storage-freed measurement is claimed. Claude/Local cards use text provider identity; additional provider artwork/motion remains a visual-review item in the requirements matrix.

At preparation no Brain Manager GitHub push/publication has been performed. The owner's saved Phoenix directive requires confirmation before pushing or publishing. A concrete normal fast-forward command is provided with the final checkpoint; the eventual immutable public APK URL must be independently downloaded and SHA-256 verified after authorization. No new public download URL is claimed before that check.

Live GitHub `jarvis-next` and the fetched HEAD were both verified at `7ac5e33efab40bb8dbfd27bacce1cc7ed65c2954`; this is an ancestor of the new local source checkpoint. The old `origin/jarvis-next` local tracking reference remains at0d57181 because the configured fetch refspec tracks onlymain; it was not used as evidence of current GitHub state. No unexpected remote commits were found.
