# LifeOS Architecture

## Current direction: Conversation Understanding Engine

LifeOS must not understand messaging UIs through hard-coded text suppression rules.

Pipeline:

1. Raw Capture
2. Structural Screen Analysis
3. Screen State Classification
4. Conversation Parsing
5. Observation Model
6. Reconciliation
7. Canonical Events
8. Life Understanding / Open Loops

Core invariant: raw evidence is preserved, while the canonical view can be reinterpreted as the engine improves.

## M1 goals

- Keep NotificationListener and AccessibilityService as capture adapters.
- Separate raw observations from canonical events.
- Parse conversation structure from geometry/tree evidence rather than specific UI phrases.
- Treat unread/date/system markers separately from messages.
- Reconcile notification and screen observations into one canonical event.
- Keep UNKNOWN as a valid low-confidence result.

## Anti-patterns retired

Do not add logic equivalent to `if this exact UI text appears, hide it` to fix individual screenshots.
Application-specific metadata may enhance evidence, but must not be the semantic engine itself.
