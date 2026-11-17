# Validation scope

The JVM suites use generated signing keys, temporary owned-model files, in-memory provider policy fixtures, MockWebServer, and actual loopback sockets on the Linux host. They do not sign in to a real ChatGPT account, contact a paid provider with real credentials, execute a native GGUF model, exercise Android Keystore, or capture/play phone audio.

`DEVICE_AND_LIVE_CASES.csv` preserves the directive’s 30 end-to-end acceptance scenarios. All are blocked here by missing authorized accounts and/or Android hardware; none is included in the automated pass count. Existing Android rendering tests can be compiled here, but cannot be reported as executed. No emulator is installed.

Provider inference proof is written only after a nonblank answer from the matching selected model and credential. OAuth identity, granted scope metadata, model lists, installation, and matching weight hashes alone never issue inference proof. Real plan responses require `response.completed`; partial deltas and failed/incomplete streams are rejected.

No shared provider key, cloud project, Firebase identity, first-party OAuth registration, browser cookie or provider subscription is fabricated. ChatGPT eligibility/permissions are decided by OpenAI. Claude requires an explicitly supplied supported API credential; consumer claude.ai login is not reused.

The existing Fish Audio reply path, speech packages, main holographic interface and wallpaper code are retained. No local LLM weights or other large runtime dependencies are installed by this change.
