package com.kareem.lifeos.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Proposed durable memory distilled from one or more episodic records. */
public final class MemoryConsolidationCandidate {
    public final String subjectEntityId;
    public final String text;
    public final MemoryRecord.Category category;
    public final float[] embedding;
    public final List<Long> sourceMemoryIds;

    public MemoryConsolidationCandidate(String subjectEntityId, String text,
                                        MemoryRecord.Category category, float[] embedding,
                                        List<Long> sourceMemoryIds) {
        this.subjectEntityId = subjectEntityId == null ? "" : subjectEntityId;
        this.text = text == null ? "" : text.trim();
        this.category = category == null ? MemoryRecord.Category.FACT : category;
        this.embedding = embedding == null ? null : embedding.clone();
        this.sourceMemoryIds = Collections.unmodifiableList(sourceMemoryIds == null
                ? new ArrayList<Long>() : new ArrayList<Long>(sourceMemoryIds));
    }
}
