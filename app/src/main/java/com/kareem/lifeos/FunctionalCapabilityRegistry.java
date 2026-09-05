package com.kareem.lifeos;

import android.content.Context;
import com.kareem.lifeos.actions.PersistentActionQueue;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Truthful product capability registry. Every visible capability has a concrete backing provider.
 * Counts come from typed providers, never from scanning arbitrary notification text.
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
        int voice=VoiceMemoryRepository.count(c);out.add(new Capability("voice","Voice memories","WAV recordings you explicitly capture in LifeOS",LifeOsIconView.MIC,LifeOsUi.BLUE,voice,Availability.OPERATIONAL,"Local audio · transcript when available"));
        int files=FileRepository.count(c);out.add(new Capability("files","Files","Documents you explicitly connected through Android's document picker",LifeOsIconView.FILE,LifeOsUi.AMBER,files,Availability.OPERATIONAL,"Persisted document access"));
        int projects=ProjectRepository.count(c);out.add(new Capability("projects","Projects","Project objects you explicitly create and manage in LifeOS",LifeOsIconView.FILE,LifeOsUi.BLUE,projects,Availability.OPERATIONAL,"User-created projects"));
        int placeCount=PlaceRepository.count(c);PlaceRepository.Availability placeAccess=PlaceRepository.availability(c);Availability placeAvailability=placeCount>0||placeAccess==PlaceRepository.Availability.OPERATIONAL?Availability.OPERATIONAL:Availability.SETUP_REQUIRED;String placeStatus=placeAccess==PlaceRepository.Availability.OPERATIONAL?"Location available for saving places":placeCount>0?"Saved places available · location needed to add more":"Location permission required";out.add(new Capability("places","Places","Locations you explicitly save from Android location evidence",LifeOsIconView.PLACE,LifeOsUi.BLUE,placeCount,placeAvailability,placeStatus));
        ContactPersonRepository.Availability people=ContactPersonRepository.availability(c);out.add(new Capability("people","People","Your Android contacts",LifeOsIconView.PEOPLE,LifeOsUi.PURPLE,people==ContactPersonRepository.Availability.OPERATIONAL?ContactPersonRepository.count(c):0,people==ContactPersonRepository.Availability.OPERATIONAL?Availability.OPERATIONAL:Availability.SETUP_REQUIRED,people==ContactPersonRepository.Availability.OPERATIONAL?"Contacts connected":"Contacts permission required"));
        CalendarEventRepository.Availability events=CalendarEventRepository.availability(c);out.add(new Capability("events","Events","Real calendar events from Android Calendar",LifeOsIconView.EVENT,LifeOsUi.PURPLE,events==CalendarEventRepository.Availability.OPERATIONAL?CalendarEventRepository.recentAndUpcoming(c,1000).size():0,events==CalendarEventRepository.Availability.OPERATIONAL?Availability.OPERATIONAL:Availability.SETUP_REQUIRED,events==CalendarEventRepository.Availability.OPERATIONAL?"Calendar connected":"Calendar permission required"));
        return out;
    }

    static Capability find(Context c,String id){for(Capability x:all(c))if(x.id.equals(id))return x;return null;}
    static List<ObjectItem> list(Context c,String id,int limit){ArrayList<ObjectItem> out=new ArrayList<>();
        if("conversations".equals(id)){for(ConversationRepository.ConversationObject x:ConversationRepository.list(c,limit))out.add(new ObjectItem(id,x.id,x.label,x.preview,x.capturedCount+" captured items · "+LifeDb.friendlyApp(x.app),x.latestEventId));}
        else if("commitments".equals(id)){for(ObligationRepository.ObligationObject x:ObligationRepository.open(c,limit))out.add(new ObjectItem(id,x.id,x.title,x.summary,humanAction(x.action)+(x.evidenceCount>1?" · "+x.evidenceCount+" evidence items":""),x.latestEventId));}
        else if("decisions".equals(id)){for(DecisionRepository.DecisionObject x:DecisionRepository.list(c,limit)){String summary=!x.choice.isEmpty()?x.choice:!x.context.isEmpty()?x.context:"Recorded decision";String meta="Recorded "+new SimpleDateFormat("d MMM yyyy",Locale.getDefault()).format(new Date(x.createdAt))+" · Decision Memory";out.add(new ObjectItem(id,x.id,x.title,summary,meta,0));}}
        else if("voice".equals(id)){for(VoiceMemoryRepository.VoiceObject x:VoiceMemoryRepository.list(c,limit)){String title=x.hasTranscript()?clip(x.transcript,72):"Voice memory";String summary=x.hasTranscript()?x.transcript:"Original WAV saved; transcript "+humanState(x.status);String meta=new SimpleDateFormat("d MMM yyyy · HH:mm",Locale.getDefault()).format(new Date(x.createdAt))+" · "+duration(x.durationMs);out.add(new ObjectItem(id,x.id,title,summary,meta,0));}}
        else if("files".equals(id)){for(FileRepository.FileObject x:FileRepository.list(c,limit)){String summary=!x.mimeType.isEmpty()?x.mimeType:"Document";if(x.sizeBytes>=0)summary+=" · "+humanBytes(x.sizeBytes);out.add(new ObjectItem(id,x.id,x.displayName,summary,"Connected "+new SimpleDateFormat("d MMM yyyy",Locale.getDefault()).format(new Date(x.addedAt)),0));}}
        else if("projects".equals(id)){for(ProjectRepository.ProjectObject x:ProjectRepository.list(c,limit)){out.add(new ObjectItem(id,x.id,x.name,x.description,x.status.isEmpty()?"active":x.status,0));}}
        else if("places".equals(id)){for(PlaceRepository.PlaceObject x:PlaceRepository.list(c,limit)){String summary=String.format(Locale.US,"%.5f, %.5f",x.latitude,x.longitude);String meta=new SimpleDateFormat("d MMM yyyy · HH:mm",Locale.getDefault()).format(new Date(x.observedAt))+(x.accuracy>0?" · ±"+Math.round(x.accuracy)+" m":"");out.add(new ObjectItem(id,x.id,x.label,summary,meta,0));}}
        else if("people".equals(id)&&ContactPersonRepository.availability(c)==ContactPersonRepository.Availability.OPERATIONAL){for(ContactPersonRepository.PersonObject x:ContactPersonRepository.list(c,limit)){String summary=!x.phones.isEmpty()?x.phones.get(0):!x.emails.isEmpty()?x.emails.get(0):"Saved contact";out.add(new ObjectItem(id,x.id,x.name,summary,"Android Contacts",0));}}
        else if("events".equals(id)&&CalendarEventRepository.availability(c)==CalendarEventRepository.Availability.OPERATIONAL){for(CalendarEventRepository.CalendarEventObject x:CalendarEventRepository.recentAndUpcoming(c,limit)){String summary=!x.location.isEmpty()?x.location:x.description;String meta=new SimpleDateFormat("EEE, d MMM · HH:mm",Locale.getDefault()).format(new Date(x.begin));out.add(new ObjectItem(id,x.id,x.title.isEmpty()?"Calendar event":x.title,summary,meta,0));}}
        return out;
    }
    static ObjectItem load(Context c,String capabilityId,String objectId){for(ObjectItem x:list(c,capabilityId,3000))if(x.objectId.equals(s(objectId)))return x;return null;}
    static int pendingActionCount(Context c){return new PersistentActionQueue(c).pending().size();}

    private static String humanAction(String a){String x=s(a).replace('_',' ').toLowerCase(Locale.ROOT);if(x.isEmpty()||"none".equals(x))return "Needs attention";return Character.toUpperCase(x.charAt(0))+x.substring(1);}
    private static String humanState(String x){String s=s(x).replace('_',' ');return s.isEmpty()?"not available":s;}
    private static String duration(long ms){long sec=Math.max(0,ms/1000);return String.format(Locale.US,"%02d:%02d",sec/60,sec%60);}
    private static String clip(String x,int n){String v=s(x).replace('\n',' ');return v.length()<=n?v:v.substring(0,n)+"…";}
    private static String humanBytes(long n){if(n<1024)return n+" B";if(n<1024L*1024)return String.format(Locale.US,"%.1f KB",n/1024.0);if(n<1024L*1024*1024)return String.format(Locale.US,"%.1f MB",n/(1024.0*1024));return String.format(Locale.US,"%.1f GB",n/(1024.0*1024*1024));}
    private static String singular(String id){if("conversations".equals(id))return "conversation";if("commitments".equals(id))return "commitment";if("decisions".equals(id))return "decision";if("voice".equals(id))return "recording";if("files".equals(id))return "file";if("projects".equals(id))return "project";if("places".equals(id))return "place";if("people".equals(id))return "person";if("events".equals(id))return "event";return "item";}
    private static String plural(String id){if("conversations".equals(id))return "conversations";if("commitments".equals(id))return "commitments";if("decisions".equals(id))return "decisions";if("voice".equals(id))return "recordings";if("files".equals(id))return "files";if("projects".equals(id))return "projects";if("places".equals(id))return "places";if("people".equals(id))return "people";if("events".equals(id))return "events";return "items";}
    private static String s(String x){return x==null?"":x.trim();}
}
