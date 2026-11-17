JARVIS — ULTIMATE PRODUCTION STABILIZATION & GOOGLE PLAY RELEASE

Master Engineering Prompt — Voice Pipeline, AI Connectivity, Animation, Device Controls, Settings & Release

You are a senior Android engineering team responsible for transforming my existing Jarvis application into a stable, intelligent, polished, production-ready voice assistant.

This is an implementation assignment. Do not stop after writing a plan.

Repository: sepehrdadehsani89-hash/gdp-dashboard

Branch: jarvis-next

Primary device: Xiaomi Redmi 9.

Application display name: Jarvis.

Preserve the existing application, package identity, signing continuity, user data, holographic design, and working features.

Another coding agent may already be modifying this repository. Inspect the working tree, existing commits, and in-progress changes before editing. Never overwrite uncommitted work or force-push.

THE FIVE CRITICAL BUGS

1. Jarvis hears its own voice. While Jarvis is speaking, the microphone detects Jarvis’s TTS audio, triggers interruption logic, cancels speech, and causes the conversation to fail.
2. AI appears connected but does not speak. Authentication or provider authorization reports success, yet questions do not produce an audible answer. Investigate the entire request-to-speech pipeline.
3. Reduced Motion is forced ON. The setting cannot be disabled, or its value is incorrectly restored. This may contribute to frozen core or wallpaper animation. Verify the cause instead of assuming it.
4. Holographic core and wallpaper animation are unreliable. The core must rotate and react to real application states.
5. The application needs production polish. Repair settings, buttons, device controls, onboarding, performance, accessibility, and release configuration.

Address these problems in that order, with measurable verification.

⸻

PHASE 1 — ELIMINATE SELF-INTERRUPTION COMPLETELY

1.1 Disable barge-in by default

Remove the current automatic speech interruption behavior from the default experience.

When Jarvis begins speaking:

* Stop or suspend active microphone recognition.
* Cancel pending speech endpoint timers.
* Prevent wake-word callbacks from interrupting the response.
* Prevent the assistant’s own audio from becoming a new user request.
* Ignore stale recognition callbacks from the previous listening session.
* Ensure that TTS is not canceled by ordinary environmental sound.
* Keep the holographic speaking animation active.

Jarvis must never interrupt itself.

Do not rely solely on echo cancellation.

Implement deterministic microphone and TTS ownership.

1.2 Explicit audio state machine

Use a single authoritative state machine:

IDLE → WAKE_DETECTED → LISTENING → FINALIZING → THINKING → SPEAKING → COOLDOWN → READY

Rules:

* LISTENING: microphone recognition is active.
* FINALIZING: recognition stops and the transcript is finalized.
* THINKING: provider request is active.
* SPEAKING: TTS owns the conversation; recognition is suspended.
* COOLDOWN: clear stale audio buffers and callbacks.
* READY: await the next supported activation.

Transitions must be serialized and cancellation-safe.

Use session IDs or generation tokens so callbacks from older sessions cannot alter the current state.

Do not allow multiple concurrent speech recognizers.

Do not allow duplicate TTS engines.

Do not let a previous microphone callback cancel a newer response.

1.3 No automatic interruptions

Default:

Allow interruptions while Jarvis speaks = OFF

Remove the old behavior where Jarvis hears sound and says:

“Okay, what is going on?”

Do not automatically ask that question.

Do not restart recognition while TTS is active.

Do not listen for interruptions from the microphone during speech in the default mode.

If a future advanced interruption feature is retained, keep it disabled by default and clearly label it experimental.

1.4 Manual Stop Speaking

Provide a reliable button:

Stop Speaking

When pressed:

1. Stop the current TTS utterance.
2. Clear its caption.
3. Cancel associated playback callbacks.
4. Release audio focus correctly.
5. Transition safely to READY.

The button must work without requiring the microphone.

1.5 Resume listening

After Jarvis finishes speaking:

* Wait for the TTS completion callback.
* Release playback resources appropriately.
* Apply a short, configurable cooldown.
* Re-enable recognition only when the selected conversation mode permits it.
* Avoid using a fixed timer as a substitute for the actual TTS completion event.

Default to a safe mode that requires the next wake command or explicit microphone activation.

Do not create an endless listening loop.

⸻

PHASE 2 — FIX CONNECTED AI THAT DOES NOT SPEAK

The user has completed an authorization flow, and the interface reports that the provider is connected.

However, Jarvis does not produce an audible answer.

Investigate the full pipeline:

User Speech → Speech Recognition → Command Router → AI Provider → Response Parser → TTS Queue → Audio Output

Instrument each stage with privacy-safe diagnostics.

2.1 Separate connection states

Display independent statuses:

* Account authenticated.
* Provider authorization granted.
* Inference available.
* Last request successful.
* Response received.
* TTS engine ready.
* Audio playback successful.

Do not treat authentication alone as proof that inference works.

2.2 Test actual inference

Perform a real authorized model request.

Validate the returned response structure.

Handle:

* Missing response content.
* Empty text.
* Invalid JSON.
* Unsupported response formats.
* Network timeout.
* HTTP 401.
* HTTP 403.
* HTTP 429.
* HTTP 500–599.
* Expired tokens.
* Missing model permissions.

Do not fabricate a response when the provider fails.

2.3 Test actual speech

After receiving a valid response:

1. Normalize the response into speakable text.
2. Submit it to the TTS engine.
3. Confirm the utterance starts.
4. Track completion or failure callbacks.
5. Restore the correct assistant state.

If the AI responds but TTS fails, show:

“AI response received. Voice playback failed.”

Provide a working retry control.

Do not report an AI connection failure when the real issue is TTS.

2.4 Remove screen-only responses

The default assistant must speak meaningful answers.

Do not replace normal answers with:

“The result is on your screen.”

For visual tasks, speak a useful summary and display the relevant visual content.

2.5 Provider support

Preserve legitimate integrations for:

* OpenAI / ChatGPT.
* Anthropic / Claude.
* Google Gemini.
* Optional Local Brain.

Use officially supported authorization and inference methods.

Do not assume a consumer ChatGPT or Claude subscription includes unrestricted API credits.

Do not bypass rate limits or quotas.

Do not display CONNECTED until the appropriate authorization and inference checks have succeeded.

⸻

PHASE 3 — REPAIR REDUCED MOTION

The Reduced Motion setting appears forced ON.

Investigate:

* Persisted preference values.
* DataStore or SharedPreferences initialization.
* Default values.
* StateFlow or Compose state synchronization.
* Settings UI bindings.
* Android accessibility animation settings.
* Battery Saver.
* Lifecycle restoration.
* Wallpaper-specific animation preferences.

3.1 Functional setting

Create a working setting:

Settings → Appearance → Reduced Motion

Allow the user to switch between:

* Full Motion.
* Reduced Motion.

The switch must persist correctly after app restart.

Do not force Reduced Motion ON because of an incorrect default, stale preference, or state synchronization bug.

3.2 Respect system accessibility

If Android’s accessibility or system animation settings require reduced motion, respect those settings.

Clearly distinguish:

* User preference.
* System accessibility setting.
* Battery Saver adaptation.

Do not silently override an accessibility requirement.

Explain when full animation is unavailable because of a system-level setting.

3.3 Verify wallpaper independence

Determine whether the frozen wallpaper is actually caused by Reduced Motion.

If not, find the true renderer or lifecycle problem.

Do not mark the wallpaper fixed simply because the switch changes.

⸻

PHASE 4 — RESTORE HOLOGRAPHIC ANIMATION

The existing Jarvis holographic core must animate smoothly.

Implement or repair:

* Continuous concentric-ring rotation.
* Independent orbital movement.
* Smooth energy pulses.
* Voice-reactive visual states.
* Thinking animation.
* Speaking animation.
* Charging particle effects.
* Correct wallpaper visibility handling.
* Proper frame scheduling.
* Efficient GPU usage.

4.1 Wallpaper lifecycle

Inspect:

* WallpaperService.
* SurfaceHolder callbacks.
* Visibility callbacks.
* Frame invalidation.
* Coroutine lifecycle.
* Rendering threads.
* Screen lock and unlock.
* Home-screen transitions.

The wallpaper must resume animation after becoming visible.

Do not render continuously while invisible.

4.2 Charging particles

When Android reports that the device is charging:

* Animate emerald-green particles upward.
* Curve particles toward the holographic core.
* Illuminate the rings.
* Show real battery percentage.
* Stop charging-specific effects when disconnected.

Do not invent charging status.

4.3 Performance

Optimize for Redmi 9 hardware.

Prefer smooth, sustainable animation over excessive effects.

Measure frame pacing where possible.

Respect thermal limits, battery saving, and reduced-motion accessibility.

⸻

PHASE 5 — REBUILD SETTINGS INTO A PROFESSIONAL CONTROL CENTER

Organize settings into clear sections:

1. Account & Identity.
2. AI Brain & Providers.
3. Voice & Microphone.
4. Device Control.
5. Security & Permissions.
6. Appearance & Wallpaper.
7. Notifications & Automation.
8. Language & Accessibility.
9. Diagnostics.
10. About Jarvis.

Requirements:

* Every button performs a real action.
* Every toggle persists.
* Every permission status is accurate.
* Every provider status is verified.
* Navigation is consistent.
* Settings are searchable.
* Descriptions are understandable.
* No duplicate or contradictory controls.
* No fake success animations.
* No dead-end screens.

Retain the application’s futuristic aesthetic.

Use restrained, polished transitions.

⸻

PHASE 6 — DEVICE CONTROL RELIABILITY

Repair supported voice commands for:

* Flashlight.
* Volume.
* Brightness.
* App launching.
* Battery information.
* Timers.
* Alarms.
* Media controls.
* Wi-Fi settings.
* Bluetooth settings.
* Mobile data settings.
* Do Not Disturb where authorized.

Use official Android APIs.

Where Android prohibits direct toggling, open the relevant system control or permission dialog.

Do not falsely announce success.

Verify hardware state where supported.

Do not bypass Android security restrictions.

⸻

PHASE 7 — SECURE OWNER VOICE AND LOCK-SCREEN BEHAVIOR

Preserve optional owner voice recognition.

Support legitimate lock-screen assistant actions where Android permits.

Do not bypass PIN, password, fingerprint, or secure keyguard.

A fully powered-off phone cannot run the application.

If the user says “Unlock my phone,” invoke the supported authentication flow where available and resume pending actions after the user authenticates.

Voice recognition alone must not be treated as secure Android unlocking.

⸻

PHASE 8 — CAMERA VISION AND LOCATION INTELLIGENCE

When the user asks:

“What is around me?”

Do not invent surroundings.

If authorized camera access is available:

* Capture a real image.
* Process it using a supported vision-capable model.
* Describe only visible information.
* Explain uncertainty.

If the camera is unavailable, ask for camera access.

Use GPS for geographic location, not for guessing objects in the room.

⸻

PHASE 9 — FIRST-RUN SETUP EXPERIENCE

Create or repair the one-time welcome wizard for this major update.

Use the existing animated holographic core.

Screens:

1. Welcome to Jarvis.
2. Choose Language.
3. Microphone Setup.
4. Voice Identity.
5. Select AI Brain.
6. Connect Provider.
7. Test AI Response.
8. Test Voice Output.
9. Device Permissions.
10. Device Control Test.
11. Wallpaper & Appearance.
12. Final Diagnostics.
13. Jarvis Ready.

Every screen must support:

* Back.
* Next.
* Skip where appropriate.
* Accurate completion status.

Do not show “Jarvis Ready” as fully connected when inference or voice output remains unavailable.

Allow setup to be reopened later.

Do not repeat onboarding every launch.

⸻

PHASE 10 — PROFESSIONAL UI AND ANIMATION

Maintain the existing visual identity.

Improve:

* Holographic transitions.
* Responsive buttons.
* Loading indicators.
* Error states.
* Provider cards.
* Settings navigation.
* Speech captions.
* Voice activity visualization.
* Wallpaper performance.
* Charging effects.

Avoid fake interactive elements.

Do not introduce visual effects that make the Redmi 9 unstable.

The product must feel polished because its interactions are reliable, not merely because it has animations.

⸻

PHASE 11 — TEST THE ACTUAL BUGS

Create automated tests and a physical-device verification checklist.

Required scenarios:

Test 1 — Self-interruption

Jarvis speaks a long answer.

Expected: The microphone does not capture Jarvis’s TTS as a new command.

Test 2 — No accidental cancellation

Background noise occurs while Jarvis speaks.

Expected: Jarvis finishes its response.

Test 3 — Manual stop

User presses Stop Speaking.

Expected: TTS stops immediately and state resets.

Test 4 — AI authenticated

Provider identity is authenticated.

Expected: Identity status is accurate.

Test 5 — AI inference

Provider returns a real response.

Expected: Response is parsed correctly.

Test 6 — Spoken answer

AI response is received.

Expected: TTS speaks the actual answer.

Test 7 — TTS failure

Audio output fails.

Expected: Clear TTS-specific error, not a false AI error.

Test 8 — Reduced Motion

Toggle Reduced Motion OFF.

Expected: Preference changes and persists unless system accessibility settings require otherwise.

Test 9 — Core animation

Core remains visible for 60 seconds.

Expected: Continuous frame updates.

Test 10 — Wallpaper

Lock and unlock the device.

Expected: Wallpaper resumes animation.

Test 11 — Charging

Connect charger.

Expected: Real charging animation activates.

Test 12 — Settings

Change settings and restart the app.

Expected: Values persist.

Test 13 — Device controls

Execute supported hardware actions.

Expected: Actual results match spoken confirmations.

Test 14 — Onboarding

Complete setup.

Expected: Wizard does not reopen unnecessarily.

Test 15 — Extended conversation

Run repeated speech-recognition, AI, and TTS cycles.

Expected: No frozen states, duplicate requests, or self-interruptions.

Document which tests were actually run.

Do not fabricate physical-device results.

⸻

PHASE 12 — GOOGLE PLAY PRODUCTION READINESS

Prepare Jarvis for legitimate Google Play publication.

12.1 Android release quality

Verify:

* Stable package identifier.
* Compatible Android SDK configuration.
* Correct permissions.
* Appropriate foreground service declarations.
* Background microphone restrictions.
* Battery optimization behavior.
* Accessibility API compliance.
* Secure credential handling.
* No embedded API secrets.
* Crash reporting configuration.
* Privacy policy requirements.
* User data handling.
* Third-party SDK disclosures.

12.2 Release artifacts

Produce:

* Signed Android App Bundle (.aab) for Google Play.
* Installable APK for direct device testing.
* Version name and version code.
* Release notes.
* SHA-256 checksums.
* Test report.
* Known limitations.

Use an authorized signing configuration.

Never invent a signing key.

Do not change the existing signing identity without an approved migration plan.

12.3 Google Play requirements

Review the current Google Play policies applicable to:

* Microphone access.
* Background services.
* Accessibility.
* User data.
* AI-generated content.
* Account authentication.
* App permissions.
* Target API level.
* App testing and publication eligibility.

Do not claim Google Play approval before the store actually approves the application.

Do not attempt to bypass Play Protect or platform security controls.

⸻

PHASE 13 — STRICT IMPLEMENTATION RULES

1. Inspect the real code before changing it.
2. Preserve working features.
3. Make small, testable changes.
4. Fix root causes rather than symptoms.
5. Never create fake provider connections.
6. Never fabricate AI responses.
7. Never claim a microphone test passed without evidence.
8. Never claim animation works solely because it compiles.
9. Never fabricate device-test results.
10. Never hide quota or billing errors.
11. Never bypass Android security.
12. Never leave decorative buttons that do nothing.
13. Never replace a working feature with an unfinished prototype.
14. Never overwrite another agent’s uncommitted work.
15. Never force-push.
16. Never invent APK links.
17. Never report a release as complete if required builds fail.

⸻

PHASE 14 — IMPLEMENTATION ORDER

Follow this exact order:

Stage 1 — Audit

Inspect repository state and existing failures.

Stage 2 — Self-interruption repair

Disable microphone interruption during TTS and fix the audio state machine.

Stage 3 — AI-to-speech repair

Verify actual inference and spoken playback.

Stage 4 — Reduced Motion repair

Fix preference persistence and accessibility interactions.

Stage 5 — Holographic core and wallpaper repair

Restore animation and lifecycle reliability.

Stage 6 — Settings reconstruction

Organize and connect all controls.

Stage 7 — Device control repair

Implement supported Android actions.

Stage 8 — Onboarding repair

Create the accurate first-run setup.

Stage 9 — Performance optimization

Tune for Redmi 9.

Stage 10 — Google Play readiness

Build, test, sign, and package the application.

Do not rush past the first four stages.

Those are the current blockers.

⸻

PHASE 15 — FINAL ENGINEERING REPORT

At completion, provide:

VOICE PIPELINE

* Root cause of self-interruption.
* Files changed.
* New state machine behavior.
* TTS completion handling.
* Microphone reactivation behavior.
* Tests performed.

AI CONNECTIVITY

* Provider authentication status.
* Inference verification.
* Response parsing status.
* TTS playback verification.
* Remaining account or quota limitations.

ANIMATION

* Reduced Motion root cause.
* Preference persistence fix.
* Core animation fix.
* Wallpaper lifecycle fix.
* Charging effects status.

SETTINGS

* New navigation structure.
* Functional controls.
* Persisted preferences.
* Removed obsolete controls.

DEVICE CONTROL

* Working actions.
* Actions requiring system confirmation.
* Unsupported actions.

RELEASE

* App version.
* APK build status.
* AAB build status.
* Signing status.
* SHA-256 checksums.
* Tests passed, failed, and skipped.
* Physical-device tests remaining.

FINAL DELIVERABLES

Provide:

JARVIS APK DOWNLOAD LINK:

A verified HTTPS link to the newly built APK.

JARVIS GOOGLE PLAY AAB:

A verified artifact path or authorized download link to the generated Android App Bundle.

If an artifact cannot be built or published, explain the blocker.

Do not fabricate links or claim successful publication without evidence.

⸻

FINAL DIRECTIVE

Build Jarvis as a real, production-grade Android voice assistant.

Its microphone must not hear and interrupt its own voice.

Its AI must generate real responses.

Its TTS must speak those responses.

Its Reduced Motion setting must work correctly.

Its holographic core must animate reliably.

Its wallpaper must function smoothly.

Its settings must be organized and functional.

Its device controls must accurately report real results.

Its first-run setup must guide the user through genuine configuration.

Its release must be suitable for real Google Play review.

START NOW WITH THE SELF-INTERRUPTION BUG. DO NOT ADD MORE FEATURES UNTIL THE AUDIO PIPELINE IS STABLE. IMPLEMENT, TEST, VERIFY, AND DELIVER THE ACTUAL RESULTS.