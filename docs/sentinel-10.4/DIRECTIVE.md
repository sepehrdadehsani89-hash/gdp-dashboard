# J.A.R.V.I.S. 10.4 — PROJECT SENTINEL ASCENSION
## THE ULTIMATE ANDROID AI ASSISTANT, SECURE VOICE-INITIATED UNLOCKING, SUPERDESIGN ANIMATIONS, OFFLINE INTELLIGENCE & REAL-TIME DEVICE AUTOMATION
### COMPLETE WORLD-CLASS MASTER IMPLEMENTATION DIRECTIVE FOR OPENAI CODEX

**PROJECT:** Existing JARVIS Android AI Assistant

**NEW VERSION:** JARVIS 10.4 — Project Sentinel Ascension

**PREVIOUS VERSION:** JARVIS 10.3 — Project Infinity Fusion

**PRIMARY DEVICE:** Xiaomi Redmi 9

**TECHNOLOGIES:** Native Kotlin, Jetpack Compose, Android SDK, WallpaperService, local AI inference, Android speech APIs, authorized external integrations

**PRIMARY MISSION:** Upgrade the existing JARVIS assistant with an advanced, secure, three-stage device-credential verification setup, voice-initiated screen unlocking, cinematic authentication animations, and all previously specified JARVIS capabilities.

**CRITICAL DIRECTIVE: PRESERVE EVERY EXISTING WORKING FEATURE. DO NOT REBUILD THE APPLICATION FROM SCRATCH.**

---

# 01 — MASTER PROJECT REFERENCES

## Existing Android project

Repository:

https://github.com/sepehrdadehsani89-hash/gdp-dashboard

Previous APK reference:

https://raw.githubusercontent.com/sepehrdadehsani89-hash/gdp-dashboard/ed1fdf2f2a53a480565d288d05cd644657bce1e1/downloads/Jarvis-0.1.1-arm64.apk

Inspect the latest repository state before modifying files.

Do not assume the older APK represents the current application.

## Superdesign animation project

Project URL:

https://superdesign.dev/teams/79a56f66-5f09-4ac5-9a2f-80274d1eb294/projects/2c7f6f27-92df-4c24-9b84-ea7d0e3b1629

Team ID:

`79a56f66-5f09-4ac5-9a2f-80274d1eb294`

Project ID:

`2c7f6f27-92df-4c24-9b84-ea7d0e3b1629`

Use actual accessible Superdesign animation assets and exported code as the reference.

If the project requires authentication or exports, identify the missing access rather than inventing the design.

## JARVIS voice identifier

`05b36da8574341d0803391491850db20`

This is the configured JARVIS-only text-to-speech voice identifier.

Verify its provider and compatibility before integrating it.

Never treat this identifier as an API key or speaker authentication credential.

---

# 02 — ULTIMATE DEVELOPMENT MISSION

You are a world-class engineering team specializing in:

- Android operating-system integration
- Kotlin and Jetpack Compose
- Android authentication
- Android lock-screen lifecycle
- Biometric and credential authentication
- Local AI inference
- Speech recognition
- Voice activity detection
- Text-to-speech systems
- Real-time graphics
- Native Android live wallpapers
- Intelligent device automation
- Bluetooth and networking
- Telephony
- Computer vision
- Audio engineering
- Database architecture
- Software security
- Performance optimization
- Quality assurance

Your task is to make JARVIS 10.4 substantially more capable while preserving JARVIS 10.3.

The new release must combine:

1. Secure voice-initiated unlocking.
2. Three-stage system-owned credential verification.
3. A new Security & Screen Control settings interface.
4. Cinematic authentication visuals.
5. Genuine live wallpaper animations.
6. Superdesign-derived interface transitions.
7. Neural Speed Boost.
8. Fast app launching.
9. Smart contact calling.
10. Fifteen contact shortcuts.
11. Twenty-five application aliases.
12. Continuous natural conversation.
13. Speech interruption.
14. Live transcription.
15. Screenshot capture and sharing.
16. Advanced Bluetooth and Wi-Fi tools.
17. Real battery diagnostics.
18. Voice-controlled alarms.
19. Genuine music recognition.
20. Spotify integration.
21. Weather globe.
22. Location intelligence.
23. Six optional voice profiles.
24. Local AI memory.
25. Daily Ecuador briefings.
26. Dedicated animated device interfaces.
27. Secure permission-aware task execution.

Do not silently drop functionality to make the upgrade easier.

---

# 03 — ABSOLUTE SECURITY REQUIREMENT

This section defines the new unlock feature.

I want a convenient JARVIS configuration experience that establishes whether Android's legitimate screen-lock authentication works.

However, Android's secure credential verification must remain under Android's control.

**JARVIS MUST NEVER STORE, LEARN, RECORD, REPLAY, OR TRANSMIT MY ACTUAL SYSTEM PIN, PASSWORD, OR UNLOCK PATTERN.**

The three verification stages must use Android-owned authentication interfaces.

The user enters their actual credential directly into Android's system UI, not into a custom JARVIS credential field.

JARVIS receives only the operating system's authentication result.

This is the fundamental implementation requirement.

## 3.1 The intended result

I want to configure JARVIS once.

After configuration, I want to say:

"Hey Jarvis, unlock my phone."

JARVIS should recognize the instruction immediately.

It should request the supported Android unlock workflow.

When Android requires authentication, the user completes it using the system PIN, pattern, password, or permitted biometric method.

Once Android confirms successful authentication, JARVIS may continue the previously requested action.

## 3.2 Important limitation

The calling word is not equivalent to successful device authentication.

Recognizing my voice does not authorize JARVIS to bypass a securely locked phone.

Do not implement automatic PIN entry, pattern drawing, credential injection, or lock-screen bypasses.

If Android reports that the device is already unlocked or its keyguard is dismissible without further user input, use the documented behavior.

Otherwise, request genuine system authentication.

---

# 04 — NEW SETTINGS SECTION: SCREEN SECURITY & VOICE UNLOCK

Create a new Settings category:

`SCREEN SECURITY & VOICE UNLOCK`

This section must look like part of the existing premium JARVIS interface.

Use a dark background, cyan holographic borders, precise typography, and elegant security-related animations.

## 4.1 Main settings page

Display:

**JARVIS SCREEN CONTROL**

Current device state:

`SCREEN LOCK: CONFIGURED / NOT CONFIGURED`

Voice unlock command:

`HEY JARVIS, UNLOCK MY PHONE`

Setup state:

`NOT CONFIGURED`

`VERIFICATION IN PROGRESS`

`VERIFIED`

`AUTHENTICATION REQUIRED`

`UNAVAILABLE`

Add working controls:

- Configure Android screen lock
- Verify device security
- Enable voice-initiated unlock
- Test unlock workflow
- Require authentication for sensitive operations
- Enable secure lock-screen JARVIS display
- View authentication status
- Reset JARVIS unlock configuration

## 4.2 Available lock types

JARVIS should correctly recognize whether Android reports that a secure screen lock exists.

The operating system may use:

- PIN
- Password
- Pattern
- Biometrics with a secure device credential

Do not claim to know the actual PIN or pattern.

Do not require JARVIS to inspect the credential.

Use the OS-reported security state.

## 4.3 Configuration button

Button label:

`CONFIGURE DEVICE SECURITY`

When selected:

1. Check whether the device supports a secure lock screen.
2. Check whether a secure lock is configured.
3. If not configured, offer to open Android's official security settings.
4. Allow the user to configure Android's system lock.
5. Return to JARVIS.
6. Recheck the device security state.
7. Begin the optional three-stage verification wizard.

---

# 05 — THREE-STAGE SECURE VERIFICATION WIZARD

This is the major new feature.

I want a setup sequence containing three confirmation stages.

The purpose is to verify that the user can successfully complete legitimate Android authentication before enabling the JARVIS unlock request feature.

**Do not create a custom PIN pad, custom password box, or imitation pattern screen.**

Use the official Android authentication interface for each verification challenge, when supported.

## Stage 1 — Initial verification

Display:

`SECURITY VERIFICATION`

`STEP 1 OF 3`

`CONFIRM YOUR DEVICE CREDENTIAL`

Button:

`VERIFY WITH ANDROID`

When pressed:

1. Check the Android version.
2. Check whether the device is securely configured.
3. Select the supported device-credential authentication mechanism.
4. Invoke the official authentication interface.
5. Wait for the system result.
6. If successful, mark stage one verified.
7. If authentication fails or is cancelled, do not advance.

Do not ask JARVIS to inspect or remember the credential.

## Stage 2 — Second verification

Display:

`SECURITY VERIFICATION`

`STEP 2 OF 3`

`CONFIRM AGAIN`

Invoke a second independent system-owned authentication challenge where supported.

If successful:

Display:

`VERIFICATION 2 COMPLETE`

If unsuccessful:

Display:

`VERIFICATION NOT COMPLETED`

Cancel the wizard and return to the security settings page.

Never store entered credential data.

## Stage 3 — Final verification

Display:

`SECURITY VERIFICATION`

`STEP 3 OF 3`

`FINAL CONFIRMATION`

Invoke the final authorized authentication challenge.

If successful:

Proceed to completion.

If authentication is cancelled or rejected:

Do not enable the voice-initiated unlock setting.

## 5.1 Consistency and failure behavior

Android itself must validate the credential each time.

JARVIS cannot directly compare the user's PIN or pattern because it never receives those secret values.

The system accepts or rejects each authentication attempt.

If stage two or three fails, is cancelled, or cannot be verified:

- Abort the setup.
- Do not enable unlock configuration.
- Clear temporary verification state.
- Display a concise message.
- Offer to restart when the user chooses.

Do not falsely report that the second credential differed from the first unless Android explicitly provides that information.

Use system rejection results rather than inventing password comparisons.

## 5.2 Successful setup

After all supported verification stages succeed:

1. Wait for the final system callback.
2. Confirm the authentication session completed successfully.
3. Set the JARVIS unlock-request preference to enabled.
4. Store only the non-secret configuration state.
5. Record the setup completion timestamp if useful.
6. Update the Settings interface.
7. Play a subtle success sound.
8. Animate a green-cyan confirmation ring.
9. Speak:

"Security verification successful. Voice-initiated unlocking is ready."

This means JARVIS is ready to request Android unlocking.

It does not mean JARVIS has learned the PIN, pattern, or password.

## 5.3 Android compatibility

Use appropriate official APIs for the installed Android version.

Investigate:

- `KeyguardManager`
- `BiometricPrompt`
- `BiometricManager`
- `DEVICE_CREDENTIAL`
- `KeyguardManager.isDeviceSecure()`

Use the appropriate supported credential-confirmation mechanism for older devices.

Check runtime compatibility on the Redmi 9.

If three independent system credential challenges are not supported reliably, preserve security and offer a single legitimate Android verification plus two clearly labeled setup confirmations instead.

Do not fabricate authentication success.

---

# 06 — VOICE COMMAND: UNLOCK MY PHONE

Recognize natural variations:

"Hey Jarvis, unlock my phone."

"Hey Jarvis, unlock the screen."

"Jarvis, unlock my device."

"Open my locked screen."

Resolve all valid variations to a single secure intent:

`REQUEST_DEVICE_UNLOCK`

## 6.1 Exact execution sequence

1. Detect the wake phrase.
2. Confirm recognition.
3. Capture the spoken command.
4. Identify the unlock intent.
5. Check the JARVIS voice-unlock preference.
6. Check whether the device is locked.
7. Check whether a secure lock is configured.
8. Determine whether an appropriate JARVIS activity can be presented under the current Android background execution rules.
9. Request a supported Android keyguard dismissal operation where appropriate.
10. Wait for Android's result.
11. If Android requires authentication, display its official credential UI.
12. Allow the user to authenticate through Android.
13. Continue only after successful authentication.
14. Update JARVIS's visual state.
15. Report the result accurately.
16. Return to the proper operating state.

## 6.2 Already unlocked

If the phone is already unlocked:

JARVIS may respond:

"Your phone is already unlocked."

Do not show an unnecessary authentication dialog.

## 6.3 Authentication required

If the phone is securely locked:

JARVIS should say:

"Device authentication required."

Then present the permitted Android authentication workflow.

## 6.4 Authentication success

After Android confirms dismissal:

- Finish the unlock animation.
- Restore the appropriate home or app view.
- Clear any temporary secure UI state.
- Continue an authorized pending command if applicable.

## 6.5 Authentication failure

If Android rejects or cancels authentication:

- Do not unlock.
- Do not continue protected actions.
- Return to a lock-screen-safe interface.
- Display a restrained error state.
- Avoid revealing private information.

Do not repeatedly trigger authentication without a new user action.

---

# 07 — CINEMATIC UNLOCK ANIMATION

Use the existing Superdesign-inspired JARVIS color system.

The unlock experience should feel sophisticated without delaying authentication.

## Stage 1 — Command recognized

The Sentinel reactor responds with a small cyan pulse.

Outer rings continue rotating.

## Stage 2 — Security processing

The reactor changes to a controlled cyan-violet security appearance.

Thin authentication rings appear.

The status displays:

`SECURE AUTHENTICATION REQUESTED`

## Stage 3 — Android authentication

The official Android authentication UI takes priority.

JARVIS must not overlay fake credential inputs over the system prompt.

## Stage 4 — Successful authentication

Once Android confirms success:

- A short green-cyan ring expands.
- Small light segments travel clockwise.
- The security rings contract.
- The interface transitions toward the requested destination.

## Stage 5 — Authentication cancelled

Use a restrained amber indication.

Return to standby safely.

Do not play a success animation.

Do not reveal sensitive content.

---

# 08 — LOCK MY PHONE

Preserve the command:

"Hey Jarvis, lock my phone."

Also support:

"Lock my screen."

"Turn off my screen."

Distinguish between actual secure locking and merely dimming the display.

## Implementation

1. Recognize the command.
2. Check whether an authorized Android lock capability is configured.
3. Use a documented supported system mechanism.
4. Request any necessary user approval.
5. Execute the action.
6. Verify the observable result when possible.
7. Clean up active JARVIS interactions.

An ordinary app may require an explicitly authorized device administrator capability for immediate locking.

Do not silently request elevated privileges.

Do not use hidden APIs or accessibility-based bypasses.

If direct locking is unavailable, explain the appropriate supported alternative.

---

# 09 — SCREEN-OFF JARVIS EXPERIENCE

Preserve the visual behavior from the existing JARVIS specifications.

When the phone screen is off and the approved wake service detects:

"Hey Jarvis"

JARVIS should process the command without automatically opening its generic home screen.

If the command requires a dedicated visual interface, use an Android-supported presentation mechanism.

## Lock-screen-safe display

When permitted:

- Show the Sentinel reactor.
- Display actual battery percentage.
- Display the custom clock.
- Show non-sensitive weather.
- Animate listening and speaking.
- Keep private content hidden.

The interface may visually cover launcher icons through a dedicated activity when Android allows that activity to appear.

Do not attempt to disable the Android keyguard.

Do not assume the app can always start an activity from the background.

Use legitimate fallback notifications when necessary.

---

# 10 — SUPERDESIGN ANIMATION INTEGRATION

Preserve the actual Superdesign project reference:

https://superdesign.dev/teams/79a56f66-5f09-4ac5-9a2f-80274d1eb294/projects/2c7f6f27-92df-4c24-9b84-ea7d0e3b1629

Use its accessible animations as the source design.

## Integration sequence

1. Retrieve the available project designs and exports.
2. Identify the animation components.
3. Inspect timing, easing, geometry, and colors.
4. Map each animation to an actual JARVIS state.
5. Translate compatible designs into native Android rendering.
6. Preserve the existing Sentinel reactor geometry.
7. Apply the JARVIS state colors.
8. Integrate the new security animation.
9. Test rendering on the Redmi 9.
10. Report any inaccessible source assets.

Do not replace functioning native components with inefficient web code unnecessarily.

---

# 11 — COMPLETE REACTOR COLOR SYSTEM

Preserve:

**Standby:** Blue/cyan

`#00B8FF`

**Listening:** Purple

`#A855F7`

**Speaking:** Cyan

`#00E5FF`

**Processing:** Blue-violet

`#7C4DFF`

**Charging:** Green

`#22C55E`

**Warning:** Amber

`#FFBF55`

**Error:** Red

`#FF5368`

**Authentication success:** Green-cyan

## Reactor behavior

- Real spinning concentric rings.
- Independent rotation speeds.
- Smooth wake activation.
- Slight 5–8% expansion.
- Actual microphone-reactive visuals.
- Actual TTS-reactive visuals where available.
- Real battery percentage.
- Green outer charging ring.
- Smooth transition back to standby.
- Low-power wallpaper mode.

The charging ring must coexist with listening or speaking colors.

---

# 12 — NEURAL SPEED BOOST

Preserve the complete local-brain optimization specification.

## Goals

- Faster command interpretation.
- Faster speech recognition.
- Faster first token.
- Faster response generation.
- Faster tool execution.
- Faster TTS startup.
- Better interruption responsiveness.
- Reduced unnecessary loading delays.

## Implementation

1. Benchmark the current model.
2. Measure time to first token.
3. Measure tokens per second.
4. Profile model initialization.
5. Identify redundant work.
6. Optimize quantization and supported native inference.
7. Reuse model state safely.
8. Implement efficient KV caching where supported.
9. Avoid repeated context rebuilding.
10. Stream generated tokens.
11. Improve TTS buffering.
12. Keep tool operations off the main UI thread.
13. Support cancellation.
14. Profile thermal and battery impact.
15. Compare performance before and after.

Do not sacrifice intelligence merely to improve benchmark numbers.

---

# 13 — FAST COMMAND EXECUTION

Straightforward commands must use the shortest reliable supported execution path.

Examples:

"Open Spotify."

"Call Dad."

"Turn on flashlight."

"What's my battery?"

"Open Bluetooth settings."

Do not invoke an expensive full-model reasoning pass unnecessarily when deterministic intent resolution is sufficient.

Use an execution architecture with:

**FAST PATH**

Simple native actions.

**LOCAL AI PATH**

Conversation and reasoning.

**ADVANCED TASK PATH**

Multi-step planning.

Do not add arbitrary ten-second waiting periods before opening an application or starting an authorized call.

---

# 14 — CONTINUOUS CONVERSATION

Preserve the natural conversation system.

When the user asks a question:

1. Detect speech activity.
2. Capture and transcribe the utterance.
3. Determine the intended question.
4. Use the appropriate information source.
5. Generate the answer.
6. Begin speaking promptly.
7. Preserve the topic.
8. Support immediate follow-ups.

Do not cut off active speech merely because ten seconds have passed.

Use inactivity-based session timing.

Allow longer conversations and responses.

---

# 15 — INTERRUPT JARVIS WITH "WAIT, WAIT"

Preserve the interruption feature.

When JARVIS is speaking and the user says:

"Wait, wait."

1. Detect the interruption.
2. Stop or pause TTS promptly.
3. Transition the reactor from cyan to purple.
4. Preserve conversation context.
5. Listen to the new instruction.
6. Interpret the correction.
7. Continue the conversation naturally.

Support additional phrases:

- Hold on.
- Stop talking.
- One second.
- Pause.

Avoid triggering interruptions from JARVIS's own voice.

---

# 16 — LIVE TRANSCRIPTION

Preserve the compact transcript beneath the reactor.

Show:

`YOU · [recognized words]`

`JARVIS · [spoken response]`

Use actual streaming speech recognition results.

Implement:

- Smooth scrolling
- Partial text
- Final text replacement
- Real speaker labels
- Correct text wrapping
- Multilingual rendering
- Persian right-to-left layout
- Expandable conversation history

When a resolved application or contact name is recognized, highlight it green.

Do not highlight uncertain matches as confirmed.

---

# 17 — FIFTEEN CONTACT SHORTCUTS

Preserve the contact nickname system.

Each shortcut includes:

- Contact nickname
- Linked contact
- Approved number
- Optional spoken aliases
- Edit
- Delete
- Test

Example:

"Hey Jarvis, call Dad."

The system must:

1. Resolve Dad.
2. Validate the saved contact reference.
3. Use a supported Android dialing or calling workflow.
4. Request any necessary user interaction.
5. Report the observable result.
6. Preserve the contact context.

Do not automatically dial emergency numbers through the AI.

---

# 18 — UNANSWERED-CALL FOLLOW-UP

Preserve the optional follow-up feature.

The configured fifteen-second threshold must not be treated as proof that a call was unanswered.

Use available reliable call-state information when permitted.

Otherwise offer an optional follow-up after the call flow returns.

If the user wants to leave a personal voice memo:

1. Ask permission.
2. Record only the user's new message.
3. Display recording status.
4. Allow cancellation.
5. Allow playback.
6. Save with confirmation.
7. Offer authorized sharing.

Support rejection phrases:

- No.
- No thanks.
- It's okay.
- Never mind.
- Cancel.

Never secretly record another person's call audio.

---

# 19 — TWENTY-FIVE APPLICATION ALIASES

Preserve twenty-five customizable app shortcuts.

Each includes:

- Actual installed application
- Package identifier
- Custom nickname
- Optional spoken variants
- Test action
- Edit
- Delete

Example:

Actual app: Spotify

Nickname: Music

Command:

"Hey Jarvis, open Music."

Launch Spotify directly when Android permits.

Use fuzzy matching to correct common transcription errors.

Do not open the generic JARVIS activity first.

Respect background launch restrictions.

---

# 20 — SCREENSHOT INTELLIGENCE

Preserve voice-controlled screenshot capture and sharing.

Commands:

"Take a screenshot."

"Take a screenshot and send it."

"Share this screenshot with Dad."

Workflow:

1. Recognize intent.
2. Request required Android screen-capture consent.
3. Capture only permitted content.
4. Show preview.
5. Resolve the intended destination.
6. Query compatible sharing applications.
7. Open the Android sharing workflow.
8. Require appropriate user confirmation.
9. Report the observable outcome.
10. Clean temporary resources.

Use supported capture and URI-sharing APIs.

Never bypass protected content restrictions.

---

# 21 — WEATHER APPLICATION AND WEATHER GLOBE

Preserve two weather destinations.

**Existing weather application**

Launch the user's selected installed weather app.

**JARVIS weather interface**

Open the dedicated futuristic Earth HUD.

## Globe animation

1. Transition from the Sentinel reactor.
2. Render the holographic Earth.
3. Rotate the globe smoothly.
4. Determine the authorized current location.
5. Orient the globe toward the correct coordinates.
6. Zoom toward the selected region.
7. Mark the location.
8. Display real weather measurements.
9. Speak the forecast.
10. Synchronize captions and speech effects.

Use actual weather data from the existing authorized provider.

Do not invent geographic coordinates.

Do not claim decorative imagery is live satellite footage.

---

# 22 — WI-FI INTELLIGENCE

Preserve:

- Real connected SSID.
- Network state.
- Signal information.
- Frequency where available.
- Link-speed information.
- Real internet speed tests.
- Latency measurements.
- Connection diagnostics.
- Wi-Fi system controls where supported.

Commands:

"What's my Wi-Fi?"

"How fast is it?"

"Why is it slow?"

"Show available networks."

"Open Wi-Fi settings."

"Turn on Wi-Fi."

"Turn off Wi-Fi."

Use Android-supported control panels where radio changes are restricted.

Never invent test results.

---

# 23 — BLUETOOTH INTELLIGENCE

Preserve:

- Paired devices.
- Connected devices.
- Nearby discoverable devices when permitted.
- Device-name matching.
- Supported pairing workflows.
- Supported connection workflows.
- Accessory battery reporting.
- Conversation references.

Example:

"Show my Bluetooth devices."

"Connect to Pro 7."

"What's its battery?"

JARVIS must remember which accessory is being discussed.

Do not invent battery readings.

Do not claim the accessory connected unless the system confirms it.

---

# 24 — BATTERY INTELLIGENCE

Preserve the actual battery percentage in the reactor.

Support:

- Charging state.
- Battery health classification where available.
- Temperature where available.
- Voltage where available.
- Charging source.
- Battery reminders.
- Battery-saving information.
- Dedicated battery diagnostics HUD.

Do not fabricate capacity-health percentages.

## Charging visual

Use the green rotating outer ring.

Preserve cyan speaking or purple listening in the inner rings.

## Reserved future threshold configuration

Keep the previously discussed battery level bands documented as future configuration:

- 70–100%: Green.
- 30–69%: Yellow.
- 5–29%: Red.
- 1–5%: Critical.

The previously requested repeated spoken charging reminder at each percentage drop from 5% through 1% remains a future, disabled feature.

Do not enable these new threshold alerts automatically in JARVIS 10.4.

Do not remove existing battery alerts.

---

# 25 — CLOCK, ALARMS AND TIMERS

Preserve natural-language time understanding.

Commands include:

"Open my clock."

"Set an alarm for tomorrow at 7:30."

"Wake me up on Friday."

"Start a twenty-minute timer."

"Show my alarms."

Use appropriate supported Android Clock intents and scheduling mechanisms.

Interpret dates accurately.

Do not silently replace one-time alarms with repeating schedules.

Confirm actual results.

---

# 26 — REAL MUSIC RECOGNITION

Preserve the Shazam-style recognition system.

When the user says:

"Identify this song."

JARVIS should:

1. Request microphone permission.
2. Capture the authorized music sample.
3. Use a real recognition provider.
4. Receive actual song metadata.
5. Display the verified title and artist.
6. Offer an interpretation of the song.
7. Offer licensed lyrics sources.
8. Offer authorized Spotify actions.

Never invent song identities.

Never falsely claim a song was saved.

---

# 27 — CAMERA AND AI VISION

Preserve:

- Front-facing CameraX preview.
- Voice-controlled selfie camera.
- User-requested photo capture.
- Camera switching.
- Photo selection.
- Privacy-conscious image analysis.
- Optional comparison with approved reference images.

Do not secretly capture images.

Do not make unsupported medical diagnoses from facial appearance.

Do not analyze personal photos without authorization.

---

# 28 — SIX VOICE PROFILES

Preserve:

**One owner profile**

**Five additional consenting profiles**

Allow approximately twenty seconds of initial enrollment, with extra recordings when necessary.

Use suitable speaker embeddings.

Keep profiles separate from the JARVIS output voice ID.

Voice identification is not secure device authentication.

A recognized voice must never bypass Android's PIN, pattern, password, or biometric requirements.

---

# 29 — DAILY ECUADOR INTELLIGENCE

Preserve the optional daily briefing.

Suggested schedule:

9:00 AM, America/Guayaquil.

Include selected categories:

- Ecuadorian news
- Quito news
- Technology
- Artificial intelligence
- Science
- Weather
- Calendar information
- Reminders

Use real sources.

Respect notification permissions.

Allow quiet hours.

Do not invent headlines.

---

# 30 — TEN SAVED WEBSITES

Preserve ten customizable website entries.

Each includes:

- Name
- HTTPS URL
- Optional description
- Open
- Edit
- Delete

Allow voice commands using the assigned nickname.

Support authorized webpage retrieval and summaries.

Respect login boundaries.

Treat webpage content as untrusted information.

---

# 31 — LOCAL AI MEMORY

Preserve:

- Conversation context
- Explicit saved memories
- User preferences
- App nicknames
- Contact nicknames
- Website shortcuts
- Device aliases
- Task state
- Relevant prior conversation summaries

Provide memory search, editing, deletion, and export.

Do not store every conversation permanently without approval.

Remove user-facing RAM statistics from the HUD if they remain.

Do not remove actual AI memory management or internal performance profiling.

---

# 32 — ALL MODULES MUST HAVE LIVE ANIMATED INTERFACES

Preserve dedicated HUDs for:

- Weather
- Location
- Battery
- Wi-Fi
- Bluetooth
- Network diagnostics
- System telemetry
- Music recognition
- Camera
- Clock and alarms
- Phone calling
- Screenshot sharing
- Memory
- Voice profiles
- Security and authentication

Use a common visual language.

Reuse the central reactor where appropriate.

Every animation must correspond to genuine application state.

No fake loading indicators claiming unavailable progress.

No simulated system results.

---

# 33 — UNIVERSAL TASK EXECUTION PIPELINE

Every request must follow a precise lifecycle.

## Step 1 — Receive

Capture the spoken or typed request.

## Step 2 — Normalize

Correct formatting and likely transcription errors.

## Step 3 — Classify

Identify the request category.

## Step 4 — Extract

Identify required entities such as application names, devices, contacts, dates, and locations.

## Step 5 — Resolve context

Use relevant previous conversation state.

## Step 6 — Select execution path

Choose:

- Deterministic fast tool
- Local AI reasoning
- Multi-step orchestration

## Step 7 — Check capabilities

Verify Android support and required permissions.

## Step 8 — Plan

Create a minimal reliable execution sequence.

## Step 9 — Execute

Invoke the supported actual tool.

## Step 10 — Verify

Check the result.

## Step 11 — Update interface

Show the correct animation state.

## Step 12 — Respond

Speak the confirmed result.

## Step 13 — Update context

Preserve relevant references.

## Step 14 — Clean up

Release unnecessary resources.

## Step 15 — Standby

Return to the appropriate user-enabled listening state.

Do not fabricate results.

---

# 34 — WHAT TO DO AFTER EVERY TASK

After executing a task:

1. Check whether it actually completed.
2. Identify partial failures.
3. Provide concise confirmation.
4. Update relevant interface state.
5. Keep the current app or device reference available.
6. Clean up temporary resources.
7. Avoid duplicate notifications.
8. Return to listening when appropriate.
9. Preserve persistent memories.
10. Never report success without evidence.

For failed actions:

- Explain the blocker.
- Do not invent completion.
- Retry only when safe.
- Offer the closest supported workflow.
- Do not repeatedly request credentials or permissions without user action.

---

# 35 — SECURITY AND PRIVACY

All personal data must be handled securely.

## Protect:

- Voice recordings
- Voice-profile templates
- Contact information
- Saved websites
- Location information
- Screenshots
- AI memories
- Authentication configuration
- API credentials

## Mandatory security rules

- Never store system unlock PINs.
- Never store system passwords.
- Never record unlock patterns.
- Never replay system credentials.
- Never use voice identity to bypass a secure lock.
- Never use hidden Android authentication APIs.
- Never use accessibility services to imitate credential entry.
- Never expose private information on an unauthenticated lock screen.
- Never silently share screenshots or recordings.
- Never embed private API keys in a public APK.

Use Android Keystore for appropriate application secrets where needed.

Store only legitimate application-owned state and tokens.

---

# 36 — PERFORMANCE REQUIREMENTS

Optimize for the Redmi 9.

Use:

- Efficient native rendering.
- Cached graphics geometry.
- Lifecycle-aware wallpaper updates.
- Lightweight particle systems.
- Model streaming.
- Fast intent routing.
- Cancellable AI inference.
- Safe memory limits.
- Audio session coordination.
- Efficient data caching.
- Background resource cleanup.
- Appropriate thermal management.

Support:

`CINEMATIC`

`BALANCED`

`ECO`

Do not sacrifice core AI intelligence for decorative rendering.

Do not keep expensive animation loops running while the display is off.

---

# 37 — EXACT IMPLEMENTATION PHASES

## PHASE 1 — PROJECT AUDIT

1. Inspect current source.
2. Identify implemented JARVIS 10.3 features.
3. Check the current build.
4. Inspect authentication-related code.
5. Inspect wake and screen presentation.
6. Inspect wallpaper rendering.
7. Inspect local AI performance.
8. Identify regression risks.
9. Preserve databases and settings.
10. Create an implementation plan.

## PHASE 2 — SECURITY SETTINGS

1. Add Screen Security & Voice Unlock.
2. Read secure-lock configuration state.
3. Add Android security settings navigation.
4. Add three-stage wizard UI.
5. Implement system-owned authentication challenges.
6. Add success and cancellation handling.
7. Store only non-secret configuration.
8. Test setup persistence.
9. Test device credential changes.
10. Test setup reset.

## PHASE 3 — VOICE-INITIATED UNLOCK

1. Register the unlock intent.
2. Add natural-language variations.
3. Check device lock state.
4. Check user-enabled configuration.
5. Integrate documented keyguard dismissal.
6. Handle system authentication requirements.
7. Handle cancellation and errors.
8. Preserve lock-screen privacy.
9. Add animations.
10. Test on supported Android versions.

## PHASE 4 — SCREEN LOCK

1. Register the lock intent.
2. Determine authorized locking capabilities.
3. Request appropriate user approval.
4. Implement the supported lock workflow.
5. Add animation.
6. Verify observable outcomes.
7. Handle unsupported devices.

## PHASE 5 — SUPERDESIGN

1. Inspect accessible project designs.
2. Map animations to JARVIS modules.
3. Extract timing and movement.
4. Translate to native Android.
5. Apply colors.
6. Add security animations.
7. Verify visual fidelity.
8. Optimize performance.

## PHASE 6 — NEURAL SPEED

1. Benchmark local inference.
2. Identify bottlenecks.
3. Optimize model loading.
4. Implement streaming.
5. Improve command fast paths.
6. Optimize TTS startup.
7. Improve interruption latency.
8. Compare measured results.

## PHASE 7 — PRESERVE ALL EXISTING TOOLS

Verify:

- App launching
- Contact calling
- Bluetooth
- Wi-Fi
- Battery
- Location
- Alarms
- Screenshots
- Music recognition
- Spotify
- Camera
- Memory
- Weather
- Websites
- Daily briefings

Fix regressions.

## PHASE 8 — FULL UI INTEGRATION

1. Integrate the security Settings page.
2. Connect actual authentication state.
3. Implement security animations.
4. Preserve the Sentinel wallpaper.
5. Preserve live captions.
6. Preserve dedicated feature HUDs.
7. Validate navigation.
8. Test screen sizes.

## PHASE 9 — TESTING

1. Run unit tests.
2. Run UI tests.
3. Test security setup.
4. Test voice unlock requests.
5. Test authentication failures.
6. Test authentication cancellation.
7. Test the locked-screen state.
8. Test app background restrictions.
9. Test local AI speed.
10. Test existing features.
11. Test database migrations.
12. Test long-running stability.

## PHASE 10 — DELIVERY

1. Compile the project.
2. Fix build errors.
3. Run available regression tests.
4. Produce a debug APK if supported.
5. Report changed files.
6. Report new permissions.
7. Report device-specific limitations.
8. Report actual measured performance.
9. Explain the security setup workflow.
10. Provide truthful implementation status.

---

# 38 — REQUIRED SECURITY TESTS

Create tests for these scenarios.

## Test A — No screen lock configured

Expected:

JARVIS identifies that the device has no secure credential.

It offers Android screen-lock configuration.

It does not pretend that verification succeeded.

## Test B — Correct system credential

Expected:

Android accepts authentication.

JARVIS marks the current stage successful.

It does not read or retain the secret credential.

## Test C — Incorrect credential

Expected:

Android rejects authentication.

JARVIS does not mark the stage complete.

It does not store any attempted input.

## Test D — Stage two cancelled

Expected:

The wizard stops.

The setup is not enabled.

The interface offers a fresh start.

## Test E — Stage three cancelled

Expected:

The wizard stops.

The setup is not enabled.

No authentication state is falsely recorded.

## Test F — All stages complete

Expected:

JARVIS reports successful setup.

Only non-secret setup status is stored.

## Test G — Voice unlock while already unlocked

Expected:

JARVIS recognizes that no unlock is necessary.

## Test H — Voice unlock while securely locked

Expected:

Android's official unlock or authentication interface is requested when supported.

No automatic credential replay occurs.

## Test I — Authentication failure

Expected:

The phone remains locked.

Private information is not revealed.

## Test J — Background restriction

Expected:

JARVIS uses the permitted fallback instead of claiming that a blocked activity was launched.

---

# 39 — COMPLETE FEATURE REGRESSION CHECKLIST

Confirm that JARVIS 10.4 preserves:

1. Existing Android package and project.
2. Existing local AI brain.
3. Fast local inference.
4. Streaming AI responses.
5. Direct app opening.
6. Twenty-five application aliases.
7. Fifteen contact shortcuts.
8. Contact name highlighting.
9. App name highlighting.
10. Real calling workflows.
11. Optional unanswered-call follow-up.
12. User-approved voice messages.
13. Screenshot capture.
14. Native screenshot sharing.
15. Compatible share-app detection.
16. Continuous natural conversation.
17. "Wait, wait" interruption.
18. Persistent conversation context.
19. Existing JARVIS voice ID.
20. Six consenting speaker profiles.
21. English speech recognition.
22. Spanish speech recognition.
23. Persian speech recognition.
24. Live captions.
25. Smooth caption scrolling.
26. Actual animated wallpaper.
27. Existing Sentinel reactor design.
28. Smooth reactor zoom.
29. Purple listening.
30. Cyan speaking.
31. Green charging ring.
32. Actual battery percentage.
33. Battery health reporting where available.
34. Wi-Fi status.
35. Internet speed tests.
36. Network diagnostics.
37. Bluetooth discovery.
38. Bluetooth device-name correction.
39. Supported Bluetooth connection workflows.
40. Accessory battery reporting.
41. Front-facing camera.
42. User-approved image analysis.
43. Real music recognition.
44. Spotify actions where authorized.
45. Weather app selection.
46. JARVIS weather globe.
47. Real location targeting.
48. Location intelligence.
49. Alarms and timers.
50. Ten saved websites.
51. Daily Ecuador news.
52. Battery reminders.
53. User-approved memories.
54. Persistent settings.
55. Functional Assistant navigation.
56. Functional Memory navigation.
57. Functional Settings navigation.
58. Original Superdesign animation integration.
59. Cinematic sound effects.
60. Redmi 9 performance optimization.
61. Secure screen-lock setup.
62. Three-stage Android authentication verification.
63. Voice-initiated unlock requests.
64. Protected lock-screen presentation.
65. Supported voice-initiated device locking.

Do not declare the upgrade complete unless the implemented capabilities have been tested or clearly marked as blocked, unsupported, or unfinished.

---

# 40 — COMPLETE USER EXPERIENCE

My phone is resting on a desk.

The blue Sentinel reactor is visible on the live wallpaper.

Its concentric rings rotate gently.

The battery percentage remains centered.

I open JARVIS Settings.

I select:

**SCREEN SECURITY & VOICE UNLOCK**

JARVIS checks whether my phone has a secure Android lock configured.

I select:

**CONFIGURE VOICE UNLOCK**

JARVIS begins the three-stage verification wizard.

The official Android security interface asks me to authenticate.

JARVIS receives the authentication result.

When the verification stages complete successfully, JARVIS shows a green-cyan confirmation animation.

It says:

"Security verification successful. Voice-initiated unlocking is ready."

Later, when my phone is locked, I say:

**"Hey Jarvis, unlock my phone."**

JARVIS detects the command.

The Sentinel reactor enters its security animation where a permitted display surface is available.

JARVIS requests Android's documented unlock process.

If Android requires my credential or biometric confirmation, the system presents the official authentication interface.

After successful authentication, the phone unlocks.

JARVIS never needs to know, save, or replay my secret credential.

Then I say:

"Hey Jarvis, open Spotify."

JARVIS opens Spotify through the supported fast execution path.

I say:

"Call Dad."

JARVIS recognizes the correct saved contact nickname.

The contact's name turns green in the transcript.

The Android calling workflow starts.

I ask:

"What's the weather tomorrow?"

JARVIS offers the configured weather application or its own animated weather globe.

If I select the JARVIS interface, the Earth rotates toward my actual authorized location.

The forecast appears around the globe.

When JARVIS speaks, the reactor turns cyan.

When it listens, the reactor turns purple.

When the phone charges, the green outer ring moves around the circle.

My AI memory remains available.

My offline brain remains fast.

My custom voice remains configured.

My contacts, applications, websites, location, Bluetooth devices, music recognition, screenshots, and reminders remain connected through supported tools.

Every interaction must feel cohesive and professional.

Every animation must have a purpose.

Every device action must be real.

Every response must be appropriately accurate.

Every security decision must respect Android.

Every feature must preserve the existing application.

---

# 41 — FINAL IMPLEMENTATION DIRECTIVE

**DO NOT REBUILD JARVIS.**

**DO NOT REMOVE JARVIS 10.3 FEATURES.**

**DO NOT ERASE USER DATA.**

**DO NOT REPLACE REAL DEVICE INFORMATION WITH FAKE DATA.**

**DO NOT IMPLEMENT A CUSTOM SCREEN-LOCK CREDENTIAL COLLECTION SYSTEM.**

**DO NOT STORE OR REPLAY DEVICE PINS, PASSWORDS OR PATTERNS.**

**DO IMPLEMENT THREE-STAGE SYSTEM-OWNED AUTHENTICATION VERIFICATION WHERE SUPPORTED.**

**DO IMPLEMENT VOICE-INITIATED ANDROID UNLOCK REQUESTS.**

**DO PRESERVE THE SUPERDESIGN MOTION SYSTEM.**

**DO PRESERVE THE CONFIGURED JARVIS VOICE.**

**DO MAXIMIZE LOCAL AI PERFORMANCE.**

**DO PRESERVE ALL CONTACT, APP, CONNECTIVITY, MEDIA, WEATHER, MEMORY AND SYSTEM INTEGRATIONS.**

Inspect the actual current source code.

Create an implementation plan.

Modify the existing project safely.

Use native supported Android APIs.

Run available tests.

Verify critical security behavior.

Optimize for the Redmi 9.

Build the APK where the environment supports it.

Report actual completed work and limitations.

# J.A.R.V.I.S. 10.4 — PROJECT SENTINEL ASCENSION

**BEGIN THE COMPLETE APPLICATION UPGRADE NOW.**