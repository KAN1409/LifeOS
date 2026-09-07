package com.kareem.lifeos;

import org.junit.Test;
import static org.junit.Assert.*;

/** Broad regression corpus for the conservative obligation gate. */
public final class SemanticRegressionCorpusTest {
    private static LifeDb.Event event(String body){return new LifeDb.Event(1,System.currentTimeMillis(),"com.whatsapp","Friend",body,"com.whatsapp|friend");}
    private static NotificationMeaning meaning(String body,String intent){return new NotificationMeaning("com.whatsapp|friend","obs:1","PERSON_CONVERSATION",intent,"WAITING_ON_USER","MEDIUM","REPLY",body,"qa",body,"",.95,"qa",System.currentTimeMillis());}

    private static void accept(String body,String intent){assertTrue("Expected obligation evidence to be accepted: "+body,CanonicalSemanticPolicy.exactTargetEvidenceSupportsObligation(event(body),meaning(body,intent)));}
    private static void reject(String body,String intent){assertFalse("Expected non-obligation evidence to be rejected: "+body,CanonicalSemanticPolicy.exactTargetEvidenceSupportsObligation(event(body),meaning(body,intent)));}

    @Test public void directRequestsAndQuestionsAreAccepted(){
        accept("Can you send me the invoice?","QUESTION");
        accept("Could you call me tonight?","QUESTION");
        accept("Please send the address","REQUEST");
        accept("I need you to review this today","REQUEST");
        accept("Would you bring the documents tomorrow?","QUESTION");
        accept("ممكن تبعتلي اللوكيشن؟","QUESTION");
        accept("لو سمحت ابعتلي رقم العربية","REQUEST");
        accept("من فضلك راجع الملف النهارده","REQUEST");
        accept("عايزك تكلمني لما تفضى","REQUEST");
        accept("محتاجك تبعتلي الصورة","REQUEST");
        accept("هل خلصت التقرير؟","QUESTION");
        accept("امتى هتبعتلي العرض؟","QUESTION");
        accept("فين الفاتورة؟","QUESTION");
        accept("ليه ما بعتش الملف؟","QUESTION");
    }

    @Test public void repliesStatementsAndSocialNoiseAreRejected(){
        reject("I think the best day is 15","INFORMATION");
        reject("Yes show me please","INFORMATION");
        reject("Please order 😅🙈","INFORMATION");
        reject("Hadry ya pirozhok","INFORMATION");
        reject("I miss you awyyyy baaa","INFORMATION");
        reject("Thank you so much","INFORMATION");
        reject("Okay","INFORMATION");
        reject("Sure","INFORMATION");
        reject("تمام","INFORMATION");
        reject("حاضر","INFORMATION");
        reject("شكرا","INFORMATION");
        reject("ماشي","INFORMATION");
        reject("أنا في الطريق","INFORMATION");
        reject("وصلت","INFORMATION");
        reject("هنشوف بكرة","INFORMATION");
        reject("😂😂😂","INFORMATION");
        reject("❤️","INFORMATION");
    }

    @Test public void exactEvidenceIsMandatory(){
        LifeDb.Event e=event("I think the best day is 15");
        NotificationMeaning m=new NotificationMeaning("com.whatsapp|friend","obs:1","PERSON_CONVERSATION","QUESTION","WAITING_ON_USER","MEDIUM","REPLY","Reply","qa","What day works best?","",.99,"qa",System.currentTimeMillis());
        assertFalse(CanonicalSemanticPolicy.exactTargetEvidenceSupportsObligation(e,m));
    }

    @Test public void platformPromotionAndContentReadyNoiseStayBlocked(){
        for(String text:new String[]{
                "Your image is ready to review",
                "Download complete",
                "You reacted 😂 to a message",
                "Sponsored limited offer",
                "Claim your free voucher",
                "خصم 50% عرض خاص",
                "App installed successfully",
                "Sync complete",
                "Backup complete"}){
            assertTrue("Expected platform/promo filter to catch: "+text,
                    CanonicalSemanticPolicy.isPlatformOrCompletedActivity(text)||CanonicalSemanticPolicy.isPromotionOrMarketing(text));
        }
    }
}
