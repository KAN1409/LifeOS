package com.kareem.lifeos;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;

/** Deep browser backed only by registered functional providers. */
public final class CapabilityActivity extends Activity {
    private static final int PERMISSION=91;private String capabilityId;
    @Override public void onCreate(Bundle state){super.onCreate(state);capabilityId=getIntent().getStringExtra("capability");if(capabilityId==null)capabilityId="conversations";render();}
    @Override protected void onResume(){super.onResume();if(capabilityId!=null)render();}

    private void render(){FunctionalCapabilityRegistry.Capability cap=FunctionalCapabilityRegistry.find(this,capabilityId);String title=cap==null?"LifeOS":cap.label;LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(LifeOsUi.BG);root.addView(LifeOsUi.detailTopBar(this,title));ScrollView sc=new ScrollView(this);sc.setVerticalScrollBarEnabled(false);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(LifeOsUi.dp(this,16),LifeOsUi.dp(this,6),LifeOsUi.dp(this,16),LifeOsUi.dp(this,24));sc.addView(body);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));if(cap==null){empty(body,"This capability is not registered as operational.");setContentView(root);return;}
        LinearLayout hero=LifeOsUi.card(this);LinearLayout hr=new LinearLayout(this);hr.setGravity(Gravity.CENTER_VERTICAL);hr.addView(LifeOsUi.iconTile(this,cap.icon,cap.color,42),new LinearLayout.LayoutParams(LifeOsUi.dp(this,42),LifeOsUi.dp(this,42)));LinearLayout copy=new LinearLayout(this);copy.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-2,1);cp.setMargins(LifeOsUi.dp(this,11),0,0,0);hr.addView(copy,cp);TextView count=LifeOsUi.text(this,cap.countLine(),15f,LifeOsUi.TEXT);LifeOsUi.weight(count,700);copy.addView(count);TextView desc=LifeOsUi.text(this,cap.description,10.6f,LifeOsUi.MUTED);LifeOsUi.weight(desc,500);desc.setPadding(0,LifeOsUi.dp(this,2),0,0);copy.addView(desc);TextView sec=LifeOsUi.text(this,cap.status,9.8f,cap.availability==FunctionalCapabilityRegistry.Availability.OPERATIONAL?cap.color:LifeOsUi.AMBER);LifeOsUi.weight(sec,500);sec.setPadding(0,LifeOsUi.dp(this,3),0,0);copy.addView(sec);hero.addView(hr);body.addView(hero);
        if(cap.availability==FunctionalCapabilityRegistry.Availability.SETUP_REQUIRED){body.addView(LifeOsUi.section(this,"Set up"));LinearLayout setup=LifeOsUi.card(this);TextView info=LifeOsUi.text(this,setupText(),11f,LifeOsUi.TEXT);LifeOsUi.weight(info,500);setup.addView(info);Button enable=LifeOsUi.primary(this,"Allow access");enable.setOnClickListener(v->requestSetupPermission());LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,LifeOsUi.dp(this,44));bp.topMargin=LifeOsUi.dp(this,9);setup.addView(enable,bp);body.addView(setup);setContentView(root);return;}
        body.addView(LifeOsUi.section(this,"Browse"));List<FunctionalCapabilityRegistry.ObjectItem> xs=FunctionalCapabilityRegistry.list(this,capabilityId,120);if(xs.isEmpty())empty(body,"No real objects are available from this source yet.");else for(FunctionalCapabilityRegistry.ObjectItem r:xs)body.addView(resultRow(r,cap));setContentView(root);
    }

    private View resultRow(FunctionalCapabilityRegistry.ObjectItem r,FunctionalCapabilityRegistry.Capability cap){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,LifeOsUi.dp(this,7),0,LifeOsUi.dp(this,7));row.addView(LifeOsUi.iconTile(this,cap.icon,cap.color,36),new LinearLayout.LayoutParams(LifeOsUi.dp(this,36),LifeOsUi.dp(this,36)));LinearLayout txt=new LinearLayout(this);txt.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,-2,1);tp.setMargins(LifeOsUi.dp(this,10),0,LifeOsUi.dp(this,8),0);row.addView(txt,tp);TextView t=LifeOsUi.text(this,r.title,12.4f,LifeOsUi.TEXT);LifeOsUi.weight(t,600);t.setMaxLines(1);txt.addView(t);if(!r.summary.isEmpty()){TextView s=LifeOsUi.text(this,r.summary,10.3f,LifeOsUi.MUTED);LifeOsUi.weight(s,500);s.setPadding(0,LifeOsUi.dp(this,2),0,0);s.setMaxLines(2);txt.addView(s);}if(!r.meta.isEmpty()){TextView m=LifeOsUi.text(this,r.meta,9.2f,cap.color);LifeOsUi.weight(m,500);m.setPadding(0,LifeOsUi.dp(this,2),0,0);m.setMaxLines(1);txt.addView(m);}row.addView(LifeOsUi.icon(this,LifeOsIconView.CHEVRON,LifeOsUi.TERTIARY,12),new LinearLayout.LayoutParams(LifeOsUi.dp(this,12),LifeOsUi.dp(this,22)));row.setOnClickListener(v->open(r));return row;}
    private void open(FunctionalCapabilityRegistry.ObjectItem r){if("conversations".equals(r.capabilityId)){startActivity(new Intent(this,ConversationDetailActivity.class).putExtra("conversation_id",r.objectId));return;}startActivity(new Intent(this,FunctionalObjectDetailActivity.class).putExtra("capability_id",r.capabilityId).putExtra("object_id",r.objectId));}
    private String setupText(){if("people".equals(capabilityId))return "People is backed by Android Contacts. LifeOS will show only contacts Android allows it to read.";if("events".equals(capabilityId))return "Events is backed by Android Calendar. LifeOS will read real calendar events instead of guessing from notification text.";return "This source needs Android permission before LifeOS can expose it.";}
    private void requestSetupPermission(){if("people".equals(capabilityId))requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},PERMISSION);else if("events".equals(capabilityId))requestPermissions(new String[]{Manifest.permission.READ_CALENDAR},PERMISSION);}
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==PERMISSION)render();}
    private void empty(LinearLayout body,String text){TextView none=LifeOsUi.text(this,text,11.5f,LifeOsUi.MUTED);LifeOsUi.weight(none,500);none.setPadding(0,LifeOsUi.dp(this,8),0,0);body.addView(none);}
}
