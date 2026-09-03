package com.kareem.lifeos.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Matches SCREEN and NOTIFICATION evidence into one canonical message event. */
public final class ReconciliationEngine {
    private ReconciliationEngine(){}

    public static List<CanonicalEvent> reconcile(String screenThread,List<MessageObservation> screen,List<NotificationObservation> notifications){
        List<MessageObservation> ss=screen==null?Collections.<MessageObservation>emptyList():screen;
        List<NotificationObservation> ns=notifications==null?Collections.<NotificationObservation>emptyList():notifications;
        boolean[] used=new boolean[ns.size()];
        List<CanonicalEvent> out=new ArrayList<CanonicalEvent>();
        for(MessageObservation s:ss){
            int best=-1;double bestScore=0.0;
            String resolvedThread=s.thread.trim().isEmpty()?screenThread:s.thread;
            ReconciliationKey sk=ReconciliationKey.fromMessage(resolvedThread,s);
            for(int i=0;i<ns.size();i++){
                if(used[i])continue;
                NotificationObservation n=ns.get(i);
                ReconciliationKey nk=new ReconciliationKey(n.thread,n.type,n.direction,n.text,n.observedAt);
                if(!sk.compatibleWith(nk))continue;
                double score=score(s,n,resolvedThread);
                if(score>bestScore){bestScore=score;best=i;}
            }
            if(best>=0&&bestScore>=0.72){
                NotificationObservation n=ns.get(best);used[best]=true;
                MessageObservation.Direction d=s.direction==MessageObservation.Direction.UNKNOWN?n.direction:s.direction;
                long at=s.observedAt>0?s.observedAt:n.observedAt;
                double confidence=Math.min(0.99,Math.max(s.confidence,n.confidence)+0.08);
                out.add(new CanonicalEvent("MESSAGE",d,s.text,at,confidence,Arrays.asList("SCREEN","NOTIFICATION")));
            }else{
                out.add(new CanonicalEvent("MESSAGE",s.direction,s.text,s.observedAt,s.confidence,Collections.singletonList("SCREEN")));
            }
        }
        for(int i=0;i<ns.size();i++)if(!used[i]){
            NotificationObservation n=ns.get(i);
            out.add(new CanonicalEvent("MESSAGE",n.direction,n.text,n.observedAt,n.confidence,Collections.singletonList("NOTIFICATION")));
        }
        return Collections.unmodifiableList(out);
    }

    private static double score(MessageObservation s,NotificationObservation n,String screenThread){
        double score=0.0;
        if(ReconciliationKey.normalize(s.text).equals(ReconciliationKey.normalize(n.text)))score+=0.52;
        if(s.direction==MessageObservation.Direction.UNKNOWN||s.direction==n.direction)score+=0.18;
        if(screenThread==null||screenThread.trim().isEmpty()||n.thread.trim().isEmpty()||screenThread.trim().equalsIgnoreCase(n.thread.trim()))score+=0.12;
        if(s.observedAt<=0||n.observedAt<=0||Math.abs(s.observedAt-n.observedAt)<=30000L)score+=0.18;
        return Math.min(1.0,score);
    }
}
