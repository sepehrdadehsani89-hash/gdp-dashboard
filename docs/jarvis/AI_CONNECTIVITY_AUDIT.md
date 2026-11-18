# AI connectivity audit and repair boundary

Baseline `f7182629059dae465f05fe432a9b9423bc43f573`, branch `jarvis-next`; rollback `checkpoint/ai-connectivity-start-f718262`. The new owner directive reports a successful Redmi 9 ADB installation and working holographic core. Those are owner-reported results, not cloud device tests. They supersede the earlier unresolved installation report for the active task.

## Existing architecture

Android `app`, namespace `com.jarvis.assistant`, production ID `com.jarvis.next`, retained debug preview `com.jarvis.next.phoenix`; Kotlin/Java, Compose, JNI/native inference. Existing JDK21/Gradle8.9/AGP8.7.3/SDK35 are retained. The entry path is MainActivity → AssistantRuntime.submit/generate → BrainProviderManager → LlmClient → provider → final response validation → deliver/speak → existing Fish Audio/SpeechController, with generation-fenced microphone/TTS ownership. Native commands, local arithmetic and device facts remain explicitly native tools, not simulated cloud responses. Conversation is RAM-only by the existing privacy policy; explicit memories remain in Room and are not silently reactivated as cloud history.

OpenAI API keys are Keystore-encrypted by JarvisStore. Official optional SIWC public-client plan authorization has its own protected credential store, scopes, host/client identity and inference proof. It is a separate legitimate route under current OpenAI documentation; identity alone is not API credit or a successful model call. Claude, Gemini and Local adapters are retained. There is no AI gateway/backend deployment/configuration in this repository; the Python companion is a device bridge, not an OpenAI proxy. Direct personal API-key access is already supported and explicitly allowed by the new directive. This repair reuses it and will label Backend Not Required; no deployed backend will be invented.

## Confirmed source defects

- LlmClient routes every API-key OpenAI model to Chat Completions and BrainProtocol always supplies legacy max_tokens. Modern reasoning/Responses model requests can be rejected for that parameter/endpoint combination. The existing SIWC route already uses Responses separately. Repair API-key Responses support while retaining explicit older Chat Completions models.
- API model discovery returns every data[].id without a chat capability distinction or verified default-selection flow. Listing embeddings, image/audio or unsupported endpoint models is not proof that Jarvis can converse with them.
- BrainProviderManager.refresh converts a stored proof into green VERIFIED after process restart. It can also replace a just-published FAILED test result with CONFIGURED, because the UI refreshes after each operation. Preserve actual failures and distinguish historical proof from current-session inference.
- The retained legacy OpenAI stream also accepts nonempty deltas without a terminal finish reason. Preserve its endpoint and parameters while requiring a completed chat response; keep historical GPT snapshot aliases compatible.
- JSON/network failures frequently collapse into UNKNOWN or Google-specific generic text; the plan stream treats any early I/O error as timeout. Preserve safe status/category/duration and cancellation, and require completed, nonempty model output.

These are verified code paths. No handset runtime log or live provider failure response was supplied, so the exact customer HTTP failure is not yet established. The owner confirms possessing an API key and credits; the key is not present in this workspace. It must not be pasted into chat. Mock tests will not be reported as live connectivity.

## Scope and evidence

Preserve the installed package, retained signer, data, core/rendering and audio ownership. No new dependency, emulator, key, forced push, reset or uninstall is needed. Add real Responses transport/parsing, honest discovery/verification/status diagnostics, bounded retries only for safe catalog reads, and test fixtures isolated from production. POST inference must not be automatically replayed after ambiguous network failure. Keep TLS verification enabled and secrets out of logs/APK/Git.

Official contracts reviewed: [Chat Completions](https://developers.openai.com/api/reference/resources/chat/subresources/completions/methods/create), [Responses](https://developers.openai.com/api/reference/resources/responses/methods/create), [API models](https://developers.openai.com/api/reference/resources/models/methods/list), [SIWC models/inference](https://developers.openai.com/siwc/token-sharing-open-source/models-and-inference), [API overview](https://developers.openai.com/api/reference/overview). API-key catalogs use data/id; the documented SIWC plan catalog legitimately uses models/slug. Do not conflate their schemas or credentials.

**LIVE PROVIDER VERIFICATION NOT PERFORMED** at audit: no runtime API credential is available to this cloud task. Physical-device tests also remain unavailable unless an authorized device is actually connected. Software fixes and builds continue independently; live acceptance remains explicit.
