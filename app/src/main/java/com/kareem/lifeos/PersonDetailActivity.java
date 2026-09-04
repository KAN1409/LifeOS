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
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public final class PersonDetailActivity extends Activity {
    private static final int BG=Color.rgb(13,17,23),SURFACE=Color.rgb(22,27,34),BORDER=Color.rgb(48,54,61),TEXT=Color.rgb(230,237,243),MUTED=Color.rgb(139,148,158),GREEN=Color.rgb(63,185,80);
    private LifeDb db;private LifeDb.Event seed;private String label;private List<LifeDb.Event> events;
    @Override public void onCreate(Bundle s){super.onCreate(s);db=new LifeDb(this);seed=db.eventById(getIntent().getLongExtra("event_id",0));label=seed==null?"Person":LifeDb.personLabel(seed);events=seed==null?java.util.Collections.emptyList():db.eventsForPerson(seed.app,label,160);render();}
    @Override protected void onDestroy(){db.close();super.onDestroy();}
    private void render(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(dp(12),dp(10),dp(16),dp(8));top.setBackgroundColor(SURFACE);Button back=plain("‹");back.setContentDescription("Back");back.setOnClickListener(v->finish());top.addView(back,new LinearLayout.LayoutParams(dp(48),dp(48)));TextView title=text(label,22,TEXT);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);top.addView(title);root.addView(top);root.addView(divider());ScrollView sc=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(16),dp(16),dp(16),dp(28));sc.addView(body);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));if(seed==null){body.addView(text("No person evidence available.",14,MUTED));setContentView(root);return;}
        LinearLayout overview=row();TextView o=text("Overview",13,GREEN);o.setTypeface(Typeface.DEFAULT,Typeface.BOLD);overview.addView(o);TextView stats=text(events.size()+" captured interactions\nLatest: "+(events.isEmpty()?"—":time(events.get(0).at))+"\nSource: "+app(seed.app),15,TEXT);stats.setPadding(0,dp(8),0,0);overview.addView(stats);body.addView(overview);
        TextView ch=text("Conversations",18,TEXT);ch.setTypeface(Typeface.DEFAULT,Typeface.BOLD);ch.setPadding(0,dp(22),0,dp(8));body.addView(ch);if(!events.isEmpty()){LifeDb.Event latest=events.get(0);LinearLayout c=row();TextView t=text("Latest conversation  ›",15,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);c.addView(t);c.addView(text(trim(latest.body,260),13,TEXT));c.addView(meta(time(latest.at)));c.setOnClickListener(v->startActivity(new Intent(this,ConversationDetailActivity.class).putExtra("event_id",latest.id)));body.addView(c);}
        TextView th=text("Timeline",18,TEXT);th.setTypeface(Typeface.DEFAULT,Typeface.BOLD);th.setPadding(0,dp(22),0,dp(8));body.addView(th);for(LifeDb.Event e:events){LinearLayout c=row();TextView t=text(e.title==null||e.title.isEmpty()?"Interaction":e.title,14,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);c.addView(t);c.addView(text(trim(e.body,220),13,TEXT));c.addView(meta(time(e.at)+" · "+app(e.app)));c.setOnClickListener(v->startActivity(new Intent(this,EvidenceDetailActivity.class).putExtra("event_id",e.id).putExtra("mode","person")));body.addView(c);}setContentView(root);}
    private static String trim(String s,int n){if(s==null)return "";s=s.trim();return s.length()>n?s.substring(0,n)+"…":s;}
    private LinearLayout row(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(13),dp(14),dp(13));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(8),0,0);c.setLayoutParams(p);c.setBackground(round(SURFACE,BORDER,9));return c;}private TextView meta(String s){TextView v=text(s,11,MUTED);v.setPadding(0,dp(7),0,0);return v;}private Button plain(String s){Button b=new Button(this);b.setText(s);b.setTextColor(TEXT);b.setTextSize(18);b.setAllCaps(false);b.setBackgroundColor(Color.TRANSPARENT);return b;}private View divider(){View v=new View(this);v.setBackgroundColor(BORDER);v.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(1)));return v;}private GradientDrawable round(int fill,int stroke,int radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));g.setStroke(dp(1),stroke);return g;}private TextView text(String s,int size,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setLineSpacing(0,1.08f);return v;}private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}private static String app(String p){if(p==null)return "Unknown";int i=p.lastIndexOf('.');String x=i>=0?p.substring(i+1):p;return x.isEmpty()?p:Character.toUpperCase(x.charAt(0))+x.substring(1);}private static String time(long at){return DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(at));}
}
