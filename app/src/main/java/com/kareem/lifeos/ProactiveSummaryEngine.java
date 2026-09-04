package com.kareem.lifeos;

import java.util.ArrayList;
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

    private static final String[] TRANSACTIONAL={"charged","credit card","debit card","transaction","purchase","payment","receipt","invoice","renewed","renewal","subscription","google play","apple.com/bill","تم خصم","تم السحب","عملية شراء","معاملة","دفع","فاتورة","اشتراك"};
    private static final Pattern URL=Pattern.compile("https?://\\S+",Pattern.CASE_INSENSITIVE);

    static Result summarizeConversation(String label,List<LifeDb.Event> events){
        if(events==null||events.isEmpty())return new Result(label,"No readable conversation captured yet.","",new ArrayList<>(),0);
        LinkedHashSet<String> sentences=new LinkedHashSet<>();Set<String> signalSet=new LinkedHashSet<>();int priority=0;boolean granular=false;
        for(LifeDb.Event e:events)if(!"Visible conversation".equals(e.title)){granular=true;break;}
        for(LifeDb.Event e:events){String raw=clean(e.body);if(raw.isEmpty())continue;String low=raw.toLowerCase(Locale.ROOT);
            for(OpenLoopExtractor.Candidate candidate:OpenLoopExtractor.extract(e.title,raw,e.at)){signalSet.add(candidate.kind);priority=Math.max(priority,Math.max(1,candidate.priority/20));}
            if(machine(low))continue;if(raw.contains("?")&&raw.length()>=8){signalSet.add("question");priority=Math.max(priority,2);}if(URL.matcher(raw).find())signalSet.add("link");
            if(granular&&"Visible conversation".equals(e.title))continue;
            for(String s:split(raw)){String x=normalize(stripSender(s,label));if(noise(x)||x.length()<5||x.length()>220)continue;if(sentences.size()<3)sentences.add(x);}
        }
        String summary=join(sentences,2);if(summary.isEmpty())summary="No substantive recent message could be summarized.";else summary="Latest · "+summary;
        String why="";if(signalSet.contains("security"))why="A security change needs verification.";else if(signalSet.contains("financial_alert"))why="A financial exception needs review.";else if(signalSet.contains("deadline"))why="A dated obligation may need action.";else if(signalSet.contains("request"))why="A direct request may need a response.";else if(signalSet.contains("commitment"))why="A commitment may still need follow-up.";else if(signalSet.contains("appointment"))why="A scheduled plan was mentioned.";else if(signalSet.contains("question"))why="A question appears in the conversation.";
        return new Result(label,summary,why,new ArrayList<>(signalSet),priority);
    }

    static String signalLine(Result r){if(r==null||r.signals.isEmpty())return "";StringBuilder b=new StringBuilder();for(String s:r.signals){if(b.length()>0)b.append(" · ");b.append(s);}return b.toString();}
    private static boolean machine(String low){return any(low,TRANSACTIONAL)||low.contains("verification code")||low.contains("otp")||low.contains("security alert");}
    private static boolean any(String s,String[] xs){for(String x:xs)if(s.contains(x))return true;return false;}
    private static String clean(String s){return s==null?"":s.trim();}
    private static List<String> split(String s){ArrayList<String> out=new ArrayList<>();for(String x:s.replace('\n',' ').split("(?<=[.!?؟])\\s+|\\s{2,}"))if(!x.trim().isEmpty())out.add(x.trim());return out;}
    private static String normalize(String s){return s.replaceAll("\\s+"," ").trim();}
    private static String stripSender(String value,String label){String x=normalize(value),who=clean(label);if(!who.isEmpty()&&x.toLowerCase(Locale.ROOT).startsWith(who.toLowerCase(Locale.ROOT)+":"))return x.substring(who.length()+1).trim();return x;}
    private static boolean noise(String value){String x=clean(value),low=x.toLowerCase(Locale.ROOT);if(x.isEmpty()||low.startsWith("reacted ")||low.matches(".{1,80}:\\s*reacted .*"))return true;if(low.matches("[📷🎤📄💟]?\\s*(photo|sticker|voice message)(\\s*\\([^)]*\\))?"))return true;return x.replaceAll("[\\p{L}\\p{N}]","").length()==x.length();}
    private static String join(Set<String> xs,int max){StringBuilder b=new StringBuilder();int i=0;for(String x:xs){if(i++>=max)break;if(b.length()>0)b.append(" • ");b.append(x);}return b.toString();}
}
