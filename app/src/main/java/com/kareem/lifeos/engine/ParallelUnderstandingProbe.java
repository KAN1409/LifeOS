package com.kareem.lifeos.engine;

import java.util.Collections;
import java.util.List;

/** Temporary in-memory bridge for M1 shadow-mode evaluation. Never writes to LifeDb. */
public final class ParallelUnderstandingProbe {
    private static volatile RawScreenSnapshot lastSnapshot;
    private static volatile ScreenState lastState=new ScreenState(ScreenState.Type.UNKNOWN,0.0);
    private static volatile List<StructuralElement> lastElements=Collections.emptyList();
    private static volatile List<BubbleCandidate> lastBubbles=Collections.emptyList();
    private static volatile List<MessageObservation> lastMessages=Collections.emptyList();
    private static volatile List<MessageMetadataEvidence> lastMetadata=Collections.emptyList();
    private ParallelUnderstandingProbe(){}
    public static void observe(RawScreenSnapshot snapshot){
        lastSnapshot=snapshot;
        lastState=StructuralScreenClassifier.classify(snapshot);
        lastElements=StructuralElementExtractor.extract(snapshot);
        lastBubbles=BubbleClusterer.cluster(snapshot,lastElements);
        lastMessages=MessageObservationBuilder.build(lastBubbles,snapshot==null?0L:snapshot.capturedAt);
        lastMetadata=MessageMetadataAssociator.associate(lastElements,lastMessages);
    }
    public static RawScreenSnapshot lastSnapshot(){return lastSnapshot;}
    public static ScreenState lastState(){return lastState;}
    public static List<StructuralElement> lastElements(){return lastElements;}
    public static List<BubbleCandidate> lastBubbles(){return lastBubbles;}
    public static List<MessageObservation> lastMessages(){return lastMessages;}
    public static List<MessageMetadataEvidence> lastMetadata(){return lastMetadata;}
}
