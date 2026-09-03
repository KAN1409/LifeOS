package com.kareem.lifeos.engine;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class WhatsAppOutgoingGroundTruthTest {
    @Test public void outgoingBubblesProduceOnlyFourBodies(){
        List<RawNode> ns=new ArrayList<RawNode>();
        ns.add(n(0,-1,0,"",0,0,1080,2400,false,false));
        ns.add(n(1,0,1,"",0,180,1080,1900,false,true));
        ns.add(n(2,1,2,"Today",430,760,650,820,false,false));
        addBubble(ns,3,"Testing",900);
        addBubble(ns,7,"1",1040);
        addBubble(ns,11,"2",1180);
        addBubble(ns,15,"3",1320);
        RawScreenSnapshot s=new RawScreenSnapshot("com.whatsapp",1000L,1080,2400,ns);
        List<StructuralElement> es=StructuralElementExtractor.extract(s);
        List<BubbleCandidate> bs=BubbleClusterer.cluster(s,es);
        assertEquals(4,bs.size());
        assertEquals("Testing",bs.get(0).text);assertEquals("1",bs.get(1).text);assertEquals("2",bs.get(2).text);assertEquals("3",bs.get(3).text);
        for(BubbleCandidate b:bs)assertEquals(BubbleCandidate.Sender.SELF,b.sender);
    }

    @Test public void combinedAccessibilityLabelStopsAtTimestamp(){
        RawNode x=n(1,0,1,"3 8:57 AM Delivered",600,600,950,680,false,false);
        assertEquals("3",BubbleTextExtractor.stripTrailingMetadata(x.text));
    }

    private static void addBubble(List<RawNode> ns,int id,String body,int top){
        ns.add(n(id,1,2,"",560,top,1030,top+105,false,false));
        ns.add(n(id+1,id,3,body,585,top+12,760,top+62,false,false));
        ns.add(n(id+2,id,3,"8:57 AM",790,top+64,890,top+91,false,false));
        ns.add(n(id+3,id,3,"Delivered",895,top+64,1015,top+91,false,false));
    }
    private static RawNode n(int id,int parent,int depth,String text,int l,int t,int r,int b,boolean clickable,boolean scrollable){return new RawNode(id,parent,depth,"android.view.View","",text,"",l,t,r,b,clickable,scrollable,false);}
}
