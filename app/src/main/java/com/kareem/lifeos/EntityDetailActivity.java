package com.kareem.lifeos;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Legacy compatibility detail. It no longer pretends copied strings are a canonical entity.
 * New functional routes use FunctionalObjectDetailActivity with typed stable IDs.
 */
public final class EntityDetailActivity extends Activity {
    private String kind,title,summary;private long eventId;
    static Intent intent(Context c,String kind,String title,String summary,long eventId){return new Intent(c,EntityDetailActivity.class).putExtra("kind",kind).putExtra("title",title).putExtra("summary",summary).putExtra("event_id",eventId);}
    @Override public void onCreate(Bundle state){super.onCreate(state);Intent i=getIntent();kind=s(i.getStringExtra("kind"));title=s(i.getStringExtra("title"));summary=s(i.getStringExtra("summary"));eventId=i.getLongExtra("event_id",0);if(title.isEmpty())title=kind.isEmpty()?"Captured item":kind;render();}
    private void render(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(LifeOsUi.BG);root.addView(LifeOsUi.detailTopBar(this,title));ScrollView sc=new ScrollView(this);sc.setVerticalScrollBarEnabled(false);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(LifeOsUi.dp(this,16),LifeOsUi.dp(this,7),LifeOsUi.dp(this,16),LifeOsUi.dp(this,24));sc.addView(body);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));TextView eyebrow=LifeOsUi.text(this,"CAPTURED EVIDENCE",9.5f,LifeOsUi.BLUE);LifeOsUi.weight(eyebrow,700);body.addView(eyebrow);TextView h=LifeOsUi.text(this,title,19.5f,LifeOsUi.TEXT);LifeOsUi.weight(h,700);h.setPadding(0,LifeOsUi.dp(this,5),0,0);body.addView(h);body.addView(LifeOsUi.section(this,"Summary"));LinearLayout summaryCard=LifeOsUi.card(this);TextView sv=LifeOsUi.text(this,summary.isEmpty()?"This legacy item has no canonical typed summary.":summary,11.6f,LifeOsUi.TEXT);LifeOsUi.weight(sv,500);summaryCard.addView(sv);body.addView(summaryCard);body.addView(LifeOsUi.section(this,"Evidence"));if(eventId>0){Button evidence=LifeOsUi.button(this,"Open source evidence");evidence.setOnClickListener(v->startActivity(new Intent(this,EvidenceDetailActivity.class).putExtra("event_id",eventId).putExtra("mode","legacy")));body.addView(evidence,new LinearLayout.LayoutParams(-1,LifeOsUi.dp(this,44)));}else{TextView no=LifeOsUi.text(this,"No stable backing object or evidence ID is attached. LifeOS will not infer relationships or actions from this copied text.",10.5f,LifeOsUi.MUTED);LifeOsUi.weight(no,500);body.addView(no);}body.addView(LifeOsUi.section(this,"Next"));Button search=LifeOsUi.primary(this,"Search connected LifeOS");search.setOnClickListener(v->startActivity(new Intent(this,SearchActivity.class).putExtra("initial_query",title)));body.addView(search,new LinearLayout.LayoutParams(-1,LifeOsUi.dp(this,44)));setContentView(root);}
    private static String s(String x){return x==null?"":x.trim();}
}
