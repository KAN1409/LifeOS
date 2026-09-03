package com.kareem.lifeos;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import com.kareem.lifeos.engine.NotificationUnderstandingProbe;
import java.util.List;
import java.util.Locale;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class LifeNotificationListener extends NotificationListenerService {
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
            boolean stored=false;if(lines!=null&&lines.length>0){for(int i=0;i<lines.length;i++){String line=text(lines[i]);if(line.isEmpty())continue;stored|=store(db,base+"|line|"+i+"|"+sha(line),app,title,line,thread,sbn.getPostTime());}}
            if(!stored)store(db,base+"|body|"+sha(body),app,title,body,thread,sbn.getPostTime());
        }
    }
    private boolean store(LifeDb db,String key,String app,String title,String body,String thread,long at){
        if(isSensitive(title+" "+body)||CapturePolicy.isNotificationSummary(body))return false;
        NotificationUnderstandingProbe.observe(this,thread,body,at);
        long id=db.upsertEvent(key,app,title,body,thread,at);
        if(id>0){List<OpenLoopExtractor.Candidate> loops=OpenLoopExtractor.extract(title,body,System.currentTimeMillis());for(OpenLoopExtractor.Candidate x:loops)db.upsertLoop(id,x);return true;}return false;
    }
    private static boolean isSensitive(String s){String x=s.toLowerCase(Locale.ROOT);return x.contains("otp")||x.contains("one-time password")||x.contains("verification code")||x.contains("رمز التحقق")||x.contains("كود التحقق");}
    private static String text(CharSequence x){return x==null?"":x.toString().trim();}
    private static String sha(String value){try{byte[] b=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder x=new StringBuilder();for(byte q:b)x.append(String.format(Locale.US,"%02x",q));return x.toString();}catch(Exception e){return Integer.toHexString(value.hashCode());}}
}
