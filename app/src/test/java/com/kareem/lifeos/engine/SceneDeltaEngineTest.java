package com.kareem.lifeos.engine;

import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class SceneDeltaEngineTest {
    @Test public void firstSceneIsBaselineAndOnlyChangesBecomeEvidence(){
        SceneDeltaEngine engine=new SceneDeltaEngine();
        assertTrue(engine.observe("app|context",Arrays.asList(e("old",1))).isEmpty());
        List<EventEvidence> added=engine.observe("app|context",Arrays.asList(e("old",2),e("new",2)));
        assertEquals(1,added.size());assertEquals("new",added.get(0).content);
    }

    @Test public void occurrenceCountsPreserveRepeatedIdenticalEvents(){
        SceneDeltaEngine engine=new SceneDeltaEngine();engine.observe("context",Arrays.asList(e("OK",1)));
        List<EventEvidence> added=engine.observe("context",Arrays.asList(e("OK",2),e("OK",2)));
        assertEquals(1,added.size());assertEquals("OK",added.get(0).content);
    }

    private static EventEvidence e(String body,long at){return new EventEvidence("SCREEN","","context","MESSAGE",MessageObservation.Direction.UNKNOWN,body,at,0.8);}
}
