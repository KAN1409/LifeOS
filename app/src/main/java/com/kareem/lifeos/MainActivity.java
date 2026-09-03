package com.kareem.lifeos;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.kareem.lifeos.context.UniversalObservationStore;
import com.kareem.lifeos.engine.CanonicalEventRecord;
import com.kareem.lifeos.engine.PersistentUnderstandingStore;
import com.kareem.lifeos.engine.UnderstandingReplayEngine;
import com.kareem.lifeos.memory.PersistentLifeMemoryStore;
import com.teya.agent.harness.ConfigManager;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public final class MainActivity extends Activity {
    private static final int BG=Color.rgb(13,17,23),SURFACE=Color.rgb(22,27,34),BORDER=Color.rgb(48,54,61),TEXT=Color.rgb(230,237,243),MUTED=Color.rgb(139,148,158),GREEN=Color.rgb(63,185,80),BLUE=Color.rgb(47,129,247),RED=Color.rgb(248,81,73);
    private static final int MIC_REQUEST=901;
    private LifeDb db; private LinearLayout content; private TextView status; private Button recent,canonical,loops; private String pendingAgentAction;

    @Override public void onCreate(Bundle state){super.onCreate(state);db=new LifeDb(this);render();startReplayIfNeeded();}
    @Override protected void onResume(){super.onResume();if(content!=null)refresh();}
    @Override protected void onDestroy(){if(db!=null)db.close();super.onDestroy();}

    private void startReplayIfNeeded(){new Thread(()->{try{final boolean rebuilt=UnderstandingReplayEngine.replayIfNeeded(MainActivity.this);if(rebuilt)runOnUiThread(()->{if(status!=null)refresh();});}catch(Throwable ignored){}},"lifeos-understanding-replay").start();}

    private void render(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.VERTICAL);top.setPadding(dp(16),dp(18),dp(16),dp(14));top.setBackgroundColor(SURFACE);
        TextView title=text("LifeOS",22,TEXT);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);top.addView(title);
        TextView subtitle=text("V2 Agent Runtime · Teya + SecondBrain + Graphiti",12,MUTED);subtitle.setPadding(0,dp(2),0,0);top.addView(subtitle);
        root.addView(top,new LinearLayout.LayoutParams(-1,-2));root.addView(divider());

        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(16),dp(14),dp(16),0);
        status=text("",12,MUTED);body.addView(status);

        LinearLayout accessRow=new LinearLayout(this);accessRow.setOrientation(LinearLayout.HORIZONTAL);accessRow.setPadding(0,dp(12),0,dp(8));
        Button access=secondary("Notification access");access.setOnClickListener(v->startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")));
        Button screen=secondary("Screen context");screen.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        accessRow.addView(access,new LinearLayout.LayoutParams(0,dp(42),1));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,dp(42),1);rp.setMargins(dp(8),0,0,0);accessRow.addView(screen,rp);body.addView(accessRow);

        LinearLayout agent=row();
        TextView agentTitle=text("Agent runtime",15,TEXT);agentTitle.setTypeface(Typeface.DEFAULT,Typeface.BOLD);agent.addView(agentTitle);
        TextView agentSub=text("Transplanted Teya Harness · wake word · VAD/AEC · streaming voice · tools · memory dreamer",12,MUTED);agentSub.setPadding(0,dp(4),0,dp(10));agent.addView(agentSub);
        LinearLayout agentActions=new LinearLayout(this);agentActions.setOrientation(LinearLayout.HORIZONTAL);
        Button configure=secondary("Brain key");configure.setOnClickListener(v->configureBrain());
        Button start=secondary("Start agent");start.setOnClickListener(v->prepareAgent("start"));
        Button talk=primary("Talk now");talk.setOnClickListener(v->prepareAgent("talk"));
        agentActions.addView(configure,new LinearLayout.LayoutParams(0,dp(42),1));LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,dp(42),1);ap.setMargins(dp(7),0,0,0);agentActions.addView(start,ap);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,dp(42),1);tp.setMargins(dp(7),0,0,0);agentActions.addView(talk,tp);agent.addView(agentActions);
        Button intelligence=secondary("Life Intelligence  →");intelligence.setOnClickListener(v->startActivity(new Intent(this,SecondBrainActivity.class)));LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(-1,dp(44));ip.setMargins(0,dp(9),0,0);agent.addView(intelligence,ip);
        Button signals=secondary("Life Signals · Social Radar + Decisions  →");signals.setOnClickListener(v->startActivity(new Intent(this,LifeSignalsActivity.class)));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(44));sp.setMargins(0,dp(8),0,0);agent.addView(signals,sp);
        body.addView(agent);
        root.addView(body,new LinearLayout.LayoutParams(-1,-2));

        LinearLayout tabs=new LinearLayout(this);tabs.setOrientation(LinearLayout.HORIZONTAL);tabs.setPadding(dp(8),0,dp(8),0);tabs.setBackgroundColor(BG);
        recent=tab("Activity");canonical=tab("Understanding");loops=tab("Attention");
        tabs.addView(recent,new LinearLayout.LayoutParams(0,dp(46),1));tabs.addView(canonical,new LinearLayout.LayoutParams(0,dp(46),1));tabs.addView(loops,new LinearLayout.LayoutParams(0,dp(46),1));root.addView(tabs);root.addView(divider());

        ScrollView scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(16),dp(12),dp(16),dp(20));scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout footer=new LinearLayout(this);footer.setPadding(dp(16),dp(8),dp(16),dp(12));footer.setBackgroundColor(SURFACE);Button erase=secondary("Erase local preview data");erase.setTextColor(RED);erase.setOnClickListener(v->confirmErase());footer.addView(erase,new LinearLayout.LayoutParams(-1,dp(42)));root.addView(divider());root.addView(footer);

        recent.setOnClickListener(v->showRecent());canonical.setOnClickListener(v->showCanonical());loops.setOnClickListener(v->showLoops());setContentView(root);refresh();
    }

    private void configureBrain(){
        ConfigManager cfg=new ConfigManager(this);EditText input=new EditText(this);input.setTextColor(TEXT);input.setHintTextColor(MUTED);input.setSingleLine(true);input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);input.setHint(cfg.isConfigured()?"Configured — paste a new Mistral key to replace":"Paste Mistral API key");int pad=dp(18);input.setPadding(pad,dp(12),pad,dp(12));
        new AlertDialog.Builder(this).setTitle("Agent brain").setMessage("Stored locally using Teya's encrypted ConfigManager. LifeOS never commits this key to GitHub.").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{String key=input.getText().toString().trim();if(!key.isEmpty()){cfg.setMistralApiKey(key);Toast.makeText(this,"Brain key saved",Toast.LENGTH_SHORT).show();refresh();}}).show();
    }

    private void prepareAgent(String action){
        ConfigManager cfg=new ConfigManager(this);if(!cfg.isConfigured()){configureBrain();return;}
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){pendingAgentAction=action;requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},MIC_REQUEST);return;}
        if(!Settings.canDrawOverlays(this)){Toast.makeText(this,"Allow LifeOS to display the AEC voice surface, then tap again",Toast.LENGTH_LONG).show();startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName())));return;}
        executeAgent(action);
    }

    private void executeAgent(String action){
        Intent i=new Intent();i.setClassName(this,"com.teya.agent.harness.HarnessService");if("talk".equals(action))i.setAction("com.teya.agent.action.TRIGGER_VOICE");startForegroundService(i);Toast.makeText(this,"talk".equals(action)?"Listening…":"Agent runtime started",Toast.LENGTH_SHORT).show();
    }

    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==MIC_REQUEST&&grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED&&pendingAgentAction!=null){String a=pendingAgentAction;pendingAgentAction=null;prepareAgent(a);}}

    private void refresh(){
        String notifications=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners"),services=Settings.Secure.getString(getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        boolean enabled=notifications!=null&&notifications.contains(getPackageName()),screen=services!=null&&services.contains(getPackageName()+"/"+LifeScreenContextService.class.getName());
        PersistentUnderstandingStore u=PersistentUnderstandingStore.get(this);String version=u.canonicalEngineVersion();if(blank(version))version="pending";
        int raw=UniversalObservationStore.get(this).count(),memories=PersistentLifeMemoryStore.get(this).searchable().size();boolean brain=new ConfigManager(this).isConfigured();
        status.setText((enabled?"●":"○")+" notifications   "+(screen?"●":"○")+" screen context   "+(brain?"●":"○")+" agent brain\n"+raw+" raw observations  ·  "+memories+" grounded memories  ·  "+db.count("open_loops")+" attention items\nengine "+version+"  ·  Teya Harness imported  ·  SecondBrain + Graphiti clients ready");
        status.setTextColor(enabled||screen||brain?GREEN:MUTED);showRecent();
    }

    private void showRecent(){select(recent);content.removeAllViews();section("Activity","Recent captured evidence");List<LifeDb.Event> xs=db.recentEvents(100);if(xs.isEmpty()){empty("No captured activity yet. Turn on notification or screen context access.");return;}for(LifeDb.Event x:xs){LinearLayout row=row();TextView t=text(blank(x.title)?friendlyApp(x.app):x.title,15,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);row.addView(t);TextView body=text(x.body,13,TEXT);body.setPadding(0,dp(5),0,0);row.addView(body);row.addView(meta(friendlyApp(x.app)+" · "+formatTime(x.at)));content.addView(row);}}
    private void showCanonical(){select(canonical);content.removeAllViews();PersistentUnderstandingStore u=PersistentUnderstandingStore.get(this);List<CanonicalEventRecord> xs=u.recentCanonical(100);long rebuilt=u.canonicalRebuiltAt();section("Understanding","Canonical interpretation · raw evidence remains replayable");if(rebuilt>0)content.addView(meta("Rebuilt "+formatTime(rebuilt)+" · provenance preserved"));if(xs.isEmpty()){empty("No canonical events yet.");return;}for(CanonicalEventRecord x:xs){LinearLayout row=row();row.addView(badge(x.direction.name()+" · "+x.type));TextView body=text(x.text,14,TEXT);body.setPadding(0,dp(7),0,0);row.addView(body);row.addView(meta(x.sources+" · confidence "+Math.round(x.confidence*100)+"% · "+formatTime(x.observedAt)));content.addView(row);}}
    private void showLoops(){select(loops);content.removeAllViews();List<LifeDb.Loop> xs=db.openLoops(100);section("Attention","Things that may need action");if(xs.isEmpty()){empty("Nothing needs attention yet. Open Life Intelligence for SecondBrain commitments and briefing.");return;}for(LifeDb.Loop x:xs){LinearLayout row=row();row.addView(badge(x.kind.toUpperCase()));TextView title=text(x.title,15,TEXT);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setPadding(0,dp(7),0,dp(9));row.addView(title);Button done=secondary("Mark done");done.setOnClickListener(v->{db.closeLoop(x.id);showLoops();});row.addView(done,new LinearLayout.LayoutParams(-1,dp(40)));content.addView(row);}}

    private void confirmErase(){new AlertDialog.Builder(this).setTitle("Erase preview data?").setMessage("This deletes legacy and V2 data stored by this install.").setNegativeButton("Cancel",null).setPositiveButton("Erase",(d,w)->{db.eraseAll();PersistentUnderstandingStore.get(this).eraseAllUnderstanding();UniversalObservationStore.get(this).eraseAll();PersistentLifeMemoryStore.get(this).eraseAll();refresh();}).show();}
    private void section(String title,String subtitle){TextView t=text(title,20,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);content.addView(t);TextView s=text(subtitle,12,MUTED);s.setPadding(0,dp(2),0,dp(12));content.addView(s);}
    private void empty(String x){LinearLayout r=row();TextView v=text(x,14,MUTED);v.setPadding(0,dp(4),0,dp(4));r.addView(v);content.addView(r);}
    private LinearLayout row(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(13),dp(14),dp(13));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(8));c.setLayoutParams(p);c.setBackground(round(SURFACE,BORDER,8));return c;}
    private TextView badge(String x){TextView v=text(x,11,GREEN);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setPadding(dp(8),dp(4),dp(8),dp(4));v.setBackground(round(Color.TRANSPARENT,GREEN,20));return v;}
    private TextView meta(String x){TextView v=text(x,11,MUTED);v.setPadding(0,dp(8),0,0);return v;}
    private Button secondary(String x){Button b=new Button(this);b.setText(x);b.setTextColor(TEXT);b.setTextSize(12);b.setAllCaps(false);b.setGravity(Gravity.CENTER);b.setBackground(round(SURFACE,BORDER,7));return b;}
    private Button primary(String x){Button b=secondary(x);b.setBackground(round(BLUE,BLUE,7));b.setTextColor(Color.WHITE);return b;}
    private Button tab(String x){Button b=new Button(this);b.setText(x);b.setTextColor(MUTED);b.setTextSize(12);b.setAllCaps(false);b.setBackgroundColor(Color.TRANSPARENT);return b;}
    private void select(Button active){for(Button b:new Button[]{recent,canonical,loops}){b.setTextColor(b==active?TEXT:MUTED);b.setTypeface(Typeface.DEFAULT,b==active?Typeface.BOLD:Typeface.NORMAL);}}
    private View divider(){View v=new View(this);v.setBackgroundColor(BORDER);v.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(1)));return v;}
    private GradientDrawable round(int fill,int stroke,int radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));g.setStroke(dp(1),stroke);return g;}
    private TextView text(String x,int size,int color){TextView v=new TextView(this);v.setText(x);v.setTextSize(size);v.setTextColor(color);v.setLineSpacing(0,1.08f);return v;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private static String formatTime(long at){return DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(at));}
    private static boolean blank(String x){return TextUtils.isEmpty(x)||x.trim().isEmpty();}
    private static String friendlyApp(String p){if(p==null)return "Unknown";int i=p.lastIndexOf('.');String x=i>=0?p.substring(i+1):p;return x.isEmpty()?p:Character.toUpperCase(x.charAt(0))+x.substring(1);}
}
