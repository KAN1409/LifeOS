package com.kareem.lifeos;

import android.content.Context;
import com.kareem.lifeos.memory.MemoryRecord;
import com.kareem.lifeos.memory.PersistentLifeMemoryStore;
import java.util.ArrayList;
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
