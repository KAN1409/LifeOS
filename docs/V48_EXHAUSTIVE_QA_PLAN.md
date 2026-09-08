# LifeOS v48 — Exhaustive QA contract

This pass treats testing as a product capability, not a smoke checklist. The objective is to test every declared feature, every important user state, every durable data path, every Android integration boundary, and the architecture choices that can silently become correctness, privacy, scalability or reliability problems.

## What “complete” means here

Literal enumeration of every possible sequence of taps, messages and device states is combinatorially impossible. LifeOS therefore uses five complementary strategies:

1. **Complete feature inventory** — every manifest Activity/component and every registered product capability is machine-accounted for.
2. **State matrix** — empty/populated, permission denied/granted/revoked, online/offline where meaningful, success/failure/retry, fresh install/update, active/resolved/handled, foreground/background/restart.
3. **End-to-end user journeys** — real visible controls are clicked on disposable emulators and verified against the same durable repositories the product UI uses.
4. **Invariant/property testing** — stable IDs, count consistency, exact evidence, search round-trip, SQLite integrity, no resolved-object resurrection, source preservation, no fabricated capability truth.
5. **Exploration/stress** — large datasets, Monkey/random UI events, process recreation, API-level matrix, performance/log/crash/ANR diagnostics.

A PASS is not granted because a screen opens. A feature passes only when its source, persistence, reload, UI, action/lifecycle and failure state behave coherently.

## A. Build and release identity
- Debug and release unit tests.
- Debug/release APK assembly.
- Android Lint.
- Manifest/package/version verification.
- Permanent signer verification on the distributable APK.
- APK v2/v3 signature verification.
- ZIP integrity and native 16-KB alignment.
- In-place update identity; never require uninstall.

## B. Complete Android component inventory
- Every declared Activity has explicit instrumentation coverage, including invalid/missing-object states.
- Exported service/receiver boundaries are permission-protected.
- Notification Listener and Accessibility setup states are reported explicitly.
- Teya service/in-call boundaries are inventoried separately from ordinary product UI.

## C. User journeys
- Now -> Timeline / Search / Ask navigation.
- Needs Attention browse -> stable detail -> handled flow (destructive only on disposable emulator; physical audit is read-only).
- Search query/filter -> real provider result -> same object detail.
- Quick capture: Voice / File / Place / Decision entry points.
- Project create -> search -> detail -> complete -> reopen.
- Decision record -> search -> detail.
- File picker dispatch -> persisted URI -> reopen.
- Voice source -> durable WAV -> playback/transcript state/retry.
- Place permission and save path.
- People/Calendar denied, granted and revoked states.
- Back navigation and missing-object/error states.

## D. Semantic truth
- Multilingual English/Arabic/Franco/emoji regression corpus.
- Previous context cannot manufacture a current obligation.
- Exact evidence span is mandatory for person obligations.
- Reactions, promos, content-ready, app/system and completed-state noise never becomes canonical attention.
- Direct requests/questions can surface.
- Resolution is durable and cannot be reopened by replay of older evidence.
- Now / Commitments / Ask read the same canonical obligation source.

## E. Durable data and databases
- SQLite `quick_check` on every app database.
- SQLite `foreign_key_check` where applicable.
- App-private database location.
- Stable ID reloads for every typed provider.
- Orphan evidence and missing source files/URIs reported.
- Source audio/image/file evidence survives derived-processing failure.
- Database version changes require explicit migration coverage.

## F. Provider round trips
- Conversations.
- Commitments.
- Decisions.
- Voice Memories.
- Files.
- Projects.
- Places.
- People.
- Events.
- Experimental Image/OCR store, without promoting Images into normal product UI before its quality gate.

For each applicable provider: create/import or observe source -> persist -> list -> count -> search -> stable reload -> detail -> lifecycle/action -> restart/reload.

## G. Permissions and Android integration
- Notifications.
- Microphone.
- Contacts.
- Calendar.
- Fine/coarse location.
- Notification Listener.
- Accessibility.
- Persisted document permission.
- Permission revoked after previous success.
- Missing provider/app/action target must fail gracefully.

## H. OCR quality
- Source-image persistence.
- Orientation/size/preprocessing pipeline.
- Arabic-only, English-only and mixed samples.
- Critical token accuracy: money, dates, times, phones, URLs/emails.
- CER/WER benchmark and minimum 30 ground-truth samples before product promotion.
- OCR engine failure must not remove the original image.

## I. Voice quality
- Permission denied/granted.
- Record/stop/source WAV validity.
- Very short recording.
- Reopen/playback.
- Arabic, English and mixed Arabic/English transcription.
- Offline/transcription failure -> source retained -> retry.
- Search/focused Ask uses only the stored verbatim transcript.

## J. Scalability and performance
- Large typed-object datasets expose silent result/count/search limits.
- Cold/warm launch timings captured.
- Memory (`dumpsys meminfo`) and rendering (`gfxinfo`) artifacts captured.
- SQLite sizes and integrity captured.
- Semantic backlog and observation/meaning compression reported.
- No crash/ANR/native-fatal during deterministic instrumentation or Monkey exploration.

## K. Compatibility matrix
At minimum:
- API 26 — declared minSdk.
- API 30 — scoped-storage era.
- API 33 — notification-permission boundary.
- API 35 — targetSdk environment.
- API 36 — Android 16 / current physical-device generation.

## L. Security/privacy architecture review
- Backups disabled.
- Base cleartext blocked; localhost exception only where explicitly required.
- No hard-coded API keys/tokens.
- Exported components permission-gated.
- Powerful permissions reviewed for least privilege.
- App databases remain private.
- Approval-gated write/action boundary preserved.
- Sensitive source text should not be silently logged or copied into diagnostic output without the audit explicitly being user-triggered.

## M. Architecture-quality review
Automated source audit records recommendations rather than hiding them behind a green build. Examples:
- swallowed exceptions / broad `catch(Throwable)` usage;
- hard-coded provider scan limits that can break counts/search/reload on large histories;
- SQLite helpers without future migration bodies;
- overly broad permission surface;
- O(N) federated search where an indexed provider-native search would scale better;
- main-thread work or lifecycle-unbound background work that can cause UI stalls.

## Physical-device acceptance
CI/emulators cannot prove Samsung-specific background policy, real Notification Listener/Accessibility streams, real Contacts/Calendar data, microphone acoustics, location fixes, retained production databases, or real mixed-language ASR/OCR quality. The in-app **Full LifeOS QA** remains non-destructive and exports the retained-device evidence ZIP for those checks.

The final product verdict therefore has two separate statuses:

- **Automated QA status** — build/unit/static/API-matrix/instrumentation/stress.
- **Physical retained-data status** — one-tap device audit + real source quality checks.

Neither status may be represented as complete until its own evidence exists.
