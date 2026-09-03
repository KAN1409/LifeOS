package com.kareem.lifeos.memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Adaptation of Teya's nightly "dream" consolidation workflow (Apache-2.0).
 *
 * Teya promotes recent episodic notes into durable facts/preferences/routines and
 * guards against duplicates. LifeOS adds a hard grounding rule: every candidate
 * must cite existing episodic memory IDs, and their raw evidence provenance is
 * carried into the promoted memory.
 */
public final class GroundedMemoryConsolidator {
    private final LifeMemoryRepository repository;
    private final MemoryConsolidationClient client;

    public GroundedMemoryConsolidator(LifeMemoryRepository repository,
                                      MemoryConsolidationClient client) {
        this.repository = repository;
        this.client = client;
    }

    public Result run(long since, int maxEpisodes, long now) {
        if (repository == null || client == null) return new Result(0,0,0);
        List<MemoryRecord> episodes = repository.recentEpisodic(since, maxEpisodes);
        if (episodes == null || episodes.isEmpty()) return new Result(0,0,0);

        Map<Long,MemoryRecord> byId = new HashMap<Long,MemoryRecord>();
        for (MemoryRecord e : episodes) if (e != null) byId.put(e.id, e);
        List<MemoryConsolidationCandidate> candidates = client.consolidate(episodes);
        if (candidates == null || candidates.isEmpty()) return new Result(episodes.size(),0,0);

        int promoted = 0, rejected = 0;
        for (MemoryConsolidationCandidate candidate : candidates) {
            if (candidate == null || candidate.text.isEmpty()
                    || candidate.sourceMemoryIds.isEmpty()
                    || candidate.category == MemoryRecord.Category.EPISODIC) {
                rejected++; continue;
            }
            LinkedHashSet<String> evidence = new LinkedHashSet<String>();
            List<Long> validSources = new ArrayList<Long>();
            boolean subjectGrounded = false;
            for (Long sourceId : candidate.sourceMemoryIds) {
                if (sourceId == null) continue;
                MemoryRecord source = byId.get(sourceId);
                if (source == null || source.category != MemoryRecord.Category.EPISODIC) continue;
                validSources.add(sourceId);
                evidence.addAll(source.evidenceIds);
                if (!candidate.subjectEntityId.isEmpty()
                        && candidate.subjectEntityId.equals(source.subjectEntityId)) subjectGrounded = true;
            }
            if (validSources.isEmpty() || evidence.isEmpty() || !subjectGrounded) {
                rejected++; continue;
            }
            if (repository.hasSimilar(candidate.text, candidate.subjectEntityId)) continue;
            String assertionId = "memory-consolidation|" + join(validSources);
            long id = repository.remember(candidate.subjectEntityId, candidate.text,
                    candidate.category, candidate.embedding, assertionId,
                    new ArrayList<String>(evidence), now);
            if (id > 0) promoted++; else rejected++;
        }
        return new Result(episodes.size(), promoted, rejected);
    }

    private static String join(List<Long> ids) {
        StringBuilder out = new StringBuilder();
        for (Long id : ids) { if (out.length() > 0) out.append(','); out.append(id); }
        return out.toString();
    }

    public static final class Result {
        public final int reviewed, promoted, rejected;
        Result(int reviewed, int promoted, int rejected) {
            this.reviewed=reviewed; this.promoted=promoted; this.rejected=rejected;
        }
    }
}
