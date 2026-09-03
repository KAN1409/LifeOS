package com.kareem.lifeos.engine;

import org.junit.Test;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class UnderstandingReplayEngineTest {
    @Test public void serializerRoundTripPreservesRawTree(){
        RawNode n=new RawNode(1,-1,0,"TextView","message","hello\nworld","desc\tvalue",10,20,300,80,true,false,false);
        RawScreenSnapshot original=new RawScreenSnapshot("com.example.chat",12345L,1080,2400,Arrays.asList(n));
        RawScreenSnapshot restored=RawEvidenceSerializer.restore(RawEvidenceSerializer.snapshot(original));
        assertNotNull(restored);assertEquals(original.packageName,restored.packageName);assertEquals(1,restored.nodes.size());assertEquals("hello\nworld",restored.nodes.get(0).text);assertEquals("desc\tvalue",restored.nodes.get(0).contentDescription);
    }

    @Test public void replayBuildsCanonicalFromPersistedRawRows(){
        PersistentRawEvidence notification=new PersistentRawEvidence(1,"NOTIFICATION","chat","MESSAGE",MessageObservation.Direction.IN,"Hello",1000L,0.86,"");
        List<CanonicalEvent> out=UnderstandingReplayEngine.rebuild(Arrays.asList(notification));
        assertEquals(1,out.size());assertEquals("Hello",out.get(0).text);assertEquals(MessageObservation.Direction.UNKNOWN,out.get(0).direction);assertEquals("NOTIFICATION",out.get(0).sources.get(0));
    }

    @Test public void replayTreatsFirstSceneAsContextAndEmitsOnlyLaterChange(){
        RawScreenSnapshot first=snapshot(1000L,"Old");
        RawScreenSnapshot second=snapshot(2000L,"Old","New");
        PersistentRawEvidence a=new PersistentRawEvidence(1,"SCREEN_TREE","thread","SCREEN_SNAPSHOT",MessageObservation.Direction.UNKNOWN,"",1000L,1.0,RawEvidenceSerializer.snapshot(first));
        PersistentRawEvidence b=new PersistentRawEvidence(2,"SCREEN_TREE","thread","SCREEN_SNAPSHOT",MessageObservation.Direction.UNKNOWN,"",2000L,1.0,RawEvidenceSerializer.snapshot(second));
        List<CanonicalEvent> out=UnderstandingReplayEngine.rebuild(Arrays.asList(a,b));
        assertEquals(1,out.size());assertEquals("New",out.get(0).text);
    }

    @Test public void repeatedSnapshotsAddOnlyNewVisibleMessages(){
        List<MessageObservation> history=new ArrayList<MessageObservation>();
        List<MessageObservation> first=Arrays.asList(message("Testing",1000),message("1",1000),message("2",1000));
        List<MessageObservation> second=Arrays.asList(message("Testing",40000),message("1",40000),message("2",40000),message("3",40000));
        UnderstandingReplayEngine.appendNewVisible(history,null,first);
        UnderstandingReplayEngine.appendNewVisible(history,first,second);
        assertEquals(4,history.size());assertEquals("3",history.get(3).text);
    }

    @Test public void sameBodyCanStillBeANewSecondMessage(){
        List<MessageObservation> history=new ArrayList<MessageObservation>();
        List<MessageObservation> first=Arrays.asList(message("OK",1000));
        List<MessageObservation> second=Arrays.asList(message("OK",2000),message("OK",2000));
        UnderstandingReplayEngine.appendNewVisible(history,null,first);
        UnderstandingReplayEngine.appendNewVisible(history,first,second);
        assertEquals(2,history.size());
    }

    private static MessageObservation message(String text,long at){return new MessageObservation(MessageObservation.Direction.OUT,text,700,800,1000,900,at,0.8);}
    private static RawScreenSnapshot snapshot(long at,String... bodies){
        List<RawNode> nodes=new ArrayList<RawNode>();nodes.add(new RawNode(1,-1,0,"List","","","",0,200,1080,2200,false,true,false));
        for(int i=0;i<bodies.length;i++)nodes.add(new RawNode(i+2,1,1,"TextView","",bodies[i],"",100,400+i*100,500,480+i*100,false,false,false));
        return new RawScreenSnapshot("com.example",at,1080,2400,nodes);
    }
}
