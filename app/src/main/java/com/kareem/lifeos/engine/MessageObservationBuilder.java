package com.kareem.lifeos.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Converts structural bubble hypotheses into canonical screen message observations. */
public final class MessageObservationBuilder {
    private MessageObservationBuilder(){}

    public static List<MessageObservation> build(List<BubbleCandidate> bubbles){
        if(bubbles==null||bubbles.isEmpty())return Collections.emptyList();
        List<MessageObservation> out=new ArrayList<MessageObservation>();
        for(BubbleCandidate b:bubbles){
            if(b==null||b.text==null||b.text.trim().isEmpty())continue;
            MessageObservation.Direction d=MessageObservation.Direction.UNKNOWN;
            if(b.sender==BubbleCandidate.Sender.SELF)d=MessageObservation.Direction.OUT;
            else if(b.sender==BubbleCandidate.Sender.OTHER)d=MessageObservation.Direction.IN;
            out.add(new MessageObservation(d,b.text,b.left,b.top,b.right,b.bottom,b.confidence));
        }
        return Collections.unmodifiableList(out);
    }
}
