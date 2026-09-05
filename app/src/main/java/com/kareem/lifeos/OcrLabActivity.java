package com.kareem.lifeos;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import java.util.Locale;

/** Experimental OCR validation surface. Images are not promoted to normal Search until quality gates pass. */
public final class OcrLabActivity extends Activity {
    private static final int PICK=701;private LinearLayout content;
    @Override public void onCreate(Bundle s){super.onCreate(s);render();}
    @Override protected void onResume(){super.onResume();refresh();}
    private void render(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(LifeOsUi.BG);root.addView(LifeOsUi.detailTopBar(this,"OCR quality lab"));ScrollView sc=new ScrollView(this);sc.setVerticalScrollBarEnabled(false);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(LifeOsUi.dp(this,16),LifeOsUi.dp(this,7),LifeOsUi.dp(this,16),LifeOsUi.dp(this,24));sc.addView(content);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);}
    private void refresh(){if(content==null)return;content.removeAllViews();OcrBenchmark.Summary b=OcrBenchmark.summarize(this);TextView intro=LifeOsUi.text(this,"This lab measures real OCR accuracy before Images is allowed into the normal LifeOS capability grid.",10.7f,LifeOsUi.MUTED);LifeOsUi.weight(intro,500);content.addView(intro);content.addView(LifeOsUi.section(this,"Benchmark"));LinearLayout bench=LifeOsUi.card(this);TextView status=LifeOsUi.text(this,b.readyForPromotion()?"PROMOTION GATE PASSED":"VALIDATION REQUIRED",9.5f,b.readyForPromotion()?LifeOsUi.GREEN:LifeOsUi.AMBER);LifeOsUi.weight(status,700);bench.addView(status);TextView metrics=LifeOsUi.text(this,b.samples+" ground-truth samples · CER "+pct(b.cer)+" · WER "+pct(b.wer),11.5f,LifeOsUi.TEXT);LifeOsUi.weight(metrics,600);metrics.setPadding(0,LifeOsUi.dp(this,5),0,0);bench.addView(metrics);TextView gate=LifeOsUi.text(this,"Gate: ≥30 scored samples, CER ≤8%, WER ≤18%. Arabic + mixed-language samples must be included before product promotion.",9.7f,LifeOsUi.MUTED);LifeOsUi.weight(gate,500);gate.setPadding(0,LifeOsUi.dp(this,4),0,0);bench.addView(gate);content.addView(bench);
        Button add=LifeOsUi.primary(this,"Add OCR test image");add.setOnClickListener(v->pick());content.addView(add,new LinearLayout.LayoutParams(-1,LifeOsUi.dp(this,46)));content.addView(LifeOsUi.section(this,"Test images"));List<ImageRepository.ImageObject> xs=ImageRepository.list(this,100);if(xs.isEmpty()){TextView none=LifeOsUi.text(this,"No OCR test images yet.",11f,LifeOsUi.MUTED);LifeOsUi.weight(none,500);content.addView(none);return;}for(ImageRepository.ImageObject x:xs)content.addView(row(x));}
    private View row(ImageRepository.ImageObject x){LinearLayout r=LifeOsUi.card(this);LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);head.addView(LifeOsUi.iconTile(this,LifeOsIconView.FILE,LifeOsUi.AMBER,36),new LinearLayout.LayoutParams(LifeOsUi.dp(this,36),LifeOsUi.dp(this,36)));LinearLayout copy=new LinearLayout(this);copy.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-2,1);cp.setMargins(LifeOsUi.dp(this,10),0,LifeOsUi.dp(this,6),0);head.addView(copy,cp);TextView t=LifeOsUi.text(this,x.displayName,12.2f,LifeOsUi.TEXT);LifeOsUi.weight(t,600);t.setMaxLines(1);copy.addView(t);OcrResult latest=x.latest(this);String meta=x.width+"×"+x.height+" · "+human(x.ocrStatus)+(latest!=null?" · "+Math.round(latest.confidence*100)+"%":"");TextView m=LifeOsUi.text(this,meta,9.7f,"complete".equals(x.ocrStatus)?LifeOsUi.GREEN:"failed".equals(x.ocrStatus)?LifeOsUi.RED:LifeOsUi.MUTED);LifeOsUi.weight(m,500);m.setPadding(0,LifeOsUi.dp(this,2),0,0);copy.addView(m);head.addView(LifeOsUi.icon(this,LifeOsIconView.CHEVRON,LifeOsUi.TERTIARY,12),new LinearLayout.LayoutParams(LifeOsUi.dp(this,12),LifeOsUi.dp(this,18)));r.addView(head);r.setOnClickListener(v->startActivity(new Intent(this,ImageOcrDetailActivity.class).putExtra("image_id",x.id)));return r;}
    private void pick(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,PICK);}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(requestCode!=PICK||resultCode!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();ImageRepository.ImageObject image=ImageRepository.importUri(this,uri,"OCR quality lab");if(image==null){Toast.makeText(this,"Image could not be imported.",Toast.LENGTH_LONG).show();return;}refresh();ImageRepository.analyze(this,image.id,new ImageOcrEngine.Callback(){public void ok(OcrResult result){refresh();startActivity(new Intent(OcrLabActivity.this,ImageOcrDetailActivity.class).putExtra("image_id",image.id));}public void fail(Exception error){refresh();Toast.makeText(OcrLabActivity.this,"OCR failed: "+error.getClass().getSimpleName(),Toast.LENGTH_LONG).show();}});}
    private static String pct(double x){return String.format(Locale.US,"%.1f%%",x*100);}private static String human(String x){String v=x==null?"not run":x.replace('_',' ');return v.isEmpty()?"not run":v;}
}
