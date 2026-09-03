package com.kareem.lifeos.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One canonical event assembled from one or more evidence sources. */
public final class CanonicalEvent {
    public final String type;
    public final MessageObservation.Direction direction;
    public final String text;
    public final long observedAt;
    public final double confidence;
    public final List<String> sources;

    public CanonicalEvent(String type,MessageObservation.Direction direction,String text,long observedAt,double confidence,List<String> sources){
        this.type=type==null?"":type;
        this.direction=direction==null?MessageObservation.Direction.UNKNOWN:direction;
        this.text=text==null?"":text;
        this.observedAt=observedAt;
        this.confidence=Math.max(0.0,Math.min(1.0,confidence));
        this.sources=Collections.unmodifiableList(new ArrayList<String>(sources==null?Collections.<String>emptyList():sources));
    }

    public boolean merged(){return sources.size()>1;}
}
