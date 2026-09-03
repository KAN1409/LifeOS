package com.kareem.lifeos.retrieval;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class RrfFusionTest {
    @Test public void consensusResultWinsWhenCombinedRanksAreBetter() {
        List<String> lexical = Arrays.asList("exact", "shared", "other");
        List<String> dense = Arrays.asList("shared", "semantic", "exact");
        List<RrfFusion.Fused<String>> out = RrfFusion.fuse(lexical, dense,
                new RrfFusion.Keyer<String>() { @Override public String key(String value) { return value; } });
        assertEquals("shared", out.get(0).value);
        assertEquals(Integer.valueOf(2), out.get(0).lexicalRank);
        assertEquals(Integer.valueOf(1), out.get(0).denseRank);
        assertTrue(out.get(0).score > out.get(1).score);
    }

    @Test public void followsExactReciprocalRankFormula() {
        List<RrfFusion.Fused<String>> out = RrfFusion.fuse(
                Arrays.asList("exact", "shared", "other"),
                Arrays.asList("semantic", "shared", "exact"),
                new RrfFusion.Keyer<String>() { @Override public String key(String value) { return value; } });
        // exact = 1/(60+1) + 1/(60+3), shared = 1/(60+2) + 1/(60+2)
        assertEquals("exact", out.get(0).value);
        assertEquals((1.0/61.0)+(1.0/63.0), out.get(0).score, 1e-12);
    }

    @Test public void preservesSingleChannelResults() {
        List<RrfFusion.Fused<String>> out = RrfFusion.fuse(Arrays.asList("a","b"),
                Arrays.<String>asList(),
                new RrfFusion.Keyer<String>() { @Override public String key(String value) { return value; } });
        assertEquals(Arrays.asList("a","b"), Arrays.asList(out.get(0).value,out.get(1).value));
    }
}
