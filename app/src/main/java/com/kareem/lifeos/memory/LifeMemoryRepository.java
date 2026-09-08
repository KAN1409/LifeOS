package com.kareem.lifeos.memory;

import java.util.List;

/** Storage boundary for the LifeOS durable-memory engine. */
public interface LifeMemoryRepository {
    long remember(String subjectEntityId, String text, MemoryRecord.Category category,
                  float[] embedding, String sourceAssertionId, List<String> evidenceIds, long now);
    List<MemoryRecord> recall(String query, float[] queryEmbedding, int topK, long now);
    List<MemoryRecord> hotForSubject(String subjectEntityId, int limit);
    List<MemoryRecord> searchable();
    List<MemoryRecord> recentEpisodic(long since, int limit);
    boolean hasSimilar(String text, String subjectEntityId);
    PersistentLifeMemoryStore.DecaySummary runDecay(long now);
    int forgetBySubstring(String query, String subjectEntityId);
    void eraseAll();
}
