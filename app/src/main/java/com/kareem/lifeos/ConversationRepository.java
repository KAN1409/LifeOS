package com.kareem.lifeos;

import android.content.Context;
import android.database.Cursor;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Stable, typed view over persisted conversation streams. */
final class ConversationRepository {
    static final class ConversationObject {
        final String id,app,threadKey,label,preview;
        final long latestEventId,latestAt;
        final int capturedCount;
        ConversationObject(String id,String app,String threadKey,String label,String preview,long latestEventId,long latestAt,int capturedCount){
            this.id=id;this.app=app;this.threadKey=threadKey;this.label=label;this.preview=preview;
            this.latestEventId=latestEventId;this.latestAt=latestAt;this.capturedCount=capturedCount;
        }
    }
    private static final class Mutable {
        final String id,app,threadKey,label;final long latestEventId,latestAt;final String preview;
        final Map<String,Long> exactSeen=new HashMap<>();int count;
        Mutable(String id,String app,String threadKey,String label,long latestEventId,long latestAt,String preview){this.id=id;this.app=app;this.threadKey=threadKey;this.label=label;this.latestEventId=latestEventId;this.latestAt=latestAt;this.preview=preview;}
        ConversationObject build(){return new ConversationObject(id,app,threadKey,label,UserFacingText.humanize(preview),latestEventId,latestAt,count);}
    }

    private ConversationRepository(){}

    static List<ConversationObject> list(Context context,int limit){return scan(context,Math.max(1,limit),null);}

    /** Count is a linear indexed DB scan, not a reconstructive recentEvents O(N²) pass. */
    static int count(Context context){return scan(context,Integer.MAX_VALUE,null).size();}

    static ConversationObject load(Context context,String objectId){
        String id=safe(objectId);if(id.isEmpty())return null;List<ConversationObject> xs=scan(context,1,id);return xs.isEmpty()?null:xs.get(0);
    }

    static List<LifeDb.Event> evidence(Context context,String objectId,int limit){
        ConversationObject c=load(context,objectId);if(c==null)return new ArrayList<>();
        try(LifeDb db=new LifeDb(context)){return db.eventsForThread(c.app,c.threadKey,c.label,Math.max(1,limit));}
    }

    static String idFor(String app,String threadKey,String label){
        String raw=safe(app)+"\n"+safe(threadKey)+"\n"+safe(label).toLowerCase(Locale.ROOT);
        return "conversation:"+sha(raw).substring(0,24);
    }

    /**
     * Reads the events index once in newest-first order. The previous path called LifeDb.recentEvents with a
     * history-sized limit, whose pairwise duplicate loop made capability counts quadratic on real 4k+ histories.
     */
    private static List<ConversationObject> scan(Context context,int maxGroups,String targetId){
        LinkedHashMap<String,Mutable> groups=new LinkedHashMap<>();String wanted=safe(targetId);
        try(LifeDb db=new LifeDb(context);Cursor c=db.getReadableDatabase().rawQuery("SELECT id,captured_at,app,title,body,thread_key FROM events ORDER BY captured_at DESC",null)){
            while(c.moveToNext()){
                LifeDb.Event e=new LifeDb.Event(c.getLong(0),c.getLong(1),safe(c.getString(2)),safe(c.getString(3)),safe(c.getString(4)),safe(c.getString(5)));
                if(CapturePolicy.isNotificationSummary(e.body)||CapturePolicy.isLauncherSnapshot(e.body)||CapturePolicy.isMessagingHomeSnapshot(e.body))continue;
                if(!LifeDb.isConversationLike(e))continue;
                String thread=safe(e.threadKey),label=LifeDb.personLabel(e);if(generic(label,e.app)&&generic(LifeDb.threadLabel(thread),e.app))continue;
                String id=idFor(e.app,thread,label);if(!wanted.isEmpty()&&!wanted.equals(id))continue;
                String group=e.app+"|"+(!thread.isEmpty()?thread:label);Mutable m=groups.get(group);
                if(m==null){if(wanted.isEmpty()&&groups.size()>=maxGroups)continue;m=new Mutable(id,e.app,thread,label,e.id,e.at,e.body);groups.put(group,m);}
                String signature=e.title+'\u001f'+e.body;Long newer=m.exactSeen.get(signature);if(newer!=null&&Math.abs(newer-e.at)<=10000L)continue;m.exactSeen.put(signature,e.at);m.count++;
            }
        }
        ArrayList<ConversationObject> out=new ArrayList<>();for(Mutable m:groups.values()){out.add(m.build());if(out.size()>=maxGroups)break;}return out;
    }

    private static boolean generic(String value,String app){String x=safe(value).toLowerCase(Locale.ROOT);if(x.isEmpty()||x.equals("visible conversation")||x.contains("new message")||x.equals("message")||x.equals("messages"))return true;return x.equals(LifeDb.friendlyApp(app).toLowerCase(Locale.ROOT));}
    private static String sha(String value){try{byte[] b=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte x:b)s.append(String.format(Locale.US,"%02x",x));return s.toString();}catch(Exception e){return Integer.toHexString(value.hashCode())+"000000000000000000000000";}}
    private static String safe(String x){return x==null?"":x.trim();}
}
