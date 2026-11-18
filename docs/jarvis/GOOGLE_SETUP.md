# Google account setup for this Jarvis preview

Owner status: no Google Cloud project / Android OAuth client is configured yet. Google login and live API reads are **USER ACTION REQUIRED**, not verified in this workspace. No Google password, OAuth client secret, API key, access token or refresh token should be posted in chat or committed.

## Register the installed application's identity

1. Open [Google Cloud Console](https://console.cloud.google.com/) using the account that will own this integration. Create or select a project.
2. Configure **Google Auth Platform** branding, audience and data access (the console may also call this the OAuth consent screen). Supply accurate app/support information. For personal testing, add your account as a test user when the audience is External / Testing. Workspace organizational policies can restrict access even after this setup.
3. Enable only the APIs you intend to test. Start with **Gmail API**. Calendar API, Drive API, Docs API, Sheets API, Slides API, Tasks API, People API, YouTube Data API v3, Meet REST API, Chat API, Forms API and Classroom API are independent services; enabling one does not enable the others.
4. Under Credentials / OAuth clients create an **Android** client for:

   - Package: `com.jarvis.next.phoenix`
   - Signing certificate SHA-1: `64:71:1C:61:B2:AC:55:D4:78:E5:F9:22:80:F5:40:61:36:9E:E3:1B`
   - Retained certificate SHA-256: `f9f13af1f84515b787b89c0f3abc8695f1d0d45e8a647d1192966c4c810964c7`

   The fingerprints were read with `apksigner verify --print-certs` from the previously published Phoenix APK, not generated for this integration. The production `com.jarvis.next` package and a future Google Play signing certificate require their own Android OAuth client. Do not substitute a different key to make an update install.
5. Add the particular read scope you will request to the project's consent configuration where required. The complete list is shown per service inside Jarvis and in `GoogleServicesPolicy.kt`. Gmail `gmail.readonly` and broad Drive read/metadata access can trigger Google's sensitive/restricted scope review requirements. Public distribution can require verification, an appropriate privacy policy and additional assessments. A personal test does not establish production approval. Follow any ownership or Play Console validation required by Google's console; Jarvis cannot bypass it.
6. Allow configuration changes to propagate. Do not create a Web OAuth secret for this native-only flow. No backend credential exchange, `google-services.json`, embedded client secret or service account is required by the implemented Android AuthorizationClient flow. Google's Android registration binds the package and signing certificate.

## Connect on the phone

1. Open Jarvis → Settings → AI → **AI & Connected Accounts · Google setup** (also available in AI Brain Manager). Unlock normally.
2. Choose **Choose / add a Google account using Android**. Android/Google, rather than a Jarvis password form, handles selection and any account sign-in.
3. Find Gmail and choose **Authorize / test real read**. Review Google's requested read permission. Refusing consent must leave Gmail unverified. If Google says the app is blocked, a test user is missing, an API is disabled or the client is misconfigured, correct that project setting; do not bypass the warning.
4. A real profile request must return the selected account's email before mail previews are displayed. A successful read gives **VERIFIED** for Gmail only. Saved authorization after a restart is displayed separately from current successful API verification.
5. Try “Catch me up on my emails,” “How many unread emails do I have?” or “Summarize today's messages.” The unread count comes from Gmail's UNREAD label. Previews show returned sender/subject/date/snippet; they are extracted locally and are not sent to an AI provider. Mail is not marked read, deleted or sent.
6. For “Did my teacher email me?” save the actual teacher sender address in the same screen. Jarvis does not infer the teacher's identity. For important mail, this version uses Gmail's actual IMPORTANT label, not an invented personal urgency score.
7. Authorize each other service individually. Docs / Sheets / Slides / Forms require an explicit accessible file ID. If file consent is needed, complete consent and then choose the file ID again; pending file requests are not replayed automatically. Sheets reads only the first sheet's `A1:T20` range. Results are bounded and do not cover your full history.

No live Google credentials are available in this cloud workspace. Public Google discovery requests from this workspace also returned proxy HTTP 403; those proxy responses are not evidence about your phone's Google account or OAuth grant. Phone authorization and actual successful data reads remain required.

## Project snapshots and privacy

After an authorized Drive / Docs / Sheets / Slides / Calendar / Tasks read, **Index this result locally** stores that bounded result in the encrypted project index. Nothing is indexed by merely signing in or reading. New snapshots replace the same service's previous snapshot for that account; this is not a complete multi-file archive. “Summarize my project documents” reports these saved extracts and their dates, clearly marked as potentially stale.

The project index is separate from Jarvis's general AI retrieval index and does not upload data to an LLM. Google tokens and mailboxes are not persisted in the connection metadata. Google Play services owns its renewable grants. Private results are cleared when the account screen stops, and Android's secure-window flag prevents ordinary screenshots of that screen.

Use **Delete indexed Google snapshots** to delete an account's snapshots. **Disable this service locally** prevents Jarvis voice reads for that service; it does not revoke Google's remote grant. **Remove locally only** removes saved Jarvis connection metadata and that account's local snapshots. **Disconnect and request revoke** also asks Google to revoke the configured grant. If network revocation is unconfirmed, use [Google Account third-party connections](https://myaccount.google.com/connections). None of these actions deletes the actual Google account, email, files or organizational project.

## Honest product boundaries

Photos uses the existing Android content picker; no unrestricted Google Photos library API is claimed. Maps opens a map application; it does not read private location history. Consumer Keep data access is not assumed. Chat lists accessible spaces, not message bodies. Meet lists eligible accessible conference records. Classroom lists courses, not grades or coursework. Google determines account/API/administrator eligibility, and failure is reported without fabricated content. There are no mail sending or remote document editing permissions in this implementation.

## Troubleshooting and required phone checks

- **SETUP / Google status 10:** check exact package, signing SHA-1, Android OAuth client, enabled API and consent/test user.
- **AUTHORIZATION / HTTP 403:** check granted service scope, enabled API, project verification and Workspace policy. This is not automatically an invalid password.
- **AUTHENTICATION / HTTP 401:** cached access is invalidated where possible; reconnect the service. No replacement account is selected automatically.
- **QUOTA / HTTP 429:** wait and inspect the project's quota. No automatic retry loop is started.
- **NETWORK / TIMEOUT:** verify connectivity and retry deliberately. Saved metadata remains.
- **ACCOUNT_MISMATCH / STALE:** no private result is accepted; choose the correct account and repeat.

The following 20 acceptance cases require an authorized phone/Google project and are **NOT RUN** here: (1) missing-project setup error; (2) correct Android client; (3) cancelled account selection; (4) denied Gmail scope; (5) successful profile identity match; (6) wrong-account rejection; (7) real unread count; (8) real sender/subject/date preview; (9) no mark-read side effect; (10) local-day query; (11) configured teacher sender; (12) IMPORTANT label; (13) app restart restoration; (14) revoked Google grant; (15) account switch during read; (16) lock during read; (17) independent Calendar consent; (18) explicit project indexing and deletion; (19) local disconnect offline; (20) confirmed remote revoke followed by denied access.

Official references: [Android authorization](https://developer.android.com/identity/authorization), [Google OAuth policies](https://developers.google.com/identity/protocols/oauth2/policies), [Gmail messages list](https://developers.google.com/workspace/gmail/api/reference/rest/v1/users.messages/list), [Gmail message metadata](https://developers.google.com/workspace/gmail/api/reference/rest/v1/users.messages/get), [Drive file metadata](https://developers.google.com/workspace/drive/api/reference/rest/v3/files/list), [Calendar events](https://developers.google.com/workspace/calendar/api/v3/reference/events/list), [Tasks](https://developers.google.com/workspace/tasks/reference/rest/v1/tasks/list), [People contacts](https://developers.google.com/people/api/rest/v1/people.connections/list).
