package com.kareem.lifeos.context;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public final class ObservationReplayEngineTest {
    @Test public void replayIsChronologicalAndKeepsEvidenceProvenance() {
        RawObservation later = new RawObservation("b", RawObservation.SourceKind.OTHER,
                "pkg", "s", "X", 20L, "later", "", null);
        RawObservation earlier = new RawObservation("a", RawObservation.SourceKind.OTHER,
                "pkg", "s", "X", 10L, "earlier", "", null);

        ContextInterpreter interpreter = new ContextInterpreter() {
            @Override public String version() { return "test-v1"; }
            @Override public List<ContextEvent> interpret(RawObservation o) {
                return Collections.singletonList(new ContextEvent(
                        "event-" + o.observationId, "OBSERVED", "", o.streamId,
                        o.text, o.observedAt, 1.0, Collections.singletonList(o.observationId)));
            }
        };

        List<ContextEvent> events = ObservationReplayEngine.replay(Arrays.asList(later, earlier), interpreter);
        assertEquals(2, events.size());
        assertEquals("earlier", events.get(0).summary);
        assertEquals("a", events.get(0).evidenceIds.get(0));
        assertEquals("later", events.get(1).summary);
        assertEquals("b", events.get(1).evidenceIds.get(0));
    }
}
