package com.kareem.lifeos;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public final class NotificationMeaningTest {
    @Test public void waitingRequestBecomesOneAttentionState() throws Exception {
        JSONObject o=new JSONObject()
                .put("type","PERSON_CONVERSATION").put("intent","REQUEST")
                .put("state","WAITING_ON_USER").put("urgency","MEDIUM")
                .put("action","DO_TASK").put("summary","Mona asked you to send the document.")
                .put("reason","The latest message asks for the document.").put("confidence",.91);
        NotificationMeaning m=NotificationMeaning.fromModel(o,"com.whatsapp|mona","notification|1","nano",100L);
        assertNotNull(m);assertTrue(m.canSummarize());assertTrue(m.needsAttention());assertEquals("request",m.loopKind());assertEquals(74,m.priority());
    }

    @Test public void mediumConfidenceCanSummarizeButCannotInterruptUser() throws Exception {
        JSONObject o=new JSONObject()
                .put("type","PERSON_CONVERSATION").put("intent","REQUEST")
                .put("state","WAITING_ON_USER").put("urgency","MEDIUM")
                .put("action","REPLY").put("summary","Ahmed may be asking for a reply.")
                .put("confidence",.70);
        NotificationMeaning m=NotificationMeaning.fromModel(o,"com.whatsapp|ahmed","notification|mid","nano",100L);
        assertTrue(m.canSummarize());assertFalse(m.needsAttention());
    }

    @Test public void informationalMessageDoesNotBecomeAttention() throws Exception {
        JSONObject o=new JSONObject()
                .put("type","PERSON_CONVERSATION").put("intent","INFORMATION")
                .put("state","INFORMATIONAL").put("urgency","LOW")
                .put("action","NONE").put("summary","Ahmed shared an update.")
                .put("confidence",.95);
        NotificationMeaning m=NotificationMeaning.fromModel(o,"com.whatsapp|ahmed","notification|2","nano",100L);
        assertNotNull(m);assertFalse(m.needsAttention());
    }

    @Test public void lowConfidenceModelOutputCannotCreateAttention() throws Exception {
        JSONObject o=new JSONObject()
                .put("type","SECURITY_ALERT").put("intent","ALERT")
                .put("state","WAITING_ON_USER").put("urgency","HIGH")
                .put("action","VERIFY").put("summary","Check this sign-in.")
                .put("confidence",.41);
        NotificationMeaning m=NotificationMeaning.fromModel(o,"com.google|security","notification|3","nano",100L);
        assertFalse(m.canSummarize());assertFalse(m.needsAttention());
    }

    @Test public void unknownModelLabelsAreConservativelyClamped() throws Exception {
        JSONObject o=new JSONObject().put("type","MAGIC").put("intent","PANIC").put("state","NOW")
                .put("urgency","CRITICAL").put("action","DELETE_EVERYTHING")
                .put("summary","Unknown classification").put("confidence",1.0);
        NotificationMeaning m=NotificationMeaning.fromModel(o,"stream","notification|4","nano",100L);
        assertEquals("OTHER",m.type);assertEquals("NONE",m.intent);assertEquals("UNKNOWN",m.state);
        assertEquals("NONE",m.urgency);assertEquals("NONE",m.action);assertFalse(m.needsAttention());
    }
}
