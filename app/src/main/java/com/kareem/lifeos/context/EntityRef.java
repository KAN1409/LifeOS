package com.kareem.lifeos.context;

/** Lightweight canonical entity reference derived from evidence, never from source-specific UI rules. */
public final class EntityRef {
    public enum Kind { PERSON, CONVERSATION, APPLICATION, DEVICE, UNKNOWN }

    public final String entityId;
    public final Kind kind;
    public final String label;
    public final double confidence;

    public EntityRef(String entityId, Kind kind, String label, double confidence) {
        this.entityId = safe(entityId);
        this.kind = kind == null ? Kind.UNKNOWN : kind;
        this.label = safe(label);
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
