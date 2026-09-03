package com.kareem.lifeos.engine;

/** Conservative metadata evidence associated structurally, without guessing semantics from words. */
public final class MessageMetadataEvidence {
    public enum Kind { SYSTEM_MARKER, ADJACENT_DECORATION, UNKNOWN }
    public enum Relation { BEFORE, AFTER, OVERLAPS, UNASSOCIATED }
    public final Kind kind;
    public final Relation relation;
    public final String text;
    public final int nodeId;
    public final int targetMessageIndex;
    public final double confidence;
    public MessageMetadataEvidence(Kind kind,Relation relation,String text,int nodeId,int targetMessageIndex,double confidence){
        this.kind=kind==null?Kind.UNKNOWN:kind;
        this.relation=relation==null?Relation.UNASSOCIATED:relation;
        this.text=text==null?"":text;
        this.nodeId=nodeId;
        this.targetMessageIndex=targetMessageIndex;
        this.confidence=Math.max(0.0,Math.min(1.0,confidence));
    }
}
