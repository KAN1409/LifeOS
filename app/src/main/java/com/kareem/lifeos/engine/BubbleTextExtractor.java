package com.kareem.lifeos.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extracts message body while treating timestamp-row fragments as metadata, without status-word lists. */
public final class BubbleTextExtractor {
    private static final Pattern TIME=Pattern.compile("(?i)(^|\\s)([0-2]?\\d:[0-5]\\d(?:\\s*[AP]M)?)(?=\\s|$)");
    private BubbleTextExtractor(){}

    public static String body(List<RawNode> nodes){
        if(nodes==null||nodes.isEmpty())return "";
        List<RawNode> timeNodes=new ArrayList<RawNode>();for(RawNode n:nodes)if(hasTime(raw(n)))timeNodes.add(n);
        List<String> parts=new ArrayList<String>();
        for(RawNode n:nodes){
            String raw=raw(n);if(raw.isEmpty())continue;
            if(!hasTime(raw)&&sameMetadataRow(n,timeNodes))continue;
            String body=stripTrailingMetadata(raw);if(body.isEmpty())continue;
            if(!containsEquivalent(parts,body))parts.add(body);
        }
        StringBuilder out=new StringBuilder();for(String p:parts){if(out.length()>0)out.append(' ');out.append(p);}return out.toString().trim();
    }

    static String stripTrailingMetadata(String raw){
        if(raw==null)return "";String x=raw.trim();if(x.isEmpty())return "";
        Matcher m=TIME.matcher(x);int cut=-1;while(m.find())cut=m.start(2);
        if(cut>0)return x.substring(0,cut).trim();if(cut==0)return "";return x;
    }

    private static boolean sameMetadataRow(RawNode n,List<RawNode> timeNodes){
        for(RawNode t:timeNodes){if(t==n)continue;int overlap=Math.min(n.bottom,t.bottom)-Math.max(n.top,t.top);int minH=Math.max(1,Math.min(n.height(),t.height()));if(overlap*2>=minH&&Math.abs(n.centerY()-t.centerY())<=Math.max(8,minH))return true;}return false;
    }
    private static boolean hasTime(String x){return x!=null&&TIME.matcher(x).find();}
    private static String raw(RawNode n){return n==null?"":(!n.text.trim().isEmpty()?n.text.trim():n.contentDescription.trim());}
    private static boolean containsEquivalent(List<String> xs,String value){String n=ReconciliationKey.normalize(value);for(String x:xs)if(ReconciliationKey.normalize(x).equals(n))return true;return false;}
}
