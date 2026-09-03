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

| Donor | License status | Candidate subsystem | Decision/status |
|---|---|---|---|
| adgapar/teya | Apache-2.0 verified from LICENSE | durable memory, Android harness/runtime patterns, tools | MEMORY PORT IMPLEMENTED |
| openintelligence-labs/secondbrain | MIT verified from LICENSE | hybrid retrieval/context/memory pipeline | RRF RETRIEVAL PORT IMPLEMENTED |
| samarthshrivas/Agentra | README advertises MIT; repository LICENSE text not currently resolvable | Accessibility action executor/planner/hierarchy | REFERENCE ONLY UNTIL LICENSE VERIFIED |
| lukeybaer/secondbrain-os | README states MIT; repository LICENSE file not currently resolvable | agent checkpoints/reflection/idempotency/decision logs | REFERENCE ONLY UNTIL LICENSE VERIFIED |
| nbramia/LifeOS | GPL-3.0 | life-model/connectors | ARCHITECTURE REFERENCE ONLY |
| flagdizero/jenny-android-ai-agent | AGPL-3.0 | persistent-agent patterns | ARCHITECTURE REFERENCE ONLY |

## Implemented transplant 1: Teya durable memory

Upstream revision: `adgapar/teya@b503872835ff3cafa8c399c4275e3e43c37d6577`

Primary donor file: `app/src/main/kotlin/com/teya/agent/household/MemoryManager.kt`.

LifeOS implementation:

- `memory/MemoryRecord.java`
- `memory/MemoryAlgorithms.java`
- `memory/PersistentLifeMemoryStore.java`
- `memory/LifeMemoryRepository.java`
- `memory/PersistentLifeMemoryRepository.java`
- `memory/LifeMemoryBridge.java`

Preserved/adapted working behavior:

- append-only durable memory;
- subject/canonical-entity association;
- vector similarity recall;
- recall reinforcement;
- HOT/COLD memory tiers;
- category-specific forgetting curves;
- episodic pruning;
- recent episodic access for future consolidation;
- similar-memory duplicate guard;
- guarded forget-by-substring behavior.

LifeOS-specific strengthening:

- every durable memory can retain `sourceAssertionId` and raw `evidenceIds`;
- Arabic/Unicode normalization is supported rather than an English-only text normalizer;
- semantic assertions must be grounded before entering durable memory;
- the storage implementation is hidden behind `LifeMemoryRepository` for testing/replacement.

Apache-2.0 attribution and the exact upstream revision are retained in `third_party/teya/LICENSE`.

## Implemented transplant 2: Open Intelligence SecondBrain hybrid retrieval

Upstream revision: `openintelligence-labs/secondbrain@a16e23cd2d5d839a3b3ec0afe886987d2da54fb1`

Donor file: `src/secondbrain/search/hybrid.py`.

LifeOS implementation: `retrieval/RrfFusion.java`.

The donor's Reciprocal Rank Fusion (`k=60`) has been ported so independently ranked lexical and dense results can be combined without pretending their raw scores share a scale. LifeOS durable-memory recall now uses RRF to combine keyword/exact retrieval with vector similarity.

MIT attribution and the exact upstream revision are retained in `third_party/openintelligence-secondbrain/LICENSE`.

## Next direct-code targets

1. Audit additional Open Intelligence SecondBrain modules whose MIT license is verified, especially capture deduplication, chunking and retrieval filtering/reranking, and port only the Android-relevant algorithms.
2. Resolve Agentra's missing/unresolvable repository license file before copying its Accessibility action executor. Its implementation remains a design reference until then.
3. Resolve SecondBrain OS's missing/unresolvable repository license file before copying agent-loop/idempotency code. README-declared MIT alone is not being treated as sufficient for direct code transplantation.
4. Continue using GPL/AGPL projects as architecture references unless the LifeOS distribution strategy explicitly changes.
