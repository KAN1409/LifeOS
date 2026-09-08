# LifeOS v45 — Obligation Truth & Lifecycle checklist

This pass is a semantic-trust pass. UI presence is not evidence that an obligation is real.

## A. Exact-evidence promotion
- [x] Add `evidence_span` to semantic model output.
- [x] Persist `evidence_span` separately from summary/reason.
- [x] PERSON_CONVERSATION attention requires a verbatim span from the exact target evidence.
- [x] Previous conversation context cannot supply the evidence span.
- [x] Conversation attention receives an independent structural corroboration check after model output.
- [x] High model confidence alone cannot promote ordinary conversation to canonical attention.
- [x] Platform reactions / completed UI activity / promotions remain blocked.
- [x] Add regression: `I think the best day is 15` cannot become attention just because prior context contained a question.
- [x] Add regression: a direct grounded request such as `Please send me the car details` remains eligible.
- [ ] Device proof: ordinary alenushka conversational messages no longer surface as commitments.

## B. Obligation identity
- [x] Remove `conversation stream == obligation` identity.
- [x] A conversation is now only a container for evidence.
- [x] Independently grounded targets remain separate obligation objects until a stronger semantic identity relation exists.
- [x] Stable obligation IDs are based on exact obligation evidence, not the whole stream.
- [ ] Future: evidence-backed semantic dedup of multiple messages that truly represent one obligation.
- [ ] Device proof: alenushka no longer appears as one 20-evidence mega-obligation.

## C. Lifecycle reconciliation
- [x] Add `resolves_observation_id` to semantic model output and durable meaning store.
- [x] The model may point only to one exact older observation supplied in previous context.
- [x] Resolution is accepted only for the same stream and an older evidence item.
- [x] Resolution closes the exact prior attention/open-loop evidence, never the whole conversation.
- [x] RESOLVED attention state cannot be reopened by re-analysis of the same old evidence.
- [ ] Device proof: explicit later completion/cancellation evidence closes the corresponding earlier obligation.
- [ ] Device proof: Waleed Car Wash does not remain open after an explicit completion/verification message that resolves the request.

## D. Historical v44 reconciliation
- [x] v44 person-conversation meanings without `evidence_span` are not trusted as v45 canonical truth.
- [x] Their attention records are retracted as false/legacy promotions, not marked handled.
- [x] Preserved raw observations are requeued once through the v45 semantic contract.
- [x] Valid historical requests can re-enter only after passing the new exact-evidence gate.
- [ ] Device proof after in-place update: old alenushka explosion is removed automatically without deleting LifeOS data.

## E. Product consistency
- [x] Now, Commitments, Search and Ask continue to read the same `ObligationRepository`.
- [x] Ask remains downstream of canonical truth; no Ask-specific masking of false commitments.
- [ ] Device proof: Now / Commitments / Ask show the same final open count.
- [ ] Device proof: every open commitment detail has narrow evidence that actually supports that exact obligation.

## F. Release gates
- [x] Android unit tests pass on final v45 head.
- [x] Release APK assembles in CI.
- [x] Artifact downloaded and inspected.
- [x] Permanent signer applied.
- [x] APK Signature Scheme v2 verified.
- [x] APK Signature Scheme v3 verified.
- [x] Native libraries verified for 16KB alignment (35/35).
- [x] `com.kareem.lifeos` and versionCode 45 confirmed as in-place update over v44.
- [x] Final APK SHA-256 recorded: `67f124e9060d9bafc2b38a85b4ec26e06b1e0dbae723b6389e07cfdfdc0851af`.
- [x] PR remains draft/open/unmerged.

## G. Device acceptance
- [ ] Install v45 directly over v44; no uninstall/data reset.
- [ ] Let historical semantic re-analysis finish.
- [ ] Capture Now screenshot.
- [ ] Capture Commitments screenshot.
- [ ] Open each remaining commitment and inspect its exact evidence.
- [ ] Verify alenushka informational/reaction evidence is absent from canonical commitments.
- [ ] Verify a genuine direct request still appears.
- [ ] Verify a later explicit resolution closes only its linked obligation.
- [ ] Ask `What needs my attention?` and verify it matches the same canonical objects.

## DONE definition

`target raw evidence -> verbatim evidence span -> independent corroboration -> canonical obligation identity -> explicit lifecycle updates -> one canonical count -> device proof`

A model label, confidence score, conversation membership, or plausible summary is never enough by itself.
