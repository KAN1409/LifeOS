package com.kareem.lifeos;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.DateFormat;
import java.util.Date;

public final class EvidenceDetailActivity extends Activity {
    private static final int BG=Color.rgb(13,17,23),SURFACE=Color.rgb(22,27,34),BORDER=Color.rgb(48,54,61),TEXT=Color.rgb(230,237,243),MUTED=Color.rgb(139,148,158),GREEN=Color.rgb(63,185,80),BLUE=Color.rgb(47,129,247);
    private LifeDb db;private long eventId,loopId;private LifeDb.Event event;
    @Override public void onCreate(Bundle s){super.onCreate(s);db=new LifeDb(this);eventId=getIntent().getLongExtra("event_id",0);loopId=getIntent().getLongExtra("loop_id",0);event=db.eventById(eventId);render();}
    @Override protected void onDestroy(){db.close();super.onDestroy();}
    private void render(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(dp(12),dp(14),dp(16),dp(12));top.setBackgroundColor(SURFACE);Button back=plain("‹");back.setOnClickListener(v->finish());top.addView(back,new LinearLayout.LayoutParams(dp(48),dp(40)));TextView title=text("Detail",22,TEXT);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);top.addView(title);root.addView(top);root.addView(divider());ScrollView sc=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(16),dp(18),dp(16),dp(28));sc.addView(body);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));if(event==null){body.addView(text("Original evidence is no longer available.",14,MUTED));setContentView(root);return;}TextView app=text(app(event.app),12,GREEN);app.setTypeface(Typeface.DEFAULT,Typeface.BOLD);body.addView(app);TextView h=text(event.title==null||event.title.isEmpty()?"Captured evidence":event.title,21,TEXT);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);h.setPadding(0,dp(8),0,dp(4));body.addView(h);body.addView(text(time(event.at),12,MUTED));LinearLayout evidence=row();TextView et=text("Original evidence",13,MUTED);et.setTypeface(Typeface.DEFAULT,Typeface.BOLD);evidence.addView(et);TextView eb=text(event.body==null?"":event.body,15,TEXT);eb.setPadding(0,dp(8),0,0);evidence.addView(eb);body.addView(evidence);Button open=primary("Open original source");open.setOnClickListener(v->openSource());body.addView(open,new LinearLayout.LayoutParams(-1,dp(48)));if(loopId>0){Button done=secondary("Mark handled");LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(48));p.setMargins(0,dp(9),0,0);done.setOnClickListener(v->{db.closeLoop(loopId);finish();});body.addView(done,p);}TextView note=text("LifeOS keeps this captured evidence so every surfaced item can be traced back to what produced it.",12,MUTED);note.setPadding(0,dp(16),0,0);body.addView(note);setContentView(root);}
    private void openSource(){try{Intent launch=getPackageManager().getLaunchIntentForPackage(event.app);if(launch==null)throw new IllegalStateException();launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(launch);}catch(Exception e){Toast.makeText(this,"This source does not expose a direct Android destination. The captured original is shown here.",Toast.LENGTH_LONG).show();}}
    private LinearLayout row(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(14),dp(14),dp(14));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(18),0,dp(14));c.setLayoutParams(p);c.setBackground(round(SURFACE,BORDER,9));return c;}private Button primary(String s){Button b=secondary(s);b.setBackground(round(BLUE,BLUE,8));b.setTextColor(Color.WHITE);return b;}private Button secondary(String s){Button b=new Button(this);b.setText(s);b.setTextColor(TEXT);b.setTextSize(13);b.setAllCaps(false);b.setBackground(round(SURFACE,BORDER,8));return b;}private Button plain(String s){Button b=secondary(s);b.setTextSize(18);b.setBackgroundColor(Color.TRANSPARENT);return b;}private View divider(){View v=new View(this);v.setBackgroundColor(BORDER);v.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(1)));return v;}private GradientDrawable round(int fill,int stroke,int radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));g.setStroke(dp(1),stroke);return g;}private TextView text(String s,int size,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setLineSpacing(0,1.08f);return v;}private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}private static String app(String p){if(p==null)return "Unknown";int i=p.lastIndexOf('.');String x=i>=0?p.substring(i+1):p;return x.isEmpty()?p:Character.toUpperCase(x.charAt(0))+x.substring(1);}private static String time(long at){return DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(new Date(at));}
}
