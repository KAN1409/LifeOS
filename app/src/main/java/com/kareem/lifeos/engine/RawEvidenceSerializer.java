package com.kareem.lifeos.engine;

import java.util.List;

/** Compact, loss-preserving text serialization for raw accessibility snapshots. */
public final class RawEvidenceSerializer {
    private RawEvidenceSerializer(){}

    public static String snapshot(RawScreenSnapshot s){
        if(s==null)return "";
        StringBuilder out=new StringBuilder();
        out.append("v1\t").append(esc(s.packageName)).append('\t').append(s.capturedAt).append('\t').append(s.screenWidth).append('\t').append(s.screenHeight).append('\n');
        List<RawNode> nodes=s.nodes;
        for(RawNode n:nodes){
            out.append(n.id).append('\t').append(n.parentId).append('\t').append(n.depth).append('\t')
               .append(esc(n.className)).append('\t').append(esc(n.viewId)).append('\t').append(esc(n.text)).append('\t').append(esc(n.contentDescription)).append('\t')
               .append(n.left).append(',').append(n.top).append(',').append(n.right).append(',').append(n.bottom).append('\t')
               .append(n.clickable?'1':'0').append(n.scrollable?'1':'0').append(n.editable?'1':'0').append('\n');
        }
        return out.toString();
    }

    static String esc(String value){
        if(value==null||value.isEmpty())return "";
        return value.replace("\\","\\\\").replace("\t","\\t").replace("\r","\\r").replace("\n","\\n");
    }
}
