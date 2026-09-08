package com.kareem.lifeos.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Durable LifeOS memory record.
 *
 * Algorithmically adapted from the memory model in adgapar/teya (Apache-2.0),
 * but made source-neutral and stricter by retaining LifeOS semantic/evidence provenance.
 */
public final class MemoryRecord {
    public enum Category { FACT, PREFERENCE, ROUTINE, EPISODIC }
    public enum Tier { HOT, COLD }

    public final long id;
    public final String subjectEntityId;
    public final String text;
    public final Category category;
    public final long addedAt;
    public final long lastAccessedAt;
    public final float strength;
    public final Tier tier;
    public final float[] embedding;
    public final String sourceAssertionId;
    public final List<String> evidenceIds;

    public MemoryRecord(long id, String subjectEntityId, String text, Category category,
                        long addedAt, long lastAccessedAt, float strength, Tier tier,
                        float[] embedding, String sourceAssertionId, List<String> evidenceIds) {
        this.id = id;
        this.subjectEntityId = safe(subjectEntityId);
        this.text = safe(text);
        this.category = category == null ? Category.FACT : category;
        this.addedAt = addedAt;
        this.lastAccessedAt = lastAccessedAt;
        this.strength = clamp(strength);
        this.tier = tier == null ? Tier.HOT : tier;
        this.embedding = embedding == null ? null : embedding.clone();
        this.sourceAssertionId = safe(sourceAssertionId);
        this.evidenceIds = Collections.unmodifiableList(evidenceIds == null
                ? new ArrayList<String>() : new ArrayList<String>(evidenceIds));
    }

    public MemoryRecord withRecall(long at) {
        return new MemoryRecord(id, subjectEntityId, text, category, addedAt, at,
                1.0f, Tier.HOT, embedding, sourceAssertionId, evidenceIds);
    }

    private static String safe(String value) { return value == null ? "" : value; }
    private static float clamp(float value) { return Math.max(0f, Math.min(1f, value)); }
}
