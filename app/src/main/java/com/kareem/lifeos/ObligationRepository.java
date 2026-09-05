package com.kareem.lifeos;

import android.content.Context;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Canonical source of truth for user-facing unresolved obligations.
 * It accepts only confirmed semantic attention, retracts unsafe historical promotions,
 * and collapses repeated evidence from one human stream into one obligation object.
 */
final class ObligationRepository {
    static final class ObligationObject {
        final String id,streamId,title,summary,action,reason,intent,type;
        final int priority,evidenceCount;
        final long latestEventId,latestAt;
        final List<Long> evidenceEventIds;
        ObligationObject(String id,String streamId,String title,String summary,String action,String reason,String intent,String type,int priority,int evidenceCount,long latestEventId,long latestAt,List<Long> evidenceEventIds){
            this.id=id;this.streamId=s(streamId);this.title=s(title);this.summary=s(summary);this.action=s(action);this.reason=s(reason);this.intent=s(intent);this.type=s(type);this.priority=priority;this.evidenceCount=evidenceCount;this.latestEventId=latestEventId;this.latestAt=latestAt;this.evidenceEventIds=evidenceEventIds;
        }
    }
    private static final class Mutable {
        String key,stream,title,summary,action,reason,intent,type;int priority;long eventId,at;final ArrayList<Long> evidence=new ArrayList<>();
    }
    private ObligationRepository(){}

    static List<ObligationObject> open(Context context,int limit){
        AttentionStore store=AttentionStore.get(context);LinkedHashMap<String,Mutable> grouped=new LinkedHashMap<>();
        try(LifeDb db=new LifeDb(context)){
            for(AttentionStore.Item item:store.openItems(500)){
                LifeDb.Event event=db.eventById(item.eventId);
                if(!CanonicalSemanticPolicy.isCanonicalAttention(item,event)){
                    store.retract(item.eventId,"Retracted by canonical semantic truth gate");
                    continue;
                }
                EventSemantics.Assessment a=EventSemantics.classify(event);
                String key=a.personConversation?"stream:"+s(item.streamId):"event:"+item.eventId;
                Mutable m=grouped.get(key);if(m==null){m=new Mutable();m.key=key;m.stream=item.streamId;grouped.put(key,m);}
                if(!m.evidence.contains(item.eventId))m.evidence.add(item.eventId);
                boolean take=m.eventId<=0||item.priority>m.priority||(item.priority==m.priority&&item.sourceAt>m.at);
                if(take){m.priority=item.priority;m.eventId=item.eventId;m.at=item.sourceAt;m.intent=item.intent;m.type=item.type;m.action=item.action;m.reason=UserFacingText.humanize(item.reason);m.title=PresentationSemantics.title(context,event);String grounded=UserFacingText.humanize(item.summary);m.summary=grounded.isEmpty()?PresentationSemantics.summary(context,event):grounded;}
            }
        }
        ArrayList<ObligationObject> out=new ArrayList<>();for(Mutable m:grouped.values())out.add(new ObligationObject(idFor(m.key),m.stream,m.title,m.summary,m.action,m.reason,m.intent,m.type,m.priority,m.evidence.size(),m.eventId,m.at,new ArrayList<>(m.evidence)));
        out.sort((a,b)->{int p=Integer.compare(b.priority,a.priority);return p!=0?p:Long.compare(b.latestAt,a.latestAt);});
        if(out.size()>Math.max(1,limit))return new ArrayList<>(out.subList(0,Math.max(1,limit)));return out;
    }

    static int count(Context c){return open(c,1000).size();}
    static ObligationObject load(Context c,String id){for(ObligationObject o:open(c,1000))if(o.id.equals(s(id)))return o;return null;}
    static void markHandled(Context c,String id){ObligationObject o=load(c,id);if(o==null)return;AttentionStore store=AttentionStore.get(c);for(Long eventId:o.evidenceEventIds)if(eventId!=null&&eventId>0)store.markHandled(eventId);}

    static String idFor(String key){return "obligation:"+sha(s(key)).substring(0,24);}
    private static String sha(String value){try{byte[] b=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder x=new StringBuilder();for(byte q:b)x.append(String.format(Locale.US,"%02x",q));return x.toString();}catch(Exception e){return Integer.toHexString(value.hashCode())+"000000000000000000000000";}}
    private static String s(String x){return x==null?"":x.trim();}
}
