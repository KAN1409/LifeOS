package com.kareem.lifeos.retrieval;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.kareem.lifeos.memory.MemoryRecord;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class HybridMemoryRecallTest {
    private static MemoryRecord m(long id, String text, float[] embedding) {
        return new MemoryRecord(id, "person:1", text, MemoryRecord.Category.FACT,
                1, 1, 1f, MemoryRecord.Tier.HOT, embedding, "assert:"+id,
                Collections.singletonList("evidence:"+id));
    }

    @Test public void lexicalExactAndDenseConsensusSurfacesFirst() {
        MemoryRecord exactAndDense = m(1, "Ahmed project meeting tomorrow", new float[]{1f,0f});
        MemoryRecord lexicalOnly = m(2, "Ahmed sent a different project note", new float[]{0f,1f});
        MemoryRecord denseOnly = m(3, "Unrelated wording", new float[]{0.95f,0.05f});
        List<MemoryRecord> out = HybridMemoryRecall.rank(
                Arrays.asList(exactAndDense, lexicalOnly, denseOnly),
                "Ahmed project meeting tomorrow", new float[]{1f,0f}, 3);
        assertEquals(1L, out.get(0).id);
    }

    @Test public void arabicLexicalTokensAreRetained() {
        MemoryRecord arabic = m(7, "اجتماع المشروع بكرة مع أحمد", null);
        List<MemoryRecord> out = HybridMemoryRecall.rank(
                Collections.singletonList(arabic), "المشروع أحمد", null, 5);
        assertEquals(1, out.size());
        assertEquals(7L, out.get(0).id);
    }

    @Test public void noEvidenceMatchReturnsEmpty() {
        MemoryRecord one = m(9, "budget review", new float[]{0f,1f});
        List<MemoryRecord> out = HybridMemoryRecall.rank(
                Collections.singletonList(one), "holiday", new float[]{1f,0f}, 5);
        assertTrue(out.isEmpty());
    }
}
