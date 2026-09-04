package com.kareem.lifeos;

import org.junit.Test;
import static org.junit.Assert.*;

public final class NotificationMeaningTest {
    private static NotificationMeaning meaning(String type,String intent,String state,String urgency,
                                               String action,String summary,String reason,double confidence){
        return new NotificationMeaning("stream","notification|test",type,intent,state,urgency,action,
                summary,reason,confidence,"nano",100L);
    }

    @Test public void waitingRequestBecomesOneAttentionState() {
        NotificationMeaning m=meaning("PERSON_CONVERSATION","REQUEST","WAITING_ON_USER","MEDIUM",
                "DO_TASK","Mona asked you to send the document.",
                "The latest message asks for the document.",.91);
        assertNotNull(m);assertTrue(m.canSummarize());assertTrue(m.needsAttention());
        assertEquals("request",m.loopKind());assertEquals(74,m.priority());
    }

    @Test public void mediumConfidenceCanSummarizeButCannotInterruptUser() {
        NotificationMeaning m=meaning("PERSON_CONVERSATION","REQUEST","WAITING_ON_USER","MEDIUM",
                "REPLY","Ahmed may be asking for a reply.","",.70);
        assertTrue(m.canSummarize());assertFalse(m.needsAttention());
    }

    @Test public void informationalMessageDoesNotBecomeAttention() {
        NotificationMeaning m=meaning("PERSON_CONVERSATION","INFORMATION","INFORMATIONAL","LOW",
                "NONE","Ahmed shared an update.","",.95);
        assertNotNull(m);assertFalse(m.needsAttention());
    }

    @Test public void lowConfidenceModelOutputCannotCreateAttention() {
        NotificationMeaning m=meaning("SECURITY_ALERT","ALERT","WAITING_ON_USER","HIGH",
                "VERIFY","Check this sign-in.","",.41);
        assertFalse(m.canSummarize());assertFalse(m.needsAttention());
    }

    @Test public void unknownModelLabelsAreConservativelyClamped() {
        NotificationMeaning m=meaning("MAGIC","PANIC","NOW","CRITICAL","DELETE_EVERYTHING",
                "Unknown classification","",1.0);
        assertEquals("OTHER",m.type);assertEquals("NONE",m.intent);assertEquals("UNKNOWN",m.state);
        assertEquals("NONE",m.urgency);assertEquals("NONE",m.action);assertFalse(m.needsAttention());
    }
}
