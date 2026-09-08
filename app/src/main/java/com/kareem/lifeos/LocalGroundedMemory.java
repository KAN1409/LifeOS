package com.kareem.lifeos;

import android.content.Context;
import com.kareem.lifeos.memory.MemoryRecord;
import com.kareem.lifeos.memory.PersistentLifeMemoryStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Zero-config bridge from high-confidence local human evidence into durable memory.
 *
 * Raw capture remains broad. Durable memory promotion is deliberately narrower:
 * only granular evidence that is already classified as a real person conversation
 * can become an episodic memory. Every memory keeps the originating LifeDb event id.
 */
final class LocalGroundedMemory {
    private static final int MAX_TEXT = 600;
    private static final double MEANING_MEMORY_CONFIDENCE = .82;

    private LocalGroundedMemory() {}

    static long materialize(Context context, LifeDb.Event event) {
        if (context == null || !eligible(event)) return -1L;
        String subject = subjectId(event);
        String text = memoryText(event);
        if (subject.isEmpty() || text.isEmpty()) return -1L;
        List<String> evidence = Collections.singletonList(Long.toString(event.id));
        return PersistentLifeMemoryStore.get(context).remember(
                subject,
                text,
                MemoryRecord.Category.EPISODIC,
                null,
                assertionId(event),
                evidence,
                event.at > 0 ? event.at : System.currentTimeMillis());
    }

    /**
     * Persist a compressed model interpretation only when it is both strongly grounded and useful.
     * Informational chatter never becomes a second semantic memory record merely because a model
     * summarized it. The raw episodic message remains the source of truth and its event id is kept
     * first in provenance so every semantic memory remains drillable back to evidence.
     */
    static long materializeMeaning(Context context, LifeDb.Event event, NotificationMeaning meaning) {
        if (context == null || event == null || meaning == null) return -1L;
        if (!EventSemantics.isPersonConversation(event)
                || !"PERSON_CONVERSATION".equals(meaning.type)
                || meaning.confidence < MEANING_MEMORY_CONFIDENCE
                || !meaning.canSummarize()
                || !meaningWorthRemembering(meaning)) return -1L;
        String subject = subjectId(event);
        if (subject.isEmpty() || meaning.summary.trim().isEmpty()) return -1L;
        String assertion = "notification-meaning|" + meaning.sourceObservationId;
        List<String> evidence = Arrays.asList(Long.toString(event.id), meaning.sourceObservationId);
        return PersistentLifeMemoryStore.get(context).remember(
                subject,
                meaning.summary.trim(),
                MemoryRecord.Category.EPISODIC,
                null,
                assertion,
                evidence,
                meaning.understoodAt > 0 ? meaning.understoodAt : System.currentTimeMillis());
    }

    /**
     * Bounded migration path for already-captured data. Newest evidence is considered first,
     * but promotion stops quickly so an app update cannot turn thousands of raw rows into memory.
     */
    static int backfill(Context context, LifeDb db, int scanLimit, int promoteLimit) {
        if (context == null || db == null || scanLimit <= 0 || promoteLimit <= 0) return 0;
        PersistentLifeMemoryStore store = PersistentLifeMemoryStore.get(context);
        int promoted = 0;
        List<LifeDb.Event> recent = db.recentEvents(Math.max(1, scanLimit));
        for (LifeDb.Event event : recent) {
            if (promoted >= promoteLimit) break;
            if (!eligible(event)) continue;
            String assertion = assertionId(event);
            if (store.hasAssertion(assertion)) continue;
            long id = materialize(context, event);
            if (id > 0) promoted++;
        }
        return promoted;
    }

    static boolean eligible(LifeDb.Event event) {
        if (event == null || event.id <= 0 || !EventSemantics.isPersonConversation(event)) return false;
        if ("Visible conversation".equals(event.title)) return false; // aggregate screen snapshot, not one assertion
        String body = clean(event.body);
        if (body.length() < 2 || CapturePolicy.isNotificationSummary(body)
                || CapturePolicy.isLauncherSnapshot(body)
                || CapturePolicy.isMessagingHomeSnapshot(body)) return false;
        String low = body.toLowerCase(Locale.ROOT);
        if (low.startsWith("reacted ") || low.matches(".{1,80}:\\s*reacted .*")) return false;
        if (low.matches("[📷🎤📄💟]?\\s*(photo|sticker|voice message|gif|image)(\\s*\\([^)]*\\))?")) return false;
        boolean hasLetterOrDigit = false;
        for (int i=0;i<body.length();i++) {
            char c=body.charAt(i);
            if (Character.isLetterOrDigit(c)) { hasLetterOrDigit=true; break; }
        }
        return hasLetterOrDigit;
    }

    static String subjectId(LifeDb.Event event) {
        if (event == null || !EventSemantics.isPersonConversation(event)) return "";
        String app = normalize(event.app);
        String label = normalize(LifeDb.personLabel(event));
        return app.isEmpty() || label.isEmpty() ? "" : "conversation:" + app + ":" + label;
    }

    static List<MemoryRecord> memoriesFor(Context context, LifeDb.Event event, int limit) {
        String subject = subjectId(event);
        if (context == null || subject.isEmpty()) return new ArrayList<MemoryRecord>();
        return PersistentLifeMemoryStore.get(context).hotForSubject(subject, Math.max(1, limit));
    }

    static String assertionId(LifeDb.Event event) {
        return event == null ? "" : "life-event|" + event.id;
    }

    private static boolean meaningWorthRemembering(NotificationMeaning meaning) {
        if (meaning == null) return false;
        if ("WAITING_ON_USER".equals(meaning.state) || "WAITING_ON_OTHER".equals(meaning.state)) return true;
        return "REQUEST".equals(meaning.intent) || "QUESTION".equals(meaning.intent)
                || "COMMITMENT".equals(meaning.intent) || "SCHEDULE".equals(meaning.intent);
    }

    private static String memoryText(LifeDb.Event event) {
        String body = clean(event.body).replaceAll("\\s+", " ");
        if (body.length() > MAX_TEXT) body = body.substring(0, MAX_TEXT) + "…";
        String label = LifeDb.personLabel(event);
        return clean(label).isEmpty() ? body : clean(label) + ": " + body;
    }

    private static String normalize(String value) {
        return clean(value).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
    private static String clean(String value) { return value == null ? "" : value.trim(); }
}
