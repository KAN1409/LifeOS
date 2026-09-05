package com.kareem.lifeos.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Grounding boundary around any Deep Brain implementation.
 * Invalid situation/evidence references are discarded before surfacing decisions.
 */
public final class GroundedDeepBrain {
    private final DeepBrainClient client;

    public GroundedDeepBrain(DeepBrainClient client) {
        if (client == null) throw new IllegalArgumentException("client required");
        this.client = client;
    }

    public DeepBrainResult analyze(DeepBrainRequest request) {
        if (request == null || request.lifeModel == null) {
            return new DeepBrainResult(request == null ? "" : request.requestId, "", Collections.<PriorityDecision>emptyList());
        }
        DeepBrainResult proposed = client.analyze(request);
        if (proposed == null || !request.requestId.equals(proposed.requestId)) {
            return new DeepBrainResult(request.requestId, "", Collections.<PriorityDecision>emptyList());
        }

        Map<String,Set<String>> situationEvidence = evidenceBySituation(request.lifeModel.situations);
        List<PriorityDecision> valid = new ArrayList<PriorityDecision>();
        for (PriorityDecision p : proposed.priorities) {
            if (p == null) continue;
            Set<String> allowed = situationEvidence.get(p.situationId);
            if (allowed == null) continue;
            if (!allowed.containsAll(p.evidenceIds)) continue;
            if (p.title.trim().isEmpty()) continue;
            valid.add(p);
        }
        Collections.sort(valid, new Comparator<PriorityDecision>() {
            @Override public int compare(PriorityDecision a, PriorityDecision b) {
                int r = Integer.compare(a.rank, b.rank);
                return r != 0 ? r : a.decisionId.compareTo(b.decisionId);
            }
        });

        List<PriorityDecision> ranked = new ArrayList<PriorityDecision>();
        for (int i = 0; i < valid.size(); i++) {
            PriorityDecision p = valid.get(i);
            ranked.add(new PriorityDecision(p.decisionId, p.situationId, i + 1, p.title,
                    p.rationale, p.confidence, p.evidenceIds, p.suggestedActions));
        }
        return new DeepBrainResult(request.requestId, proposed.answer, ranked);
    }

    private static Map<String,Set<String>> evidenceBySituation(List<Situation> situations) {
        Map<String,Set<String>> out = new HashMap<String,Set<String>>();
        if (situations == null) return out;
        for (Situation s : situations) {
            if (s == null || s.situationId.isEmpty()) continue;
            Set<String> evidence = new HashSet<String>();
            for (Episode episode : s.episodes) {
                if (episode == null) continue;
                for (ContextEvent event : episode.events) {
                    if (event != null) evidence.addAll(event.evidenceIds);
                }
            }
            out.put(s.situationId, evidence);
        }
        return out;
    }
}
