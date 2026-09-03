package com.kareem.lifeos.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Associates marker-like structural evidence to nearby messages conservatively. */
public final class MessageMetadataAssociator {
    private MessageMetadataAssociator(){}
    public static List<MessageMetadataEvidence> associate(List<StructuralElement> elements,List<MessageObservation> messages){
        if(elements==null||elements.isEmpty())return Collections.emptyList();
        List<MessageMetadataEvidence> out=new ArrayList<MessageMetadataEvidence>();
        for(StructuralElement e:elements){
            if(e==null||e.node==null||!e.node.hasText())continue;
            if(e.role==StructuralElement.Role.CENTER_MARKER){
                out.add(new MessageMetadataEvidence(MessageMetadataEvidence.Kind.SYSTEM_MARKER,MessageMetadataEvidence.Relation.UNASSOCIATED,text(e.node),e.node.id,-1,e.confidence));
            }else if(e.role==StructuralElement.Role.OTHER&&messages!=null&&!messages.isEmpty()){
                int best=-1;int bestGap=Integer.MAX_VALUE;MessageMetadataEvidence.Relation rel=MessageMetadataEvidence.Relation.UNASSOCIATED;
                for(int i=0;i<messages.size();i++){
                    MessageObservation m=messages.get(i);int gap;
                    if(e.node.bottom<=m.top){gap=m.top-e.node.bottom;if(gap<bestGap){bestGap=gap;best=i;rel=MessageMetadataEvidence.Relation.BEFORE;}}
                    else if(e.node.top>=m.bottom){gap=e.node.top-m.bottom;if(gap<bestGap){bestGap=gap;best=i;rel=MessageMetadataEvidence.Relation.AFTER;}}
                    else {bestGap=0;best=i;rel=MessageMetadataEvidence.Relation.OVERLAPS;break;}
                }
                if(best>=0&&bestGap<180)out.add(new MessageMetadataEvidence(MessageMetadataEvidence.Kind.ADJACENT_DECORATION,rel,text(e.node),e.node.id,best,0.55));
            }
        }
        return Collections.unmodifiableList(out);
    }
    private static String text(RawNode n){String t=n.text==null?"":n.text.trim();return !t.isEmpty()?t:(n.contentDescription==null?"":n.contentDescription.trim());}
}
