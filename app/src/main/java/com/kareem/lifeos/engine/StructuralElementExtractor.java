package com.kareem.lifeos.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Converts raw accessibility geometry/roles into coarse structural elements.
 * It deliberately does not use visible strings or phrase blacklists.
 */
public final class StructuralElementExtractor {
    private StructuralElementExtractor(){}

    public static List<StructuralElement> extract(RawScreenSnapshot s){
        if(s==null||s.nodes.isEmpty())return Collections.emptyList();
        List<StructuralElement> out=new ArrayList<StructuralElement>();
        for(RawNode n:s.nodes){
            StructuralElement.Role role=roleFor(n,s);
            out.add(new StructuralElement(role,n,confidence(role,n,s)));
        }
        Collections.sort(out,new Comparator<StructuralElement>(){
            @Override public int compare(StructuralElement a,StructuralElement b){
                int y=a.node.top-b.node.top;
                if(y!=0)return y;
                return a.node.left-b.node.left;
            }
        });
        return Collections.unmodifiableList(out);
    }

    private static StructuralElement.Role roleFor(RawNode n,RawScreenSnapshot s){
        int cy=n.centerY();
        int cx=n.centerX();
        int w=n.width();
        int h=n.height();
        boolean topBand=cy<s.screenHeight/6;
        boolean bottomBand=cy>(s.screenHeight*2/3);
        boolean middleBand=cy>s.screenHeight/6&&cy<(s.screenHeight*5/6);
        boolean nearCenter=Math.abs(cx-s.screenWidth/2)<s.screenWidth/8;
        boolean narrow=w<s.screenWidth*3/4;
        boolean compact=h<s.screenHeight/5;

        if(n.scrollable&&h>s.screenHeight/3)return StructuralElement.Role.SCROLL_REGION;
        if(n.editable&&bottomBand)return StructuralElement.Role.COMPOSER;
        if(topBand&&n.clickable&&compact)return StructuralElement.Role.TOP_BAR;
        if(bottomBand&&n.clickable&&!n.editable&&compact)return StructuralElement.Role.ACTION;

        // Centered, narrow, non-interactive text is marker-like evidence, not a message by default.
        if(middleBand&&nearCenter&&n.hasText()&&!n.clickable&&!n.editable&&narrow&&compact){
            return StructuralElement.Role.CENTER_MARKER;
        }

        // Message candidates live inside the central reading area, carry content, and are not controls.
        if(middleBand&&n.hasText()&&!n.clickable&&!n.editable&&compact){
            return StructuralElement.Role.MESSAGE_CANDIDATE;
        }
        return StructuralElement.Role.OTHER;
    }

    private static double confidence(StructuralElement.Role role,RawNode n,RawScreenSnapshot s){
        switch(role){
            case SCROLL_REGION:return n.scrollable?0.96:0.70;
            case COMPOSER:return n.editable?0.97:0.70;
            case TOP_BAR:return 0.78;
            case ACTION:return 0.82;
            case CENTER_MARKER:return 0.74;
            case MESSAGE_CANDIDATE:return 0.70;
            default:return 0.40;
        }
    }
}
