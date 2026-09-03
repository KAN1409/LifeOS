package com.kareem.lifeos.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds visual message-bubble hypotheses from MESSAGE_CANDIDATE elements.
 * It uses hierarchy, geometry and alignment only; visible words are never used for classification.
 */
public final class BubbleClusterer {
    private BubbleClusterer(){}

    public static List<BubbleCandidate> cluster(RawScreenSnapshot s,List<StructuralElement> elements){
        if(s==null||elements==null||elements.isEmpty())return Collections.emptyList();
        Map<Integer,List<RawNode>> byParent=new LinkedHashMap<Integer,List<RawNode>>();
        for(StructuralElement e:elements){
            if(e.role!=StructuralElement.Role.MESSAGE_CANDIDATE)continue;
            RawNode n=e.node;
            int key=n.parentId>=0?n.parentId:n.id;
            List<RawNode> group=byParent.get(key);
            if(group==null){group=new ArrayList<RawNode>();byParent.put(key,group);}
            group.add(n);
        }
        List<BubbleCandidate> out=new ArrayList<BubbleCandidate>();
        for(List<RawNode> group:byParent.values()){
            if(group.isEmpty())continue;
            Collections.sort(group,new Comparator<RawNode>(){
                @Override public int compare(RawNode a,RawNode b){int y=a.top-b.top;return y!=0?y:a.left-b.left;}
            });
            int l=Integer.MAX_VALUE,t=Integer.MAX_VALUE,r=0,b=0;
            StringBuilder text=new StringBuilder();
            for(RawNode n:group){
                l=Math.min(l,n.left);t=Math.min(t,n.top);r=Math.max(r,n.right);b=Math.max(b,n.bottom);
                String part=!n.text.trim().isEmpty()?n.text.trim():n.contentDescription.trim();
                if(!part.isEmpty()){if(text.length()>0)text.append(" ");text.append(part);}
            }
            int cx=l+Math.max(0,r-l)/2;
            BubbleCandidate.Sender sender=BubbleCandidate.Sender.UNKNOWN;
            double confidence=0.58;
            int margin=s.screenWidth/10;
            if(cx<s.screenWidth/2-margin){sender=BubbleCandidate.Sender.OTHER;confidence=0.74;}
            else if(cx>s.screenWidth/2+margin){sender=BubbleCandidate.Sender.SELF;confidence=0.74;}
            out.add(new BubbleCandidate(group,text.toString(),l,t,r,b,sender,confidence));
        }
        Collections.sort(out,new Comparator<BubbleCandidate>(){
            @Override public int compare(BubbleCandidate a,BubbleCandidate b){int y=a.top-b.top;return y!=0?y:a.left-b.left;}
        });
        return Collections.unmodifiableList(out);
    }
}
