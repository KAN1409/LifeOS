package com.kareem.lifeos.engine;

public final class ScreenState {
    public enum Type { CONVERSATION, CONVERSATION_LIST, SEARCH, CHANNEL, COMMUNITY, SYSTEM_DIALOG, UNKNOWN }
    public final Type type;
    public final double confidence;
    public ScreenState(Type type,double confidence){this.type=type==null?Type.UNKNOWN:type;this.confidence=Math.max(0,Math.min(1,confidence));}
}
