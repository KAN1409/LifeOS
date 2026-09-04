package com.kareem.lifeos;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class TimelineActivity extends Activity {
    private LifeDb db;private LinearLayout content;private String filter="All";
    @Override public void onCreate(Bundle s){super.onCreate(s);db=new LifeDb(this);render();}
    @Override protected void onResume(){super.onResume();show();}
    @Override protected void onDestroy(){db.close();super.onDestroy();}
    private void render(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(LifeOsUi.BG);root.addView(LifeOsUi.topBar(this,"Timeline",true));LinearLayout chips=new LinearLayout(this);chips.setPadding(LifeOsUi.dp(this,12),0,LifeOsUi.dp(this,12),LifeOsUi.dp(this,8));for(String x:new String[]{"All","Messages","Emails","Calls","Events"}){Button b=LifeOsUi.button(this,x);b.setTextSize(10);b.setOnClickListener(v->{filter=x;show();});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,LifeOsUi.dp(this,38),1);p.setMargins(LifeOsUi.dp(this,3),0,LifeOsUi.dp(this,3),0);chips.addView(b,p);}root.addView(chips);ScrollView sc=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(LifeOsUi.dp(this,16),LifeOsUi.dp(this,6),LifeOsUi.dp(this,16),LifeOsUi.dp(this,18));sc.addView(content);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));root.addView(LifeOsUi.bottomNav(this,TimelineActivity.class));setContentView(root);}
    private void show(){if(content==null)return;content.removeAllViews();List<LifeDb.Event> xs=db.recentEvents(180);String lastDay="";for(LifeDb.Event e:xs){if(!matches(e))continue;String day=new SimpleDateFormat("EEE, d MMM yyyy",Locale.getDefault()).format(new Date(e.at));if(!day.equals(lastDay)){TextView h=LifeOsUi.text(this,day,13,LifeOsUi.MUTED);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);h.setPadding(0,LifeOsUi.dp(this,12),0,LifeOsUi.dp(this,8));content.addView(h);lastDay=day;}LinearLayout r=LifeOsUi.card(this);LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);TextView time=LifeOsUi.text(this,new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date(e.at)),11,LifeOsUi.MUTED);line.addView(time,new LinearLayout.LayoutParams(LifeOsUi.dp(this,52),-2));TextView title=LifeOsUi.text(this,(e.title==null||e.title.trim().isEmpty()?LifeDb.friendlyApp(e.app):e.title)+"  ›",14,LifeOsUi.TEXT);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);line.addView(title,new LinearLayout.LayoutParams(0,-2,1));r.addView(line);TextView body=LifeOsUi.text(this,UserFacingText.humanize(clip(e.body,170)),12,LifeOsUi.MUTED);body.setPadding(LifeOsUi.dp(this,52),LifeOsUi.dp(this,5),0,0);body.setMaxLines(2);r.addView(body);r.setOnClickListener(v->startActivity(new Intent(this,EvidenceDetailActivity.class).putExtra("event_id",e.id)));content.addView(r);}if(content.getChildCount()==0)content.addView(LifeOsUi.text(this,"Nothing captured in this filter yet.",14,LifeOsUi.MUTED));}
    private boolean matches(LifeDb.Event e){if("All".equals(filter))return true;String p=(e.app==null?"":e.app).toLowerCase(Locale.ROOT),t=(e.title+" "+e.body).toLowerCase(Locale.ROOT);if("Emails".equals(filter))return p.contains("gm")||p.contains("mail");if("Messages".equals(filter))return p.contains("whatsapp")||p.contains("telegram")||p.contains("messenger")||p.contains("signal");if("Calls".equals(filter))return p.contains("dialer")||p.contains("phone")||t.contains("call");return "Events".equals(filter)&&(p.contains("calendar")||t.contains("meeting")||t.contains("appointment"));}
    private static String clip(String x,int n){String v=x==null?"":x.trim();return v.length()>n?v.substring(0,n)+"…":v;}
}
