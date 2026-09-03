# Open-source transplant plan

LifeOS V2 will prefer proven permissively licensed implementation over clean-room rewrites when the donor module has a clear boundary and fits the V2 invariants.

## Rules

- Preserve upstream copyright/license notices for copied or substantially adapted Apache-2.0/MIT code.
- Record exact upstream repository, path, and source commit for every transplant.
- Do not copy GPL/AGPL implementation into the APK core unless the distribution strategy explicitly accepts those obligations.
- Keep LifeOS interfaces source-neutral. Donor code sits behind LifeOS contracts rather than defining the product domain.
- Raw evidence remains immutable; semantic interpretation remains replayable; write actions remain approval-gated and idempotent.
- Port the smallest coherent working subsystem, including relevant upstream tests, rather than isolated snippets.

## Audit status

| Donor | License | Candidate subsystem | Decision |
|---|---|---|---|
| adgapar/teya | Apache-2.0 | durable memory, Android harness/runtime patterns, tools | PORT/ADAPT |
| samarthshrivas/Agentra | MIT stated by upstream README; verify license text at source revision before copying | Accessibility action executor/planner/hierarchy | AUDIT FIRST |
| openintelligence-labs/secondbrain | MIT | retrieval/context/memory pipeline | AUDIT |
| lukeybaer/secondbrain-os | MIT | agent checkpoints/reflection/idempotency/decision logs | AUDIT |
| nbramia/LifeOS | GPL-3.0 | life-model/connectors | ARCHITECTURE REFERENCE ONLY |
| flagdizero/jenny-android-ai-agent | AGPL-3.0 | persistent-agent patterns | ARCHITECTURE REFERENCE ONLY |

## First transplant target: Teya memory

Upstream source reviewed: `adgapar/teya/app/src/main/kotlin/com/teya/agent/household/MemoryManager.kt`.

Useful proven behavior:

- append-only durable memory entries;
- subject-scoped persona association;
- semantic retrieval with keyword fallback;
- recall reinforcement;
- HOT/COLD memory tiers;
- category-specific decay;
- episodic pruning;
- consolidation-oriented episodic access;
- duplicate/similarity guard.

### LifeOS adaptation

Do not copy Teya's household-specific domain (`Member`, contact roster, persona block) as our domain model. Port the algorithms behind a LifeOS-neutral `MemoryRepository` using canonical entity IDs and V2 evidence provenance.

Every durable memory created from LifeOS observations must retain source assertion/evidence IDs. This is stricter than the donor implementation and preserves V2 grounding.

## Second target: Android Action Engine

Agentra's `ActionExecutor.kt` demonstrates a coherent AccessibilityService implementation for tap, double-tap, long-press, swipe, drag, scroll, typing, global/system actions and app launch without shell touch injection.

Before direct code import, verify the actual license file/text for the exact source revision. Until then, its implementation is reference-only despite the upstream README advertising MIT.

When imported, all mutating actions must sit behind LifeOS `ActionPolicy`, explicit approval, and persistent idempotency ledger. No donor executor may bypass that boundary.
