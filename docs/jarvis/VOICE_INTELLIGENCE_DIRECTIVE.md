PROJECT J.A.R.V.I.S.

ULTIMATE VOICE INTELLIGENCE, MICROPHONE RECONSTRUCTION & HOLOGRAPHIC CORE ENGINE

Production Engineering Master Prompt — Android

You are acting as a world-class Android engineering team consisting of:

* Principal Android/Kotlin Engineer
* Senior Speech Recognition and Audio DSP Engineer
* AI Conversational Systems Architect
* Android Graphics and GPU Rendering Engineer
* Live Wallpaper Specialist
* UI/UX Motion Design Director
* Performance and Battery Optimization Engineer
* Accessibility and Localization Engineer
* Security Engineer
* Senior QA and Release Engineer

Your mission is to transform my existing J.A.R.V.I.S. Android application into an exceptionally polished, intelligent, responsive, futuristic voice assistant.

This is an implementation assignment, not a request for ideas or a design proposal.

Inspect the existing project, identify the real causes of its problems, implement working solutions, test them, and produce a verified Android APK.

PROJECT CONTEXT

Application: J.A.R.V.I.S.

Primary device: Xiaomi Redmi 9.

Platform: Android.

Repository: sepehrdadehsani89-hash/gdp-dashboard

Development branch: jarvis-next

The project already contains voice assistant functionality, a holographic core, wallpaper functionality, animations, and other systems.

Preserve existing working features and the established visual identity.

Do not replace the existing app with a generic assistant template.

Do not blindly rewrite the application.

Start by inspecting the actual repository, build configuration, AndroidManifest, audio pipeline, speech recognition implementation, TTS engine, wallpaper service, animation code, lifecycle management, and existing tests.

Determine which functionality is real, partially implemented, broken, or merely simulated.

Use the current repository state as the source of truth. Do not assume earlier build reports prove the current APK works.

Before editing, establish a recoverable Git checkpoint and document the baseline.

Never force-push, delete user data, overwrite unrelated work, or introduce unnecessary dependencies.

⸻

PHASE 1 — REBUILD THE MICROPHONE INTELLIGENCE

1.1 The primary problem

My current assistant sometimes continues listening after I have finished speaking.

For example:

I say:

“Hey Jarvis, what is the weather today?”

I stop speaking.

However, cars, people talking, music, traffic, or background sounds continue.

J.A.R.V.I.S. incorrectly keeps listening, fails to recognize the end of my request, or never answers.

This behavior must be investigated and fixed.

The microphone must not depend exclusively on complete environmental silence.

1.2 Speaker-aware end-of-turn detection

Implement a robust, adaptive speech endpoint detection system.

Its objective is to identify when the active user has finished a request, rather than waiting for all surrounding sounds to disappear.

Evaluate and integrate suitable technologies such as:

* Android AudioRecord
* Android SpeechRecognizer
* On-device speech recognition where available
* Voice Activity Detection (VAD)
* Silero VAD or a suitable alternative
* WebRTC audio processing components where compatible
* Noise suppression
* Acoustic echo cancellation
* Automatic gain control
* Adaptive endpointing
* Partial transcription
* Speech confidence analysis
* Speaker embeddings or speaker verification where technically justified

Do not assume any one library solves the entire problem.

Benchmark candidate solutions on the target device class.

Choose the smallest, most reliable architecture that provides good results.

1.3 Detect the actual user’s speech

The system should distinguish, as reliably as technically possible, between:

A. The user speaking.

B. The user pausing.

C. The user finishing a sentence.

D. A television playing in the background.

E. Traffic and engine sounds.

F. People talking nearby.

G. The assistant’s own synthesized speech.

H. Microphone noise.

I. A new user command.

J. A genuine interruption from the user.

Do not falsely promise perfect speaker separation from a single microphone.

Implement confidence-based decisions, appropriate fallbacks, and optional user voice enrollment only if needed.

Never store biometric voiceprints without clear consent and secure handling.

1.4 Adaptive end-of-speech engine

Implement a state machine such as:

IDLE → WAKE_DETECTED → LISTENING → SPEECH_ACTIVE → ENDPOINT_CANDIDATE → FINALIZING → THINKING → SPEAKING → READY.

Provide configurable endpoint timing.

Suggested initial tuning ranges for experimentation:

* Short pause detection: 350–650 ms.
* Normal end-of-turn candidate: 650–1,100 ms.
* Extended pause allowance for complex sentences: 1,200–1,800 ms.

These are starting points, not universal requirements.

Tune the values using recorded test scenarios and measured false-cutoff rates.

Combine:

1. Speech activity confidence.
2. Partial transcription stability.
3. Acoustic features.
4. Active-speaker confidence where available.
5. Semantic sentence-completion signals.
6. Maximum utterance safeguards.
7. User-configurable sensitivity.

Crucial requirement:

When the user stops speaking but other people continue talking, the assistant should attempt to finalize the user’s request promptly rather than wait indefinitely.

When confidence is insufficient, use a graceful fallback instead of silently hanging.

1.5 No more endless listening

Every voice session must have bounded states and clear recovery paths.

Handle:

* Speech recognizer timeout.
* Empty transcription.
* Network timeout.
* Microphone permission loss.
* Recognition service failure.
* Audio focus changes.
* Bluetooth microphone changes.
* Screen lock.
* App backgrounding.
* TTS interruption.
* Repeated wake detection.
* Android lifecycle changes.

Never allow the interface to remain permanently stuck on “Listening.”

Show a meaningful status and offer a retry when recovery cannot be automatic.

⸻

PHASE 2 — INTELLIGENT CONVERSATION ENGINE

J.A.R.V.I.S. must be able to process a broad variety of legitimate questions and commands.

Examples:

“Jarvis, explain quantum computing.”

“Jarvis, what’s 35 times 48?”

“Jarvis, open Spotify.”

“Jarvis, explain this in Spanish.”

“Jarvis, help me study physics.”

“Jarvis, summarize my last answer.”

“Jarvis, what did I just ask?”

“Jarvis, stop talking.”

“Jarvis, continue.”

“Jarvis, speak more slowly.”

“Jarvis, can you repeat that?”

Support natural follow-up conversations and contextual understanding.

Do not implement a fake response generator that pretends to know answers.

Use the application’s existing secure AI backend when available.

If the backend is disconnected, report that accurately and preserve supported offline functionality.

2.1 Conversation flow

The expected experience is:

1. User activates Jarvis.
2. Microphone begins listening.
3. User speaks.
4. Jarvis detects the end of the user’s turn.
5. Recognition finalizes.
6. AI processes the request.
7. Jarvis begins responding.
8. Voice synthesis plays naturally.
9. The interface returns to a ready state.

No unnecessary repeated prompts.

No endless microphone loops.

No duplicate AI requests.

No overlapping voice responses.

2.2 Barge-in support

Implement interruption handling where supported.

When Jarvis is speaking and the user intentionally interrupts, Jarvis should stop or pause its speech and process the new request.

Use echo cancellation and appropriate audio focus handling to avoid Jarvis recognizing its own voice as a new user command.

Ensure the system does not repeatedly interrupt itself.

2.3 English and Spanish

Provide robust English and Spanish support.

Allow users to choose:

* English
* Español
* Automatic language selection where supported

Handle code-switching gracefully when the recognition engine supports it.

Do not advertise automatic bilingual detection if the installed recognition engine cannot provide it reliably.

⸻

PHASE 3 — COMPLETELY FIX VOICE CAPTIONS

The current wallpaper and voice interface have confusing caption behavior.

I want a clean, professional separation between user speech and Jarvis speech.

3.1 Jarvis captions

Display captions only for what J.A.R.V.I.S. says aloud.

When Jarvis speaks:

* Display the corresponding assistant response.
* Synchronize captions with speech when reliable timing information is available.
* Use elegant typography.
* Animate text appearance subtly.
* Handle long responses gracefully.
* Keep the holographic core visible.
* Remove the caption after the response ends, with a short configurable delay.

3.2 User speech

When I speak:

* Do not display a persistent transcription overlay on the wallpaper.
* Do not show the assistant’s previous captions as if they were my words.
* Do not allow user transcription to cover the holographic core.
* Process my speech normally.

If a transcription preview is useful for debugging or accessibility, place it in an optional settings-controlled mode.

Default experience: clean, minimal, futuristic.

3.3 Caption synchronization

The caption must reflect the actual spoken response.

Avoid:

* Text appearing before a response is generated.
* Old captions remaining permanently visible.
* Duplicate caption layers.
* Captions continuing after speech is canceled.
* Unrelated text appearing during microphone listening.
* Captions becoming stuck after lifecycle transitions.

Use the actual TTS progress callbacks when supported.

⸻

PHASE 4 — REPAIR THE FROZEN HOLOGRAPHIC CORE

Critical issue: The holographic core is currently frozen.

The application’s central core and the wallpaper core must animate correctly.

Do not simply add animation flags or report success without verifying rendering.

Inspect:

* Compose animation state.
* Canvas rendering.
* Choreographer frame callbacks.
* Coroutine and lifecycle ownership.
* Live Wallpaper Engine lifecycle.
* Surface creation and destruction.
* Visibility callbacks.
* Render invalidation.
* Frame scheduling.
* Hardware acceleration.
* Main-thread blocking.
* GPU resource usage.
* Battery optimization behavior.
* Existing shader or particle implementations.

Find the actual reason the core is not moving.

Fix the cause.

4.1 Core rotation

The holographic core should feature:

* Smooth continuous rotation.
* Multiple independently rotating concentric rings.
* Subtle depth and parallax.
* Dynamic glow.
* Reactive lighting.
* Soft pulsation.
* Controlled orbital particles.
* Fluid transitions between states.

The result should feel like an advanced cinematic AI interface while remaining efficient on a Redmi 9.

4.2 Voice-reactive animation

When Jarvis is idle:

Core rotates gently.

When the microphone is listening:

Core illumination becomes more active.

When user speech is detected:

Core reacts to real audio amplitude or processed voice features.

When Jarvis is thinking:

Core transitions into a distinctive processing animation.

When Jarvis is speaking:

Core pulses in synchronization with actual synthesized audio activity where technically possible.

When an error occurs:

Core uses a subtle visual warning state.

Never fake microphone responsiveness with random movement presented as real audio input.

4.3 Smooth performance

Target smooth rendering appropriate to device capabilities.

Prefer 60 FPS when sustainable, but use adaptive frame pacing.

Avoid excessive GPU usage, thermal throttling, battery drain, and memory leaks.

Support reduced-motion accessibility settings.

⸻

PHASE 5 — FUTURISTIC CHARGING ANIMATION

Create a beautiful charging effect inspired by premium Xiaomi/Redmi visual experiences, while retaining J.A.R.V.I.S.’s original identity.

When the phone begins charging:

1. The holographic core becomes brighter.
2. Green particles rise from the bottom of the screen.
3. Particles spiral toward the center of the core.
4. The core absorbs the incoming particles.
5. Energy rings illuminate.
6. A subtle charging pulse travels through the rings.
7. The animation continues smoothly while charging.

Use real Android battery and charging status.

Do not simulate charging when the device is disconnected.

5.1 Particle engine

Design a performant particle system with:

* Emerald green energy particles.
* Soft bloom-like highlights.
* Variable particle sizes.
* Smooth upward movement.
* Curved trajectories.
* Controlled turbulence.
* Subtle particle trails.
* Gradual fade-out.
* Core absorption effects.

Avoid overwhelming visual clutter.

5.2 Battery percentage

Provide an elegant optional battery indicator.

Examples:

“CHARGING — 64%”

“BATTERY FULL — 100%”

Use real battery readings.

Do not invent charging speed or time-to-full estimates.

5.3 Charging state changes

Handle:

* Charger connected.
* Charger disconnected.
* Battery fully charged.
* Low battery.
* Battery saver enabled.
* Screen turned off.
* Wallpaper hidden.
* Device thermal constraints.

Reduce animation workload when appropriate.

Do not continue unnecessary full-speed rendering while the wallpaper is invisible.

⸻

PHASE 6 — LIVE WALLPAPER RECONSTRUCTION

Ensure the animated holographic core works both:

A. Inside the main J.A.R.V.I.S. application.

B. Inside the Android live wallpaper.

These may require separate rendering lifecycle implementations.

Do not assume fixing the Compose UI automatically fixes the WallpaperService.

6.1 Wallpaper requirements

* Continuously animated core when visible.
* Correct wallpaper scaling.
* Proper orientation handling.
* Stable frame scheduling.
* No frozen Canvas frames.
* Smooth charging effects.
* Correct battery state updates.
* Minimal battery usage when hidden.
* No crashes when switching wallpapers.
* Proper recovery after screen lock/unlock.
* Correct surface lifecycle management.

6.2 Caption behavior on wallpaper

Only display assistant speech captions when the platform, lifecycle, and app architecture allow them.

Do not claim the wallpaper can run an unrestricted always-listening microphone in the background.

Respect Android microphone, foreground service, lock-screen, and privacy restrictions.

If wallpaper speech captions cannot be reliably supported under a particular device state, implement a graceful alternative.

6.3 Visual quality

Create a polished futuristic appearance featuring:

* Sophisticated holographic geometry.
* High-quality circular motion.
* Green energy accents.
* Layered luminous rings.
* Smooth gradients.
* Responsive light.
* Elegant particle effects.
* Clean typography.
* Minimal interface clutter.

Maintain consistency with the existing app.

⸻

PHASE 7 — PROFESSIONAL MICROPHONE SETTINGS

Create a dedicated settings section:

Settings → Voice Intelligence

Include working controls for:

1. Microphone sensitivity.
2. End-of-speech detection sensitivity.
3. Background noise filtering.
4. Voice recognition language.
5. Preferred response language.
6. Speech playback speed.
7. Voice interruption.
8. Optional speaker recognition.
9. Optional transcription preview.
10. Microphone diagnostic test.
11. Voice engine status.
12. Speech recognition troubleshooting.

Each setting must have a real functional effect.

Do not create decorative switches that do nothing.

Provide clear explanations for settings that affect privacy, performance, or recognition accuracy.

⸻

PHASE 8 — MICROPHONE DIAGNOSTIC CENTER

Create an interactive microphone diagnostic screen.

Display:

* Microphone permission status.
* Current recording state.
* Audio input activity.
* VAD confidence.
* Recognizer availability.
* Current language.
* End-of-turn detection state.
* Recognition latency.
* Last recoverable error.

Offer a controlled microphone test.

The test should demonstrate whether Jarvis correctly detects the end of speech when background noise continues.

Do not retain diagnostic recordings by default.

Protect sensitive microphone data.

⸻

PHASE 9 — TEST THE REAL FAILURE SCENARIOS

Build a comprehensive automated and manual test plan.

Prioritize the exact problems I reported.

Test A — Traffic noise

User speaks a question.

User stops.

Traffic noise continues.

Expected: Jarvis finalizes the user’s utterance and responds without waiting for environmental silence, provided speech recognition confidence is sufficient.

Test B — Nearby conversation

User speaks.

Another person continues talking nearby.

Expected: Jarvis attempts to identify the active user’s endpoint without indefinitely extending the session.

Measure errors and document limitations.

Test C — Background music

User speaks while music plays.

Expected: Jarvis recognizes the request when possible and recovers gracefully when confidence is low.

Test D — Long question

User speaks a complex question with natural pauses.

Expected: Jarvis avoids prematurely cutting off the sentence.

Test E — Short question

User asks a brief question.

Expected: Fast, accurate endpoint detection.

Test F — Assistant interruption

Jarvis is speaking.

User interrupts.

Expected: Controlled interruption without feedback loops.

Test G — Frozen core

Open the main application.

Observe the core for a sustained period.

Expected: Real, continuous animation.

Test H — Wallpaper

Apply the live wallpaper.

Lock and unlock the device.

Switch between applications.

Expected: Wallpaper animation resumes correctly.

Test I — Charging

Connect the charger.

Expected: Real charging state activates green particle animation.

Disconnect charger.

Expected: Charging animation stops.

Test J — Captions

Jarvis speaks.

Expected: Captions display the actual assistant response.

User speaks.

Expected: No unwanted persistent user transcription overlay.

Test K — Extended use

Run repeated conversation cycles.

Expected: No permanent listening state, resource leak, duplicate responses, or frozen animation.

⸻

PHASE 10 — PERFORMANCE AND STABILITY

Optimize for the Redmi 9 hardware class.

Focus on:

* Memory consumption.
* CPU usage.
* GPU rendering.
* Battery efficiency.
* Thermal behavior.
* Audio pipeline stability.
* Speech recognition latency.
* App startup time.
* Wallpaper lifecycle.
* Network reliability.
* Crash recovery.

Avoid heavy ML models if they make the application unusable on the target hardware.

Use device-compatible acceleration where available.

Prefer measurable performance improvements over impressive-sounding but unnecessary frameworks.

⸻

PHASE 11 — SECURITY AND PRIVACY

Maintain a secure architecture.

Requirements:

* Never hardcode AI API keys in the APK.
* Use the existing secure backend where possible.
* Obtain microphone permission appropriately.
* Respect Android background microphone restrictions.
* Avoid covert audio recording.
* Do not upload raw audio without a clear functional reason and appropriate user consent.
* Protect conversation data.
* Use secure communication.
* Avoid unnecessary permissions.
* Do not bypass Android security restrictions.
* Ensure diagnostic logging does not expose private speech content.

⸻

PHASE 12 — ENGINEERING EXECUTION PLAN

Perform the work in this order:

Step 1: Repository audit

Inspect the current project and identify the exact files responsible for audio capture, VAD, speech recognition, TTS, UI captions, holographic rendering, and wallpaper rendering.

Step 2: Baseline verification

Run available tests and compile the existing project.

Document current failures.

Step 3: Microphone repair

Fix audio lifecycle and end-of-turn detection first.

Step 4: Conversation repair

Ensure recognized speech is reliably submitted to the backend and produces a response.

Step 5: Caption repair

Separate user recognition from assistant speech captions.

Step 6: Core animation repair

Fix the frozen holographic core in the main application.

Step 7: Wallpaper repair

Fix the live wallpaper rendering loop independently.

Step 8: Charging animation

Implement the green particle charging system.

Step 9: Settings and diagnostics

Add functional controls and diagnostic tools.

Step 10: Testing

Run automated tests, inspect build output, and document what requires physical-device verification.

Step 11: Release build

Generate the production-ready APK.

Do not skip ahead and claim completion while foundational features remain broken.

⸻

PHASE 13 — STRICT QUALITY REQUIREMENTS

I want an exceptional final product, but I do not want fabricated success reports.

Follow these rules:

1. Never say a feature works unless there is supporting evidence.
2. Never substitute a visual mockup for working Android functionality.
3. Never leave buttons that perform no action.
4. Never hide compilation failures.
5. Never claim physical-device testing occurred unless it actually occurred.
6. Never fabricate test results.
7. Never claim the microphone perfectly isolates the user in all environments.
8. Never claim an APK was generated unless the file exists.
9. Never provide an unverified download URL.
10. Never delete working features unnecessarily.
11. Never abandon an incomplete feature without documenting it.
12. Never create placeholder AI responses and call them intelligence.
13. Never mark a frozen animation as fixed without testing the frame-update logic.
14. Never sacrifice microphone stability for visual effects.
15. Never sacrifice battery efficiency for unnecessary animation complexity.

Make focused, testable commits.

Preserve a rollback path.

If a dependency, Android SDK, build tool, or external service is unavailable, explain the exact blocker and continue with the portions that can be verified.

⸻

PHASE 14 — FINAL APK DELIVERY

After implementation:

1. Run the appropriate Gradle build.
2. Run available automated tests.
3. Inspect build failures and resolve them.
4. Verify the APK file exists.
5. Verify its package identifier.
6. Verify signing information.
7. Verify the intended Android architecture and compatibility.
8. Calculate the APK SHA-256 checksum.
9. Produce a release summary.
10. Upload or publish the APK to an accessible release location using authorized repository access.

Do not overwrite an existing release without authorization.

Prefer an appropriately versioned GitHub Release.

If a production signing key is unavailable, explain the limitation. Do not invent credentials or claim a debug APK is a production-signed release.

If a physical Redmi 9 is unavailable, clearly identify device-specific tests that remain unverified.

REQUIRED FINAL REPORT

At the end, provide:

A. Microphone

What was broken?

What was changed?

How does end-of-speech detection work now?

What evidence supports the improvement?

B. Conversation engine

Does Jarvis reliably process questions?

What backend dependencies remain?

C. Captions

Do captions appear only during assistant speech?

D. Holographic core

Is the main core animated?

What rendering mechanism was repaired?

E. Wallpaper

Is the wallpaper animation operational?

What remains to be verified on the Redmi 9?

F. Charging

Do green particles respond to actual charging state?

G. Performance

What measurements were obtained?

H. Testing

How many tests passed, failed, or were skipped?

Which tests were performed on actual hardware?

I. Release

APK version.

Package name.

APK size.

Signing status.

SHA-256 checksum.

Release commit.

FINAL INSTRUCTION — GIVE ME THE APPLICATION LINK

Your final response must end with:

J.A.R.V.I.S. APK DOWNLOAD LINK:

Provide the actual working HTTPS download URL to the newly built APK.

The URL must point to the exact verified APK artifact, not merely the repository homepage.

If you cannot upload or publish the APK, provide the verified artifact path and state clearly that a public download link is not yet available.

Do not invent a link.

BEGIN NOW. Audit the real repository, repair the microphone and frozen animations, implement the charging effects, verify the build, and deliver the most reliable J.A.R.V.I.S. release possible.