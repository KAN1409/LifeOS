package com.kareem.lifeos.context;

/** Explicit user/system approval for one concrete proposal. */
public final class ActionApproval {
    public final String proposalId;
    public final boolean approved;
    public final long approvedAt;

    public ActionApproval(String proposalId, boolean approved, long approvedAt) {
        this.proposalId = proposalId == null ? "" : proposalId;
        this.approved = approved;
        this.approvedAt = approvedAt;
    }
}
