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
            ArrayList<Long> loopIds=new ArrayList<>();ArrayList<Long> evidenceIds=new ArrayList<>();int attention=0;
            if(loops!=null)for(LifeDb.Loop l:loops){LifeDb.Event e=db.eventById(l.evidenceId);if(e!=null&&belongs(e,c,eventIds)){attention++;loopIds.add(l.id);evidenceIds.add(l.evidenceId);claimedLoops.add(l.id);}}
            ArrayList<String> proposalIds=new ArrayList<>();int actionCount=0;if(actions!=null)for(PersistentActionQueue.Item a:actions)if(actionBelongs(a,c,eventIds)){proposalIds.add(a.proposal.proposalId);actionCount++;}

            // A Situation is not just "something happened". It must contain an open loop,
            // an actionable proposal, or a semantic signal worth following over time.
            boolean meaningfulSignal=r.summary.priority>0 && !r.summary.signals.isEmpty();
            if(attention==0 && actionCount==0 && !meaningfulSignal)continue;

            int score=r.score+attention*15+actionCount*8;String status=attention>0?"NEEDS ATTENTION":actionCount>0?"ACTION READY":"ACTIVE";
            String why=r.summary.why;if(attention>0)why=attention+" related item"+(attention==1?"":"s")+" currently need attention.";else if(actionCount>0)why=actionCount+" related action"+(actionCount==1?" is":"s are")+" ready for review.";
            String title=displayTitle(c.label,c.threadKey,c.app);
            out.add(new Situation(key(c.app,c.threadKey,c.label),title,r.summary.summary,why,status,c.app,c.threadKey,c.latestEventId,c.latestAt,score,events.size(),attention,actionCount,new ArrayList<>(r.summary.signals),loopIds,evidenceIds,proposalIds));
        }
        if(loops!=null)for(LifeDb.Loop l:loops)if(!claimedLoops.contains(l.id)){LifeDb.Event e=db.eventById(l.evidenceId);if(e!=null){ArrayList<Long> lids=new ArrayList<>();lids.add(l.id);ArrayList<Long> ev=new ArrayList<>();ev.add(e.id);ArrayList<String> sig=new ArrayList<>();sig.add(l.kind);out.add(new Situation("attention:"+l.id,l.title,clip(e.body,260),"This item is currently open in Needs attention.","NEEDS ATTENTION",e.app,e.threadKey,e.id,e.at,50,1,1,0,sig,lids,ev,new ArrayList<String>()));}}
        Collections.sort(out,new Comparator<Situation>(){@Override public int compare(Situation a,Situation b){int x=Integer.compare(b.score,a.score);return x!=0?x:Long.compare(b.latestAt,a.latestAt);}});return out;
    }

    static Situation find(LifeDb db,List<LifeDb.Loop> loops,List<PersistentActionQueue.Item> actions,String id,long now){for(Situation s:build(db,loops,actions,now))if(s.id.equals(id))return s;return null;}
    static List<LifeDb.Event> evidence(LifeDb db,Situation s,int limit){if(s==null)return new ArrayList<>();if(s.threadKey!=null&&!s.threadKey.trim().isEmpty())return db.eventsForThread(s.app,s.threadKey,s.title,limit);LifeDb.Event seed=db.eventById(s.latestEventId);ArrayList<LifeDb.Event> out=new ArrayList<>();if(seed!=null)out.add(seed);return out;}
    private static String displayTitle(String label,String thread,String app){
        String x=safe(label).trim();
        if(x.contains("|")){String[] p=x.split("\\|");x=p[p.length-1].trim();}
        if(x.startsWith("com.")){x="";}
        if(blank(x)){x=safe(thread).trim();if(x.contains("|")){String[] p=x.split("\\|");x=p[p.length-1].trim();}}
        if(blank(x)){x=friendlyApp(app);}return x;
    }
    private static String friendlyApp(String p){if(blank(p))return "Situation";int i=p.lastIndexOf('.');String x=i>=0?p.substring(i+1):p;if(blank(x))return "Situation";return Character.toUpperCase(x.charAt(0))+x.substring(1);}
    private static boolean belongs(LifeDb.Event e,LifeDb.Conversation c,Set<Long> ids){if(ids.contains(e.id))return true;if(!same(e.app,c.app))return false;if(!blank(c.threadKey)&&same(e.threadKey,c.threadKey))return true;return LifeDb.personLabel(e).equalsIgnoreCase(c.label);}
    private static boolean actionBelongs(PersistentActionQueue.Item a,LifeDb.Conversation c,Set<Long> ids){for(String x:a.proposal.evidenceIds)try{if(ids.contains(Long.parseLong(x)))return true;}catch(Exception ignored){}String text=(a.proposal.target+" "+a.proposal.payloadSummary).toLowerCase(Locale.ROOT);return !blank(c.label)&&text.contains(c.label.toLowerCase(Locale.ROOT));}
    private static String key(String app,String thread,String label){return Integer.toHexString((safe(app)+"|"+safe(thread)+"|"+safe(label)).hashCode());}
    private static boolean same(String a,String b){return a==null?b==null:a.equals(b);}private static boolean blank(String s){return s==null||s.trim().isEmpty();}private static String safe(String s){return s==null?"":s;}private static String clip(String s,int n){s=safe(s).trim();return s.length()>n?s.substring(0,n)+"…":s;}
}
