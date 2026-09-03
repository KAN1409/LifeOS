package com.kareem.lifeos.engine;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

public class MessageObservationBuilderTest {
    @Test public void mapsSenderEvidenceToCanonicalDirection(){
        BubbleCandidate other=new BubbleCandidate(Collections.<RawNode>emptyList(),"hello",50,500,500,620,BubbleCandidate.Sender.OTHER,0.82);
        BubbleCandidate self=new BubbleCandidate(Collections.<RawNode>emptyList(),"hey",600,700,1030,820,BubbleCandidate.Sender.SELF,0.88);
        BubbleCandidate unknown=new BubbleCandidate(Collections.<RawNode>emptyList(),"maybe",350,900,730,1020,BubbleCandidate.Sender.UNKNOWN,0.55);
        List<MessageObservation> xs=MessageObservationBuilder.build(Arrays.asList(other,self,unknown));
        assertEquals(3,xs.size());
        assertEquals(MessageObservation.Direction.IN,xs.get(0).direction);
        assertEquals(MessageObservation.Direction.OUT,xs.get(1).direction);
        assertEquals(MessageObservation.Direction.UNKNOWN,xs.get(2).direction);
        assertEquals("SCREEN",xs.get(0).source);
        assertEquals("MESSAGE",xs.get(0).type);
    }

    @Test public void preservesTextBoundsAndConfidence(){
        BubbleCandidate b=new BubbleCandidate(Collections.<RawNode>emptyList(),"Hello there",100,400,800,560,BubbleCandidate.Sender.OTHER,0.73);
        MessageObservation m=MessageObservationBuilder.build(Collections.singletonList(b)).get(0);
        assertEquals("Hello there",m.text);
        assertEquals(100,m.left);assertEquals(400,m.top);assertEquals(800,m.right);assertEquals(560,m.bottom);
        assertEquals(0.73,m.confidence,0.0001);
    }
}
