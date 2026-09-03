package com.kareem.lifeos.engine;

import android.content.Context;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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
        SceneDeltaEngine sceneDelta=new SceneDeltaEngine();
        if(raw!=null)for(PersistentRawEvidence r:raw){
            if(r==null)continue;
            if("SCREEN_TREE".equals(r.source)){
                RawScreenSnapshot snapshot=RawEvidenceSerializer.restore(r.payload);if(snapshot==null)continue;
                List<StructuralElement> elements=StructuralElementExtractor.extract(snapshot);
                List<BubbleCandidate> bubbles=BubbleClusterer.cluster(snapshot,elements);
                String thread=ConversationIdentityExtractor.fromSnapshot(snapshot);
                List<MessageObservation> current=MessageObservationBuilder.build(bubbles,snapshot.capturedAt,thread);
                String historyKey=snapshot.packageName+"|"+thread;
                List<EventEvidence> visible=new ArrayList<EventEvidence>();for(MessageObservation m:current)visible.add(EventEvidence.fromScreen(m));
                for(EventEvidence e:sceneDelta.observe(historyKey,visible))screen.add(new MessageObservation(e.context,e.direction,e.content,0,0,0,0,e.observedAt,e.confidence));
            }else if("NOTIFICATION".equals(r.source)&&!r.text.trim().isEmpty()){
                notifications.add(new NotificationObservation(r.payload,r.thread,r.text,r.observedAt,r.confidence));
            }
        }
        List<CanonicalEvent> out=new ArrayList<CanonicalEvent>(ReconciliationEngine.reconcile("",screen,dedupeNotifications(notifications)));
        Collections.sort(out,new Comparator<CanonicalEvent>(){@Override public int compare(CanonicalEvent a,CanonicalEvent b){return Long.compare(a.observedAt,b.observedAt);}});
        return Collections.unmodifiableList(out);
    }

    static void appendNewVisible(List<MessageObservation> history,List<MessageObservation> previous,List<MessageObservation> current){
        if(current==null||current.isEmpty())return;
        if(previous==null||previous.isEmpty()){history.addAll(current);return;}
        int a=previous.size(),b=current.size();int[][] lcs=new int[a+1][b+1];
        for(int i=a-1;i>=0;i--)for(int j=b-1;j>=0;j--)lcs[i][j]=same(previous.get(i),current.get(j))?1+lcs[i+1][j+1]:Math.max(lcs[i+1][j],lcs[i][j+1]);
        boolean[] matched=new boolean[b];int i=0,j=0;
        while(i<a&&j<b){if(same(previous.get(i),current.get(j))){matched[j]=true;i++;j++;}else if(lcs[i+1][j]>=lcs[i][j+1])i++;else j++;}
        for(j=0;j<b;j++)if(!matched[j])history.add(current.get(j));
    }

    private static boolean same(MessageObservation a,MessageObservation b){return a.direction==b.direction&&ReconciliationKey.normalize(a.text).equals(ReconciliationKey.normalize(b.text));}

    private static List<NotificationObservation> dedupeNotifications(List<NotificationObservation> input){
        Map<String,NotificationObservation> identified=new java.util.LinkedHashMap<String,NotificationObservation>();
        List<NotificationObservation> anonymous=new ArrayList<NotificationObservation>();
        for(NotificationObservation n:input){if(n.evidenceId.trim().isEmpty())anonymous.add(n);else identified.put(n.evidenceId,n);}
        anonymous.addAll(identified.values());return anonymous;
    }
}
