package com.kareem.lifeos.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Source-neutral port/adaptation of the durable-memory mechanics in
 * adgapar/teya MemoryManager.kt (Apache-2.0).
 *
 * Preserved behavior: category-specific forgetting curves, HOT/COLD tiers,
 * episodic death threshold, cosine retrieval with keyword fallback, and
 * recall reinforcement. LifeOS adds evidence provenance at the record layer.
 */
public final class MemoryAlgorithms {
    public static final float MIN_SIMILARITY = 0.35f;
    public static final float HOT_THRESHOLD = 0.50f;
    public static final float DEAD_THRESHOLD = 0.05f;

    private MemoryAlgorithms() {}

    public static double halfLifeDays(MemoryRecord.Category category) {
        if (category == MemoryRecord.Category.EPISODIC) return 3.0;
        if (category == MemoryRecord.Category.PREFERENCE) return 45.0;
        if (category == MemoryRecord.Category.ROUTINE) return 120.0;
        return 3650.0;
    }

    public static float strengthNow(MemoryRecord record, long now) {
        if (record == null) return 0f;
        double elapsedDays = Math.max(0L, now - record.lastAccessedAt) / 86_400_000.0;
        double strength = Math.pow(0.5, elapsedDays / halfLifeDays(record.category));
        return (float)Math.max(0.0, Math.min(1.0, strength));
    }

    public static MemoryRecord.Tier tierFor(float strength) {
        return strength >= HOT_THRESHOLD ? MemoryRecord.Tier.HOT : MemoryRecord.Tier.COLD;
    }

    public static boolean shouldPrune(MemoryRecord record, long now) {
        return record != null && record.category == MemoryRecord.Category.EPISODIC
                && strengthNow(record, now) < DEAD_THRESHOLD;
    }

    /** Semantic-first retrieval plus keyword fallback, de-duplicated by durable record id. */
    public static List<MemoryRecord> rank(List<MemoryRecord> pool, String query,
                                          float[] queryEmbedding, int topK) {
        if (pool == null || pool.isEmpty() || topK <= 0) return Collections.emptyList();
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isEmpty() && queryEmbedding == null) return Collections.emptyList();

        List<Scored> semantic = new ArrayList<Scored>();
        if (queryEmbedding != null) {
            for (MemoryRecord record : pool) {
                if (record == null || record.embedding == null) continue;
                float similarity = cosine(queryEmbedding, record.embedding);
                if (similarity >= MIN_SIMILARITY) semantic.add(new Scored(record, similarity));
            }
            Collections.sort(semantic, new Comparator<Scored>() {
                @Override public int compare(Scored a, Scored b) {
                    return Float.compare(b.score, a.score);
                }
            });
        }

        LinkedHashMap<Long,MemoryRecord> merged = new LinkedHashMap<Long,MemoryRecord>();
        for (Scored hit : semantic) merged.put(hit.record.id, hit.record);
        if (!normalizedQuery.isEmpty()) {
            for (MemoryRecord record : pool) {
                if (record == null) continue;
                String text = normalize(record.text);
                if (text.contains(normalizedQuery)) merged.put(record.id, record);
            }
        }

        List<MemoryRecord> out = new ArrayList<MemoryRecord>();
        for (MemoryRecord record : merged.values()) {
            out.add(record);
            if (out.size() >= topK) break;
        }
        return out;
    }

    public static boolean similarText(String left, String right) {
        String a = normalize(left), b = normalize(right);
        return !a.isEmpty() && !b.isEmpty() && (a.equals(b) || a.contains(b) || b.contains(a));
    }

    public static float cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || a.length != b.length) return -1f;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return -1f;
        return (float)(dot / (Math.sqrt(na) * Math.sqrt(nb)));
    }

    public static String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N} ]", " ")
                .replaceAll("\\s+", " ").trim();
    }

    private static final class Scored {
        final MemoryRecord record; final float score;
        Scored(MemoryRecord record, float score) { this.record = record; this.score = score; }
    }
}
