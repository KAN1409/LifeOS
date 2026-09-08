package com.kareem.lifeos.retrieval;

import com.kareem.lifeos.memory.MemoryAlgorithms;
import com.kareem.lifeos.memory.MemoryRecord;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * LifeOS hybrid recall: Teya-style durable memory + Open Intelligence SecondBrain-style RRF.
 *
 * Lexical and dense channels are ranked independently, then fused by durable memory id.
 * Exact words/names therefore remain strong while semantically similar memories can still surface.
 */
public final class HybridMemoryRecall {
    private HybridMemoryRecall() {}

    public static List<MemoryRecord> rank(List<MemoryRecord> pool, String query,
                                          float[] queryEmbedding, int topK) {
        if (pool == null || pool.isEmpty() || topK <= 0) return Collections.emptyList();
        List<MemoryRecord> lexical = lexical(pool, query);
        List<MemoryRecord> dense = dense(pool, queryEmbedding);
        if (lexical.isEmpty() && dense.isEmpty()) return Collections.emptyList();

        List<RrfFusion.Fused<MemoryRecord>> fused = RrfFusion.fuse(lexical, dense,
                new RrfFusion.Keyer<MemoryRecord>() {
                    @Override public String key(MemoryRecord value) {
                        return Long.toString(value.id);
                    }
                });
        List<MemoryRecord> out = new ArrayList<MemoryRecord>();
        for (RrfFusion.Fused<MemoryRecord> hit : fused) {
            out.add(hit.value);
            if (out.size() >= topK) break;
        }
        return out;
    }

    static List<MemoryRecord> lexical(List<MemoryRecord> pool, String query) {
        final Set<String> queryTokens = tokens(query);
        final String normalizedQuery = MemoryAlgorithms.normalize(query);
        if (normalizedQuery.isEmpty()) return Collections.emptyList();
        List<Scored> scored = new ArrayList<Scored>();
        for (MemoryRecord record : pool) {
            if (record == null) continue;
            String text = MemoryAlgorithms.normalize(record.text);
            if (text.isEmpty()) continue;
            Set<String> textTokens = tokens(text);
            int overlap = 0;
            for (String token : queryTokens) if (textTokens.contains(token)) overlap++;
            boolean contains = text.contains(normalizedQuery);
            if (!contains && overlap == 0) continue;
            double coverage = queryTokens.isEmpty() ? 0.0 : ((double) overlap / queryTokens.size());
            double score = (contains ? 2.0 : 0.0) + coverage + (record.strength * 0.05);
            scored.add(new Scored(record, score));
        }
        return sorted(scored);
    }

    static List<MemoryRecord> dense(List<MemoryRecord> pool, float[] queryEmbedding) {
        if (queryEmbedding == null || queryEmbedding.length == 0) return Collections.emptyList();
        List<Scored> scored = new ArrayList<Scored>();
        for (MemoryRecord record : pool) {
            if (record == null || record.embedding == null) continue;
            float similarity = MemoryAlgorithms.cosine(queryEmbedding, record.embedding);
            if (similarity >= MemoryAlgorithms.MIN_SIMILARITY) {
                scored.add(new Scored(record, similarity));
            }
        }
        return sorted(scored);
    }

    private static List<MemoryRecord> sorted(List<Scored> scored) {
        Collections.sort(scored, new Comparator<Scored>() {
            @Override public int compare(Scored a, Scored b) {
                int byScore = Double.compare(b.score, a.score);
                if (byScore != 0) return byScore;
                return Long.compare(b.record.lastAccessedAt, a.record.lastAccessedAt);
            }
        });
        List<MemoryRecord> out = new ArrayList<MemoryRecord>();
        for (Scored hit : scored) out.add(hit.record);
        return out;
    }

    private static Set<String> tokens(String value) {
        Set<String> out = new HashSet<String>();
        String normalized = MemoryAlgorithms.normalize(value);
        for (String token : normalized.split("\\s+")) if (!token.isEmpty()) out.add(token);
        return out;
    }

    private static final class Scored {
        final MemoryRecord record; final double score;
        Scored(MemoryRecord record, double score) { this.record = record; this.score = score; }
    }
}
