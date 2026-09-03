package com.kareem.lifeos.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class LifeModelAssemblerTest {
    @Test public void rawTextDoesNotBecomeFactByItself() {
        List<RawObservation> raw = new ArrayList<RawObservation>();
        raw.add(obs("raw", 1000L, "call Ahmed tomorrow"));
        LifeContextSnapshot context = LifeContextAssembler.rebuild(raw, 2000L);
        LifeModelSnapshot model = LifeModelAssembler.rebuild(
                Collections.<SemanticAssertion>emptyList(), context, 2000L);
        assertTrue(model.factHistory.isEmpty());
        assertTrue(model.currentFacts.isEmpty());
    }

    @Test public void laterRevisionWinsWithoutDeletingHistory() {
        List<SemanticAssertion> xs = new ArrayList<SemanticAssertion>();
        xs.add(assertion("one", "person:ahmed", "meeting_status", "planned", SemanticAssertion.State.ASSERTED, 1000L, 0L));
        xs.add(assertion("two", "person:ahmed", "meeting_status", "confirmed", SemanticAssertion.State.ASSERTED, 2000L, 0L));
        LifeModelSnapshot model = LifeModelAssembler.rebuild(xs, null, 3000L);
        assertEquals(2, model.factHistory.size());
        assertEquals(1, model.currentFacts.size());
        assertEquals("confirmed", model.currentFacts.values().iterator().next().value);
    }

    @Test public void retractionRemovesCurrentProjectionButPreservesRevision() {
        List<SemanticAssertion> xs = new ArrayList<SemanticAssertion>();
        xs.add(assertion("one", "person:ahmed", "needs_reply", "true", SemanticAssertion.State.ASSERTED, 1000L, 0L));
        xs.add(assertion("two", "person:ahmed", "needs_reply", "true", SemanticAssertion.State.RETRACTED, 2000L, 0L));
        LifeModelSnapshot model = LifeModelAssembler.rebuild(xs, null, 3000L);
        assertEquals(2, model.factHistory.size());
        assertTrue(model.currentFacts.isEmpty());
        assertEquals(LifeFact.State.RETRACTED, model.factHistory.get(1).state);
    }

    @Test public void expiredFactIsNotCurrent() {
        List<SemanticAssertion> xs = new ArrayList<SemanticAssertion>();
        xs.add(assertion("one", "person:ahmed", "appointment", "dentist", SemanticAssertion.State.ASSERTED, 1000L, 1500L));
        LifeModelSnapshot model = LifeModelAssembler.rebuild(xs, null, 2000L);
        assertEquals(1, model.factHistory.size());
        assertFalse(model.currentFacts.containsKey("entity:person:ahmed|appointment"));
    }

    @Test public void evidenceIdSurvivesIntoLifeFact() {
        List<SemanticAssertion> xs = new ArrayList<SemanticAssertion>();
        xs.add(assertion("assert-1", "person:ahmed", "commitment", "send file", SemanticAssertion.State.ASSERTED, 1000L, 0L));
        LifeModelSnapshot model = LifeModelAssembler.rebuild(xs, null, 2000L);
        assertEquals("raw-assert-1", model.factHistory.get(0).evidenceIds.get(0));
    }

    private static RawObservation obs(String id, long at, String text) {
        return new RawObservation(id, RawObservation.SourceKind.NOTIFICATION, "com.chat",
                "com.chat|ahmed", "OBSERVED", at, text, "", null);
    }

    private static SemanticAssertion assertion(String id, String subject, String predicate,
                                                String value, SemanticAssertion.State state,
                                                long at, long validTo) {
        List<String> evidence = new ArrayList<String>();
        evidence.add("raw-" + id);
        return new SemanticAssertion(id, subject, predicate, value, state,
                at, at, validTo, 0.95, evidence);
    }
}
