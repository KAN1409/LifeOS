package com.kareem.lifeos;

import com.kareem.lifeos.engine.BubbleCandidate;
import com.kareem.lifeos.engine.BubbleClusterer;
import com.kareem.lifeos.engine.RawNode;
import com.kareem.lifeos.engine.RawScreenSnapshot;
import com.kareem.lifeos.engine.StructuralElement;
import com.kareem.lifeos.engine.StructuralElementExtractor;
import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class BubbleClustererTest {
    @Test public void groupsFragmentsByVisualParentAndIgnoresActions(){
        RawScreenSnapshot s=new RawScreenSnapshot("test",1,1080,2400,Arrays.asList(
            new RawNode(1,-1,0,"ScrollView","","","",0,250,1080,2000,false,true,false),
            new RawNode(10,1,1,"ViewGroup","","","",40,650,620,850,false,false,false),
            new RawNode(11,10,2,"TextView","","Hello","",70,680,430,740,false,false,false),
            new RawNode(12,10,2,"TextView","","there","",70,745,400,805,false,false,false),
            new RawNode(20,1,1,"ViewGroup","","","",520,1050,1030,1200,false,false,false),
            new RawNode(21,20,2,"TextView","","Got it","",650,1080,980,1150,false,false,false),
            new RawNode(30,1,1,"Button","","Copy","",300,2050,500,2160,true,false,false),
            new RawNode(31,1,1,"EditText","","","",80,2200,900,2340,true,false,true)
        ));
        List<StructuralElement> elements=StructuralElementExtractor.extract(s);
        List<BubbleCandidate> bubbles=BubbleClusterer.cluster(s,elements);
        assertEquals(2,bubbles.size());
        assertEquals("Hello there",bubbles.get(0).text);
        assertEquals(BubbleCandidate.Sender.OTHER,bubbles.get(0).sender);
        assertEquals("Got it",bubbles.get(1).text);
        assertEquals(BubbleCandidate.Sender.SELF,bubbles.get(1).sender);
        for(BubbleCandidate b:bubbles)assertFalse(b.text.contains("Copy"));
    }

    @Test public void centeredBubbleStaysUnknownInsteadOfInventingSender(){
        RawScreenSnapshot s=new RawScreenSnapshot("test",1,1080,2400,Arrays.asList(
            new RawNode(1,-1,0,"ScrollView","","","",0,250,1080,2000,false,true,false),
            new RawNode(10,1,1,"ViewGroup","","","",320,800,760,940,false,false,false),
            new RawNode(11,10,2,"TextView","","Ambiguous","",330,820,750,920,false,false,false)
        ));
        List<BubbleCandidate> bubbles=BubbleClusterer.cluster(s,StructuralElementExtractor.extract(s));
        assertEquals(1,bubbles.size());
        assertEquals(BubbleCandidate.Sender.UNKNOWN,bubbles.get(0).sender);
    }
}
