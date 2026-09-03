package com.kareem.lifeos.memory;

import java.util.List;

/** Storage boundary for the LifeOS durable-memory engine. */
public interface LifeMemoryRepository {
    long remember(String subjectEntityId, String text, MemoryRecord.Category category,
                  float[] embedding, String sourceAssertionId, List<String> evidenceIds, long now);
    boolean hasSimilar(String text, String subjectEntityId);
}
