package com.kareem.lifeos.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds message-bubble hypotheses from hierarchy and geometry, then extracts only the message body. */
public final class BubbleClusterer {
    private BubbleClusterer(){}

    public static List<BubbleCandidate> cluster(RawScreenSnapshot s,List<StructuralElement> elements){
        if(s==null||elements==null||elements.isEmpty())return Collections.emptyList();
        Map<Integer,List<RawNode>> byContainer=new LinkedHashMap<Integer,List<RawNode>>();
        for(StructuralElement e:elements){
            if(e.role!=StructuralElement.Role.MESSAGE_CANDIDATE)continue;
            RawNode n=e.node;int key=bubbleContainerId(s,n);
            List<RawNode> group=byContainer.get(key);if(group==null){group=new ArrayList<RawNode>();byContainer.put(key,group);}group.add(n);
        }
        List<BubbleCandidate> out=new ArrayList<BubbleCandidate>();
        for(Map.Entry<Integer,List<RawNode>> entry:byContainer.entrySet()){
            List<RawNode> group=entry.getValue();if(group.isEmpty())continue;
            Collections.sort(group,new Comparator<RawNode>(){@Override public int compare(RawNode a,RawNode b){int y=a.top-b.top;return y!=0?y:a.left-b.left;}});
            RawNode container=findNode(s,entry.getKey());
            int l=container!=null?container.left:Integer.MAX_VALUE,t=container!=null?container.top:Integer.MAX_VALUE,r=container!=null?container.right:0,b=container!=null?container.bottom:0;
            if(container==null)for(RawNode n:group){l=Math.min(l,n.left);t=Math.min(t,n.top);r=Math.max(r,n.right);b=Math.max(b,n.bottom);}
            String body=BubbleTextExtractor.body(group);if(body.isEmpty())continue;
            // Ancestors are useful for grouping but WhatsApp may give them nearly
            // full-row bounds. Direction must come from the tight text/metadata row.
            int tightLeft=Integer.MAX_VALUE,tightRight=0;
            for(RawNode n:group){tightLeft=Math.min(tightLeft,n.left);tightRight=Math.max(tightRight,n.right);}
            int cx=tightLeft+Math.max(0,tightRight-tightLeft)/2;BubbleCandidate.Sender sender=BubbleCandidate.Sender.UNKNOWN;double confidence=0.60;int margin=s.screenWidth/12;
            if(tightRight<s.screenWidth/2+margin){sender=BubbleCandidate.Sender.OTHER;confidence=0.80;}
            else if(tightLeft>s.screenWidth/2-margin){sender=BubbleCandidate.Sender.SELF;confidence=0.80;}
            else if(cx<s.screenWidth/2-margin){sender=BubbleCandidate.Sender.OTHER;confidence=0.72;}
            else if(cx>s.screenWidth/2+margin){sender=BubbleCandidate.Sender.SELF;confidence=0.72;}
            out.add(new BubbleCandidate(group,body,l,t,r,b,sender,confidence));
        }
        Collections.sort(out,new Comparator<BubbleCandidate>(){@Override public int compare(BubbleCandidate a,BubbleCandidate b){int y=a.top-b.top;return y!=0?y:a.left-b.left;}});
        return Collections.unmodifiableList(out);
    }

    private static int bubbleContainerId(RawScreenSnapshot s,RawNode n){
        RawNode current=n,best=n;
        for(int hops=0;hops<8;hops++){
            RawNode p=findNode(s,current.parentId);if(p==null||p.scrollable||p.editable)break;
            if(p.width()>=s.screenWidth*9/10)break;
            if(p.height()>s.screenHeight/3)break;
            best=p;current=p;
        }
        return best.id;
    }

    private static RawNode findNode(RawScreenSnapshot s,int id){if(id<0)return null;for(RawNode n:s.nodes)if(n.id==id)return n;return null;}
}
