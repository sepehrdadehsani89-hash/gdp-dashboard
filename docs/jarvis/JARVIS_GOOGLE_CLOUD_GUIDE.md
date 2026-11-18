# Jarvis 5.2 Google Cloud, API keys and OAuth

Owner status at this checkpoint: **no Google Cloud Android OAuth project/client configured yet**. Live account authorization and data reads require owner action on the actual phone. Cloud tests use controlled HTTP fixtures and do not establish live Google access.

## Register this application's real identity

Create/select an owner-controlled project in [Google Cloud Console](https://console.cloud.google.com/). Configure Google Auth Platform branding, audience/test users and data access. Enable only the APIs you will use. Create an **Android OAuth client** for:

- Package: `com.jarvis.next.phoenix`
- SHA-1: `64:71:1C:61:B2:AC:55:D4:78:E5:F9:22:80:F5:40:61:36:9E:E3:1B`
- Certificate SHA-256: `f9f13af1f84515b787b89c0f3abc8695f1d0d45e8a647d1192966c4c810964c7`

The configuration screen also reads the **actually installed APK's** signing SHA-1 using Android PackageManager. Match that value exactly when registering a different authorized build. A future production package/Play signing identity needs its own Android client. Do not generate a replacement signing key to make an update install.

Start with Gmail API. Calendar API, Drive API, Docs API, Sheets API, Slides API, Tasks API, People API, YouTube Data API v3, Meet REST API, Chat API, Forms API and Classroom API are independent services and scopes. See the exact per-service definitions in [`GoogleServicesPolicy.kt`](../../app/src/main/java/com/jarvis/assistant/google/GoogleServicesPolicy.kt). Workspace administration, account eligibility, API quotas and Google's verification rules can restrict access.

For External/Testing consent add your real account as a test user. Gmail read and some Drive scopes are sensitive/restricted. Public distribution may need verified ownership, consent review, a privacy policy and additional assessments. Do not bypass an unverified-app or organization-policy restriction.

## Saved configuration and optional public API key

Open Settings → Connections → **Google Cloud Configuration**. Save the actual Project ID and Android OAuth Client ID as registration metadata. Save an optional authorized API key if you intend to use a permitted public API operation. Values are encrypted in the existing Android Keystore-backed feature vault.

Google AuthorizationClient resolves the native OAuth registration using the app's package and signing certificate. The saved client-ID field documents your setup; it does not override that registration or implement a Web-client credential exchange. Jarvis does not treat the Project ID as verified project ownership or automatically set a billing/quota project.

An API key does not authorize private Gmail, Drive, Calendar, Photos or other account data. The **Test saved API key** action performs one bounded real public YouTube language-metadata request. Enable YouTube Data API v3 in that key's project and apply appropriate key/API/application restrictions. The request uses the actual Android package/certificate headers where supported. Public API success is displayed separately from private account verification. It consumes the service's normal quota; there is no automatic retry loop.

Do not put a Google password, cookie, OAuth access/refresh token, service-account key or client secret in these fields or in chat. The native flow does not require an embedded client secret or a `google-services.json` file. A Google/Gemini AI-provider API key and a private Gmail OAuth grant are separate configurations.

## Authorize and test a private service

1. Open **Google Connected Services / AI & Connected Accounts**.
2. Use Android's real Google account chooser. Selecting an email is not verification.
3. Choose Gmail and **Authorize / test real read**. Review Google's genuine requested scope.
4. A real Gmail profile request must return the chosen account's email before previews are accepted. Only a successful read establishes the current Gmail proof.
5. Test “Check my Gmail,” “Summarize my inbox,” “How many unread emails do I have?” or teacher/IMPORTANT-label queries after configuring the actual teacher sender address. Native voice results are bounded actual previews/counts, not invented messages or an automatic cloud summary.
6. Authorize Calendar and Drive separately and perform their actual read tests. Docs/Sheets/Slides/Forms need an explicit accessible file ID and matching resource response. Sheets reads only the bounded supported range.
7. An authorized read can be explicitly indexed as a bounded local project snapshot. It is account-separated and dated; signing in does not index your full history or upload an index to an AI.

Google Play services owns the renewable grants. Jarvis stores encrypted connection metadata and keeps access tokens transient. Saved authorization and historical success remain distinct from current process read proof. Account switching/removal, grant changes, lock and failure invalidate acceptance of stale results.

## Optional AI summaries and wallpaper data

After a real read, **Review an AI summary of this result** shows the exact selected provider/model/mode and the bounded excerpt size. Approve that one transfer and complete normal Android authentication where configured. A review binds the Google account, grant epoch, source digest, selected provider/model and provider credential. A change or expiration requires a new review. Local Brain keeps its approved request on-device; a cloud provider receives the approved excerpt.

This action uses no general conversation history, saved-memory retrieval, tools, automatic cloud fallback or automatic speech. The answer is a private **AI draft** and can be cleared; it is not a new Google API fact or a remote action. Ordinary Google reads never automatically send mail or document contents to an AI provider.

Private wallpaper widgets require a separate profile opt-in. Gmail unread count comes only from the actual UNREAD-label response, not Android notification counts. Calendar/Tasks snapshots are from recent successful authorized reads. They are RAM-only, account/grant-bound, marked with the source time and expire after five minutes. Lock, quiet mode, revoked scope or a different account hides/unverifies the data. Notification previews use their separate Android source permissions.

## Gmail push infrastructure remains a separate requirement

No Gmail `watch` subscription, Pub/Sub topic, history-sync cursor or deployed backend has been created. There is no owner Google project/backend credential here. A real deployment would need an owner-approved authenticated backend, a correctly permissioned Pub/Sub topic, verified push delivery, watch renewal, account-bound history synchronization and handling of expired history IDs. Saving a Project ID/API key does not create that infrastructure.

This build updates notification-based widgets from actual Android notification events and Google read widgets from actual explicit successful reads. These are distinct sources. No automatic cloud polling or fictional new-email event is substituted for Gmail push. Automatic per-email API cards and background semantic mail summaries are not claimed.

## Revocation and product boundaries

Disable a service locally to stop Jarvis voice access. Remove locally to delete its Jarvis metadata/snapshots without deleting the Google account or cloud files. **Disconnect and request revoke** also asks Google to revoke; offline revocation must be reported unconfirmed. Check [Google third-party connections](https://myaccount.google.com/connections) when needed. Removing only Cloud configuration does not revoke existing service grants.

Photos uses an official picker/app boundary; private Maps history and consumer Keep API access are unsupported. Meet/Chat/Classroom have account/API eligibility constraints and bounded supported endpoints; spaces are not complete message histories and courses are not grades/coursework. No universal private-account access is promised.

For status 10 check package/SHA-1/client/consent setup. For 401 reconnect; for 403 check scope/API/administrator policy; for 429 inspect quota; for NETWORK/TIMEOUT check connectivity. No failure creates a green connection state. Official references: [Android authorization](https://developer.android.com/identity/authorization), [OAuth policy](https://developers.google.com/identity/protocols/oauth2/policies), [Gmail UNREAD label](https://developers.google.com/workspace/gmail/api/reference/rest/v1/users.labels/get), [Gmail push](https://developers.google.com/workspace/gmail/api/guides/push).
