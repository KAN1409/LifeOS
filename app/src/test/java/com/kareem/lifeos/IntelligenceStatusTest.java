package com.kareem.lifeos;

import org.junit.Test;
import static org.junit.Assert.*;

public final class IntelligenceStatusTest {
    @Test public void readyStateReportsNoPendingWork(){
        IntelligenceStatus.Snapshot s=new IntelligenceStatus.Snapshot(42,0,5,5);
        assertTrue(s.upToDate());assertTrue(s.line().contains("42 analyzed"));assertTrue(s.line().contains("0 pending"));
    }

    @Test public void processingStateReportsQueuedAndProvisionalWork(){
        IntelligenceStatus.Snapshot s=new IntelligenceStatus.Snapshot(40,3,6,4);
        assertFalse(s.upToDate());assertEquals(2,s.provisional);assertTrue(s.line().contains("3 queued / processing"));
    }
}
