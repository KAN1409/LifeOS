package com.kareem.lifeos.engine;

import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class EvidenceFusionEngineTest {
    @Test public void sameEventAcrossSensorsFusesWithoutAppSpecificLogic(){
        EventEvidence screen=new EventEvidence("SCREEN","node","person-x","MESSAGE",MessageObservation.Direction.OUT,"Send file",1000,0.8);
        EventEvidence notification=new EventEvidence("NOTIFICATION","post","person-x","MESSAGE",MessageObservation.Direction.UNKNOWN,"Send file",1100,0.86);
        List<CanonicalEvent> out=EvidenceFusionEngine.fuse(Arrays.asList(screen,notification));
        assertEquals(1,out.size());assertEquals(2,out.get(0).sources.size());assertEquals(MessageObservation.Direction.OUT,out.get(0).direction);
    }

    @Test public void differentKindsOrContextsNeverFuse(){
        EventEvidence message=new EventEvidence("SCREEN","","person-a","MESSAGE",MessageObservation.Direction.UNKNOWN,"Tomorrow",1000,0.8);
        EventEvidence calendar=new EventEvidence("NOTIFICATION","","calendar","APPOINTMENT",MessageObservation.Direction.UNKNOWN,"Tomorrow",1000,0.8);
        assertEquals(2,EvidenceFusionEngine.fuse(Arrays.asList(message,calendar)).size());
    }

    @Test public void notificationDirectionIsNotInvented(){
        NotificationObservation n=new NotificationObservation("post|body|hash","app|topic","Update",1000,0.86);
        assertEquals(MessageObservation.Direction.UNKNOWN,EventEvidence.fromNotification(n).direction);
        assertEquals("post",EventEvidence.fromNotification(n).sourceInstance);
    }

    @Test public void oneSensorPostWithLineAndBodyRevisionIsOneEvidenceItem(){
        EventEvidence line=EventEvidence.fromNotification(new NotificationObservation("post|line|0|a","app|topic","Update",1000,0.86));
        EventEvidence body=EventEvidence.fromNotification(new NotificationObservation("post|body|b","app|topic","Update",1100,0.86));
        assertEquals(1,EvidenceFusionEngine.fuse(Arrays.asList(line,body)).size());
    }
}
