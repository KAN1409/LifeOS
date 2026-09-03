# Teya vendored subsystem

Upstream: https://github.com/adgapar/teya
Pinned source revision audited for this import: `b503872835ff3cafa8c399c4275e3e43c37d6577`
License: Apache License 2.0 (upstream `LICENSE`).

This directory is a source mirror of the coherent Teya durable-memory subsystem. Files under `vendor/teya/upstream/` are kept with their original package names and source text. LifeOS-specific adaptation must live outside the mirror so upstream code remains diffable and updateable.

Initial subsystem boundary:
- `com.teya.agent.household.MemoryManager`
- household Room entities and DAOs required by MemoryManager
- `Member` used for persona-memory context assembly
- `com.teya.agent.safety.TeyaDatabase` and the contact schema it requires

Do not silently edit vendored upstream files. If an upstream patch is unavoidable, document it in `PATCHES.md` and keep the patch minimal.
