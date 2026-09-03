# Open Intelligence SecondBrain subsystem

Upstream: https://github.com/openintelligence-labs/secondbrain
Pinned revision: `a16e23cd2d5d839a3b3ec0afe886987d2da54fb1`
Upstream package: `secondbrain-ai` 0.3.2
License: MIT.

LifeOS uses the upstream Python subsystem as an intact service dependency rather than translating its Tantivy/LanceDB/Kuzu/search stack into Android/Java. The Android application is a sensor/client; the upstream SecondBrain process owns its native Python retrieval, indexing, knowledge-graph, reranking, MCP and HTTP implementation.

The pin is immutable. Update it only as an explicit upstream upgrade with tests. LifeOS-specific transport/adapters must live outside upstream code.

Key upstream subsystem boundaries retained intact:
- `src/secondbrain/search/` — hybrid search, KG filter, reranker
- `src/secondbrain/store/` — captures, OLTP, text index, vectors, visual index, temporal KG, encrypted storage/backup
- `src/secondbrain/api/` — HTTP and MCP surfaces
- `src/secondbrain/chunking.py` and `indexing.py`
- upstream daemon/runtime and package dependency graph
