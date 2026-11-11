# Combined-prompt progress and acceptance matrix

Status is scoped: source implemented is not physical-device verification. This is a development release milestone, not completion of every sentence of the master specification. Final build counts/signature are in the adjacent release verification.

| Task / source issue | Implementation and evidence | Software check | Real-device status | Status / remaining limit |
| --- | --- | --- | --- | --- |
| Video 6158 reproduction | Video6158_Timeline.md | File availability audit | NOT RUN | BLOCKED: recording not supplied; no timestamps invented |
| One wake / combined suffix | WakePhraseMatcher, existing coordinator and ManagedMicrophone | 100 logical wake /30 follow-up /20 suffix loops | NOT RUN | IMPLEMENTED; acoustic rates/latency not measured |
| Optional wake phrase | VoicePreferences + Voice health; reserved/recipe conflict checks | Pure phrase/guard cases | NOT RUN | IMPLEMENTED text matching; not acoustic retraining |
| Recognition failure categories | Existing valid Vosk N-best parser and stage-specific failures preserved | Entire prior suite retained | NOT RUN | IMPLEMENTED; physical input/Bluetooth/OEM acceptance pending |
| Local identity/time zone/math | LocalKnowledge, LocalArithmetic, LocalVoiceQueries | Actual bounded parser/clock regressions | NOT RUN | IMPLEMENTED offline; broad factual questions still need configured brain/backend |
| Local brain | Existing optional licensed/checksummed installers retained | Existing installation policies | NOT RUN | Existing smaller/near-1GB choices retained; new ~2GB pack/benchmarks NOT IMPLEMENTED |
| Same core/layout | Existing Canvas/Compose/WallpaperService; shared blue speaking and true charging predicate | Palette/charging policy regressions | NOT RUN | IMPLEMENTED source repair; no handset screenshot/FPS comparison available |
| Actual center battery | Existing Android battery repository preserved | Existing telemetry suite | NOT RUN | No fabricated 100% reading; charger/hardware acceptance pending |
| System Doctor probes | Typed HealthProbe registry across actual metadata, explicit companion check | 9 repair/diagnosis policy tests | NOT RUN | IMPLEMENTED scoped probes; unavailable measurements labeled unknown |
| Approved safe repair | Encrypted last-known config, budget/cooldown, readback and rollback | Admission/readback/rollback policies | NOT RUN | Configuration verified only, never claims acoustic repair |
| General OS/third-party repair | Settings handoffs and truthful permission feedback | Existing OS policies | NOT RUN | Automatic permission grant, OS repair and arbitrary self-modification excluded |
| Teach phrases and recipes | Encrypted LearningStore + review UI + shared typed plan execution | 16 tests incl 20 aliases, isolation, conflict, undo/import | NOT RUN | IMPLEMENTED exact approved aliases; uncertain prose opens review |
| Voice teaching without punctuation | English/Spanish draft boundary parsing | Vosk-style transcript cases | NOT RUN | Preview only; no automatic save or inferred unsupported step |
| Vocabulary/corrections | Explicit editable vocabulary notes + app/recipe aliases | Storage/privacy cases | NOT RUN | No weight training; broad fuzzy/embedding semantic generalization NOT IMPLEMENTED |
| Learning account isolation | Explicit local-profile namespaces tested | A/B injected-store isolation | NOT RUN | App uses one installation profile; Google multi-account UI/sync NOT IMPLEMENTED |
| Predictive inputs | Separate opt-ins: actual fresh battery, explicit zoned routines, read-only authorized Android Calendar | 14 policy/codec cases | NOT RUN | IMPLEMENTED while visible process is active; no background/exact-delivery promise |
| Anti-spam/controls | One strip; quiet/DND/music/power/lock guards, cap, dedupe, dismiss/snooze/never/opt-out | Clock/DST/source/permission/cap cases | NOT RUN | Generic voice announcements only if explicitly selected |
| Suggestion action safety | Accept returns proposal; shared plan review and current recipe/device checks | Shared registry and policy cases | NOT RUN | No automatically executed external action |
| Mission readiness and progress | Existing pair reused; authenticated nonce-bound manifest, friendly name, real job stages | 12 JVM policy tests + 36 companion Python tests | Linux local fixture only | IMPLEMENTED single selected paired device; status never substitutes for receipt |
| Verified desktop Chrome/search | Signed typed request/receipt, actual loaded document/status check, replay/idempotence/revoke/cancel | Real HTTPS + isolated Chromium, real404, duplicate once | No phone/Google/native Windows/macOS round trip | IMPLEMENTED scoped browser actions |
| Saved multi-step missions | Max6 typed steps; recipe/profile/session/expiry recheck before each step; stop on unverified | Registry/device/session/security cases | NOT RUN | IMPLEMENTED for supported steps; native app dispatch may remain unverified |
| Android selected-app launch | Existing visible app registry + approved launch, optional existing Screen-access package-only postcondition | Guards and prior app tests | NOT RUN | Without observed package: DISPATCHED_UNVERIFIED, not fake success |
| Desktop app/file/workspace expansion | Capability manifest explicitly reports unavailable actions | Negative capability tests | NOT RUN | Non-Chrome apps, authorized folder access/file transfer, VSCode/folder workspaces NOT IMPLEMENTED |
| Native target distributions | Existing manual Windows/macOS/Linux packaging recipe + updated source bundle | Linux protocol/browser verified | Native installers NOT RUN | No EXE/macOS installer/iOS claim; actual supported source instructions supplied |
| Unified action flow | PolishActionRegistry, metadata journal, current authorization and verified per-step result | Root contract and per-engine tests | NOT RUN | New engines share execution; prior trusted workflows retained |
| 2000-question/6000-answer new library | No such new dataset attached | Existing earlier corpus untouched | N/A | NOT IMPORTED; no invented evaluation or training counts |
| Performance/screenshots/crash rate | Performance_Baseline.md and existing crash metadata probes | Build/test evidence only | NOT MEASURED | Acoustic p95, FPS, RAM/thermal/battery and real crash rate need hardware |

Existing capabilities remain: local/optional cloud brain, charging/check-ins, calls, owner voice and pickup settings, widgets, private content, saved Fish voices, chat cleanup and approved summaries. Gmail stays removed. The removed standalone fifth core module was not added.
