# Connect Gemini on your phone

Jarvis 0.1.3 adds a short connection check with a visible result and 15-second deadline. A saved key alone does not mean Google accepted it or that the selected model can answer.

1. Install the 0.1.3 APK as an update to Jarvis. Keep the existing app installed so Android can preserve its settings, memories and downloaded models. The package and signing certificate are unchanged.
2. Open Jarvis → Settings → Intelligence, your way → Gemini.
3. Enter a Gemini API key created in [Google AI Studio](https://aistudio.google.com/apikey), or leave the field blank to keep the Gemini key already stored on the phone. Do not send your key in chat or screenshots.
4. Tap **Find Gemini models**. This asks Google's API for model identifiers that support `generateContent`. It does not send your conversations or phone data. The API may return a paginated catalog; this version requests up to 1,000 models from its first page. Listing a model does not guarantee generation quota or regional availability.
5. Select an exact identifier from the list. Jarvis uses Gemini's text-generation API, then reads its response with the selected voice. A Live-only model uses a different API and cannot be selected from this compatible-model list. Do not guess identifiers such as `gemini-3.8-live`.
6. Tap **Test connection**. A checking dialog shows elapsed time and a Cancel check button. The API request has a 15-second deadline. This sends one short sample prompt, without your conversations, memories, files, screen contents or notifications. Provider quota or charges may apply.
7. The result dialog must show **Connection verified**, the exact model, elapsed time and an actual sample reply. Tap **Connect & save** there. A failure is shown as **Connection not verified**; a stored key alone is not success. Return to Assistant and type **Hello** before testing voice commands. Prefer a listed Flash-Lite or Flash text model for lower latency; Pro/thinking models can be slower. Keep optional OpenAI cloud voice off for faster on-device speech.

## If the test fails

- **HTTP 404 / NOT_FOUND:** the chosen model cannot serve this request. Refresh the model list and select another supported text model.
- **HTTP 400 / INVALID_ARGUMENT:** Google rejected the request. Check the key, exact model identifier, and project configuration. This status alone does not identify the cause.
- **API key rejected:** create or check your key in Google AI Studio, enter it privately in Jarvis, test and save.
- **Key restrictions:** inspect the key's API and application restrictions in Google Cloud. The key needs access to the Generative Language API. This app calls the REST API with `x-goog-api-key`; it does not implement browser-referrer or Android application attestation headers. Do not assume a key made for a different application will work here.
- **FAILED_PRECONDITION:** inspect Google's account/project requirements, billing and supported regions. This code alone does not prove which requirement failed.
- **HTTP 429:** check the project's available quota, rate limits and billing tier. A Gemini consumer subscription does not automatically supply API quota.
- **Connection failure:** check that the phone can reach Google's Gemini API. Device permissions cannot resolve an API model, project or account error.

Jarvis displays fixed explanations and known HTTP/status codes instead of echoing Google's raw error text, which can contain sensitive details. If it still fails, share the displayed status/code and selected model identifier, with the API key hidden.

Validation uses a local mock provider. No live Gemini key or authenticated API access was available in the development environment, so a successful live connection on your phone remains to be verified.
