package com.kareem.lifeos.memory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class MemoryAlgorithmsTest {
    @Test public void episodicDecaysMuchFasterThanFact() {
        long now = 10L * 86_400_000L;
        MemoryRecord episodic = record(1, MemoryRecord.Category.EPISODIC, 0L, "note", null);
        MemoryRecord fact = record(2, MemoryRecord.Category.FACT, 0L, "fact", null);
        assertTrue(MemoryAlgorithms.strengthNow(episodic, now) < MemoryAlgorithms.strengthNow(fact, now));
    }

    @Test public void oldEpisodicCanBePrunedButFactCannot() {
        long now = 20L * 86_400_000L;
        assertTrue(MemoryAlgorithms.shouldPrune(record(1, MemoryRecord.Category.EPISODIC, 0L, "x", null), now));
        assertFalse(MemoryAlgorithms.shouldPrune(record(2, MemoryRecord.Category.FACT, 0L, "x", null), now));
    }

    @Test public void semanticSearchPrecedesKeywordFallback() {
        List<MemoryRecord> pool = new ArrayList<MemoryRecord>();
        pool.add(record(1, MemoryRecord.Category.FACT, 0L, "dentist appointment", new float[]{1f,0f}));
        pool.add(record(2, MemoryRecord.Category.FACT, 0L, "project alpha", new float[]{0f,1f}));
        List<MemoryRecord> out = MemoryAlgorithms.rank(pool, "project", new float[]{1f,0f}, 2);
        assertEquals(2, out.size());
        assertEquals(1L, out.get(0).id);
        assertEquals(2L, out.get(1).id);
    }

    @Test public void arabicKeywordNormalizationWorks() {
        assertTrue(MemoryAlgorithms.similarText("ميعاد الدكتور بكرة", "الدكتور بكرة"));
    }

    @Test public void recallRecordRetainsEvidenceAndBecomesHot() {
        MemoryRecord r = new MemoryRecord(3L, "entity:ahmed", "send file",
                MemoryRecord.Category.ROUTINE, 1L, 1L, 0.2f, MemoryRecord.Tier.COLD,
                null, "assert:1", Arrays.asList("obs:1","obs:2"));
        MemoryRecord recalled = r.withRecall(99L);
        assertEquals(MemoryRecord.Tier.HOT, recalled.tier);
        assertEquals(1.0f, recalled.strength, 0.0001f);
        assertEquals("assert:1", recalled.sourceAssertionId);
        assertEquals(Arrays.asList("obs:1","obs:2"), recalled.evidenceIds);
    }

    private static MemoryRecord record(long id, MemoryRecord.Category category, long accessed,
                                       String text, float[] embedding) {
        return new MemoryRecord(id, "entity:test", text, category, 0L, accessed,
                1.0f, MemoryRecord.Tier.HOT, embedding, "assert:test", Arrays.asList("obs:test"));
    }
}
