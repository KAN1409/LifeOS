package com.kareem.lifeos.memory;

import java.util.List;

/** Thin adapter keeping the durable SQLite implementation behind a testable boundary. */
public final class PersistentLifeMemoryRepository implements LifeMemoryRepository {
    private final PersistentLifeMemoryStore store;

    public PersistentLifeMemoryRepository(PersistentLifeMemoryStore store) { this.store = store; }

    @Override public long remember(String subjectEntityId, String text,
                                   MemoryRecord.Category category, float[] embedding,
                                   String sourceAssertionId, List<String> evidenceIds, long now) {
        return store.remember(subjectEntityId, text, category, embedding,
                sourceAssertionId, evidenceIds, now);
    }

    @Override public boolean hasSimilar(String text, String subjectEntityId) {
        return store.hasSimilar(text, subjectEntityId);
    }
}
