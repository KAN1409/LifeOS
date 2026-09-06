package com.kareem.lifeos;

import org.junit.Test;
import static org.junit.Assert.*;

public class CanonicalSemanticPolicyTest {
    private static LifeDb.Event event(String body){return new LifeDb.Event(1,1000,"com.whatsapp","Ahmed",body,"com.whatsapp|ahmed");}
    private static AttentionStore.Item item(String intent,String action,double confidence){return new AttentionStore.Item(1,1,1000,1000,1000,0,80,"obs","com.whatsapp|ahmed",AttentionStore.OPEN,"PERSON_CONVERSATION",intent,"MEDIUM",action,"summary","reason","test",confidence,false);}
    private static NotificationMeaning meaning(String intent,String state,String action,String span,double confidence){return new NotificationMeaning("com.whatsapp|ahmed","obs","PERSON_CONVERSATION",intent,state,"MEDIUM",action,"summary","reason",span,"",confidence,"test",1000);}

    @Test public void reactionCannotBecomeCanonicalAttention(){assertFalse(CanonicalSemanticPolicy.isCanonicalAttention(item("REQUEST","REPLY",.95),event("You reacted to 'Soon hayaty' on WhatsApp"),meaning("REQUEST","WAITING_ON_USER","REPLY","You reacted to 'Soon hayaty' on WhatsApp",.95)));}
    @Test public void ordinaryInformationIsNotObligation(){assertFalse(CanonicalSemanticPolicy.isCanonicalAttention(item("INFORMATION","REPLY",.95),event("I miss you"),meaning("INFORMATION","WAITING_ON_USER","REPLY","I miss you",.95)));}
    @Test public void directGroundedRequestCanBecomeObligation(){assertTrue(CanonicalSemanticPolicy.isCanonicalAttention(item("REQUEST","REPLY",.95),event("Please send me the car details"),meaning("REQUEST","WAITING_ON_USER","REPLY","Please send me the car details",.95)));}

    /** Regression: previous context may contain a question, but this target is only an answer. */
    @Test public void previousContextCannotCreateAttentionFromInformationalTarget(){LifeDb.Event target=event("I think the best day is 15");NotificationMeaning hallucinated=meaning("QUESTION","WAITING_ON_USER","REPLY","I think the best day is 15",.94);assertFalse(CanonicalSemanticPolicy.isCanonicalAttention(item("QUESTION","REPLY",.94),target,hallucinated));}

    @Test public void missingExactEvidenceSpanRejectsConversationAttention(){assertFalse(CanonicalSemanticPolicy.isCanonicalAttention(item("REQUEST","REPLY",.95),event("Please send me the car details"),meaning("REQUEST","WAITING_ON_USER","REPLY","",.95)));}
    @Test public void provisionalNeverBecomesCanonicalTruth(){AttentionStore.Item provisional=new AttentionStore.Item(1,1,1000,1000,1000,0,80,"obs","com.whatsapp|ahmed",AttentionStore.PROVISIONAL,"PERSON_CONVERSATION","REQUEST","MEDIUM","REPLY","summary","reason","fast",.99,true);assertFalse(CanonicalSemanticPolicy.isCanonicalAttention(provisional,event("Please send the file"),meaning("REQUEST","WAITING_ON_USER","REPLY","Please send the file",.99)));}
    @Test public void promotionCannotEnterTimelineEvenWhenMislabeled(){NotificationMeaning m=new NotificationMeaning("com.app|x","obs","PERSON_CONVERSATION","INFORMATION","INFORMATIONAL","LOW","NONE","Summary","",.95,"test",1000);assertFalse(CanonicalSemanticPolicy.isCanonicalTimeline(new LifeDb.Event(2,1000,"com.app","Offer","Unlock your free Mschool voucher","com.app|x"),m));}
}
