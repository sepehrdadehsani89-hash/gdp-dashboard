# Jarvis 4.0 — Neural Core

This upgrades the existing **Jarvis**, package `com.jarvis.next`, to 4.0.0 / code 4 using the retained Jarvis Next signing key. It updates the separate Jarvis Next/Jarvis 2.0/3.0 installation without uninstalling. Original Jarvis, `main`, `jarvis-android-download`, the original Room database, credentials and the existing Lite model path are preserved.

## Start on the phone

Open **Settings → Neural Core · brain, knowledge & briefings**. New privacy/briefing controls have Spanish and Persian translations and the pane follows the selected Persian right-to-left direction. Model identifiers, diagnostics, source excerpts and some technical descriptions remain verbatim. Keep this pane visible during evaluation/downloads; Android can interrupt background work. Cancel/Stop is supported.

1. **Lite** retains the existing official Qwen2.5 0.5B Instruct Q4_K_M model. **Balanced** is an optional experimental official Qwen2.5 1.5B Q4_K_M candidate, with higher RAM requirements. **Advanced** is unavailable until a larger model and hardware are verified. No large model is downloaded automatically.
2. Use **Inspect actual size & license before download**. The app fetches official repository metadata, requires Apache-2.0, pins an immutable revision and SHA-256, and shows exact bytes and conservative RAM/storage budgets before confirmation. A failed download keeps the current profile selected. Other installed model files remain on disk. Use the installed-profile button to switch back.
3. Choose documents explicitly to index locally, or install an optional small CC0 starter knowledge pack. Ask ordinary questions with the local model, or use `search my documents: energy` to see relevant excerpts without loading a model. Indexed sources are not automatically uploaded. Cloud use of their excerpts is off by default and has its own confirmation; turning it off clears the short conversation buffers.
4. Add an app nickname using the discovered-app list. For example save `my music app` for the available Spotify package. You can also say `remember that when I say my music app, I mean Spotify` and approve the proposal. Review/delete aliases in this pane. Near matches such as `open what sap` ask before launching; unavailable or ambiguous names do not silently select unrelated packages.
5. Enable a daily briefing only if wanted. Choose local notification time, weather, news topic, authorized calendar, saved tasks and quiet hours. WorkManager can run late. Notifications contain a generic opening invitation, never private task/calendar text, and never speak automatically. Use **Prepare briefing now** to review it, then explicitly request on-device playback if desired.
6. Use the synthetic phone evaluation to record actual model measurements and responses. Weights and physical Redmi 9 hardware were unavailable in this cloud; see the audit and verification records. Neither model file size nor a successful build proves language quality or phone speed.

## The five cooperating layers

| Layer | Implementation and behavior |
| --- | --- |
| Local language model | `local/LocalBrain.kt`, bounded JNI llama.cpp CPU generation, streamed UTF-8, exact model integrity checks on cold load, single inference mutex/handle, cancellation, thermal/memory guards and idle unloading |
| Local knowledge | `neural/KnowledgeIndex.kt`: separate additive SQLite FTS4 database, bounded candidate ranking, source metadata, approved documents, optional licensed packs, approved conversation excerpts and local tasks |
| Android tools | Existing `DeviceTools`, `PhoneContext` and accessibility/capture safeguards, plus app discovery/matching, forecast context and a controlled tool manifest with bounded JSON plans |
| Optional cloud | Existing configured `LlmClient` providers; no silent local-to-cloud fallback. Explicit local-only wording overrides cloud selection. A cloud-to-local fallback requires its separate opt-in and does not restart after a partial cloud reply |
| Memory/context | Existing explicit Room memories, relevance selection, recent turns, bounded ephemeral extractive summaries, weather follow-up state, user-controlled history retention/export/delete and app aliases |

`AssistantRuntime` and `NeuralOrchestrator` coordinate these layers. Documents, source pages, captured app text, saved notes and model output are reference data; none enters the user-command route as executable instructions.

## Model audit and resource management

The [source audit](docs/neural/AUDIT.md) records the original model/runtime and the blocked baseline attempt. Lite uses the original private path `files/local-brain/qwen2.5-0.5b-instruct-q4_k_m.gguf`; Balanced has its own subdirectory and manifest. Existing working weights are retained rather than automatically migrated to a new upstream revision. Switching profiles releases the loaded LLM before another is opened.

Lite's runtime context is 1,536 tokens and Balanced's 2,048. The native tokenizer is read from the actual GGUF. Whole oldest history pairs may be omitted to fit; current user input and trusted system instructions are never silently truncated by that process. If they still exceed the limit, the app asks for a shorter input. Concise generation is capped at 256 new tokens and detailed generation at 512 when context permits. Eco uses one CPU thread; the other setting uses two. Both use mmap and CPU-only inference with bounded batches, not speculative GPU support.

RAM figures shown before selection are conservative admission estimates, not measured consumption. Lite requires at least 1,800 MiB total and 850 MiB available; Balanced at least 5,500 MiB total and 1,900 MiB available. ARM64 is required for phone profiles. Android low-memory state or severe thermal status stops generation. Idle models are released after roughly 90 seconds where no model operation is active, and on memory trim when possible. No two LLM profiles are loaded together.

Interrupted downloads retain a revision/hash/size-bound partial file. Resuming rehashes prior bytes and requires a valid full Content-Range for HTTP 206; HTTP 200 safely restarts. Final size/hash verification precedes installation. The original working file is not deleted in advance. Downloads can be cancelled. Metadata changes after inspection require a fresh review. No model weights are redistributed in the APK.

## Requests, tools and follow-ups

Examples supported by actual code:

- `Can you open WhatsApp for me?` resolves against visible installed apps; `open what sap` can propose WhatsApp for confirmation. `No, I meant Telegram` replaces the active interpretation. Nicknames require an explicit saved preference.
- `What time is it then open WhatsApp` creates a bounded plan. External actions wait for registered confirmation; execution stops at that pending approval rather than running later steps behind it.
- `Weather in Quito tomorrow, should I take an umbrella?` uses dated Open-Meteo forecast fields and a transparent precipitation threshold. Missing precipitation remains unavailable. `And the day after?` uses the current weather city/day for up to ten minutes. `What about next week?` requests day seven, not a fabricated whole-week forecast. The source's dates/timezone and retrieval time are displayed.
- `plan: check the battery` requests local model JSON classification. Only the small model allowlist is accepted, with at most three steps, text arguments and explicit current-input validation. Unknown keys, unsupported tools and malformed proposals fail closed. This small model's actual planning quality is unmeasured; direct deterministic requests remain available. Calendar/contacts and other private phone readers use their existing deterministic permission-gated routes, not model-generated authority.
- `research https://example.org/article` reads a bounded, user-requested public HTTPS text page. Private/internal destinations and redirects are refused. Extracted text is explicitly attributed and untrusted, with publication date unknown unless supplied elsewhere. It is an excerpt, not independent fact checking. News lists include real source dates/publishers, duplicate filtering and short-lived numbered-headline follow-ups. No breaking-news claim is invented.
- `add task: bring the charger` stores a local task for the briefing. It does not create an alarm. Existing calendar/clock/media/share integrations remain reviewed Android requests; sends remain manual. Successful intent dispatch is described as a request, not proof the target app completed an action.
- `offline only: explain gravity` uses the installed local model despite a configured cloud provider. A local-only request for fresh weather/news makes no network request and explains that limitation. No unavailable local model is silently replaced by a cloud call.

The registry records risk, argument fields, permissions, timeout, confirmation and verification descriptions. Trusted dispatchers execute supported requests. Pending external actions remain single-use, expiring and target-bound. The action audit keeps time, tool identifier and status, without request bodies, private source text or credentials. A returned result is distinguished from verified external completion.

## Knowledge, memory and privacy

The separate FTS index supports at most 200 source documents and bounded text/chunks/search candidates. It normalizes Persian/Arabic character variants. This is lexical full-text retrieval; it does not pretend to perform multilingual semantic embedding search. Source references have kind, title, origin, version, publication date and license. Local excerpts appear as labelled references; live information retains its actual source/timestamp.

The five optional packs are compact original CC0 starter notes for science, programming, education, language and general knowledge. They are not an encyclopedia or a broad factual-quality benchmark. Their published JSON bytes are pinned by SHA-256 in the signed app catalog. Pack updates install transactionally with version/source metadata. The language pack includes English, Spanish and Persian notes; other starter material is primarily English. Additional user-approved material can be indexed separately.

Recent conversation and bounded extractive summaries are ephemeral. Assistant excerpts are labelled unverified, not converted into facts. You can review and explicitly save a conversation reference into the local index; nothing does this silently. Existing long-term memory still requires an explicit remember request or Memory-screen save. Memory relevance is lexical and bounded, not a claim to know private thoughts or precise emotions.

History storage can be disabled for new turns; those turns then live only in the process's memory. Existing saved history remains until deleted. Optional one-, seven- or thirty-day retention is applied to saved conversations on later requests and expires matching transient context; it never deletes long-term facts. Export contains approved documents, explicit memories, aliases and tasks in a user-chosen destination. Provider credentials are excluded. Deleting indexed documents removes their retained text copies. The original Room schema is still version 1; there is no destructive migration.

Credentials remain encrypted with Android Keystore. Knowledge/history are app-private SQLite data, relying on Android's sandbox/device storage protection; this is not a claim of separate application-layer encryption for every note. The app disables platform backup as before. Exported private files need a private destination. No private signing key is packaged in resources or committed.

## HUD, voice and secure activation

The Sentinel wallpaper, original sound resources and live data remain. Actual wake, listening, model loading/generation, tool activity, speech, briefing, authorization, error and idle state changes drive shared labels. Inference has no fake percentage progress. Silent/DND/call rules remain.

An allowed wake-opened assistant uses its opaque ordinary activity and transient immersive system bars, covering launcher icons through that window. System security indicators and gestures remain Android-controlled. Home-screen/background-launch eligibility and the notification fallback remain; no known foreign live-control target is moved to force the HUD forward.

While locked, an activation notification can open a **public-only** presentation with actual clock/battery and permitted weather artwork. It does not collect private messages, memories or calendar fields. Its Unlock button uses Android's official keyguard dismissal flow and only opens the private assistant after successful unlock. It does not bypass a PIN/biometric or claim a microphone service can always launch an activity. New private Neural Core settings refuse locked entry and use FLAG_SECURE.

The **10-second command window** and fresh follow-up window after completed speech remain. Expiry returns to user-enabled wake-only detection. Stop disables capture immediately. Tap Mic/Stop to interrupt supported playback; always-on acoustic barge-in is not claimed. Compatible selected-language ASR/TTS models remain separate from the LLM. Their real EN/ES/FA speech quality still needs phone testing.

## Components and delivery

Created implementation: `neural/BrainProfiles`, `InferenceMetrics`, `Evaluation`, frozen `BaselinePromptBuilder`, `NeuralPreferences`, `KnowledgeIndex`, `KnowledgePacks`, `AppMatching`, `AppRegistry`, `ToolRegistry`, `ConversationState`, `Forecast`, `WebResearch`, `ContextPrivacyPolicy`, `DownloadResumePolicy`, `NeuralOrchestrator`, `NeuralEvents`, `Briefing`, `NeuralSettingsActivity`, `NeuralStrings` and `SafeActivationActivity`.

Modified integration: native `jarvis_local.cpp`; `LocalBrain`, `LocalPromptBuilder`, `ModelDownloadPolicy`; runtime/activity/HUD/settings/actions/wake notification; `BrainProtocol`, `JarvisStore`, existing conversation context, `WeatherClient`, `DeviceTools`, manifest and Gradle. New normal scheduling components are WorkManager and a time/timezone receiver. WorkManager contributes its normal boot/network/wake permissions; no exact-alarm, overlay, broad package visibility or new dangerous runtime permission is requested. Existing calendar/notification permissions remain contextual and optional.

Tests include the original suite plus new policy/transport cases and actual Android FTS/pack/app-alias/WorkManager/settings/native-failure tests. [Evaluation method](docs/neural/EVALUATION.md) · [Build verification and hardware limits](docs/neural/VERIFICATION.md).

The retained signing key stays in `/workspace/private/jarvis-next/`. Its private encrypted backup remains `/workspace/private/jarvis-next-signing.tar.enc`, with separate unlock file `/workspace/private/jarvis-next-backup-password`. Copy both privately outside the cloud workspace, store separately, and retain this same certificate for future updates. See [recovery instructions](JARVIS_2.md#signing-and-private-recovery). Do not generate a replacement key for the installed separate app.

No app identity change is claimed to clear Play Protect or change Google's HTTP 429 quota.

Signed [Jarvis 4.0 ARM64 download](https://raw.githubusercontent.com/sepehrdadehsani89-hash/gdp-dashboard/1b854d8584be588232be13df723c1dc3f981579d/downloads/neural/Jarvis-4.0.0-arm64.apk) · [Build identity/checksum](downloads/neural/build-info.json).
