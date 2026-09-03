package com.kareem.lifeos.engine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class GenericConversationParser {
    public ConversationSnapshot parse(RawScreenSnapshot s){
        if(s==null||s.nodes==null||s.nodes.isEmpty()) return new ConversationSnapshot(new ScreenState(ScreenState.Type.UNKNOWN,0.0),"",new ArrayList<ParsedEvent>());
        List<RawNode> nodes=new ArrayList<RawNode>(s.nodes);
        Collections.sort(nodes,new Comparator<RawNode>(){ public int compare(RawNode a,RawNode b){ int d=a.top-b.top; return d!=0?d:a.left-b.left; }});
        boolean composer=false; int textCount=0;
        for(RawNode n:nodes){ if(n.text.length()>0) textCount++; String c=n.className.toLowerCase(); if(c.contains("edittext")) composer=true; }
        ScreenState state=new ScreenState(composer&&textCount>=3?ScreenState.Type.CONVERSATION:ScreenState.Type.UNKNOWN,composer?0.78:0.3);
        List<ParsedEvent> out=new ArrayList<ParsedEvent>();
        if(state.type==ScreenState.Type.CONVERSATION){
            for(RawNode n:nodes){
                String t=n.text.trim(); if(t.length()==0) continue;
                ParsedEvent.Kind kind=isCenteredMarker(n,s)?ParsedEvent.Kind.SYSTEM_MARKER:ParsedEvent.Kind.MESSAGE;
                ParsedEvent.Direction dir=ParsedEvent.Direction.UNKNOWN;
                if(kind==ParsedEvent.Kind.MESSAGE){ double center=(n.left+n.right)/2.0; dir=center>s.width*0.58?ParsedEvent.Direction.OUT:(center<s.width*0.42?ParsedEvent.Direction.IN:ParsedEvent.Direction.UNKNOWN); }
                out.add(new ParsedEvent(kind,dir,t,kind==ParsedEvent.Kind.MESSAGE?0.70:0.66));
            }
        }
        return new ConversationSnapshot(state,"",out);
    }
    private boolean isCenteredMarker(RawNode n,RawScreenSnapshot s){ double center=(n.left+n.right)/2.0; double w=n.right-n.left; return Math.abs(center-s.width/2.0)<s.width*0.10 && w<s.width*0.65; }
}
