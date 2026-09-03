package com.kareem.lifeos.engine;

import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class MetadataAndReconciliationTest {
    @Test public void centerMarkerStaysSystemEvidenceNotMessageMetadataGuess(){
        RawNode marker=new RawNode(9,1,1,"TextView","","7 unread messages","",430,800,650,850,false,false,false);
        StructuralElement e=new StructuralElement(StructuralElement.Role.CENTER_MARKER,marker,0.8);
        List<MessageMetadataEvidence> xs=MessageMetadataAssociator.associate(Arrays.asList(e),Arrays.<MessageObservation>asList());
        assertEquals(1,xs.size());assertEquals(MessageMetadataEvidence.Kind.SYSTEM_MARKER,xs.get(0).kind);assertEquals(-1,xs.get(0).targetMessageIndex);
    }
    @Test public void reconciliationAllowsUnknownDirectionAndNearbyTime(){
        MessageObservation a=new MessageObservation(MessageObservation.Direction.UNKNOWN,"  Hello   World ",0,0,1,1,30000L,0.7);
        MessageObservation b=new MessageObservation(MessageObservation.Direction.IN,"hello world",0,0,1,1,44000L,0.9);
        assertTrue(ReconciliationKey.fromMessage("Chat",a).compatibleWith(ReconciliationKey.fromMessage("chat",b)));
    }
    @Test public void reconciliationRejectsOppositeKnownDirections(){
        MessageObservation a=new MessageObservation(MessageObservation.Direction.IN,"hello",0,0,1,1,30000L,0.7);
        MessageObservation b=new MessageObservation(MessageObservation.Direction.OUT,"hello",0,0,1,1,30000L,0.9);
        assertFalse(ReconciliationKey.fromMessage("chat",a).compatibleWith(ReconciliationKey.fromMessage("chat",b)));
    }
}
