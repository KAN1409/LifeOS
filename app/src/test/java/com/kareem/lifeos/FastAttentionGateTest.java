package com.kareem.lifeos;

import com.kareem.lifeos.context.RawObservation;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public final class FastAttentionGateTest {
    private static RawObservation raw(String text){
        Map<String,String> attrs=new HashMap<>();attrs.put("structured_message","true");attrs.put("category","msg");attrs.put("ongoing","false");
        return new RawObservation("notification|test",RawObservation.SourceKind.NOTIFICATION,"com.whatsapp",
                "com.whatsapp|kareem abdel nasser","POSTED",1000L,text,"",attrs);
    }
    private static LifeDb.Event event(String text){return new LifeDb.Event(1L,1000L,"com.whatsapp","Kareem Abdel Nasser",text,"com.whatsapp|kareem abdel nasser");}

    @Test public void urgentArabicSendRequestIsReservedImmediately(){
        String text="كريم ابعتلي الميل ضروري جدا";
        FastAttentionGate.Result r=FastAttentionGate.evaluate(event(text),raw(text),1000L);
        assertTrue(r.provisional);assertEquals("REQUEST",r.intent);assertEquals("DO_TASK",r.action);
        assertTrue(r.queuePriority>=r.attentionPriority);assertTrue(r.confidence>=.80);
    }

    @Test public void wordOrderDoesNotDependOnOneExactSentence(){
        String text="كريم ضروري جدا تبعتلي الميل";
        FastAttentionGate.Result r=FastAttentionGate.evaluate(event(text),raw(text),1000L);
        assertTrue(r.provisional);assertEquals("PERSON_CONVERSATION",r.type);
    }

    @Test public void colloquialMorphologyGeneralizesBeyondSendVerb(){
        assertTrue(FastAttentionGate.looksLikeColloquialDirectRequest("محتاجك تراجعلي الملف النهارده"));
        assertTrue(FastAttentionGate.looksLikeColloquialDirectRequest("لو سمحت تكلمني لما تفضى"));
    }

    @Test public void urgencyWithoutSecondPersonActionIsNotEnough(){
        assertFalse(FastAttentionGate.looksLikeColloquialDirectRequest("الموضوع ضروري جدا ومهم"));
    }

    @Test public void ordinaryGreetingIsQueuedButNotSurfacedProvisionally(){
        String text="Hi";FastAttentionGate.Result r=FastAttentionGate.evaluate(event(text),raw(text),1000L);
        assertFalse(r.provisional);assertTrue(r.queuePriority>0);
    }
}
