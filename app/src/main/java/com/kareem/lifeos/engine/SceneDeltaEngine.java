package com.kareem.lifeos.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Detects event candidates from state transitions instead of treating a visible scene as new history. */
public final class SceneDeltaEngine {
    private final Map<String,List<EventEvidence>> previousByContext=new HashMap<String,List<EventEvidence>>();

    public List<EventEvidence> observe(String context,List<EventEvidence> visible){
        String key=EventEvidence.normalized(context);
        List<EventEvidence> current=visible==null?Collections.<EventEvidence>emptyList():new ArrayList<EventEvidence>(visible);
        List<EventEvidence> previous=previousByContext.put(key,current);
        if(previous==null)return Collections.emptyList(); // The first scene is context, not an event.

        Map<String,Integer> remaining=new HashMap<String,Integer>();
        for(EventEvidence e:previous)if(e!=null){String signature=e.sceneSignature();Integer count=remaining.get(signature);remaining.put(signature,count==null?1:count+1);}
        List<EventEvidence> added=new ArrayList<EventEvidence>();
        for(EventEvidence e:current)if(e!=null){String signature=e.sceneSignature();Integer count=remaining.get(signature);if(count==null||count==0)added.add(e);else remaining.put(signature,count-1);}
        return Collections.unmodifiableList(added);
    }
}
