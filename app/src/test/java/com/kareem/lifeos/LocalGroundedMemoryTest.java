package com.kareem.lifeos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LocalGroundedMemoryTest {
    @Test public void granularHumanMessageCanBecomeMemory(){
        LifeDb.Event e=new LifeDb.Event(42,1000,"com.whatsapp","Suzanne Rashed","Please order this for me","com.whatsapp|suzanne rashed");
        assertTrue(LocalGroundedMemory.eligible(e));
        assertEquals("conversation:com.whatsapp:suzanne rashed",LocalGroundedMemory.subjectId(e));
        assertEquals("life-event|42",LocalGroundedMemory.assertionId(e));
    }

    @Test public void aggregateVisibleConversationIsNotDurableMemory(){
        LifeDb.Event e=new LifeDb.Event(43,1000,"com.whatsapp","Visible conversation","Suzanne · old message · random screen chrome","com.whatsapp|suzanne rashed");
        assertFalse(LocalGroundedMemory.eligible(e));
    }

    @Test public void contentReadyNotificationIsNotDurableMemory(){
        LifeDb.Event e=new LifeDb.Event(44,1000,"com.openai.chatgpt","Joyful Selfie in a Golden Interior","Image is ready to view","chatgpt|image");
        assertFalse(LocalGroundedMemory.eligible(e));
    }
}
