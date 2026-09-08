package com.kareem.lifeos;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.kareem.lifeos.actions.PersistentActionQueue;
import java.util.List;

/** Deep lists from Now. Only canonical or explicitly real sources are rendered. */
public final class FeedSectionActivity extends Activity {
    private LinearLayout content;private String mode;
    @Override public void onCreate(Bundle s){super.onCreate(s);mode=getIntent().getStringExtra("mode");render();}
    @Override protected void onResume(){super.onResume();load();}
    private void render(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(LifeOsUi.BG);root.addView(LifeOsUi.detailTopBar(this,title()));ScrollView sc=new ScrollView(this);sc.setVerticalScrollBarEnabled(false);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(LifeOsUi.dp(this,16),LifeOsUi.dp(this,8),LifeOsUi.dp(this,16),LifeOsUi.dp(this,24));sc.addView(content);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);}
    private String title(){if("attention".equals(mode))return "Needs attention";if("actions".equals(mode))return "Ready actions";if("situations".equals(mode))return "Active situations";return "Recent conversations";}
    private void load(){content.removeAllViews();if("attention".equals(mode)){loadAttention();return;}if("actions".equals(mode)){loadActions();return;}if("situations".equals(mode)){empty("Canonical situations are intentionally hidden until the V2 Life Model becomes the product source of truth.");return;}loadRecent();}
    private void loadAttention(){List<ObligationRepository.ObligationObject> xs=ObligationRepository.open(this,150);for(ObligationRepository.ObligationObject x:xs){LinearLayout row=semanticRow(LifeOsIconView.COMMITMENT,LifeOsUi.RED,x.title,x.summary,human(x.action));row.setOnClickListener(v->startActivity(new Intent(this,FunctionalObjectDetailActivity.class).putExtra("capability_id","commitments").putExtra("object_id",x.id)));content.addView(row);}if(xs.isEmpty())empty("Nothing is canonically waiting on you right now.");}
    private void loadActions(){List<PersistentActionQueue.Item> actions=new PersistentActionQueue(this).pending();for(PersistentActionQueue.Item a:actions){String title=UserFacingText.humanize(a.proposal.payloadSummary.isEmpty()?a.proposal.actionType:a.proposal.payloadSummary);LinearLayout row=semanticRow(LifeOsIconView.ACTION,LifeOsUi.GREEN,title,UserFacingText.humanize(a.proposal.target),"Ready for approval");row.setOnClickListener(v->startActivity(new Intent(this,SuggestedActionDetailActivity.class).putExtra("proposal_id",a.proposal.proposalId)));content.addView(row);}if(actions.isEmpty())empty("No real action proposals are waiting for your approval.");}
    private void loadRecent(){List<ConversationRepository.ConversationObject> xs=ConversationRepository.list(this,80);for(ConversationRepository.ConversationObject c:xs){LinearLayout row=semanticRow(LifeOsIconView.ASK,LifeOsUi.BLUE,c.label,c.preview,c.capturedCount+" captured items");row.setOnClickListener(v->startActivity(new Intent(this,ConversationDetailActivity.class).putExtra("conversation_id",c.id)));content.addView(row);}if(xs.isEmpty())empty("No persisted conversations are available yet.");}
    private LinearLayout semanticRow(String icon,int color,String title,String summary,String meta){LinearLayout row=LifeOsUi.card(this);LinearLayout main=new LinearLayout(this);main.setGravity(Gravity.CENTER_VERTICAL);main.addView(LifeOsUi.iconTile(this,icon,color,38),new LinearLayout.LayoutParams(LifeOsUi.dp(this,38),LifeOsUi.dp(this,38)));LinearLayout copy=new LinearLayout(this);copy.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(LifeOsUi.dp(this,10),0,LifeOsUi.dp(this,7),0);main.addView(copy,p);TextView t=LifeOsUi.text(this,title,12.6f,LifeOsUi.TEXT);LifeOsUi.weight(t,700);t.setMaxLines(1);copy.addView(t);if(summary!=null&&!summary.trim().isEmpty()){TextView s=LifeOsUi.text(this,summary,10.3f,LifeOsUi.MUTED);LifeOsUi.weight(s,500);s.setPadding(0,LifeOsUi.dp(this,2),0,0);s.setMaxLines(2);copy.addView(s);}if(meta!=null&&!meta.trim().isEmpty()){TextView m=LifeOsUi.text(this,meta,9.3f,color);LifeOsUi.weight(m,600);m.setPadding(0,LifeOsUi.dp(this,3),0,0);copy.addView(m);}main.addView(LifeOsUi.icon(this,LifeOsIconView.CHEVRON,LifeOsUi.TERTIARY,12),new LinearLayout.LayoutParams(LifeOsUi.dp(this,12),LifeOsUi.dp(this,18)));row.addView(main);return row;}
    private void empty(String s){TextView v=LifeOsUi.text(this,s,11.2f,LifeOsUi.MUTED);LifeOsUi.weight(v,500);v.setPadding(LifeOsUi.dp(this,2),LifeOsUi.dp(this,8),0,0);content.addView(v);}
    private static String human(String v){String x=v==null?"":v.replace('_',' ').trim().toLowerCase();return x.isEmpty()||"none".equals(x)?"Needs attention":Character.toUpperCase(x.charAt(0))+x.substring(1);}
}
