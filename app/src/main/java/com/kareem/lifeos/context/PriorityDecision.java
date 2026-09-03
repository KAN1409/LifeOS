package com.kareem.lifeos.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** User-visible priority conclusion, not private chain-of-thought. */
public final class PriorityDecision {
    public final String decisionId;
    public final String situationId;
    public final int rank;
    public final String title;
    public final String rationale;
    public final double confidence;
    public final List<String> evidenceIds;
    public final List<String> suggestedActions;

    public PriorityDecision(String decisionId, String situationId, int rank, String title,
                            String rationale, double confidence, List<String> evidenceIds,
                            List<String> suggestedActions) {
        this.decisionId = safe(decisionId);
        this.situationId = safe(situationId);
        this.rank = Math.max(1, rank);
        this.title = safe(title);
        this.rationale = safe(rationale);
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
        this.evidenceIds = Collections.unmodifiableList(evidenceIds == null ?
                new ArrayList<String>() : new ArrayList<String>(evidenceIds));
        this.suggestedActions = Collections.unmodifiableList(suggestedActions == null ?
                new ArrayList<String>() : new ArrayList<String>(suggestedActions));
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
