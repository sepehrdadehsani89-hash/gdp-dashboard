# Neural Core evaluation method

No downloaded weight model or physical Redmi 9 was available to this build. The official model metadata request returned proxy HTTP 403. Native compilation, policy tests and Android flow checks establish the checked software behavior; they do not establish LLM quality, energy use or phone latency.

## Repeatable phone procedure

1. Keep the installed Jarvis data and signing identity. Choose a supported profile, inspect its actual official size/license, explicitly download it, and verify the app reports the installed model.
2. Record phone/Android version, selected profile, Eco/thread setting, available RAM, charging state, battery level, ambient conditions and Android battery-saving settings. Close unrelated heavy workloads. Do not run inference while the phone is overheating.
3. Open Neural Core and run the evaluation. It contains 27 synthetic cases, nine each in English, Spanish and Persian, covering arithmetic, factual questions, missing live evidence, writing, reasoning, context, untrusted instructions, summarization and structured tool proposals. It makes no microphone or cloud request and executes no proposed tool.
4. Each case runs with the frozen Jarvis 3.0 prompt formatter and the current formatter, alternating pair order in the same v4 runtime/profile. The original 3.0 system/history formatting is retained in `BaselinePromptBuilder`; this comparison does **not** recreate the original APK's entire runtime, file-integrity checks, hardware state or orchestration. Native model/header values and measurements come from the actually loaded GGUF.
5. Export the synthetic evaluation JSON. It saves completed cases progressively and explicitly records whether all 54 answers finished. Cancellation/error can leave a partial evaluation. Repeat runs and distinguish cold load from warm inference; a single first-token time is not a device benchmark.
6. Review the raw answers separately for each language. Marker checks and JSON schema checks are narrow checks, not general accuracy, hallucination or language-quality scores. Judge correctness, missing-evidence honesty, relevance, coherence, instruction boundaries and preservation of supplied constraints. Record disagreements and sources for factual judgments. Report sample counts and raw failures.

The metrics include actual model file bytes, training-context/vocabulary/parameter/tensor metadata from llama.cpp, prompt and generated token counts, native load timing, perceived first-token time, total time, sampled process PSS, end thermal status and generation-phase tokens/sec. Perceived first-token time includes applicable cold integrity verification. PSS is a sampled app-wide value, not exact model-exclusive RAM or a proof of no leaks. Thermal status is Android's coarse status. There is no invented battery-energy reading.

A true original-binary comparison requires a controlled separate test setup using the preserved 3.0 source/artifact, same model revision and matched hardware conditions. No such comparison is claimed here. A model replacement recommendation requires actual quality/performance measurements; Lite is retained and Balanced is labelled experimental. Advanced is blocked until suitable hardware/model support is verified.

## Device integration checks

Use actual discovered packages, including unavailable and duplicate names. Test obvious, approximate, multilingual and corrected commands; verify a fuzzy match waits for confirmation and a stale/uninstalled target fails visibly. Check that risky actions cannot be approved by document/model text and that private readers still need permission/unlock.

Index a chosen harmless document, search a fact near its end, inspect attribution, export privately and remove it. Check offline search and failure when the index lacks evidence. Confirm cloud indexing permission is off by default and revocation clears transient context.

Enable a daily briefing at a future local time and check WorkManager behavior, time-zone changes, quiet hours, denied notification/calendar permission, missing weather/news, no private lock-screen text and no automatic speech. Work can be late. Repeated real-day runs are needed before claiming scheduling reliability.

Check wake-to-immersive UI, notification fallback, locked public presentation, normal authentication, preserved launcher behavior, 10-second expiry and post-speech follow-ups using actual installed ASR/TTS models. Check calls, Bluetooth, background restrictions and Android/Xiaomi battery optimization. Only physical tests establish noisy ASR, audible voice quality, sustained thermals, memory survival and battery drain.
