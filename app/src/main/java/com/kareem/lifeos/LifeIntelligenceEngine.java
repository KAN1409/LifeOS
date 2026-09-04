package com.kareem.lifeos;

import android.content.Context;
import com.kareem.lifeos.memory.MemoryRecord;
import com.kareem.lifeos.memory.PersistentLifeMemoryStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/** Local-first personal knowledge retrieval shared by Search and Ask LifeOS. */
final class LifeIntelligenceEngine {
    static final class Result {final String kind,title,summary;final long eventId;final double score;Result(String kind,String title,String summary,long eventId,double score){this.kind=s(kind);this.title=UserFacingText.humanize(title);this.summary=UserFacingText.humanize(summary);this.eventId=eventId;this.score=score;}}
    private LifeIntelligenceEngine(){}

    static List<Result> search(Context context,String query,int limit){
        String q=norm(query);long now=System.currentTimeMillis();ArrayList<Result> all=new ArrayList<>();
        try(LifeDb db=new LifeDb(context)){
            for(AttentionStore.Item x:AttentionStore.get(context).openItems(100)){
                LifeDb.Event e=db.eventById(x.eventId);String who=e==null?"":LifeDb.personLabel(e);String title=e==null?(who.isEmpty()?x.summary:who):PresentationSemantics.title(context,e);String summary=e==null?x.summary:PresentationSemantics.summary(context,e);double score=120+x.priority+match(q,title+" "+summary+" "+x.reason)*30;all.add(new Result("Attention",title,summary+(x.reason.isEmpty()?"":" · "+UserFacingText.humanize(x.reason)),x.eventId,score));
            }
            for(LifeDb.Conversation c:db.recentConversations(100)){double m=match(q,c.label+" "+c.preview);if(q.isEmpty()||m>0)all.add(new Result("People",c.label,UserFacingText.humanize(c.preview),c.latestEventId,65+m*28+recency(c.latestAt,now)));}
            for(LifeDb.Decision d:db.recentDecisions(100)){double m=match(q,d.title+" "+d.context+" "+d.choice+" "+d.consequences);if(q.isEmpty()||m>0)all.add(new Result("Decision",d.title,d.choice.isEmpty()?d.context:d.choice,0,55+m*30+recency(d.createdAt,now)));}
            for(LifeDb.Event e:db.recentEvents(360)){String semanticTitle=PresentationSemantics.title(context,e),semanticSummary=PresentationSemantics.summary(context,e);double m=match(q,semanticTitle+" "+semanticSummary+" "+e.title+" "+e.body+" "+e.threadKey+" "+LifeDb.friendlyApp(e.app));if(q.isEmpty()?all.size()<30:m>0)all.add(new Result(PresentationSemantics.kind(context,e),semanticTitle,semanticSummary,e.id,45+m*32+recency(e.at,now)));}
        }
        try{List<MemoryRecord> memories=PersistentLifeMemoryStore.get(context).recall(query,null,Math.max(12,limit),now);for(MemoryRecord m:memories){double mm=match(q,m.subjectEntityId+" "+m.text+" "+m.category.name());all.add(new Result("Memory",m.subjectEntityId.isEmpty()?human(m.category.name()):m.subjectEntityId,m.text,0,75+mm*35+m.strength*10));}}catch(Throwable ignored){}
        Collections.sort(all,Comparator.comparingDouble((Result r)->r.score).reversed());LinkedHashMap<String,Result> dedupe=new LinkedHashMap<>();for(Result r:all){String k=norm(r.kind+"|"+r.title+"|"+r.summary);if(!dedupe.containsKey(k))dedupe.put(k,r);if(dedupe.size()>=Math.max(1,limit))break;}return new ArrayList<>(dedupe.values());
    }

    static String context(Context c,String question){List<Result> xs=search(c,question,12);StringBuilder b=new StringBuilder();b.append("GROUNDING FROM KAREEM'S LIFEOS:\n");int i=1;for(Result r:xs){b.append(i++).append(". [").append(r.kind).append("] ").append(r.title).append(" — ").append(clip(r.summary,360)).append('\n');}if(xs.isEmpty())b.append("No grounded matching evidence was found.\n");return b.toString();}
    static String fallbackAnswer(Context c,String question){List<Result> xs=search(c,question,5);if(xs.isEmpty())return "I couldn't find grounded information for that yet.";StringBuilder b=new StringBuilder("Here's what I found in your LifeOS:\n");for(Result r:xs)b.append("• ").append(r.title).append(r.summary.isEmpty()?"":" — "+clip(r.summary,180)).append('\n');return b.toString().trim();}

    private static double match(String q,String text){if(q.isEmpty())return 0.25;String n=norm(text);if(n.contains(q))return 1.0;String[] ts=q.split(" ");int hit=0,total=0;for(String t:ts){if(t.length()<2)continue;total++;if(n.contains(t))hit++;}return total==0?0:(double)hit/total;}
    private static double recency(long at,long now){long age=Math.max(0,now-at);return Math.max(0,20-(age/86400000.0));}
    private static String norm(String x){return s(x).toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+"," ").trim();}
    private static String clip(String x,int n){String v=s(x).trim();return v.length()>n?v.substring(0,n)+"…":v;}
    private static String human(String x){String v=s(x).toLowerCase(Locale.ROOT).replace('_',' ');return v.isEmpty()?"Memory":Character.toUpperCase(v.charAt(0))+v.substring(1);}
    private static String s(String x){return x==null?"":x;}
}
