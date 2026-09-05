package com.kareem.lifeos;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.kareem.lifeos.actions.PersistentActionQueue;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Deep list reached from Now gateways; keeps the same visual and semantic language. */
public final class FeedSectionActivity extends Activity {
    private LifeDb db;private LinearLayout content;private String mode;
    @Override public void onCreate(Bundle s){super.onCreate(s);db=new LifeDb(this);mode=getIntent().getStringExtra("mode");render();}
    @Override protected void onResume(){super.onResume();load();}
    @Override protected void onDestroy(){db.close();super.onDestroy();}
    private void render(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(LifeOsUi.BG);root.addView(LifeOsUi.detailTopBar(this,title()));ScrollView sc=new ScrollView(this);sc.setVerticalScrollBarEnabled(false);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(LifeOsUi.dp(this,16),LifeOsUi.dp(this,8),LifeOsUi.dp(this,16),LifeOsUi.dp(this,24));sc.addView(content);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);}
    private String title(){if("attention".equals(mode))return "Needs attention";if("actions".equals(mode))return "Ready actions";if("situations".equals(mode))return "Active situations";return "Today";}

    private void load(){content.removeAllViews();if("attention".equals(mode)){loadAttention();return;}if("actions".equals(mode)){loadActions();return;}if("situations".equals(mode)){loadSituations();return;}loadRecent();}
    private void loadAttention(){List<AttentionStore.Item> durable=AttentionStore.get(this).openItems(120);Set<Long> covered=new HashSet<>();for(AttentionStore.Item x:durable){covered.add(x.eventId);attentionRow(x);}for(LifeDb.Loop l:db.openLoops(120))if(covered.add(l.evidenceId))loopRow(l);if(content.getChildCount()==0)empty("Nothing needs your attention right now.");}
    private void loadActions(){List<PersistentActionQueue.Item> actions=new PersistentActionQueue(this).pending();for(PersistentActionQueue.Item a:actions)actionRow(a);for(AttentionStore.Item a:AttentionStore.get(this).openItems(100))if(!a.provisional&&!a.action.trim().isEmpty())attentionActionRow(a);if(content.getChildCount()==0)empty("No actions are waiting for your approval.");}
    private void loadSituations(){List<PersistentActionQueue.Item> actions=new PersistentActionQueue(this).pending();List<SituationEngine.Situation> xs=SituationEngine.build(db,db.openLoops(160),actions,System.currentTimeMillis());for(SituationEngine.Situation s:xs)situationRow(s);if(xs.isEmpty())empty("No active connected situations right now.");}
    private void loadRecent(){List<LifeDb.Conversation> xs=db.recentConversations(80);for(LifeDb.Conversation c:xs){LinearLayout row=semanticRow(LifeOsIconView.ASK,LifeOsUi.BLUE,c.label,UserFacingText.humanize(c.preview),c.count+" captured items");row.setOnClickListener(v->startActivity(new Intent(this,ConversationDetailActivity.class).putExtra("event_id",c.latestEventId)));content.addView(row);}if(xs.isEmpty())empty("Nothing here right now.");}

    private void attentionRow(AttentionStore.Item a){LifeDb.Event e=db.eventById(a.eventId);String title=e==null?UserFacingText.humanize(a.summary):PresentationSemantics.title(this,e);String summary=e==null?UserFacingText.humanize(a.summary):PresentationSemantics.summary(this,e);LinearLayout row=semanticRow(LifeOsIconView.ALERT,a.provisional?LifeOsUi.AMBER:LifeOsUi.RED,title,summary,a.provisional?"Analyzing":"Needs attention");row.setOnClickListener(v->startActivity(EntityDetailActivity.intent(this,"Attention",title,summary,a.eventId)));content.addView(row);}
    private void loopRow(LifeDb.Loop l){LifeDb.Event e=db.eventById(l.evidenceId);String title=e==null?UserFacingText.humanize(l.title):PresentationSemantics.title(this,e),summary=e==null?UserFacingText.humanize(l.title):PresentationSemantics.summary(this,e);LinearLayout row=semanticRow(LifeOsIconView.COMMITMENT,LifeOsUi.RED,title,summary,human(l.kind));row.setOnClickListener(v->startActivity(EntityDetailActivity.intent(this,"Commitment",title,summary,l.evidenceId)));content.addView(row);}
    private void actionRow(PersistentActionQueue.Item a){String title=UserFacingText.humanize(a.proposal.payloadSummary.isEmpty()?a.proposal.actionType:a.proposal.payloadSummary);LinearLayout row=semanticRow(LifeOsIconView.ACTION,LifeOsUi.GREEN,title,UserFacingText.humanize(a.proposal.target),"Ready for approval");row.setOnClickListener(v->startActivity(new Intent(this,SuggestedActionDetailActivity.class).putExtra("proposal_id",a.proposal.proposalId)));content.addView(row);}
    private void attentionActionRow(AttentionStore.Item a){LifeDb.Event e=db.eventById(a.eventId);String title=e==null?UserFacingText.humanize(a.summary):PresentationSemantics.title(this,e);LinearLayout row=semanticRow(LifeOsIconView.ACTION,LifeOsUi.BLUE,human(a.action)+" · "+title,UserFacingText.humanize(a.reason),"Grounded suggestion");row.setOnClickListener(v->startActivity(new Intent(this,EvidenceDetailActivity.class).putExtra("event_id",a.eventId).putExtra("mode","action")));content.addView(row);}
    private void situationRow(SituationEngine.Situation s){String summary=safeSummary(s);LinearLayout row=semanticRow(LifeOsIconView.ACTIVITY,s.attentionCount>0?LifeOsUi.RED:LifeOsUi.GREEN,UserFacingText.humanize(s.title),summary,s.attentionCount+" open · "+s.actionCount+" actions");row.setOnClickListener(v->startActivity(new Intent(this,SituationDetailActivity.class).putExtra("situation_id",s.id)));content.addView(row);}

    private LinearLayout semanticRow(String icon,int color,String title,String summary,String meta){LinearLayout row=LifeOsUi.card(this);LinearLayout main=new LinearLayout(this);main.setGravity(Gravity.CENTER_VERTICAL);main.addView(LifeOsUi.iconTile(this,icon,color,38),new LinearLayout.LayoutParams(LifeOsUi.dp(this,38),LifeOsUi.dp(this,38)));LinearLayout copy=new LinearLayout(this);copy.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(LifeOsUi.dp(this,10),0,LifeOsUi.dp(this,7),0);main.addView(copy,p);TextView t=LifeOsUi.text(this,title,12.6f,LifeOsUi.TEXT);LifeOsUi.weight(t,700);t.setMaxLines(1);copy.addView(t);if(summary!=null&&!summary.trim().isEmpty()){TextView s=LifeOsUi.text(this,summary,10.3f,LifeOsUi.MUTED);LifeOsUi.weight(s,500);s.setPadding(0,LifeOsUi.dp(this,2),0,0);s.setMaxLines(2);copy.addView(s);}if(meta!=null&&!meta.trim().isEmpty()){TextView m=LifeOsUi.text(this,meta,9.3f,color);LifeOsUi.weight(m,600);m.setPadding(0,LifeOsUi.dp(this,3),0,0);copy.addView(m);}main.addView(LifeOsUi.icon(this,LifeOsIconView.CHEVRON,LifeOsUi.TERTIARY,12),new LinearLayout.LayoutParams(LifeOsUi.dp(this,12),LifeOsUi.dp(this,18)));row.addView(main);return row;}
    private String safeSummary(SituationEngine.Situation s){String raw=UserFacingText.humanize(s.summary);if(raw.matches(".*\\d{8,}.*")||raw.startsWith("Request ·")||raw.length()>180)return UserFacingText.humanize(s.why);return raw;}
    private void empty(String s){TextView v=LifeOsUi.text(this,s,11.2f,LifeOsUi.MUTED);LifeOsUi.weight(v,500);v.setPadding(LifeOsUi.dp(this,2),LifeOsUi.dp(this,8),0,0);content.addView(v);}
    private static String human(String v){String x=v==null?"":v.replace("brain_","").replace('_',' ').trim().toLowerCase();return x.isEmpty()?"Open item":Character.toUpperCase(x.charAt(0))+x.substring(1);}
}
