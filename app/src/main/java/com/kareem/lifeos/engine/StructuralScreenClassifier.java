package com.kareem.lifeos.engine;

/**
 * First structural classifier. It intentionally uses geometry and accessibility roles only;
 * visible strings are not used to decide whether a screen is a conversation.
 */
public final class StructuralScreenClassifier {
    private StructuralScreenClassifier(){}

    public static ScreenState classify(RawScreenSnapshot s){
        if(s==null||s.nodes.isEmpty())return new ScreenState(ScreenState.Type.UNKNOWN,0.0);
        int editableBottom=0,scrollRegions=0,textNodes=0,lowerActions=0;
        for(RawNode n:s.nodes){
            if(n.hasText())textNodes++;
            if(n.scrollable)scrollRegions++;
            if(n.editable&&n.centerY()>(s.screenHeight*2/3))editableBottom++;
            if(n.clickable&&n.centerY()>(s.screenHeight*2/3))lowerActions++;
        }
        // A conversation typically exposes a scrolling history plus a composer/action area near the bottom.
        if(scrollRegions>0&&editableBottom>0&&textNodes>=2){
            double c=0.70;
            if(textNodes>=4)c+=0.10;
            if(lowerActions>0)c+=0.08;
            return new ScreenState(ScreenState.Type.CONVERSATION,Math.min(0.95,c));
        }
        // Repeated textual rows in a scroll container, with no composer, are structurally list-like.
        if(scrollRegions>0&&editableBottom==0&&textNodes>=4)return new ScreenState(ScreenState.Type.CONVERSATION_LIST,0.62);
        return new ScreenState(ScreenState.Type.UNKNOWN,0.35);
    }
}
