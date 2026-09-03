package com.kareem.lifeos;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** One tap: walk every important LifeOS surface, capture pixels + hierarchy + runtime state, export ZIP. */
public final class ExperienceAuditActivity extends Activity {
    private static final int BG=Color.rgb(13,17,23),SURFACE=Color.rgb(22,27,34),BORDER=Color.rgb(48,54,61),TEXT=Color.rgb(230,237,243),MUTED=Color.rgb(139,148,158),GREEN=Color.rgb(63,185,80),BLUE=Color.rgb(47,129,247);
    private TextView state;private boolean launching;
    @Override public void onCreate(Bundle b){super.onCreate(b);render();}
    @Override protected void onResume(){super.onResume();launching=false;if(ExperienceAudit.active())continueAudit();}

    private void render(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(24),dp(20),dp(24));root.setBackgroundColor(BG);TextView h=t("Experience Audit",25,TEXT);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);root.addView(h);TextView sub=t("Automatically visits LifeOS screen-by-screen, captures the real rendered UI, visible text and view hierarchy, then adds a sanitized under-the-hood data/runtime snapshot.",14,MUTED);sub.setPadding(0,dp(8),0,dp(18));root.addView(sub);state=t("Ready",13,GREEN);root.addView(state);Button run=button("Run full experience audit",true);run.setOnClickListener(v->begin());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(50));p.setMargins(0,dp(20),0,0);root.addView(run,p);Button back=button("Back",false);back.setOnClickListener(v->finish());LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(46));bp.setMargins(0,dp(10),0,0);root.addView(back,bp);TextView info=t("Output: screenshots (.png) + screen structure (.json) + runtime/data snapshot (.json), packaged into one ZIP in Downloads/LifeOS.",12,MUTED);info.setPadding(0,dp(20),0,0);root.addView(info);setContentView(root);}

    private void begin(){List<ExperienceAudit.Target> xs=new ArrayList<>();
        xs.add(target("01 Home",FeedActivity.class,null,null));
        xs.add(target("02 Needs Attention",FeedSectionActivity.class,"mode","attention"));
        xs.add(target("03 Suggested Actions",FeedSectionActivity.class,"mode","actions"));
        xs.add(target("04 Today",FeedSectionActivity.class,"mode","activity"));
        xs.add(target("05 Diagnostics Activity",MainActivity.class,"audit_tab","activity"));
        xs.add(target("06 Diagnostics Understanding",MainActivity.class,"audit_tab","understanding"));
        xs.add(target("07 Diagnostics Attention",MainActivity.class,"audit_tab","attention"));
        xs.add(target("08 Social Radar",LifeSignalsActivity.class,"audit_tab","social"));
        xs.add(target("09 Decision Memory",LifeSignalsActivity.class,"audit_tab","decisions"));
        xs.add(target("10 Life Intelligence",SecondBrainActivity.class,null,null));
        xs.add(target("11 Action Center",ActionCenterActivity.class,null,null));
        try{LifeDb db=new LifeDb(this);List<LifeDb.Event> e=db.recentEvents(1);if(!e.isEmpty()){xs.add(new ExperienceAudit.Target("12 Conversation Detail",new Intent(this,ConversationDetailActivity.class).putExtra("event_id",e.get(0).id)));xs.add(new ExperienceAudit.Target("13 Evidence Detail",new Intent(this,EvidenceDetailActivity.class).putExtra("event_id",e.get(0).id).putExtra("mode","activity")));}List<SituationEngine.Situation> s=SituationEngine.build(db,db.openLoops(50),new com.kareem.lifeos.actions.PersistentActionQueue(this).pending(),System.currentTimeMillis());if(!s.isEmpty())xs.add(new ExperienceAudit.Target("14 Situation Detail",new Intent(this,SituationDetailActivity.class).putExtra("situation_id",s.get(0).id)));db.close();}catch(Throwable ignored){}
        ExperienceAudit.start(this,xs);continueAudit();
    }
    private ExperienceAudit.Target target(String label,Class<?> c,String k,String v){Intent i=new Intent(this,c);if(k!=null)i.putExtra(k,v);return new ExperienceAudit.Target(label,i);}
    private void continueAudit(){if(launching)return;ExperienceAudit.Target x=ExperienceAudit.current();if(x==null){File d=ExperienceAudit.finish();if(d!=null)export(d);return;}launching=true;state.setText("Capturing "+(ExperienceAudit.index()+1)+" / "+ExperienceAudit.total()+" · "+x.label);startActivity(x.intent);}

    private void export(File dir){new Thread(()->{try{File zip=new File(getCacheDir(),dir.getName()+".zip");zip(dir,zip);ContentValues v=new ContentValues();v.put(MediaStore.Downloads.DISPLAY_NAME,zip.getName());v.put(MediaStore.Downloads.MIME_TYPE,"application/zip");v.put(MediaStore.Downloads.RELATIVE_PATH,"Download/LifeOS");Uri u=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,v);if(u==null)throw new IllegalStateException("Cannot create Downloads file");try(OutputStream o=getContentResolver().openOutputStream(u);FileInputStream in=new FileInputStream(zip)){byte[] b=new byte[65536];for(int n;(n=in.read(b))>0;)o.write(b,0,n);}runOnUiThread(()->{state.setText("Complete · "+zip.getName());Toast.makeText(this,"Audit saved to Downloads/LifeOS",Toast.LENGTH_LONG).show();});}catch(Exception e){runOnUiThread(()->state.setText("Export failed · "+e.getMessage()));}},"lifeos-audit-export").start();}
    private static void zip(File dir,File out)throws Exception{try(ZipOutputStream z=new ZipOutputStream(new FileOutputStream(out))){File[] fs=dir.listFiles();if(fs==null)return;byte[] b=new byte[65536];for(File f:fs){if(!f.isFile())continue;z.putNextEntry(new ZipEntry(f.getName()));try(FileInputStream in=new FileInputStream(f)){for(int n;(n=in.read(b))>0;)z.write(b,0,n);}z.closeEntry();}}}
    private Button button(String x,boolean primary){Button b=new Button(this);b.setText(x);b.setAllCaps(false);b.setTextColor(TEXT);b.setTextSize(14);b.setGravity(Gravity.CENTER);b.setBackground(round(primary?BLUE:SURFACE,primary?BLUE:BORDER,9));return b;}private TextView t(String x,int s,int c){TextView v=new TextView(this);v.setText(x);v.setTextSize(s);v.setTextColor(c);v.setLineSpacing(0,1.08f);return v;}private GradientDrawable round(int fill,int stroke,int r){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(r));g.setStroke(dp(1),stroke);return g;}private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
