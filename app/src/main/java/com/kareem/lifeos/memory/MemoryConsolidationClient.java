package com.kareem.lifeos.memory;

import java.util.List;

/** Model/transport boundary for episodic -> durable memory consolidation. */
public interface MemoryConsolidationClient {
    List<MemoryConsolidationCandidate> consolidate(List<MemoryRecord> episodicMemories);
}
