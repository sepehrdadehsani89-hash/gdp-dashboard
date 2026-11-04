# J.A.R.V.I.S. 10.7 — PROJECT TITAN
## THE ULTIMATE ANDROID PERSONAL AI ASSISTANT
### NEXT-GENERATION VOICE INTELLIGENCE, INSTANT EXECUTION, AUTONOMOUS TOOL ORCHESTRATION, ADVANCED MEMORY, LIVE WALLPAPER AND REAL-TIME SYSTEM CONTROL

**MASTER IMPLEMENTATION SPECIFICATION FOR OPENAI CODEX**

**Project:** Existing JARVIS Android AI Assistant

**Target release:** JARVIS 10.7 — PROJECT TITAN

**Previous specifications to consolidate:** JARVIS 5.0, 6.0, 7.0, 8.0, 8.1, 10.0, 10.1, Neural Speed Boost, 10.2, 10.3, 10.4 and 10.5.

**Primary target hardware:** Xiaomi Redmi 9

**Operating system:** Android, with version-aware implementations

**Application framework:** Native Kotlin and Jetpack Compose

**Primary architecture:** Local-first AI with authorized Android integrations

**Product objective:** Build an exceptionally capable, responsive, intelligent, memorable and visually extraordinary Android personal assistant.

**Important:** This is a comprehensive upgrade to an EXISTING APPLICATION. Preserve its working code, database, interfaces, permissions, models, integrations and user settings.

---

# PART I — PROJECT DEFINITION

## 1. THE TITAN MISSION

You are operating as a coordinated team of world-class engineers specializing in:

- Android development
- Kotlin
- Jetpack Compose
- Android OS services
- Native Android security
- Local language models
- AI inference optimization
- Speech recognition
- Wake-word recognition
- Speaker identification
- Noise suppression
- Conversational AI
- Intelligent memory
- AI tool orchestration
- Bluetooth
- Wi-Fi
- Android telecommunications
- Location services
- Navigation
- Computer vision
- Audio engineering
- Real-time graphics
- GPU optimization
- Live wallpapers
- Product design
- Software testing
- Android release engineering

Your mission is to create the most capable JARVIS application that the actual device hardware and Android platform can reliably support.

JARVIS must be more than a chatbot.

It must be an integrated personal intelligence system.

It must understand what the user wants.

It must remember relevant context.

It must determine the correct tools.

It must execute real supported operations.

It must verify the results internally.

It must speak naturally.

It must adapt to the user's chosen language.

It must support interruptions and follow-up instructions.

It must provide beautiful animated interfaces.

It must remain fast, stable, privacy-conscious and efficient.

The user should not need to memorize dozens of exact command phrases.

JARVIS must interpret natural language.

**Every capability must be implemented as a working component connected to one central intelligence architecture.**

---

## 2. EXISTING PROJECT REFERENCES

### GitHub repository

https://github.com/sepehrdadehsani89-hash/gdp-dashboard

### Previous APK reference

https://raw.githubusercontent.com/sepehrdadehsani89-hash/gdp-dashboard/ed1fdf2f2a53a480565d288d05cd644657bce1e1/downloads/Jarvis-0.1.1-arm64.apk

Inspect the latest available repository code.

Do not treat the older APK as the current source of truth.

### Superdesign animation project

https://superdesign.dev/teams/79a56f66-5f09-4ac5-9a2f-80274d1eb294/projects/2c7f6f27-92df-4c24-9b84-ea7d0e3b1629

**Team ID:**

`79a56f66-5f09-4ac5-9a2f-80274d1eb294`

**Project ID:**

`2c7f6f27-92df-4c24-9b84-ea7d0e3b1629`

Retrieve actual Superdesign assets and motion references when authorized access is available.

If they cannot be accessed, report the missing access rather than inventing their exact appearance.

### English JARVIS voice reference

`05b36da8574341d0803391491850db20`

Preserve this existing configured JARVIS voice identifier, subject to actual provider compatibility.

### Exclusive Spanish voice

`ae74d1059517440aa6e5d2d50598d179`

**Use this exact identifier as the ONLY Spanish text-to-speech voice.**

Never silently substitute a different Spanish voice.

If the specified voice is unavailable, report the configuration error.

Verify which provider owns each voice identifier before implementation.

Do not expose API credentials.

---

## 3. MANDATORY PRESERVATION POLICY

Before editing the application:

1. Inspect the source tree.
2. Identify the current Git branch.
3. Check the working tree for unfinished changes.
4. Inspect the Android manifest.
5. Inspect application services.
6. Inspect the wallpaper implementation.
7. Inspect the local AI model.
8. Inspect speech recognition.
9. Inspect TTS integrations.
10. Inspect memory storage.
11. Inspect tool execution.
12. Inspect Bluetooth and Wi-Fi services.
13. Inspect contacts and calling.
14. Inspect weather and location.
15. Inspect screenshot sharing.
16. Inspect existing animations.
17. Inspect settings and onboarding.
18. Inspect database migrations.
19. Inspect tests.
20. Establish a baseline build result.

Preserve:

- Application identity
- Saved memories
- Existing conversations
- Downloaded AI models
- API configurations
- Contact shortcuts
- Application aliases
- Website shortcuts
- Six voice profiles
- Saved locations
- User settings
- Authorized integrations

Do not erase data during migrations.

Do not rewrite working features merely to simplify implementation.

**The new architecture must integrate existing components, not duplicate them.**

---

# PART II — THE MOST ADVANCED VOICE SYSTEM

## 4. PRIMARY WAKE WORD

The main calling word is:

**"Hey Jarvis."**

The Spanish calling word is:

**"Oye Jarvis."**

Remove active support for previously discontinued languages.

This release supports English and Spanish.

The wake-word detector must operate through an explicitly enabled, Android-compliant listening mechanism.

It must not secretly record background conversations.

The user must be able to disable wake listening.

The application must respect Android microphone permissions, foreground-service requirements and power-management restrictions.

---

## 5. ULTRA-POWERFUL MICROPHONE ENGINE

This is a top-priority system.

I want JARVIS to recognize the user's voice reliably in realistic environments.

Design a layered audio pipeline:

**Microphone Input**

→ **Audio Preprocessing**

→ **Noise Reduction**

→ **Voice Activity Detection**

→ **Wake-Word Recognition**

→ **Streaming Speech Recognition**

→ **Speaker Profile Matching**

→ **Language Detection**

→ **Intent Recognition**

→ **Action or Conversation**

Do not rely on one fragile recognizer for every task.

### 5.1 Audio preprocessing

Evaluate available Android support for:

- Acoustic echo cancellation
- Noise suppression
- Automatic gain control
- Audio normalization
- Microphone routing
- Noise-floor measurement
- Signal quality estimation
- Audio-buffer synchronization
- Streaming voice activity detection

Use processing that improves recognition without unnecessarily distorting speech.

### 5.2 Noisy environments

Test recognition with:

- Television noise
- Music playback
- Several nearby speakers
- Fans
- Outdoor traffic
- Quiet speech
- Normal speaking volume
- Headset microphones
- Different distances from the device

Measure false rejections and false activations.

Do not claim guaranteed recognition in every environment.

### 5.3 Multiple speakers

JARVIS should recognize the calling word when it is acoustically distinguishable.

Use appropriate speaker separation or confidence estimation when technically available.

Do not combine unrelated speakers into one command.

Do not treat another person's speech as the owner's identity without sufficient evidence.

### 5.4 Microphone sensitivity

Add:

`VOICE SENSITIVITY`

Options:

- LOW
- BALANCED
- HIGH
- CUSTOM

Custom controls may include measured wake threshold, confidence threshold and noise sensitivity.

High sensitivity should improve recognition of quieter speech without allowing arbitrary sounds to activate JARVIS.

### 5.5 Audio diagnostics

Provide a voice calibration interface that measures actual microphone input quality.

Include:

- Input level
- Background noise estimate
- Wake-word detection tests
- Recognition confidence
- Supported audio processing
- Current microphone source

Use real measurements.

---

## 6. REMOVE THE FIXED TEN-SECOND LISTENING WINDOW

**REMOVE THE PREVIOUS RIGID TEN-SECOND CONVERSATION WINDOW.**

JARVIS must not stop listening merely because ten seconds have elapsed.

Replace it with a state-aware voice activity system.

### Required behavior

When the user is actively speaking:

Keep recognizing speech within the limits of the chosen speech engine.

When the user briefly pauses:

Do not immediately assume the command is finished.

When the user finishes:

Use voice activity, language structure and command completeness to determine the end of the utterance.

When the user gives a direct command:

Execute promptly after the required intent and parameters are confidently recognized.

### Technical requirements

Implement:

- Adaptive end-of-utterance detection
- Voice activity tracking
- Partial transcript continuity
- Speech-segment stitching when necessary
- Safe recognizer restart behavior
- End-of-turn confidence
- Cancellation
- Resource timeouts for abandoned sessions

Do not implement an infinite microphone recording session with no cancellation or resource control.

Use sensible inactivity and safety limits without reintroducing a rigid ten-second cutoff.

---

## 7. INSTANT WAKE RESPONSE AND GREETING

When JARVIS hears:

**"Hey Jarvis."**

it should immediately acknowledge activation.

Use the speaker profile and time of day to select the appropriate greeting.

### Important owner profile

Owner's configured name:

**Sepehr**

Correct spelling:

`S E P E H R`

However, the owner prefers that JARVIS address him as:

**"sir"**

Do not routinely use his first name in owner greetings.

### Owner-specific greetings

Use the actual local time.

Suggested behavior:

**05:00–11:59**

"Good morning, sir. How can I help you?"

**12:00–17:59**

"Good afternoon, sir. How can I help you?"

**18:00–21:59**

"Good evening, sir. How can I help you?"

**22:00–04:59**

"Good night, sir. How can I help you?"

Make the time boundaries configurable.

### If the owner is not confidently recognized

Use a neutral greeting:

"Hello. How can I help you?"

Do not claim to have recognized the owner when speaker confidence is insufficient.

### Other speaker profiles

Support personalized greetings for the five additional consenting profiles.

Use their approved names or greeting preferences.

Do not call every user "sir" unless their profile specifies it.

### Critical speed rule

If the user says only:

"Hey Jarvis."

respond with the greeting promptly.

If the user says:

"Hey Jarvis, open Spotify."

do not delay opening Spotify just to recite a greeting.

The microphone should capture the entire command.

Use a brief nonblocking acknowledgement when useful.

Prevent JARVIS's own greeting from interfering with speech recognition.

**Voice responsiveness takes priority over ceremonial greetings.**

---

## 8. SIX-PROFILE VOICE IDENTIFICATION

Preserve:

**ONE OWNER + FIVE ADDITIONAL PROFILES**

Each profile may contain:

- Profile name
- Preferred spoken form of address
- Preferred language
- Optional voice enrollment
- Approved memories
- Authorized app aliases
- Personal preferences
- Greeting preferences

### Enrollment

Allow an initial recording of approximately twenty seconds.

Request additional samples where necessary.

Use suitable speaker-embedding technology.

Securely store voice templates.

### Recognition

The system should distinguish:

- Owner confidently recognized
- Known guest confidently recognized
- Unknown speaker
- Uncertain speaker

If the match is uncertain, use neutral personalization.

Never treat voice identity as equivalent to Android's secure authentication.

### Profile privacy

Protect private memories.

Do not expose one user's private information to another speaker simply because the voices sound similar.

Allow users to remove their voice profiles.

---

## 9. NATURAL AND REALISTIC SPEECH

JARVIS should speak fluently and naturally.

Avoid robotic word-by-word speech.

Implement punctuation-aware speech generation.

### Text formatting

Support:

- Commas
- Periods
- Question marks
- Exclamation marks where appropriate
- Sentence boundaries
- Correct capitalization
- Natural dates
- Accurate numbers
- Application names
- Contact names

### Spanish formatting

Use natural Spanish punctuation, including opening question and exclamation marks when appropriate.

### Spoken delivery

Use:

- Natural sentence rhythm
- Appropriate pauses
- Clear pronunciation
- Question intonation where supported
- Streaming speech
- Smooth sentence transitions

Do not force unnecessary pauses after every comma.

Do not use excessive dramatic effects in normal conversation.

### Exclusive Spanish output

Whenever the response language is Spanish:

Use only:

`ae74d1059517440aa6e5d2d50598d179`

If that voice fails, provide readable text and a clear error rather than selecting another Spanish voice without permission.

### English output

Preserve the existing English voice configuration.

---

## 10. HUMANLIKE CONVERSATION FLOW

JARVIS should respond naturally without sounding repetitive.

Avoid beginning every answer with:

"Certainly."

"Of course."

"I'd be happy to help."

Adapt response length to the question.

### Short command

User:

"Turn on the flashlight."

Response:

"Flashlight on."

Only after the supported operation succeeds.

### Informational question

User:

"Why is my Wi-Fi slow?"

JARVIS gathers available evidence and gives a useful explanation.

### Complex question

User:

"Explain how machine learning works."

JARVIS provides a structured answer.

### Follow-up

User:

"Explain it more simply."

JARVIS retains the subject.

### Correction

User:

"No, I meant Bluetooth."

JARVIS updates the active context rather than restarting from scratch.

---

## 11. IMMEDIATE INTERRUPTION

Preserve:

**"Wait, wait."**

Also support:

- Wait.
- Hold on.
- Stop talking.
- One second.
- Pause.

When JARVIS is speaking:

1. Detect a genuine user interruption.
2. Suppress interference from the assistant's own audio.
3. Stop or pause TTS promptly.
4. Preserve the unfinished conversation context.
5. Change the reactor from cyan to purple.
6. Begin listening to the next instruction.
7. Process the correction.
8. Continue naturally.

Do not erase the active subject.

Do not restart the entire AI session.

---

# PART III — THE TITAN AI BRAIN

## 12. MAXIMUM LOCAL INTELLIGENCE

Preserve the existing approximately 400 MB-class downloadable local AI model.

Benchmark it against compatible alternatives.

The goal is maximum useful intelligence within the actual hardware limits.

Measure:

- Time to first token
- Tokens per second
- Model loading time
- Peak memory usage
- Context retention
- Tool selection accuracy
- English quality
- Spanish quality
- Reasoning accuracy
- Battery impact
- Thermal behavior

### Optimization

Investigate:

- ARM64 native inference
- Compatible quantization
- KV-cache optimization
- Prompt caching
- Context summarization
- Streaming token generation
- Efficient thread scheduling
- Model warm reuse
- Memory-safe model unloading
- Cancellable inference

Do not increase model size beyond the device's reliable capacity.

Do not remove the actual AI memory system.

---

## 13. THREE INTELLIGENCE EXECUTION PATHS

### PATH A — INSTANT NATIVE EXECUTION

Use for straightforward actions:

- Open an application
- Call a saved contact
- Read battery percentage
- Activate flashlight
- Open supported settings
- Launch Google Maps
- Start a timer

Do not invoke expensive generative reasoning unnecessarily.

### PATH B — LOCAL AI REASONING

Use for:

- Conversation
- Explanation
- Summarization
- Writing
- Mathematics
- Study questions
- Language understanding
- Personal organization
- Contextual follow-ups

### PATH C — MULTI-STEP INTELLIGENCE

Use for:

- Compound commands
- System diagnostics
- Research
- Troubleshooting
- Planning
- Tasks requiring several tools

Do not add a separate special activation mode.

Complex reasoning should be available naturally when needed.

---

## 14. UNIVERSAL QUESTION HANDLING

JARVIS should be prepared for a very broad range of questions.

Subjects include:

- Science
- Mathematics
- Technology
- Programming
- History
- Geography
- Writing
- Language learning
- Education
- General research
- Personal productivity
- Android assistance
- Device troubleshooting
- Current information
- User-approved personal information

It cannot literally know every fact.

Use the appropriate information source.

### Source hierarchy

For live phone status:

Use Android APIs.

For user-approved memories:

Use the memory database.

For local documents:

Use authorized document retrieval.

For general offline questions:

Use the local AI.

For current information:

Use an authorized live source.

For exact calculations:

Use deterministic computation.

For unknown information:

State uncertainty clearly.

**Never invent measurements, news, contact data, or operation results to make JARVIS appear intelligent.**

---

## 15. ADVANCED LONG-TERM MEMORY

Create a layered memory architecture.

### Short-term memory

Active utterance and immediate conversation.

### Session memory

Current topic, relevant pronouns, recent tools and results.

### Task memory

Requested objectives, completed actions, pending steps and blockers.

### Long-term memory

User-approved information retained between sessions.

### Semantic memory

Indexed knowledge that can be retrieved by meaning.

### Preferences

Language, greeting, voice, aliases, display settings and response style.

### Memory execution

Before a response:

1. Determine whether memory is relevant.
2. Identify the appropriate memory category.
3. Retrieve relevant records.
4. Check access permissions.
5. Exclude unrelated information.
6. Combine the information with the current request.
7. Generate the response.
8. Preserve temporary context.
9. Save new permanent information only when authorized.

Do not send an entire lifetime of conversation history with every request.

Use compact summaries and relevant retrieval.

### Memory transparency

Allow the user to:

- View
- Search
- Edit
- Pin
- Delete
- Export
- Clear

Never treat a model guess as a confirmed personal fact.

---

# PART IV — BATTERY INTELLIGENCE 10.7

## 16. EXACT BATTERY COLOR SYSTEM

**THIS FEATURE MUST BE IMPLEMENTED PRECISELY.**

The real battery percentage appears in the center of the Sentinel reactor.

The number must reflect actual Android battery data.

Use these exact, nonoverlapping thresholds:

| Actual battery level | Visual state | Color |
|---|---|---|
| 70–100% | NORMAL | White |
| 31–69% | MODERATE | Yellow |
| 11–30% | LOW | Orange |
| 6–10% | VERY LOW | Red |
| 1–5% | CRITICAL | Intense dark red |
| 0% if reported | CRITICAL | Intense dark red |

### Exact boundary rules

At 70%:

WHITE.

At 69%:

YELLOW.

At 31%:

YELLOW.

At 30%:

ORANGE.

At 11%:

ORANGE.

At 10%:

RED.

At 6%:

RED.

At 5%:

CRITICAL RED.

At 1%:

CRITICAL RED.

Never allow overlapping conditions to produce unpredictable colors.

### Suggested colors

White:

`#F8FAFC`

Yellow:

`#FACC15`

Orange:

`#FB923C`

Red:

`#EF4444`

Critical red:

`#991B1B`

Critical accent:

`#FF1744`

Ensure sufficient readability against the dark HUD.

---

## 17. BATTERY COLOR ANIMATIONS

### 70–100%

White battery percentage.

Subtle neutral progress arc.

Normal Sentinel animation.

### 31–69%

Yellow percentage.

Soft yellow progress highlights.

No unnecessary warning sound.

### 11–30%

Orange percentage.

Warm amber-orange edge illumination.

A restrained low-battery visual state.

### 6–10%

Red percentage.

Slow red pulses around the battery indicator.

No rapid flashing.

### 1–5%

Intense critical-red percentage.

A stronger but controlled outer warning glow.

A slower critical pulse.

Avoid strobing or aggressive full-screen flashing.

### Charging state

If the device is charging, retain the separate rotating green charging ring.

The battery percentage itself must still use the appropriate level color.

If JARVIS is speaking while charging:

Cyan speaking reactor.

Green charging ring.

Battery percentage colored according to charge level.

These are separate visual layers.

---

## 18. PRECISE LOW-BATTERY VOICE ALERTS

This is a mandatory new feature.

When the battery is discharging, JARVIS must issue a spoken reminder every time the actual battery percentage decreases through the specified low-battery levels.

### Normal urgent warnings

At 10%:

"Please charge your phone. Battery percentage is at ten percent."

At 9%:

"Please charge your phone. Battery percentage is at nine percent."

At 8%:

"Please charge your phone. Battery percentage is at eight percent."

At 7%:

"Please charge your phone. Battery percentage is at seven percent."

At 6%:

"Please charge your phone. Battery percentage is at six percent."

### Critical warnings

At 5%:

"Please charge your phone immediately. Critical battery percentage is at five percent."

At 4%:

"Please charge your phone immediately. Critical battery percentage is at four percent."

At 3%:

"Please charge your phone immediately. Critical battery percentage is at three percent."

At 2%:

"Please charge your phone immediately. Critical battery percentage is at two percent."

At 1%:

"Please charge your phone immediately. Critical battery percentage is at one percent."

Use the configured JARVIS voice and appropriate language selection.

If the preferred language is Spanish, synthesize a natural Spanish equivalent with the exclusive Spanish voice.

---

## 19. LOW-BATTERY ALERT TRIGGER RULES

The alert system must be event-driven.

Do not use a repeating timer to announce the same percentage again and again.

Observe actual battery updates through Android's supported battery APIs and broadcasts.

### Algorithm

Maintain:

- Current observed percentage
- Previous observed percentage
- Current charging state
- Last announced percentage
- Current discharge episode
- Alert preferences
- Quiet-hours preferences

When the battery decreases:

1. Read the new actual percentage.
2. Compare it with the last observed percentage.
3. Check whether the phone is discharging.
4. Determine whether the new level is between 1% and 10%.
5. Check whether this level was already announced in the current discharge episode.
6. If not, schedule its spoken alert promptly.
7. Mark the level as announced only when appropriate.
8. Update the reactor color.
9. Preserve the current AI conversation state.
10. Avoid duplicating the alert after service restart.

### Important cases

**10% → 9%**

Announce 9%.

**9% → 9%**

Do not announce again.

**9% → 8%**

Announce 8%.

**6% → 5%**

Switch to critical-red styling and announce the critical 5% warning.

**5% → 6% while charging**

Do not announce a new low-battery warning.

**11% → 8% because Android skipped intermediate reports**

Announce the currently observed 8% level.

Do not rapidly play missed 10% and 9% warnings afterward.

### Reliability

Persist enough state to prevent duplicate announcements caused by service restarts.

Allow a new discharge episode after a genuine charging or recovery cycle.

Avoid duplicate queued audio.

If the device shuts down at a critically low percentage, do not claim that the alert is guaranteed to play.

---

## 20. BATTERY ALERT PRIORITY AND SAFETY

Low-battery speech must not interfere with emergency calls or protected system operations.

Integrate with the audio-session manager.

Respect:

- Audio focus
- Active phone calls
- Silent mode where applicable
- Do Not Disturb
- User-configured quiet hours
- User-selected notification preferences

If JARVIS is already speaking, queue or briefly interrupt according to user settings.

Do not mix the charging reminder into another sentence.

Add:

`BATTERY VOICE ALERTS`

Setting:

`ON / OFF`

Add:

`CRITICAL ALERTS`

Setting:

`ON / OFF`

Add configurable audio volume.

Preserve the requested default alerts while allowing commercial users to adjust or disable repeated speech.

Avoid excessive wakeups.

---

## 21. COMPLETE BATTERY DIAGNOSTICS HUD

Preserve a dedicated battery intelligence interface.

Show real supported measurements:

- Battery percentage
- Charging state
- Charging source
- Battery health classification
- Temperature
- Voltage
- Available charging current
- Battery saver state
- Estimated remaining charging time when reliable
- Last update timestamp

Distinguish between Android's health classification and a capacity-health percentage.

Do not fabricate battery capacity health when the device does not expose sufficient data.

The visual interface must update when real measurements change.

---

# PART V — LOCATION AND GOOGLE MAPS INTELLIGENCE

## 22. SMART LOCATION CONVERSATIONS

Implement an advanced natural-language location assistant.

Recognize:

"Where am I?"

"What's my current location?"

"Tell me my address."

"Take me home."

"Navigate home."

"Set my home location."

"Save this place as home."

"Open Google Maps."

"Start navigation."

"How do I get home?"

"Show me the route."

"Where is my saved school?"

Use intent and context rather than literal keywords alone.

---

## 23. LOCATION-OFF CONVERSATION

When a location-dependent action is requested:

1. Determine whether location services are enabled.
2. Check the application's location permission.
3. Determine whether the request requires approximate or precise location.
4. Check whether a recent suitable location fix exists.
5. If location is disabled, explain the blocker.
6. Offer the supported location-enabling workflow.
7. Wait for user approval and actual system changes.
8. Resume the original request automatically after the required state becomes available.

### Required example

User:

"Hey Jarvis, where am I?"

If location is off:

JARVIS:

"I can't find your location, sir, because location services are turned off. Would you like to turn them on?"

User:

"Yes."

JARVIS:

"Opening location controls, sir."

Use Android's supported settings-resolution flow.

If the user enables location, verify the actual location state.

Only after Android confirms location is enabled may JARVIS say:

"Location is on, sir. Where would you like to go?"

### If the user says no

JARVIS:

"Understood, sir."

Cancel the pending location action.

Do not continue requesting permission.

### Important technical requirement

Do not claim that an ordinary Android app can silently toggle global location services.

Use an approved settings dialog or system settings interface.

Resume the task after the user completes the system action.

---

## 24. SAVE HOME LOCATION

Create a dedicated destination repository.

Support named locations:

- Home
- School
- Work
- Favorite places
- Custom labels

### Commands

"Save this location as home."

"Set home to this address."

"Remember this place as home."

"Change my home location."

"Show my saved home."

"Delete my home location."

### Required behavior

When saving Home:

1. Identify that the user wants to create or update a saved destination.
2. Determine whether coordinates or an address were provided.
3. If the user means their current location, obtain an authorized location fix.
4. Check accuracy.
5. Resolve a suitable address or place identifier when possible.
6. Show or speak the proposed destination.
7. Save the destination through the user-approved workflow.
8. Persist the destination securely.
9. Mark it as `HOME`.
10. Confirm the saved destination.

Do not silently overwrite an existing Home destination if the new location is ambiguous.

Do not assume the current location is home unless the user explicitly says to save it.

---

## 25. GOOGLE MAPS NAVIGATION TO HOME

This is a priority feature.

When the user says:

**"Hey Jarvis, take me home."**

JARVIS must resolve the saved Home location and launch Google Maps navigation using the supported mechanism.

### Exact execution

1. Detect wake phrase.
2. Recognize the navigation intent.
3. Resolve `HOME`.
4. Retrieve the saved location.
5. Check whether the destination is valid.
6. Determine whether Google Maps is available.
7. Check current location state as required by the navigation workflow.
8. If location is off, request user-enabled location through the system flow.
9. Wait for the actual state change.
10. Resume the pending navigation request.
11. Construct an official Google Maps navigation URL or supported Android Maps intent.
12. Encode location parameters correctly.
13. Use the user's configured travel mode.
14. Launch Google Maps through Android.
15. Request turn-by-turn navigation when supported.
16. Verify the observable launch result.
17. Avoid claiming that navigation has started if Maps only displayed a route preview.
18. Preserve Home as the current destination context.

### Google Maps URL format

Use the official Maps URLs API.

For supported directions, use the structure:

`https://www.google.com/maps/dir/?api=1&destination=DESTINATION&travelmode=driving&dir_action=navigate`

Replace the placeholder with the actual encoded destination.

Do not hardcode an example address.

Use a place ID or coordinates when appropriate.

### Android-native alternative

Use an appropriate supported Maps navigation intent when preferable.

Validate that the destination application can handle the intent.

### Critical limitation

Do not simulate pressing the Start button in Google Maps through unauthorized automation.

Use the supported navigation request.

If Maps displays a route preview or requires an additional tap, explain that accurately.

---

## 26. INTELLIGENT NAVIGATION FOLLOW-UPS

Example:

User:

"Hey Jarvis, take me home."

If location is off:

JARVIS requests permission to open location controls.

User:

"Yes."

JARVIS opens the system resolution.

The user enables location.

JARVIS verifies the result.

Then:

"Location is enabled, sir. Opening directions to Home."

Google Maps opens.

### Additional follow-ups

"Make it walking."

"Use driving."

"Show another route."

"How far away is it?"

"Stop navigation."

"Open Maps."

For actions outside JARVIS's actual control, use supported Google Maps functionality or explain the required user interaction.

Do not claim to control a Google Maps navigation session without an appropriate integration.

### Ambiguous phrases

If the user says:

"Set my location to home."

and Home already exists, interpret navigation when context indicates travel.

If Home does not exist, ask whether the user wants to save a new Home destination.

Do not overwrite or guess an address.

---

## 27. DEDICATED LOCATION HUD

Create a cinematic, interactive location interface.

Display:

- Real map when authorized
- Current-location marker
- Accuracy estimate
- Latitude and longitude
- Human-readable address when available
- City and country
- Compass-style visual indicators
- Saved destinations
- Navigation controls

Use the existing location and mapping integration.

Do not display fake coordinates.

Do not retain ongoing location history by default.

---

# PART VI — EVERYDAY ANDROID AUTOMATION

## 28. DIRECT APPLICATION LAUNCHING

Commands include:

"Open Spotify."

"Open WhatsApp."

"Open YouTube."

"Open Instagram."

"Open Settings."

"Open yourself."

"Open Jarvis."

### Critical behavior

When the user says:

**"Open yourself."**

JARVIS must open its own main application interface.

When the user says:

**"Open Jarvis."**

JARVIS must also open its main interface.

When the user says:

**"Hey Jarvis."**

without a separate opening command, JARVIS should activate listening and greeting behavior without unnecessarily navigating to the main app.

### Implementation

1. Recognize `OPEN_APPLICATION`.
2. Extract the target.
3. Normalize speech recognition errors.
4. Search permitted installed applications.
5. Resolve exact and saved aliases.
6. Check match confidence.
7. Ask only when ambiguous.
8. Retrieve the app's launch intent.
9. Check Android activity-launch restrictions.
10. Start the target activity where permitted.
11. Report the observable result.
12. Preserve the selected application as context.

Do not insert a fixed ten-second wait.

---

## 29. TWENTY-FIVE APPLICATION ALIASES

Preserve exactly 25 configurable shortcut slots.

Each includes:

- Actual app name
- Package identifier
- Custom nickname
- Optional spoken variants
- Test button
- Edit
- Delete

Example:

Actual app:

Spotify

Nickname:

Music

User:

"Hey Jarvis, open Music."

JARVIS opens Spotify.

### Name recognition

Use:

- Exact matching
- Fuzzy matching
- Phonetic matching
- Spoken-number normalization
- Approved nicknames
- Conversation context

If two aliases conflict, resolve the ambiguity.

When an app is uninstalled, mark the shortcut unavailable.

Do not silently open an unrelated app.

---

## 30. FIFTEEN CONTACT SHORTCUTS

Preserve exactly 15 user-configurable contact shortcuts.

Each includes:

- Nickname
- Linked Android contact
- Selected phone number
- Optional spoken aliases
- Edit
- Delete
- Test

### Example

Nickname:

Dad

Command:

"Hey Jarvis, call Dad."

### Execution

1. Detect calling intent.
2. Resolve the nickname.
3. Retrieve the approved phone number.
4. Validate the contact reference.
5. Check calling capability.
6. Use an authorized Android dial or call workflow.
7. Observe supported call state.
8. Speak the observable result.
9. Preserve the contact reference for follow-ups.

Do not claim a call was answered without reliable information.

Do not automatically dial emergency services through a generated tool call.

---

## 31. UNANSWERED-CALL ASSISTANCE

Preserve the optional post-call voice message workflow.

The previous fifteen-second preference remains configurable.

Do not interpret fifteen elapsed seconds as proof that a call went unanswered.

Use actual supported telephony information.

If the app cannot reliably determine the result, offer a user-controlled post-call follow-up.

### Voice memo

JARVIS may ask:

"Would you like to record a message?"

If yes:

- Start a separate, consented personal voice recording.
- Show recording status.
- Allow Stop.
- Allow Replay.
- Allow Cancel.
- Save only when approved.
- Offer supported sharing.

Do not record the other party's call audio.

### Rejection phrases

Support:

- No.
- No thanks.
- It's okay.
- Never mind.
- Cancel.

If rejected, do not begin recording.

---

## 32. SCREENSHOT AND SMART SHARING

Preserve:

"Take a screenshot."

"Take a screenshot and send it."

"Send this screenshot to Dad."

"Which apps can share this?"

### Capture

Use Android-supported MediaProjection consent where required.

Do not capture protected content or bypass secure windows.

### Sharing

1. Capture permitted content.
2. Save a temporary private image.
3. Show a preview.
4. Query actual compatible sharing apps.
5. Resolve the intended recipient.
6. Use an appropriate Android share intent.
7. Let the user complete required destination selection and authorization.
8. Report only observable outcomes.
9. Clean temporary files.

Do not claim that a message was delivered merely because the Sharesheet opened.

---

## 33. ANDROID QUICK-SETTINGS CONTROL

Preserve voice-controlled supported settings.

Examples:

- Flashlight
- Media volume
- Brightness
- Wi-Fi settings
- Bluetooth settings
- Airplane mode settings
- Mobile data settings
- Hotspot settings
- Battery saver settings
- Location settings
- Display settings
- Sound settings

### Rules

Use direct Android APIs when permitted.

When direct control is restricted, open the official system panel.

Never fabricate a changed state.

After a user-controlled system change, recheck the actual status.

Do not repeatedly ask for confirmation on harmless operations that are already authorized.

---

## 34. SHUT DOWN ALL SYSTEMS

Recognize:

"Hey Jarvis, shut down all systems."

Also:

"Shut down."

"Lock everything."

"End JARVIS and lock my phone."

### Intended behavior

Treat this as an instruction to end JARVIS's active session and securely lock the phone where supported.

Do not treat it as permission to power off Android or destroy unrelated data.

### Workflow

1. Recognize the command.
2. Cancel active JARVIS generation.
3. Stop JARVIS speech.
4. Stop temporary microphone sessions.
5. Stop music recognition if active.
6. Close JARVIS-owned camera sessions.
7. Stop JARVIS-owned temporary background operations.
8. Release screenshot resources.
9. Preserve saved memories and settings.
10. Dismiss JARVIS-owned temporary notifications where appropriate.
11. Execute the authorized screen-lock workflow.
12. Finish the shutdown animation without delaying the lock action.
13. Return to the configured standby state when possible.

### Security

Use an approved Android locking capability where available.

If Device Administrator authorization is required, provide the appropriate official setup.

Do not simulate secure locking by displaying a black screen.

Do not force-stop emergency apps or arbitrary third-party processes.

Do not bypass Android security.

---

# PART VII — CORE JARVIS 10.0 CAPABILITIES

## 35. GENUINE ANIMATED LIVE WALLPAPER

Preserve the existing Sentinel Standby reactor.

Use a native Android `WallpaperService`.

### Standby

- Slow concentric ring rotation
- Independent clockwise and counterclockwise arcs
- Small cyan particles
- Soft breathing glow
- Actual battery percentage
- Compact clock
- Weather beside clock
- Low-power rendering

### Wake activation

- Cyan recognition pulse
- Smooth acceleration
- Slight 5–8% scale expansion
- Purple listening state
- Live transcript
- Smooth return

### Speaking

- Cyan reactor
- Audio-reactive tracks
- Smooth caption scrolling

### Charging

- Separate rotating green outer ring

### Important platform distinction

The wallpaper stays behind launcher icons.

An interactive full-screen JARVIS surface may visually cover icons where Android permits.

Do not assume WallpaperService itself can hide unrelated apps or replace a secure lock screen.

When opening another app directly, do not force JARVIS's activity into the foreground first.

---

## 36. DEDICATED FUTURISTIC HUD MODULES

Each major feature must have a real dedicated UI.

Preserve:

1. Weather HUD
2. Location HUD
3. Battery Diagnostics HUD
4. Wi-Fi HUD
5. Bluetooth Radar HUD
6. Network Diagnostics HUD
7. System Link HUD
8. System Telemetry HUD
9. Music Recognition HUD
10. Camera HUD
11. Clock and Alarm HUD
12. Calling HUD
13. Screenshot Sharing HUD
14. Memory HUD
15. Voice Profile HUD
16. Security Settings HUD

All interfaces must share the same design language.

Use:

- Dark backgrounds
- Cyan geometry
- Real-time state changes
- Smooth circular transitions
- Meaningful data panels
- Responsive controls
- Real information sources

Do not create visual-only mockups.

---

## 37. SUPERDESIGN MOTION INTEGRATION

Use the previously supplied Superdesign project.

Preserve its accessible animation choreography.

### Animation requirements

- Precise easing
- Ring rotation
- Controlled scale
- Smooth screen morphing
- Real audio synchronization
- Animated panel entry
- Small particles
- Consistent colors
- Reduced-motion accessibility
- Device performance adaptation

### State colors

Standby:

Cyan-blue.

Listening:

Purple.

Speaking:

Cyan.

Processing:

Blue-violet.

Charging:

Green outer ring.

Battery percentage:

Use the new battery level color system.

### Rendering

Use native Compose or appropriate accelerated rendering.

Avoid embedding heavy web components for simple native animations.

Pause unnecessary rendering when hidden.

---

## 38. WEATHER INTELLIGENCE

When the user asks:

"What's today's weather?"

"What's tomorrow's weather?"

"Tell me the forecast."

Preserve the two destinations:

**External Weather Application**

**JARVIS Weather UI**

If a default preference exists, use it.

If the user specifies a destination, do not ask again.

If no destination preference is known, offer the two options.

### Data

Use real configured weather sources.

Support:

- Current temperature
- Feels-like temperature
- Humidity
- Wind
- Rain probability
- Hourly forecast
- Daily forecast
- Weather conditions
- Sunrise and sunset
- Timestamp

---

## 39. THE ANIMATED EARTH WEATHER GLOBE

Preserve the cinematic globe.

### Opening

1. Sentinel reactor contracts slightly.
2. Orbital rings expand.
3. A holographic Earth appears.
4. The planet rotates smoothly.
5. Latitude and longitude lines illuminate.
6. JARVIS obtains the authorized location.
7. The globe turns toward the correct coordinates.
8. The camera zooms toward the relevant region.
9. A cyan marker identifies the location.
10. Real weather panels appear.
11. JARVIS speaks the forecast.
12. Captions display the response.

### Technology

Use an efficient native 3D or equivalent rendering system.

Do not load expensive globe assets while the UI is closed.

Use appropriately licensed Earth textures.

Do not claim decorative cloud imagery is live satellite data.

---

## 40. WI-FI INTELLIGENCE

Preserve genuine Wi-Fi information.

Commands include:

"What's my Wi-Fi name?"

"How strong is my signal?"

"Is my internet fast?"

"Run a speed test."

"Why is my connection slow?"

"Show nearby networks."

### Data

Read supported values:

- Current SSID
- Connection state
- Signal strength
- Frequency
- Link speed
- Active network type
- Available network metadata

Distinguish Wi-Fi link speed from internet throughput.

### Real speed testing

Support:

- Download throughput
- Upload throughput
- Latency
- Relevant variability
- Failure reporting

Use a genuine authorized testing endpoint.

Warn about significant data use when necessary.

Do not invent results.

---

## 41. BLUETOOTH INTELLIGENCE

Preserve:

- Paired-device discovery
- Available nearby device scanning
- Connected-device status
- Name recognition
- Device aliases
- Supported connection workflows
- Battery reporting when exposed
- Follow-up context

### Example

"Show my Bluetooth devices."

"Connect to Pro 7."

"What's its battery?"

"Disconnect it."

JARVIS must remember the active accessory.

Do not invent battery levels.

Do not claim unrestricted Bluetooth profile control.

Use supported Android APIs or system pairing workflows.

---

## 42. MUSIC RECOGNITION

Preserve Shazam-style real music identification.

When the user says:

"Identify this song."

1. Request appropriate microphone access.
2. Coordinate audio ownership.
3. Capture the authorized music sample.
4. Use a genuine recognition provider.
5. Receive a verified match.
6. Display the song title and artist.
7. Display artwork when authorized.
8. Offer Spotify actions.
9. Offer the song's meaning.
10. Offer authorized lyrics sources.
11. Clean temporary recordings.

Do not invent song matches.

Do not reproduce full copyrighted song lyrics.

Do not claim Spotify saved a track unless a valid API result confirms it.

---

## 43. CAMERA AND VISION

Preserve front-facing camera access using CameraX or another appropriate supported native API.

Commands:

"Open selfie camera."

"Take a picture."

"Switch camera."

"Close camera."

"Analyze this photo."

### Visual functionality

- Genuine camera preview
- Camera status
- Capture action
- Switch camera
- Photo preview
- Approved analysis

Do not secretly capture photos.

Do not make unsupported medical or appearance judgments.

---

## 44. CLOCK, ALARMS AND TIMERS

Preserve voice-controlled productivity.

Commands:

"Open my clock."

"Set an alarm for tomorrow at seven."

"Wake me up Friday at six thirty."

"Start a twenty-minute timer."

"Show my alarms."

"Cancel my timer."

### Execution

1. Extract the time and date.
2. Resolve the device timezone.
3. Determine whether the request is one-time or repeating.
4. Use the appropriate Android Clock intent or authorized scheduling mechanism.
5. Request missing parameters only when necessary.
6. Verify supported results.
7. Speak a concise confirmation.
8. Preserve the alarm context.

Do not silently convert a specific-date alarm into a recurring weekly alarm.

---

## 45. SAVED WEBSITES

Preserve ten customizable website shortcuts.

Each includes:

- Website name
- HTTPS address
- Description
- Voice nickname
- Edit
- Delete

Support:

"Open my website."

"Search my saved site."

"Summarize that page."

Treat external webpage content as untrusted.

Do not bypass website authentication.

---

## 46. DAILY ECUADOR BRIEFINGS

Preserve optional daily news and information.

Default proposed schedule:

**9:00 AM — America/Guayaquil**

Support:

- Ecuador news
- Quito news
- Technology
- Artificial intelligence
- Science
- Weather
- Authorized calendar information
- Reminders

Use real dated sources.

Respect quiet hours and notification permissions.

Do not fabricate headlines.

---

## 47. PROACTIVE ASSISTANT

Preserve user-configurable proactive features:

- Low-battery alerts
- Charging notifications
- Calendar reminders
- Scheduled news
- Weather updates
- Requested follow-ups
- JARVIS voice-session notifications

Avoid unnecessary notification spam.

Respect user preferences.

Do not promise exact background execution timing when Android power management prevents it.

---

## 48. INTERACTIVE JARVIS VOICE SESSIONS

Preserve the call-like assistant session interface.

Use legitimate Android notifications and activities.

Allow:

- Open session
- Answer
- Decline
- Mute
- End conversation

Do not impersonate an actual cellular call.

Do not misuse restricted full-screen intent privileges.

---

# PART VIII — SECURITY, PERMISSIONS AND TRUST

## 49. REMOVE UNNECESSARY CONFIRMATIONS

The user wants direct execution.

JARVIS must not repeatedly ask:

"Are you sure?"

for every harmless command.

### Low-risk examples

Open an app.

Read the battery percentage.

Open Bluetooth settings.

Show weather.

Start a supported informational query.

These should execute immediately when authorized.

### Internal verification remains mandatory

JARVIS must still verify actual results.

Do not confuse internal result verification with asking the user for unnecessary approval.

### Protected actions

For activities involving sensitive data, external sending, protected device settings or security, follow Android's required consent and authentication flows.

Do not bypass operating-system authentication or permission requirements.

---

## 50. SCREEN LOCK AND UNLOCK

Preserve supported screen-control capabilities.

### Lock

"Hey Jarvis, lock my phone."

Use an authorized supported Android mechanism.

### Unlock

"Hey Jarvis, unlock my phone."

Use Android's official keyguard dismissal and authentication mechanisms.

Do not store or replay system PINs, passwords or patterns.

Do not require an unnecessary custom triple-credential setup.

If Android requires credentials, the user must authenticate through the system UI.

Do not use speaker recognition as a security bypass.

---

## 51. PRIVACY AND PERMISSION DESIGN

Protect:

- Contacts
- Voice profiles
- Audio recordings
- Saved locations
- Home destination
- Screenshots
- Memories
- API credentials
- Personal preferences

### Rules

- Ask for permissions at the appropriate time.
- Explain permissions clearly.
- Use minimum necessary access.
- Preserve Android microphone indicators.
- Do not record other people without consent.
- Do not secretly capture screenshots.
- Do not expose private memories on a locked display.
- Do not embed API secrets in source code.
- Do not collect system unlock credentials.
- Allow users to delete personal information.

Make privacy controls part of the main Settings interface.

---

# PART IX — FIVE PREMIUM PRODUCT CAPABILITIES

## 52. VOICE CALIBRATION LAB

Implement an advanced microphone and speaker test suite.

Features:

- Wake phrase testing
- Noise measurement
- Speech recognition quality
- Supported echo cancellation
- Microphone routing
- Sensitivity adjustment
- Spanish voice testing
- English voice testing

Display actual measured results.

Offer useful configuration recommendations.

---

## 53. ROUTINE STUDIO

Allow the user to create custom multi-step routines.

Example:

"When I say good morning, give me the weather, tell me my reminders, and read my schedule."

Each routine contains:

- Name
- Trigger
- Ordered steps
- Required permissions
- Optional confirmations
- Enabled state
- Test command

Use the normal tool execution engine.

Do not implement a separate unreliable automation subsystem.

---

## 54. INTELLIGENT MEMORY CARDS

Organize approved memories into categories.

Examples:

- Preferences
- Projects
- Personal notes
- Favorite apps
- Saved destinations
- Website shortcuts
- Task context

Allow:

- Search
- Edit
- Pin
- Delete
- Export

Use semantic retrieval for relevant memories.

Do not automatically store everything permanently.

---

## 55. CAPABILITY CENTER

Create a professional page showing what JARVIS can actually do on the current phone.

Classify functions as:

- Available
- Needs permission
- Needs setup
- Requires external service
- Unsupported
- Temporarily unavailable

Include tests for:

- Microphone
- Local model
- TTS
- Bluetooth
- Wi-Fi
- Camera
- Location
- Notifications
- Wallpaper

Do not present unimplemented features as working.

---

## 56. FIRST-RUN PERSONAL AI SETUP

Create a premium onboarding experience.

Steps:

1. Choose English or Spanish.
2. Select the configured voice.
3. Test the microphone.
4. Configure wake sensitivity.
5. Enable desired wake listening.
6. Select local AI preferences.
7. Configure memory.
8. Configure the wallpaper.
9. Configure location permissions.
10. Configure optional contacts and app aliases.
11. Test the first command.
12. Show the user's available capabilities.

Do not force unnecessary permission requests.

Provide skip controls for optional features.

---

# PART X — JARVIS INTERFACE AND EXPERIENCE

## 57. TITAN VISUAL DESIGN SYSTEM

Use the existing black, dark-blue, cyan and purple visual identity.

Suggested base colors:

Deep black:

`#000000`

Navy:

`#050B14`

Cyan:

`#00E5FF`

Blue:

`#00B8FF`

Purple:

`#A855F7`

Charging green:

`#22C55E`

Add the battery-specific white, yellow, orange and red colors previously defined.

### Visual elements

- Rotating reactor rings
- Radial tick marks
- Soft glow
- Orbiting particles
- Animated waveform tracks
- Glasslike information panels
- Compact labels
- Real-time data cards
- Smooth module transitions
- High-quality typography

Do not use random decoration unrelated to state.

---

## 58. TRANSCRIPTION AND ENTITY HIGHLIGHTING

Keep the transcript directly beneath the central reactor.

Display:

`YOU · [actual transcript]`

`JARVIS · [actual response]`

### Behavior

- Streaming partial recognition
- Final transcript replacement
- Smooth scrolling
- Smart punctuation
- Multilingual support
- Compact default height
- Expandable history

### Green entity highlighting

When the user says:

"Open Spotify."

The recognized application name becomes green after resolution.

When the user says:

"Call Dad."

The resolved contact nickname becomes green.

Do not highlight an ambiguous name as confirmed.

---

## 59. CINEMATIC SOUND EFFECTS

Preserve and refine sounds for:

- Wake detection
- Listening
- Processing
- Speaking
- App opening
- Calling
- Bluetooth connection
- Wi-Fi connection
- Weather globe
- Location retrieval
- Screenshot capture
- Alarm creation
- Music recognition
- Camera opening
- Charging
- Low battery
- Critical battery
- Errors
- Returning to standby

Provide separate effect and voice volume controls.

Respect Android audio focus and system sound settings.

Do not let animations or effects delay command execution.

---

## 60. PERFORMANCE MODES

Provide:

**CINEMATIC**

Maximum visual quality within safe hardware limits.

**BALANCED**

Good visuals and efficient battery consumption.

**ECO**

Reduced decorative animation with preserved AI functionality.

### Important

Never remove reasoning capability or user memory simply because ECO mode is selected.

Reduce unnecessary visual overhead first.

---

# PART XI — COMPLETE IMPLEMENTATION ARCHITECTURE

## 61. SUGGESTED CORE COMPONENTS

Extend or integrate with the existing architecture.

Suggested modules:

`JarvisRuntimeCoordinator`

`WakeWordEngine`

`MicrophonePipeline`

`NoiseProcessingCoordinator`

`VoiceActivityDetector`

`StreamingTranscriptionManager`

`SpeakerProfileMatcher`

`GreetingCoordinator`

`LanguageRouter`

`SpanishVoiceProvider`

`EnglishVoiceProvider`

`ConversationCoordinator`

`IntentRouter`

`EntityResolver`

`TaskPlanner`

`ToolRegistry`

`ToolResultVerifier`

`TaskCancellationManager`

`MemoryRepository`

`SemanticMemoryIndex`

`LocalBrainManager`

`FastCommandExecutor`

`BatteryStateRepository`

`BatteryBandResolver`

`BatteryAlertCoordinator`

`LocationRepository`

`SavedDestinationRepository`

`GoogleMapsNavigationCoordinator`

`AppAliasRepository`

`ContactShortcutRepository`

`BluetoothRepository`

`WifiRepository`

`NetworkDiagnosticsRepository`

`WallpaperStateCoordinator`

`SentinelWallpaperEngine`

`HudModuleNavigator`

`AnimationCoordinator`

`AudioEffectsCoordinator`

`PrivacyPermissionCoordinator`

These are conceptual names.

Reuse existing classes when appropriate.

Do not create duplicate repositories for the same system data.

---

## 62. ONE SHARED TASK RESULT CONTRACT

Every tool must return structured results.

Suggested statuses:

`SUCCESS`

`PARTIAL_SUCCESS`

`AWAITING_USER`

`PERMISSION_REQUIRED`

`AUTHENTICATION_REQUIRED`

`UNSUPPORTED`

`FAILED`

`TIMED_OUT`

`CANCELLED`

Each tool result should include:

- Tool name
- Request identifier
- Execution status
- Relevant result data
- Error category
- User-action requirement
- Timestamp
- Retry eligibility
- Verification evidence

Do not use vague success flags disconnected from actual Android results.

---

## 63. UNIVERSAL TASK EXECUTION LIFECYCLE

Every request must follow this process.

### STEP 1 — RECEIVE

Capture voice or text.

### STEP 2 — NORMALIZE

Correct transcription formatting and common recognition errors.

### STEP 3 — CLASSIFY

Determine the request type.

### STEP 4 — EXTRACT

Identify:

- Application names
- Contact names
- Device names
- Dates
- Times
- Destinations
- Numbers
- Commands

### STEP 5 — RESOLVE CONTEXT

Use relevant recent conversation state.

### STEP 6 — SELECT EXECUTION PATH

Choose:

- Native fast path
- Local AI reasoning
- Multi-step planning

### STEP 7 — CHECK CAPABILITIES

Verify:

- Android support
- Permissions
- Connectivity
- Required data
- Current device state

### STEP 8 — PLAN

Construct the minimum reliable sequence.

### STEP 9 — EXECUTE

Call the real supported tool.

### STEP 10 — VERIFY

Check observable results.

### STEP 11 — RESPOND

Give the user an accurate spoken response.

### STEP 12 — UPDATE UI

Trigger the correct animation state.

### STEP 13 — UPDATE CONTEXT

Preserve useful references for follow-ups.

### STEP 14 — CLEAN UP

Release temporary resources.

### STEP 15 — RETURN TO READY

Resume the appropriate authorized wake or conversation state.

---

## 64. POST-TASK BEHAVIOR

After every completed or attempted task:

1. Confirm the result internally.
2. Identify any incomplete steps.
3. Report important failures.
4. Avoid unnecessary confirmation questions.
5. Update relevant system status.
6. Preserve useful recent context.
7. Stop unused microphone or camera operations.
8. Release unnecessary native resources.
9. Prevent duplicate notifications.
10. Return to standby or follow-up listening.

### If a task fails

Do not invent success.

Explain the actual blocker.

Offer the nearest supported alternative.

Do not repeatedly execute consequential operations after failures without authorization.

---

# PART XII — REAL CONVERSATION SCENARIOS

## 65. SCENARIO — OWNER GREETING

The owner says:

"Hey Jarvis."

The wake recognizer detects the phrase.

The voice profile matcher confidently recognizes the owner.

The phone's actual local time is 9:30 AM.

JARVIS responds:

**"Good morning, sir. How can I help you?"**

The reactor transitions to purple listening.

The assistant is ready for the next instruction.

If a different speaker is recognized, use that speaker's approved greeting.

If speaker identity is uncertain, use a neutral greeting.

---

## 66. SCENARIO — DIRECT APP OPENING

User:

"Hey Jarvis, open Spotify."

JARVIS:

1. Detects the wake phrase.
2. Transcribes the command.
3. Recognizes `OPEN_APPLICATION`.
4. Resolves Spotify.
5. Checks that the app is installed.
6. Uses the supported Android launch workflow.
7. Opens Spotify where permitted.
8. Says "Opening Spotify" or reports the blocker accurately.
9. Returns to standby.

Do not delay the application launch for a long greeting.

---

## 67. SCENARIO — BATTERY DROPS TO TEN PERCENT

The system observes a real change from 11% to 10%.

JARVIS:

1. Reads the new battery percentage.
2. Confirms the device is discharging.
3. Sets battery-number color to red.
4. Checks that the 10% alert has not already played in this discharge episode.
5. Schedules the alert promptly.
6. Speaks:

"Please charge your phone. Battery percentage is at ten percent."

7. Records the alert state.
8. Avoids repeating the same alert unnecessarily.

---

## 68. SCENARIO — BATTERY DROPS TO FIVE PERCENT

The system observes a real change from 6% to 5%.

JARVIS:

1. Detects the new percentage.
2. Enters critical battery state.
3. Changes the battery percentage to intense red.
4. Activates restrained critical visual effects.
5. Speaks:

"Please charge your phone immediately. Critical battery percentage is at five percent."

6. Records the level as announced.
7. Waits for the next actual battery event.

It must not repeat the 5% warning every few seconds.

---

## 69. SCENARIO — LOCATION IS OFF

User:

"Hey Jarvis, where am I?"

The location system is disabled.

JARVIS:

"I can't find your location, sir, because location services are turned off. Would you like to turn them on?"

User:

"Yes."

JARVIS:

"Opening location controls, sir."

The supported Android location settings resolution appears.

The user enables location.

JARVIS verifies the state.

JARVIS:

"Location is on, sir. Where would you like to go?"

User:

"Take me home."

JARVIS resolves the saved Home destination.

It launches Google Maps through the supported navigation workflow.

---

## 70. SCENARIO — GO HOME

User:

"Hey Jarvis, navigate home."

JARVIS:

1. Recognizes a navigation request.
2. Retrieves the saved Home destination.
3. Checks location availability.
4. Requests any necessary system setting change.
5. Creates the Maps navigation intent.
6. Opens Google Maps.
7. Requests navigation to Home.
8. Reports whether Maps launched or additional user interaction is required.

Do not invent a home address.

---

## 71. SCENARIO — SHUT DOWN ALL SYSTEMS

User:

"Hey Jarvis, shut down all systems."

JARVIS:

1. Recognizes the shutdown-session intent.
2. Stops its own active AI operations.
3. Stops temporary speech sessions.
4. Saves permitted conversation state.
5. Cleans up camera, recording and screenshot resources.
6. Runs the shutdown animation.
7. Executes an authorized phone-lock workflow.
8. Reports an error if secure locking is unavailable.

Do not factory-reset the phone.

Do not delete memories.

Do not disable unrelated Android services.

---

## 72. SCENARIO — LONG CONVERSATION

User:

"Hey Jarvis, explain how artificial intelligence works."

JARVIS begins its explanation.

The answer lasts longer than ten seconds.

It continues normally.

User:

"Wait, wait."

JARVIS pauses.

User:

"Explain it more simply and give me an example."

JARVIS preserves the topic.

It answers the revised question.

The conversation continues naturally.

No rigid ten-second cutoff is involved.

---

## 73. SCENARIO — SPANISH CONVERSATION

User:

"Oye Jarvis, ¿cómo está el clima mañana?"

JARVIS:

1. Detects Spanish.
2. Retrieves the relevant real forecast.
3. Builds a naturally punctuated Spanish response.
4. Selects the exact Spanish voice identifier:

`ae74d1059517440aa6e5d2d50598d179`

5. Speaks through that voice.
6. Displays the response in the live captions.
7. Preserves the weather context.

Do not silently switch to another Spanish speaker.

---

# PART XIII — WORLD-CLASS TESTING

## 74. MICROPHONE TEST MATRIX

Test:

- Quiet-room wake word
- Music in background
- Television in background
- Multiple speakers
- Soft speech
- Distant speech
- Owner voice
- Guest voice
- Unknown speaker
- Spanish calling word
- Direct compound commands
- Long dictation
- Interrupted TTS
- Phone screen off
- Bluetooth headset
- Audio-focus conflicts

Record actual recognition results.

Measure false positives and false negatives.

---

## 75. BATTERY TEST MATRIX

Test every boundary:

- 100%
- 70%
- 69%
- 31%
- 30%
- 11%
- 10%
- 9%
- 8%
- 7%
- 6%
- 5%
- 4%
- 3%
- 2%
- 1%
- 0% if reported

Verify:

- Correct color
- Actual percentage displayed
- Correct alert phrasing
- No duplicate alert for unchanged percentage
- No repeated alert on service restart
- No alert spam during charging
- Correct behavior when percentages are skipped
- Correct speaking/charging visual coexistence
- Proper audio focus
- Quiet-hour handling

Use simulated battery events for automated tests, but do not claim they are real-device measurements.

---

## 76. NAVIGATION TEST MATRIX

Test:

- Home location saved
- Home location missing
- New Home selection
- Location enabled
- Location disabled
- Approximate location only
- Precise location available
- Permission denied
- Google Maps installed
- Google Maps unavailable
- Navigation URL encoding
- Walking mode
- Driving mode
- Route preview
- Navigation requiring additional user interaction
- Device locked
- App in background

Do not claim that a Google Maps Start button was pressed automatically unless a supported integration genuinely starts navigation.

---

## 77. COMPLETE FEATURE REGRESSION

Test every existing capability:

- Local AI
- English TTS
- Exclusive Spanish TTS
- Wake listening
- Owner greetings
- Six voice profiles
- Long conversations
- Interruption
- Live transcription
- Smart punctuation
- Twenty-five app aliases
- Fifteen contact shortcuts
- Calling
- Voice memos
- Screenshot sharing
- Weather
- Globe animation
- Google Maps
- Bluetooth
- Wi-Fi
- Speed tests
- Battery health
- Battery alerts
- Alarms
- Timers
- Music recognition
- Spotify
- Camera
- Image analysis
- Saved websites
- Daily news
- Notifications
- Local memories
- Wallpaper
- Security and locking
- Settings
- Onboarding
- Performance profiles

Mark tests as:

`PASS`

`FAIL`

`BLOCKED`

`NOT TESTED`

Do not fabricate test success.

---

# PART XIV — STEP-BY-STEP IMPLEMENTATION ROADMAP

## PHASE 1 — FULL SOURCE AUDIT

Inspect the latest repository, current build and feature status.

Map all existing JARVIS 10.0 through 10.5 functionality.

Identify overlapping components and incomplete work.

Protect user data.

## PHASE 2 — CORE ARCHITECTURE

Unify the runtime coordinator, intent router, tool registry, permission manager and conversation context engine.

Remove duplicate or conflicting services.

## PHASE 3 — MICROPHONE ENGINE

Improve wake recognition, noise processing, voice activity detection and streaming transcription.

Add tests for noisy and multispeaker environments.

## PHASE 4 — GREETING SYSTEM

Implement time-aware owner greetings using "sir."

Integrate the six optional voice profiles.

Avoid unnecessary greetings before direct commands.

## PHASE 5 — REMOVE TEN-SECOND WINDOW

Replace rigid timers with adaptive speech activity handling.

Test long utterances, interruptions and immediate app commands.

## PHASE 6 — VOICE PROVIDERS

Preserve the configured English voice.

Use the new exclusive Spanish voice.

Remove discontinued language support safely.

Validate provider compatibility.

## PHASE 7 — LOCAL AI OPTIMIZATION

Benchmark the existing model.

Optimize streaming, context retrieval, inference and startup latency.

Preserve intelligence.

## PHASE 8 — BATTERY COLOR ENGINE

Implement the exact nonoverlapping color thresholds.

Integrate with the Sentinel reactor.

Test every boundary.

## PHASE 9 — BATTERY ALERT ENGINE

Implement real battery observation.

Implement warnings at 10, 9, 8, 7 and 6 percent.

Implement critical warnings at 5, 4, 3, 2 and 1 percent.

Prevent duplicate announcements.

Respect audio focus and user settings.

## PHASE 10 — SMART LOCATION ASSISTANT

Integrate real location detection.

Implement the location-off conversation.

Implement Android-supported settings resolution.

Verify actual location enablement.

## PHASE 11 — SAVED DESTINATIONS

Create persistent named destinations, including Home.

Implement saving, editing, deleting and retrieval.

Protect saved addresses.

## PHASE 12 — GOOGLE MAPS NAVIGATION

Implement official Maps navigation URLs or supported intents.

Add driving, walking and other supported travel modes.

Test the Home workflow.

## PHASE 13 — APP AND CONTACT CONTROL

Preserve twenty-five application aliases.

Preserve fifteen contact shortcuts.

Optimize direct launching and calling.

Add green transcript highlights.

## PHASE 14 — SYSTEM AUTOMATION

Preserve Bluetooth, Wi-Fi, flashlight, volume, brightness and supported settings workflows.

Add shutdown-session and authorized locking behavior.

## PHASE 15 — VISUAL SYSTEM

Preserve the live wallpaper.

Integrate the Superdesign motion references.

Implement battery colors.

Refine dedicated feature HUDs.

Synchronize with speech.

## PHASE 16 — MEDIA AND PRODUCTIVITY

Preserve music recognition, Spotify, camera, screenshots, clock, alarms and timers.

Verify tool outcomes.

## PHASE 17 — MEMORY AND PERSONALIZATION

Preserve user-approved long-term memory.

Integrate owner and guest profiles.

Implement memory cards, preferences and task context.

## PHASE 18 — PREMIUM FEATURES

Implement the Voice Calibration Lab, Routine Studio, Capability Center and premium onboarding.

Ensure that these features share the existing architecture.

## PHASE 19 — SECURITY REVIEW

Review permissions, microphone services, contacts, location, screenshots, secure lock-screen access and API credentials.

Remove unnecessary custom confirmation prompts.

Preserve mandatory Android authentication.

## PHASE 20 — PERFORMANCE

Measure real AI latency, recognition speed, power use and frame pacing.

Optimize for the Redmi 9.

## PHASE 21 — REGRESSION TESTS

Run available automated and instrumentation tests.

Test all previously working capabilities.

Fix detected regressions.

## PHASE 22 — BUILD AND DELIVERY

Build the updated application.

Generate an APK when the build environment supports it.

Report:

- Modified files
- New components
- Data migrations
- Added permissions
- Implemented features
- Unimplemented features
- Device-specific limitations
- Performance results
- Test outcomes
- Build artifact

---

# PART XV — FINAL ACCEPTANCE REQUIREMENTS

JARVIS 10.7 must satisfy the following requirements or clearly document the blockers.

1. The existing app is preserved.
2. The current local AI model remains functional.
3. Persistent memory is preserved.
4. The microphone is improved using measured results.
5. Wake-word recognition is more robust.
6. No rigid ten-second listening window remains.
7. Active speech is not cut off arbitrarily.
8. Owner voice recognition supports "sir" greetings.
9. Greetings change with the time of day.
10. Unknown speakers receive neutral greetings.
11. Six optional speaker profiles remain available.
12. English speech remains functional.
13. Spanish uses the exclusive specified voice.
14. Removed language support is not reactivated.
15. Responses use natural punctuation and pacing.
16. Voice interruption works where supported.
17. Direct application opening remains fast.
18. Twenty-five application aliases work.
19. Fifteen contact shortcuts work.
20. Calling uses supported Android workflows.
21. Screenshot capture respects consent.
22. Screenshot sharing uses real compatible apps.
23. Battery percentage is accurate.
24. Battery percentage colors match exact thresholds.
25. At 10% through 6%, each observed downward percentage triggers the correct normal warning once per discharge episode.
26. At 5% through 1%, each observed downward percentage triggers the correct critical warning once per discharge episode.
27. Duplicate battery announcements are prevented.
28. Charging effects remain functional.
29. Battery diagnostics use real supported measurements.
30. The location-off conversation is implemented.
31. Location settings use the supported Android resolution mechanism.
32. JARVIS verifies when location becomes available.
33. Home can be saved.
34. Home can be edited or deleted.
35. "Take me home" resolves the correct destination.
36. Google Maps opens through an official navigation workflow.
37. No fake navigation-start result is reported.
38. "Open yourself" launches JARVIS.
39. "Shut down all systems" ends JARVIS activity and requests authorized locking.
40. The genuine live wallpaper remains animated.
41. The Sentinel reactor preserves its existing design.
42. Cyan speaking and purple listening remain functional.
43. The weather globe remains functional.
44. The dedicated module interfaces remain available.
45. Bluetooth and Wi-Fi intelligence remain functional.
46. Music recognition uses a real provider.
47. Spotify actions require authorization.
48. Front-camera capabilities remain functional.
49. Alarms and timers use real supported mechanisms.
50. Ten website shortcuts remain available.
51. Daily Ecuador briefings remain available.
52. Voice Calibration Lab is integrated.
53. Routine Studio is integrated.
54. Intelligent Memory Cards are integrated.
55. Capability Center is integrated.
56. First-run setup is integrated.
57. Android security is not bypassed.
58. Device performance is measured.
59. Available tests are executed.
60. The project builds successfully in the available environment.

**Do not mark blocked or incomplete features as finished.**

---

# PART XVI — THE FINAL TITAN EXPERIENCE

My phone is on the home screen.

The real Sentinel live wallpaper is visible.

The familiar circular reactor rotates gently.

The actual battery percentage sits at the center.

The clock and compact weather information remain above it.

I say:

**"Hey Jarvis."**

The microphone recognizes the calling word.

The reactor responds instantly.

A cyan activation pulse moves around its rings.

The circle expands slightly.

The rings accelerate.

The assistant recognizes my enrolled owner voice.

It checks the phone's local time.

If it is morning, JARVIS says:

**"Good morning, sir. How can I help you?"**

The reactor turns purple.

JARVIS listens.

I say:

**"What's my battery percentage?"**

JARVIS reads the actual device measurement and responds naturally.

Later, the battery reaches ten percent while discharging.

The percentage changes to red.

JARVIS says:

**"Please charge your phone. Battery percentage is at ten percent."**

When it decreases to nine percent, it announces nine percent.

When it reaches five percent, the reactor enters critical-red battery styling.

JARVIS says:

**"Please charge your phone immediately. Critical battery percentage is at five percent."**

The percentage remains real.

The warning does not repeat unnecessarily.

I say:

**"Hey Jarvis, where am I?"**

Location is disabled.

JARVIS responds:

**"I can't find your location, sir, because location services are turned off. Would you like to turn them on?"**

I say:

**"Yes."**

JARVIS opens the supported Android location controls.

I enable location.

JARVIS verifies that the service is available.

It responds:

**"Location is on, sir. Where would you like to go?"**

I say:

**"Take me home."**

JARVIS remembers my saved Home destination.

It prepares the correct Google Maps navigation request.

Google Maps opens with the appropriate destination and navigation action.

I say:

**"Hey Jarvis, open yourself."**

JARVIS opens its main application.

The futuristic HUD appears.

The reactor rotates smoothly.

I ask a complex question.

JARVIS uses its optimized local AI brain.

Its response begins promptly.

Its speech feels natural.

Its captions scroll smoothly.

When I interrupt:

**"Wait, wait."**

JARVIS immediately transitions toward listening.

It preserves the current topic.

I ask another question.

JARVIS understands the reference.

When I speak Spanish, it uses the exclusive selected Spanish voice.

When I request Bluetooth devices, the real radar interface appears.

When I request weather, the holographic Earth rotates toward my location.

When I request music recognition, JARVIS uses a real identification provider.

When I request a call, it resolves my approved contact shortcut.

When I request a screenshot, it uses Android's permitted capture and sharing workflow.

When I say:

**"Hey Jarvis, shut down all systems."**

JARVIS ends its active operations, preserves my memories, and requests the authorized screen-lock action.

The interface returns to standby.

The entire experience feels immediate, intelligent, cohesive and technically advanced.

---

# FINAL ENGINEERING DIRECTIVE

**BUILD THE MOST CAPABLE JARVIS THAT THE CURRENT ANDROID PLATFORM AND TARGET HARDWARE CAN RELIABLY SUPPORT.**

Do not sacrifice intelligence for speed.

Do not sacrifice security for convenience.

Do not sacrifice battery life for decorative animations.

Do not replace real Android actions with mock responses.

Do not ask unnecessary confirmation questions for harmless, already-authorized commands.

Do not fabricate successful operations.

Do not invent phone measurements.

Do not remove existing functionality.

Do not erase user memories.

Do not introduce a rigid ten-second listening window.

Do not use the wrong Spanish voice.

Do not reintroduce removed language modes or separate special AI activation modes.

**DO IMPLEMENT:**

- Highly optimized microphone recognition.
- Owner-specific time-aware greetings.
- Natural uninterrupted conversation.
- Persistent contextual memory.
- Exclusive Spanish TTS configuration.
- Ultra-low-latency local AI.
- Direct Android command execution.
- Exact battery colors and alerts.
- Smart location-permission conversations.
- Saved Home navigation.
- Google Maps integration.
- Application aliases.
- Contact shortcuts.
- Calling and screenshot workflows.
- Genuine real-time animated interfaces.
- All original JARVIS 10.0 modules.
- Professional error recovery.
- Comprehensive tests and performance validation.

**INSPECT THE CURRENT CODEBASE. IMPLEMENT THE COMPLETE UPGRADE SAFELY. VERIFY EVERY CAPABILITY. FIX REGRESSIONS. OPTIMIZE FOR THE REDMI 9. DELIVER A REAL WORKING BUILD WHERE SUPPORTED.**

# J.A.R.V.I.S. 10.7 — PROJECT TITAN

**BEGIN THE COMPLETE ENGINEERING UPGRADE NOW.**