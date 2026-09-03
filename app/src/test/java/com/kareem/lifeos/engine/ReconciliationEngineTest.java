package com.kareem.lifeos.engine;

import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class ReconciliationEngineTest {
    @Test public void mergesScreenAndNotificationCopies(){
        long at=100000L;
        MessageObservation s=new MessageObservation(MessageObservation.Direction.UNKNOWN,"  Hello   there ",0,0,100,100,at,0.74);
        NotificationObservation n=new NotificationObservation("chat","hello there",at+5000L,0.86);
        List<CanonicalEvent> out=ReconciliationEngine.reconcile("chat",Arrays.asList(s),Arrays.asList(n));
        assertEquals(1,out.size());
        assertTrue(out.get(0).merged());
        assertEquals(MessageObservation.Direction.IN,out.get(0).direction);
        assertEquals(2,out.get(0).sources.size());
    }

    @Test public void knownOppositeDirectionsDoNotMerge(){
        long at=200000L;
        MessageObservation s=new MessageObservation(MessageObservation.Direction.OUT,"same",0,0,100,100,at,0.80);
        NotificationObservation n=new NotificationObservation("chat","same",at,0.86);
        List<CanonicalEvent> out=ReconciliationEngine.reconcile("chat",Arrays.asList(s),Arrays.asList(n));
        assertEquals(2,out.size());
        assertFalse(out.get(0).merged());
        assertFalse(out.get(1).merged());
    }

    @Test public void differentTextsStaySeparate(){
        long at=300000L;
        MessageObservation s=new MessageObservation(MessageObservation.Direction.IN,"alpha",0,0,100,100,at,0.80);
        NotificationObservation n=new NotificationObservation("chat","beta",at,0.86);
        assertEquals(2,ReconciliationEngine.reconcile("chat",Arrays.asList(s),Arrays.asList(n)).size());
    }

    @Test public void sameBodyInDifferentThreadsDoesNotMerge(){
        long at=30000L;
        MessageObservation s=new MessageObservation("com.whatsapp|alice",MessageObservation.Direction.IN,"hello",0,0,100,100,at,0.80);
        NotificationObservation n=new NotificationObservation("id","com.whatsapp|bob","hello",at,0.86);
        assertEquals(2,ReconciliationEngine.reconcile("",Arrays.asList(s),Arrays.asList(n)).size());
    }
}
