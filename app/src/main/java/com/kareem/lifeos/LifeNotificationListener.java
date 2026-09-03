package com.kareem.lifeos;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import com.kareem.lifeos.context.NotificationCapture;
import com.kareem.lifeos.context.NotificationObservationAdapter;
import com.kareem.lifeos.context.RawObservation;
import com.kareem.lifeos.context.UniversalObservationStore;
import com.kareem.lifeos.engine.NotificationUnderstandingProbe;
import java.util.List;
import java.util.Locale;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;

public final class LifeNotificationListener extends NotificationListenerService {
    private static final NotificationObservationAdapter V2_ADAPTER=new NotificationObservationAdapter();

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        if(sbn==null||sbn.isOngoing()||sbn.getNotification()==null)return;
        Notification n=sbn.getNotification();Bundle e=n.extras;
        String title=text(e.getCharSequence(Notification.EXTRA_TITLE));
        String body=text(e.getCharSequence(Notification.EXTRA_BIG_TEXT));if(body.isEmpty())body=text(e.getCharSequence(Notification.EXTRA_TEXT));if(title.isEmpty()&&body.isEmpty())return;
        String app=sbn.getPackageName()==null?"unknown":sbn.getPackageName();
        String conversation=text(e.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE));
        String thread=app+"|"+(conversation.isEmpty()?title:conversation).toLowerCase(Locale.ROOT).trim();
        String base=sbn.getKey()==null?app+"|"+sbn.getId()+"|"+sbn.getPostTime():sbn.getKey();CharSequence[] lines=e.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        try(LifeDb db=new LifeDb(this)){
            boolean hadLine=false;Set<String> seen=new HashSet<String>();if(lines!=null&&lines.length>0){for(int i=0;i<lines.length;i++){String line=text(lines[i]);String normalized=line.toLowerCase(Locale.ROOT).replaceAll("\\s+"," ");if(line.isEmpty()||!seen.add(normalized))continue;hadLine=true;store(db,base+"|line|"+i+"|"+sha(line),app,title,conversation,line,thread,sbn.getPostTime());}}
            if(!hadLine)store(db,base+"|body|"+sha(body),app,title,conversation,body,thread,sbn.getPostTime());
        }
    }

    private boolean store(LifeDb db,String key,String app,String title,String conversation,String body,String thread,long at){
        if(isSensitive(title+" "+body)||CapturePolicy.isNotificationSummary(body))return false;

        // V2 shadow path: preserve source facts before semantic interpretation.
        RawObservation raw=V2_ADAPTER.adapt(new NotificationCapture(key,app,title,conversation,body,at));
        UniversalObservationStore.get(this).append(raw);

        // Existing M1 path remains intact until V2 proves equivalent/better.
        NotificationUnderstandingProbe.observe(this,thread,body,at,key);
        long id=db.upsertEvent(key,app,title,body,thread,at);
        if(id>0){List<OpenLoopExtractor.Candidate> loops=OpenLoopExtractor.extract(title,body,System.currentTimeMillis());for(OpenLoopExtractor.Candidate x:loops)db.upsertLoop(id,x);return true;}return false;
    }
    private static boolean isSensitive(String s){String x=s.toLowerCase(Locale.ROOT);return x.contains("otp")||x.contains("one-time password")||x.contains("verification code")||x.contains("رمز التحقق")||x.contains("كود التحقق");}
    private static String text(CharSequence x){return x==null?"":x.toString().trim();}
    private static String sha(String value){try{byte[] b=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder x=new StringBuilder();for(byte q:b)x.append(String.format(Locale.US,"%02x",q));return x.toString();}catch(Exception e){return Integer.toHexString(value.hashCode());}}
}
