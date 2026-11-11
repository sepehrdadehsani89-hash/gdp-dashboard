# Neural Evolution — approved personal commands

The new `finishpolish.learning` package is an additive offline command-learning component. It stores reviewed phrase-to-typed-action bindings in the existing Android Keystore-backed FeatureVault, using a separate versioned encrypted document for each explicitly selected local profile. Existing app aliases, memories, private voice profiles, models and pairing credentials are unchanged.

## Implemented source

- `LearningPolicy.kt`: validated schema, exact multilingual normalization, typed action/target separation, reserved-command and alias collision detection, speech-teaching drafts, supported-step previews and import/export codecs.
- `LearningStore.kt`: opt-in matching, explicit approval before save, storage readback before success, recipe edits/deletion, bounded undo history with new revisions, explicit vocabulary notes, private export and reviewed import with matching disabled.
- `LearningActivity.kt`: the existing private Settings-style surface, app picker using actual launcher-visible apps, current paired-device selection, multiple aliases, multi-step recipe preview, authenticated save, plan-review Try, edit/delete/undo, vocabulary note controls, Android file-picker export/import and delete-all for this new learning profile.
- `LearningPolicyTest.kt`: twenty approved custom aliases; normalized collisions/reserved commands; opt-in/approval; edits/undo/revision invalidation; per-profile isolation; serialization/import; unsupported actions; target validation; unavailable storage; readback failure; draft-only teaching; vocabulary privacy; bounded history.

Root integration exposes `runtime.learning: LearningStore` and routes `runLearnedRecipe(id, voiced)` through the shared typed action registry, with a fresh preview/confirmation and current revision/device checks. Learning never directly dispatches a command from the UI or a stored text string. Imported recipes are disabled and require explicit re-review/enabling. Saving a phrase never executes it.

## Supported recipe actions

| Action | Bound target | Execution evidence required |
| --- | --- | --- |
| `LOCAL_APP_OPEN` | Reviewed installed app package ID | Root registry checks presence and dispatch; dispatch alone cannot claim foreground success |
| `DESKTOP_CHROME` | Current paired desktop UUID | Current pairing, explicit plan confirmation, verified loaded-page receipt |
| `DESKTOP_SEARCH` | Current paired desktop UUID plus reviewed query | Same pairing/confirmation and authenticated verified navigation receipt |

There are at most 50 recipes, 10 aliases per recipe and 6 steps per plan. A local profile also supports at most 100 approved vocabulary notes. Stored history is at most five snapshots and is trimmed further to stay within the encrypted document limit. Exports contain only the current approved profile, not undo snapshots, provider keys or pairing tokens. Exported JSON is plaintext in the user's explicitly selected file and can contain private search/trigger text; the UI explains this before export. Imported profiles are limited to 300 KB and validated before mutation. A corrupted or invalidated encrypted record fails closed and is not overwritten with an empty profile.

## Personalization boundaries

The user explicitly approves each phrase/target. Ambient speech, private conversation, files, screen contents and provider results are never collected as training data. Exact aliases produce deterministic intent bindings; foundation-model weights are not retrained. Alternative pronunciations must be supplied explicitly as additional aliases. A phrase that collides with a system command, normalized existing alias or configured custom wake phrase is rejected. The current runtime also rechecks approved profile and recipe revisions at execution, so old approval cannot execute an edited/undone plan.

Vocabulary notes preserve the user's approved term and meaning for review, correction and forgetting. They do not silently substitute arbitrary words into executable commands or alter the ASR model. To make an app nickname or actionable correction work, save a recipe bound to the real app. This intentionally distinguishes reviewed command interpretation from acoustic retraining.

`profileId` namespaces isolate explicit local profile IDs. The present Android app has no new Google-account selector or server account system; the UI uses the existing local installation profile. The profile-isolation software test does not claim verified remote multi-user account isolation. An imported recipe targeting a different desktop remains disabled and cannot silently choose another device.

## Remaining acceptance / limits

Broad desktop workspace apps/folders, arbitrary scripts, file transfers, messaging, contact mutation, fuzzy semantic execution and foundation-model retraining are not implemented by this engine. Unsupported targets must be reported by the shared executor. The UI can create a safe draft from “when I say coding time, open Chrome on my computer”; unsupported prose remains a draft requiring manual supported-step selection, never guessed executable code.

The twenty-alias regression measures deterministic exact-intent matching on approved software fixtures, not human pronunciation recognition or a wake/ASR accuracy rate. Real Android credential prompts, file pickers, encrypted persistence across actual process death, voice teaching, phone-to-desktop execution and hardware timing still require device acceptance. Tests are reported as passed only after the root executes the combined validation pipeline.
