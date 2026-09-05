package com.kareem.lifeos;

import org.junit.Test;
import static org.junit.Assert.*;

public final class IntelligenceStatusTest {
    @Test public void readyBackgroundWorkIsReportedAsProcessing(){
        IntelligenceStatus.Snapshot s=new IntelligenceStatus.Snapshot(42,3,5,4,"ready",100,true);
        assertFalse(s.upToDate());assertTrue(s.modelReady());
        assertTrue(s.line().contains("3 processing in background"));assertTrue(s.line().contains("1 provisional"));
    }

    @Test public void modelDownloadKeepsPendingWorkExplicitlySafe(){
        IntelligenceStatus.Snapshot s=new IntelligenceStatus.Snapshot(20,7,2,1,"downloading",64,true);
        assertTrue(s.line().contains("downloading 64%"));assertTrue(s.line().contains("7 safely queued"));
    }

    @Test public void zeroPendingMeansPreparedNowSurface(){
        IntelligenceStatus.Snapshot s=new IntelligenceStatus.Snapshot(50,0,4,4,"ready",100,false);
        assertTrue(s.upToDate());assertTrue(s.line().startsWith("✓ Intelligence ready"));assertTrue(s.line().contains("0 pending"));
    }
}
