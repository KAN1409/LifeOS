package com.kareem.lifeos.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class LifeContextAssemblerTest {
    @Test public void interleavedStreamsDoNotBreakEpisodes() {
        List<RawObservation> xs = new ArrayList<RawObservation>();
        xs.add(obs("n1", RawObservation.SourceKind.NOTIFICATION, "com.chat", "com.chat|ahmed", 1000L, "one", null));
        xs.add(obs("m1", RawObservation.SourceKind.NOTIFICATION, "com.mail", "com.mail|work", 2000L, "mail", null));
        xs.add(obs("n2", RawObservation.SourceKind.NOTIFICATION, "com.chat", "com.chat|ahmed", 3000L, "two", null));

        LifeContextSnapshot snapshot = LifeContextAssembler.rebuild(xs, 5000L);
        assertEquals(2, snapshot.episodes.size());
        Episode chat = snapshot.episodes.get(0);
        assertEquals("com.chat|ahmed", chat.streamId);
        assertEquals(2, chat.events.size());
    }

    @Test public void sameStreamAfterLongGapStartsNewEpisode() {
        List<RawObservation> xs = new ArrayList<RawObservation>();
        xs.add(obs("a", RawObservation.SourceKind.NOTIFICATION, "com.chat", "com.chat|ahmed", 0L, "one", null));
        xs.add(obs("b", RawObservation.SourceKind.NOTIFICATION, "com.chat", "com.chat|ahmed",
                LifeContextAssembler.EPISODE_GAP_MS + 1L, "two", null));
        LifeContextSnapshot snapshot = LifeContextAssembler.rebuild(xs, 1L);
        assertEquals(2, snapshot.episodes.size());
    }

    @Test public void explicitCanonicalEntityLinksDifferentSourcesIntoOneSituation() {
        List<RawObservation> xs = new ArrayList<RawObservation>();
        Map<String,String> chat = entity("person:ahmed", "Ahmed", "PERSON");
        Map<String,String> calendar = entity("person:ahmed", "Ahmed", "PERSON");
        xs.add(obs("chat", RawObservation.SourceKind.NOTIFICATION, "com.chat", "com.chat|ahmed", 1000L, "Can we meet?", chat));
        xs.add(obs("cal", RawObservation.SourceKind.CALENDAR, "calendar", "calendar|event42", 2000L, "Meeting with Ahmed", calendar));

        LifeContextSnapshot snapshot = LifeContextAssembler.rebuild(xs, 3000L);
        assertEquals(2, snapshot.episodes.size());
        assertEquals(1, snapshot.situations.size());
        assertEquals(2, snapshot.situations.get(0).episodes.size());
        assertEquals("Ahmed", snapshot.situations.get(0).title);
    }

    @Test public void applicationIdentityAloneNeverMergesUnrelatedStreams() {
        List<RawObservation> xs = new ArrayList<RawObservation>();
        xs.add(obs("a", RawObservation.SourceKind.ACCESSIBILITY, "com.chat", "com.chat", 1000L, "", null));
        xs.add(obs("b", RawObservation.SourceKind.NOTIFICATION, "com.chat", "com.chat|ahmed", 2000L, "hello", null));
        LifeContextSnapshot snapshot = LifeContextAssembler.rebuild(xs, 3000L);
        assertEquals(2, snapshot.situations.size());
    }

    @Test public void provenanceSurvivesIntoEpisodeEvents() {
        List<RawObservation> xs = new ArrayList<RawObservation>();
        xs.add(obs("proof-1", RawObservation.SourceKind.NOTIFICATION, "com.chat", "com.chat|ahmed", 1000L, "hello", null));
        LifeContextSnapshot snapshot = LifeContextAssembler.rebuild(xs, 3000L);
        assertEquals(1, snapshot.episodes.size());
        assertEquals("proof-1", snapshot.episodes.get(0).events.get(0).evidenceIds.get(0));
        assertTrue(snapshot.episodes.get(0).events.get(0).eventId.contains("proof-1"));
    }

    private static RawObservation obs(String id, RawObservation.SourceKind kind, String pkg,
                                      String stream, long at, String text, Map<String,String> attrs) {
        return new RawObservation(id, kind, pkg, stream, "OBSERVED", at, text, "", attrs);
    }

    private static Map<String,String> entity(String id, String label, String kind) {
        Map<String,String> out = new HashMap<String,String>();
        out.put("canonical_entity_id", id);
        out.put("canonical_entity_label", label);
        out.put("canonical_entity_kind", kind);
        return out;
    }
}
