> Historical code15 candidate record. The owner subsequently requested Gmail removal and publication of a speech-repaired beta. See [the current release scope](CONVERSATION-UPDATE.md) and [current download/verification](../../downloads/rebirth-11.2-beta2/README.md). Gmail integration is absent from the current build.

# Google Gmail setup for the 11.2 validation candidate

The owner confirmed that Google Cloud registration is not set up yet. Source compilation does not create that registration or grant access to a mailbox. Keep passwords, OAuth access tokens and Fish API keys out of chat, source control, APK assets and screenshots.

These steps are for the app developer's Google Cloud project. A future customer should authorize the registered app through Google's own screen; customers should not have to create their own developer projects. Google can require restricted-scope verification before public distribution. This is not commercial approval.

1. Open [Google Cloud Console](https://console.cloud.google.com/) in your browser and sign in privately. Use the project selector at the top to select your Jarvis project or choose **New project** and create one.
2. Open **APIs & Services → Library**, search **Gmail API**, open Google's Gmail API entry and click **Enable** for that project.
3. Open **Google Auth Platform** (some layouts expose this through **APIs & Services → OAuth consent screen**). Complete **Branding/Get started** with the real app name and your own support/contact details. Google handles these details; do not send credentials to Codex.
4. In **Audience**, choose the audience appropriate to the actual project. For an external development test, leave publishing in **Testing** and add only your own authorized Gmail test accounts under **Test users**. Internal organization access works only for the matching Workspace organization.
5. In **Data Access / Scopes**, add the minimum Gmail scope: `https://www.googleapis.com/auth/gmail.readonly`. This candidate does not need Gmail send/modify permissions, Google Drive or Google Calendar permissions for its mail feature.
6. Open **Clients** (or **Credentials → Create credentials → OAuth client ID**), choose **Android**, and enter the package **`com.jarvis.next`**.
7. Enter this **public SHA-1 certificate fingerprint** for the retained reinstall signing line:

   `5F:8A:64:DE:13:21:BC:CC:6B:53:B2:55:53:4C:2E:70:5A:2C:47:71`

   Save the Android OAuth client. Its public certificate SHA-256 is `1094ecc1e01dfb873a8e63689b09dc156fff9d41f77ffdfae27e702991711bda`. An Android OAuth registration is bound to package **and signing certificate**. A debug APK or original Jarvis7.0 has a different certificate and needs a separate legitimate test registration. No private signing key should be uploaded to Google or pasted into chat.
8. The official Google authorization SDK discovers the registered Android app; this integration embeds no web client secret and does not use a custom password form. Allow time for Google configuration to become available. If consent returns a developer/configuration error, check project, package, certificate, API enablement, scope and test-user membership before retrying.
9. On an **authorized compatible test installation**, open **Jarvis Settings → Rebirth · accounts, personal content & listening diagnostics → Google Account Center · Gmail read-only**. Select **Add Google account / reconnect**, authenticate with Android when requested, choose the intended Google account in Android's account picker, and review Google's own scope-consent screen. Grant only the intended read-only permission.
10. A connection is reported as verified only after the official Gmail profile endpoint returns the chosen account. Select **Authorize this mailbox for this session** before querying it. The read session lasts at most two minutes and ends on lock or Stop. Voice profile similarity is never account authentication.
11. Say **“Read my latest email.”** Confirm sender, subject and date against your actual Gmail inbox. **“Read it”** retrieves a bounded inline-text excerpt; attachments are not fetched automatically. **“Summarize it”** uses the optional installed local model on retrieved content. Without a local model, the app explicitly returns an excerpt instead of inventing a summary. Nothing is written to normal persistent chat history.
12. To speak private email text through Fish, separately review the private-text consent in **Settings → Output voice**. With that consent off, private replies remain text-only. The configured English/Spanish Fish voice is never replaced by Android or OpenAI speech.
13. Test **Disconnect and request token revocation**. Verify both local removal and the stated remote-revocation outcome. If remote revocation is unverified or the network was unavailable, inspect/remove the app's access in your Google Account's security/third-party connections page. Do not interpret local removal as proof that Google revoked a grant.

Do not uninstall or clear the original Jarvis7.0 to perform this test. Its original private signing key is still unavailable, so this retained-reinstall candidate cannot update that installation in place. Use a legitimately compatible test installation/device, or recover the original signing key through the previously described private backup path. The source work does not bypass Android signature or account protections.

Release remains held under the user's directive until the core/device and real-account gates in the final report are addressed. No mailbox success, authorization grant or audible Fish result has been verified in this cloud environment.
