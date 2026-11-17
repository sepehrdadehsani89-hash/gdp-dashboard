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
