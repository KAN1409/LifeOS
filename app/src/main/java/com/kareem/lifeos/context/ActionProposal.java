package com.kareem.lifeos.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Proposed action only; creation never implies execution. */
public final class ActionProposal {
    public enum Risk { READ_ONLY, REVERSIBLE_WRITE, IRREVERSIBLE_WRITE, SENSITIVE }

    public final String proposalId;
    public final String situationId;
    public final String actionType;
    public final String target;
    public final String payloadSummary;
    public final Risk risk;
    public final String idempotencyKey;
    public final List<String> evidenceIds;

    public ActionProposal(String proposalId, String situationId, String actionType, String target,
                          String payloadSummary, Risk risk, String idempotencyKey,
                          List<String> evidenceIds) {
        this.proposalId = safe(proposalId);
        this.situationId = safe(situationId);
        this.actionType = safe(actionType);
        this.target = safe(target);
        this.payloadSummary = safe(payloadSummary);
        this.risk = risk == null ? Risk.SENSITIVE : risk;
        this.idempotencyKey = safe(idempotencyKey);
        this.evidenceIds = Collections.unmodifiableList(evidenceIds == null ?
                new ArrayList<String>() : new ArrayList<String>(evidenceIds));
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
