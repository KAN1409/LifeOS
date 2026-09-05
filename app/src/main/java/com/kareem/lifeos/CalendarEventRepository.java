package com.kareem.lifeos;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.CalendarContract;
import java.util.ArrayList;
import java.util.List;

/** Real calendar capability backed by Android CalendarContract, never text heuristics. */
final class CalendarEventRepository {
    enum Availability { OPERATIONAL, SETUP_REQUIRED }
    static final class CalendarEventObject {
        final String id,title,description,location,calendarName;
        final long eventId,begin,end;
        final boolean allDay;
        CalendarEventObject(String id,long eventId,String title,String description,String location,String calendarName,long begin,long end,boolean allDay){
            this.id=id;this.eventId=eventId;this.title=s(title);this.description=s(description);this.location=s(location);this.calendarName=s(calendarName);this.begin=begin;this.end=end;this.allDay=allDay;
        }
    }
    private CalendarEventRepository(){}

    static Availability availability(Context c){return c.checkSelfPermission(Manifest.permission.READ_CALENDAR)==PackageManager.PERMISSION_GRANTED?Availability.OPERATIONAL:Availability.SETUP_REQUIRED;}

    static List<CalendarEventObject> upcoming(Context c,int limit,long horizonMs){
        long now=System.currentTimeMillis();return between(c,now,now+Math.max(86400000L,horizonMs),limit);
    }

    static List<CalendarEventObject> recentAndUpcoming(Context c,int limit){
        long now=System.currentTimeMillis();return between(c,now-14L*86400000L,now+45L*86400000L,limit);
    }

    static int upcomingCount(Context c,long horizonMs){return upcoming(c,500,horizonMs).size();}

    static CalendarEventObject load(Context c,String objectId){
        if(objectId==null||objectId.trim().isEmpty())return null;
        for(CalendarEventObject e:recentAndUpcoming(c,1000))if(e.id.equals(objectId))return e;
        return null;
    }

    private static List<CalendarEventObject> between(Context c,long begin,long end,int limit){
        ArrayList<CalendarEventObject> out=new ArrayList<>();if(availability(c)!=Availability.OPERATIONAL)return out;
        String[] projection={CalendarContract.Instances.EVENT_ID,CalendarContract.Instances.TITLE,CalendarContract.Instances.DESCRIPTION,CalendarContract.Instances.EVENT_LOCATION,CalendarContract.Instances.CALENDAR_DISPLAY_NAME,CalendarContract.Instances.BEGIN,CalendarContract.Instances.END,CalendarContract.Instances.ALL_DAY};
        Cursor cur=null;try{ContentResolver r=c.getContentResolver();cur=CalendarContract.Instances.query(r,projection,begin,end);while(cur!=null&&cur.moveToNext()&&out.size()<Math.max(1,limit)){long eventId=cur.getLong(0),b=cur.getLong(5),e=cur.getLong(6);String id="event:calendar:"+eventId+":"+b;out.add(new CalendarEventObject(id,eventId,cur.getString(1),cur.getString(2),cur.getString(3),cur.getString(4),b,e,cur.getInt(7)!=0));}}catch(SecurityException ignored){}finally{if(cur!=null)cur.close();}return out;
    }
    private static String s(String x){return x==null?"":x.trim();}
}
