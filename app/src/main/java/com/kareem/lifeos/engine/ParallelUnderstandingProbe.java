package com.kareem.lifeos.engine;

import android.content.Context;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

/** Shadow bridge: evaluates M1 and persists only to the isolated understanding database, never LifeDb. */
public final class ParallelUnderstandingProbe {
    private static volatile RawScreenSnapshot lastSnapshot;
    private static volatile ScreenState lastState=new ScreenState(ScreenState.Type.UNKNOWN,0.0);
    private static volatile List<StructuralElement> lastElements=Collections.emptyList();
    private static volatile List<BubbleCandidate> lastBubbles=Collections.emptyList();
    private static volatile List<MessageObservation> lastMessages=Collections.emptyList();
    private static final SceneDeltaEngine sceneDelta=new SceneDeltaEngine();
    private static final List<MessageObservation> eventMessages=new ArrayList<MessageObservation>();
    private static volatile List<MessageMetadataEvidence> lastMetadata=Collections.emptyList();
    private ParallelUnderstandingProbe(){}

    public static void observe(Context context,RawScreenSnapshot snapshot){
        lastSnapshot=snapshot;
        lastState=StructuralScreenClassifier.classify(snapshot);
        lastElements=StructuralElementExtractor.extract(snapshot);
        lastBubbles=BubbleClusterer.cluster(snapshot,lastElements);
        String thread=ConversationIdentityExtractor.fromSnapshot(snapshot);
        lastMessages=MessageObservationBuilder.build(lastBubbles,snapshot==null?0L:snapshot.capturedAt,thread);
        lastMetadata=MessageMetadataAssociator.associate(lastElements,lastMessages);
        List<EventEvidence> visible=new ArrayList<EventEvidence>();for(MessageObservation m:lastMessages)visible.add(EventEvidence.fromScreen(m));
        synchronized(eventMessages){for(EventEvidence e:sceneDelta.observe((snapshot==null?"":snapshot.packageName)+"|"+thread,visible))eventMessages.add(new MessageObservation(e.context,e.direction,e.content,0,0,0,0,e.observedAt,e.confidence));}
        ShadowCanonicalStore store=ShadowCanonicalStore.shared();
        for(MessageObservation m:lastMessages)store.appendRaw(new RawEvidenceRecord("SCREEN","",m.type,m.direction,m.text,m.observedAt,m.confidence));
        List<CanonicalEvent> canonical=ReconciliationEngine.reconcile(thread,eventMessages(),NotificationUnderstandingProbe.recent());
        store.replaceCanonical(canonical);
        if(context!=null){
            PersistentUnderstandingStore persistent=PersistentUnderstandingStore.get(context);
            persistent.recordScreen(snapshot,lastMessages);
            persistent.replaceCanonical(canonical);
        }
    }

    public static void observe(RawScreenSnapshot snapshot){observe(null,snapshot);}
    public static RawScreenSnapshot lastSnapshot(){return lastSnapshot;}
    public static ScreenState lastState(){return lastState;}
    public static List<StructuralElement> lastElements(){return lastElements;}
    public static List<BubbleCandidate> lastBubbles(){return lastBubbles;}
    public static List<MessageObservation> lastMessages(){return lastMessages;}
    public static List<MessageObservation> eventMessages(){synchronized(eventMessages){return Collections.unmodifiableList(new ArrayList<MessageObservation>(eventMessages));}}
    public static List<MessageMetadataEvidence> lastMetadata(){return lastMetadata;}
}
