# OpenAI API setup and real verification

Use the **AI Brain Manager → OpenAI API** card. This is separate from **Continue with ChatGPT**: an API key with API project credits uses the OpenAI API, while optional official ChatGPT authorization uses a different account grant and usage limit.

1. Unlock Jarvis and enter the personal API key in its masked private field. Never send the key, an authorization header, a password or a token in chat or diagnostic exports.
2. Choose **Discover, test & select default**. This explicit action retrieves the key's actual model catalog, chooses one returned text candidate, stores the key with Android Keystore protection, sends a small real inference test that can consume API credits, and selects it only after a completed, nonempty response succeeds. It never tries a different model after a failed inference.
3. Alternatively, use **Refresh Models / Retry**, select a returned candidate, then **Test / Retry & Save Connection** and **Set as Primary Brain**. A listed model is a candidate, not proof of inference access. If the catalog is unavailable, the existing exact model field remains available for a known compatible model; no fabricated catalog is substituted.
4. Read **Connection Details** for the requested and resolved model, current verification state, previous verification time, last test duration, error category, HTTP status and retry availability. An Android-validated internet connection does not establish provider reachability. Direct personal API access displays **Backend: Not required**; no server deployment is implied.
5. After success, ask an ordinary question, then a related follow-up. Confirm that the displayed answer and spoken reply are the actual model output. The voice preview tests audio separately; it cannot verify OpenAI. Voice diagnostics distinguish a completed inference from synthesis readiness, playback start and playback completion.

The existing UI, native tools, privacy rules and half-duplex microphone ownership are retained. Streaming deltas are provisional text only. A completed response is required before inference verification and normal answer delivery; a broken stream must produce a useful error rather than speak partial text as a completed answer. Existing optional Local fallback remains opt-in and now discloses its local source in both the displayed and spoken answer.

## Failure handling

| Result | Action |
| --- | --- |
| Authentication failed / HTTP 401 | Replace or reconnect the key in the private API card; check its project and revocation status. |
| Authorization failed / HTTP 403 | Review project permissions and model access. |
| Insufficient quota | Review **API** billing/project credits; a ChatGPT subscription does not supply API credits. |
| Model unavailable / HTTP 404 | Refresh the actual catalog and explicitly test another text model. |
| Request rejected / HTTP 400 | Review the exact selected model and API compatibility. The repair uses Responses for modern text models and retains legacy Chat Completions for older GPT-3.5/GPT-4 models. |
| Rate limited / HTTP 429 | Respect the provider's retry delay. Inference POSTs are not automatically replayed. |
| DNS/TLS/network failure | Check Android connectivity, device clock, DNS and network certificates. TLS checks remain enabled. |
| Timeout/incomplete/invalid response | No response is verified. Retry explicitly after checking connectivity and the model. |
| Previously verified | The last result belongs to a prior app session. Run an explicit test or a real question to establish current-session access. |

Only safe model-catalog GETs can retry automatically: at most two retries, bounded backoff with jitter, and no automatic retries for rejected credentials, permissions, unavailable models or insufficient quota. Long Retry-After values are left for an explicit later action. There is no automatic paid-provider fallback.

## Key security and backend status

This project already supports direct personal, user-supplied API keys and has no OpenAI gateway. The key remains encrypted at rest using the existing Android Keystore-backed store. It is transmitted only to the provider over HTTPS and is absent from APK source/configuration, logs and connection proofs. The model response uses `store: false`; existing ephemeral conversation privacy is unchanged. This does **not** offer the isolation of a key held exclusively by a separately authenticated server. A shared service credential must not be bundled into a distributed APK; a future managed service requires its own backend authentication, hosting and server-held secrets.

No backend was deployed by this repair. No backend URL, fabricated health result or extra dependency is required for the existing personal-key path.

## Device acceptance still required

This workspace cannot access the API key saved on your phone or your Redmi 9. Ownership of an API key does not expose it to the build environment. The cloud's mock tests do not authenticate to OpenAI or play physical audio.

**LIVE PROVIDER VERIFICATION NOT PERFORMED** unless separately recorded with real runtime credentials. Verify the real catalog, selected model, completed answer and audible speech using the steps above. If it fails, share only the model ID, error category and HTTP status from diagnostics; do not share secrets or private conversation text.

Update the existing Phoenix installation using the retained package and certificate. Do not uninstall, clear storage or change the package to work around an installation rejection. Static signing/version compatibility with the previous published APK is separate from an actual handset update test. The original production signing identity remains a separate unresolved release requirement.
