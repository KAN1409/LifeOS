package com.kareem.lifeos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** High-precision local summarizer used to turn captured evidence into proactive feed signals. */
final class ProactiveSummaryEngine {
    static final class Result {
        final String headline,summary,why;
        final List<String> signals;
        final int priority;
        Result(String headline,String summary,String why,List<String> signals,int priority){this.headline=headline;this.summary=summary;this.why=why;this.signals=signals;this.priority=priority;}
    }

    private static final String[] REQUEST={"please","can you","could you","send me","remind me","لو سمحت","ممكن","ابعتلي","فكرني","عايزك","عاوزك"};
    private static final String[] COMMITMENT={"i will","i'll","will send","i can send","هعمل","هبعت","هكلم","هخلص","هراجع","هروح"};
    private static final String[] SCHEDULE={"meeting","appointment","booked","booking","reservation","موعد","حجز","مقابلة","اجتماع","معاد","tomorrow","today","tonight","بكرة","بكره","النهارده","النهاردة","الليلة"};
    private static final String[] TRANSACTIONAL={"charged","credit card","debit card","transaction","purchase","payment","receipt","invoice","renewed","renewal","subscription","google play","apple.com/bill","تم خصم","تم السحب","عملية شراء","معاملة","دفع","فاتورة","اشتراك"};
    private static final Pattern URL=Pattern.compile("https?://\\S+",Pattern.CASE_INSENSITIVE);

    static Result summarizeConversation(String label,List<LifeDb.Event> events){
        if(events==null||events.isEmpty())return new Result(label,"No readable conversation captured yet.","",new ArrayList<>(),0);
        LinkedHashSet<String> sentences=new LinkedHashSet<>();Set<String> signalSet=new LinkedHashSet<>();int priority=0;
        for(LifeDb.Event e:events){String raw=clean(e.body);if(raw.isEmpty())continue;String low=raw.toLowerCase(Locale.ROOT);if(machine(low))continue;
            if(raw.contains("?")){signalSet.add("question");priority=Math.max(priority,2);}if(any(low,REQUEST)){signalSet.add("request");priority=Math.max(priority,3);}if(any(low,COMMITMENT)){signalSet.add("commitment");priority=Math.max(priority,3);}if(any(low,SCHEDULE)){signalSet.add("schedule");priority=Math.max(priority,2);}if(URL.matcher(raw).find())signalSet.add("link");
            for(String s:split(raw)){String x=normalize(s);if(x.length()<8||x.length()>220)continue;if(sentences.size()<4)sentences.add(x);}
        }
        String summary=join(sentences,3);if(summary.isEmpty())summary="Conversation captured; no concise text summary yet.";
        String why="";if(signalSet.contains("request"))why="A request may need a response.";else if(signalSet.contains("commitment"))why="A commitment was mentioned and may need follow-up.";else if(signalSet.contains("question"))why="A question appears in the conversation.";else if(signalSet.contains("schedule"))why="A time-sensitive plan or schedule was mentioned.";
        return new Result(label,summary,why,new ArrayList<>(signalSet),priority);
    }

    static String signalLine(Result r){if(r==null||r.signals.isEmpty())return "";StringBuilder b=new StringBuilder();for(String s:r.signals){if(b.length()>0)b.append(" · ");b.append(s);}return b.toString();}
    private static boolean machine(String low){return any(low,TRANSACTIONAL)||low.contains("verification code")||low.contains("otp")||low.contains("security alert");}
    private static boolean any(String s,String[] xs){for(String x:xs)if(s.contains(x))return true;return false;}
    private static String clean(String s){return s==null?"":s.trim();}
    private static List<String> split(String s){ArrayList<String> out=new ArrayList<>();for(String x:s.replace('\n',' ').split("(?<=[.!?؟])\\s+|\\s{2,}"))if(!x.trim().isEmpty())out.add(x.trim());return out;}
    private static String normalize(String s){return s.replaceAll("\\s+"," ").trim();}
    private static String join(Set<String> xs,int max){StringBuilder b=new StringBuilder();int i=0;for(String x:xs){if(i++>=max)break;if(b.length()>0)b.append(" • ");b.append(x);}return b.toString();}
}
