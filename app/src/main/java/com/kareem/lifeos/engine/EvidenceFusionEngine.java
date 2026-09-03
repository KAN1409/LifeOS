package com.kareem.lifeos.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Source-neutral evidence fusion. Compatibility is based on invariants, never app names or phrases. */
public final class EvidenceFusionEngine {
    private EvidenceFusionEngine(){}

    public static List<CanonicalEvent> fuse(List<EventEvidence> evidence){
        List<EventEvidence> input=collapseSensorRevisions(evidence);
        boolean[] used=new boolean[input.size()];List<CanonicalEvent> out=new ArrayList<CanonicalEvent>();
        for(int i=0;i<input.size();i++){
            if(used[i]||input.get(i)==null)continue;EventEvidence a=input.get(i);used[i]=true;
            int best=-1;double bestScore=0.0;
            for(int j=i+1;j<input.size();j++){
                if(used[j]||input.get(j)==null||a.source.equals(input.get(j).source))continue;
                double score=score(a,input.get(j));if(score>bestScore){bestScore=score;best=j;}
            }
            if(best>=0&&bestScore>=0.72){
                EventEvidence b=input.get(best);used[best]=true;
                MessageObservation.Direction direction=resolveDirection(a.direction,b.direction);
                out.add(new CanonicalEvent(a.kind,direction,a.content,earliest(a.observedAt,b.observedAt),
                        Math.min(0.99,Math.max(a.confidence,b.confidence)+0.08),Arrays.asList(a.source,b.source)));
            }else out.add(new CanonicalEvent(a.kind,a.direction,a.content,a.observedAt,a.confidence,Collections.singletonList(a.source)));
        }
        return Collections.unmodifiableList(out);
    }

    /** One sensor post may expose summary, lines and body revisions for the same observation. */
    private static List<EventEvidence> collapseSensorRevisions(List<EventEvidence> evidence){
        if(evidence==null||evidence.isEmpty())return Collections.emptyList();
        Map<String,EventEvidence> identified=new LinkedHashMap<String,EventEvidence>();List<EventEvidence> anonymous=new ArrayList<EventEvidence>();
        for(EventEvidence e:evidence)if(e!=null){
            if(e.sourceInstance.isEmpty()){anonymous.add(e);continue;}
            String key=EventEvidence.normalized(e.source)+"|"+EventEvidence.normalized(e.sourceInstance)+"|"+
                    EventEvidence.normalized(e.context)+"|"+EventEvidence.normalized(e.kind)+"|"+EventEvidence.normalized(e.content);
            EventEvidence old=identified.get(key);if(old==null||e.confidence>=old.confidence)identified.put(key,e);
        }
        anonymous.addAll(identified.values());return anonymous;
    }

    private static double score(EventEvidence a,EventEvidence b){
        if(!EventEvidence.normalized(a.kind).equals(EventEvidence.normalized(b.kind)))return 0.0;
        if(!EventEvidence.normalized(a.content).equals(EventEvidence.normalized(b.content)))return 0.0;
        if(!a.context.isEmpty()&&!b.context.isEmpty()&&!EventEvidence.normalized(a.context).equals(EventEvidence.normalized(b.context)))return 0.0;
        if(a.direction!=MessageObservation.Direction.UNKNOWN&&b.direction!=MessageObservation.Direction.UNKNOWN&&a.direction!=b.direction)return 0.0;
        if(a.observedAt>0&&b.observedAt>0&&Math.abs(a.observedAt-b.observedAt)>30000L)return 0.0;
        double score=0.52+0.18;
        if(a.context.isEmpty()||b.context.isEmpty()||EventEvidence.normalized(a.context).equals(EventEvidence.normalized(b.context)))score+=0.12;
        if(a.observedAt<=0||b.observedAt<=0||Math.abs(a.observedAt-b.observedAt)<=30000L)score+=0.18;
        return Math.min(1.0,score);
    }

    private static MessageObservation.Direction resolveDirection(MessageObservation.Direction a,MessageObservation.Direction b){
        return a==MessageObservation.Direction.UNKNOWN?b:a;
    }
    private static long earliest(long a,long b){if(a<=0)return b;if(b<=0)return a;return Math.min(a,b);}
}
