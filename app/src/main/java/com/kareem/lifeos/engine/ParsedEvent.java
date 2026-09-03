package com.kareem.lifeos.engine;
public final class ParsedEvent {
    public enum Kind { MESSAGE, REACTION, MEDIA, SYSTEM_MARKER }
    public enum Direction { IN, OUT, NONE, UNKNOWN }
    public final Kind kind; public final Direction direction; public final String text; public final double confidence;
    public ParsedEvent(Kind kind,Direction direction,String text,double confidence){this.kind=kind;this.direction=direction;this.text=text;this.confidence=confidence;}
}
