package com.kareem.lifeos.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Safety/grounding wrapper around any semantic model client.
 * Assertions are accepted only when they cite the current raw observation and carry a predicate.
 */
public final class GroundedSemanticInterpreter implements SemanticInterpreter {
    private final String version;
    private final SemanticModelClient client;

    public GroundedSemanticInterpreter(String version, SemanticModelClient client) {
        if (client == null) throw new IllegalArgumentException("client required");
        this.version = version == null ? "semantic-model" : version;
        this.client = client;
    }

    @Override public String version() { return version; }

    @Override public List<SemanticAssertion> interpret(RawObservation observation) {
        if (observation == null) return Collections.emptyList();
        List<EntityRef> entities = EntityResolver.resolve(observation);
        List<SemanticAssertion> proposed = client.analyze(observation, entities);
        if (proposed == null || proposed.isEmpty()) return Collections.emptyList();

        List<SemanticAssertion> accepted = new ArrayList<SemanticAssertion>();
        for (SemanticAssertion a : proposed) {
            if (a == null || a.predicate.trim().isEmpty()) continue;
            if (!a.evidenceIds.contains(observation.observationId)) continue;
            if (a.observedAt != observation.observedAt) continue;
            accepted.add(a);
        }
        return accepted;
    }
}
