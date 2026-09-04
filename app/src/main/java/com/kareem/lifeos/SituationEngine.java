package com.kareem.lifeos;

import com.kareem.lifeos.actions.PersistentActionQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Unifies conversation, attention, suggestions and evidence into one user-facing life situation. */
final class SituationEngine {
    static final class Situation {
        final String id,title,summary,why,status,app,threadKey;
        final long latestEventId,latestAt;
        final int score,eventCount,attentionCount,actionCount;
        final List<String> signals;
        final List<Long> loopIds,evidenceIds;
        final List<String> proposalIds;
        Situation(String id,String title,String summary,String why,String status,String app,String threadKey,long latestEventId,long latestAt,int score,int eventCount,int attentionCount,int actionCount,List<String> signals,List<Long> loopIds,List<Long> evidenceIds,List<String> proposalIds){this.id=id;this.title=title;this.summary=summary;this.why=why;this.status=status;this.app=app;this.threadKey=threadKey;this.latestEventId=latestEventId;this.latestAt=latestAt;this.score=score;this.eventCount=eventCount;this.attentionCount=attentionCount;this.actionCount=actionCount;this.signals=signals;this.loopIds=loopIds;this.evidenceIds=evidenceIds;this.proposalIds=proposalIds;}
    }

    static List<Situation> build(LifeDb db,List<LifeDb.Loop> loops,List<PersistentActionQueue.Item> actions,long now){
        List<ProactiveFeedEngine.RankedConversation> ranked=ProactiveFeedEngine.rank(db,db.recentConversations(40),now);
        ArrayList<Situation> out=new ArrayList<>();Set<Long> claimedLoops=new HashSet<>();
        for(ProactiveFeedEngine.RankedConversation r:ranked){
            LifeDb.Conversation c=r.conversation;List<LifeDb.Event> events=db.eventsForThread(c.app,c.threadKey,c.label,160);Set<Long> eventIds=new HashSet<>();for(LifeDb.Event e:events)eventIds.add(e.id);
            ArrayList<Long> loopIds=new ArrayList<>();ArrayList<Long> evidenceIds=new ArrayList<>();int attention=0,maxLoopPriority=0;LifeDb.Loop primaryLoop=null;
            if(loops!=null)for(LifeDb.Loop l:loops){LifeDb.Event e=db.eventById(l.evidenceId);if(e!=null&&EventSemantics.supportsLoop(e,l.kind)&&belongs(e,c,eventIds)){attention++;loopIds.add(l.id);evidenceIds.add(l.evidenceId);claimedLoops.add(l.id);if(primaryLoop==null||l.priority>primaryLoop.priority)primaryLoop=l;maxLoopPriority=Math.max(maxLoopPriority,l.priority);}}
            ArrayList<String> proposalIds=new ArrayList<>();int actionCount=0;if(actions!=null)for(PersistentActionQueue.Item a:actions)if(actionBelongs(a,c,eventIds)){proposalIds.add(a.proposal.proposalId);actionCount++;}

            // Raw activity never promotes itself. A Situation must carry unresolved state.
            if(attention==0&&actionCount==0)continue;

            int score=maxLoopPriority+Math.min(10,r.score)+actionCount*8;String status=attention>0?"NEEDS ATTENTION":"ACTION READY";
            String why=primaryLoop==null?actionCount+" action"+(actionCount==1?" is":"s are")+" ready for review.":why(primaryLoop.kind);
            String summary=primaryLoop==null?r.summary.summary:summary(primaryLoop,attention);
            String title=displayTitle(c.label,c.threadKey,c.app);
            out.add(new Situation(key(c.app,c.threadKey,c.label),title,summary,why,status,c.app,c.threadKey,c.latestEventId,c.latestAt,score,events.size(),attention,actionCount,new ArrayList<>(r.summary.signals),loopIds,evidenceIds,proposalIds));
        }
        if(loops!=null)for(LifeDb.Loop l:loops)if(!claimedLoops.contains(l.id)){LifeDb.Event e=db.eventById(l.evidenceId);if(e!=null&&EventSemantics.supportsLoop(e,l.kind)){ArrayList<Long> lids=new ArrayList<>();lids.add(l.id);ArrayList<Long> ev=new ArrayList<>();ev.add(e.id);ArrayList<String> sig=new ArrayList<>();sig.add(l.kind);String title=LifeDb.isConversationLike(e)?LifeDb.personLabel(e):LifeDb.friendlyApp(e.app);out.add(new Situation("attention:"+l.id,title,summary(l,1),why(l.kind),"NEEDS ATTENTION",e.app,e.threadKey,e.id,e.at,l.priority,1,1,0,sig,lids,ev,new ArrayList<String>()));}}
        Collections.sort(out,new Comparator<Situation>(){@Override public int compare(Situation a,Situation b){int x=Integer.compare(b.score,a.score);return x!=0?x:Long.compare(b.latestAt,a.latestAt);}});return out;
    }

    static Situation find(LifeDb db,List<LifeDb.Loop> loops,List<PersistentActionQueue.Item> actions,String id,long now){for(Situation s:build(db,loops,actions,now))if(s.id.equals(id))return s;return null;}
    static List<LifeDb.Event> evidence(LifeDb db,Situation s,int limit){
        if(s==null)return new ArrayList<>();
        if(!s.id.startsWith("attention:")&&s.threadKey!=null&&!s.threadKey.trim().isEmpty())return db.eventsForThread(s.app,s.threadKey,s.title,limit);
        LifeDb.Event seed=db.eventById(s.latestEventId);ArrayList<LifeDb.Event> out=new ArrayList<>();if(seed!=null)out.add(seed);return out;
    }
    private static String displayTitle(String label,String thread,String app){
        String x=safe(label).trim();
        if(x.contains("|")){String[] p=x.split("\\|");x=p[p.length-1].trim();}
        if(x.startsWith("com.")){x="";}
        if(blank(x)){x=safe(thread).trim();if(x.contains("|")){String[] p=x.split("\\|");x=p[p.length-1].trim();}}
        if(blank(x)){x=LifeDb.friendlyApp(app);}return x;
    }
    private static String summary(LifeDb.Loop loop,int count){String prefix;if("request".equals(loop.kind))prefix="Request";else if("commitment".equals(loop.kind))prefix="Open commitment";else if("appointment".equals(loop.kind))prefix="Scheduled plan";else if("deadline".equals(loop.kind))prefix="Dated obligation";else if("security".equals(loop.kind))prefix="Security change";else if("financial_alert".equals(loop.kind))prefix="Financial exception";else prefix="Open item";return prefix+" · "+clip(loop.title,220)+(count>1?" · "+(count-1)+" related open item"+(count==2?"":"s"):"");}
    private static String why(String kind){if("security".equals(kind))return "A change to account access needs verification.";if("financial_alert".equals(kind))return "A financial exception could require prompt action.";if("deadline".equals(kind))return "A dated obligation may be approaching.";if("appointment".equals(kind))return "A scheduled plan may need preparation.";if("commitment".equals(kind))return "A commitment is still open.";return "A direct request may still need a response.";}
    private static boolean belongs(LifeDb.Event e,LifeDb.Conversation c,Set<Long> ids){if(ids.contains(e.id))return true;if(!same(e.app,c.app))return false;if(!blank(c.threadKey)&&same(e.threadKey,c.threadKey))return true;return LifeDb.isConversationLike(e)&&LifeDb.personLabel(e).equalsIgnoreCase(c.label);}
    private static boolean actionBelongs(PersistentActionQueue.Item a,LifeDb.Conversation c,Set<Long> ids){for(String x:a.proposal.evidenceIds)try{if(ids.contains(Long.parseLong(x)))return true;}catch(Exception ignored){}String text=(a.proposal.target+" "+a.proposal.payloadSummary).toLowerCase(Locale.ROOT);return !blank(c.label)&&text.contains(c.label.toLowerCase(Locale.ROOT));}
    private static String key(String app,String thread,String label){return Integer.toHexString((safe(app)+"|"+safe(thread)+"|"+safe(label)).hashCode());}
    private static boolean same(String a,String b){return a==null?b==null:a.equals(b);}private static boolean blank(String s){return s==null||s.trim().isEmpty();}private static String safe(String s){return s==null?"":s;}private static String clip(String s,int n){s=safe(s).trim();return s.length()>n?s.substring(0,n)+"…":s;}
}
