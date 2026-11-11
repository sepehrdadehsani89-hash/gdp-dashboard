# Predictive Intelligence implementation

This is a real, local opt-in suggestion engine, integrated by the existing runtime with the shared action registry. It is not a habit-prediction model or an autonomous executor.

## Implemented

- New additive encrypted `FeatureVault` document `finish_predictive_v_one`, schema 1. Configuration, approved routines, suppression and aggregate counts are separate typed fields. Writes are read back before a saved state is reported. Unknown/corrupt storage disables this subsystem without overwriting existing data.
- Independent opt-ins for the engine, actual low-battery observations and Android calendar. Defaults are off. Device calendar additionally needs `READ_CALENDAR`; enabling consent does not imply the Android permission was granted.
- Real fresh Android battery <=10%, not charging, proposes opening battery saver settings. No battery value is invented or setting toggled automatically. Recovery/charging rearms the low-battery occurrence; dismissal cannot cause the same discharge observation to repeat continuously.
- Explicit dated and weekday-repeating routines, editable/deletable title, IANA zone, lead time and optional link to an existing approved learned recipe. A saved trigger is bound to its recipe ID. The current enabled recipe is re-resolved before acceptance; a deleted/reused trigger cannot silently execute another recipe.
- Exact dated local times reject DST gaps and ambiguous overlaps. Repeating schedules skip anomalous occurrences instead of guessing or notifying twice. Stale missed occurrences expire five minutes after the configured time.
- Separately authorized read-only calendar queries inspect at most the next 20 minutes, return at most five candidates, and run on IO with a cancellation signal/time bound. Titles remain in temporary local state; history stores identifiers and offer metadata, not event titles or event contents. No Google API, OAuth credential or calendar grant is invented.
- One current suggestion, source/explanation/freshness/expiry, no fabricated confidence percentage. A single strip is wired into the existing HUD; the existing core layout is not replaced. Silent/text modes use the strip; explicitly selected voice mode offers only a fixed generic invitation, without calendar/routine titles. The root reuses existing TTS and does not start a new microphone service.
- Unlocked visible app, idle session, quiet hours, Silent/DND, music, battery saver, minimum gap (15–240 minutes) and daily cap (1–6) gate delivery. Default gap is 30 minutes and cap is three. Device/local zone drives quiet hours; a routine's saved zone drives its occurrence.
- Not now, 30-minute snooze, never suggest this source, restore suppressed sources, edit/delete routine, and delete predictive data/opt out. Snooze still respects original expiry and limits; expired reminders are not resurrected. Full opt-out immediately cancels pending refresh and clears the offered suggestion. Lock/privacy handling calls `hide()` to cancel a query and clear private temporary state.
- Accepting an offer returns a typed proposal (`BATTERY_SETTINGS`, `REVIEW_RECIPE`, `REVIEW_CALENDAR`) to the shared registry. It does not execute an action itself and never reports execution success. Recipe/action preview and normal authorization still apply. Accepted/dismissed counts are explicitly described as aggregate interaction counts, not accuracy or execution-success scores.

## Integration API

`runtime.predictive` is `PredictiveEngine(context, features)`.

- `state: StateFlow<FutureSuggestion?>`
- `status: StateFlow<String>`
- `store.state: StateFlow<PredictiveProfile>`
- `refresh(power: PowerReading, idle: Boolean)`; root invokes from the existing runtime tick. Refresh is bounded/throttled and never creates a new foreground service.
- `dismiss(id)`, `snooze(id)`, `never(id)`, `accept(id): SuggestionAction?`, `disable()`, `hide()`, `reset()`.
- `SuggestionAction` has `kind`, `target`, `referenceId`. Root verifies a recipe ID/trigger match again, then opens its normal review flow. Calendar review opens the selected actual event, without editing it.
- `PredictiveActivity` belongs under the existing finish-polish/settings surface; app identity and existing UI remain.

## Automated tests and unverified behavior

Fourteen new JVM tests cover opt-out, real/fresh discharge observations, explicit routine and reminder-only behavior, time zones/overnight quiet boundaries, unavailable calendar consent, lock/busy/DND/music/power saver, deduplication/daily caps/clock rollback, snooze/never/expiry, DST gaps/overlaps, weekday recurrence, document codec/unknown schema and twenty independent routine occurrence IDs. These tests use injected clocks/typed observations; they are not phone sensor, provider or audible-output tests. Root records final executed results separately.

This subsystem operates while the existing app process and an eligible visible session are available. It does not promise background exact alarms, delivery after force-stop, or a notification while the UI is unavailable. Native calendar permission revocation/provider behavior, real battery transitions, voice delivery and OEM lifecycle behavior require physical acceptance. Calendar suggestions are limited to event review; no inferred personal habits, location trigger, unauthorized Google data, automatic message, purchase, file transfer or significant external action is implemented here.

Data is local to the current installed app profile. No multi-user/cloud account system is introduced or claimed. Recent occurrence history is bounded to 100 seen IDs, and source suppression is capped; explicit deletion remains available. No raw audio, private screen, personal conversation or model training data is collected by this engine.
