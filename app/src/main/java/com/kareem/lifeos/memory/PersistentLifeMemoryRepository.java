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

    @Override public List<MemoryRecord> recall(String query, float[] queryEmbedding, int topK, long now) {
        return store.recall(query, queryEmbedding, topK, now);
    }

    @Override public List<MemoryRecord> hotForSubject(String subjectEntityId, int limit) {
        return store.hotForSubject(subjectEntityId, limit);
    }

    @Override public List<MemoryRecord> searchable() { return store.searchable(); }

    @Override public List<MemoryRecord> recentEpisodic(long since, int limit) {
        return store.recentEpisodic(since, limit);
    }

    @Override public boolean hasSimilar(String text, String subjectEntityId) {
        return store.hasSimilar(text, subjectEntityId);
    }

    @Override public PersistentLifeMemoryStore.DecaySummary runDecay(long now) {
        return store.runDecay(now);
    }

    @Override public int forgetBySubstring(String query, String subjectEntityId) {
        return store.forgetBySubstring(query, subjectEntityId);
    }

    @Override public void eraseAll() { store.eraseAll(); }
}
