package com.kareem.lifeos.engine;

import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

/** Regression distilled from the real Samsung/WhatsApp UI hierarchy captured on 2026-09-03. */
public class WhatsAppCapturedStructureTest {
    @Test public void stableViewRolesProduceBodiesOnlyAndCorrectDirections(){
        RawScreenSnapshot s=new RawScreenSnapshot("com.whatsapp",1L,1440,3120,Arrays.asList(
            n(1,-1,"","",0,350,1440,2856,false,true,false),
            n(2,1,"","",0,350,1440,420,false,false,false),
            n(3,2,"com.whatsapp:id/message_text","Testing",844,350,1103,407,false,false,false),
            n(4,2,"com.whatsapp:id/date","8:57 AM",1103,350,1268,403,false,false,false),
            n(5,2,"com.whatsapp:id/status","Delivered",1268,350,1343,395,false,false,false),
            n(6,1,"","",0,2695,1440,2856,false,false,false),
            n(7,6,"com.whatsapp:id/message_text","Test received",71,2721,487,2829,false,false,false),
            n(8,6,"com.whatsapp:id/date","9:56 AM",510,2768,675,2829,false,false,false),
            n(9,-1,"com.whatsapp:id/voice_note_btn","Voice message, Button. Double tap and hold to record",1225,2864,1405,3044,false,false,false)
        ));
        List<BubbleCandidate> bubbles=BubbleClusterer.cluster(s,StructuralElementExtractor.extract(s));
        assertEquals(2,bubbles.size());
        assertEquals("Testing",bubbles.get(0).text);assertEquals(BubbleCandidate.Sender.SELF,bubbles.get(0).sender);
        assertEquals("Test received",bubbles.get(1).text);assertEquals(BubbleCandidate.Sender.OTHER,bubbles.get(1).sender);
    }

    @Test public void conversationTitleMatchesNotificationThreadKey(){
        RawScreenSnapshot s=new RawScreenSnapshot("com.whatsapp",1L,1440,3120,Arrays.asList(
            n(1,-1,"com.whatsapp:id/conversation_contact_name","+20 11 44445113",375,198,875,284,false,false,false)
        ));
        assertEquals("com.whatsapp|+20 11 44445113",ConversationIdentityExtractor.fromSnapshot(s));
    }

    private static RawNode n(int id,int parent,String viewId,String value,int l,int t,int r,int b,boolean clickable,boolean scrollable,boolean editable){
        return new RawNode(id,parent,1,"android.view.View",viewId,value,"",l,t,r,b,clickable,scrollable,editable);
    }
}
