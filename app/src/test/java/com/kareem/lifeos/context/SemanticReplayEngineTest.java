package com.kareem.lifeos.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class SemanticReplayEngineTest {
    @Test public void replayOrdersEvidenceBeforeInterpretation() {
        List<RawObservation> raw = new ArrayList<RawObservation>();
        raw.add(obs("late", 2000L));
        raw.add(obs("early", 1000L));

        List<SemanticAssertion> out = SemanticReplayEngine.replay(raw, echoInterpreter());
        assertEquals(2, out.size());
        assertEquals("assert:early", out.get(0).assertionId);
        assertEquals("assert:late", out.get(1).assertionId);
    }

    @Test public void zeroAssertionsIsValidWhenEvidenceIsInsufficient() {
        List<RawObservation> raw = Collections.singletonList(obs("x", 1000L));
        List<SemanticAssertion> out = SemanticReplayEngine.replay(raw, new SemanticInterpreter() {
            @Override public String version() { return "empty"; }
            @Override public List<SemanticAssertion> interpret(RawObservation observation) {
                return Collections.emptyList();
            }
        });
        assertTrue(out.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void interpreterIsRequired() {
        SemanticReplayEngine.replay(Collections.<RawObservation>emptyList(), null);
    }

    private static SemanticInterpreter echoInterpreter() {
        return new SemanticInterpreter() {
            @Override public String version() { return "echo-v1"; }
            @Override public List<SemanticAssertion> interpret(RawObservation o) {
                List<String> evidence = Collections.singletonList(o.observationId);
                return Collections.singletonList(new SemanticAssertion(
                        "assert:" + o.observationId,
                        "stream:" + o.streamId,
                        "seen",
                        o.text,
                        SemanticAssertion.State.ASSERTED,
                        o.observedAt,
                        o.observedAt,
                        0L,
                        1.0,
                        evidence));
            }
        };
    }

    private static RawObservation obs(String id, long at) {
        return new RawObservation(id, RawObservation.SourceKind.OTHER, "test", "test|stream",
                "OBSERVED", at, id, "", null);
    }
}
