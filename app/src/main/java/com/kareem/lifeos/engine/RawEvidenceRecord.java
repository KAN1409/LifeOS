package com.kareem.lifeos.engine;

/** Immutable raw evidence record retained independently from canonical interpretation. */
public final class RawEvidenceRecord {
    public final String source;
    public final String thread;
    public final String type;
    public final MessageObservation.Direction direction;
    public final String text;
    public final long observedAt;
    public final double confidence;

    public RawEvidenceRecord(String source,String thread,String type,MessageObservation.Direction direction,String text,long observedAt,double confidence){
        this.source=source==null?"":source;
        this.thread=thread==null?"":thread;
        this.type=type==null?"":type;
        this.direction=direction==null?MessageObservation.Direction.UNKNOWN:direction;
        this.text=text==null?"":text;
        this.observedAt=observedAt;
        this.confidence=Math.max(0.0,Math.min(1.0,confidence));
    }
}
