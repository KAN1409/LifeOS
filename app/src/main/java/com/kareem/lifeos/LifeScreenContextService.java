package com.kareem.lifeos;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.os.Handler;
import android.os.Looper;
import com.kareem.lifeos.engine.AccessibilityTreeCapture;
import com.kareem.lifeos.engine.ParallelUnderstandingProbe;
import com.kareem.lifeos.engine.RawScreenSnapshot;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashSet;
import java.util.Locale;

public final class LifeScreenContextService extends AccessibilityService {
    private static final String[] SUPPORTED={"com.whatsapp","com.whatsapp.w4b","org.telegram.messenger","org.thoughtcrime.securesms","com.facebook.orca"};
    private static final class Snapshot{final String pkg,conversation,text,thread;final LinkedHashSet<String> pieces;Snapshot(String pkg,String conversation,String text,String thread,LinkedHashSet<String> pieces){this.pkg=pkg;this.conversation=conversation;this.text=text;this.thread=thread;this.pieces=pieces;}}
    private final Handler handler=new Handler(Looper.getMainLooper());private Snapshot pending;
    private final Runnable pendingCapture=this::commitPending;

    @Override public void onAccessibilityEvent(AccessibilityEvent event){
        if(event==null||event.getPackageName()==null)return;String pkg=event.getPackageName().toString();if(!supported(pkg))return;Snapshot next=prepare(pkg);if(next==null)return;pending=next;handler.removeCallbacks(pendingCapture);handler.postDelayed(pendingCapture,1200);
    }
    @Override public void onInterrupt(){}
    @Override public void onDestroy(){handler.removeCallbacks(pendingCapture);super.onDestroy();}
    private Snapshot prepare(String pkg){AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null)return null;CharSequence actual=root.getPackageName();if(actual==null||!pkg.equals(actual.toString())){root.recycle();return null;}
        try{
            int width=getResources().getDisplayMetrics().widthPixels;
            int height=getResources().getDisplayMetrics().heightPixels;
            RawScreenSnapshot raw=AccessibilityTreeCapture.capture(root,pkg,System.currentTimeMillis(),width,height);
            ParallelUnderstandingProbe.observe(this,raw);
        }catch(Throwable ignored){}
        LinkedHashSet<String> pieces=new LinkedHashSet<>();collect(root,pieces,0);root.recycle();if(pieces.size()<2||!isConversation(pieces))return null;String conversation=conversationTitle(pieces);if(conversation.isEmpty())return null;StringBuilder body=new StringBuilder();for(String x:pieces){if(skipChrome(x))continue;if(body.length()>0)body.append(" · ");body.append(x);}String text=body.toString();if(text.length()<2||CapturePolicy.isMessagingHomeSnapshot(text))return null;if(text.length()>12000)text=text.substring(0,12000);String thread=pkg+"|"+conversation.toLowerCase(Locale.ROOT);return new Snapshot(pkg,conversation,text,thread,pieces);}
    private void commitPending(){Snapshot s=pending;pending=null;if(s==null)return;long now=System.currentTimeMillis();try(LifeDb db=new LifeDb(this)){long id=db.upsertScreenEvent("screen|"+sha(s.thread),s.pkg,"Visible conversation",s.text,s.thread,now);if(id>0)for(String piece:s.pieces)if(!skipChrome(piece))for(OpenLoopExtractor.Candidate x:OpenLoopExtractor.extract("",piece,now))db.upsertLoop(id,x);}}
    private static void collect(AccessibilityNodeInfo n,LinkedHashSet<String> out,int depth){if(n==null||depth>35||out.size()>250)return;CharSequence t=n.getText();if(t!=null){String x=t.toString().trim();if(!x.isEmpty()&&x.length()<=1000)out.add(x);}for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo child=n.getChild(i);if(child!=null){collect(child,out,depth+1);child.recycle();}}}
    private static boolean isConversation(LinkedHashSet<String> xs){for(String x:xs)if("Message".equalsIgnoreCase(x)||"رسالة".equals(x))return true;return false;}
    private static String conversationTitle(LinkedHashSet<String> xs){for(String x:xs){String q=x.trim();if(q.matches("\\+?[0-9][0-9 ()-]{7,}")||(!skipChrome(q)&&q.length()>2&&!q.matches(".*\\d{1,2}:\\d{2}.*")))return q;}return "";}
    private static boolean skipChrome(String value){String x=value.trim(),l=x.toLowerCase(Locale.ROOT);return x.isEmpty()||CapturePolicy.isUnreadMarker(x)||l.equals("message")||l.equals("whatsapp")||l.equals("today")||l.equals("monday")||l.equals("tuesday")||l.equals("wednesday")||l.equals("thursday")||l.equals("friday")||l.equals("saturday")||l.equals("sunday")||l.startsWith("ask meta ai")||l.contains("end-to-end encrypted")||l.equals("video call")||l.equals("voice call")||x.equals("رسالة")||x.matches("\\d{1,2}:\\d{2}\\s*(AM|PM|am|pm)?");}
    private static boolean supported(String pkg){for(String x:SUPPORTED)if(x.equals(pkg))return true;return false;}
    private static String sha(String value){try{byte[] b=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder x=new StringBuilder();for(byte q:b)x.append(String.format(Locale.US,"%02x",q));return x.toString();}catch(Exception e){return Integer.toHexString(value.hashCode());}}
}
