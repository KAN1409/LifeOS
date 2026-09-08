package com.kareem.lifeos.context;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import org.junit.Test;

public class ObservationDeduplicatorTest {
    private static RawObservation obs(String pkg, String payload, long at) {
        return new RawObservation("id-" + at, RawObservation.SourceKind.ACCESSIBILITY,
                pkg, pkg, "TREE_SNAPSHOT", at, "", payload, Collections.<String,String>emptyMap());
    }

    @Test public void firstObservationPersistsThenExactDuplicateDrops() {
        ObservationDeduplicator d = new ObservationDeduplicator();
        assertTrue(d.evaluate(obs("com.example", "{\"screen\":\"Inbox\"}", 1)).persist);
        ObservationDeduplicator.Decision duplicate = d.evaluate(
                obs("com.example", "{\"screen\":\"Inbox\"}", 2));
        assertFalse(duplicate.persist);
        assertEquals(ObservationDeduplicator.Gate.EXACT, duplicate.gate);
    }

    @Test public void timestampOnlyChangeDoesNotCreateNewEvidence() {
        ObservationDeduplicator d = new ObservationDeduplicator();
        assertTrue(d.evaluate(obs("com.example", "{\"capturedAt\":100,\"title\":\"Inbox\"}", 100)).persist);
        ObservationDeduplicator.Decision duplicate = d.evaluate(
                obs("com.example", "{\"capturedAt\":200,\"title\":\"Inbox\"}", 200));
        assertFalse(duplicate.persist);
        assertEquals(ObservationDeduplicator.Gate.EXACT, duplicate.gate);
    }

    @Test public void meaningfulContentChangePersists() {
        ObservationDeduplicator d = new ObservationDeduplicator(0.98);
        assertTrue(d.evaluate(obs("com.example", "Ahmed sent the project drawing revision one", 1)).persist);
        ObservationDeduplicator.Decision changed = d.evaluate(
                obs("com.example", "Mona approved the budget and requested a meeting tomorrow", 2));
        assertTrue(changed.persist);
        assertEquals(ObservationDeduplicator.Gate.PERSIST, changed.gate);
    }

    @Test public void streamsAreIndependent() {
        ObservationDeduplicator d = new ObservationDeduplicator();
        assertTrue(d.evaluate(obs("com.whatsapp", "same content", 1)).persist);
        assertTrue(d.evaluate(obs("com.google.android.gm", "same content", 2)).persist);
    }
}
