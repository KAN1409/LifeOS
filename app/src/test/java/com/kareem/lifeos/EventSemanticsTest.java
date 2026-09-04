package com.kareem.lifeos;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EventSemanticsTest {
    @Test public void whatsappPersonMessageIsConversation(){
        LifeDb.Event e=new LifeDb.Event(1,1,"com.whatsapp","Suzanne Rashed","Please order this for me","com.whatsapp|suzanne rashed");
        assertTrue(EventSemantics.isPersonConversation(e));
        assertTrue(EventSemantics.supportsLoop(e,"request"));
    }

    @Test public void genericWhatsappSystemNotificationIsNotConversation(){
        LifeDb.Event e=new LifeDb.Event(1,1,"com.whatsapp","WhatsApp","Backup complete","com.whatsapp");
        assertFalse(EventSemantics.isPersonConversation(e));
        assertFalse(EventSemantics.supportsLoop(e,"request"));
    }

    @Test public void chatGptImageReadyIsContentNotPerson(){
        LifeDb.Event e=new LifeDb.Event(1,1,"com.openai.chatgpt","Joyful Selfie in a Golden Interior","Image is ready to view","chatgpt|image");
        assertFalse(EventSemantics.isPersonConversation(e));
        assertFalse(EventSemantics.shouldShowInToday(e));
    }

    @Test public void securityAlertCanSurfaceWithoutPerson(){
        LifeDb.Event e=new LifeDb.Event(1,1,"com.google.android.gms","Security alert","New sign-in from an unknown device","");
        assertTrue(EventSemantics.supportsLoop(e,"security"));
        assertTrue(EventSemantics.shouldShowInToday(e));
    }
}
