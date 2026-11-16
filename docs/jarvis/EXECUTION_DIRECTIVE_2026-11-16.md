PROJECT J.A.R.V.I.S.

ULTIMATE MASTER EXECUTION DIRECTIVE

FULL-SYSTEM RECOVERY • FEATURE IMPLEMENTATION • VOICE ENGINE • COMPUTER CONTROL • 100× VERIFICATION

You are now acting as the principal engineering team responsible for completing my existing J.A.R.V.I.S. AI assistant.

You must operate as a coordinated team of senior specialists in:

* Android engineering
* Kotlin and Jetpack Compose
* Artificial intelligence and LLM integration
* Speech recognition and voice activity detection
* Audio signal processing
* Text-to-speech synthesis
* Android system integration
* Desktop automation
* Backend development
* Secure device communication
* UI/UX engineering
* Animation and rendering performance
* Software testing and quality assurance
* Build engineering and release management
* Security, privacy, and reliability

Your goal is to transform my existing J.A.R.V.I.S. project into the most capable, reliable, responsive, polished assistant possible within the actual capabilities of the supported platforms.

This is not a request to create a new application.

You are continuing development of an existing, substantial Android project.

Do not rebuild from zero.

Do not erase existing functionality.

Do not replace real functionality with fake demonstrations.

Do not silently remove features.

Do not claim completion without executable evidence.

⸻

PART 1 — PROJECT IDENTITY

Repository:
sepehrdadehsani89-hash/gdp-dashboard

Development branch:
jarvis-next

Last reported release:
J.A.R.V.I.S. 2.11.8

Previously reported audit checkpoint:
a7f2c83

Previously reported rollback branch:
checkpoint/jarvis-next-before-core-repair

Previous audit directory:
docs/jarvis/

Existing application:
J.A.R.V.I.S. Android AI Assistant

Existing visual architecture:
Jetpack Compose Canvas for the main assistant interface.

Existing wallpaper architecture:
Native Android Canvas.

Existing master specification:
24 parts, including Part 24 as the integration directive and Parts 01–23 as the detailed original specification.

Previously reported original specification length:
2,386,999 words.

Verify all relevant repository and checkpoint details rather than assuming that earlier reports are still current.

⸻

PART 2 — MASTER SPECIFICATION AUTHORITY

The complete 24-part ZIP was previously provided in this conversation.

Locate it.

Locate the extracted source files.

Locate the previously created searchable index.

Locate the provenance ledger.

Verify the file hashes against the previously recorded audit.

Do not rewrite or modify the original master prompts.

Read Part 24 first to understand the overall integration architecture.

Use Parts 01–23 as the authoritative detailed requirements.

Do not repeatedly load all 2,386,999 words into context.

Instead:

1. Use the existing index.
2. Identify requirements by feature.
3. Retrieve the relevant original sections.
4. Compare each requirement against the actual implementation.
5. Record missing functionality.
6. Implement missing or incomplete functionality.
7. Verify the result.
8. Record evidence.
9. Continue to the next requirement.

Every requirement must be traceable to its source.

Maintain a permanent requirements-to-code-to-test mapping.

If the ZIP or index is unavailable, stop and identify the missing files instead of inventing their contents.

⸻

PART 3 — CRITICAL SAFETY AND RECOVERY

Before modifying the application:

Verify:

* Git branch.
* Git status.
* Current commit.
* Remote commit.
* Existing backup branches.
* Local uncommitted changes.
* Remote synchronization.
* Available build tools.
* Available disk space.
* Available memory.
* Existing release artifacts.

Confirm that the latest checkpoint is backed up remotely.

If it is not, prepare a safe backup and request confirmation before pushing.

Never use destructive Git operations.

Never force-push.

Never delete the existing Android project.

Never delete user data.

Never overwrite encryption keys.

Never expose API credentials.

Never modify production services without authorization.

Create small, meaningful commits after verified milestones.

Keep rollback instructions for every substantial change.

⸻

PART 4 — COMPLETE FEATURE AUDIT

Perform a comprehensive audit of every existing application feature.

Do not audit only the most visible screens.

Inspect the entire repository.

Include:

* Kotlin source files.
* Java source files.
* Compose UI components.
* Android manifests.
* Services.
* Broadcast receivers.
* Background workers.
* Permissions.
* Audio systems.
* AI integrations.
* Networking.
* Local storage.
* Encryption.
* Device controls.
* Automation workflows.
* Wallpapers.
* Widgets.
* Notifications.
* Settings.
* Authentication.
* Backend services.
* Computer companion components.
* Tests.
* Build scripts.
* Release assets.

For each feature, record:

FEATURE ID

FEATURE NAME

ORIGINAL REQUIREMENT

SOURCE FILES

CURRENT IMPLEMENTATION

MISSING COMPONENTS

KNOWN DEFECTS

PLATFORM LIMITATIONS

SECURITY REQUIREMENTS

IMPLEMENTATION PLAN

TEST CASES

ACTUAL TEST RESULTS

FINAL STATUS

Use these statuses:

IMPLEMENTED_AND_VERIFIED

IMPLEMENTED_NOT_TESTED

PARTIALLY_IMPLEMENTED

MISSING

BROKEN

BLOCKED_BY_ENVIRONMENT

UNSUPPORTED_BY_PLATFORM

REQUIRES_USER_AUTHORIZATION

Never confuse source code presence with working functionality.

⸻

PART 5 — VOICE ASSISTANT ENGINE

The voice system is a major priority.

J.A.R.V.I.S. must be capable of receiving spoken requests, interpreting them, executing authorized actions, and responding naturally.

Audit and improve the complete audio pipeline.

Microphone access

Implement reliable microphone permission handling.

Check:

* Android RECORD_AUDIO permission.
* Runtime permission requests.
* Microphone availability.
* Audio input initialization.
* Audio session lifecycle.
* Audio interruptions.
* Bluetooth microphone compatibility.
* Wired headset compatibility.
* Device microphone selection where supported.
* Background microphone restrictions.
* Audio focus.
* Error recovery.

Never record audio without appropriate user permission.

Speech recognition

Investigate and integrate appropriate recognition engines, including:

* Android SpeechRecognizer.
* Sherpa-ONNX where compatible.
* Offline speech recognition where practical.
* Optional cloud recognition when authorized.
* Automatic fallback between available engines.

Do not claim offline support unless the required model and inference runtime actually work.

Voice activity detection

Investigate Silero VAD or a compatible alternative.

Implement:

* Speech start detection.
* Speech end detection.
* Silence handling.
* Noise tolerance.
* Timeout handling.
* Audio buffer management.
* False-trigger reduction.
* Microphone release.
* Recovery after interruption.

Wake-word behavior

Implement reliable wake-word detection where technically supported.

Support an appropriate wake phrase such as:

“Hey Jarvis”

Wake-word functionality must obey Android microphone and background execution restrictions.

Do not promise unrestricted always-listening behavior on unsupported devices.

Text-to-speech

Implement reliable spoken responses.

Support:

* Natural speech output.
* Configurable voice.
* Configurable speech rate.
* Interruption handling.
* Queue management.
* Cancellation.
* Audio focus.
* Recovery after engine failure.
* Clear status reporting.

Avoid overlapping responses.

Voice interaction state machine

Implement and verify:

IDLE

LISTENING

PROCESSING

THINKING

SPEAKING

EXECUTING_ACTION

WAITING_FOR_CONFIRMATION

ERROR

OFFLINE

Ensure that UI animations accurately reflect these states.

Do not display a successful action unless the action actually succeeded.

⸻

PART 6 — ARTIFICIAL INTELLIGENCE SYSTEM

Audit the existing AI architecture.

Determine which AI providers and models are actually supported.

Implement a modular AI provider architecture where appropriate.

Include:

* Secure backend connections.
* Configurable provider selection.
* Supported model selection.
* Connection health checks.
* Authentication validation.
* Streaming responses where available.
* Timeout handling.
* Retry limits.
* Rate-limit handling.
* Context management.
* Conversation history.
* Error explanations.
* Offline fallback where feasible.
* Safe action execution.
* User confirmation for sensitive operations.

Never hardcode secret API keys in the Android APK.

Never claim a model is free, unlimited, or available without verification.

Never simulate an AI response when the backend is disconnected.

If no AI provider is configured, show an understandable setup message.

⸻

PART 7 — ANDROID DEVICE CONTROL

J.A.R.V.I.S. should support useful Android actions through authorized system APIs.

Audit and implement supported capabilities such as:

* Open installed applications.
* Search installed applications.
* Launch supported activities.
* Open Android settings screens.
* Read battery status.
* Read charging status.
* Report network connectivity.
* Report device information.
* Read available storage information.
* Manage user-approved notifications where supported.
* Start phone calls through authorized system intents.
* Search contacts with permission.
* Open maps and navigation.
* Create reminders.
* Create alarms.
* Open calendar events.
* Control supported media playback.
* Adjust supported audio settings.
* Manage compatible Bluetooth actions.
* Interact with supported smart-home integrations.
* Execute explicitly authorized automation recipes.

Never bypass Android security restrictions.

Never silently activate sensitive permissions.

Never pretend to have completed an action that Android blocked.

Use system permission dialogs and user confirmation where required.

For destructive, financial, security-sensitive, or externally visible actions, require appropriate confirmation.

⸻

PART 8 — COMPUTER CONTROL COMPANION

Computer control is now a major development priority.

The user has explicitly allowed computer support to replace iPad-specific control features when iPadOS restrictions prevent implementation.

Design a secure computer companion for supported desktop platforms.

Prioritize:

Windows

macOS

Linux

Implement the architecture in stages.

Secure pairing

Support:

* Explicit device pairing.
* QR-based pairing where practical.
* Authentication.
* Revocable device authorization.
* Encrypted communication.
* Session expiration.
* Device identity verification.
* Connection status.
* Audit logs.
* Clear disconnection controls.

Authorized computer actions

Investigate and implement supported actions such as:

* Open applications.
* Open approved websites.
* Open files and folders.
* Search files.
* Read authorized file contents.
* Create documents.
* Manage approved workspace files.
* Control media playback.
* Report battery status.
* Report system status.
* Read resource usage.
* Manage windows where supported.
* Execute approved automation workflows.
* Interact with supported productivity applications.
* Run user-approved development commands.
* Display desktop notifications.
* Send files between paired devices.

Computer actions must run through a dedicated, permission-controlled companion.

Do not expose an unrestricted remote shell to the internet.

Do not implement hidden surveillance, credential collection, or permission bypasses.

Require explicit approval for destructive or sensitive operations.

Use least-privilege permissions.

Remote voice workflow

The intended user experience:

The user speaks to J.A.R.V.I.S. on Android.

J.A.R.V.I.S. understands the request.

J.A.R.V.I.S. identifies the authorized paired computer.

J.A.R.V.I.S. sends a structured command.

The computer companion validates authorization.

The action executes.

The computer returns a real result.

J.A.R.V.I.S. reports the actual outcome.

If the computer is offline, J.A.R.V.I.S. must report that it is offline.

Never fabricate execution success.

⸻

PART 9 — IPAD PLATFORM POLICY

Do not waste development time trying to bypass iPadOS restrictions.

If an iPad feature is unsupported, classify it as:

UNSUPPORTED_BY_IPADOS

Then evaluate whether the equivalent functionality can be provided through:

* Android.
* Windows.
* macOS.
* Linux.
* A supported web interface.
* An authorized backend.

Retain a Safari companion only where it provides genuine value.

Do not prioritize iPad development over repairing the existing Android application.

Do not create fake system controls for iPad.

⸻

PART 10 — HOLOGRAPHIC ANIMATION REPAIR

The existing J.A.R.V.I.S. interface is a core part of the application identity.

Preserve its established visual design.

Audit:

* Compose Canvas rendering.
* Native Canvas wallpaper rendering.
* Frame scheduling.
* Animation loops.
* State transitions.
* Coroutine scopes.
* Lifecycle awareness.
* Surface visibility.
* Recomposition.
* Frame invalidation.
* Main-thread blocking.
* GPU workload.
* Memory allocations.
* Particle lifecycle.
* Pause and resume logic.
* Screen rotation.
* Background and foreground transitions.
* Battery optimization behavior.
* Error handling.

Investigate the reported freezing problem.

Do not guess the root cause.

Collect evidence through supported static checks, unit tests, instrumentation, frame profiling, or device logs.

Where runtime evidence is unavailable, report the limitation and continue with verifiable work.

Implement minimal, targeted repairs.

Preserve the original appearance and interactions.

Do not redesign the holographic interface unless I explicitly authorize it.

⸻

PART 11 — VISUAL POLISH AND INTERACTION QUALITY

Improve reliability and polish without changing the established J.A.R.V.I.S. design language.

Evaluate:

* Animation smoothness.
* State synchronization.
* Touch responsiveness.
* Loading indicators.
* Error states.
* Accessibility.
* Screen reader compatibility.
* Text scaling.
* Color contrast.
* Responsive layouts.
* Different Android screen sizes.
* Orientation changes.
* Keyboard handling.
* Gesture conflicts.
* Battery usage.
* Memory usage.
* Startup performance.

Preserve working animations and visual effects.

Any improvement must maintain or enhance existing functionality.

⸻

PART 12 — STORAGE AND MEMORY

Audit the application’s data architecture.

Include:

* Settings persistence.
* Conversation storage.
* Encrypted secrets.
* Paired devices.
* Saved automation recipes.
* User preferences.
* Local caches.
* Offline models.
* Database migrations.
* Backup and restore.
* Data export.
* Data deletion.
* Recovery after interrupted writes.

Do not delete existing user data.

Test migration compatibility.

Use secure storage for sensitive credentials.

Avoid unnecessary collection of personal information.

⸻

PART 13 — ERROR HANDLING

Every major subsystem must handle failure.

Test scenarios including:

* No internet connection.
* AI provider unavailable.
* Invalid API credentials.
* Microphone permission denied.
* Microphone disconnected.
* Speech recognition timeout.
* TTS engine unavailable.
* Bluetooth disconnected.
* Paired computer offline.
* Backend unavailable.
* Authentication expired.
* Low storage.
* Low memory.
* App backgrounded.
* Android process restarted.
* Device reboot.
* Interrupted network request.
* Corrupted cache.
* Unsupported platform capability.

Failures must produce meaningful messages.

Do not leave the application frozen.

Do not create infinite retry loops.

Do not consume resources indefinitely.

⸻

PART 14 — 100× TESTING REQUIREMENT

I want extremely thorough verification.

For every significant feature, create a structured verification plan targeting 100 meaningful checks or repetitions where technically appropriate.

Do not simply run the same meaningless test 100 times.

Use a balanced combination of:

* Functional tests.
* Unit tests.
* Integration tests.
* State transition tests.
* Permission tests.
* Failure recovery tests.
* Lifecycle tests.
* Regression tests.
* Performance tests.
* Security tests.
* Compatibility tests.
* Repeated stability tests.

For deterministic behavior, run appropriate repeated checks.

For performance-sensitive features, run repeated measurements and analyze variability.

For animations, evaluate multiple lifecycle and rendering scenarios.

For microphone systems, evaluate different permission and audio conditions.

For computer control, test authorization, disconnection, invalid commands, and actual execution.

For every test, record:

TEST ID

FEATURE

TEST PURPOSE

EXECUTION ENVIRONMENT

EXPECTED RESULT

ACTUAL RESULT

PASS / FAIL / BLOCKED / NOT RUN

ERROR DETAILS

REPAIR COMMIT

RETEST RESULT

Never count an unexecuted test as passed.

Never fabricate results.

If a test cannot run because a physical Android device is unavailable, classify it as BLOCKED.

Do not use excessive repeated tests when they provide no additional evidence.

Prioritize meaningful coverage, reliability, and resource efficiency.

⸻

PART 15 — AUTOMATIC REPAIR LOOP

For every confirmed defect:

1. Identify the affected feature.
2. Reproduce the problem where possible.
3. Gather evidence.
4. Identify the likely cause.
5. Implement the smallest reliable repair.
6. Run relevant tests.
7. Check for regressions.
8. Document the result.
9. Commit the verified repair.
10. Continue to the next defect.

Do not repeatedly modify code without understanding the failure.

Do not create endless repair loops.

Limit automatic retries.

If blocked, document the exact blocker and move to independent work that can be completed safely.

⸻

PART 16 — ANDROID BUILD AND RELEASE

Inspect the existing Gradle configuration.

Use compatible JDK and Android SDK versions.

Do not upgrade dependencies without a reason.

Verify:

* Gradle configuration.
* Dependency resolution.
* Kotlin compilation.
* Android compilation.
* Unit tests.
* Static analysis.
* Resource processing.
* Manifest merging.
* APK generation.
* Signing configuration.
* Version information.
* Release artifact integrity.

Do not overwrite existing signing credentials.

Do not invent signing keys and represent them as the original release identity.

Never claim an APK is installable until build and signing checks have passed.

Do not claim a physical-device test passed without a connected device.

⸻

PART 17 — PERFORMANCE OPTIMIZATION

Investigate:

* Cold startup.
* Warm startup.
* Memory usage.
* CPU usage.
* Battery consumption.
* Audio processing latency.
* Speech recognition latency.
* AI response latency.
* Animation frame stability.
* Network request efficiency.
* Background task scheduling.
* Storage overhead.
* Application responsiveness.

Use real measurements where tools are available.

Avoid unsupported performance claims.

Do not sacrifice correctness or security for artificial benchmark improvements.

⸻

PART 18 — SECURITY

Audit:

* API credential handling.
* Backend authentication.
* Paired device authentication.
* Encryption.
* Local secret storage.
* Network transport security.
* Permission requests.
* File access.
* Automation command authorization.
* Logging.
* Session expiration.
* Data retention.
* Third-party dependencies.

Avoid unrestricted device-control endpoints.

Never disable Android security protections.

Never request unnecessary permissions.

Never leak private data into build logs.

⸻

PART 19 — DEVELOPMENT PRIORITIES

Follow this priority order:

PRIORITY 0:
Preserve the repository and verified backups.

PRIORITY 1:
Establish a working Android build environment.

PRIORITY 2:
Repair confirmed compilation and dependency failures.

PRIORITY 3:
Repair the microphone, speech recognition, VAD, and TTS pipeline.

PRIORITY 4:
Investigate and repair the holographic animation freeze.

PRIORITY 5:
Verify AI backend connectivity and conversation handling.

PRIORITY 6:
Repair authorized Android device-control functions.

PRIORITY 7:
Develop secure computer companion functionality.

PRIORITY 8:
Complete remaining original specification requirements.

PRIORITY 9:
Perform regression, stability, security, and performance testing.

PRIORITY 10:
Produce verified release artifacts and documentation.

Independent tasks may proceed in parallel only when doing so is safe and resource-efficient.

⸻

PART 20 — CONTINUOUS DEVELOPMENT RECORD

Maintain:

docs/jarvis/FEATURE_AUDIT.md

docs/jarvis/REQUIREMENTS_MATRIX.md

docs/jarvis/TEST_RESULTS.md

docs/jarvis/VOICE_ENGINE.md

docs/jarvis/ANIMATION_DIAGNOSIS.md

docs/jarvis/COMPUTER_COMPANION.md

docs/jarvis/ENVIRONMENT_STATUS.md

docs/jarvis/CHANGELOG.md

docs/jarvis/RELEASE_READINESS.md

Update documents with actual evidence.

Do not claim success merely because documentation exists.

⸻

PART 21 — AUTONOMY AND AUTHORIZATION

You may autonomously:

* Inspect repository files.
* Analyze existing code.
* Write implementation plans.
* Implement approved features.
* Create targeted unit tests.
* Run reasonable local checks.
* Repair confirmed defects.
* Create local commits.
* Update technical documentation.

Request confirmation before:

* Pushing to remote GitHub.
* Deleting or overwriting existing data.
* Installing large toolchains or models.
* Changing production credentials.
* Deploying externally accessible services.
* Performing destructive operations.
* Making major architectural replacements.
* Incurring significant additional resource costs.

If the execution environment fails, stop immediately and report the error.

Do not consume credits through repeated failed retries.

⸻

PART 22 — REQUIRED PROGRESS REPORT

After each meaningful milestone, report:

COMPLETED WORK

FILES CHANGED

FEATURES IMPLEMENTED

FEATURES REPAIRED

TESTS EXECUTED

TESTS PASSED

TESTS FAILED

TESTS BLOCKED

KNOWN LIMITATIONS

CURRENT COMMIT

BACKUP STATUS

NEXT ACTION

Distinguish between verified facts and proposed work.

⸻

PART 23 — DEFINITION OF DONE

A feature is complete only when:

1. Its implementation exists.
2. It matches the applicable original requirements.
3. It compiles in the relevant build configuration.
4. Relevant automated tests pass.
5. Platform restrictions are respected.
6. Security checks are satisfied.
7. Regressions are assessed.
8. Evidence is documented.
9. Known limitations are disclosed.

Physical-device-only functionality cannot be marked fully device-verified without physical-device testing.

The project is not fully complete merely because a build succeeds.

⸻

PART 24 — EXECUTION COMMAND

Begin working on my existing J.A.R.V.I.S. project now.

First verify that the previous checkpoint is safely backed up to GitHub.

Then verify whether the Android toolchain installation completed successfully.

If the toolchain is ready, run the minimum appropriate build and test checks.

If those checks pass, begin the full feature-by-feature reconciliation using my existing 24-part specification.

Prioritize real microphone functionality, speech recognition, voice activity detection, text-to-speech, animation stability, AI connectivity, Android device control, and secure computer companion functionality.

For every defect, implement a targeted repair and verify it.

Target extensive, meaningful testing, including up to 100 checks or repetitions per significant feature where appropriate and practical.

Do not pretend blocked tests passed.

Do not invent unsupported functionality.

Do not destroy existing work.

Do not recreate the application.

Do not stop after writing a plan if implementation can safely proceed.

Continue through verified development milestones within the available execution time and resources.

When a real blocker requires my intervention, stop and provide one specific next action.

The ultimate objective is a working, reliable, secure, highly capable J.A.R.V.I.S. assistant with a polished Android interface, real voice interaction, authorized Android actions, and secure computer-control capabilities.

PRESERVE THE EXISTING PROJECT.

IMPLEMENT REAL FUNCTIONALITY.

TEST WHAT CAN ACTUALLY BE TESTED.

REPAIR VERIFIED FAILURES.

DOCUMENT ALL REMAINING LIMITATIONS.

BEGIN.