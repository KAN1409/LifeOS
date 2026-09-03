package com.kareem.lifeos.engine;

import java.util.Locale;

/** Stable matching key foundation for later SCREEN + NOTIFICATION reconciliation. */
public final class ReconciliationKey {
    public final String thread;
    public final String type;
    public final MessageObservation.Direction direction;
    public final String normalizedText;
    public final long timeBucket;
    public ReconciliationKey(String thread,String type,MessageObservation.Direction direction,String text,long observedAt){
        this.thread=thread==null?"":thread.trim().toLowerCase(Locale.ROOT);
        this.type=type==null?"":type;
        this.direction=direction==null?MessageObservation.Direction.UNKNOWN:direction;
        this.normalizedText=normalize(text);
        this.timeBucket=observedAt<=0?0:observedAt/15000L;
    }
    public static ReconciliationKey fromMessage(String thread,MessageObservation m){return new ReconciliationKey(thread,m==null?"":m.type,m==null?MessageObservation.Direction.UNKNOWN:m.direction,m==null?"":m.text,m==null?0:m.observedAt);}
    public boolean compatibleWith(ReconciliationKey other){
        if(other==null)return false;
        if(!type.equals(other.type)||!normalizedText.equals(other.normalizedText))return false;
        if(!thread.isEmpty()&&!other.thread.isEmpty()&&!thread.equals(other.thread))return false;
        if(direction!=MessageObservation.Direction.UNKNOWN&&other.direction!=MessageObservation.Direction.UNKNOWN&&direction!=other.direction)return false;
        return timeBucket==0||other.timeBucket==0||Math.abs(timeBucket-other.timeBucket)<=1;
    }
    static String normalize(String s){return s==null?"":s.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+"," ");}
}
