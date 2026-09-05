# LifeOS v44 — Functional Truth Architecture

Status: architecture reset / acceptance contract. This document replaces the assumption that a visible feature is “implemented” merely because a screen, counter, card, route, heuristic query, or placeholder action exists.

## Core rule

**No UI promise without a real backing capability.**

A feature may appear as operational in the primary LifeOS UI only when all of the following are true:

1. **Real source** — the app has a concrete source of data or state for that capability.
2. **Durable model** — the capability has a persistent typed representation, not only text matching over generic events.
3. **Grounded query** — counts, lists, summaries and relationships are produced by the same typed store/repository.
4. **Real detail** — tapping an item opens actual data for that same object, not a generic page populated from copied title/summary strings.
5. **Real action contract** — any action shown in the UI has a real read/write implementation or is explicitly labelled unavailable/read-only.
6. **Lifecycle** — create/update/retract/resolve semantics are defined and testable.
7. **Single source of truth** — Now, Search, Ask, Timeline and detail pages agree on IDs, counts and states.
8. **Automated tests** — repository-level tests verify the capability using real persisted objects.
9. **Device proof** — the capability is exercised on-device end-to-end before it is marked complete.

If any requirement is missing, the feature is **PARTIAL** or **NOT OPERATIONAL** and must not masquerade as a finished capability.

---

## Audit of v43

### A. Raw capture — REAL foundation

**Status: REAL / usable foundation**

- Notification capture persists source-neutral `RawObservation` records in `lifeos_context_v2.db`.
- Accessibility capture also has a V2 observation adapter.
- Raw evidence is append-oriented and independently replayable.
- Source return addresses are persisted separately.

UI implication: Capture/health/status may truthfully report observation intake and source access.

### B. Context Engine — REAL foundation, not the primary UI source

**Status: REAL foundation / NOT fully wired to product UI**

- `LifeContextAssembler` can rebuild episodes and situations from raw observations.
- Entity resolution is source-scoped and conservative.
- `LifeContextSnapshot` is a rebuildable model.

Problem: the primary v43 UI still largely reads legacy `LifeDb`, `AttentionStore`, `SituationEngine`, and heuristic presentation layers instead of making the V2 context model the canonical product source.

### C. Semantic Life Model — SAFETY FOUNDATION ONLY

**Status: PARTIAL**

- `SemanticInterpreter`, `GroundedSemanticInterpreter`, `SemanticAssertion`, `LifeFact`, semantic replay, and `LifeModelAssembler` exist.
- Grounding validation exists.

Missing production requirement:

- There is no finished product semantic client wired as the canonical source-neutral enrichment path for all observations.
- The V2 architecture itself lists semantic enrichment as next work.
- Therefore the Life Model cannot yet be treated as the authoritative live product model.

### D. Deep Brain — CONTRACT / VALIDATOR, not a finished intelligence feature

**Status: PARTIAL / not product-complete**

- `DeepBrainClient` boundary and `GroundedDeepBrain` validation exist.
- Grounding checks for proposed priorities exist.

Missing production requirement:

- Authorized Deep Brain transport is not implemented as the canonical live reasoning path.
- Checkpoint/reflection/outcome flow is incomplete.

UI implication: LifeOS must not imply that a full Deep Brain continuously reasons over the complete life model until this transport and pipeline are real.

### E. Action Engine — SAFETY + HARNESS BRIDGE, not a complete general action system

**Status: PARTIAL**

- Proposal/approval/result persistence exists.
- Teya harness broadcasts can enter the persistent action queue.
- Approved items can be forwarded to the harness service.
- Persistent idempotency infrastructure exists in V2.

Missing production requirement:

- The V2 architecture explicitly states that a typed production Android `ActionExecutor` is still next work.
- The UI must distinguish a real supported harness action from a merely suggested generic action.

### F. Timeline — REAL capture, PARTIAL semantics

**Status: PARTIAL**

- Rows originate from captured persisted events.
- Day grouping, source icons and navigation are real.

Problems:

- semantic noise filtering produces false positives;
- promotions/content-ready/system/social metadata can still surface;
- the product timeline is therefore not yet a canonical meaningful-life-event stream.

Target: `CanonicalTimelineRepository` built from reconciled canonical events + semantic assertions, not generic event text heuristics.

### G. Attention / commitments — DURABLE, currently semantically unreliable

**Status: PARTIAL / currently unsafe to trust**

- `AttentionStore` and `open_loops` are durable.
- State survives process restarts.

Problems:

- false-positive message/reaction activity can become attention;
- confirmed/open state can remain sticky after later evidence says it is not attention;
- Now and Ask currently compute counts through different paths;
- multiple records can represent one real obligation.

Target: one canonical `ObligationRepository` with obligation-level IDs and explicit `OPEN / WAITING_ON_USER / WAITING_ON_OTHER / RESOLVED / RETRACTED` lifecycle.

### H. People — derived view, not yet a true people capability

**Status: PARTIAL**

Current implementation derives people from recent conversation labels.

Missing:

- persistent canonical person records;
- source identities linked through explicit evidence;
- stable person IDs across app restarts/source changes;
- relationship facts and verified related-object queries.

Target: `PersonRepository` backed by V2 `EntityRef`/canonical identity evidence. UI must navigate by `person_id`, never just copied display name.

### I. Conversations — closest capability to real

**Status: PARTIAL → candidate for first production provider**

- conversation-like events, thread keys and persisted source events exist;
- thread detail is based on real event data.

Missing:

- universal canonical thread model across sources;
- robust incoming/outgoing participant identity across all messaging apps;
- summary/state should come from semantic episode model rather than copied previews.

Target: `ConversationRepository` with stable conversation IDs, participants, events, summary and source links.

### J. Decisions — typed storage exists, discovery/extraction incomplete

**Status: PARTIAL**

- `decisions` is a first-class table with title/context/options/choice/consequences/status.

Missing:

- automatic grounded decision extraction/revision from semantic facts;
- source evidence links on each decision;
- explicit edit/confirm/retract flows in UI.

Target: keep this as a real capability only after each displayed decision has an ID + evidence + status.

### K. Files — NOT a real capability in v43

**Status: NOT OPERATIONAL**

Current v43 file counts/results are inferred by regex and words such as `attachment`, `document`, `.pdf`, `.xlsx`, etc. over generic events/search results.

There is no first-class file object/store used by the capability UI.

Target: `FileRepository` backed by actual attachment/download/file observations with stable URI/path/source metadata, MIME type, timestamps, provenance, accessibility state and open/share actions.

Until then: do not display a Files count as a finished capability.

### L. Places — NOT a real capability in v43

**Status: NOT OPERATIONAL**

Current place detection includes text matches such as `arrived at`, `location`, `office`, `cairo`, `zamalek`.

This is a heuristic search surface, not a place model.

Target: `PlaceRepository` with typed place observations, canonical place IDs, timestamped visits/context and provenance. If no permission/source exists, UI must show setup/unavailable rather than synthetic counts.

### M. Events — NOT a real unified events capability in v43

**Status: PARTIAL / misleading as a capability count**

Current counts use semantic kind plus text such as `meeting`, `appointment`, `calendar`.

Target: `EventRepository` combining real calendar read data + grounded event assertions from other sources, with stable event IDs, time ranges, status and evidence.

### N. Projects — NOT a real capability in v43

**Status: NOT OPERATIONAL**

Current project count is based on search results containing the word `project`.

Target: `ProjectRepository` with explicit project entities, aliases, linked people/files/conversations/decisions/obligations, membership provenance and confidence.

### O. Search — REAL engine pieces, misleading capability layer

**Status: PARTIAL**

- query UI, voice entry and local retrieval are real;
- `LifeIntelligenceEngine` searches persisted attention, conversations, decisions, events and memory.

Problems:

- results are mixed from heterogeneous stores without canonical object IDs;
- files/places/projects/events filters depend heavily on text heuristics;
- relevance is lexical/recency scoring, not a typed cross-domain search contract.

Target: Search becomes federation over real capability providers. Every result must contain `capability_id + object_id + evidence references`.

### P. Ask — real local model call, incomplete intelligence product

**Status: PARTIAL**

- composer, voice input, question history and local model generation are real.
- grounded context currently comes from `LifeIntelligenceEngine.search`.

Problems:

- grounding can include polluted attention/heuristic capability results;
- fallback answer is only a formatted retrieval list;
- Ask is not yet using the canonical V2 Life Model / Grounded Deep Brain path.

Target: `GroundedQueryEngine` pulls typed objects/facts/situations from canonical repositories, then uses the authorized model path. Every answer keeps object/evidence references internally.

### Q. Generic Entity Detail — presentation shell, not a universal real entity model

**Status: NOT acceptable as proof of feature completeness**

Current generic detail can be instantiated from copied `kind/title/summary/eventId` strings. `What matters` is produced by another title search, and Related chips are global capability links.

Target: every detail screen loads by stable object ID from its real repository. Sections are rendered only when real data exists.

---

## New production architecture

```text
ANDROID SOURCES
  NotificationListener / Accessibility / Calendar / Files / Location / explicit user input
          |
          v
UNIVERSAL OBSERVATION LAYER
  RawObservation + UniversalObservationStore
          |
          v
RECONCILIATION + CONTEXT
  canonical events + entities + episodes + source provenance
          |
          v
SEMANTIC ENRICHMENT
  grounded assertions, zero assertions when uncertain
          |
          v
CANONICAL DOMAIN REPOSITORIES
  PersonRepository
  ConversationRepository
  FileRepository
  EventRepository
  PlaceRepository
  ProjectRepository
  DecisionRepository
  ObligationRepository
  ActionRepository
          |
          v
LIFE MODEL / GRAPH
  facts + relations + situations + lifecycle
          |
          +-------------------+
          |                   |
          v                   v
GROUNDING / RETRIEVAL     PRIORITY / DEEP BRAIN
          |                   |
          +---------+---------+
                    v
             PRODUCT FACADE
      one canonical read contract for UI
                    |
        +-----------+-----------+-----------+
        v           v           v           v
       NOW       TIMELINE     SEARCH       ASK
```

---

## UI truth contract

### Now
May show only:
- canonical unresolved obligations/situations;
- canonical action proposals;
- real upcoming events;
- priority decisions grounded in those IDs.

Every card carries a stable object ID and opens the actual object.

### Timeline
May show only canonical meaningful events. Raw capture remains available as Evidence, not the default timeline.

### Search
Capability tiles are generated from registered **operational providers**, not hard-coded labels. A provider returns:

- availability state;
- real count;
- list query;
- object loader;
- supported actions;
- last successful sync/capture time.

If a provider is not operational, it is hidden from the normal Browse grid or shown explicitly as `Set up` — never with a guessed count.

### Ask
Suggestions must be generated only from canonical objects. The question context must be assembled from the same object IDs visible elsewhere in the UI.

### Detail
No generic copied-string entity page. Detail routes use typed stable IDs:

- `person:<id>`
- `conversation:<id>`
- `file:<id>`
- `event:<id>`
- `place:<id>`
- `project:<id>`
- `decision:<id>`
- `obligation:<id>`
- `situation:<id>`
- `action:<id>`

---

## Functional capability provider contract

Each visible capability must implement the equivalent of:

```text
id()
label()
availability()
count()
list(query, filter, limit)
load(objectId)
related(objectId)
evidence(objectId)
supportedActions(objectId)
health()
```

The UI is forbidden from fabricating a capability count by scanning arbitrary strings.

---

## Migration order

### Phase 0 — stop false promises
- [ ] Replace v43 “implemented” checklist with this functional truth contract.
- [ ] Mark Files / Places / Projects as not operational until providers exist.
- [ ] Stop Ask from summing incompatible attention/open-loop counts.
- [ ] Add canonical object IDs to UI navigation contracts.

### Phase 1 — canonical obligations and timeline
- [ ] Implement `ObligationRepository` and historical reconciliation/retraction.
- [ ] One real obligation = one canonical object regardless of number of messages.
- [ ] Implement `CanonicalTimelineRepository` with negative semantic gate.
- [ ] Make Now/Ask/Timeline use the same canonical state.

### Phase 2 — conversations and people
- [ ] Implement stable Conversation IDs from reconciled streams.
- [ ] Implement Person IDs from V2 source-scoped entities + explicit cross-source links.
- [ ] Replace name-based related/search navigation with object-ID navigation.

### Phase 3 — decisions and events
- [ ] Add evidence IDs and lifecycle to decisions.
- [ ] Wire actual Calendar read model into `EventRepository`.
- [ ] Merge calendar events with grounded semantic event assertions without duplication.

### Phase 4 — real files
- [ ] Capture real file/download/attachment references.
- [ ] Persist File objects with URI/MIME/source/provenance.
- [ ] Implement open/share/details actions.
- [ ] Only then expose Files as operational in Browse LifeOS.

### Phase 5 — places
- [ ] Define permission/source strategy.
- [ ] Persist Place + Visit objects with provenance.
- [ ] Only then expose Places as operational.

### Phase 6 — projects
- [ ] Add explicit Project entity + membership edges.
- [ ] Link files/people/conversations/decisions/obligations through evidence-backed relations.
- [ ] Do not create a project merely because text contains the word “project”.

### Phase 7 — Grounded Ask / Deep Brain
- [ ] Implement canonical semantic enrichment client.
- [ ] Implement authorized Deep Brain transport.
- [ ] Build GroundedQueryEngine over typed providers/life facts.
- [ ] Ask answers and suggestions reference canonical IDs.

### Phase 8 — typed actions
- [ ] Implement real typed Android ActionExecutor(s).
- [ ] UI shows only actions whose executor is available.
- [ ] Every write remains approval-gated and idempotent.
- [ ] Capture action outcome as new observation and verify result.

---

## Definition of functional done

A capability is **DONE** only when this full chain passes on a real device:

`source event -> durable observation -> canonical object -> UI list -> object detail -> related/evidence -> supported real action -> outcome -> refreshed canonical state`

A beautiful screen, a route, a model prompt, a guessed count, a keyword filter, a generic detail shell, or a passing compile test is **not** proof of a functional feature.
