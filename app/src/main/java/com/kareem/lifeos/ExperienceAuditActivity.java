package com.kareem.lifeos;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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

/** One tap: walk important LifeOS surfaces, run runtime invariants and export one QA ZIP. */
public final class ExperienceAuditActivity extends Activity {
    private static final int BG=Color.rgb(13,17,23),SURFACE=Color.rgb(22,27,34),BORDER=Color.rgb(48,54,61),TEXT=Color.rgb(230,237,243),MUTED=Color.rgb(139,148,158),GREEN=Color.rgb(63,185,80),BLUE=Color.rgb(47,129,247);
    private TextView state;private Button runButton;private boolean launching,preparing;private volatile boolean auditCancelled;
    @Override public void onCreate(Bundle b){super.onCreate(b);auditCancelled=false;render();}
    @Override protected void onResume(){super.onResume();launching=false;if(ExperienceAudit.active())continueAudit();}
    @Override protected void onDestroy(){auditCancelled=true;super.onDestroy();}

    private void render(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(24),dp(20),dp(24));root.setBackgroundColor(BG);TextView h=t("Full LifeOS QA",25,TEXT);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);root.addView(h);TextView sub=t("Runs non-destructive runtime/provider checks, then automatically visits product and diagnostic screens, captures pixels + hierarchy + runtime state, and exports one QA ZIP.",14,MUTED);sub.setPadding(0,dp(8),0,dp(18));root.addView(sub);state=t("Ready",13,GREEN);root.addView(state);runButton=button("Run full LifeOS test",true);runButton.setOnClickListener(v->begin());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(50));p.setMargins(0,dp(20),0,0);root.addView(runButton,p);Button back=button("Back",false);back.setOnClickListener(v->finish());LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(48));bp.setMargins(0,dp(10),0,0);root.addView(back,bp);TextView info=t("Output: qa_report.json + db_integrity.json + screenshots (.png) + screen structure (.json) + runtime/data snapshot (.json), saved as one ZIP in Downloads/LifeOS on Android 10+ or app external files on older Android. The test does not mark commitments handled or create/delete user data.",12,MUTED);info.setPadding(0,dp(20),0,0);root.addView(info);setContentView(root);}

    private void begin(){
        if(preparing||ExperienceAudit.active())return;
        preparing=true;runButton.setEnabled(false);state.setText("Preparing QA · scanning runtime state…");
        new Thread(()->{
            try{
                List<ExperienceAudit.Target> xs=buildTargets();
                if(auditCancelled||isFinishing()||(Build.VERSION.SDK_INT>=17&&isDestroyed()))return;
                ExperienceAudit.start(this,xs);
                runOnUiThread(()->{
                    if(auditCancelled||isFinishing()||(Build.VERSION.SDK_INT>=17&&isDestroyed())){ExperienceAudit.finish();return;}
                    preparing=false;runButton.setEnabled(true);continueAudit();
                });
            }catch(Throwable t){
                if(auditCancelled)return;
                runOnUiThread(()->{
                    if(auditCancelled)return;
                    preparing=false;runButton.setEnabled(true);state.setText("QA start failed · "+safeMessage(t));
                });
            }
        },"lifeos-audit-prepare").start();
    }

    private List<ExperienceAudit.Target> buildTargets(){
        List<ExperienceAudit.Target> xs=new ArrayList<>();
        xs.add(target("01 Now",FeedActivity.class,null,null));
        xs.add(target("02 Needs Attention",FeedSectionActivity.class,"mode","attention"));
        xs.add(target("03 Timeline",TimelineActivity.class,null,null));
        xs.add(target("04 Search",SearchActivity.class,null,null));
        xs.add(target("05 Ask",AskLifeOsActivity.class,null,null));
        xs.add(target("06 LifeOS Status",SystemStatusActivity.class,null,null));
        addCapability(xs,"07 Conversations","conversations");
        addCapability(xs,"08 Commitments","commitments");
        addCapability(xs,"09 Decisions","decisions");
        addCapability(xs,"10 Voice Memories","voice");
        addCapability(xs,"11 Files","files");
        addCapability(xs,"12 Projects","projects");
        addCapability(xs,"13 Places","places");
        addCapability(xs,"14 People","people");
        addCapability(xs,"15 Events","events");
        xs.add(target("16 Voice Recorder",VoiceRecorderActivity.class,null,null));
        xs.add(target("17 OCR Quality Lab",OcrLabActivity.class,null,null));
        xs.add(target("18 Suggested Actions",FeedSectionActivity.class,"mode","actions"));
        xs.add(target("19 Today",FeedSectionActivity.class,"mode","activity"));
        xs.add(target("20 Diagnostics Activity",MainActivity.class,"audit_tab","activity"));
        xs.add(target("21 Diagnostics Understanding",MainActivity.class,"audit_tab","understanding"));
        xs.add(target("22 Diagnostics Attention",MainActivity.class,"audit_tab","attention"));
        xs.add(target("23 Social Radar",LifeSignalsActivity.class,"audit_tab","social"));
        xs.add(target("24 Decision Memory",LifeSignalsActivity.class,"audit_tab","decisions"));
        xs.add(target("25 Action Center Pending",ActionCenterActivity.class,null,null));
        xs.add(target("26 Action Center History",ActionCenterActivity.class,null,null));
        addRealObjectDetails(xs);
        return xs;
    }

    private void addCapability(List<ExperienceAudit.Target> xs,String label,String id){xs.add(new ExperienceAudit.Target(label,new Intent(this,CapabilityActivity.class).putExtra("capability",id)));}

    private void addRealObjectDetails(List<ExperienceAudit.Target> xs){
        try{
            for(String cap:new String[]{"commitments","files","projects","places","people","events","decisions"}){
                List<FunctionalCapabilityRegistry.ObjectItem> items=FunctionalCapabilityRegistry.list(this,cap,1);if(items.isEmpty())continue;FunctionalCapabilityRegistry.ObjectItem x=items.get(0);xs.add(new ExperienceAudit.Target("Detail "+cap,new Intent(this,FunctionalObjectDetailActivity.class).putExtra("capability_id",cap).putExtra("object_id",x.objectId)));
            }
            List<VoiceMemoryRepository.VoiceObject> voice=VoiceMemoryRepository.list(this,1);if(!voice.isEmpty())xs.add(new ExperienceAudit.Target("Detail voice",new Intent(this,VoiceMemoryDetailActivity.class).putExtra("voice_id",voice.get(0).id)));
            List<ImageRepository.ImageObject> images=ImageRepository.list(this,1);if(!images.isEmpty())xs.add(new ExperienceAudit.Target("Detail OCR image",new Intent(this,ImageOcrDetailActivity.class).putExtra("image_id",images.get(0).id)));
            List<ConversationRepository.ConversationObject> conv=ConversationRepository.list(this,1);if(!conv.isEmpty())xs.add(new ExperienceAudit.Target("Detail conversation",new Intent(this,ConversationDetailActivity.class).putExtra("conversation_id",conv.get(0).id)));
        }catch(Throwable ignored){}
    }

    private ExperienceAudit.Target target(String label,Class<?> c,String k,String v){Intent i=new Intent(this,c);if(k!=null)i.putExtra(k,v);return new ExperienceAudit.Target(label,i);}
    private void continueAudit(){if(launching)return;ExperienceAudit.Target x=ExperienceAudit.current();if(x==null){File d=ExperienceAudit.finish();if(d!=null)export(d);return;}launching=true;state.setText("Capturing "+(ExperienceAudit.index()+1)+" / "+ExperienceAudit.total()+" · "+x.label);startActivity(x.intent);}

    private void export(File dir){state.setText("Exporting QA report…");new Thread(()->{try{
        FullQaHarness.Report report=FullQaHarness.write(this,new File(dir,"qa_report.json"));
        PhysicalStorageAudit.write(this,new File(dir,"db_integrity.json"));
        File zip=new File(getCacheDir(),dir.getName()+".zip");zip(dir,zip);String destination=publishZip(zip);runOnUiThread(()->{state.setText("Complete · "+report.pass+" pass · "+report.fail+" fail · "+report.warn+" warn");Toast.makeText(this,"QA saved to "+destination,Toast.LENGTH_LONG).show();});
    }catch(Exception e){runOnUiThread(()->state.setText("Export failed · "+e.getMessage()));}},"lifeos-audit-export").start();}

    private String publishZip(File zip)throws Exception{
        if(Build.VERSION.SDK_INT>=29){ContentValues v=new ContentValues();v.put(MediaStore.Downloads.DISPLAY_NAME,zip.getName());v.put(MediaStore.Downloads.MIME_TYPE,"application/zip");v.put(MediaStore.Downloads.RELATIVE_PATH,"Download/LifeOS");Uri u=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,v);if(u==null)throw new IllegalStateException("Cannot create Downloads file");try(OutputStream o=getContentResolver().openOutputStream(u);FileInputStream in=new FileInputStream(zip)){copy(in,o);}return "Downloads/LifeOS";}
        File base=getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);if(base==null)base=getExternalFilesDir(null);if(base==null)throw new IllegalStateException("External files directory unavailable");File outDir=new File(base,"LifeOS");if(!outDir.exists()&&!outDir.mkdirs())throw new IllegalStateException("Cannot create QA export directory");File out=new File(outDir,zip.getName());try(FileOutputStream o=new FileOutputStream(out);FileInputStream in=new FileInputStream(zip)){copy(in,o);}return out.getAbsolutePath();
    }
    private static String safeMessage(Throwable t){String m=t==null?null:t.getMessage();return m==null||m.trim().isEmpty()?String.valueOf(t):m;}
    private static void copy(FileInputStream in,OutputStream out)throws Exception{byte[] b=new byte[65536];for(int n;(n=in.read(b))>0;)out.write(b,0,n);}
    private static void zip(File dir,File out)throws Exception{try(ZipOutputStream z=new ZipOutputStream(new FileOutputStream(out))){File[] fs=dir.listFiles();if(fs==null)return;byte[] b=new byte[65536];for(File f:fs){if(!f.isFile())continue;z.putNextEntry(new ZipEntry(f.getName()));try(FileInputStream in=new FileInputStream(f)){for(int n;(n=in.read(b))>0;)z.write(b,0,n);}z.closeEntry();}}}
    private Button button(String x,boolean primary){Button b=new Button(this);b.setText(x);b.setAllCaps(false);b.setTextColor(TEXT);b.setTextSize(14);b.setGravity(Gravity.CENTER);b.setBackground(round(primary?BLUE:SURFACE,primary?BLUE:BORDER,9));return b;}private TextView t(String x,int s,int c){TextView v=new TextView(this);v.setText(x);v.setTextSize(s);v.setTextColor(c);v.setLineSpacing(0,1.08f);return v;}private GradientDrawable round(int fill,int stroke,int r){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(r));g.setStroke(dp(1),stroke);return g;}private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
