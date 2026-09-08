package com.kareem.lifeos.context;

/** Immutable request envelope for a replaceable Deep Brain implementation. */
public final class DeepBrainRequest {
    public final String requestId;
    public final long requestedAt;
    public final LifeModelSnapshot lifeModel;

    public DeepBrainRequest(String requestId, long requestedAt, LifeModelSnapshot lifeModel) {
        this.requestId = requestId == null ? "" : requestId;
        this.requestedAt = requestedAt;
        this.lifeModel = lifeModel;
    }
}
