package com.kareem.lifeos.memory;

import com.kareem.lifeos.context.SemanticAssertion;

/**
 * Explicit boundary from replayable semantics into durable memory.
 * Nothing is memorized merely because raw text exists.
 */
public final class LifeMemoryBridge {
    private final LifeMemoryRepository repository;

    public LifeMemoryBridge(LifeMemoryRepository repository) { this.repository = repository; }

    /**
     * Materialize one grounded assertion as durable memory.
     * Category is explicit: the bridge does not guess memory meaning from source text.
     */
    public long remember(SemanticAssertion assertion, MemoryRecord.Category category,
                         float[] embedding, long now) {
        if (assertion == null || assertion.state != SemanticAssertion.State.ASSERTED) return -1L;
        if (assertion.assertionId.isEmpty() || assertion.subjectEntityId.isEmpty()
                || assertion.value.trim().isEmpty() || assertion.evidenceIds.isEmpty()) return -1L;
        if (repository.hasSimilar(assertion.value, assertion.subjectEntityId)) return -1L;
        return repository.remember(assertion.subjectEntityId, assertion.value, category, embedding,
                assertion.assertionId, assertion.evidenceIds, now);
    }
}
