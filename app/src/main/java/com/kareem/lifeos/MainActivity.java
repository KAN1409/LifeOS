package com.kareem.lifeos;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public final class MainActivity extends Activity {
    private LifeDb db; private LinearLayout content; private TextView status;
    @Override public void onCreate(Bundle b){super.onCreate(b);db=new LifeDb(this);render();}
    @Override protected void onResume(){super.onResume();if(status!=null)refresh();}
    @Override protected void onDestroy(){if(db!=null)db.close();super.onDestroy();}
    private void render(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(28,28,28,28);root.setBackgroundColor(Color.rgb(8,10,12));TextView t=tv("LifeOS",30,Color.WHITE);root.addView(t);root.addView(tv("Understanding Engine M1",13,Color.rgb(184,226,74)));status=tv("",12,Color.LTGRAY);root.addView(status);Button n=button("MANAGE NOTIFICATION ACCESS");n.setOnClickListener(new View.OnClickListener(){public void onClick(View v){startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));}});root.addView(n);Button a=button("MANAGE SCREEN CONTEXT ACCESS");a.setOnClickListener(new View.OnClickListener(){public void onClick(View v){startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));}});root.addView(a);ScrollView s=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);s.addView(content);root.addView(s,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);refresh();}
    private void refresh(){status.setText(db.count("canonical_events")+" understood events · "+db.count("raw_observations")+" raw observations");content.removeAllViews();List<LifeDb.Event> xs=db.recentEvents(80);for(LifeDb.Event e:xs){TextView v=tv((e.direction==null?"?":e.direction)+" · "+(e.title==null?e.app:e.title)+"\n"+(e.body==null?"":e.body)+"\n"+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(e.at)),14,Color.WHITE);v.setPadding(0,14,0,18);content.addView(v);}}
    private Button button(String x){Button b=new Button(this);b.setText(x);return b;} private TextView tv(String x,int s,int c){TextView v=new TextView(this);v.setText(x);v.setTextSize(s);v.setTextColor(c);return v;}
}
