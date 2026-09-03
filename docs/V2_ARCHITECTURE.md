# LifeOS V2 Architecture

LifeOS V2 evolves the existing Android application; it is not a rewrite.

## Invariants

1. Raw observations are immutable evidence and remain separate from interpretations.
2. Capture is source-agnostic. WhatsApp is one adapter, not the domain model.
3. Interpretations retain evidence IDs and can be replayed/rebuilt.
4. Existing LifeOS behavior remains operational while V2 runs in parallel until proven better.
5. Action execution is downstream of understanding and requires an explicit policy/approval boundary.

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
  -> Life Model (people, projects, commitments, relationships)
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

### V2.1 Universal Observation Core
Introduce source-neutral observation contracts and persistence beside the existing M1 engine. Adapt notifications and accessibility first without removing legacy paths.

### V2.2 Context Engine
Deduplication, normalization, entity resolution, episode construction, replay, provenance, and confidence.

### V2.3 Life Model
People, conversations, projects, commitments, open loops, relationships, situations, and temporal state.

### V2.4 Deep Brain
Situation ranking, options, decisions, reflection, checkpoints, and idempotent agent execution.

### V2.5 Action Engine
Accessibility-driven structured Android actions behind policy and approval gates.

## Non-goal

Do not add source-specific text/visual patches to make individual WhatsApp layouts appear correct. Fix the generalized observation/context algorithm instead.
