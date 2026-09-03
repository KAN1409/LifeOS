package com.kareem.lifeos.memory;

import static org.junit.Assert.assertEquals;

import com.kareem.lifeos.context.SemanticAssertion;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class LifeMemoryBridgeTest {
    @Test public void groundedAssertionIsPersistedWithProvenance() {
        FakeRepository repo = new FakeRepository();
        LifeMemoryBridge bridge = new LifeMemoryBridge(repo);
        SemanticAssertion a = assertion("a1", "entity:ahmed", "send file", Arrays.asList("obs:1"));
        assertEquals(42L, bridge.remember(a, MemoryRecord.Category.FACT, null, 100L));
        assertEquals("a1", repo.assertionId);
        assertEquals(Arrays.asList("obs:1"), repo.evidenceIds);
    }

    @Test public void ungroundedAssertionIsRejected() {
        FakeRepository repo = new FakeRepository();
        LifeMemoryBridge bridge = new LifeMemoryBridge(repo);
        SemanticAssertion a = assertion("a1", "entity:ahmed", "send file", Arrays.<String>asList());
        assertEquals(-1L, bridge.remember(a, MemoryRecord.Category.FACT, null, 100L));
        assertEquals(0, repo.rememberCalls);
    }

    @Test public void similarDurableMemoryIsNotDuplicated() {
        FakeRepository repo = new FakeRepository();
        repo.similar = true;
        LifeMemoryBridge bridge = new LifeMemoryBridge(repo);
        assertEquals(-1L, bridge.remember(
                assertion("a1", "entity:ahmed", "send file", Arrays.asList("obs:1")),
                MemoryRecord.Category.FACT, null, 100L));
        assertEquals(0, repo.rememberCalls);
    }

    private static SemanticAssertion assertion(String id, String subject, String value, List<String> evidence) {
        return new SemanticAssertion(id, subject, "commitment", value,
                SemanticAssertion.State.ASSERTED, 1L, 1L, 0L, 1.0, evidence);
    }

    private static final class FakeRepository implements LifeMemoryRepository {
        boolean similar;
        int rememberCalls;
        String assertionId;
        List<String> evidenceIds;

        @Override public long remember(String subjectEntityId, String text,
                                       MemoryRecord.Category category, float[] embedding,
                                       String sourceAssertionId, List<String> evidenceIds, long now) {
            rememberCalls++;
            assertionId = sourceAssertionId;
            this.evidenceIds = evidenceIds;
            return 42L;
        }

        @Override public boolean hasSimilar(String text, String subjectEntityId) { return similar; }
    }
}
