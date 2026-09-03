package com.kareem.lifeos.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class LifeModelAssemblerTest {
    @Test public void onlyExplicitSemanticAttributesBecomeFacts() {
        List<RawObservation> xs = new ArrayList<RawObservation>();
        xs.add(obs("raw", 1000L, "call Ahmed tomorrow", null));
        LifeContextSnapshot context = LifeContextAssembler.rebuild(xs, 2000L);
        LifeModelSnapshot model = LifeModelAssembler.rebuild(xs, context, 2000L);
        assertTrue(model.factHistory.isEmpty());
        assertTrue(model.currentFacts.isEmpty());
    }

    @Test public void laterRevisionWinsWithoutDeletingHistory() {
        List<RawObservation> xs = new ArrayList<RawObservation>();
        xs.add(obs("one", 1000L, "", fact("person:ahmed", "meeting_status", "planned", "ASSERTED")));
        xs.add(obs("two", 2000L, "", fact("person:ahmed", "meeting_status", "confirmed", "ASSERTED")));
        LifeModelSnapshot model = LifeModelAssembler.rebuild(xs, LifeContextAssembler.rebuild(xs, 3000L), 3000L);
        assertEquals(2, model.factHistory.size());
        assertEquals(1, model.currentFacts.size());
        assertEquals("confirmed", model.currentFacts.values().iterator().next().value);
    }

    @Test public void retractionRemovesCurrentProjectionButPreservesRevision() {
        List<RawObservation> xs = new ArrayList<RawObservation>();
        xs.add(obs("one", 1000L, "", fact("person:ahmed", "needs_reply", "true", "ASSERTED")));
        xs.add(obs("two", 2000L, "", fact("person:ahmed", "needs_reply", "true", "RETRACTED")));
        LifeModelSnapshot model = LifeModelAssembler.rebuild(xs, LifeContextAssembler.rebuild(xs, 3000L), 3000L);
        assertEquals(2, model.factHistory.size());
        assertTrue(model.currentFacts.isEmpty());
        assertEquals(LifeFact.State.RETRACTED, model.factHistory.get(1).state);
    }

    @Test public void expiredFactIsNotCurrent() {
        Map<String,String> attrs = fact("person:ahmed", "appointment", "dentist", "ASSERTED");
        attrs.put("life_fact_valid_to", "1500");
        List<RawObservation> xs = new ArrayList<RawObservation>();
        xs.add(obs("one", 1000L, "", attrs));
        LifeModelSnapshot model = LifeModelAssembler.rebuild(xs, LifeContextAssembler.rebuild(xs, 2000L), 2000L);
        assertEquals(1, model.factHistory.size());
        assertFalse(model.currentFacts.containsKey("entity:person:ahmed|appointment"));
    }

    @Test public void evidenceIdSurvivesIntoLifeFact() {
        List<RawObservation> xs = new ArrayList<RawObservation>();
        xs.add(obs("proof", 1000L, "", fact("person:ahmed", "commitment", "send file", "ASSERTED")));
        LifeModelSnapshot model = LifeModelAssembler.rebuild(xs, LifeContextAssembler.rebuild(xs, 2000L), 2000L);
        assertEquals("proof", model.factHistory.get(0).evidenceIds.get(0));
    }

    private static RawObservation obs(String id, long at, String text, Map<String,String> attrs) {
        return new RawObservation(id, RawObservation.SourceKind.NOTIFICATION, "com.chat",
                "com.chat|ahmed", "OBSERVED", at, text, "", attrs);
    }

    private static Map<String,String> fact(String subject, String predicate, String value, String state) {
        Map<String,String> out = new HashMap<String,String>();
        out.put("life_fact_subject_id", subject);
        out.put("life_fact_predicate", predicate);
        out.put("life_fact_value", value);
        out.put("life_fact_state", state);
        return out;
    }
}
