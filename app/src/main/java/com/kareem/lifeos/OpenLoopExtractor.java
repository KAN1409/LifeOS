package com.kareem.lifeos;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class OpenLoopExtractor {
    static final class Candidate {
        final String kind,title,fingerprint;
        final long dueAt;
        final double confidence;
        Candidate(String kind,String title,long dueAt,double confidence){this.kind=kind;this.title=title;this.dueAt=dueAt;this.confidence=confidence;this.fingerprint=sha(kind+"\n"+title.toLowerCase(Locale.ROOT));}
    }
    private static final Pattern TIME=Pattern.compile("(?i)(?:at|الساعة|الساعه)\\s*(1[0-2]|0?[1-9])(?::([0-5]\\d))?\\s*(am|pm|ص|م)?");
    private static final Pattern DATE_WORD=Pattern.compile("(?i)\\b(today|tomorrow|tonight|النهارده|النهاردة|بكرة|بكره|الليلة)\\b");
    private static final String[] REQUEST={"please","can you","could you","send me","remind me","لو سمحت","ممكن","ابعتلي","فكرني","عايزك","عاوزك"};
    private static final String[] COMMITMENT={"i will","i'll","will send","هعمل","هبعت","هكلم","هخلص","هراجع","هروح"};

    static List<Candidate> extract(String title,String body,long now){
        String text=((title==null?"":title)+" "+(body==null?"":body)).trim();
        String low=text.toLowerCase(Locale.ROOT);ArrayList<Candidate> out=new ArrayList<>();
        String kind=null;double confidence=.0;
        if(containsAny(low,COMMITMENT)){kind="commitment";confidence=.82;}
        else if(containsAny(low,REQUEST)){kind="request";confidence=.72;}
        Matcher tm=TIME.matcher(low);
        if(tm.find()){kind=kind==null?"appointment":kind;confidence=Math.max(confidence,.76);}
        if(kind!=null)out.add(new Candidate(kind,clip(text,180),0,confidence));
        return out;
    }
    private static boolean containsAny(String s,String[] xs){for(String x:xs)if(s.contains(x))return true;return false;}
    private static String clip(String s,int n){return s.length()<=n?s:s.substring(0,n)+"…";}
    private static String sha(String value){try{byte[] b=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder x=new StringBuilder();for(byte q:b)x.append(String.format(Locale.US,"%02x",q));return x.toString();}catch(Exception e){return Integer.toHexString(value.hashCode());}}
}
