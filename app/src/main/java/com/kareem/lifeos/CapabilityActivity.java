package com.kareem.lifeos;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;

/** One reusable browser for the deep capability layer under the four primary tabs. */
public final class CapabilityActivity extends Activity {
    private String capabilityId;
    @Override public void onCreate(Bundle state){super.onCreate(state);capabilityId=getIntent().getStringExtra("capability");if(capabilityId==null)capabilityId="people";render();}

    private void render(){LifeOsCapabilityRegistry.Capability cap=LifeOsCapabilityRegistry.find(this,capabilityId);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(LifeOsUi.BG);root.addView(LifeOsUi.detailTopBar(this,cap.label));
        ScrollView sc=new ScrollView(this);sc.setVerticalScrollBarEnabled(false);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(LifeOsUi.dp(this,16),LifeOsUi.dp(this,6),LifeOsUi.dp(this,16),LifeOsUi.dp(this,24));sc.addView(body);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout hero=LifeOsUi.card(this);LinearLayout hr=new LinearLayout(this);hr.setGravity(Gravity.CENTER_VERTICAL);hr.addView(LifeOsUi.iconTile(this,cap.icon,cap.color,42),new LinearLayout.LayoutParams(LifeOsUi.dp(this,42),LifeOsUi.dp(this,42)));LinearLayout copy=new LinearLayout(this);copy.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-2,1);cp.setMargins(LifeOsUi.dp(this,11),0,0,0);hr.addView(copy,cp);TextView count=LifeOsUi.text(this,cap.primaryLine(),15f,LifeOsUi.TEXT);LifeOsUi.weight(count,700);copy.addView(count);TextView desc=LifeOsUi.text(this,cap.description,10.6f,LifeOsUi.MUTED);LifeOsUi.weight(desc,500);desc.setPadding(0,LifeOsUi.dp(this,2),0,0);copy.addView(desc);if(!cap.secondary.isEmpty()){TextView sec=LifeOsUi.text(this,cap.secondary,9.8f,cap.color);LifeOsUi.weight(sec,500);sec.setPadding(0,LifeOsUi.dp(this,3),0,0);copy.addView(sec);}hero.addView(hr);body.addView(hero);
        body.addView(LifeOsUi.section(this,"Browse"));List<LifeIntelligenceEngine.Result> xs=LifeOsCapabilityRegistry.browse(this,capabilityId,80);if(xs.isEmpty()){TextView none=LifeOsUi.text(this,"No grounded items in this capability yet.",11.5f,LifeOsUi.MUTED);LifeOsUi.weight(none,500);none.setPadding(0,LifeOsUi.dp(this,6),0,0);body.addView(none);}else for(LifeIntelligenceEngine.Result r:xs)body.addView(resultRow(r,cap));
        setContentView(root);
    }

    private View resultRow(LifeIntelligenceEngine.Result r,LifeOsCapabilityRegistry.Capability cap){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,LifeOsUi.dp(this,7),0,LifeOsUi.dp(this,7));row.addView(LifeOsUi.iconTile(this,cap.icon,cap.color,36),new LinearLayout.LayoutParams(LifeOsUi.dp(this,36),LifeOsUi.dp(this,36)));LinearLayout txt=new LinearLayout(this);txt.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,-2,1);tp.setMargins(LifeOsUi.dp(this,10),0,LifeOsUi.dp(this,8),0);row.addView(txt,tp);TextView t=LifeOsUi.text(this,r.title,12.4f,LifeOsUi.TEXT);LifeOsUi.weight(t,600);t.setMaxLines(1);txt.addView(t);if(!r.summary.isEmpty()){TextView s=LifeOsUi.text(this,r.summary,10.3f,LifeOsUi.MUTED);LifeOsUi.weight(s,500);s.setPadding(0,LifeOsUi.dp(this,2),0,0);s.setMaxLines(2);txt.addView(s);}row.addView(LifeOsUi.icon(this,LifeOsIconView.CHEVRON,LifeOsUi.TERTIARY,12),new LinearLayout.LayoutParams(LifeOsUi.dp(this,12),LifeOsUi.dp(this,22)));row.setOnClickListener(v->open(r));return row;}

    private void open(LifeIntelligenceEngine.Result r){if("People".equalsIgnoreCase(r.kind)&&r.eventId>0){startActivity(new Intent(this,PersonDetailActivity.class).putExtra("event_id",r.eventId));return;}if("Conversation".equalsIgnoreCase(r.kind)&&r.eventId>0){startActivity(new Intent(this,ConversationDetailActivity.class).putExtra("event_id",r.eventId));return;}startActivity(EntityDetailActivity.intent(this,r.kind,r.title,r.summary,r.eventId));}
}
