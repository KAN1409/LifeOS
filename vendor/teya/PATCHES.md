# LifeOS patches over Teya

Upstream remains pinned to `adgapar/teya@b503872835ff3cafa8c399c4275e3e43c37d6577` and every imported source blob is verified before modification.

## Approval gate patch

LifeOS applies one deterministic patch to the **generated build copy** of `HarnessService.kt` after verifying the upstream blob SHA `45d4b82ec8ec9310d7999ecc22f381bffe34a81c`.

Purpose: Teya upstream executes tool calls immediately. LifeOS requires explicit user approval before external/write actions.

The patch:

- leaves known read-only tools (`get_events`, `read_shopping_list`, `query_expenses`) on the upstream execution path;
- converts every other tool call into a package-scoped `TEYA_APPROVAL_REQUEST` broadcast containing the original provider tool-call id, function name and exact argument map serialized as JSON;
- returns an `awaiting approval` tool result to the current Teya turn rather than executing the action;
- adds a package-scoped `TEYA_EXECUTE_APPROVED` service action that reconstructs the original `ToolCall` and invokes the original upstream actuator implementation;
- broadcasts the actuator result to LifeOS as `TEYA_EXECUTION_RESULT` for the persistent Action Center ledger.

No upstream actuator logic, tool schema, calendar/telephony/shopping/expense implementation, or provider code is reimplemented by this patch. Removing the patch restores the verified upstream Harness source exactly on the next build.
