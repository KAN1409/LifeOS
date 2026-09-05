package com.kareem.lifeos;

import android.content.Context;
import com.kareem.lifeos.actions.PersistentActionQueue;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Single source of truth for user-visible LifeOS capabilities.
 * Counts and previews are grounded in the local database; no decorative/fake numbers.
 */
final class LifeOsCapabilityRegistry {
    static final class Capability {
        final String id,label,description,icon,query;
        final int color,count;
        final String countLabel,secondary;
        Capability(String id,String label,String description,String icon,int color,String query,int count,String countLabel,String secondary){
            this.id=id;this.label=label;this.description=description;this.icon=icon;this.color=color;this.query=query;
            this.count=Math.max(0,count);this.countLabel=countLabel==null?"":countLabel;this.secondary=secondary==null?"":secondary;
        }
        String primaryLine(){return count+" "+countLabel;}
    }

    private LifeOsCapabilityRegistry(){}

    static List<Capability> all(Context context){
        ArrayList<Capability> out=new ArrayList<>();
        try(LifeDb db=new LifeDb(context)){
            List<LifeDb.Conversation> conversations=db.recentConversations(300);
            Set<String> people=new HashSet<>();int activePeople=0;long week=System.currentTimeMillis()-7L*86400000L;
            for(LifeDb.Conversation c:conversations){String p=clean(c.label).toLowerCase(Locale.ROOT);if(!p.isEmpty())people.add(p);if(c.latestAt>=week)activePeople++;}
            int decisions=db.recentDecisions(1000).size();
            int commitments=db.openLoopCount();
            int files=countEvents(context,db,"Files");
            int places=countEvents(context,db,"Places");
            int events=countEvents(context,db,"Events");
            int projects=countProjects(context);
            out.add(new Capability("people","People","People LifeOS recognizes across conversations",LifeOsIconView.PEOPLE,LifeOsUi.PURPLE,"people",people.size(),"people",activePeople+" active this week"));
            out.add(new Capability("conversations","Conversations","Conversation threads connected across apps",LifeOsIconView.ASK,LifeOsUi.BLUE,"conversation",conversations.size(),"threads","recently captured"));
            out.add(new Capability("files","Files","Documents, photos and attachments",LifeOsIconView.FILE,LifeOsUi.AMBER,"file document attachment",files,"known items","from grounded evidence"));
            out.add(new Capability("decisions","Decisions","Past decisions and their context",LifeOsIconView.DECISION,LifeOsUi.PINK,"decision",decisions,"decisions","remembered by LifeOS"));
            out.add(new Capability("places","Places","Locations and arrival context",LifeOsIconView.PLACE,LifeOsUi.BLUE,"location place arrived office",places,"known places","from captured context"));
            out.add(new Capability("events","Events","Meetings, appointments and reminders",LifeOsIconView.EVENT,LifeOsUi.PURPLE,"meeting appointment reminder calendar event",events,"known events","grounded in your timeline"));
            out.add(new Capability("projects","Projects","Project-related work, files and conversations",LifeOsIconView.FILE,LifeOsUi.BLUE,"project",projects,"project threads","connected context"));
            out.add(new Capability("commitments","Commitments","Open promises, requests and follow-ups",LifeOsIconView.COMMITMENT,LifeOsUi.GREEN,"open commitment follow up waiting",commitments,"open","need a future outcome"));
        }
        return out;
    }

    static Capability find(Context context,String id){for(Capability c:all(context))if(c.id.equals(id))return c;return all(context).get(0);}

    static List<LifeIntelligenceEngine.Result> browse(Context context,String id,int limit){
        ArrayList<LifeIntelligenceEngine.Result> out=new ArrayList<>();
        try(LifeDb db=new LifeDb(context)){
            if("people".equals(id)||"conversations".equals(id)){
                for(LifeDb.Conversation c:db.recentConversations(Math.max(limit,80))){
                    out.add(new LifeIntelligenceEngine.Result("people".equals(id)?"People":"Conversation",c.label,UserFacingText.humanize(c.preview),c.latestEventId,80+c.count));
                    if(out.size()>=limit)break;
                }
                return out;
            }
            if("decisions".equals(id)){
                for(LifeDb.Decision d:db.recentDecisions(Math.max(limit,80))){
                    String s=d.choice.isEmpty()?d.context:d.choice;
                    out.add(new LifeIntelligenceEngine.Result("Decision",d.title,s,0,70));if(out.size()>=limit)break;
                }
                return out;
            }
            if("commitments".equals(id)){
                for(LifeDb.Loop l:db.openLoops(Math.max(limit,80))){
                    LifeDb.Event e=db.eventById(l.evidenceId);String title=e==null?l.title:PresentationSemantics.title(context,e);String summary=e==null?l.title:PresentationSemantics.summary(context,e);
                    out.add(new LifeIntelligenceEngine.Result("Commitment",title,summary,l.evidenceId,90+l.priority));if(out.size()>=limit)break;
                }
                return out;
            }
        }
        Capability c=find(context,id);List<LifeIntelligenceEngine.Result> raw=LifeIntelligenceEngine.search(context,c.query,Math.max(limit*3,60));LinkedHashMap<String,LifeIntelligenceEngine.Result> dedupe=new LinkedHashMap<>();
        for(LifeIntelligenceEngine.Result r:raw){if(matches(id,r)){String k=(r.kind+"|"+r.title).toLowerCase(Locale.ROOT);if(!dedupe.containsKey(k))dedupe.put(k,r);}if(dedupe.size()>=limit)break;}
        out.addAll(dedupe.values());return out;
    }

    static LifeIntelligenceEngine.Result first(Context context,String id){List<LifeIntelligenceEngine.Result> xs=browse(context,id,1);return xs.isEmpty()?null:xs.get(0);}

    private static int countEvents(Context context,LifeDb db,String id){int n=0;Set<String> seen=new HashSet<>();for(LifeDb.Event e:db.recentEvents(1000)){String kind=PresentationSemantics.kind(context,e);String text=(e.title+" "+e.body+" "+kind).toLowerCase(Locale.ROOT);boolean yes;
        if("Files".equals(id))yes="File".equalsIgnoreCase(kind)||text.matches(".*\\.(pdf|docx?|xlsx?|pptx?|jpg|jpeg|png|zip)(\\s|$).*" )||text.contains("attachment")||text.contains("document");
        else if("Places".equals(id))yes="Place".equalsIgnoreCase(kind)||text.contains("arrived at")||text.contains("location")||text.contains("zamalek")||text.contains("office");
        else yes="Event".equalsIgnoreCase(kind)||"Reminder".equalsIgnoreCase(kind)||text.contains("meeting")||text.contains("appointment")||text.contains("calendar");
        if(yes){String k=(kind+"|"+PresentationSemantics.title(context,e)).toLowerCase(Locale.ROOT);if(seen.add(k))n++;}
    }return n;}

    private static int countProjects(Context context){Set<String> seen=new HashSet<>();for(LifeIntelligenceEngine.Result r:LifeIntelligenceEngine.search(context,"project",120)){String t=clean(r.title);String s=clean(r.summary);String both=(t+" "+s).toLowerCase(Locale.ROOT);if(!both.contains("project"))continue;String k=t.toLowerCase(Locale.ROOT);if(!k.isEmpty())seen.add(k);}return seen.size();}

    private static boolean matches(String id,LifeIntelligenceEngine.Result r){String kind=clean(r.kind).toLowerCase(Locale.ROOT);String x=(r.title+" "+r.summary+" "+r.kind).toLowerCase(Locale.ROOT);
        if("files".equals(id))return kind.contains("file")||x.contains("attachment")||x.contains("document")||x.matches(".*\\.(pdf|docx?|xlsx?|pptx?|jpg|jpeg|png|zip).*" );
        if("places".equals(id))return kind.contains("place")||x.contains("location")||x.contains("arrived")||x.contains("office")||x.contains("zamalek")||x.contains("cairo");
        if("events".equals(id))return kind.contains("event")||kind.contains("reminder")||x.contains("meeting")||x.contains("appointment")||x.contains("calendar");
        if("projects".equals(id))return x.contains("project");
        return true;
    }
    private static String clean(String s){return s==null?"":s.trim();}
}
