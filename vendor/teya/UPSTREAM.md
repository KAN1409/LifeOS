# Teya vendored subsystem

Upstream: https://github.com/adgapar/teya
Pinned source revision audited for this import: `b503872835ff3cafa8c399c4275e3e43c37d6577`
License: Apache License 2.0 (upstream `LICENSE`).

LifeOS consumes Teya in two exact-source forms:

1. Durable-memory sources are mirrored under `vendor/teya/upstream/` with their original package names and source text.
2. Larger agent-core/provider sources are fetched from the pinned upstream revision by `syncTeyaAgentCore` during the build. Every fetched file is verified against its expected Git blob SHA before it is admitted to the Kotlin source set. This keeps large upstream files exact without maintaining hand-copied derivatives.

Current imported subsystem boundary:
- `com.teya.agent.household.MemoryManager`
- household Room entities and DAOs required by MemoryManager
- `Member` used for persona-memory context assembly
- `com.teya.agent.safety.TeyaDatabase` and the contact schema it requires
- provider-agnostic agent contract: `BrainClient`, `ChatMessage`, `BrainResponse`, `ToolCall`
- Teya persona and complete provider-agnostic tool schema: `TeyaPersona`, `ToolSpec`, `AgentTools`
- upstream Mistral provider stack: `KtorClientFactory`, `MistralClient`, `MistralModels`, `MistralVoices`

LifeOS-specific adaptation must live outside the upstream source boundary so donor code remains diffable and updateable. Do not silently edit imported upstream files. If an upstream patch is unavoidable, document it in `PATCHES.md` and keep the patch minimal.
