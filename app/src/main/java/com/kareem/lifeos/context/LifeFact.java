package com.kareem.lifeos.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Evidence-backed semantic fact. Facts are revisions, not mutable truth rows;
 * current state is rebuilt from the revision history.
 */
public final class LifeFact {
    public enum State { ASSERTED, RETRACTED }

    public final String factId;
    public final String subjectEntityId;
    public final String predicate;
    public final String value;
    public final State state;
    public final long observedAt;
    public final long validFrom;
    public final long validTo;
    public final double confidence;
    public final List<String> evidenceIds;

    public LifeFact(String factId, String subjectEntityId, String predicate, String value,
                    State state, long observedAt, long validFrom, long validTo,
                    double confidence, List<String> evidenceIds) {
        this.factId = safe(factId);
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

    public String logicalKey() { return subjectEntityId + "|" + predicate; }

    private static String safe(String value) { return value == null ? "" : value; }
}
