package com.kareem.lifeos.engine;

import org.junit.Test;
import java.util.Arrays;
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
        assertEquals(1,out.size());assertEquals("Hello",out.get(0).text);assertEquals(MessageObservation.Direction.IN,out.get(0).direction);assertEquals("NOTIFICATION",out.get(0).sources.get(0));
    }
}
