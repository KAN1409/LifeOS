package com.kareem.lifeos.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.Test;

public class GroundedDeepBrainTest {
    @Test public void acceptsGroundedPriorityAndNormalizesRank() {
        final DeepBrainRequest request = request();
        GroundedDeepBrain brain = new GroundedDeepBrain(new DeepBrainClient() {
            @Override public DeepBrainResult analyze(DeepBrainRequest ignored) {
                PriorityDecision p = priority("d1", "s1", 9, "Reply to Ahmed", "raw-1");
                return new DeepBrainResult(request.requestId, "One thing needs attention", Collections.singletonList(p));
            }
        });
        DeepBrainResult out = brain.analyze(request);
        assertEquals(1, out.priorities.size());
        assertEquals(1, out.priorities.get(0).rank);
        assertEquals("s1", out.priorities.get(0).situationId);
    }

    @Test public void rejectsInventedSituation() {
        final DeepBrainRequest request = request();
        GroundedDeepBrain brain = new GroundedDeepBrain(new DeepBrainClient() {
            @Override public DeepBrainResult analyze(DeepBrainRequest ignored) {
                return new DeepBrainResult(request.requestId, "", Collections.singletonList(
                        priority("d1", "made-up", 1, "Invented", "raw-1")));
            }
        });
        assertTrue(brain.analyze(request).priorities.isEmpty());
    }

    @Test public void rejectsInventedEvidence() {
        final DeepBrainRequest request = request();
        GroundedDeepBrain brain = new GroundedDeepBrain(new DeepBrainClient() {
            @Override public DeepBrainResult analyze(DeepBrainRequest ignored) {
                return new DeepBrainResult(request.requestId, "", Collections.singletonList(
                        priority("d1", "s1", 1, "Reply", "not-real")));
            }
        });
        assertTrue(brain.analyze(request).priorities.isEmpty());
    }

    @Test public void rejectsMismatchedRequestId() {
        final DeepBrainRequest request = request();
        GroundedDeepBrain brain = new GroundedDeepBrain(new DeepBrainClient() {
            @Override public DeepBrainResult analyze(DeepBrainRequest ignored) {
                return new DeepBrainResult("different", "", Collections.singletonList(
                        priority("d1", "s1", 1, "Reply", "raw-1")));
            }
        });
        assertTrue(brain.analyze(request).priorities.isEmpty());
    }

    private static DeepBrainRequest request() {
        List<String> evidence = Collections.singletonList("raw-1");
        ContextEvent event = new ContextEvent("event:raw-1", "MESSAGE", "", "chat|ahmed",
                "hello", 1000L, 1.0, evidence);
        Episode episode = new Episode("ep1", "chat|ahmed", 1000L, 1000L,
                Collections.singletonList(event), Collections.<EntityRef>emptyList());
        Situation situation = new Situation("s1", 1000L, 1000L, "Ahmed",
                Collections.singletonList(episode), Collections.<EntityRef>emptyList(), 0.9);
        LifeModelSnapshot model = new LifeModelSnapshot("test", 2000L,
                Collections.<LifeFact>emptyList(), new LinkedHashMap<String,LifeFact>(),
                Collections.singletonList(situation));
        return new DeepBrainRequest("req-1", 2000L, model);
    }

    private static PriorityDecision priority(String id, String situationId, int rank,
                                             String title, String evidenceId) {
        List<String> evidence = new ArrayList<String>();
        evidence.add(evidenceId);
        return new PriorityDecision(id, situationId, rank, title, "Because it is pending", 0.9,
                evidence, Collections.singletonList("Reply"));
    }
}
