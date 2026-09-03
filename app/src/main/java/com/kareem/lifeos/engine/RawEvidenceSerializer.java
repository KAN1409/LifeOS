package com.kareem.lifeos.engine;

import java.util.ArrayList;
import java.util.List;

/** Compact, loss-preserving text serialization for raw accessibility snapshots. */
public final class RawEvidenceSerializer {
    private RawEvidenceSerializer(){}

    public static String snapshot(RawScreenSnapshot s){
        if(s==null)return "";
        StringBuilder out=new StringBuilder();
        out.append("v1\t").append(esc(s.packageName)).append('\t').append(s.capturedAt).append('\t').append(s.screenWidth).append('\t').append(s.screenHeight).append('\n');
        for(RawNode n:s.nodes){
            out.append(n.id).append('\t').append(n.parentId).append('\t').append(n.depth).append('\t')
               .append(esc(n.className)).append('\t').append(esc(n.viewId)).append('\t').append(esc(n.text)).append('\t').append(esc(n.contentDescription)).append('\t')
               .append(n.left).append(',').append(n.top).append(',').append(n.right).append(',').append(n.bottom).append('\t')
               .append(n.clickable?'1':'0').append(n.scrollable?'1':'0').append(n.editable?'1':'0').append('\n');
        }
        return out.toString();
    }

    public static RawScreenSnapshot restore(String payload){
        if(payload==null||payload.isEmpty())return null;
        String[] lines=payload.split("\\n",-1);if(lines.length==0)return null;
        String[] h=lines[0].split("\\t",-1);if(h.length<5||!"v1".equals(h[0]))return null;
        try{
            String pkg=unesc(h[1]);long at=Long.parseLong(h[2]);int width=Integer.parseInt(h[3]);int height=Integer.parseInt(h[4]);
            List<RawNode> nodes=new ArrayList<RawNode>();
            for(int i=1;i<lines.length;i++){
                if(lines[i].isEmpty())continue;String[] p=lines[i].split("\\t",-1);if(p.length<9)continue;
                String[] b=p[7].split(",",-1);if(b.length!=4)continue;String flags=p[8];
                nodes.add(new RawNode(Integer.parseInt(p[0]),Integer.parseInt(p[1]),Integer.parseInt(p[2]),unesc(p[3]),unesc(p[4]),unesc(p[5]),unesc(p[6]),Integer.parseInt(b[0]),Integer.parseInt(b[1]),Integer.parseInt(b[2]),Integer.parseInt(b[3]),flags.length()>0&&flags.charAt(0)=='1',flags.length()>1&&flags.charAt(1)=='1',flags.length()>2&&flags.charAt(2)=='1'));
            }
            return new RawScreenSnapshot(pkg,at,width,height,nodes);
        }catch(RuntimeException e){return null;}
    }

    static String esc(String value){if(value==null||value.isEmpty())return "";return value.replace("\\","\\\\").replace("\t","\\t").replace("\r","\\r").replace("\n","\\n");}
    static String unesc(String value){
        if(value==null||value.isEmpty())return "";StringBuilder out=new StringBuilder();boolean slash=false;
        for(int i=0;i<value.length();i++){char c=value.charAt(i);if(slash){if(c=='t')out.append('\t');else if(c=='r')out.append('\r');else if(c=='n')out.append('\n');else out.append(c);slash=false;}else if(c=='\\')slash=true;else out.append(c);}if(slash)out.append('\\');return out.toString();
    }
}
