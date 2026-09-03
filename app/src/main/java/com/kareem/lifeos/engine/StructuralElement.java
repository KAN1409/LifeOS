package com.kareem.lifeos.engine;

/** One structure-level interpretation of a raw accessibility node. */
public final class StructuralElement {
    public enum Role {
        TOP_BAR,
        SCROLL_REGION,
        COMPOSER,
        MESSAGE_CANDIDATE,
        CENTER_MARKER,
        ACTION,
        OTHER
    }

    public final Role role;
    public final RawNode node;
    public final double confidence;

    public StructuralElement(Role role, RawNode node, double confidence){
        this.role=role==null?Role.OTHER:role;
        this.node=node;
        this.confidence=Math.max(0.0,Math.min(1.0,confidence));
    }
}
