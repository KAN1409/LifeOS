package com.kareem.lifeos.engine;

import java.util.Locale;

/** Source-neutral evidence. Sensors describe observations; they do not create canonical events. */
public final class EventEvidence {
    public final String source;
    public final String sourceInstance;
    public final String context;
    public final String kind;
    public final MessageObservation.Direction direction;
    public final String content;
    public final long observedAt;
    public final double confidence;

    public EventEvidence(String source,String sourceInstance,String context,String kind,
                         MessageObservation.Direction direction,String content,long observedAt,double confidence){
        this.source=clean(source);
        this.sourceInstance=clean(sourceInstance);
        this.context=clean(context);
        this.kind=clean(kind).isEmpty()?"UNKNOWN":clean(kind);
        this.direction=direction==null?MessageObservation.Direction.UNKNOWN:direction;
        this.content=content==null?"":content.trim();
        this.observedAt=observedAt;
        this.confidence=Math.max(0.0,Math.min(1.0,confidence));
    }

    public static EventEvidence fromScreen(MessageObservation m){
        if(m==null)return null;
        String instance=m.left+":"+m.top+":"+m.right+":"+m.bottom;
        return new EventEvidence("SCREEN",instance,m.thread,m.type,m.direction,m.text,m.observedAt,m.confidence);
    }

    public static EventEvidence fromNotification(NotificationObservation n){
        if(n==null)return null;
        return new EventEvidence("NOTIFICATION",notificationPost(n.evidenceId),n.thread,n.type,
                MessageObservation.Direction.UNKNOWN,n.text,n.observedAt,n.confidence);
    }

    /** Equality inside one visible scene. Occurrence counts preserve repeated identical events. */
    String sceneSignature(){
        return normalized(context)+"|"+normalized(kind)+"|"+direction.name()+"|"+normalized(content);
    }

    private static String notificationPost(String id){
        String value=clean(id);int line=value.indexOf("|line|");int body=value.indexOf("|body|");
        int cut=line>=0?line:body;return cut>=0?value.substring(0,cut):value;
    }
    static String normalized(String value){return clean(value).toLowerCase(Locale.ROOT).replaceAll("\\s+"," ");}
    private static String clean(String value){return value==null?"":value.trim();}
}
