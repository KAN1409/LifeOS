package com.kareem.lifeos.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class GroundedSemanticInterpreterTest {
    @Test public void acceptsAssertionGroundedInCurrentObservation() {
        RawObservation raw = obs("raw-1", 1000L);
        GroundedSemanticInterpreter interpreter = new GroundedSemanticInterpreter("test", new SemanticModelClient() {
            @Override public List<SemanticAssertion> analyze(RawObservation observation, List<EntityRef> entities) {
                return Collections.singletonList(assertion("a1", observation.observationId, observation.observedAt, "commitment"));
            }
        });
        List<SemanticAssertion> out = interpreter.interpret(raw);
        assertEquals(1, out.size());
        assertEquals("commitment", out.get(0).predicate);
    }

    @Test public void rejectsHallucinatedEvidenceId() {
        RawObservation raw = obs("raw-1", 1000L);
        GroundedSemanticInterpreter interpreter = new GroundedSemanticInterpreter("test", new SemanticModelClient() {
            @Override public List<SemanticAssertion> analyze(RawObservation observation, List<EntityRef> entities) {
                return Collections.singletonList(assertion("a1", "raw-does-not-exist", observation.observedAt, "commitment"));
            }
        });
        assertTrue(interpreter.interpret(raw).isEmpty());
    }

    @Test public void rejectsAssertionWithInventedObservationTime() {
        RawObservation raw = obs("raw-1", 1000L);
        GroundedSemanticInterpreter interpreter = new GroundedSemanticInterpreter("test", new SemanticModelClient() {
            @Override public List<SemanticAssertion> analyze(RawObservation observation, List<EntityRef> entities) {
                return Collections.singletonList(assertion("a1", observation.observationId, 9999L, "commitment"));
            }
        });
        assertTrue(interpreter.interpret(raw).isEmpty());
    }

    @Test public void rejectsEmptyPredicate() {
        RawObservation raw = obs("raw-1", 1000L);
        GroundedSemanticInterpreter interpreter = new GroundedSemanticInterpreter("test", new SemanticModelClient() {
            @Override public List<SemanticAssertion> analyze(RawObservation observation, List<EntityRef> entities) {
                return Collections.singletonList(assertion("a1", observation.observationId, observation.observedAt, ""));
            }
        });
        assertTrue(interpreter.interpret(raw).isEmpty());
    }

    private static RawObservation obs(String id, long at) {
        return new RawObservation(id, RawObservation.SourceKind.NOTIFICATION, "com.chat",
                "com.chat|ahmed", "POSTED", at, "hello", "", null);
    }

    private static SemanticAssertion assertion(String id, String evidenceId, long at, String predicate) {
        List<String> evidence = new ArrayList<String>();
        evidence.add(evidenceId);
        return new SemanticAssertion(id, "person:ahmed", predicate, "send file",
                SemanticAssertion.State.ASSERTED, at, at, 0L, 0.9, evidence);
    }
}
