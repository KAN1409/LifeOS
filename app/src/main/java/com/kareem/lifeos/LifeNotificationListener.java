package com.kareem.lifeos;

import android.app.Notification;
import android.content.ComponentName;
import android.os.Bundle;
import android.os.Parcelable;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import com.kareem.lifeos.context.NotificationCapture;
import com.kareem.lifeos.context.NotificationObservationAdapter;
import com.kareem.lifeos.context.RawObservation;
import com.kareem.lifeos.context.UniversalObservationStore;
import com.kareem.lifeos.engine.NotificationUnderstandingProbe;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Loss-minimizing notification ingress for LifeOS.
 *
 * Capture is intentionally broad; semantic promotion is intentionally narrow. MessagingStyle
 * notifications are decomposed into individual message facts before any interpretation happens.
 */
public final class LifeNotificationListener extends NotificationListenerService {
    private static final NotificationObservationAdapter V2_ADAPTER=new NotificationObservationAdapter();

    @Override public void onNotificationPosted(StatusBarNotification sbn) { processNotification(sbn); }

    /** Recover active evidence that may have been posted while Android had the listener detached. */
    @Override public void onListenerConnected(){
        super.onListenerConnected();
        try{
            StatusBarNotification[] active=getActiveNotifications();
            if(active!=null)for(StatusBarNotification sbn:active)processNotification(sbn);
        }catch(Throwable ignored){}
    }

    /** Ask Android to restore the listener binding after a transient process/service disconnect. */
    @Override public void onListenerDisconnected(){
        super.onListenerDisconnected();
        try{requestRebind(new ComponentName(this,LifeNotificationListener.class));}catch(Throwable ignored){}
    }

    private void processNotification(StatusBarNotification sbn) {
        if(sbn==null||sbn.getNotification()==null)return;
        String app=sbn.getPackageName()==null?"unknown":sbn.getPackageName();
        // Never feed LifeOS/Teya foreground-service notifications back into LifeOS itself.
        if(getPackageName().equals(app))return;

        Notification n=sbn.getNotification();
        Bundle e=n.extras==null?Bundle.EMPTY:n.extras;
        String title=text(e.getCharSequence(Notification.EXTRA_TITLE));
        String body=text(e.getCharSequence(Notification.EXTRA_BIG_TEXT));
        if(body.isEmpty())body=text(e.getCharSequence(Notification.EXTRA_TEXT));
        String conversation=text(e.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE));
        boolean group=e.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION,false);
        String category=n.category==null?"":n.category;
        String channel=n.getChannelId()==null?"":n.getChannelId();
        boolean ongoing=sbn.isOngoing();
        String base=sbn.getKey()==null?app+"|"+sbn.getId()+"|"+sbn.getPostTime():sbn.getKey();

        try(LifeDb db=new LifeDb(this)){
            List<MessagePart> messages=structuredMessages(e,sbn.getPostTime());
            Set<String> seen=new HashSet<String>();
            boolean storedMessage=false;
            for(MessagePart m:messages){
                String normalized=(m.sender+"\n"+m.text).toLowerCase(Locale.ROOT).replaceAll("\\s+"," ").trim();
                if(m.text.isEmpty()||!seen.add(normalized))continue;
                String messageKey=base+"|msg|"+m.at+"|"+sha(m.sender+"\n"+m.text);
                store(db,messageKey,app,title,conversation,m.sender,m.text,category,channel,group,ongoing,m.at);
                storedMessage=true;
            }

            // Many apps do not expose MessagingStyle. Preserve their multi-line notification
            // payload as independent facts rather than one UI-formatted blob.
            if(!storedMessage){
                CharSequence[] lines=e.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
                boolean hadLine=false;
                if(lines!=null&&lines.length>0){
                    for(int i=0;i<lines.length;i++){
                        String line=text(lines[i]);
                        String normalized=line.toLowerCase(Locale.ROOT).replaceAll("\\s+"," ").trim();
                        if(line.isEmpty()||!seen.add(normalized))continue;
                        hadLine=true;
                        store(db,base+"|line|"+i+"|"+sha(line),app,title,conversation,"",line,category,channel,group,ongoing,sbn.getPostTime());
                    }
                }
                if(!hadLine){
                    store(db,base+"|body|"+sha(body),app,title,conversation,"",body,category,channel,group,ongoing,sbn.getPostTime());
                }
            }
        }catch(Throwable ignored){
            // A malformed third-party notification must never break capture for subsequent apps.
        }
    }

    private boolean store(LifeDb db,String key,String app,String originalTitle,String conversation,
                          String sender,String body,String category,String channel,
                          boolean group,boolean ongoing,long at){
        String structuralLabel=!conversation.trim().isEmpty()?conversation.trim():!sender.trim().isEmpty()?sender.trim():originalTitle.trim();
        String thread=app+"|"+structuralLabel.toLowerCase(Locale.ROOT).trim();
        String participant=participant(originalTitle,conversation,sender,body);
        if(!participant.isEmpty()&&conversation.trim().isEmpty())thread=app+"|"+participant.toLowerCase(Locale.ROOT).trim();

        boolean sensitive=isSensitive(originalTitle+" "+body);
        String capturedText=sensitive?"[sensitive notification content redacted]":body;
        NotificationCapture capture=new NotificationCapture(key,app,originalTitle,conversation,sender,capturedText,
                category,channel,group,ongoing,at);
        RawObservation raw=V2_ADAPTER.adapt(capture);
        UniversalObservationStore.get(this).append(raw);

        // Raw capture is broader than understanding. Summaries and secrets remain evidence but
        // are never promoted into conversation memory, open loops, or semantic work.
        if(sensitive||CapturePolicy.isNotificationSummary(body))return false;

        // Canonical understanding is immediate and independent from deep semantic inference.
        NotificationUnderstandingProbe.observe(this,thread,body,at,key);
        String eventTitle=structuralLabel.isEmpty()?LifeDb.friendlyApp(app):structuralLabel;
        long id=db.upsertEvent(key,app,eventTitle,body,thread,at);
        if(id>0){
            LifeDb.Event event=db.eventById(id);
            FastAttentionGate.Result fast=FastAttentionGate.evaluate(event,raw,System.currentTimeMillis());
            AttentionStore attention=AttentionStore.get(this);
            // Every eligible notification is durably queued immediately. Opening/dismissing the
            // Android notification cannot remove this work item.
            attention.enqueue(raw.observationId,raw.streamId,id,raw.observedAt,fast.queuePriority);
            if(fast.provisional){
                attention.provisional(raw.observationId,raw.streamId,id,raw.observedAt,fast.type,fast.intent,
                        fast.urgency,fast.action,fast.summary,fast.reason,fast.confidence,fast.attentionPriority);
            }

            // Keep the legacy extractor as a compatibility/fallback surface while the new durable
            // attention ledger becomes authoritative.
            List<OpenLoopExtractor.Candidate> loops=OpenLoopExtractor.extract(eventTitle,body,System.currentTimeMillis());
            for(OpenLoopExtractor.Candidate x:loops)db.upsertLoop(id,x);
            LocalGroundedMemory.materialize(this,event);

            // AICore permits deep inference only while LifeOS is foreground. If it already is,
            // consume the queue immediately; otherwise FeedActivity will drain it on next resume.
            if(LifeOsApp.isAppForeground())NotificationBrain.analyzeForeground(this,null);
            return true;
        }
        return false;
    }

    private static List<MessagePart> structuredMessages(Bundle extras,long fallbackAt){
        ArrayList<MessagePart> out=new ArrayList<MessagePart>();
        Parcelable[] parcels=extras.getParcelableArray(Notification.EXTRA_MESSAGES);
        if(parcels==null)return out;
        for(Parcelable parcel:parcels){
            if(!(parcel instanceof Bundle))continue;
            Bundle b=(Bundle)parcel;
            String message=text(b.getCharSequence("text"));
            long at=b.getLong("time",fallbackAt);
            String sender=text(b.getCharSequence("sender"));
            if(sender.isEmpty())sender=personName(b.getParcelable("sender_person"));
            if(!message.isEmpty())out.add(new MessagePart(sender,message,at>0?at:fallbackAt));
        }
        return out;
    }

    private static String personName(Object person){
        if(person==null)return "";
        try{
            Method m=person.getClass().getMethod("getName");
            Object value=m.invoke(person);
            return value==null?"":value.toString().trim();
        }catch(Exception ignored){return "";}
    }

    private static String participant(String title,String conversation,String sender,String body){
        if(conversation!=null&&!conversation.trim().isEmpty())return conversation.trim();
        if(sender!=null&&!sender.trim().isEmpty())return sender.trim();
        String t=title==null?"":title.trim(),low=t.toLowerCase(Locale.ROOT);
        if(!low.equals("whatsapp")&&!low.equals("messenger")&&!low.equals("telegram")&&!low.equals("signal")&&!low.contains("new message"))return "";
        int colon=body==null?-1:body.indexOf(':');
        if(colon>1&&colon<80)return body.substring(0,colon).trim();
        return "";
    }

    private static boolean isSensitive(String s){
        String x=s==null?"":s.toLowerCase(Locale.ROOT);
        return x.contains("otp")||x.contains("one-time password")||x.contains("verification code")||x.contains("رمز التحقق")||x.contains("كود التحقق");
    }
    private static String text(CharSequence x){return x==null?"":x.toString().trim();}
    private static String sha(String value){try{byte[] b=MessageDigest.getInstance("SHA-256").digest((value==null?"":value).getBytes(StandardCharsets.UTF_8));StringBuilder x=new StringBuilder();for(byte q:b)x.append(String.format(Locale.US,"%02x",q));return x.toString();}catch(Exception e){return Integer.toHexString(value==null?0:value.hashCode());}}

    private static final class MessagePart{
        final String sender,text;final long at;
        MessagePart(String sender,String text,long at){this.sender=sender==null?"":sender;this.text=text==null?"":text;this.at=at;}
    }
}
