package com.kareem.lifeos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
        final long eventId,loopId;
        final int score;
        Suggestion(String title,String why,String kind,long eventId,long loopId,int score){this.title=title;this.why=why;this.kind=kind;this.eventId=eventId;this.loopId=loopId;this.score=score;}
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

    static List<Suggestion> suggestions(LifeDb db,List<LifeDb.Loop> loops){
        ArrayList<Suggestion> out=new ArrayList<>();
        if(loops==null)return out;
        for(LifeDb.Loop loop:loops){LifeDb.Event event=db.eventById(loop.evidenceId);if(event==null)continue;String who=LifeDb.isConversationLike(event)?LifeDb.personLabel(event):LifeDb.friendlyApp(event.app);String quote=clip(loop.title,72),title,why;
            if("security".equals(loop.kind)){title="Verify the account security change";why=quote;}
            else if("financial_alert".equals(loop.kind)){title="Review the card or payment exception";why=quote;}
            else if("deadline".equals(loop.kind)){title="Handle the dated obligation";why=quote+(loop.dueAt>0?" · due date detected":"");}
            else if("appointment".equals(loop.kind)){title="Review the scheduled plan";why=who+" · "+quote;}
            else if("commitment".equals(loop.kind)){title="Follow up on the commitment";why=who+" · "+quote;}
            else{title="Respond to "+who;why=quote;}
            out.add(new Suggestion(title,why,displayKind(loop.kind),loop.evidenceId,loop.id,loop.priority));
        }
        Collections.sort(out,new Comparator<Suggestion>(){@Override public int compare(Suggestion a,Suggestion b){return Integer.compare(b.score,a.score);}});
        return out;
    }

    static DaySummary daySummary(List<RankedConversation> ranked,List<LifeDb.Loop> attention,int persistentActions){
        int requests=0,commitments=0,deadlines=0,alerts=0,active=0;
        for(RankedConversation r:ranked)if(r.conversation.latestAt>=System.currentTimeMillis()-24*60*60*1000L)active++;
        if(attention!=null)for(LifeDb.Loop loop:attention){if("request".equals(loop.kind))requests++;else if("commitment".equals(loop.kind))commitments++;else if("deadline".equals(loop.kind)||"appointment".equals(loop.kind))deadlines++;else if("security".equals(loop.kind)||"financial_alert".equals(loop.kind))alerts++;}
        int needs=attention==null?0:attention.size();
        String headline=needs>0?needs+" thing"+(needs==1?"":"s")+" worth checking":"Nothing urgent surfaced today";
        String detail="";
        if(alerts>0)detail+=alerts+" important alert"+(alerts==1?"":"s");
        if(deadlines>0)detail+=(detail.isEmpty()?"":" · ")+deadlines+" dated item"+(deadlines==1?"":"s");
        if(requests>0)detail+=(detail.isEmpty()?"":" · ")+requests+" request"+(requests==1?"":"s");
        if(commitments>0)detail+=(detail.isEmpty()?"":" · ")+commitments+" commitment"+(commitments==1?"":"s");
        if(persistentActions>0)detail+=(detail.isEmpty()?"":" · ")+persistentActions+" agent action"+(persistentActions==1?"":"s");
        if(detail.isEmpty())detail=active+" active conversations";
        return new DaySummary(headline,detail);
    }
    private static String displayKind(String kind){if("financial_alert".equals(kind))return "FINANCIAL";return kind==null?"ACTION":kind.toUpperCase();}
    private static String clip(String value,int max){String x=value==null?"":value.trim();return x.length()>max?x.substring(0,max)+"…":x;}
}
