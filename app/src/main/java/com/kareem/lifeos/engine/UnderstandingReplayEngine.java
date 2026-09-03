package com.kareem.lifeos.engine;

import android.content.Context;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Rebuilds canonical interpretation from immutable persisted raw evidence. */
public final class UnderstandingReplayEngine {
    private UnderstandingReplayEngine(){}

    public static List<CanonicalEvent> replay(Context context){
        if(context==null)return Collections.emptyList();
        PersistentUnderstandingStore store=PersistentUnderstandingStore.get(context);
        List<CanonicalEvent> rebuilt=rebuild(store.loadRawEvidence());
        store.replaceCanonical(rebuilt,UnderstandingEngineVersion.CURRENT,System.currentTimeMillis());
        return rebuilt;
    }

    public static boolean replayIfNeeded(Context context){
        if(context==null)return false;
        PersistentUnderstandingStore store=PersistentUnderstandingStore.get(context);
        if(!store.needsReplay())return false;
        replay(context);return true;
    }

    public static List<CanonicalEvent> rebuild(List<PersistentRawEvidence> raw){
        List<MessageObservation> screen=new ArrayList<MessageObservation>();
        List<NotificationObservation> notifications=new ArrayList<NotificationObservation>();
        if(raw!=null)for(PersistentRawEvidence r:raw){
            if(r==null)continue;
            if("SCREEN_TREE".equals(r.source)){
                RawScreenSnapshot snapshot=RawEvidenceSerializer.restore(r.payload);if(snapshot==null)continue;
                List<StructuralElement> elements=StructuralElementExtractor.extract(snapshot);
                List<BubbleCandidate> bubbles=BubbleClusterer.cluster(snapshot,elements);
                screen.addAll(MessageObservationBuilder.build(bubbles,snapshot.capturedAt));
            }else if("NOTIFICATION".equals(r.source)&&!r.text.trim().isEmpty()){
                notifications.add(new NotificationObservation(r.thread,r.text,r.observedAt,r.confidence));
            }
        }
        List<CanonicalEvent> out=new ArrayList<CanonicalEvent>(ReconciliationEngine.reconcile("",dedupeScreen(screen),notifications));
        Collections.sort(out,new Comparator<CanonicalEvent>(){@Override public int compare(CanonicalEvent a,CanonicalEvent b){return Long.compare(a.observedAt,b.observedAt);}});
        return Collections.unmodifiableList(out);
    }

    private static List<MessageObservation> dedupeScreen(List<MessageObservation> input){
        List<MessageObservation> out=new ArrayList<MessageObservation>();
        for(MessageObservation m:input){boolean duplicate=false;for(int i=out.size()-1;i>=0;i--){MessageObservation prior=out.get(i);long dt=Math.abs(prior.observedAt-m.observedAt);if(dt>30000L)break;if(prior.direction==m.direction&&ReconciliationKey.normalize(prior.text).equals(ReconciliationKey.normalize(m.text))){duplicate=true;break;}}if(!duplicate)out.add(m);}return out;
    }
}
