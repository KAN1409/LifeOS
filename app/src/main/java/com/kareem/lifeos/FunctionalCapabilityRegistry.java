package com.kareem.lifeos;

import android.content.Context;
import com.kareem.lifeos.actions.PersistentActionQueue;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Truthful product capability registry. Only providers with a concrete backing source may appear.
 * No capability count is produced by scanning arbitrary text.
 */
final class FunctionalCapabilityRegistry {
    enum Availability { OPERATIONAL, SETUP_REQUIRED, UNAVAILABLE }
    static final class Capability {
        final String id,label,description,icon;final int color,count;final Availability availability;final String status;
        Capability(String id,String label,String description,String icon,int color,int count,Availability availability,String status){this.id=id;this.label=label;this.description=description;this.icon=icon;this.color=color;this.count=Math.max(0,count);this.availability=availability;this.status=status==null?"":status;}
        String countLine(){return availability==Availability.OPERATIONAL?count+" "+(count==1?singular(id):plural(id)):"Set up";}
    }
    static final class ObjectItem {
        final String capabilityId,objectId,title,summary,meta;final long eventId;
        ObjectItem(String capabilityId,String objectId,String title,String summary,String meta,long eventId){this.capabilityId=capabilityId;this.objectId=objectId;this.title=s(title);this.summary=s(summary);this.meta=s(meta);this.eventId=eventId;}
    }
    private FunctionalCapabilityRegistry(){}

    static List<Capability> all(Context c){
        ArrayList<Capability> out=new ArrayList<>();
        int conversations=ConversationRepository.count(c);out.add(new Capability("conversations","Conversations","Persisted conversation threads and their captured evidence",LifeOsIconView.ASK,LifeOsUi.BLUE,conversations,Availability.OPERATIONAL,"Captured by LifeOS"));
        int obligations=ObligationRepository.count(c);out.add(new Capability("commitments","Commitments","Confirmed unresolved obligations only",LifeOsIconView.COMMITMENT,LifeOsUi.GREEN,obligations,Availability.OPERATIONAL,"Canonical attention"));
        int decisions=DecisionRepository.count(c);out.add(new Capability("decisions","Decisions","Choices you explicitly recorded in LifeOS Decision Memory",LifeOsIconView.DECISION,LifeOsUi.PINK,decisions,Availability.OPERATIONAL,"Explicitly recorded by you"));
        ContactPersonRepository.Availability people=ContactPersonRepository.availability(c);out.add(new Capability("people","People","Your Android contacts",LifeOsIconView.PEOPLE,LifeOsUi.PURPLE,people==ContactPersonRepository.Availability.OPERATIONAL?ContactPersonRepository.count(c):0,people==ContactPersonRepository.Availability.OPERATIONAL?Availability.OPERATIONAL:Availability.SETUP_REQUIRED,people==ContactPersonRepository.Availability.OPERATIONAL?"Contacts connected":"Contacts permission required"));
        CalendarEventRepository.Availability events=CalendarEventRepository.availability(c);out.add(new Capability("events","Events","Real calendar events from Android Calendar",LifeOsIconView.EVENT,LifeOsUi.PURPLE,events==CalendarEventRepository.Availability.OPERATIONAL?CalendarEventRepository.recentAndUpcoming(c,1000).size():0,events==CalendarEventRepository.Availability.OPERATIONAL?Availability.OPERATIONAL:Availability.SETUP_REQUIRED,events==CalendarEventRepository.Availability.OPERATIONAL?"Calendar connected":"Calendar permission required"));
        return out;
    }

    static Capability find(Context c,String id){for(Capability x:all(c))if(x.id.equals(id))return x;return null;}
    static List<ObjectItem> list(Context c,String id,int limit){ArrayList<ObjectItem> out=new ArrayList<>();
        if("conversations".equals(id)){for(ConversationRepository.ConversationObject x:ConversationRepository.list(c,limit))out.add(new ObjectItem(id,x.id,x.label,x.preview,x.capturedCount+" captured items · "+LifeDb.friendlyApp(x.app),x.latestEventId));}
        else if("commitments".equals(id)){for(ObligationRepository.ObligationObject x:ObligationRepository.open(c,limit))out.add(new ObjectItem(id,x.id,x.title,x.summary,humanAction(x.action)+(x.evidenceCount>1?" · "+x.evidenceCount+" evidence items":""),x.latestEventId));}
        else if("decisions".equals(id)){for(DecisionRepository.DecisionObject x:DecisionRepository.list(c,limit)){String summary=!x.choice.isEmpty()?x.choice:!x.context.isEmpty()?x.context:"Recorded decision";String meta="Recorded "+new SimpleDateFormat("d MMM yyyy",Locale.getDefault()).format(new Date(x.createdAt))+" · Decision Memory";out.add(new ObjectItem(id,x.id,x.title,summary,meta,0));}}
        else if("people".equals(id)&&ContactPersonRepository.availability(c)==ContactPersonRepository.Availability.OPERATIONAL){for(ContactPersonRepository.PersonObject x:ContactPersonRepository.list(c,limit)){String summary=!x.phones.isEmpty()?x.phones.get(0):!x.emails.isEmpty()?x.emails.get(0):"Saved contact";out.add(new ObjectItem(id,x.id,x.name,summary,"Android Contacts",0));}}
        else if("events".equals(id)&&CalendarEventRepository.availability(c)==CalendarEventRepository.Availability.OPERATIONAL){for(CalendarEventRepository.CalendarEventObject x:CalendarEventRepository.recentAndUpcoming(c,limit)){String summary=!x.location.isEmpty()?x.location:x.description;String meta=new SimpleDateFormat("EEE, d MMM · HH:mm",Locale.getDefault()).format(new Date(x.begin));out.add(new ObjectItem(id,x.id,x.title.isEmpty()?"Calendar event":x.title,summary,meta,0));}}
        return out;
    }
    static ObjectItem load(Context c,String capabilityId,String objectId){for(ObjectItem x:list(c,capabilityId,3000))if(x.objectId.equals(s(objectId)))return x;return null;}
    static int pendingActionCount(Context c){return new PersistentActionQueue(c).pending().size();}

    private static String humanAction(String a){String x=s(a).replace('_',' ').toLowerCase(Locale.ROOT);if(x.isEmpty()||"none".equals(x))return "Needs attention";return Character.toUpperCase(x.charAt(0))+x.substring(1);}
    private static String singular(String id){if("conversations".equals(id))return "conversation";if("commitments".equals(id))return "commitment";if("decisions".equals(id))return "decision";if("people".equals(id))return "person";if("events".equals(id))return "event";return "item";}
    private static String plural(String id){if("conversations".equals(id))return "conversations";if("commitments".equals(id))return "commitments";if("decisions".equals(id))return "decisions";if("people".equals(id))return "people";if("events".equals(id))return "events";return "items";}
    private static String s(String x){return x==null?"":x.trim();}
}
