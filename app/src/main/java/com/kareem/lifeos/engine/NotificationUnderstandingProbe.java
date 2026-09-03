package com.kareem.lifeos.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** In-memory notification-side shadow probe. Never writes to LifeDb. */
public final class NotificationUnderstandingProbe {
    private static final int MAX=24;
    private static final List<NotificationObservation> recent=new ArrayList<NotificationObservation>();
    private static volatile List<CanonicalEvent> lastCanonical=Collections.emptyList();

    private NotificationUnderstandingProbe(){}

    public static synchronized void observe(String thread,String text,long at){
        if(text==null||text.trim().isEmpty())return;
        NotificationObservation observation=new NotificationObservation(thread,text,at,0.86);
        recent.add(observation);
        while(recent.size()>MAX)recent.remove(0);
        ShadowCanonicalStore store=ShadowCanonicalStore.shared();
        store.appendRaw(new RawEvidenceRecord("NOTIFICATION",thread,"MESSAGE",MessageObservation.Direction.IN,text,at,observation.confidence));
        lastCanonical=ReconciliationEngine.reconcile("",ParallelUnderstandingProbe.lastMessages(),new ArrayList<NotificationObservation>(recent));
        store.replaceCanonical(lastCanonical);
    }

    public static synchronized List<NotificationObservation> recent(){
        return Collections.unmodifiableList(new ArrayList<NotificationObservation>(recent));
    }

    public static List<CanonicalEvent> lastCanonical(){return lastCanonical;}
}
