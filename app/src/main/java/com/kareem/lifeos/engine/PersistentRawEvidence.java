package com.kareem.lifeos.engine;

/** One persisted raw row loaded from the isolated understanding database for replay. */
public final class PersistentRawEvidence {
    public final long id;
    public final String source;
    public final String thread;
    public final String type;
    public final MessageObservation.Direction direction;
    public final String text;
    public final long observedAt;
    public final double confidence;
    public final String payload;

    public PersistentRawEvidence(long id,String source,String thread,String type,MessageObservation.Direction direction,String text,long observedAt,double confidence,String payload){
        this.id=id;
        this.source=source==null?"":source;
        this.thread=thread==null?"":thread;
        this.type=type==null?"":type;
        this.direction=direction==null?MessageObservation.Direction.UNKNOWN:direction;
        this.text=text==null?"":text;
        this.observedAt=observedAt;
        this.confidence=Math.max(0.0,Math.min(1.0,confidence));
        this.payload=payload==null?"":payload;
    }
}
