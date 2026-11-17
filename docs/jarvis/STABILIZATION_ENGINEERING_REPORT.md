# Stabilization candidate engineering report

This checkpoint repairs the existing Jarvis application; it does not implement all master-specification requests or claim production/device acceptance. The previous release artifacts and rollback branches are retained.

## Voice pipeline

The self-interruption root cause was an enabled playback-interruption path reopening wake recognition during TTS before normal suppression. Barge-in is now retired, even for a persisted true preference. `VoiceSessionStateMachine`, `WakeListenerService`, `SpeechController` and `AssistantRuntime` reserve one conversational audio owner and fence old callbacks with generation tokens. Recognition is blocked during finalization, inference, synthesis, playback and cooldown. Finalization and thinking have explicit transitions. Default follow-up requires another wake/manual activation; opt-in continuous conversation remains separate.

Completion uses actual MediaPlayer callbacks followed by the persisted 250–2,000 ms cooldown, not a timer pretending playback completed. New speech attempts also have their own cancellation serial, so cancellation of an older synthesis cannot release a newer owner's reservation. Battery-alert stop, owner-enrollment prompts and Android identity flow use the same safe cancellation boundary. Stop Speaking and Retry are in the fixed control bar; microphone activation is disabled during audio output. Audio focus/resources remain owned by the existing controller.

Thirty half-duplex tests pass. The 32 delivery tests cover separate inference/audio evidence, retry, failed route state handling, stale callbacks, normalization and preparation. Actual microphone echo isolation, audibility and manual-stop timing need the phone.

## AI connectivity and speech

Authentication, provider authorization and exact-model inference proofs remain in the existing Brain Manager. This cloud run used no live accounts or provider quota, and does not verify the user's ChatGPT subscription or local GGUF installation. Provider HTTP/auth/protocol regressions use fixtures and mock servers.

The battery-specific silent answer came from a private native reply being replaced with a generic screen acknowledgement. Native battery answers now speak the measured reading. Fixed public native save/confirmation/status summaries do not include contact payloads, URLs or private memories. Other private content still requires explicit consent for Fish Audio. Final AI text is validated and normalized once before serialized speech; provisional streaming deltas remain visual rather than creating overlapping utterances. Empty/failed provider responses are not fabricated.

`VoiceDeliveryTracker` distinguishes inference requesting/received/failed from voice preparing/engine prepared/started/completed/failed/stopped. Voice diagnostics show these separately. Retry preserves received-answer evidence, clears the old TTS error and retries the retained transient answer without a new inference call. A valid AI answer followed by TTS failure reports “AI response received. Voice playback failed.” Completion proves an audio API event, not a physical speaker test.

## Animation and performance

The reduced-motion root cause was copying a temporary memory restriction into the UI's saved preference projection. That mutation was removed. Existing user preferences, system animation accessibility, Battery Saver and memory frame budgets remain distinct, with explicit descriptions. Previously corrupted saved choices are not guessed or reset; the user's next explicit Off choice is preserved.

Prior core frame, wallpaper surface/lifecycle and charging-particle implementations are retained. Their 24/20/20 software cases and 20 new motion-preference cases pass. Assets/native source were not changed. No 60-second GPU run, lock/unlock, charging animation, FPS, battery-drain or Redmi 9 runtime-memory measurement was possible. Cloud build memory is not a phone-performance benchmark.

## Settings, contacts and setup

Settings has ten searchable sections opening existing controls. Detailed preferences remain available; no parallel preference store or reflection-driven arbitrary writes was introduced. The main holographic home and original renderer are retained. Typed voice commands write the same preferences used by UI controls. Low-risk values are range checked and read back. Privacy, wake and provider edits require explicit confirmation; the existing identity policy and exact provider proof gate remain in force. Credential commands are intercepted before history/model routing. Not every Android/consent operation can be performed by voice; those retain their supported system/private screens.

Explicit contact alias commands resolve exact unique local/Android matches, never infer family relationships, and require confirmation before encrypted local save. Linked numbers/names and the shortcut-store fingerprint are rechecked. Permission grants resume only the current unexpired internal request. Calling remains ACTION_DIAL with Android's final Call action, not an automatic or verified connected call.

The major-update setup has thirteen steps with Back/Next/Skip and a persisted revision; it can be reopened. Its ready label requires microphone permission, a real selected-model test, a completed output test and the owner's confirmation of audibility. Partial setup can be saved honestly. New settings navigation/wizard labels currently use English; existing language selection, Spanish recognition/voice connections and detailed localized controls are retained. Full localization of the new navigation remains follow-up polish.

New JVM cases: settings parser/ranges/privacy 41; directory search 20; contacts 36; startup eligibility 20; setup truthfulness/navigation 20; native speech summaries 20. The 20 new real-Android preference tests and updated existing instrumented fixtures await execution on an authorized test device. Their compilation result is recorded with the build evidence; no executed device/UI test is claimed.

## Device control

The existing supported APIs are preserved. Battery replies contain actual readings; torch operations require permission and callback observation; media volume reports the actual readback. Protected brightness/connectivity/DND controls use official access or Android panels and retain awaiting-user status. Timers, alarms, media dispatch, installed-app resolution and lock authentication remain in their existing implementations. Unsupported privileged toggles, credential bypass, fully powered-off listening and restricted iPadOS functions are not claimed. See `PRODUCTION_STABILIZATION_PROGRESS.md` for mechanisms and `STABILIZATION_DEVICE_ACCEPTANCE.md` for unexecuted hardware scenarios.

## Verification and release boundary

Final JVM result: **1,297 cases; 1,296 passed; zero failures/errors; one optional private-export fixture skipped because no export was supplied**. Earlier focused passes and failed/cancelled attempts remain recorded; they are not substituted for this run. Final full run 3 passed lint (zero errors, 55 warnings, one informational item), APK assembly and Android-test compilation. Twenty artifact/signature/package/asset checks passed. APK SHA-256: `ad796f1920f4853ff855acd710d1360bfa32a1e185ca733d4850c071ab7261ed`; 56,199,459 bytes. All 14 native libraries remain ARM64 and all 134 compared asset/native entries are unchanged. Exact files and per-case results are in the final build evidence and release notes.

Candidate version: **2.11.9-stabilize.1-debug**, version code **30**, display name **Jarvis**, package **com.jarvis.next.phoenix**. It preserves the retained Phoenix signer for updates of that preview. It cannot update the original differently signed `com.jarvis.next` package. No dependency, model, emulator or key was installed during this milestone. No application data was deleted and the Room schema is unchanged.

The owner confirmed no Google Play listing and unavailable/uncertain original signing keystore. Production AAB signing remains blocked. API 36 migration, Vosk's 16 KB alignment, publisher privacy/reporting/declarations and device testing also remain open. See `GOOGLE_PLAY_READINESS.md` and `SIGNING_RECOVERY_PLAN.md`. Neither Google Play approval, zero bugs nor production completion is claimed.

GitHub publication requires the owner's confirmation under `PHOENIX_DIRECTIVE.md`. No new public APK URL is claimed before the concrete checkpoint is pushed and its downloaded bytes verified.
