J.A.R.V.I.S. — ULTIMATE ENGINEERING RECOVERY DIRECTIVE

PROJECT PHOENIX: RESTORE, REPAIR, COMPLETE AND RELEASE

YOUR MISSION

You are the principal engineer responsible for rescuing and completing my existing J.A.R.V.I.S. Android AI assistant.

You must act as an expert team consisting of:

* Principal Android/Kotlin engineer
* Jetpack Compose rendering specialist
* Android graphics and animation engineer
* Speech recognition engineer
* AI integration architect
* Google authentication specialist
* Android permissions specialist
* UI/UX design lead
* Backend and security engineer
* Quality assurance engineer
* Release and deployment engineer

This project is experiencing serious problems.

The main holographic core is not animating correctly.

The live wallpaper animation is not functioning correctly.

Microphone and voice interaction require major repairs.

Settings need professional organization.

Google account integration and guided setup are required.

Previous development attempts have not produced the improvements I expected.

YOUR JOB IS TO FIX THESE PROBLEMS IN THE EXISTING APPLICATION.

Do not respond with a plan alone.

Do not claim success without verification.

Do not perform massive speculative rewrites.

Do not create a replacement application.

⸻

1. PROJECT INFORMATION

Repository:
sepehrdadehsani89-hash/gdp-dashboard

Branch:
jarvis-next

Last known release:
J.A.R.V.I.S. 2.11.8

Previously reported audit checkpoint:
a7f2c83

Existing specification:
24 master parts, including the integration introduction.

Previously indexed original specification:
2,386,999 words.

The original 24-part specification remains authoritative wherever its requirements are technically feasible and do not conflict with the recovery and verification rules in this directive.

Use the existing search index and provenance ledger to retrieve relevant requirements efficiently.

Do not repeatedly reread millions of words.

⸻

2. EMERGENCY APPLICATION RECOVERY

Before changing anything:

1. Verify the repository and branch.
2. Check Git status.
3. Inspect the latest commits.
4. Identify changes made by previous repair attempts.
5. Compare the current implementation against the last known working release.
6. Identify regressions.
7. Confirm whether checkpoint a7f2c83 was pushed to GitHub.
8. Preserve all uncommitted changes.
9. Establish a safe, non-destructive rollback point.
10. Verify that the existing application source and assets remain intact.

Do not use destructive Git commands.

Do not force-push.

Do not delete user data.

Do not overwrite existing release signing credentials.

If previous changes broke a working feature, investigate the exact regression and restore its functionality with the smallest appropriate correction.

Before implementing new features, establish a reproducible build baseline.

⸻

3. REPAIR THE MAIN HOLOGRAPHIC CORE

THIS IS A CRITICAL PRIORITY.

The holographic core must not appear frozen.

Preserve the existing J.A.R.V.I.S. design language and recognizable appearance.

Investigate the complete rendering architecture.

Inspect:

* Jetpack Compose Canvas
* Native Canvas
* Animation clocks
* Frame scheduling
* State updates
* Recomposition
* Lifecycle handling
* Coroutines
* Main-thread workload
* Particle systems
* Rendering invalidation
* Pause and resume logic
* Surface lifecycle
* Screen transitions
* Background restrictions
* Memory allocations
* CPU usage
* GPU workload

Identify why the core fails to animate.

Do not assume a cause without evidence.

Create focused tests and instrumentation to isolate the problem.

The core should support visually distinct states:

IDLE — cyan

LISTENING — purple

THINKING — animated processing state

SPEAKING — blue

CHARGING — green particle effects

ERROR — clear, accessible error indication

OFFLINE — appropriate disconnected state

Use the existing color and animation definitions wherever they already exist.

Maintain smooth, continuous animation while the relevant screen is active.

Use lifecycle-aware rendering.

Ensure that the animation does not freeze after:

* Returning from another app
* Opening Settings
* Closing Settings
* Starting speech recognition
* Ending speech recognition
* Receiving an AI response
* Starting TTS
* Stopping TTS
* Screen rotation
* Background and foreground transitions

Add an optional diagnostic overlay for development builds that reports animation state and measured frame behavior.

Do not invent FPS measurements.

Do not claim the freeze is repaired until it is reproduced and verified, or clearly disclose that runtime verification remains unavailable.

⸻

4. REPAIR THE LIVE WALLPAPER

The J.A.R.V.I.S. live wallpaper must operate as a real Android wallpaper service.

Investigate:

* WallpaperService implementation
* Engine lifecycle
* SurfaceHolder callbacks
* Canvas drawing
* Frame scheduling
* Visibility changes
* Surface recreation
* Battery behavior
* Rendering cancellation
* Memory leaks
* Background execution
* Device compatibility

The wallpaper must not permanently freeze after a normal visibility transition.

It must resume when the wallpaper becomes visible.

It must stop unnecessary rendering when hidden.

Preserve the existing visual identity.

Avoid uncontrolled background loops.

Ensure that the wallpaper remains independent from the main application screen where appropriate.

Test both the application core and the wallpaper separately.

Do not mark wallpaper behavior verified solely because the main app compiles.

⸻

5. COMPLETE MICROPHONE SYSTEM REBUILD AND REPAIR

The microphone system must become reliable and functional.

Inspect the existing audio pipeline before replacing components.

Implement or repair:

A. Microphone permissions

* RECORD_AUDIO permission
* Runtime permission handling
* Clear permission explanations
* Permission denial recovery
* Microphone availability checks
* Audio focus management
* Microphone release after use

B. Speech recognition

Evaluate and integrate compatible options:

* Android SpeechRecognizer
* Google-supported speech recognition services
* Sherpa-ONNX where suitable
* Offline recognition where supported
* Optional cloud recognition

Choose engines based on actual device compatibility, language support, performance, and available resources.

Do not install every engine blindly.

Use a configurable engine architecture.

C. Voice activity detection

Evaluate Silero VAD or a compatible alternative.

Implement:

* Speech-start detection
* Speech-end detection
* Silence detection
* Noise tolerance
* Audio buffering
* Timeout handling
* Recovery from audio interruptions

D. Noise management

Investigate supported Android audio processing capabilities.

Implement appropriate:

* Noise suppression
* Echo cancellation
* Audio gain management
* Input-level monitoring

Avoid unsupported claims about audio quality.

E. Wake word

Support an appropriate wake phrase:

“Hey Jarvis”

Implement wake-word functionality within Android’s permission and background execution restrictions.

Do not create unauthorized hidden recording behavior.

F. Voice output

Implement reliable TTS with:

* Natural voice selection
* Speech rate controls
* Voice previews
* Interruption handling
* Queue management
* Audio focus
* Cancellation
* Recovery from engine errors

G. Complete voice workflow

The intended interaction is:

User: “Hey Jarvis.”

J.A.R.V.I.S.: “Hello, sir. How can I help you?”

User: “Open YouTube.”

J.A.R.V.I.S. recognizes the request.

J.A.R.V.I.S. validates the requested action.

J.A.R.V.I.S. opens YouTube through an authorized Android mechanism.

J.A.R.V.I.S. confirms the actual result.

Do not pretend an action succeeded if it failed.

⸻

6. GOOGLE ACCOUNT INTEGRATION

Add professional Google account integration.

IMPORTANT:

Do not attempt to silently install or modify the official Google app.

Use supported Google SDKs, Google Identity Services, Android APIs, and authorized integrations.

The application should detect relevant supported Google services and guide the user to install or enable missing components when necessary.

Implement Google account sign-in using a supported authentication flow.

Use appropriate OAuth authorization and consent.

Never request Google passwords directly.

Never hardcode OAuth client secrets.

Never collect unrestricted Google account data.

Provide a professional Google connection experience.

Google connection features

Investigate and implement supported integrations for:

* Google account sign-in
* Google Calendar
* Gmail
* Google Drive
* Google Maps
* Google Search
* YouTube
* Google Assistant-compatible actions where supported
* Google speech recognition services
* Google Text-to-Speech
* Google Play services availability

Each integration must use an appropriate supported API or Android intent.

Some integrations require a Google Cloud project, OAuth configuration, API activation, verification, or user consent.

If external configuration is required, provide exact setup instructions instead of pretending the feature is already connected.

Do not claim access to Gmail, Drive, or Calendar without real authorization.

⸻

7. PROFESSIONAL FIRST-RUN SETUP WIZARD

Create a polished, interactive onboarding system.

When the user first launches J.A.R.V.I.S., guide them through every necessary setup step.

The wizard should be clear, animated, accessible, and easy to understand.

Welcome screen

Introduce J.A.R.V.I.S.

Explain its major capabilities.

Show the existing holographic core.

Provide a clear Get Started button.

Step 1 — Google account

Offer supported Google sign-in.

Explain why account access may be useful.

Allow users to skip optional connections.

Step 2 — Microphone

Explain microphone permission.

Request authorization through Android’s permission system.

Test microphone initialization.

Display the actual result.

Step 3 — Speech recognition

Detect available recognition engines.

Offer supported options.

Verify the selected engine.

Step 4 — Voice output

Allow voice selection.

Provide a real TTS preview.

Step 5 — AI provider

Explain available provider options.

Configure the secure backend.

Verify authentication and connectivity.

Show meaningful error messages.

Step 6 — Android permissions

Guide the user through supported permissions one by one.

Explain what each permission enables.

Open the appropriate Android settings page where necessary.

Do not bypass security restrictions.

Step 7 — Contacts and calling

Request permission only if the user enables the feature.

Provide an optional contact lookup test.

Step 8 — Notifications

Explain supported notification access.

Allow optional configuration.

Step 9 — Wallpaper

Offer the existing J.A.R.V.I.S. live wallpaper.

Launch the supported Android wallpaper selection interface.

Verify the wallpaper service is available.

Step 10 — Device pairing

Offer secure pairing with supported computers.

Use explicit user authorization.

Step 11 — Final system check

Verify:

* Microphone
* Speech recognition
* Text-to-speech
* AI backend
* Google authentication status
* Required permissions
* Animation state
* Wallpaper service availability
* Network connection
* Paired devices

Clearly distinguish successful checks from incomplete or blocked checks.

Finish with a professional setup summary.

Allow users to revisit any step later.

⸻

8. COMPLETE SETTINGS REDESIGN

The current Settings experience must be professionally organized.

Preserve working functionality.

Create a clean, intuitive hierarchy.

Suggested sections:

GENERAL

* Language
* Theme
* Accessibility
* Startup preferences

J.A.R.V.I.S. CORE

* Animation settings
* Visual effects
* Performance options
* Wallpaper settings

VOICE & AUDIO

* Microphone
* Speech recognition
* Wake word
* VAD
* Noise suppression
* TTS voice
* Voice testing

ARTIFICIAL INTELLIGENCE

* AI provider
* Model selection
* Backend URL
* Connection status
* Response preferences
* Conversation settings

GOOGLE SERVICES

* Account
* Calendar
* Gmail
* Drive
* Maps
* YouTube
* Connected services

ANDROID CONTROLS

* App launching
* Contacts
* Calling
* Notifications
* Media
* Device status
* Permissions

COMPUTER COMPANION

* Pair devices
* Connected devices
* Authorized actions
* Security settings
* Connection diagnostics

PRIVACY & SECURITY

* Data permissions
* Account management
* Encrypted storage
* Connected sessions
* Data export
* Data deletion

DIAGNOSTICS

* Microphone test
* Speech recognition test
* TTS test
* AI connectivity test
* Animation diagnostics
* Wallpaper diagnostics
* Build information
* Error reports

ABOUT

* Application version
* Release notes
* Licenses
* Documentation

Every settings control must have a real implementation.

Do not create decorative switches that do nothing.

Settings must persist appropriately.

Changing a setting should update the relevant subsystem.

Display clear confirmation and error states.

⸻

9. REAL ANDROID APPLICATION CONTROL

Support authorized actions including:

* Open installed applications
* Search installed applications
* Open YouTube
* Open Maps
* Open Settings
* Open Calendar
* Open supported websites
* Search the web
* Start authorized phone calls
* Read approved contacts
* Create alarms
* Create reminders
* Control supported media
* Report battery status
* Report charging status
* Report network status
* Report storage status

Use supported Android APIs and intents.

Do not attempt to bypass device locks or security protections.

Require confirmation for sensitive operations.

⸻

10. COMPUTER COMPANION

Develop a secure companion architecture for:

Windows

macOS

Linux

Support explicitly authorized actions such as:

* Opening applications
* Opening websites
* Searching approved files
* Managing authorized workspace documents
* Controlling media
* Displaying notifications
* Running approved automation workflows
* Reporting computer status

Use authenticated pairing and encrypted communication.

Do not expose an unrestricted remote shell.

Do not create hidden surveillance features.

Require appropriate confirmation for sensitive operations.

Do not prioritize unsupported iPad automation over functioning Android and desktop features.

⸻

11. FULL APPLICATION QUALITY AUDIT

Inspect every existing feature.

For each feature:

1. Locate its implementation.
2. Compare it with the original specification.
3. Identify incomplete behavior.
4. Identify broken behavior.
5. Identify regressions.
6. Implement targeted repairs.
7. Add appropriate tests.
8. Execute available tests.
9. Record actual results.
10. Continue only after reviewing the outcome.

Use the original 24-part specification as the requirements source.

Maintain a searchable requirements matrix.

Do not skip less visible features.

⸻

12. TESTING — 100-CHECK QUALITY TARGET

For each major subsystem, design up to 100 meaningful verification cases or repetitions where justified.

Prioritize:

* Functional correctness
* Lifecycle behavior
* Regression prevention
* Error recovery
* Permissions
* Security
* Performance
* State transitions
* Compatibility
* Repeated stability

Do not waste resources running identical tests without meaningful benefit.

For each test record:

TEST NAME
EXPECTED RESULT
ACTUAL RESULT
PASS / FAIL / BLOCKED
EXECUTION ENVIRONMENT
EVIDENCE
REPAIR STATUS

Never fabricate test results.

Never report emulator tests as physical-device tests.

Never mark unexecuted tests as passed.

If a device is unavailable, run applicable static and unit tests, then clearly identify the remaining device-dependent verification.

⸻

13. BUILD AND APK RELEASE

The final deliverable must be an actual Android application artifact, not merely source code or a progress report.

Use the existing project configuration and compatible Android toolchain.

Verify:

* Dependency resolution
* Kotlin compilation
* Android compilation
* Manifest merging
* Resource processing
* Unit tests
* Static analysis
* APK generation
* Package identity
* Version information
* Signing configuration
* APK integrity

Preserve the existing signing identity where available and authorized.

Do not expose private signing credentials.

Do not claim that a build is successful unless the build actually succeeds.

Do not claim that an APK is signed unless signing verification succeeds.

Do not claim device compatibility without appropriate evidence.

When a valid APK has been produced:

1. Save the release artifact.
2. Record its exact version.
3. Calculate its SHA-256 checksum.
4. Document the completed tests.
5. Identify remaining limitations.
6. Prepare a stable downloadable artifact location.
7. Verify that the download actually works.
8. Provide the direct APK link.

Prefer the existing GitHub repository’s release or download workflow where appropriate.

Do not overwrite existing release artifacts.

Request my confirmation before pushing or publishing to GitHub.

If the environment cannot produce a valid APK, report the exact blocker and do not invent a download link.

⸻

14. DEVELOPMENT RULES

Do not respond with generic promises.

Do not claim everything is fixed without evidence.

Do not redesign working components unnecessarily.

Do not destroy existing code.

Do not repeatedly install the same dependencies.

Do not enter endless retry loops.

Do not repeatedly consume credits on failing commands.

Do not reread the complete master specification unnecessarily.

Do not implement fake integrations.

Do not silently remove existing functionality.

Do not create nonfunctional UI controls.

Do not report blocked tests as successful.

Do not make unsupported performance claims.

Use incremental implementation.

Create focused commits.

Maintain rollback capability.

Use professional engineering judgment.

⸻

15. REQUIRED EXECUTION ORDER

PHASE 1:
Verify backup, repository, and existing project state.

PHASE 2:
Establish the Android build baseline.

PHASE 3:
Investigate and repair the holographic core animation.

PHASE 4:
Investigate and repair the live wallpaper animation.

PHASE 5:
Repair microphone and speech recognition.

PHASE 6:
Integrate and verify VAD and TTS.

PHASE 7:
Repair AI backend and conversation functionality.

PHASE 8:
Implement Google account integration and supported Google services.

PHASE 9:
Build the professional onboarding wizard.

PHASE 10:
Reorganize Settings and connect every control to real functionality.

PHASE 11:
Repair Android application-control capabilities.

PHASE 12:
Implement secure computer companion milestones.

PHASE 13:
Reconcile all remaining requirements from the original 24-part specification.

PHASE 14:
Run meaningful regression, security, performance, and stability tests.

PHASE 15:
Build and verify the final APK.

PHASE 16:
Prepare the verified download link and release documentation.

⸻

16. AUTONOMOUS WORK POLICY

You are authorized to inspect, implement, refactor carefully, test, document, and create local commits within the existing project.

Do not stop after every minor step to ask whether you should continue.

Proceed through safe, reversible development milestones.

Ask for confirmation before:

* Pushing commits
* Publishing releases
* Installing unexpectedly large dependencies
* Changing credentials
* Performing destructive operations
* Deploying external services
* Making major architectural replacements

If execution becomes unavailable, stop and report the exact blocker.

Do not waste credits retrying indefinitely.

⸻

17. FINAL DELIVERABLE

At completion, provide:

1. A summary of actual application repairs.
2. A list of implemented features.
3. A list of incomplete or unsupported features.
4. Animation verification evidence.
5. Wallpaper verification evidence.
6. Microphone verification evidence.
7. Google integration status.
8. Onboarding implementation status.
9. Settings implementation status.
10. Test execution results.
11. Current Git commit.
12. Backup status.
13. APK version.
14. APK checksum.
15. Verified APK download link, if successfully produced and published.

If some features remain blocked, clearly disclose them.

Do not describe the application as fully complete while major requirements remain unverified.

⸻

FINAL DIRECTIVE

I want my existing J.A.R.V.I.S. application repaired and improved.

I am dissatisfied with previous changes because the animations remain frozen and expected functionality has not improved.

Focus on observable, working results.

Restore the holographic core.

Restore the live wallpaper.

Repair the microphone.

Make speech recognition reliable.

Implement real voice responses.

Connect supported Google services.

Create professional account setup.

Create a guided first-run experience.

Organize all settings.

Fix broken application controls.

Preserve the existing design.

Test actual behavior.

Produce a verified Android APK.

Continue through safe, testable milestones without repeatedly asking for permission for routine development.

Work thoroughly, efficiently, and professionally.

Your success is measured by working, verified software—not by the length of your reports.

BEGIN WITH REPOSITORY VERIFICATION AND A BASELINE BUILD, THEN REPAIR THE CORE AND WALLPAPER BEFORE ADDING NEW FEATURES.