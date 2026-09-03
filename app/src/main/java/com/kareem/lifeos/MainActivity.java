package com.kareem.lifeos;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.kareem.lifeos.engine.CanonicalEventRecord;
import com.kareem.lifeos.engine.PersistentUnderstandingStore;
import com.kareem.lifeos.engine.UnderstandingEngineVersion;
import com.kareem.lifeos.engine.UnderstandingReplayEngine;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public final class MainActivity extends Activity {
    private static final int BG=Color.rgb(8,10,12),CARD=Color.rgb(20,23,25),TEXT=Color.rgb(244,247,248),MUTED=Color.rgb(158,166,170),ACCENT=Color.rgb(184,226,74);
    private LifeDb db;private LinearLayout content;private TextView status;

    @Override public void onCreate(Bundle state){super.onCreate(state);db=new LifeDb(this);render();startReplayIfNeeded();}
    @Override protected void onResume(){super.onResume();if(content!=null)refresh();}
    @Override protected void onDestroy(){if(db!=null)db.close();super.onDestroy();}

    private void startReplayIfNeeded(){
        new Thread(new Runnable(){@Override public void run(){try{final boolean rebuilt=UnderstandingReplayEngine.replayIfNeeded(MainActivity.this);if(rebuilt)runOnUiThread(new Runnable(){@Override public void run(){if(status!=null)refresh();}});}catch(Throwable ignored){}}},"lifeos-understanding-replay").start();
    }

    private void render(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);root.setPadding(dp(20),dp(18),dp(20),dp(18));
        TextView title=text("LifeOS",32,TEXT);title.setTypeface(null,1);root.addView(title);
        root.addView(text("Your phone context, understood locally",13,MUTED));
        status=text("",12,MUTED);status.setPadding(0,dp(16),0,dp(8));root.addView(status);

        Button access=button("MANAGE NOTIFICATION ACCESS");access.setOnClickListener(v->startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")));root.addView(access,lp(dp(8)));
        Button screen=button("MANAGE SCREEN CONTEXT ACCESS");screen.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));root.addView(screen,lp(dp(8)));
        LinearLayout tabs=new LinearLayout(this);tabs.setOrientation(LinearLayout.HORIZONTAL);
        Button recent=button("RECENT"),canonical=button("CANONICAL M1"),loops=button("OPEN LOOPS");
        tabs.addView(recent,new LinearLayout.LayoutParams(0,dp(48),1));LinearLayout.LayoutParams mid=new LinearLayout.LayoutParams(0,dp(48),1);mid.setMargins(dp(8),0,0,0);tabs.addView(canonical,mid);LinearLayout.LayoutParams right=new LinearLayout.LayoutParams(0,dp(48),1);right.setMargins(dp(8),0,0,0);tabs.addView(loops,right);root.addView(tabs,lp(dp(10)));
        ScrollView scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        Button erase=button("ERASE ALL LOCAL DATA");erase.setTextColor(Color.rgb(255,145,145));erase.setOnClickListener(v->confirmErase());root.addView(erase,lp(dp(8)));
        recent.setOnClickListener(v->showRecent());canonical.setOnClickListener(v->showCanonical());loops.setOnClickListener(v->showLoops());setContentView(root);refresh();
    }
    private void refresh(){String notifications=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners"),services=Settings.Secure.getString(getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);boolean enabled=notifications!=null&&notifications.contains(getPackageName()),screen=services!=null&&services.contains(getPackageName()+"/"+LifeScreenContextService.class.getName());PersistentUnderstandingStore u=PersistentUnderstandingStore.get(this);String version=u.canonicalEngineVersion();if(blank(version))version="pending";status.setText((enabled?"● Notifications":"○ Notifications")+"   "+(screen?"● Screen context":"○ Screen context")+"\n"+db.count("events")+" stored events   ·   "+db.count("open_loops")+" loops   ·   M1 "+version);status.setTextColor(enabled||screen?ACCENT:MUTED);showRecent();}
    private void showRecent(){content.removeAllViews();List<LifeDb.Event> xs=db.recentEvents(100);heading("RECENT EVIDENCE · LEGACY");if(xs.isEmpty()){empty("Nothing captured yet. Enable context access, then new useful notifications will appear here.");return;}for(LifeDb.Event x:xs){LinearLayout c=card();c.addView(text(blank(x.title)?friendlyApp(x.app):x.title,16,TEXT));TextView body=text(x.body,13,TEXT);body.setPadding(0,dp(6),0,0);c.addView(body);c.addView(meta(friendlyApp(x.app)+"  ·  "+formatTime(x.at)));content.addView(c,lp(dp(8)));}}
    private void showCanonical(){content.removeAllViews();PersistentUnderstandingStore u=PersistentUnderstandingStore.get(this);List<CanonicalEventRecord> xs=u.recentCanonical(100);String version=u.canonicalEngineVersion();long rebuilt=u.canonicalRebuiltAt();heading("CANONICAL RECENT · SHADOW "+(blank(version)?UnderstandingEngineVersion.CURRENT:version));if(rebuilt>0)content.addView(meta("Rebuilt "+formatTime(rebuilt)+" · raw history preserved"));if(xs.isEmpty()){empty("No canonical events yet. This view is experimental and does not replace Recent.");return;}for(CanonicalEventRecord x:xs){LinearLayout c=card();c.addView(text(x.direction.name()+" · "+x.type,11,ACCENT));TextView body=text(x.text,14,TEXT);body.setPadding(0,dp(6),0,0);c.addView(body);c.addView(meta(x.sources+"  ·  confidence "+Math.round(x.confidence*100)+"%  ·  "+formatTime(x.observedAt)));content.addView(c,lp(dp(8)));}}
    private void showLoops(){content.removeAllViews();List<LifeDb.Loop> xs=db.openLoops(100);heading("OPEN LOOPS");if(xs.isEmpty()){empty("No requests, commitments, or appointment-like messages need attention yet.");return;}for(LifeDb.Loop x:xs){LinearLayout c=card();TextView kind=text(x.kind.toUpperCase(),11,ACCENT);c.addView(kind);TextView title=text(x.title,15,TEXT);title.setPadding(0,dp(5),0,dp(8));c.addView(title);Button done=button("MARK DONE");done.setOnClickListener(v->{db.closeLoop(x.id);showLoops();});c.addView(done);content.addView(c,lp(dp(8)));}}
    private void confirmErase(){new AlertDialog.Builder(this).setTitle("Erase LifeOS data?").setMessage("This permanently deletes legacy events, open loops, raw understanding evidence, and the canonical view stored by LifeOS on this phone.").setNegativeButton("Cancel",null).setPositiveButton("Erase",(d,w)->{db.eraseAll();PersistentUnderstandingStore.get(this).eraseAllUnderstanding();refresh();}).show();}
    private void heading(String x){TextView v=text(x,12,MUTED);v.setPadding(0,dp(12),0,dp(8));content.addView(v);}
    private void empty(String x){TextView v=text(x,14,MUTED);v.setPadding(dp(16),dp(28),dp(16),dp(28));content.addView(v);}
    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(14),dp(16),dp(14));c.setBackgroundColor(CARD);return c;}
    private TextView meta(String x){TextView v=text(x,11,MUTED);v.setPadding(0,dp(8),0,0);return v;}
    private Button button(String x){Button b=new Button(this);b.setText(x);b.setTextColor(TEXT);b.setTextSize(12);b.setAllCaps(false);b.setBackgroundColor(CARD);return b;}
    private TextView text(String x,int size,int color){TextView v=new TextView(this);v.setText(x);v.setTextSize(size);v.setTextColor(color);v.setLineSpacing(0,1.08f);return v;}
    private LinearLayout.LayoutParams lp(int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,top,0,0);return p;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private static String formatTime(long at){return DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(at));}
    private static boolean blank(String x){return TextUtils.isEmpty(x)||x.trim().isEmpty();}
    private static String friendlyApp(String p){if(p==null)return "Unknown";int i=p.lastIndexOf('.');String x=i>=0?p.substring(i+1):p;return x.isEmpty()?p:Character.toUpperCase(x.charAt(0))+x.substring(1);}
}
