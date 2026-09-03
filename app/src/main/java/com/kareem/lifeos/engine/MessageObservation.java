package com.kareem.lifeos.engine;

/** Canonical screen-derived message observation produced in M1 shadow mode. */
public final class MessageObservation {
    public enum Direction { IN, OUT, UNKNOWN }

    public final String type="MESSAGE";
    public final String source="SCREEN";
    public final Direction direction;
    public final String text;
    public final int left,top,right,bottom;
    public final double confidence;

    public MessageObservation(Direction direction,String text,int left,int top,int right,int bottom,double confidence){
        this.direction=direction==null?Direction.UNKNOWN:direction;
        this.text=text==null?"":text;
        this.left=left;this.top=top;this.right=right;this.bottom=bottom;
        this.confidence=Math.max(0.0,Math.min(1.0,confidence));
    }
}
