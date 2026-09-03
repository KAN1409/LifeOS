# LifeOS

LifeOS is a standalone Android personal-context monitor. It is intentionally independent from Cortex: a different package, project, database, permissions, and lifecycle.

## V0.7 scope

- Explicit Android Notification Access onboarding.
- Local-only capture of alerting/conversation notifications.
- Notification updates are linked by Android key. WhatsApp-style child/summary notifications with different keys but identical content in the same five-second posting window resolve to one canonical event.
- Existing duplicate rows remain intact for history continuity but are collapsed in the Recent UI.
- Generic WhatsApp group summaries such as `3 new messages` are rejected and old summary rows are hidden without deleting history.
- Optional, visibly user-enabled Android Accessibility service reads text currently visible in supported messaging apps, allowing local context from both incoming and outgoing messages.
- Screen capture is debounced until the UI is quiet, then upserts one canonical event per conversation instead of persisting every intermediate screen mutation.
- Old screen snapshots remain stored for history continuity but collapse to the latest conversation state in Recent.
- Date separators such as `Today` no longer become appointments; existing false-positive date-only loops are retained as invalidated records and disappear from Open Loops.
- Screen snapshots are copied while the messaging window is active and committed after stabilization; leaving WhatsApp can no longer mislabel the Samsung launcher as WhatsApp.
- WhatsApp notification text lines and distinct body revisions are stored as separate message evidence, so rapid messages such as `Test`, `1`, `2`, `3` do not collapse to only the final update.
- Previously misattributed launcher snapshots remain stored for audit continuity but are hidden from Recent.
- Overlapping screen snapshots that refer to the same visible conversation are collapsed in Recent even when WhatsApp changes the header from a phone number to a contact name. The stored history is not deleted.
- WhatsApp interface markers such as `7 unread messages` are filtered from newly captured conversation evidence.
- A dedicated adaptive LifeOS launcher icon is included.
- WhatsApp home, Channels, and Communities screens are rejected instead of being misclassified as conversations; previously captured home-screen rows remain stored but are hidden from Recent.
- Notification fragments already contained in a newer visible-conversation snapshot are collapsed from Recent.
- The launcher mark is now the meaningful “Life Thread”: multiple life signals converging into one local memory core.
- OTP and verification-code notifications are discarded before persistence.
- Basic conversation threading by source app and conversation/title.
- Deterministic English/Arabic extraction of requests, commitments, and appointment-like messages.
- Recent evidence and open-loop screens.
- User-controlled mark-done and erase-all actions.
- No Internet permission, cloud model, account, analytics, hidden recording, or external action execution.

## Privacy boundary

The application stores data only in its private Android app storage. Android file-based encryption protects that storage while the device is locked. Backups and device-to-device extraction are disabled. V0.1 does not claim database-level encryption and does not request Internet access.

## Build

Requires JDK 17, Android SDK 35, and Gradle 8.9+.

```bash
gradle :app:assembleDebug :app:testDebugUnitTest
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
