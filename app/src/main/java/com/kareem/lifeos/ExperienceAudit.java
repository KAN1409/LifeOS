package com.kareem.lifeos;

import android.app.Activity;
import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.kareem.lifeos.actions.PersistentActionQueue;
import com.kareem.lifeos.context.UniversalObservationStore;
import com.kareem.lifeos.engine.PersistentUnderstandingStore;
import com.kareem.lifeos.memory.PersistentLifeMemoryStore;
import com.teya.agent.harness.ConfigManager;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/** Records real rendered LifeOS screens plus a sanitized runtime/data snapshot. */
public final class ExperienceAudit {
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static volatile Session session;
    private ExperienceAudit(){}

    public static final class Target {
        public final String label; public final android.content.Intent intent;
        public Target(String label,android.content.Intent intent){this.label=label;this.intent=intent;}
    }
    static final class Session {
        final File dir; final List<Target> targets; int index;
        Session(File d,List<Target> t){dir=d;targets=t;}
    }

    public static void install(Application app){
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks(){
            public void onActivityCreated(Activity a,Bundle b){} public void onActivityStarted(Activity a){}
            public void onActivityResumed(Activity a){captureIfNeeded(a);} public void onActivityPaused(Activity a){}
            public void onActivityStopped(Activity a){} public void onActivitySaveInstanceState(Activity a,Bundle b){} public void onActivityDestroyed(Activity a){}
        });
    }

    public static synchronized void start(Activity owner,List<Target> targets){
        File root=new File(owner.getExternalFilesDir(null),"experience-audits"); root.mkdirs();
        String stamp=new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date());
        File dir=new File(root,"LifeOS_Audit_"+stamp);dir.mkdirs();
        session=new Session(dir,new ArrayList<>(targets));
        writeJson(new File(dir,"runtime.json"),runtimeSnapshot(owner));
    }
    public static synchronized boolean active(){return session!=null;}
    public static synchronized int index(){return session==null?0:session.index;}
    public static synchronized int total(){return session==null?0:session.targets.size();}
    public static synchronized Target current(){return session==null||session.index>=session.targets.size()?null:session.targets.get(session.index);}
    public static synchronized File finish(){File d=session==null?null:session.dir;session=null;return d;}

    private static void captureIfNeeded(Activity a){
        Session s=session;if(s==null||a instanceof ExperienceAuditActivity)return;
        Target target=current(); if(target==null)return;
        MAIN.postDelayed(()->{
            View root=a.getWindow().getDecorView().getRootView();
            tapRequestedTab(root,target.label);
            MAIN.postDelayed(()->captureAndFinish(a,root,s,target),needsAsyncWait(target.label)?1200:450);
        },550);
    }
    private static void captureAndFinish(Activity a,View root,Session s,Target target){
        try{
            String base=String.format(Locale.US,"%02d_%s",s.index+1,safe(target.label));
            int w=Math.max(1,root.getWidth()),h=Math.max(1,root.getHeight());
            Bitmap bmp=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);root.draw(new Canvas(bmp));
            try(FileOutputStream out=new FileOutputStream(new File(s.dir,base+".png"))){bmp.compress(Bitmap.CompressFormat.PNG,100,out);}bmp.recycle();
            JSONObject screen=new JSONObject();screen.put("label",target.label);screen.put("activity",a.getClass().getName());screen.put("captured_at",System.currentTimeMillis());screen.put("width",w);screen.put("height",h);screen.put("view_tree",viewJson(root));screen.put("visible_text",visibleText(root));
            writeJson(new File(s.dir,base+".json"),screen);
            synchronized(ExperienceAudit.class){if(session==s)s.index++;}
        }catch(Throwable t){
            try{JSONObject e=new JSONObject();e.put("screen",target.label);e.put("error",String.valueOf(t));writeJson(new File(s.dir,"capture_error_"+s.index+".json"),e);}catch(Throwable ignored){}
            synchronized(ExperienceAudit.class){if(session==s)s.index++;}
        }
        a.finish();
    }
    private static void tapRequestedTab(View root,String label){
        String text=null;
        if(label.contains("Understanding"))text="Understanding";
        else if(label.contains("Diagnostics Attention"))text="Attention";
        else if(label.contains("Social Radar"))text="Social Radar";
        else if(label.contains("Decision Memory"))text="Decision Memory";
        if(text!=null){View v=findText(root,text);if(v!=null)v.performClick();}
    }
    private static boolean needsAsyncWait(String label){return label.contains("Social Radar")||label.contains("Decision Memory")||label.contains("Life Intelligence");}
    private static View findText(View v,String wanted){if(v instanceof TextView&&wanted.contentEquals(((TextView)v).getText()))return v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){View r=findText(g.getChildAt(i),wanted);if(r!=null)return r;}}return null;}

    private static JSONObject runtimeSnapshot(Activity a){
        JSONObject j=new JSONObject();
        try{
            LifeDb db=new LifeDb(a);j.put("package",a.getPackageName());j.put("version_name",a.getPackageManager().getPackageInfo(a.getPackageName(),0).versionName);j.put("version_code",a.getPackageManager().getPackageInfo(a.getPackageName(),0).getLongVersionCode());
            j.put("raw_observations",UniversalObservationStore.get(a).count());j.put("grounded_memories",PersistentLifeMemoryStore.get(a).searchable().size());j.put("canonical_engine",PersistentUnderstandingStore.get(a).canonicalEngineVersion());j.put("attention_items",db.count("open_loops"));j.put("agent_actions",new PersistentActionQueue(a).pending().size());j.put("agent_brain_configured",new ConfigManager(a).isConfigured());
            JSONArray events=new JSONArray();for(LifeDb.Event e:db.recentEvents(30)){JSONObject x=new JSONObject();x.put("id",e.id);x.put("app",e.app);x.put("title",e.title);x.put("body",e.body);x.put("at",e.at);events.put(x);}j.put("recent_events",events);
            JSONArray loops=new JSONArray();for(LifeDb.Loop l:db.openLoops(30)){JSONObject x=new JSONObject();x.put("id",l.id);x.put("kind",l.kind);x.put("title",l.title);x.put("evidence_id",l.evidenceId);loops.put(x);}j.put("open_loops",loops);
            List<PersistentActionQueue.Item> actions=new PersistentActionQueue(a).pending();List<SituationEngine.Situation> situations=SituationEngine.build(db,db.openLoops(50),actions,System.currentTimeMillis());JSONArray ss=new JSONArray();for(SituationEngine.Situation q:situations){JSONObject x=new JSONObject();x.put("id",q.id);x.put("title",q.title);x.put("status",q.status);x.put("summary",q.summary);x.put("why",q.why);x.put("priority",q.score);x.put("evidence",q.eventCount);x.put("attention",q.attentionCount);x.put("actions",q.actionCount);ss.put(x);}j.put("situations",ss);db.close();
        }catch(Throwable t){try{j.put("snapshot_error",String.valueOf(t));}catch(Exception ignored){}}
        return j;
    }

    private static JSONObject viewJson(View v)throws Exception{
        JSONObject j=new JSONObject();j.put("class",v.getClass().getName());j.put("id",v.getId());j.put("visible",v.getVisibility()==View.VISIBLE);j.put("clickable",v.isClickable());j.put("enabled",v.isEnabled());j.put("x",v.getX());j.put("y",v.getY());j.put("w",v.getWidth());j.put("h",v.getHeight());
        if(v instanceof TextView)j.put("text",((TextView)v).getText().toString());
        if(v instanceof ViewGroup){JSONArray c=new JSONArray();ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)c.put(viewJson(g.getChildAt(i)));j.put("children",c);}return j;
    }
    private static JSONArray visibleText(View v)throws Exception{JSONArray out=new JSONArray();collectText(v,out);return out;}
    private static void collectText(View v,JSONArray out)throws Exception{if(v.getVisibility()!=View.VISIBLE)return;if(v instanceof TextView){String t=((TextView)v).getText().toString().trim();if(!t.isEmpty())out.put(t);}if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)collectText(g.getChildAt(i),out);}}
    private static void writeJson(File f,JSONObject j){try(FileOutputStream o=new FileOutputStream(f)){o.write(j.toString(2).getBytes(StandardCharsets.UTF_8));}catch(Exception ignored){}}
    private static String safe(String x){return x.replaceAll("[^A-Za-z0-9._-]+","_");}
}
