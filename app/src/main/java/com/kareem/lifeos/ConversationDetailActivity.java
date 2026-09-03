package com.kareem.lifeos;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/** Conversation -> message evidence -> original source. */
public final class ConversationDetailActivity extends Activity {
    private static final int BG=Color.rgb(13,17,23),SURFACE=Color.rgb(22,27,34),BORDER=Color.rgb(48,54,61),TEXT=Color.rgb(230,237,243),MUTED=Color.rgb(139,148,158),GREEN=Color.rgb(63,185,80),BLUE=Color.rgb(47,129,247);
    private LifeDb db; private LifeDb.Event seed; private List<LifeDb.Event> events;
    @Override public void onCreate(Bundle s){super.onCreate(s);db=new LifeDb(this);seed=db.eventById(getIntent().getLongExtra("event_id",0));load();render();}
    @Override protected void onDestroy(){db.close();super.onDestroy();}
    private void load(){events=seed==null?new ArrayList<>():db.eventsForThread(seed.app,seed.threadKey,seed.title,120);}
    private void render(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(dp(12),dp(14),dp(14),dp(12));top.setBackgroundColor(SURFACE);Button back=plain("‹");back.setOnClickListener(v->finish());top.addView(back,new LinearLayout.LayoutParams(dp(48),dp(40)));TextView title=text(seed==null?"Conversation":LifeDb.personLabel(seed),21,TEXT);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setOnClickListener(v->openPerson());top.addView(title,new LinearLayout.LayoutParams(0,-2,1));Button person=plain("Person ›");person.setTextSize(12);person.setOnClickListener(v->openPerson());top.addView(person,new LinearLayout.LayoutParams(dp(82),dp(40)));root.addView(top);root.addView(divider());ScrollView sc=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(16),dp(16),dp(16),dp(28));sc.addView(body);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));if(seed==null){body.addView(text("Conversation evidence is no longer available.",14,MUTED));setContentView(root);return;}
        TextView meta=text(events.size()+" captured items · "+app(seed.app),12,MUTED);body.addView(meta);LinearLayout summary=row();TextView sh=text("Summary",13,GREEN);sh.setTypeface(Typeface.DEFAULT,Typeface.BOLD);summary.addView(sh);TextView sv=text(extractiveSummary(events),15,TEXT);sv.setPadding(0,dp(8),0,0);summary.addView(sv);body.addView(summary);
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);Button open=primary("Open conversation");open.setOnClickListener(v->openSource(seed));actions.addView(open,new LinearLayout.LayoutParams(0,dp(46),1));Button personBtn=secondary("Person history");personBtn.setOnClickListener(v->openPerson());LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(0,dp(46),1);pp.setMargins(dp(8),0,0,0);actions.addView(personBtn,pp);body.addView(actions);
        TextView mh=text("Messages & evidence",18,TEXT);mh.setTypeface(Typeface.DEFAULT,Typeface.BOLD);mh.setPadding(0,dp(22),0,dp(10));body.addView(mh);ArrayList<LifeDb.Event> ordered=new ArrayList<>(events);Collections.reverse(ordered);for(LifeDb.Event e:ordered){LinearLayout card=row();TextView tt=text(displayTitle(e),13,TEXT);tt.setTypeface(Typeface.DEFAULT,Typeface.BOLD);card.addView(tt);TextView b=text(trim(e.body,320),14,TEXT);b.setPadding(0,dp(6),0,0);card.addView(b);card.addView(meta(time(e.at)+" · "+app(e.app)));card.setOnClickListener(v->startActivity(new Intent(this,EvidenceDetailActivity.class).putExtra("event_id",e.id).putExtra("mode","conversation")));body.addView(card);}setContentView(root);}
    private void openPerson(){if(seed!=null)startActivity(new Intent(this,PersonDetailActivity.class).putExtra("event_id",seed.id));}
    private void openSource(LifeDb.Event e){try{String label=LifeDb.personLabel(e);String digits=label.replaceAll("[^0-9+]","");if(e.app!=null&&e.app.toLowerCase().contains("whatsapp")&&digits.matches("\\+?[0-9]{8,}")){String n=digits.replace("+","");startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://wa.me/"+n)));return;}Intent i=getPackageManager().getLaunchIntentForPackage(e.app);if(i==null)throw new IllegalStateException();startActivity(i);}catch(Exception ex){Toast.makeText(this,"Android cannot target this exact conversation. LifeOS still keeps the captured messages here.",Toast.LENGTH_LONG).show();}}
    private static String extractiveSummary(List<LifeDb.Event> xs){if(xs==null||xs.isEmpty())return "No summary yet.";for(LifeDb.Event e:xs){if("Visible conversation".equals(e.title)&&e.body!=null&&e.body.trim().length()>30)return trim(e.body,420);}StringBuilder b=new StringBuilder();for(int i=0;i<Math.min(3,xs.size());i++){String s=trim(xs.get(i).body,140);if(!s.isEmpty()){if(b.length()>0)b.append("  •  ");b.append(s);}}return b.length()==0?"Conversation captured; no readable text summary yet.":b.toString();}
    private static String displayTitle(LifeDb.Event e){String t=e.title==null?"":e.title.trim();return t.isEmpty()||"Visible conversation".equals(t)?"Captured message":t;}
    private static String trim(String s,int n){if(s==null)return "";s=s.trim();return s.length()>n?s.substring(0,n)+"…":s;}
    private LinearLayout row(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(13),dp(14),dp(13));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(10),0,0);c.setLayoutParams(p);c.setBackground(round(SURFACE,BORDER,9));return c;}private TextView meta(String s){TextView v=text(s,11,MUTED);v.setPadding(0,dp(7),0,0);return v;}private Button plain(String s){Button b=new Button(this);b.setText(s);b.setTextColor(TEXT);b.setAllCaps(false);b.setBackgroundColor(Color.TRANSPARENT);return b;}private Button secondary(String s){Button b=plain(s);b.setTextSize(12);b.setBackground(round(SURFACE,BORDER,8));return b;}private Button primary(String s){Button b=secondary(s);b.setTextColor(Color.WHITE);b.setBackground(round(BLUE,BLUE,8));return b;}private View divider(){View v=new View(this);v.setBackgroundColor(BORDER);v.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(1)));return v;}private GradientDrawable round(int fill,int stroke,int radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));g.setStroke(dp(1),stroke);return g;}private TextView text(String s,int size,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setLineSpacing(0,1.08f);return v;}private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}private static String app(String p){if(p==null)return "Unknown";int i=p.lastIndexOf('.');String x=i>=0?p.substring(i+1):p;return x.isEmpty()?p:Character.toUpperCase(x.charAt(0))+x.substring(1);}private static String time(long at){return DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(at));}
}
