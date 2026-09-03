package com.kareem.lifeos.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One visual message-bubble hypothesis assembled from structural message fragments. */
public final class BubbleCandidate {
    public enum Sender { SELF, OTHER, UNKNOWN }

    public final List<RawNode> nodes;
    public final String text;
    public final int left,top,right,bottom;
    public final Sender sender;
    public final double confidence;

    public BubbleCandidate(List<RawNode> nodes,String text,int left,int top,int right,int bottom,Sender sender,double confidence){
        this.nodes=Collections.unmodifiableList(new ArrayList<RawNode>(nodes));
        this.text=text==null?"":text;
        this.left=left;this.top=top;this.right=right;this.bottom=bottom;
        this.sender=sender==null?Sender.UNKNOWN:sender;
        this.confidence=Math.max(0.0,Math.min(1.0,confidence));
    }

    public int centerX(){return left+Math.max(0,right-left)/2;}
}
