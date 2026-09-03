package com.kareem.lifeos.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Replaceable semantic interpretation derived from immutable raw evidence.
 * Assertions can be replayed when the semantic engine changes; they never mutate raw observations.
 */
public final class SemanticAssertion {
    public enum State { ASSERTED, RETRACTED }

    public final String assertionId;
    public final String subjectEntityId;
    public final String predicate;
    public final String value;
    public final State state;
    public final long observedAt;
    public final long validFrom;
    public final long validTo;
    public final double confidence;
    public final List<String> evidenceIds;

    public SemanticAssertion(String assertionId, String subjectEntityId, String predicate,
                             String value, State state, long observedAt, long validFrom,
                             long validTo, double confidence, List<String> evidenceIds) {
        this.assertionId = safe(assertionId);
        this.subjectEntityId = safe(subjectEntityId);
        this.predicate = safe(predicate);
        this.value = safe(value);
        this.state = state == null ? State.ASSERTED : state;
        this.observedAt = observedAt;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
        this.evidenceIds = Collections.unmodifiableList(evidenceIds == null ?
                new ArrayList<String>() : new ArrayList<String>(evidenceIds));
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
