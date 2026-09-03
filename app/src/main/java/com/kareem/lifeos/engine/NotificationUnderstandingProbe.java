package com.kareem.lifeos.engine;

import android.content.Context;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Notification-side shadow probe. Legacy LifeDb remains untouched. */
public final class NotificationUnderstandingProbe {
    private static final int MAX=24;
    private static final List<NotificationObservation> recent=new ArrayList<NotificationObservation>();
    private static volatile List<CanonicalEvent> lastCanonical=Collections.emptyList();

    private NotificationUnderstandingProbe(){}

    public static synchronized void observe(Context context,String thread,String text,long at){
        if(text==null||text.trim().isEmpty())return;
        NotificationObservation observation=new NotificationObservation(thread,text,at,0.86);
        recent.add(observation);while(recent.size()>MAX)recent.remove(0);
        ShadowCanonicalStore store=ShadowCanonicalStore.shared();
        store.appendRaw(new RawEvidenceRecord("NOTIFICATION",thread,"MESSAGE",MessageObservation.Direction.IN,text,at,observation.confidence));
        lastCanonical=ReconciliationEngine.reconcile("",ParallelUnderstandingProbe.lastMessages(),new ArrayList<NotificationObservation>(recent));
        store.replaceCanonical(lastCanonical);
        if(context!=null){
            PersistentUnderstandingStore persistent=PersistentUnderstandingStore.get(context);
            persistent.recordNotification(observation);
            persistent.replaceCanonical(lastCanonical);
        }
    }

    public static void observe(String thread,String text,long at){observe(null,thread,text,at);}
    public static synchronized List<NotificationObservation> recent(){return Collections.unmodifiableList(new ArrayList<NotificationObservation>(recent));}
    public static List<CanonicalEvent> lastCanonical(){return lastCanonical;}
}
