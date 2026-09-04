package com.kareem.lifeos;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Semantic life timeline: meaningful events on a compact rail, never a notification-card dump. */
public final class TimelineActivity extends Activity {
    private LifeDb db;private LinearLayout content;private String filter="All";private final Map<String,Button> chipViews=new LinkedHashMap<>();
    @Override public void onCreate(Bundle s){super.onCreate(s);db=new LifeDb(this);render();}
    @Override protected void onResume(){super.onResume();show();}
    @Override protected void onDestroy(){db.close();super.onDestroy();}

    private void render(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(LifeOsUi.BG);LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);head.setPadding(LifeOsUi.dp(this,16),LifeOsUi.dp(this,9),LifeOsUi.dp(this,10),LifeOsUi.dp(this,7));TextView title=LifeOsUi.text(this,"Timeline",21,LifeOsUi.TEXT);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);head.addView(title,new LinearLayout.LayoutParams(0,-2,1));Button search=LifeOsUi.iconButton(this,"⌕");search.setContentDescription("Search");search.setOnClickListener(v->LifeOsUi.go(this,SearchActivity.class));head.addView(search,new LinearLayout.LayoutParams(LifeOsUi.dp(this,40),LifeOsUi.dp(this,40)));Button filters=LifeOsUi.iconButton(this,"≡");filters.setContentDescription("Timeline filters");head.addView(filters,new LinearLayout.LayoutParams(LifeOsUi.dp(this,40),LifeOsUi.dp(this,40)));root.addView(head);HorizontalScrollView hs=new HorizontalScrollView(this);hs.setHorizontalScrollBarEnabled(false);LinearLayout chips=new LinearLayout(this);chips.setPadding(LifeOsUi.dp(this,16),0,LifeOsUi.dp(this,10),LifeOsUi.dp(this,7));for(String x:new String[]{"All","Messages","Emails","Calls","Events"}){Button b=LifeOsUi.chip(this,x,x.equals(filter));b.setOnClickListener(v->{filter=x;updateChips();show();});chipViews.put(x,b);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,LifeOsUi.dp(this,32));p.setMargins(0,0,LifeOsUi.dp(this,6),0);chips.addView(b,p);}hs.addView(chips);root.addView(hs);ScrollView sc=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(LifeOsUi.dp(this,16),LifeOsUi.dp(this,2),LifeOsUi.dp(this,16),LifeOsUi.dp(this,12));sc.addView(content);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));root.addView(LifeOsUi.bottomNav(this,TimelineActivity.class));setContentView(root);}
    private void updateChips(){for(Map.Entry<String,Button> e:chipViews.entrySet()){boolean on=e.getKey().equals(filter);e.getValue().setBackground(LifeOsUi.round(this,on?LifeOsUi.BLUE:LifeOsUi.SURFACE_2,on?LifeOsUi.BLUE:LifeOsUi.BORDER,18));e.getValue().setTextColor(on?android.graphics.Color.WHITE:LifeOsUi.TEXT);}}
    private void show(){if(content==null)return;content.removeAllViews();List<LifeDb.Event> xs=db.recentEvents(260);String lastDay="";int shown=0;for(LifeDb.Event e:xs){if(!matches(e)||!PresentationSemantics.meaningfulTimeline(this,e))continue;String day=dayKey(e.at);if(!day.equals(lastDay)){TextView h=LifeOsUi.text(this,dayLabel(e.at),12,LifeOsUi.MUTED);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);h.setPadding(0,LifeOsUi.dp(this,12),0,LifeOsUi.dp(this,4));content.addView(h);lastDay=day;}timelineRow(e);shown++;if(shown>=120)break;}if(shown==0){TextView none=LifeOsUi.text(this,"No meaningful activity in this filter yet.",12,LifeOsUi.MUTED);none.setPadding(0,LifeOsUi.dp(this,18),0,0);content.addView(none);}}
    private void timelineRow(LifeDb.Event e){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,LifeOsUi.dp(this,2),0,LifeOsUi.dp(this,2));TextView time=LifeOsUi.text(this,new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date(e.at)),10,LifeOsUi.MUTED);time.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);row.addView(time,new LinearLayout.LayoutParams(LifeOsUi.dp(this,45),LifeOsUi.dp(this,58)));FrameLayout marker=new FrameLayout(this);View line=new View(this);line.setBackgroundColor(LifeOsUi.BORDER);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(LifeOsUi.dp(this,1),-1,Gravity.CENTER_HORIZONTAL);marker.addView(line,lp);TextView dot=LifeOsUi.text(this,"•",20,PresentationSemantics.accent(this,e));dot.setGravity(Gravity.CENTER);marker.addView(dot,new FrameLayout.LayoutParams(-1,-1));row.addView(marker,new LinearLayout.LayoutParams(LifeOsUi.dp(this,25),LifeOsUi.dp(this,58)));View icon=LifeOsUi.appIcon(this,e.app,34);LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(LifeOsUi.dp(this,34),LifeOsUi.dp(this,34));ip.setMargins(0,0,LifeOsUi.dp(this,9),0);row.addView(icon,ip);LinearLayout txt=new LinearLayout(this);txt.setOrientation(LinearLayout.VERTICAL);TextView title=LifeOsUi.text(this,PresentationSemantics.title(this,e)+"  ›",13,LifeOsUi.TEXT);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setMaxLines(1);txt.addView(title);TextView body=LifeOsUi.text(this,PresentationSemantics.summary(this,e),11,LifeOsUi.MUTED);body.setPadding(0,LifeOsUi.dp(this,2),0,0);body.setMaxLines(1);txt.addView(body);row.addView(txt,new LinearLayout.LayoutParams(0,LifeOsUi.dp(this,58),1));row.setOnClickListener(v->startActivity(new Intent(this,EvidenceDetailActivity.class).putExtra("event_id",e.id)));content.addView(row);}
    private boolean matches(LifeDb.Event e){if("All".equals(filter))return true;String kind=PresentationSemantics.kind(this,e);if("Messages".equals(filter))return "Message".equals(kind);if("Emails".equals(filter))return "Email".equals(kind)||LifeDb.friendlyApp(e.app).toLowerCase(Locale.ROOT).contains("gmail");if("Calls".equals(filter))return "Call".equals(kind);return "Events".equals(filter)&&("Event".equals(kind)||"Reminder".equals(kind));}
    private String dayKey(long at){return new SimpleDateFormat("yyyyMMdd",Locale.US).format(new Date(at));}
    private String dayLabel(long at){long now=System.currentTimeMillis(),day=86400000L;String d=new SimpleDateFormat("EEE, d MMM yyyy",Locale.getDefault()).format(new Date(at));String a=dayKey(at),today=dayKey(now),yesterday=dayKey(now-day);if(a.equals(today))return "Today  ·  "+d;if(a.equals(yesterday))return "Yesterday  ·  "+d;return d;}
}
