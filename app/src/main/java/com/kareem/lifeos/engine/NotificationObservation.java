package com.kareem.lifeos.engine;

/** Notification-derived message evidence used only by the M1 shadow reconciler. */
public final class NotificationObservation {
    public final String evidenceId;
    public final String type="MESSAGE";
    public final String source="NOTIFICATION";
    public final String thread;
    public final MessageObservation.Direction direction;
    public final String text;
    public final long observedAt;
    public final double confidence;

    public NotificationObservation(String thread,String text,long observedAt,double confidence){
        this("",thread,text,observedAt,confidence);
    }

    public NotificationObservation(String evidenceId,String thread,String text,long observedAt,double confidence){
        this.evidenceId=evidenceId==null?"":evidenceId;
        this.thread=thread==null?"":thread;
        this.text=text==null?"":text;
        this.observedAt=observedAt;
        this.direction=MessageObservation.Direction.IN;
        this.confidence=Math.max(0.0,Math.min(1.0,confidence));
    }
}
