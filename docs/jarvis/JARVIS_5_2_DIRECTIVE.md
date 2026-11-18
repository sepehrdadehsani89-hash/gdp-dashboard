# J.A.R.V.I.S. 5.2 — NEXUS INFINITY
## ULTIMATE UNIFIED MASTER ENGINEERING PROMPT
### JARVIS 5.0 + JARVIS 5.1 + QR PAIRING REPAIR
### Android 12 • Real AI • Google Cloud • Real Accounts • Windows Companion • Full System Controls • Custom Wallpaper • Voice Intelligence • Verified APK

# 0. MISSION

You are an elite software engineering team consisting of Android specialists, Kotlin engineers, AI systems architects, Google Cloud developers, OAuth security engineers, Windows desktop engineers, computer vision specialists, networking engineers, and futuristic UI designers.

Upgrade my EXISTING J.A.R.V.I.S. Android application.

Target hardware:

Xiaomi Redmi 9  
Android 12  
Windows development computer  
Local computer address previously observed: 192.168.100.74  
Existing companion endpoint previously observed: HTTPS port 8765

Verify all actual environment details before using them.

Do not create a replacement application or change the installed application identity unnecessarily.

This specification merges ALL features requested across:

- JARVIS 5.0 — NEXUS
- JARVIS 5.1 — Final Integration & Delivery
- JARVIS 5.2 — QR Scanner and Secure Pairing Repair

Preserve all earlier 4.1 features as well.

Nothing already working should be removed.

Do not sacrifice actual functionality for beautiful animations.

# THE PRIMARY RULE

REAL CONNECTIONS.

REAL DATA.

REAL AUTHENTICATION.

REAL ANDROID ACTIONS.

REAL WINDOWS ACTIONS.

REAL TEST RESULTS.

REAL BUILD ARTIFACTS.

NEVER USE FAKE CONNECTED STATES.

---

# 1. MANDATORY REPOSITORY AUDIT

Before changing source code:

1. Identify the existing repository and active branch.
2. Identify the actual Android application ID.
3. Identify the installed app version.
4. Identify the package signature requirements.
5. Identify the QR pairing screen source.
6. Identify the camera or QR scanning implementation.
7. Identify the existing invitation parser.
8. Identify the Android pairing network client.
9. Identify the Windows companion implementation.
10. Identify the computer approval protocol.
11. Identify the AI backend implementation.
12. Identify Google authorization code.
13. Identify Android command execution code.
14. Identify the wallpaper and holographic core implementation.
15. Identify existing test coverage and known defects.

Preserve existing user data, certificates, private pairing state, settings, and working integrations.

Make a baseline build before major changes when possible.

Do not delete the existing Windows companion identity.

Do not create new signing keys unnecessarily.

---

# 2. CURRENT CRITICAL BUG — QR SCANNER DOES NOT OPEN

This is a confirmed user-reported failure.

Inside Jarvis:

Settings → Rebirth → Paired Computers

The screen shows:

Saved computers: 0/20

Selected: None

Button:

"Scan computer's one-time QR"

The button does not successfully open a functioning QR scanner.

The user attempted manual invitation transfer instead.

The manual text invitation appeared valid in format.

The Windows Companion generated a real QR and invitation.

However, pressing "Request pairing — approve on both devices" did not create a pending request on Windows.

Typing "pending" in the companion returned immediately to the prompt without a ticket.

The phone could reach an HTTPS server at the previously observed private computer address, but successful companion authentication was not established.

The user also observed an HTTPS certificate warning in Chrome, consistent with an untrusted private certificate.

Do not mistake browser reachability for verified pairing.

Do not bypass TLS certificate verification.

FIX THE ENTIRE SCANNING, INVITATION, REQUEST, AND APPROVAL PIPELINE.

---

# 3. BUILD A REAL QR SCANNER

Investigate why pressing the existing scanner button produces no working result.

Check:

- Missing onClick handler
- Disabled state
- Incorrect navigation
- Missing scanner activity
- Activity lifecycle errors
- Android manifest configuration
- Google Play services availability
- QR scanner dependency
- Camera permission handling
- Missing camera initialization
- Compose state bugs
- Exception handling
- ActivityResult integration
- Android 12 compatibility
- Xiaomi device behavior
- Missing scanner result callback

Implement a production-quality QR scanner.

Preferred approach:

Google Code Scanner API where compatible.

Use the official API:

GmsBarcodeScanning.getClient(context)

Configure QR-only scanning.

Launch the real scan flow.

Receive actual scanned barcode data.

Handle cancellation and failure.

Do not use a fake camera preview.

Do not display a static screenshot.

If Google Code Scanner is unavailable, implement an appropriate alternative using CameraX and a maintained, compatible ML Kit barcode-scanning implementation.

Where appropriate, use bundled model dependencies to avoid relying on a delayed model download.

Use official documentation and compatible library versions.

---

# 4. QR SCANNER USER EXPERIENCE

When I tap:

"Scan computer's one-time QR"

Jarvis must:

1. Open the real scanning interface.
2. Display a clear QR scanning frame.
3. Detect an actual QR code.
4. Read the full encoded data.
5. Return to the pairing screen.
6. Validate the actual invitation.
7. Display the detected computer.
8. Show the address and certificate fingerprint for review.
9. Enable the Request Pairing action.
10. Begin secure pairing only after user confirmation.

Add:

- Cancel
- Retry scanning
- Flashlight where supported
- Permission assistance where needed
- Manual invitation entry
- Paste invitation
- Select QR image from gallery
- Clear previous invitation
- Expired invitation warning

Gallery import must decode the actual selected image locally.

Never upload the QR image to an external service just to decode it.

---

# 5. QR INVITATION FORMAT

Support the existing JARVIS-PAIR-1 protocol.

The invitation is generated by the actual Windows companion.

Preserve the established wire format.

Do not invent a new incompatible format without an explicit migration strategy.

The parser must:

- Recognize the correct protocol prefix.
- Decode the full payload.
- Validate required fields.
- Validate syntax and encoding.
- Validate expiration.
- Validate supported protocol version.
- Validate the intended companion address.
- Validate certificate pin data.
- Reject malformed invitations.
- Reject expired invitations.
- Reject unsupported versions.

Handle safely:

- Leading or trailing whitespace
- Line wrapping introduced by text editors
- Clipboard transfer
- Complete payload length
- Scanner result normalization

Do not truncate the encoded invitation.

Do not confuse:

- QR image filename
- Private invitation payload
- Companion HTTPS URL
- SHA-256 certificate fingerprint
- Pending pairing ticket
- Six-character comparison code

These are separate pieces of information.

---

# 6. FIX THE DISABLED OR NONFUNCTIONAL REQUEST PAIRING BUTTON

The Request Pairing button previously became blue but appeared to do nothing.

Fix this completely.

The button must have an actual handler.

When tapped:

1. Verify that the invitation is valid.
2. Verify that it has not expired.
3. Enter an explicit PAIRING_REQUESTING state.
4. Show a loading indicator.
5. Send a real request to the invited companion endpoint.
6. Report progress.
7. Handle network and TLS errors.
8. Handle server rejection.
9. Receive the pairing challenge.
10. Display the actual verification code.
11. Advance to the approval state.

Never silently ignore a tap.

Every failure must produce an understandable error.

Examples:

INVITATION_EXPIRED

INVALID_INVITATION

UNSUPPORTED_PROTOCOL

NETWORK_UNREACHABLE

TLS_CERTIFICATE_MISMATCH

COMPANION_OFFLINE

REQUEST_REJECTED

PAIRING_TIMEOUT

APPROVAL_PENDING

APPROVAL_DENIED

PAIRING_SUCCESSFUL

Preserve useful redacted diagnostic details.

---

# 7. COMPLETE SECURE PAIRING STATE MACHINE

Implement:

UNPAIRED

SCANNER_OPENING

SCANNING

INVITATION_SCANNED

INVITATION_VALIDATING

INVITATION_READY

REQUESTING_PAIRING

AWAITING_DESKTOP_APPROVAL

DESKTOP_APPROVED

FINALIZING_PAIRING

PAIRED_UNVERIFIED

PAIRED_VERIFIED

EXPIRED

FAILED

CANCELLED

REVOKED

Every state must have appropriate user-visible behavior.

Do not claim successful pairing merely because the invitation was scanned.

---

# 8. REAL WINDOWS APPROVAL TICKET

When the phone submits an authorized pairing request, the Windows Companion must create an actual pending request.

In the companion terminal:

pending

Must show the real outstanding request.

The output should identify:

- Ticket identifier
- Comparison code
- Request age
- Expiration
- Intended device

Do not output permanent private tokens.

The operator approves using:

approve ACTUAL_TICKET

The app must never tell the user to substitute the computer URL or certificate fingerprint for the ticket.

If no ticket exists, return an explicit message:

"No pending pairing requests."

Do not silently return to the prompt without a useful explanation.

Make invalid, expired, duplicate, and already-approved ticket responses understandable.

---

# 9. FIX END-TO-END PAIRING COMPLETION

After approval on Windows:

1. Android receives or retrieves the legitimate approval result.
2. Verify session identity.
3. Verify certificate pin.
4. Verify request correspondence.
5. Store authorized session credentials securely.
6. Mark the computer as paired.
7. Display its actual name.
8. Update Saved Computers from 0/20 to 1/20.
9. Allow selecting the paired computer.
10. Perform an authenticated connection check.

Do not confuse saved pairing with live connection.

Display separate indicators:

PAIRED

ONLINE

AUTHENTICATED

CAPABILITIES VERIFIED

OFFLINE

REAUTHENTICATION REQUIRED

---

# 10. QR PAIRING SECURITY

Use the existing private-LAN HTTPS design unless a justified compatibility improvement is required.

Preserve:

- TLS certificate validation
- Certificate fingerprint pinning
- Invitation expiration
- Mutual user approval
- Short-lived pairing challenges
- Secure session credentials
- Replay prevention
- Command authorization
- Device revocation

Do not:

- Disable TLS validation
- Accept any certificate
- Bypass hostname checks
- Open an unauthenticated remote-control server
- Automatically approve unknown phones
- Store credentials in plaintext
- Publish private pairing invitations
- Treat ADB as permanent remote control

The QR invitation is sensitive and must remain private.

---

# 11. QR AND NETWORK DIAGNOSTICS

Create an internal pairing diagnostics interface.

Display:

- Scanner initialization status
- Google Code Scanner availability
- Camera status where applicable
- QR payload validation result
- Invitation expiration status
- Companion address
- TLS certificate verification status
- Network reachability
- Pairing request result
- Desktop approval result
- Final session verification
- Last successful connection

Add:

"Run Pairing Diagnostics"

"Retry"

"Generate New Invitation on Computer"

"View Setup Instructions"

"Export Redacted Logs"

Never export the invitation secret or session token.

---

# 12. WINDOWS COMPANION REPAIR

Preserve the existing Windows companion.

Do not force the user to reinstall Python unnecessarily.

Support:

- Existing source launcher
- Existing private TLS identity
- Existing pairing invitation generator
- Existing approved command protocol
- Existing pairing database
- Existing authenticated browser actions

Repair compatibility issues between the Android client and companion.

Verify that the versions actually speak the same protocol.

If a companion update is required, package and document it.

Preserve the existing private state directory whenever possible.

Do not silently regenerate certificates or break previously trusted devices.

---

# 13. AUTOMATIC NEARBY COMPUTER DISCOVERY

Preserve the previously requested local-network discovery system.

Support up to 20 explicitly paired computers.

Implement supported discovery using:

- Android Network Service Discovery
- mDNS where appropriate
- Trusted local networking
- Authenticated companion connections

The first pairing must require user approval.

Subsequent reconnection should use saved trust where valid.

Do not require rescanning every time.

Do not require Android Wireless Debugging for ordinary Jarvis computer operations.

ADB is a development transport, not the production computer companion protocol.

---

# 14. REAL WINDOWS COMPUTER CONTROL

Preserve and extend authorized companion capabilities.

Support where actually implemented:

- Open Chrome
- Search Google
- Open websites
- Open approved applications
- Control media
- Adjust volume
- Read computer status
- View approved telemetry
- Open authorized files
- Open approved folders
- Access approved workspaces
- Lock Windows
- Sleep or restart with confirmation
- Shutdown with confirmation

Use typed, permission-checked commands.

Do not allow unrestricted AI-generated shell commands.

Clearly distinguish existing browser-only capabilities from newly verified features.

Never claim functionality that remains unimplemented.

---

# 15. REAL GOOGLE CLOUD CONFIGURATION

Preserve every JARVIS 5.1 Google Cloud feature.

Add:

Settings → Connections → Google Cloud Configuration

Fields:

- Google Cloud Project ID
- Optional Google API Key
- OAuth Client ID configuration
- Google Account Selector
- Authorized Service List
- Permission Management
- Test Google Identity
- Test Gmail
- Test Calendar
- Test Drive
- Reauthorize
- Remove Account
- Reconfigure

Use real saved configuration.

Never display a green connected indicator merely because an email address is present.

---

# 16. GOOGLE API KEY VS OAUTH

Implement both, with proper separation.

A Google Cloud API key can support certain permitted API operations.

It does not give unrestricted access to private Gmail, Calendar, Drive, Photos, or other account content.

For private Google data, use official OAuth 2.0 authorization and service-specific permissions.

Use the real Google account chooser.

Never request Google passwords inside Jarvis.

Never scrape cookies or consumer sessions.

Never invent Google credentials.

Explain Google Cloud setup in the application.

---

# 17. COMPLETE GOOGLE SERVICES

Preserve supported integrations for:

- Gmail
- Calendar
- Drive
- Docs
- Sheets
- Slides
- Tasks
- Contacts
- Google Photos
- YouTube
- Google Maps
- Google Meet
- Google Chat
- Google Forms
- Google Classroom
- Google Keep where supported
- Additional eligible Google services

For every service:

1. Check API availability.
2. Check account eligibility.
3. Check authorization.
4. Perform a real API request.
5. Display actual results.
6. Report unsupported capabilities honestly.

Do not promise universal account access without the appropriate service permissions.

---

# 18. REAL GMAIL CONNECTION

This is a mandatory high-priority integration.

Implement:

- Gmail OAuth
- Account selection
- Genuine authenticated requests
- Unread message count
- Message search
- Sender information
- Subject information
- Authorized message previews
- AI summaries
- Relevant email notifications
- Secure session restoration
- Reauthentication
- Error reporting

Commands:

"Check my Gmail."

"Summarize my inbox."

"How many unread emails do I have?"

"Did my teacher email me?"

"What's my most important email?"

"Catch me up on my messages."

Never generate fictional email results.

Do not confuse notification counts with real Gmail unread counts.

---

# 19. REAL-TIME GMAIL AND NOTIFICATION UPDATES

Preserve the entire event-driven update architecture.

Use supported Gmail push mechanisms where configured:

- Gmail watch
- Google Cloud Pub/Sub
- History-based synchronization
- Appropriate backend processing

For Android notifications, use NotificationListenerService after explicit permission.

Support:

- Gmail
- WhatsApp
- Instagram
- Telegram
- SMS app notifications
- Other authorized notifications

Read only data actually exposed to the authorized integration.

Do not scrape private histories.

Do not poll cloud services every millisecond.

Update the UI when actual events arrive.

---

# 20. REAL GOOGLE DRIVE PROJECT MEMORY

Support authorized discovery of:

- Actual folders
- Real documents
- Real spreadsheets
- Real presentations
- File titles
- Modification timestamps
- User-approved project records

Example:

"What projects am I working on?"

Return projects grounded in actual accessible data.

Do not invent project names.

Keep separate Google accounts isolated.

Allow the user to delete indexed metadata.

---

# 21. REAL AI PROVIDER CONNECTIONS

Preserve:

- OpenAI
- Gemini
- Claude
- Perplexity
- Local Brain

Fix provider authentication, model access, inference verification, session restoration, and error reporting.

Distinguish consumer ChatGPT login from OpenAI API access.

Do not treat an account login as working inference.

Provide real model tests.

Implement supported credential refresh and automatic reconnection.

Do not add cloud workspaces to Local Brain.

---

# 22. REAL AI TOOL EXECUTION

Implement a structured assistant tool router.

Flow:

Voice
→ Speech Recognition
→ Intent Detection
→ Tool Selection
→ Authorization
→ Real Execution
→ Verified Result
→ Spoken Response

Support tools for:

- Gmail
- Calendar
- Drive
- Tasks
- Device reminders
- Notifications
- Installed Android applications
- Android system controls
- Wallpaper changes
- Silent Mode
- Deep Shutdown
- Paired computer discovery
- Computer commands

Never report success from an AI-generated sentence alone.

---

# 23. ANDROID 12 CONTROL ENGINE

Preserve all previously requested device controls:

- Real flashlight control
- Installed app launching
- Approved device locking
- Secure unlock flow
- Wi-Fi settings
- Bluetooth settings
- Hotspot settings
- Airplane Mode settings
- Volume
- Mute
- Vibrate
- Do Not Disturb
- Battery
- Battery Saver
- Dark Mode
- Reading Mode
- Orientation
- Screen recording
- Cast
- Quick Share
- Mi Share
- TV remote integrations
- Focus Mode
- Bedtime Mode
- Wallet launching
- Scanner
- Song search
- Reminders
- Calendar
- Notification summaries

Use the actual Android API when available.

When Android requires user confirmation or system settings, open the legitimate interface.

Never claim to have silently toggled a restricted system control.

Never bypass device authentication.

---

# 24. COMPLETE FUTURISTIC WALLPAPER STUDIO

Preserve all JARVIS 5.0 wallpaper features.

Implement a real WallpaperService and visual editor.

Allow full customization of Jarvis-owned elements.

Include:

- Free drag-and-drop placement
- X/Y positioning
- Rotation
- Scaling
- Opacity
- Layer ordering
- Visibility
- Alignment
- Snap-to-grid
- Save layout
- Restore layout
- Multiple profiles

Preserve the original holographic core geometry unless the user intentionally customizes it.

---

# 25. FULL-SPECTRUM COLOR SYSTEM

Support:

- HEX
- RGB
- HSV/HSL
- Gradient editor
- Glow intensity
- Transparency
- Preset palettes
- Custom palettes

Editable elements include:

- Core
- Rings
- Particles
- Waveforms
- Clock
- Date
- City
- Weather
- Temperature
- Battery
- Gmail indicators
- Notifications
- Computer status
- AI status
- Reminders
- Background
- Typography
- Borders
- Additional user-added widgets

Do not limit users to a small number of preset colors.

---

# 26. WALLPAPER INFORMATION WIDGETS

Preserve the complete data widget library.

Provide actual data where available for:

- Time
- Date
- City
- Weather
- Temperature
- Forecast
- Sunrise
- Sunset
- Battery
- Charging
- Network
- Bluetooth
- AI provider
- Connection status
- Gmail unread count
- New email
- Notification previews
- Next reminder
- Calendar schedule
- Tasks
- Computer connectivity
- Music
- Volume
- Microphone state
- Silent Mode
- Jarvis status
- Storage
- Device telemetry where permitted

Clearly mark decorative information separately from live data.

No invented system metrics.

---

# 27. WALLPAPER EMAIL SUMMARIES

When an authorized new email arrives:

1. Receive a genuine event or synchronization result.
2. Show an appropriate wallpaper indicator.
3. Display only permitted private information.
4. Allow the user to ask for a summary.
5. Retrieve and summarize genuine message data.
6. Animate the corresponding temporary Jarvis card away after successful delivery.
7. Keep the original email unchanged by default.

Do not automatically delete emails.

Do not automatically mark emails as read without permission.

New messages should create new cards.

Use deduplication.

---

# 28. PREMIUM VISUAL THEMES

Preserve:

- Holographic Command Center
- Arc Reactor-inspired core
- Deep Space
- Cyberpunk Neon
- Quantum Violet
- Minimal Intelligence
- Obsidian
- Custom Laboratory

Offer:

- Battery Saver
- Balanced
- High Quality

Use smooth, optimized animation.

Respect reduced motion.

Pause expensive rendering when the wallpaper is not visible.

---

# 29. SILENT MODE

Preserve advanced Silent Mode.

Commands:

"Activate Silent Mode."

"Enter quiet mode."

"Activate focus protocol."

Behavior:

- Change Jarvis wallpaper
- Suppress Jarvis alerts
- Hide private previews
- Reduce animations
- Apply permitted Android Do Not Disturb behavior when authorized
- Respect user-defined exceptions
- Preserve essential reminders according to settings

Do not falsely claim that outside messages stop arriving.

---

# 30. VOICE REACTIVATION ANIMATION

When Silent Mode allows optional wake-word listening:

"Hey Jarvis."

Trigger a futuristic, approximately 3–5-second visual activation sequence.

The core gradually illuminates.

Orbital rings activate.

Information panels fade in.

The assistant becomes visually active.

Do not delay actual command processing merely to complete an animation.

If wake-word listening is unavailable in the background, provide a manual alternative.

Accurately indicate when the microphone is active.

---

# 31. INTELLIGENT NATURAL SPEECH

Preserve professional voice interactions.

Example:

User:

"Hey Jarvis."

Assistant:

"Good to have you here, sir. What can I help with?"

The assistant should:

- Understand follow-up questions
- Respond naturally
- Avoid repetitive greetings
- Use real tool results
- Speak concisely
- Support English, Spanish, and Persian
- Use "sir" as a configurable preference

Optimize response latency with local intent routing and streaming where available.

No fabricated information.

---

# 32. 500+ FAREWELL RESPONSES

Preserve the entire 500+ farewell variation requirement.

Recognize natural farewell intent.

Examples:

"Goodbye."

"See you later."

"I have to go."

"Thank you, I'm leaving."

"That's all."

"You can rest."

"Stand down."

"Good night."

"Stop listening."

Use context-aware classification.

Do not shut down when a phrase merely appears inside an unrelated request.

Generate varied professional replies, optionally mentioning real upcoming reminders.

---

# 33. FIX MICROPHONE SHUTDOWN

This bug must be repaired.

After a genuine farewell:

1. Finish any permitted reminder check.
2. Speak the final response.
3. Wait for speech synthesis completion.
4. Stop active speech recognition.
5. Cancel stale callbacks.
6. Release conversation microphone resources.
7. Prevent automatic conversation restart.
8. Return the core to idle.

Do not claim complete microphone shutdown while optional wake-word listening is still active.

Provide Stop All Listening.

---

# 34. DEEP SHUTDOWN

Preserve JARVIS Deep Shutdown.

Commands:

"Jarvis, shut down your systems."

"Deactivate Jarvis completely."

"Enter Deep Shutdown."

Require appropriate confirmation.

Stop:

- Active conversation
- Active speech capture
- Optional wake-word monitoring
- Jarvis-owned active companion sessions
- Optional background AI activity
- Unnecessary Jarvis jobs
- Jarvis-owned animations where appropriate

Manual application opening must reactivate Jarvis.

Do not shut down unrelated Android services.

Do not turn off the phone or bypass OS safeguards.

---

# 35. COMPLETE SETTINGS ARCHITECTURE

Provide:

AI & Intelligence

Google Cloud Configuration

Google Connected Services

Account & Workspace Management

Gmail & Messages

Android Controls

Permissions Center

Wallpaper Studio

Color Studio

Widget Library

Voice Personality

Reminders

Silent Mode

Deep Shutdown

Computers

Scan Computer QR

Pairing Diagnostics

Computer Permissions

System Diagnostics

Setup Wizard

Every control must have working functionality.

---

# 36. USER SETUP WIZARD

Build an interactive first-launch and reconfiguration wizard.

Include:

1. Welcome
2. Device permissions
3. AI provider setup
4. Google Cloud configuration
5. Google account selection
6. OAuth permissions
7. Gmail testing
8. Calendar and Drive testing
9. Notification listener setup
10. Android system controls
11. Voice setup
12. Wallpaper customization
13. Computer pairing
14. Live verification
15. Completion

Allow optional services to be skipped.

Do not mark skipped integrations as connected.

---

# 37. REAL QR PAIRING ACCEPTANCE TEST

This is the highest-priority new acceptance test.

On a physically connected Redmi 9:

1. Start the actual Windows companion.
2. Generate a fresh private QR invitation.
3. Display the QR on Windows.
4. Open Jarvis.
5. Tap Scan Computer's One-Time QR.
6. Verify that a real scanner opens.
7. Scan the displayed QR.
8. Confirm that the complete invitation was decoded.
9. Verify invitation validity.
10. Tap Request Pairing.
11. Observe an actual pending request on Windows.
12. Compare the verification code.
13. Approve the actual ticket.
14. Finish pairing on Android.
15. Confirm Saved Computers becomes 1/20.
16. Perform an authenticated connection check.
17. Run one authorized computer command.
18. Verify the actual Windows result.

Record each step's outcome.

Do not report success if only scanner opening was verified.

Do not substitute mocked test results for a real phone-to-Windows test.

---

# 38. QR SCANNER NEGATIVE TESTS

Test:

- Scanner opens correctly
- Scanner cancellation
- Camera unavailable
- Play services unavailable
- Camera permission denied where relevant
- Wrong QR format
- Expired invitation
- Malformed invitation
- Missing payload
- Incorrect certificate
- Companion offline
- Network unreachable
- Companion on another subnet
- Wrong protocol version
- Replayed invitation
- Duplicate pairing
- Approval timeout
- Approval rejected
- App backgrounding
- Activity recreation
- Screen rotation
- Lost connectivity
- Reconnection after process restart

Every failure must show a helpful explanation.

---

# 39. REAL GOOGLE CONNECTION TESTS

Verify:

- Account selection
- Correct identity
- OAuth scope grants
- Gmail API request
- Actual Gmail data or empty response
- Drive API request
- Calendar API request
- Credential refresh
- App restart persistence
- Account switching
- Account removal

No test should claim success simply because an email address is visible.

---

# 40. REAL ANDROID AND AI TESTS

Verify:

- Open Gmail application
- Open Chrome
- Flashlight
- Volume
- Supported phone locking
- Legitimate unlock authentication
- Reminders
- Notifications
- OpenAI inference
- Gemini inference
- Tool selection
- Actual tool execution
- Farewell
- Microphone release
- Silent Mode
- Deep Shutdown
- Wallpaper persistence

Report physical-device tests separately from mocked tests.

---

# 41. COMPLETE SETUP DOCUMENTATION

Generate:

JARVIS_SETUP_GUIDE.md

JARVIS_GOOGLE_CLOUD_GUIDE.md

JARVIS_COMPUTER_PAIRING_GUIDE.md

JARVIS_TROUBLESHOOTING.md

Include exact instructions for:

- APK installation
- Android permissions
- Google Cloud project setup
- OAuth client setup
- SHA certificate fingerprints
- API key setup
- Gmail authorization
- AI provider configuration
- Android controls
- Wallpaper editing
- QR scanning
- Windows companion startup
- Generating a QR invitation
- Approving pairing tickets
- Testing computer connectivity
- Recovering from connection errors

Use actual project paths and values.

Do not invent credentials, ports, package names, or signing information.

---

# 42. APK BUILD AND DELIVERY

Build one final Android APK.

Preserve update compatibility.

Verify the actual application ID and signing certificate.

Do not delete user data.

After building:

- Confirm the APK exists.
- Verify application metadata.
- Compute SHA-256.
- Report actual file size.
- Record version information.
- Run relevant tests.

If authorized publication is available, upload the APK to GitHub Releases or another approved artifact destination.

Return the real verified download URL.

Do not invent an APK link.

Do not claim a release exists unless publication actually succeeded.

Build and package the compatible Windows companion when appropriate.

---

# 43. FINAL IMPLEMENTATION REPORT

Provide:

A. Repository audit

B. Existing bug root causes

C. QR scanner repair results

D. Pairing protocol test results

E. Windows companion verification

F. Google Cloud configuration

G. Google OAuth verification

H. Gmail connection results

I. AI provider results

J. Android system control results

K. Wallpaper results

L. Voice and microphone results

M. Silent Mode results

N. Deep Shutdown results

O. Performance measurements

P. APK build details

Q. Actual download link

R. Exact installation instructions

S. Remaining setup requirements

T. Known limitations

Use statuses:

IMPLEMENTED

AUTOMATED TEST PASSED

LIVE API VERIFIED

PHYSICAL DEVICE VERIFIED

USER ACTION REQUIRED

BLOCKED

UNSUPPORTED

Never label something physically verified unless a physical test was performed.

---

# 44. FINAL ENGINEERING DIRECTIVE

The application already has an impressive holographic design.

Now turn it into an assistant with equally impressive real capabilities.

Preserve all requirements from JARVIS 5.0 and 5.1.

Repair the QR scanner as a foundational feature.

Make scanning, request submission, desktop approval, and connection verification work as one complete workflow.

Fix Google account authorization and real Gmail data.

Fix AI tool calling.

Fix Android application launching.

Preserve advanced wallpaper customization.

Preserve 500+ farewells.

Fix microphone shutdown.

Preserve Silent Mode and Deep Shutdown.

Preserve up to 20 authorized computer connections.

Provide complete setup documentation.

Build one stable APK.

Provide a real verified APK download link after successful authorized publication.

**THE MOST IMPORTANT ACCEPTANCE STANDARD:**

If a user taps Scan Computer's One-Time QR, they must get a working scanner.

If they scan a valid invitation, Jarvis must process it.

If they request pairing, the Windows companion must receive a real pending ticket.

If they approve the ticket, Jarvis must complete the authenticated pairing.

If the system fails anywhere, the user must receive the actual reason.

NO FAKE CONNECTIONS.

NO FAKE EMAILS.

NO FAKE PAIRING.

NO FAKE SUCCESS MESSAGES.

NO FAKE DOWNLOAD LINKS.

START WITH THE QR SCANNER AND PAIRING FAILURE, THEN COMPLETE THE REMAINING INTEGRATIONS AND RELEASE WORK.