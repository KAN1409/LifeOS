package com.kareem.lifeos;

import android.content.Context;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/** Canonical product timeline: meaningful interpreted capture + real calendar events. */
final class CanonicalTimelineRepository {
    static final class TimelineObject {
        final String id,kind,title,summary,sourcePackage,sourceLabel;
        final long at,end,eventId,calendarEventId;
        final boolean calendar;
        TimelineObject(String id,String kind,String title,String summary,String sourcePackage,String sourceLabel,long at,long end,long eventId,long calendarEventId,boolean calendar){
            this.id=s(id);this.kind=s(kind);this.title=s(title);this.summary=s(summary);this.sourcePackage=s(sourcePackage);this.sourceLabel=s(sourceLabel);this.at=at;this.end=end;this.eventId=eventId;this.calendarEventId=calendarEventId;this.calendar=calendar;
        }
    }
    private CanonicalTimelineRepository(){}

    static List<TimelineObject> recent(Context context,int limit){
        ArrayList<TimelineObject> all=new ArrayList<>();NotificationMeaningStore meanings=NotificationMeaningStore.get(context);
        try(LifeDb db=new LifeDb(context)){
            for(LifeDb.Event e:db.recentEvents(Math.max(600,limit*4))){
                NotificationMeaning m=meanings.forStreamAt(e.threadKey,e.at);
                if(!CanonicalSemanticPolicy.isCanonicalTimeline(e,m))continue;
                String kind=CanonicalSemanticPolicy.canonicalTimelineKind(e,m);
                String title=timelineTitle(context,e,m,kind);String summary=timelineSummary(context,e,m);
                if(summary.equalsIgnoreCase(title))summary="";
                all.add(new TimelineObject("timeline:event:"+e.id,kind,title,summary,e.app,LifeDb.friendlyApp(e.app),e.at,0,e.id,0,false));
            }
        }
        if(CalendarEventRepository.availability(context)==CalendarEventRepository.Availability.OPERATIONAL){
            for(CalendarEventRepository.CalendarEventObject e:CalendarEventRepository.recentAndUpcoming(context,250)){
                long now=System.currentTimeMillis();if(e.begin>now+7L*86400000L)continue;String summary=!e.location.isEmpty()?e.location:e.description;
                all.add(new TimelineObject("timeline:"+e.id,"Event",e.title.isEmpty()?"Calendar event":e.title,summary,"calendar",e.calendarName,e.begin,e.end,0,e.eventId,true));
            }
        }
        all.sort((a,b)->Long.compare(b.at,a.at));LinkedHashMap<String,TimelineObject> dedupe=new LinkedHashMap<>();
        for(TimelineObject x:all){String key=(x.kind+"|"+x.title+"|"+x.summary).toLowerCase(Locale.ROOT).replaceAll("\\s+"," ").trim();TimelineObject seen=dedupe.get(key);if(seen!=null&&Math.abs(seen.at-x.at)<120000L)continue;dedupe.put(key,x);if(dedupe.size()>=Math.max(1,limit))break;}
        return new ArrayList<>(dedupe.values());
    }

    static TimelineObject load(Context c,String id){for(TimelineObject x:recent(c,1000))if(x.id.equals(s(id)))return x;return null;}

    private static String timelineTitle(Context c,LifeDb.Event e,NotificationMeaning m,String kind){
        if("Message".equals(kind))return LifeDb.personLabel(e);
        String x=PresentationSemantics.title(c,e);return x==null||x.trim().isEmpty()?LifeDb.friendlyApp(e.app):x;
    }
    private static String timelineSummary(Context c,LifeDb.Event e,NotificationMeaning m){if(m!=null&&m.canSummarize()&&!m.summary.trim().isEmpty())return UserFacingText.humanize(m.summary);return PresentationSemantics.summary(c,e);}
    private static String s(String x){return x==null?"":x.trim();}
}
