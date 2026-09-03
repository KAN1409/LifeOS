package com.kareem.lifeos.engine;

import java.util.ArrayList;
import java.util.List;

/** Compatibility adapter for the original M1 UI. New inference is source-neutral. */
public final class ReconciliationEngine {
    private ReconciliationEngine(){}

    public static List<CanonicalEvent> reconcile(String screenThread,List<MessageObservation> screen,List<NotificationObservation> notifications){
        List<EventEvidence> evidence=new ArrayList<EventEvidence>();
        if(screen!=null)for(MessageObservation s:screen)if(s!=null){
            String thread=s.thread.trim().isEmpty()?screenThread:s.thread;
            evidence.add(new EventEvidence("SCREEN",s.left+":"+s.top+":"+s.right+":"+s.bottom,thread,s.type,s.direction,s.text,s.observedAt,s.confidence));
        }
        if(notifications!=null)for(NotificationObservation n:notifications)if(n!=null)evidence.add(EventEvidence.fromNotification(n));
        return EvidenceFusionEngine.fuse(evidence);
    }
}
