# Optional service setup and evidence boundaries

The beta contains real integrations; configured account access and live hardware tests are separate prerequisites. No provider credentials or weights are bundled. Settings → Neural Core exposes the private music, location, vision and voice-profile screens. The matching module panels also open their screens.

## Music and Spotify

Enter an AudD API token privately on the music screen. Review its terms and ten-second upload consent. A real request returns a match or no match; an account/HTTP failure never becomes a fake match. Captured samples stay in device RAM and are zeroed after use; AudD's external retention terms still apply. Previously enabled wake listening may resume after capture.

For optional Spotify library saving, register a Spotify public/native client. Configure the exact redirect `com.jarvis.next://spotify-callback`, enter its public client ID, then authorize in the external browser. No client secret belongs in this APK. PKCE S256, single-use random state and a five-minute pending authorization bind callbacks. Saving uses the real Spotify API and checks library membership afterward. Spotify app access restrictions, current API availability, user permissions and production/commercial approval are not bypassed. No real Spotify/AudD account was available for end-to-end verification in this environment.

Artwork is downloaded only by explicit request from approved HTTPS Spotify CDN hosts. Lyrics open a licensed-source website search. The meaning action uses only returned title/artist metadata with the selected AI and may be uncertain; it is not a lyrics retrieval service.

## Location

Grant approximate or precise location only when requested. The shared repository requires a real, sufficiently recent Android fix. Address lookup uses Android Geocoder and may share coordinates with its configured provider. A separately consented real OpenStreetMap embed shares the chosen area/marker and IP with OSM/tile servers. North-up is not a device compass heading. Destinations require explicit names and saves and use the encrypted private feature vault. No continuous tracking or automatic location history is added.

## Vision

Choose one or two images or explicitly hand a camera still to analysis. Local OCR uses the existing OCR path. Optional cloud description/comparison requires a configured Gemini, OpenAI or Anthropic vision-capable model and per-request consent. Resized JPEG copies strip source metadata before upload. Images/results are temporary and excluded from chat memory; image/provider text cannot invoke tools. Camera stills explicitly handed off to analysis remain in the app-private camera cache under the existing policy: retained files older than 24 hours are cleaned on the next capture. Clearing the vision preview does not delete a photo owned by the Android picker provider. No offline general vision model is claimed. Live provider and physical-camera behavior remain unverified.

## Speaker profiles

Download the optional 26,530,550-byte model on the private profile screen after reviewing the license/credit. See the APK asset `legal/omnisystem-notices.txt` for WeSpeaker CC BY 4.0 attribution, exact artifact URL/size/hash and separate runtime licensing. The model is not bundled. HTTPS download integrity is checked before native loading; no memory-admission gate is added.

Explicitly enroll one owner and up to five consenting guests using a real twenty-second sample; extra samples are optional. Encrypted normalized embeddings and consent metadata are saved, not raw recordings. Manual matching uses an explicit five-second sample. Conservative cosine/margin checks allow unknown/ambiguous results. Automatic hints are off by default: enabling them taps at most four seconds from the already-enabled wake recorder and adds no new background recorder. Possible identity hints expire, never unlock Android, grant authority, or change the persistent user name.

The host smoke used the real downloaded model and official Sherpa-ONNX Python 1.12.26 with a synthetic harmonic signal: finite 256-dimensional output, 6.706 seconds load and 0.213 seconds compute on the cloud host. The app compiles against its existing Android Sherpa-ONNX 1.12.15 API. This smoke is not Android native execution, a speaker accuracy test or a Redmi 9 performance result. Human enrollment, acoustic variation, replay resistance and Spanish/Persian performance are unverified. Voice matching is not secure authentication.

## Timers, exports and future updates

Jarvis-owned timers use elapsed time and Android exact alarms after notification/exact-alarm permission and explicit review. Restarting interrupts them; notification request is not proof of sound or delivery. Android settings/DND remain authoritative. External Clock intents require the user to review/save and do not supply a trusted external countdown or saved-alarm status. One-off dates are never silently converted into weekly repeats.

The legacy v1 private-memory export imports document text, approved memories, tasks and eligible app aliases only. It excludes conversations, settings, credentials, weights, signing keys, speaker templates, destinations and timer state. Those new features use additive namespaces; no original database migration/reset is introduced. Backup UX for the new private feature data is not implemented in this beta. Keep the installed original 7.0 and its data until migration preparation is complete.
