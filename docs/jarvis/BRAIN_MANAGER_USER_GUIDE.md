# AI Brain Manager preview

This preview adds Settings → Artificial Intelligence → AI Brain Manager to the existing application. The main voice pipeline and holographic interface remain in place. Live account and phone validation are still required; the cloud test suites use mock servers and temporary files.

## ChatGPT

Choose **Continue with ChatGPT**, review the plan-usage acknowledgement, and complete the official OpenAI browser flow. If the provider offers Google account selection, choose it inside that official flow. Return to Jarvis after consent. Account/workspace eligibility and actual granted permissions come from OpenAI.

An identity-only login is not AI Connected. Load the account's available models, choose one, and use **Test Connection**. This sends a small real inference request using authorized ChatGPT plan usage. Only a completed model answer enables **Set as Primary Brain**. The first successful browser return can run that small test automatically. No chats are imported and no paid API fallback is automatic. **Manage ChatGPT usage** opens the provider's official usage page.

Add an account/workspace using its own browser authorization. Switching registrations invalidates prior account/model verification and clears transient voice context. A healthy refresh with the same granted scopes retains the verification binding. Verification has a one-hour lifetime; repeat the real test if it expires.

**Disconnect ChatGPT** removes local credentials immediately and attempts official refresh-token revocation. A remote failure is reported and does not restore the credentials; review the app connection in ChatGPT settings to confirm revocation.

## Claude and existing API routes

Claude requires a personal supported Anthropic Console API credential. Consumer claude.ai sign-in is not reused. Enter the key privately, choose a compatible Claude model, then **Test & Save API Connection**. The test stores the key using existing Android Keystore-backed encryption and makes one small request against that API account. Review the account's API credits/billing. Identity subscriptions and API permission are distinct.

OpenAI API is an explicit separate card/mode. Existing Gemini support is retained. A model list or saved key alone does not enable a new primary brain. Successful exact-provider/model/credential inference is required. Disconnecting API access removes the local key for that provider; disable the key in its provider console if you also want it revoked elsewhere.

## Optional local model

Cloud brains do not require downloaded LLM weights. **Install / Repair Local Brain** shows the actual official descriptor, revision, SHA-256, byte size, partial download and free-space estimate before confirmation. Download completion and checksum verification do not select the model. **Test Local Inference** must load that model and produce an actual native answer before **Set as Primary Brain** becomes available.

Removal asks for confirmation, cancels and joins managed download/inference work, releases native resources and removes only allowlisted managed LLM weights, partials and integrity records. It can remove the selected model or all downloaded managed LLMs. Voice/wake/STT/TTS/OCR assets, accounts, settings, databases and benchmark records remain. Bundled runtime/APK bytes cannot be removed independently. A removed selected Local Brain remains unavailable until reinstalled and tested; no cloud provider is selected silently.

## Existing voice and installation

The selected brain feeds the existing assistant response and Fish Audio playback/caption path. Brain authorization does not configure microphone permissions, wake/STT models or Fish Audio credentials. Those remain separate in the existing voice settings.

The preview uses `com.jarvis.next.phoenix` / **Jarvis Phoenix Test**, alongside the original production package. With the retained preview certificate and higher version code it is intended to update the previous Phoenix/voice preview, preserving that installation's data. The production signing key is unavailable here, so this is not a production-signed update and does not migrate the original package's data. Do not uninstall either installation to troubleshoot without first considering its local data.
