package com.kareem.lifeos.engine;
import java.util.List;
public final class ConversationSnapshot {
    public final ScreenState state; public final String title; public final List<ParsedEvent> events;
    public ConversationSnapshot(ScreenState state,String title,List<ParsedEvent> events){this.state=state;this.title=title;this.events=events;}
}
