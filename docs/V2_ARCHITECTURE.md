# LifeOS V2 Architecture

LifeOS V2 evolves the existing Android application; it is not a rewrite.

## Invariants

1. Raw observations are immutable evidence and remain separate from interpretations.
2. Capture is source-agnostic. WhatsApp is one adapter, not the domain model.
3. Interpretations retain evidence IDs and can be replayed/rebuilt.
4. Existing LifeOS behavior remains operational while V2 runs in parallel until proven better.
5. Action execution is downstream of understanding and requires an explicit policy/approval boundary.
6. Inferred identities are source-scoped. Cross-source identity requires explicit canonical evidence; matching display names alone are never sufficient.
7. Semantic Life Model state is a projection over evidence-backed revisions; newer interpretations never erase historical evidence.

## Pipeline

```text
Android sources
  -> ObservationAdapter
  -> RawObservation
  -> durable observation store
  -> normalization / reconciliation
  -> ContextEvent
  -> Episode
  -> Situation
  -> Life Model (temporal facts + current projection)
  -> Deep Brain / decision engine
  -> proposed action
  -> policy + approval
  -> Android Action Engine
  -> outcome observation
```

## Donor strategy

- Teya: Android runtime/sensor patterns. Prefer Apache-2.0-compatible transplantation with attribution.
- Open Intelligence SecondBrain: capture -> store -> retrieve -> temporal-context architecture.
- Agentra: Android accessibility/action planning and execution patterns (MIT).
- SecondBrain OS: agent loop, reflection, checkpoint, idempotency, and decision-log patterns (MIT).
- nbramia/LifeOS: life-model and connector architecture. Treat GPL code as architecture reference unless distribution strategy explicitly accepts GPL obligations.

## Migration phases

### V2.1 Universal Observation Core — implemented in shadow mode
Source-neutral observation contracts and durable persistence run beside the existing M1 engine. Notifications and accessibility are adapted without removing legacy paths. Accessibility V2 capture is app-agnostic; the old supported-messaging-app list now gates only the legacy parser.

### V2.2 Context Engine — implemented foundation
Current implementation includes deterministic ordering/replay, source-scoped entity resolution, explicit canonical cross-source entity linking, independent time-bounded episodes for interleaved streams, conservative cross-source situation grouping, evidence provenance, confidence, and rebuildable `LifeContextSnapshot` output from the universal store.

Current conservative rules:
- episode boundary: same stream, maximum 15-minute inactivity gap;
- interleaved app/stream activity does not split another stream's episode;
- situation boundary: maximum six hours and at least one shared non-application entity;
- app identity alone never merges situations;
- matching names across different sources never merge unless a canonical entity ID is supplied.

### V2.3 Life Model — implemented foundation
`LifeFact` provides evidence-backed semantic revisions with subject, predicate, value, assertion/retraction state, observation time, validity interval, confidence, and evidence IDs. `LifeModelAssembler` consumes only explicit normalized `life_fact_*` attributes; it never reparses WhatsApp/UI/source text. It retains the complete fact history and derives `currentFacts` as a rebuildable projection, so later revisions/retractions do not destroy history. The universal store can rebuild context and the Life Model from the same retained evidence set.

Next V2.3 work is semantic extraction/enrichment that can emit normalized facts for people, projects, commitments, open loops, appointments and relationships without source-specific patches.

### V2.4 Deep Brain
Situation ranking, options, decisions, reflection, checkpoints, and idempotent agent execution.

### V2.5 Action Engine
Accessibility-driven structured Android actions behind policy and approval gates.

## Non-goal

Do not add source-specific text/visual patches to make individual WhatsApp layouts appear correct. Fix the generalized observation/context algorithm instead.
