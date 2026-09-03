package com.kareem.lifeos.engine;

/** Persisted canonical event row for read-only UI/debug views. */
public final class CanonicalEventRecord {
    public final long id;
    public final String type;
    public final MessageObservation.Direction direction;
    public final String text;
    public final long observedAt;
    public final double confidence;
    public final String sources;

    public CanonicalEventRecord(long id,String type,MessageObservation.Direction direction,String text,long observedAt,double confidence,String sources){
        this.id=id;
        this.type=type==null?"":type;
        this.direction=direction==null?MessageObservation.Direction.UNKNOWN:direction;
        this.text=text==null?"":text;
        this.observedAt=observedAt;
        this.confidence=Math.max(0.0,Math.min(1.0,confidence));
        this.sources=sources==null?"":sources;
    }
}
