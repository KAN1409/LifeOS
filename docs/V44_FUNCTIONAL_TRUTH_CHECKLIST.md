# LifeOS v44 — Functional Truth implementation checklist

This checklist verifies **real backing capabilities**, not screen presence. A capability is not DONE because a card, count, route, keyword rule, or model prompt exists.

## A. Architecture truth gate
- [x] Add `V44_FUNCTIONAL_TRUTH_ARCHITECTURE.md` as the acceptance contract.
- [x] Add `CanonicalSemanticPolicy` as one product-level promotion gate.
- [x] Raw capture remains loss-minimizing and separate from promotion.
- [x] Confirmed attention can now be semantically retracted instead of remaining sticky forever.
- [x] Provisional attention is never canonical product truth.
- [x] Legacy generic entity detail no longer fabricates Related/Actions from copied strings.
- [x] Legacy capability registry no longer creates Files / Places / Projects from text heuristics.

## B. Canonical obligations
- [x] Add `ObligationRepository` as the single user-facing unresolved-obligation source.
- [x] Repository accepts only confirmed semantic attention with a real future action.
- [x] Reaction/platform/completed/promotion/content-ready/system activity is rejected before product promotion.
- [x] Historical false OPEN/PROVISIONAL attention is persistently retracted as `rejected`, not falsely marked handled.
- [x] Human-stream evidence is collapsed to one canonical obligation surface instead of dozens of message-level attention rows.
- [x] Obligation objects have stable IDs.
- [x] Obligation detail reloads by stable ID.
- [x] Obligation detail exposes real evidence events.
- [x] `Mark handled` closes all evidence records represented by that canonical obligation.
- [x] Now attention count comes only from `ObligationRepository`.
- [x] Ask attention suggestion uses the same canonical count.
- [x] Search Commitments capability uses the same canonical repository.
- [ ] Device proof: previous false `alenushka` reaction/open-item explosion is removed after v44 reconciliation.
- [ ] Device proof: Now / Ask / Commitments count agree.

## C. Canonical Timeline
- [x] Add `CanonicalTimelineRepository`.
- [x] Timeline consults durable `NotificationMeaningStore` per stream/time when available.
- [x] Central negative gate blocks platform/completed/promotion/content-ready/system noise.
- [x] Timeline fallback is restricted to meaningful semantic event classes.
- [x] Real Android Calendar events can join Timeline when calendar permission is granted.
- [x] Captured conversation rows deep-link to stable conversation objects.
- [x] Calendar rows deep-link to real calendar objects.
- [x] Other captured rows open provenance/evidence rather than a fabricated generic entity.
- [ ] Device proof: voucher/promo rows disappear.
- [ ] Device proof: generated-image-ready rows disappear.
- [ ] Device proof: reactions/platform metadata disappear.

## D. Real Conversations
- [x] Add `ConversationRepository` over persisted thread streams.
- [x] Conversation objects have stable deterministic IDs.
- [x] Conversation detail accepts `conversation_id` and reloads the backing persisted thread.
- [x] Conversation evidence remains traceable to captured events.
- [x] Conversation detail only claims an unresolved item when a canonical obligation exists for the same stream.
- [x] Open-conversation action remains a real Android/source action with an explicit fallback message when exact targeting is unavailable.
- [ ] Device proof on WhatsApp conversation.
- [ ] Device proof on at least one non-WhatsApp conversation source.

## E. Real People provider
- [x] Add `ContactPersonRepository` backed by Android Contacts.
- [x] People availability is permission-aware.
- [x] No guessed people count is shown when Contacts permission is absent.
- [x] Person objects have stable contact-backed IDs.
- [x] Person detail reloads from Android Contacts by object ID.
- [x] Contact phone/email values come from the real provider.
- [x] Call action uses Android dialer.
- [x] Message action uses Android SMS intent.
- [ ] Cross-source conversation linking is intentionally NOT claimed yet.
- [ ] Device proof with granted Contacts permission.

## F. Real Events provider
- [x] Add `CalendarEventRepository` backed by `CalendarContract.Instances`.
- [x] Events availability is permission-aware.
- [x] No guessed event count is shown when Calendar permission is absent.
- [x] Calendar event objects have stable event+instance IDs.
- [x] Event detail reloads from Calendar provider.
- [x] Event time/location/calendar source are real provider data.
- [x] `Open in Calendar` is a real Android action.
- [x] Now Upcoming count comes from real future calendar instances only.
- [ ] Device proof with granted Calendar permission.

## G. Real Decision Memory provider
- [x] Add `DecisionRepository` over the first-class `decisions` table.
- [x] The source is explicit user input recorded through LifeOS Decision Memory, not inferred notification text.
- [x] Decision objects have stable `decision:<id>` IDs.
- [x] Decisions appear in the operational capability registry with a real count.
- [x] Decision list/search results come from the typed decision store.
- [x] Decision detail reloads the same object by stable ID.
- [x] Detail shows the stored Context / Options / Choice / Expected consequences / Status when present.
- [x] Detail identifies its provenance as explicitly recorded LifeOS Decision Memory.
- [x] Ask may suggest a decision question only when an actual recorded decision exists.
- [ ] Automatic decision extraction from ambient evidence is intentionally NOT claimed.
- [ ] Per-decision external evidence links are intentionally NOT claimed for manually recorded decisions.
- [ ] Device proof: create/read/search one Decision Memory object end-to-end.

## H. Real Files provider
- [x] Add durable `files` objects in `lifeos_user_objects.db`.
- [x] File source is explicit Android Storage Access Framework document selection.
- [x] Persist document URI access when Android grants it.
- [x] Persist display name, MIME type, size, source and connected timestamp.
- [x] File IDs are stable hashes of the selected content URI.
- [x] Files count/list/search come from `FileRepository`, never filename regex over notifications.
- [x] File detail reloads by stable file ID.
- [x] `Open file` is a real Android `ACTION_VIEW` against the stored content URI.
- [x] Ask explicitly knows file metadata only and is forbidden from pretending file content was parsed.
- [ ] File-content parsing/indexing is intentionally NOT claimed yet.
- [ ] Device proof: connect, search and open at least one document.

## I. Real Places provider
- [x] Add durable typed `places` in `lifeos_user_objects.db`.
- [x] Place source is Android location evidence captured only after the user explicitly chooses Save current place.
- [x] Persist label, latitude, longitude, accuracy, provider and observed timestamp.
- [x] Places count/list/search come from `PlaceRepository`, not `office/cairo/zamalek` text matching.
- [x] Location permission state is explicit; saved places remain browsable if permission is later unavailable.
- [x] Place detail reloads by stable place ID.
- [x] `Open on map` is a real geo intent.
- [ ] Automatic visit/history inference is intentionally NOT claimed yet.
- [ ] Device proof: save current place, search it and open it on a map.

## J. Real Projects provider
- [x] Add durable typed `projects` in `lifeos_user_objects.db`.
- [x] Project source is explicit user creation, never a search result containing the word `project`.
- [x] Persist name, description, status and timestamps.
- [x] Project IDs are stable UUID-backed object IDs.
- [x] Project count/list/search come only from `ProjectRepository`.
- [x] Project detail reloads by stable ID.
- [x] Project lifecycle supports Active ↔ Completed state changes.
- [x] Project UI explicitly states that unlinked people/files/conversations are NOT automatically claimed as related.
- [ ] Evidence-backed project membership/link graph is intentionally NOT claimed yet.
- [ ] Device proof: create, search, open and complete/reopen one project.

## K. Search / capability discovery
- [x] Add `FunctionalCapabilityRegistry`.
- [x] Browse grid contains only capabilities with concrete backing providers.
- [x] Current real providers: People, Conversations, Files, Decisions, Places, Events, Projects, Commitments.
- [x] People/Events show `Set up` when permission is missing rather than a synthetic count.
- [x] Files / Places / Projects re-enter the grid only after receiving real repositories and real create/import/capture flows.
- [x] Add `FunctionalSearchEngine` federating only typed provider objects.
- [x] Search matching includes provider identity + real object fields, never raw capability keyword guesses over notifications.
- [x] Search results carry `capability_id + object_id`.
- [x] Search result navigation reloads the same backing object.
- [ ] Device proof: Search grid/counts/statuses match provider reality.

## L. Ask / grounding
- [x] Add `GroundedQueryEngine` over canonical product providers.
- [x] Ask no longer sums `AttentionStore.openCount + LifeDb.openLoopCount`.
- [x] Ask no longer generates Project/File/Place prompts from heuristic capability results.
- [x] Decision prompts come only from recorded Decision Memory objects.
- [x] File/Place/Project suggestions can appear only when a real persisted object exists.
- [x] Ask suggestions come only from canonical obligations, persisted conversations, typed user objects, real calendar events, real action proposals, or canonical timeline activity.
- [x] Ask prompt explicitly forbids inventing absent capabilities/relations/actions.
- [x] Ask prompt explicitly forbids pretending connected file contents were read when only URI/metadata is available.
- [x] Fallback response is built from canonical provider objects.
- [ ] Production V2 Life Model/Deep Brain transport still not the canonical Ask transport.
- [ ] Device proof for attention, conversation, decision, project/file/place and recent-timeline questions.

## M. Real actions
- [x] Ready-actions count still comes from `PersistentActionQueue` only.
- [x] Existing Teya approval/execution bridge remains approval-gated.
- [x] Person Call/Message actions use real Android intents.
- [x] Calendar Open action uses a real Android Calendar URI.
- [x] File Open action uses the persisted Android document URI.
- [x] Place Open action uses a real geo intent.
- [x] Project status action changes durable project state.
- [ ] General typed V2 Android `ActionExecutor` remains future work.
- [ ] Outcome-as-observation loop remains future work for general actions.

## N. Explicitly NOT claimed in v44
- [ ] Automatic file-content understanding/indexing.
- [ ] Automatic place/visit history inference.
- [ ] Automatic project membership/relationship inference.
- [ ] Automatic ambient decision extraction with evidence links.
- [ ] Cross-source People ↔ Conversation identity linking without explicit canonical evidence.
- [ ] Generic cross-domain relationship graph as a finished product feature.
- [ ] Full V2 Deep Brain transport as the canonical Ask/priority engine.
- [ ] General typed Android ActionExecutor with outcome-as-observation loop.

These unchecked items are **not failures hidden behind UI**. The primary product is required to state the narrower capability it actually has rather than imply these deeper layers already exist.

## O. Experimental OCR quality gate — NOT yet a normal Images capability
- [x] Add `V44_OCR_QUALITY_ARCHITECTURE.md` with an explicit promotion contract.
- [x] Add durable `ImageOcrStore`; source image metadata survives OCR failure and OCR engine replacement.
- [x] Add stable `image:<id>` objects for explicitly selected images.
- [x] Persist image URI/name/MIME/dimensions/source independently from OCR text.
- [x] Persist each OCR run separately with engine/status/raw text/search text/language/confidence/duration.
- [x] Raw OCR text is immutable per run; search normalization is a separate derived representation.
- [x] Add EXIF orientation handling and bounded image decoding.
- [x] Add multi-pass preprocessing: original / small-text upscale / grayscale-contrast / Otsu threshold.
- [x] Add multi-pass ML Kit Latin OCR with candidate scoring and selected-line bounding boxes.
- [x] Add multi-pass Arabic OCR using Tesseract `tessdata_best` Arabic model rather than Cortex's old fast single pass.
- [x] Add candidate agreement/consensus scoring instead of trusting one OCR pass.
- [x] Add separate critical-token extraction for amounts, dates, times, phone numbers, URLs and email addresses.
- [x] Critical numeric tokens are never silently corrected into a different value.
- [x] Add Arabic-aware search normalization without modifying displayed raw OCR.
- [x] Add ground-truth/correction store; user correction does not overwrite raw OCR.
- [x] Add CER/WER benchmark implementation.
- [x] Add `OCR quality lab` device UI for import → scan → inspect candidates → correct ground truth → measure.
- [x] Keep Images/OCR out of `FunctionalCapabilityRegistry`, normal Search and normal Ask until the quality gate passes.
- [ ] Collect at least 30 real ground-truth samples.
- [ ] Corpus includes representative Arabic-only screenshots/documents.
- [ ] Corpus includes representative mixed Arabic/English screenshots/documents.
- [ ] Aggregate CER ≤ 8% on the accepted validation corpus.
- [ ] Aggregate WER ≤ 18% on the accepted validation corpus.
- [ ] Separately review critical amount/date/phone-number errors before promotion.
- [ ] Implement an authorized secure cloud OCR gateway; no long-lived provider key in the Android APK.
- [ ] Benchmark Google document OCR vs Azure Read on the same ground-truth corpus if recovery/cloud OCR is required.
- [ ] Choose local/cloud routing policy from measured accuracy/latency evidence, not vendor preference.
- [ ] Only after passing the gate: register `Images` as an operational LifeOS capability.
- [ ] Only after passing the gate: federate OCR text into normal Search and Ask grounding.
- [ ] Only after passing the gate: expose Image in normal Quick Capture.
- [ ] Device proof: Arabic OCR quality substantially exceeds the old Cortex OCR on the benchmark corpus.

## P. Automated safety gates
- [x] Add `CanonicalSemanticPolicyTest` for reaction/information/request/provisional/promotion cases.
- [x] Add `OcrQualityTest` for Arabic normalization, critical tokens, digit equivalence and duplicate-line fusion.
- [ ] Android unit tests pass in CI on the final v44 head.
- [ ] Release APK assembles in CI on the final v44 head.
- [ ] CI artifact downloaded and inspected.
- [ ] Permanent signer applied.
- [ ] APK Signature Scheme v2 verified.
- [ ] APK Signature Scheme v3 verified.
- [ ] 16KB native alignment verified.
- [ ] Package/version confirmed as in-place update.
- [ ] Final APK SHA-256 recorded.
- [ ] PR remains draft/open/unmerged.

## Q. Device acceptance
- [ ] Install directly over v43 without uninstall.
- [ ] Now screenshot verified.
- [ ] Timeline screenshot verified.
- [ ] Search screenshot verified.
- [ ] Ask screenshot verified.
- [ ] Open one Commitment by stable ID and inspect evidence.
- [ ] Open one Conversation by stable ID and inspect evidence.
- [ ] Open one recorded Decision by stable ID and inspect its stored fields.
- [ ] Connect/open one File.
- [ ] Save/open one Place.
- [ ] Create/complete/reopen one Project.
- [ ] Open one Contact-backed Person and exercise Dial/Message path.
- [ ] Open one Calendar-backed Event and exercise Open in Calendar.
- [ ] Verify all visible counts agree across surfaces.
- [ ] Open OCR quality lab and import at least one Arabic-heavy image.
- [ ] Verify original image remains available when OCR fails or is re-run.
- [ ] Save exact ground truth and verify per-image CER/WER is calculated.

## Functional DONE definition

A checked capability must pass:

`real source -> durable typed object -> canonical ID -> list/count -> detail reload -> evidence/source -> supported action -> refreshed state`

A beautiful screen, passing navigation, model-generated description, string match, guessed count, or unbenchmarked OCR demo is never sufficient.
