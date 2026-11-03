# J.A.R.V.I.S. 10.0 — PROJECT OMNISYSTEM
## THE ULTIMATE ANDROID AI ASSISTANT, LIVING WALLPAPER, INTELLIGENT DEVICE CONTROL AND REAL-TIME HOLOGRAPHIC OPERATING EXPERIENCE

### COMPLETE MASTER ENGINEERING AND IMPLEMENTATION DIRECTIVE

**Project:** Existing JARVIS Android Assistant

**Version:** JARVIS 10.0 — Project OMNISYSTEM

**Primary device:** Xiaomi Redmi 9

**Platform:** Native Android, Kotlin, Jetpack Compose, Android SDK, native AI inference

**Main objective:** Build an exceptionally intelligent, responsive, reliable, local-first personal AI assistant with integrated Android tools and a premium interactive holographic interface.

**IMPORTANT: THIS IS A MAJOR UPDATE TO AN EXISTING APPLICATION. DO NOT REBUILD THE PROJECT FROM SCRATCH.**

Repository:

https://github.com/sepehrdadehsani89-hash/gdp-dashboard

Previous APK reference:

https://raw.githubusercontent.com/sepehrdadehsani89-hash/gdp-dashboard/ed1fdf2f2a53a480565d288d05cd644657bce1e1/downloads/Jarvis-0.1.1-arm64.apk

Use the most recent actual repository source, not assumptions based on the previous APK.

---

# 01. THE COMPLETE VISION

You are an elite engineering team consisting of:

- Android operating-system integration specialists
- Kotlin developers
- Jetpack Compose engineers
- Local AI inference specialists
- Natural-language processing engineers
- AI agent architects
- Speech recognition specialists
- Audio processing engineers
- Computer vision engineers
- Networking engineers
- Bluetooth specialists
- Cybersecurity engineers
- Real-time graphics developers
- UX and interaction designers
- Quality assurance engineers

Your task is to transform my existing JARVIS application into a unified Android personal intelligence system.

I want to use my phone naturally.

I want to speak commands.

I want JARVIS to understand them.

I want the assistant to perform real actions using supported Android capabilities.

I want JARVIS to answer questions using local AI, approved stored knowledge, real phone measurements, and optional internet sources.

I want the entire experience to be visually extraordinary.

JARVIS must have:

1. A genuine animated Android live wallpaper.
2. A cinematic voice-activation system.
3. A separate holographic interface for each major capability.
4. A highly optimized offline AI brain.
5. Intelligent conversation memory.
6. Real Android tool integrations.
7. Accurate system telemetry.
8. Advanced Wi-Fi and Bluetooth intelligence.
9. Real battery monitoring.
10. Genuine music recognition.
11. Natural-language alarms and timers.
12. Real location and navigation integration.
13. Front-camera capabilities.
14. Personalized voice profiles.
15. Proactive daily briefings.
16. Dynamic audio-reactive animations.
17. Natural, deep speech output.
18. A comprehensive execution and verification system.
19. Strong privacy and security controls.
20. Efficient performance suitable for my Android device.

All features must work together.

Do not create disconnected demonstrations.

Do not produce fake system readings.

Do not claim unsupported capabilities work.

---

# 02. INSPECT AND PRESERVE THE EXISTING APPLICATION

Before writing code, inspect the actual current repository.

Review:

- Main Android activity
- Application class
- Assistant runtime
- Voice recognition
- Wake listener
- Speech synthesis
- Local model inference
- Existing native AI libraries
- Tool execution system
- Memory storage
- Notifications
- Permissions
- Wallpaper implementation
- Compose interfaces
- Existing settings
- Existing integrations
- Build configuration
- Test infrastructure

Relevant earlier source files include:

`MainActivity.kt`

`AssistantRuntime.kt`

`local/LocalBrain.kt`

`local/LocalPromptBuilder.kt`

`local/ModelDownloadPolicy.kt`

`data/LlmClient.kt`

`data/BrainProtocol.kt`

`data/JarvisStore.kt`

`tools/DeviceTools.kt`

`core/PhoneContext.kt`

`voice/WakeListenerService.kt`

`voice/SpeechController.kt`

`ui/JarvisAppUi.kt`

`ui/JarvisUiState.kt`

Identify which capabilities are implemented and which are only planned.

Create a dependency map before editing.

Preserve:

- Existing user data
- Saved memories
- Conversation history
- Downloaded AI models
- API configurations
- User settings
- Device aliases
- Voice preferences
- Language support
- Existing functional tools

If a database change is needed, implement a migration.

Never silently destroy user data.

---

# 03. NEW JARVIS VISUAL ARCHITECTURE

This is the most important redesign.

JARVIS must no longer have only one generic interface for every request.

**Every major capability needs its own dedicated, futuristic interface.**

The interfaces must feel like specialized modules of a single holographic operating system.

For example:

Weather must have a weather HUD.

Bluetooth must have a device-discovery HUD.

Battery diagnostics must have a battery health HUD.

Location must have a map-oriented HUD.

Music recognition must have a music-identification HUD.

All interfaces must use a common visual design language.

## 3.1 Shared design system

Background:

`#000000`

Dark navy:

`#050B14`

Electric cyan:

`#00E5FF`

Blue:

`#00B8FF`

Listening purple:

`#A855F7`

Charging green:

`#22C55E`

Warning amber:

`#FFBF55`

Error red:

`#FF5368`

Use:

- Thin holographic borders
- Radial instrument markings
- Concentric circles
- Radar sweeps
- Digital waveform tracks
- Reactive glowing particles
- Microtext labels
- Information grids
- Smooth animated transitions
- Glasslike translucent data panels
- Real-time graphical indicators
- Clean typography
- Advanced but restrained lighting

Every effect must be intentional and efficient.

---

# 04. GENUINE LIVE WALLPAPER FOUNDATION

Implement or preserve a native Android live wallpaper using:

`WallpaperService`

and:

`WallpaperService.Engine`

This must be a real wallpaper, not a static screenshot or video imitation.

## 4.1 Wallpaper standby state

When no assistant activity is being displayed:

- The circular reactor remains visible on the home wallpaper.
- Rings rotate slowly.
- Small particles move subtly.
- The central battery percentage uses actual system data.
- Charging effects respond to charging state.
- The wallpaper uses minimal GPU resources.
- Normal home-screen icons remain available during ordinary phone use.

## 4.2 Wake-word behavior

Calling word:

`Hey Jarvis`

Also support configured Spanish and Persian equivalents:

`Oye Jarvis`

`هی جارویس`

When the wake phrase is detected:

1. Confirm recognition.
2. Prevent duplicate wake events.
3. Start listening to the command.
4. Identify the requested capability.
5. Decide whether a visual interface is appropriate.
6. Present the corresponding dedicated HUD where Android permits.
7. Preserve the current application when a direct action does not require opening JARVIS.
8. Execute the request.
9. Show or speak the result.
10. Return to standby when appropriate.

## 4.3 Critical navigation rule

Do not always open the generic JARVIS home screen.

If the user asks for weather, open the weather HUD where permitted.

If the user asks for Bluetooth details, open the Bluetooth HUD.

If the user asks for battery health, open the battery HUD.

If the user says:

"Open Spotify."

Launch Spotify directly where permitted.

Do not open the JARVIS home screen unnecessarily.

## 4.4 Full-screen mode

When a JARVIS HUD is displayed, use a dedicated immersive activity.

The activity should visually cover normal launcher icons and redundant app controls.

Keep the HUD dominant.

Hide optional system bars where Android permits.

Do not bypass mandatory security UI.

## 4.5 Locked display behavior

Support a lock-screen-compatible presentation only through documented Android mechanisms.

Use safe information before authentication.

Never bypass the system PIN, pattern, password, or biometric authentication.

If Android blocks an activity launch from the background, use a supported notification or other permitted user-initiated activation mechanism.

Do not claim universal automatic screen-on behavior.

---

# 05. UNIVERSAL JARVIS REACTOR

The main reactor is the shared visual identity of every module.

It must remain truly animated.

## 5.1 Speaking mode

Color:

**CYAN**

When JARVIS speaks:

- The inner reactor glows cyan.
- Outer rings rotate.
- Audio tracks animate around the circle.
- The animation reacts to available real speech output levels.
- Captions display the spoken response.
- The battery percentage remains visible.

## 5.2 Listening mode

Color:

**PURPLE**

When the user speaks:

- The reactor becomes purple.
- Audio bars react to actual microphone input.
- Rings pulse according to voice activity.
- Partial transcription appears in real time.
- Final transcription replaces unstable partial text.

## 5.3 Processing mode

Color:

**BLUE/VIOLET**

When the AI processes:

- Selected rings rotate faster.
- Data trails animate.
- The current task stage is displayed.
- The visual state remains smooth.

Never invent reasoning progress percentages.

## 5.4 Charging mode

Color:

**GREEN**

When the actual phone is charging:

- A separate outer ring becomes green.
- Green energy arcs rotate clockwise.
- Small particles move around the reactor.
- Real battery percentage updates.
- The charging animation continues independently of listening or speaking.

If speaking while charging:

Cyan inner reactor plus green outer charging ring.

If listening while charging:

Purple inner reactor plus green outer charging ring.

## 5.5 Standby

- Dim cyan
- Slow movement
- Minimal particles
- Low GPU usage
- Real battery percentage

---

# 06. PERFECT HUD LAYOUT

Create a consistent visual hierarchy.

## Top area

Display:

`J.A.R.V.I.S.`

A compact real-time clock.

The date.

A small weather module beside the clock.

Actual location name when available.

Connection status.

## Central area

Display:

- Main reactor
- Real battery percentage
- Current assistant state
- Dynamic audio tracks

## Below reactor

Display compact live captions.

Examples:

`YOU · What's the weather tomorrow?`

`JARVIS · Tomorrow's forecast is...`

Use actual transcript and response text.

## Bottom area

Display:

- Relevant module controls
- Assistant navigation
- Memory
- Settings
- Microphone
- Stop or cancel

Avoid displaying RAM usage in the normal HUD.

Preserve intelligent AI memory.

---

# 07. DEDICATED WEATHER INTELLIGENCE HUD

When the user says:

"Hey Jarvis, tell me the weather."

JARVIS should activate a dedicated weather interface where supported.

## 7.1 Visual design

Create a futuristic atmospheric display.

Include:

- Large actual temperature
- Animated current weather condition
- City name
- Current date and time
- Feels-like temperature
- Humidity
- Wind speed
- Rain probability where available
- Hourly forecast
- Daily forecast
- Sunrise and sunset
- Weather update timestamp

## 7.2 Dynamic visuals

Sunny:

Subtle golden light interacting with cyan holographic rings.

Rain:

Animated rainfall particles.

Cloudy:

Soft cloudlike atmospheric layers.

Storm:

Controlled electric-blue illumination without aggressive flashing.

Wind:

Directional flowing particles.

Choose effects from real weather data.

## 7.3 Exact execution

1. Recognize a weather-related request.
2. Determine the requested location.
3. Resolve whether it refers to the user's current or saved city.
4. Check location permission if needed.
5. Check existing cached weather.
6. Retrieve fresh data if necessary.
7. Validate source timestamps.
8. Convert units according to user settings.
9. Open the weather HUD when presentation is permitted.
10. Populate the actual returned measurements.
11. Play a subtle weather-interface activation sound.
12. Generate a concise explanation.
13. Speak the answer.
14. Animate the reactor in cyan during speech.
15. Update live captions.
16. Preserve the requested location and date for follow-up questions.
17. Return to standby or remain in the weather view according to user interaction.

## 7.4 Follow-ups

Understand:

"What about tomorrow?"

"Will it rain?"

"How cold will it get?"

"What about Friday?"

"Should I bring an umbrella?"

Use real forecast information.

Do not invent future measurements.

---

# 08. DEDICATED LOCATION INTELLIGENCE HUD

When the user asks:

"Where am I?"

Activate the location interface when appropriate.

## 8.1 Visual design

Create a holographic navigation display.

Include:

- Map viewport where a permitted provider is available
- Current-location marker
- Compass-style indicators
- Location accuracy indicator
- Current coordinates
- Human-readable address when available
- City and country
- Navigation shortcuts

Use a real authorized map service rather than a decorative fake map.

## 8.2 Exact execution

1. Recognize the location intent.
2. Check whether location services are enabled.
3. Check app permissions.
4. Request permission if missing.
5. Obtain a fresh location fix through supported location APIs.
6. Check timestamp and accuracy.
7. Reject stale or unsuitable location fixes when necessary.
8. Reverse-geocode the coordinates through an available service.
9. Display the result with accuracy information.
10. Open the location HUD when permitted.
11. Speak the result naturally.
12. Offer navigation or map opening if useful.
13. Avoid saving location history without permission.

## 8.3 Settings

Add:

`LOCATION INTELLIGENCE`

Include:

- Location permission
- Approximate or precise mode
- Provider settings
- Current-location test
- Map integration
- Saved destinations
- Location history controls

---

# 09. DEDICATED BATTERY HEALTH HUD

When the user says:

"Hey Jarvis, show battery health."

Open a dedicated battery diagnostics interface.

## 9.1 Visual design

Include:

- Large battery percentage
- Circular charge indicator
- Charging state
- Battery health classification where reported
- Battery temperature where available
- Voltage where available
- Charging current where available
- Power source
- Battery-saving status
- Estimated remaining charge time only when reasonably measurable

## 9.2 Battery health accuracy

Distinguish between Android's battery health classification and an estimated capacity-health percentage.

Do not fabricate a capacity-health number when the required data is unavailable.

## 9.3 Exact execution

1. Recognize the battery query.
2. Read supported Android battery properties.
3. Determine current charging state.
4. Check which health metrics are actually exposed.
5. Normalize units.
6. Validate returned values.
7. Populate the HUD.
8. Activate the appropriate charging animation.
9. Explain the available measurements.
10. Clearly identify unavailable properties.
11. Speak the answer.
12. Update the displayed values when Android reports changes.
13. Clean up temporary listeners properly.

## 9.4 Charging visual

When charging:

- Green outer reactor ring
- Rotating energy arcs
- Small flowing particles
- Actual charge percentage
- Optional short charging sound

---

# 10. DEDICATED SYSTEM CONNECTION HUD

Create:

`JARVIS SYSTEM LINK`

This module displays real connection information.

Include:

- Internet connection
- Active network transport
- Wi-Fi name
- Signal strength
- Bluetooth state
- Connected accessories
- Accessible accessory battery levels
- VPN status when available
- Current AI provider
- Local/online AI mode

Use small interactive cards.

Never display a status as connected merely because the app is running.

---

# 11. ADVANCED WI-FI INTELLIGENCE HUD

When the user asks about Wi-Fi:

1. Identify the current network.
2. Retrieve supported Android information.
3. Display the actual SSID.
4. Display signal strength.
5. Display available link speed information.
6. Distinguish link speed from internet throughput.
7. Offer a real speed test.
8. Diagnose available connection evidence.
9. Provide grounded troubleshooting suggestions.

## Visual design

Use:

- Cyan connection arcs
- Signal indicators
- Real speed-test charts
- Network quality labels
- Connection timeline
- Current SSID

## Commands

Support:

"What's my Wi-Fi?"

"How fast is my internet?"

"Why is it slow?"

"Show available Wi-Fi networks."

"Connect to this network."

"Open Wi-Fi settings."

"Turn on Wi-Fi."

"Turn off Wi-Fi."

Use Android-supported system controls where direct toggles are restricted.

Never bypass OS restrictions.

---

# 12. ADVANCED BLUETOOTH RADAR HUD

Create a specialized Bluetooth interface.

The main visual should resemble a futuristic scanning radar.

## 12.1 Device discovery

Display actual permitted:

- Paired devices
- Connected devices
- Nearby discoverable devices
- Device names
- Device types
- Available signal information
- Available battery levels

## 12.2 Device names

Use intelligent name matching.

Example:

"Connect to Pro Seven."

Resolve it to the actual available device named:

`Pro 7`

If multiple matches exist, request clarification.

## 12.3 Exact execution

1. Recognize a Bluetooth-related intent.
2. Determine whether the user requests discovery, connection, disconnection, or information.
3. Check Bluetooth state.
4. Request appropriate permissions.
5. Retrieve paired or discoverable devices.
6. Match the user-provided name.
7. Confirm ambiguous matches.
8. Determine whether a supported connection API or system workflow exists.
9. Start the appropriate operation.
10. Observe the resulting connection state.
11. Populate the device HUD.
12. Report the verified result.
13. Preserve device context for follow-up commands.

## 12.4 Follow-up questions

Understand:

"What's its battery?"

"Is it connected?"

"Disconnect it."

"Connect to the other one."

Remember which device is being discussed.

---

# 13. DEDICATED NETWORK DIAGNOSTICS HUD

When a speed test is requested:

1. Determine the active network.
2. Check internet availability.
3. Warn about significant data usage.
4. Request approval where appropriate.
5. Select an authorized testing endpoint.
6. Measure actual download throughput.
7. Measure upload throughput where supported.
8. Measure latency.
9. Calculate relevant performance statistics.
10. Display live charts.
11. Stop the test cleanly.
12. Analyze the results.
13. Explain likely performance limitations.
14. Offer useful troubleshooting actions.

Do not display hardcoded speed results.

Do not confuse Wi-Fi radio link rate with actual internet throughput.

---

# 14. DEDICATED SYSTEM TELEMETRY HUD

Build:

`JARVIS SYSTEM TELEMETRY`

Show supported actual metrics such as:

- Battery percentage
- Charging status
- Network status
- Connected Bluetooth device information
- Device storage information where available
- AI engine status
- Local model loaded/unloaded state
- Microphone status
- Current JARVIS activity
- Available thermal status signals

Remove user-facing RAM graphs and RAM percentages.

Do not remove actual memory management from the AI engine.

Do not invent CPU utilization or other inaccessible system statistics.

---

# 15. DEDICATED MUSIC RECOGNITION HUD

Implement the previously specified Shazam-style capability.

When the user says:

"Identify this song."

1. Confirm microphone access.
2. Pause conflicting speech capture.
3. Activate the music-identification screen.
4. Record a supported short sample, approximately ten seconds.
5. Generate the provider-required audio representation.
6. Submit it to a genuine music recognition service.
7. Wait for a verified match.
8. Display actual song metadata.
9. Show available artwork.
10. Offer a Spotify action where authorized.
11. Offer an explanation of the song's meaning.
12. Offer a licensed lyrics source.
13. Dispose of temporary recordings appropriately.

Never make up a song title.

## Visual design

Use:

- Large central waveform
- Animated frequency bars
- Music identification countdown
- Album artwork
- Song title
- Artist
- Result status
- Spotify controls

---

# 16. DEDICATED CLOCK AND ALARM HUD

When the user asks:

"Set an alarm for tomorrow at 7:30."

1. Extract the requested time.
2. Resolve the date.
3. Check the device timezone.
4. Determine whether the alarm is one-time or repeating.
5. Use the supported Android clock workflow.
6. Request any necessary user interaction.
7. Confirm the result.
8. Show the alarm HUD.
9. Speak a concise confirmation.
10. Preserve the relevant alarm context.

Support timers and alarms separately.

Use legitimate Android alarm and timer APIs.

Do not silently create repeating alarms for one-time dates.

## Visual design

Create:

- Animated clock dial
- Time selection indicator
- Alarm status
- Scheduled time
- Remaining duration when relevant
- Timer progress ring
- Dismiss and cancel controls

---

# 17. DEDICATED CAMERA AND AI VISION HUD

When the user says:

"Open selfie camera."

Open the front-facing camera through supported APIs.

Use CameraX where appropriate.

## Visual design

Include:

- Actual camera preview
- Thin futuristic corner brackets
- Capture control
- Switch camera
- Close button
- Camera status
- Optional selected-image analysis panel

## Analysis

Support user-requested image descriptions and comparisons.

Do not secretly record.

Do not infer medical diagnoses from someone's face.

Clearly distinguish visual observations from uncertain interpretations.

---

# 18. APP LAUNCHING — DIRECT AND INTELLIGENT

When the user says:

"Hey Jarvis, open Spotify."

JARVIS should attempt to open Spotify directly.

Do not force the generic JARVIS interface to appear first.

## Exact workflow

1. Detect wake phrase.
2. Transcribe the command.
3. Extract the application name.
4. Normalize spelling.
5. Search launchable installed apps permitted by Android.
6. Resolve the best match.
7. Ask for clarification if ambiguous.
8. Retrieve the launch intent.
9. Check whether the current activity state permits launch.
10. Launch the requested application if allowed.
11. Verify the result where observable.
12. Speak a short confirmation or explain the blocker.
13. Return to standby.

Handle Android background launch restrictions through supported system interactions.

Do not bypass them.

---

# 19. LOCAL AI BRAIN — MAXIMUM USEFUL INTELLIGENCE

Preserve and optimize the existing approximately 400 MB-class local brain.

The objective is not to pretend that a small model knows every fact.

The objective is to build a highly capable hybrid intelligence architecture.

## 19.1 AI components

Use:

- Local LLM
- Fast intent classifier
- Knowledge retrieval
- Android tools
- User-approved memory
- Optional external providers
- Conversation context manager
- Response validation
- Tool result verification

## 19.2 Model optimization

Benchmark:

- Token speed
- First-token delay
- RAM consumption
- Context length
- Multilingual performance
- Tool-calling accuracy
- Reasoning quality
- Thermal behavior

Support compatible model sizes.

Prevent unnecessary duplicate model loading.

Keep the app responsive.

## 19.3 Broad questions

JARVIS should handle a wide range of subjects, including:

- Science
- Mathematics
- Technology
- Programming
- History
- Geography
- Language learning
- General education
- Writing
- Everyday explanations
- Personal organization
- Approved device information

For questions requiring current or specialized facts, use authorized sources when available.

If information is unknown, acknowledge uncertainty.

Never invent facts to appear knowledgeable.

---

# 20. ADVANCED CONVERSATION MEMORY

Create intelligent memory layers.

## Short-term context

Maintain the current topic.

## Session context

Maintain the active conversation and tool results.

## Long-term memory

Store user-approved information.

## Task memory

Preserve relevant instructions during multi-step operations.

## Knowledge memory

Search indexed local reference material.

## User preferences

Maintain settings, aliases, and approved personalization.

Provide controls to:

- Search memories
- Save
- Edit
- Delete
- Export
- Clear

Never silently store everything permanently.

---

# 21. UNIVERSAL QUESTION AND TASK EXECUTION ENGINE

Every request must follow a consistent processing lifecycle.

## Step 1 — Receive

Capture speech or text.

## Step 2 — Normalize

Resolve transcription errors and language variations.

## Step 3 — Interpret

Identify what the user wants.

## Step 4 — Classify

Determine whether the request is:

- Factual question
- Device command
- Research
- Navigation
- Media action
- Reminder
- Conversation
- Multi-step task
- Protected operation

## Step 5 — Resolve context

Determine referenced devices, apps, dates, locations, and previous results.

## Step 6 — Select tools

Identify the necessary actual APIs and available AI components.

## Step 7 — Validate capabilities

Check:

- OS support
- Permissions
- Required services
- User authorization
- Connectivity
- Model availability

## Step 8 — Plan

Build a minimal reliable sequence.

## Step 9 — Execute

Use the appropriate validated tool.

## Step 10 — Verify

Check the result.

## Step 11 — Respond

Produce an accurate answer.

## Step 12 — Synchronize interface

Show the correct module and state where a visual surface is active or can be presented.

## Step 13 — Update context

Keep relevant recent information.

## Step 14 — Clean up

Stop unused temporary processes.

## Step 15 — Standby

Return to the appropriate listening or idle state.

Never open the wrong interface merely because a command was recognized.

---

# 22. MULTI-STEP TASKS

Support compound instructions.

Example:

"Hey Jarvis, check the weather, connect to my headphones, and set an alarm for tomorrow."

Processing:

1. Extract all three requested tasks.
2. Determine independent and dependent steps.
3. Retrieve weather.
4. Resolve the saved headphone alias.
5. Check Bluetooth availability.
6. Perform supported connection workflow.
7. Interpret the requested alarm.
8. Ask for the time if genuinely missing.
9. Execute the supported alarm operation.
10. Verify individual results.
11. Summarize completed and pending actions.

Do not report complete success if one step failed.

Allow cancellation.

---

# 23. SIX VOICE PROFILES

Preserve optional recognition for:

- One owner profile
- Five additional profiles

Enrollment should use approximately twenty seconds of initial speech, with extra recordings when necessary.

Use an appropriate speaker-identification model.

Keep templates secure.

Voice identity can influence personalization.

It must not replace Android device authentication.

Require consent from every enrolled person.

---

# 24. DAILY PROACTIVE INTELLIGENCE

Preserve optional:

- Ecuadorian news briefing
- Weather briefing
- Calendar information
- Battery reminders
- Charging notifications
- Scheduled assistant sessions
- User-created reminders

Default proposed Ecuador news schedule:

9:00 AM, America/Guayaquil.

Use real news sources.

Never invent news.

Respect Android notification scheduling restrictions.

Allow quiet hours.

---

# 25. SCREEN LOCK AND SECURITY

Support voice-initiated screen-control requests only through documented, authorized Android functionality.

For unlocking, invoke Android's own authentication mechanisms.

Never save or replay the user's actual PIN, password, or unlock pattern.

For locking, use a supported authorized mechanism when available.

If unsupported, provide an appropriate system workflow.

Preserve device security.

Do not automatically reveal sensitive information on a locked display.

---

# 26. SOUND EFFECTS — WORLD-CLASS AUDIO IDENTITY

Create original, professionally designed sounds for:

- Wake recognition
- Listening
- Processing
- Speaking
- Success
- Error
- Weather module activation
- Bluetooth discovery
- Bluetooth connection
- Wi-Fi connection
- Network speed test
- Battery charging
- Battery warning
- Alarm creation
- Music recognition
- Camera activation
- Location retrieval
- Screen transitions
- Standby

Use controlled volume mixing.

Provide separate settings for:

- Assistant voice volume
- Interface sound effects
- Activation sound
- Notification sound

Respect Android audio focus, silent mode, and Do Not Disturb.

---

# 27. ADVANCED MODULE TRANSITIONS

Transitions must feel like parts of one futuristic computer.

## From wallpaper to weather

- The central reactor shifts upward.
- An atmospheric grid expands.
- Weather data panels fade in.
- The main forecast becomes visible.

## From wallpaper to Bluetooth

- Radar arcs spread outward.
- Device markers appear based on actual results.
- A selected device becomes highlighted.

## From wallpaper to battery

- The reactor enlarges.
- Charge rings become more prominent.
- Battery diagnostics appear around it.

## From wallpaper to network diagnostics

- Digital connection lines extend.
- Speed-test graphs become visible.
- Measured values update.

## From wallpaper to location

- The interface transitions into a map-centered HUD.
- Position and accuracy indicators appear.
- Navigation controls become available.

## From wallpaper to music

- The central reactor transforms into a music waveform.
- Recognition begins.
- Verified result artwork and information appear.

Use actual state-driven animations.

Do not animate nonexistent data.

Keep transitions efficient.

---

# 28. SETTINGS — FULL SYSTEM CUSTOMIZATION

Preserve Assistant, Memory, and Settings navigation.

Add settings for:

**Local AI**

- Model selection
- Download management
- Offline/online preferences
- Inference profile

**Voice**

- Wake phrase
- Language
- Deep voice
- Speech rate
- Sound effects
- Microphone controls

**Wallpaper**

- Animation intensity
- Accent colors
- Reactor size
- Charging animation
- Idle mode
- Frame-rate profile

**Modules**

- Weather
- Location
- Battery
- Bluetooth
- Wi-Fi
- Music recognition
- Camera
- Notifications

**Personalization**

- Six voice profiles
- Device aliases
- App aliases
- Ten saved websites
- Memory settings

**Security**

- Android authentication
- Lock-screen information
- Permissions
- Protected actions
- Data management

Do not display user-facing RAM meters.

Keep AI memory functional.

---

# 29. DEVICE PERFORMANCE

Optimize for the Redmi 9.

Use:

- Efficient wallpaper rendering
- Jetpack Compose state management
- Kotlin coroutines
- StateFlow
- Native drawing
- Cached geometry
- Limited particle counts
- Safe audio buffering
- Lightweight voice detection
- Efficient model loading
- Thermal-aware inference
- Proper observer cleanup

Create performance profiles:

`CINEMATIC`

`BALANCED`

`ECO`

Suspend unnecessary rendering while invisible.

Do not run expensive processing continuously.

---

# 30. RELIABILITY AND ERROR RECOVERY

Handle:

- Offline device
- Missing permissions
- Wi-Fi disconnected
- Bluetooth disabled
- Accessory unavailable
- Missing battery information
- Unavailable location fix
- Expired weather cache
- Failed music recognition
- Missing model
- Insufficient storage
- Microphone conflict
- Camera conflict
- Background launch restriction
- Cancelled task
- Tool timeout

Every failure must have a meaningful user-facing state.

Do not fabricate success.

Never crash simply because a capability is unavailable.

---

# 31. SOFTWARE ARCHITECTURE

Create or extend a modular design.

Suggested components:

`JarvisCommandCoordinator`

`JarvisIntentRouter`

`JarvisEntityResolver`

`JarvisTaskPlanner`

`JarvisToolRegistry`

`JarvisResultVerifier`

`JarvisConversationContext`

`JarvisMemoryRepository`

`JarvisLocalBrainManager`

`JarvisSpeechCoordinator`

`JarvisWallpaperEngine`

`JarvisHudStateController`

`JarvisModuleNavigator`

`JarvisAudioEffectsEngine`

`WeatherIntelligenceModule`

`LocationIntelligenceModule`

`BatteryIntelligenceModule`

`BluetoothIntelligenceModule`

`WifiIntelligenceModule`

`NetworkDiagnosticsModule`

`MusicRecognitionModule`

`CameraVisionModule`

`AlarmAndTimerModule`

`SystemTelemetryModule`

Reuse existing code whenever possible.

Do not duplicate the same data repository for multiple visual screens.

Use a single source of truth for each real system metric.

---

# 32. STEP-BY-STEP DEVELOPMENT PLAN

Follow this order carefully.

## PHASE 1 — COMPLETE REPOSITORY AUDIT

1. Read the current source tree.
2. Identify working features.
3. Identify incomplete features.
4. Inspect existing wallpaper code.
5. Inspect the local AI engine.
6. Inspect voice recognition.
7. Inspect device tools.
8. Inspect settings and memory storage.
9. Inspect current build settings.
10. Document integration constraints.

## PHASE 2 — CORE INTELLIGENCE

1. Benchmark the current model.
2. Improve inference performance.
3. Implement or refine command routing.
4. Add entity extraction.
5. Add conversational context resolution.
6. Add structured tool requests.
7. Implement result verification.
8. Test basic and compound commands.

## PHASE 3 — WALLPAPER ENGINE

1. Verify the current native wallpaper.
2. Implement missing reactor states.
3. Connect actual battery information.
4. Add speaking/listening animation.
5. Add charging effects.
6. Implement visibility-based rendering control.
7. Verify wallpaper lifecycle behavior.

## PHASE 4 — MODULAR HUD SYSTEM

1. Build a common visual component library.
2. Implement module navigation.
3. Create a shared reactor component.
4. Create shared captions.
5. Create consistent header and controls.
6. Add module-specific animations.
7. Handle screen sizes and orientation.

## PHASE 5 — WEATHER MODULE

1. Integrate real weather data.
2. Implement cache.
3. Add forecast parsing.
4. Create animated weather HUD.
5. Connect voice commands.
6. Test offline and stale-data behavior.

## PHASE 6 — LOCATION MODULE

1. Integrate supported location APIs.
2. Add permissions.
3. Implement accuracy checks.
4. Add reverse geocoding.
5. Integrate map display.
6. Connect spoken commands.

## PHASE 7 — BATTERY MODULE

1. Observe battery status.
2. Read supported health properties.
3. Implement diagnostics.
4. Create charging animations.
5. Add battery HUD.
6. Connect spoken queries.

## PHASE 8 — CONNECTION MODULES

1. Implement Wi-Fi state observation.
2. Implement Bluetooth discovery.
3. Add app/device-name correction.
4. Add connection-status verification.
5. Add accessory battery reporting.
6. Implement supported system panels.
7. Create connection HUDs.

## PHASE 9 — NETWORK DIAGNOSTICS

1. Add real speed testing.
2. Measure latency.
3. Implement result validation.
4. Create performance charts.
5. Add troubleshooting reasoning.
6. Test failures and cancellation.

## PHASE 10 — MUSIC AND CAMERA

1. Configure real music recognition.
2. Validate song metadata.
3. Connect authorized Spotify actions.
4. Implement CameraX front preview.
5. Add user-controlled image analysis.
6. Create dedicated HUDs.

## PHASE 11 — PRODUCTIVITY

1. Implement natural-language time parsing.
2. Integrate clock intents.
3. Support timers and alarms.
4. Add reminders.
5. Add saved website commands.
6. Add scheduled daily briefings.

## PHASE 12 — PERSONALIZATION

1. Preserve memory storage.
2. Implement voice-profile management.
3. Improve aliases.
4. Add user preferences.
5. Verify data isolation.
6. Test deletion and export.

## PHASE 13 — AUDIO AND ANIMATIONS

1. Integrate sound effect resources.
2. Implement audio focus handling.
3. Add playback amplitude visuals where supported.
4. Add smooth captions.
5. Refine state transitions.
6. Optimize rendering.

## PHASE 14 — SECURITY

1. Review permissions.
2. Review protected actions.
3. Verify system authentication.
4. Test lock-screen-safe presentation.
5. Test background restrictions.
6. Review personal data storage.

## PHASE 15 — COMPREHENSIVE TESTING

1. Execute unit tests.
2. Test voice intents.
3. Test real device readings.
4. Test module navigation.
5. Test task failure recovery.
6. Test animation performance.
7. Test local AI quality.
8. Test multilingual behavior.
9. Test user data migration.
10. Run available Android instrumentation tests.

## PHASE 16 — FINAL DELIVERY

1. Compile the project.
2. Resolve build errors.
3. Run regression tests.
4. Generate a debug APK where possible.
5. Report all changed files.
6. Report implemented capabilities.
7. Report unsupported features.
8. Report test results.
9. Report hardware limitations.
10. Provide the verified build artifact location.

---

# 33. EXAMPLE — WEATHER REQUEST

User says:

"Hey Jarvis, tell me tomorrow's weather."

JARVIS:

1. Detects the wake phrase.
2. Captures the command.
3. Recognizes the weather intent.
4. Extracts "tomorrow."
5. Resolves the relevant city.
6. Retrieves actual forecast data.
7. Validates the source timestamp.
8. Opens the weather HUD where Android permits.
9. Animates the appropriate weather conditions.
10. Displays the forecast.
11. Speaks a concise explanation.
12. Animates the reactor cyan while speaking.
13. Scrolls the real spoken response beneath the reactor.
14. Preserves the weather context.
15. Returns to standby when appropriate.

If Android prevents immediate visual presentation, JARVIS uses an approved fallback without pretending the screen opened.

---

# 34. EXAMPLE — BLUETOOTH REQUEST

User says:

"Hey Jarvis, show my Bluetooth devices."

JARVIS:

1. Checks Bluetooth access.
2. Retrieves permitted device information.
3. Opens the Bluetooth HUD when allowed.
4. Displays actual devices.
5. Announces relevant results.
6. Waits for follow-up commands.

User says:

"Connect to Pro 7."

JARVIS:

1. Resolves the device name.
2. Checks the device's status.
3. Starts the supported connection workflow.
4. Handles required confirmation.
5. Verifies the connection.
6. Reports the outcome.

User says:

"What's its battery?"

JARVIS:

1. Resolves the selected Pro 7 device.
2. Retrieves supported battery data.
3. Displays the actual result.
4. Speaks a short response.
5. Avoids inventing unavailable battery information.

---

# 35. EXAMPLE — BATTERY REQUEST

User says:

"Hey Jarvis, show my battery health."

JARVIS:

1. Reads actual battery properties.
2. Checks available health data.
3. Opens the battery diagnostics HUD.
4. Displays real measurements.
5. Explains the health classification.
6. Distinguishes available measurements from estimates.
7. Activates charging visuals if applicable.
8. Speaks the findings.
9. Updates data as Android reports changes.

Never fabricate health percentages.

---

# 36. EXAMPLE — DIRECT APP LAUNCH

User says:

"Hey Jarvis, open YouTube."

JARVIS:

1. Detects the wake phrase.
2. Recognizes the application intent.
3. Extracts YouTube.
4. Matches the actual installed app.
5. Checks Android launch restrictions.
6. Opens YouTube directly where permitted.
7. Reports the result.
8. Does not unnecessarily open the JARVIS HUD.

---

# 37. FINAL ACCEPTANCE REQUIREMENTS

Do not claim completion unless the following requirements are implemented and verified or explicitly reported as blocked.

1. Existing application preserved.
2. Local AI remains functional.
3. Persistent memory preserved.
4. Direct app launching supported.
5. Wake-word recognition retained.
6. Genuine live wallpaper operating.
7. Wallpaper reactor continuously animated only when appropriate.
8. Purple listening visuals functional.
9. Cyan speaking visuals functional.
10. Green charging visuals functional.
11. Actual battery percentage displayed.
12. Weather beside clock.
13. Dedicated weather interface.
14. Dedicated location interface.
15. Dedicated battery interface.
16. Dedicated Wi-Fi interface.
17. Dedicated Bluetooth interface.
18. Dedicated network diagnostics interface.
19. Dedicated music recognition interface.
20. Dedicated camera interface.
21. Dedicated clock/alarm interface.
22. Dedicated system telemetry interface.
23. Actual information sources used.
24. Real action results verified.
25. Smooth live captions.
26. English, Spanish, and Persian supported.
27. Six optional voice profiles supported.
28. Daily briefings available.
29. Saved websites available.
30. Device permissions handled properly.
31. Protected operations respect Android authentication.
32. Background activity restrictions respected.
33. Performance optimized for the Redmi 9.
34. Tests executed and documented.
35. Project builds successfully in the configured environment.

---

# 38. FINAL PRODUCT EXPERIENCE

I want JARVIS to feel like a sophisticated artificial intelligence integrated into the phone.

When I normally use my Android home screen, I see the live JARVIS wallpaper behind my applications.

The central holographic reactor moves slowly.

My actual battery percentage is displayed.

When I say:

"Hey Jarvis, tell me the weather."

The assistant recognizes my voice.

Where Android permits, the screen transitions into an advanced weather interface.

The weather animations reflect real conditions.

The clock shows actual time.

The weather information is retrieved from a real provider.

JARVIS answers naturally.

Its reactor glows cyan while speaking.

Its words appear in a small scrolling caption area.

When I ask:

"Where am I?"

JARVIS uses an authorized location source and presents the location interface.

When I ask:

"Show my Bluetooth devices."

A holographic Bluetooth radar appears with actual device information.

When I ask:

"What's my battery health?"

The reactor becomes the centerpiece of a detailed battery diagnostics interface.

When I ask:

"Identify this song."

The music recognition interface activates and uses a real provider.

When I ask:

"Open Spotify."

Spotify opens directly.

When I ask difficult questions, the local AI uses its language model, knowledge sources, context memory, and approved tools to generate helpful answers.

When I ask multiple connected questions, JARVIS understands the conversation.

When I ask it to perform tasks, it follows a structured execution process and verifies the results.

JARVIS operates through supported background services and Android-approved mechanisms, with clear user controls for listening and notifications.

The result must be elegant, responsive, efficient, intelligent, secure, and genuinely functional.

**DO NOT BUILD A STATIC DEMONSTRATION.**

**DO NOT REPLACE REAL SYSTEM DATA WITH PLACEHOLDERS.**

**DO NOT HARD-CODE A SMALL SET OF QUESTIONS AND CALL IT INTELLIGENCE.**

**BUILD A REAL, EXTENSIBLE, LOCAL-FIRST ANDROID AI ASSISTANT WITH DEDICATED FUTURISTIC INTERFACES FOR EVERY MAJOR CAPABILITY.**

Inspect the existing source.

Implement the actual code.

Verify integrations.

Test performance.

Fix errors.

Prepare the working build.

**J.A.R.V.I.S. 10.0 — PROJECT OMNISYSTEM**

**BEGIN THE COMPLETE APPLICATION UPGRADE NOW.**