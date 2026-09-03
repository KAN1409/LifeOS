package com.kareem.lifeos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Ranks conversation situations and derives proactive suggestions/day summary from grounded evidence. */
final class ProactiveFeedEngine {
    static final class RankedConversation {
        final LifeDb.Conversation conversation;
        final ProactiveSummaryEngine.Result summary;
        final int score;
        RankedConversation(LifeDb.Conversation c,ProactiveSummaryEngine.Result s,int score){conversation=c;summary=s;this.score=score;}
    }
    static final class Suggestion {
        final String title,why,kind;
        final long eventId;
        final int score;
        Suggestion(String title,String why,String kind,long eventId,int score){this.title=title;this.why=why;this.kind=kind;this.eventId=eventId;this.score=score;}
    }
    static final class DaySummary {
        final String headline,detail;
        DaySummary(String headline,String detail){this.headline=headline;this.detail=detail;}
    }

    static List<RankedConversation> rank(LifeDb db,List<LifeDb.Conversation> conversations,long now){
        ArrayList<RankedConversation> out=new ArrayList<>();
        for(LifeDb.Conversation c:conversations){
            List<LifeDb.Event> events=db.eventsForThread(c.app,c.threadKey,c.label,120);
            ProactiveSummaryEngine.Result s=ProactiveSummaryEngine.summarizeConversation(c.label,events);
            long age=Math.max(0,now-c.latestAt);int recency=age<60*60*1000L?3:age<6*60*60*1000L?2:age<24*60*60*1000L?1:0;
            int volume=Math.min(2,c.count/4);
            int score=s.priority*10+recency+volume;
            out.add(new RankedConversation(c,s,score));
        }
        Collections.sort(out,new Comparator<RankedConversation>(){@Override public int compare(RankedConversation a,RankedConversation b){int q=Integer.compare(b.score,a.score);return q!=0?q:Long.compare(b.conversation.latestAt,a.conversation.latestAt);}});
        return out;
    }

    static List<Suggestion> suggestions(List<RankedConversation> ranked){
        ArrayList<Suggestion> out=new ArrayList<>();
        for(RankedConversation r:ranked){
            List<String> signals=r.summary.signals;
            String title=null,why=null,kind=null;int score=r.score;
            if(signals.contains("request")){title="Review what "+r.conversation.label+" asked for";why="A request was detected in this conversation.";kind="REQUEST";score+=8;}
            else if(signals.contains("commitment")){title="Follow up on the commitment with "+r.conversation.label;why="A commitment was mentioned and may still be open.";kind="COMMITMENT";score+=7;}
            else if(signals.contains("question")){title="Check the unanswered question from "+r.conversation.label;why="A question appears in the recent conversation.";kind="QUESTION";score+=5;}
            else if(signals.contains("schedule")){title="Review the plan mentioned with "+r.conversation.label;why="A time-sensitive plan or schedule was detected.";kind="SCHEDULE";score+=4;}
            if(title!=null)out.add(new Suggestion(title,why,kind,r.conversation.latestEventId,score));
        }
        Collections.sort(out,new Comparator<Suggestion>(){@Override public int compare(Suggestion a,Suggestion b){return Integer.compare(b.score,a.score);}});
        return out;
    }

    static DaySummary daySummary(List<RankedConversation> ranked,List<LifeDb.Loop> attention,int persistentActions){
        int requests=0,commitments=0,questions=0,schedules=0,active=0;
        for(RankedConversation r:ranked){if(r.conversation.latestAt>=System.currentTimeMillis()-24*60*60*1000L)active++;if(r.summary.signals.contains("request"))requests++;if(r.summary.signals.contains("commitment"))commitments++;if(r.summary.signals.contains("question"))questions++;if(r.summary.signals.contains("schedule"))schedules++;}
        int needs=attention==null?0:attention.size();
        String headline=needs>0?needs+" things need attention":(requests+commitments+questions+schedules>0?"Your day has a few open signals":"Nothing urgent surfaced today");
        String detail=active+" active conversations";
        if(requests>0)detail+=" · "+requests+" requests";
        if(commitments>0)detail+=" · "+commitments+" commitments";
        if(questions>0)detail+=" · "+questions+" questions";
        if(schedules>0)detail+=" · "+schedules+" plans";
        if(persistentActions>0)detail+=" · "+persistentActions+" agent actions";
        return new DaySummary(headline,detail);
    }
}
