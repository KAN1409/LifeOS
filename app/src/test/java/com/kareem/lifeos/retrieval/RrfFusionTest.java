package com.kareem.lifeos.retrieval;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class RrfFusionTest {
    @Test public void itemPresentInBothRankingsWins() {
        List<String> lexical = Arrays.asList("exact", "shared", "other");
        List<String> dense = Arrays.asList("semantic", "shared", "exact");
        List<RrfFusion.Fused<String>> out = RrfFusion.fuse(lexical, dense,
                new RrfFusion.Keyer<String>() { @Override public String key(String value) { return value; } });
        assertEquals("shared", out.get(0).value);
        assertTrue(out.get(0).lexicalRank != null);
        assertTrue(out.get(0).denseRank != null);
    }

    @Test public void preservesSingleChannelResults() {
        List<RrfFusion.Fused<String>> out = RrfFusion.fuse(Arrays.asList("a","b"),
                Arrays.<String>asList(),
                new RrfFusion.Keyer<String>() { @Override public String key(String value) { return value; } });
        assertEquals(Arrays.asList("a","b"), Arrays.asList(out.get(0).value,out.get(1).value));
    }
}
