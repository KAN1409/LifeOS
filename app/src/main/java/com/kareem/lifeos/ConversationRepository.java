package com.kareem.lifeos;

import android.content.Context;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    private ConversationRepository(){}

    static List<ConversationObject> list(Context context,int limit){
        ArrayList<ConversationObject> out=new ArrayList<>();
        try(LifeDb db=new LifeDb(context)){
            for(LifeDb.Conversation c:db.recentConversations(Math.max(1,limit))){
                out.add(from(c));if(out.size()>=limit)break;
            }
        }
        return out;
    }

    static int count(Context context){return list(context,1000).size();}

    static ConversationObject load(Context context,String objectId){
        String id=safe(objectId);if(id.isEmpty())return null;
        for(ConversationObject c:list(context,1200))if(c.id.equals(id))return c;
        return null;
    }

    static List<LifeDb.Event> evidence(Context context,String objectId,int limit){
        ConversationObject c=load(context,objectId);if(c==null)return new ArrayList<>();
        try(LifeDb db=new LifeDb(context)){return db.eventsForThread(c.app,c.threadKey,c.label,Math.max(1,limit));}
    }

    static String idFor(String app,String threadKey,String label){
        String raw=safe(app)+"\n"+safe(threadKey)+"\n"+safe(label).toLowerCase(Locale.ROOT);
        return "conversation:"+sha(raw).substring(0,24);
    }

    private static ConversationObject from(LifeDb.Conversation c){
        return new ConversationObject(idFor(c.app,c.threadKey,c.label),c.app,c.threadKey,c.label,
                UserFacingText.humanize(c.preview),c.latestEventId,c.latestAt,c.count);
    }
    private static String sha(String value){try{byte[] b=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte x:b)s.append(String.format(Locale.US,"%02x",x));return s.toString();}catch(Exception e){return Integer.toHexString(value.hashCode())+"000000000000000000000000";}}
    private static String safe(String x){return x==null?"":x.trim();}
}
