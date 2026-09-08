package com.kareem.lifeos.retrieval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reciprocal Rank Fusion port/adaptation from
 * openintelligence-labs/secondbrain src/secondbrain/search/hybrid.py (MIT).
 *
 * It combines independently ranked lexical and dense result lists without
 * requiring their raw scores to be on the same scale.
 */
public final class RrfFusion {
    public static final int DEFAULT_K = 60;

    private RrfFusion() {}

    public interface Keyer<T> { String key(T value); }

    public static final class Fused<T> {
        public final T value;
        public final double score;
        public final Integer lexicalRank;
        public final Integer denseRank;

        Fused(T value, double score, Integer lexicalRank, Integer denseRank) {
            this.value = value;
            this.score = score;
            this.lexicalRank = lexicalRank;
            this.denseRank = denseRank;
        }
    }

    public static <T> List<Fused<T>> fuse(List<T> lexical, List<T> dense, Keyer<T> keyer) {
        return fuse(lexical, dense, keyer, DEFAULT_K);
    }

    public static <T> List<Fused<T>> fuse(List<T> lexical, List<T> dense,
                                          Keyer<T> keyer, int k) {
        if (keyer == null) return Collections.emptyList();
        int safeK = Math.max(1, k);
        LinkedHashMap<String,Mutable<T>> scored = new LinkedHashMap<String,Mutable<T>>();
        add(scored, lexical, keyer, safeK, true);
        add(scored, dense, keyer, safeK, false);

        List<Mutable<T>> rows = new ArrayList<Mutable<T>>(scored.values());
        Collections.sort(rows, new Comparator<Mutable<T>>() {
            @Override public int compare(Mutable<T> a, Mutable<T> b) {
                int score = Double.compare(b.score, a.score);
                if (score != 0) return score;
                return a.key.compareTo(b.key);
            }
        });

        List<Fused<T>> out = new ArrayList<Fused<T>>();
        for (Mutable<T> row : rows) {
            out.add(new Fused<T>(row.value, row.score, row.lexicalRank, row.denseRank));
        }
        return out;
    }

    private static <T> void add(Map<String,Mutable<T>> scored, List<T> values,
                                Keyer<T> keyer, int k, boolean lexical) {
        if (values == null) return;
        int rank = 0;
        for (T value : values) {
            if (value == null) continue;
            rank++;
            String key = keyer.key(value);
            if (key == null || key.isEmpty()) continue;
            Mutable<T> row = scored.get(key);
            if (row == null) {
                row = new Mutable<T>(key, value);
                scored.put(key, row);
            }
            row.score += 1.0 / (k + rank);
            if (lexical) row.lexicalRank = rank; else row.denseRank = rank;
        }
    }

    private static final class Mutable<T> {
        final String key;
        final T value;
        double score;
        Integer lexicalRank;
        Integer denseRank;

        Mutable(String key, T value) { this.key = key; this.value = value; }
    }
}
