package com.kareem.lifeos;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Conversation detail: summary -> what matters -> related person -> evidence -> actions. */
public final class ConversationDetailActivity extends Activity {
    private LifeDb db;private LifeDb.Event seed;private List<LifeDb.Event> events;private String label;
    @Override public void onCreate(Bundle s){super.onCreate(s);db=new LifeDb(this);seed=db.eventById(getIntent().getLongExtra("event_id",0));events=seed==null?new ArrayList<>():db.eventsForThread(seed.app,seed.threadKey,seed.title,160);label=seed==null?"Conversation":LifeDb.personLabel(seed);render();}
    @Override protected void onDestroy(){db.close();super.onDestroy();}

    private void render(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(LifeOsUi.BG);root.addView(LifeOsUi.detailTopBar(this,label));ScrollView sc=new ScrollView(this);sc.setVerticalScrollBarEnabled(false);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(LifeOsUi.dp(this,16),LifeOsUi.dp(this,7),LifeOsUi.dp(this,16),LifeOsUi.dp(this,24));sc.addView(body);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));if(seed==null){TextView none=LifeOsUi.text(this,"Conversation evidence is no longer available.",11.5f,LifeOsUi.MUTED);LifeOsUi.weight(none,500);body.addView(none);setContentView(root);return;}
        TextView eyebrow=LifeOsUi.text(this,"CONVERSATION",9.5f,LifeOsUi.BLUE);LifeOsUi.weight(eyebrow,700);body.addView(eyebrow);TextView h=LifeOsUi.text(this,label,20f,LifeOsUi.TEXT);LifeOsUi.weight(h,700);h.setPadding(0,LifeOsUi.dp(this,4),0,0);body.addView(h);TextView meta=LifeOsUi.text(this,events.size()+" captured items · "+LifeDb.friendlyApp(seed.app),10.2f,LifeOsUi.MUTED);LifeOsUi.weight(meta,500);meta.setPadding(0,LifeOsUi.dp(this,3),0,0);body.addView(meta);
        ProactiveSummaryEngine.Result insight=ProactiveSummaryEngine.summarizeConversation(label,events);
        body.addView(LifeOsUi.section(this,"Summary"));LinearLayout summary=LifeOsUi.card(this);TextView sv=LifeOsUi.text(this,insight.summary,11.6f,LifeOsUi.TEXT);LifeOsUi.weight(sv,500);summary.addView(sv);String signals=ProactiveSummaryEngine.signalLine(insight);if(!signals.isEmpty()){TextView sig=LifeOsUi.text(this,signals.toUpperCase(),9.4f,LifeOsUi.GREEN);LifeOsUi.weight(sig,700);sig.setPadding(0,LifeOsUi.dp(this,6),0,0);summary.addView(sig);}body.addView(summary);
        body.addView(LifeOsUi.section(this,"What matters"));if(!insight.why.isEmpty()){LinearLayout why=LifeOsUi.card(this);TextView wt=LifeOsUi.text(this,insight.why,10.8f,LifeOsUi.TEXT);LifeOsUi.weight(wt,500);why.addView(wt);body.addView(why);}else{TextView calm=LifeOsUi.text(this,"No unresolved signal is confidently grounded in this conversation.",10.5f,LifeOsUi.MUTED);LifeOsUi.weight(calm,500);body.addView(calm);}
        body.addView(LifeOsUi.section(this,"Related"));LinearLayout related=new LinearLayout(this);Button person=LifeOsUi.button(this,"Person");person.setOnClickListener(v->openPerson());related.addView(person,new LinearLayout.LayoutParams(0,LifeOsUi.dp(this,40),1));Button search=LifeOsUi.button(this,"Search related");search.setOnClickListener(v->startActivity(new Intent(this,SearchActivity.class).putExtra("initial_query",label)));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,LifeOsUi.dp(this,40),1);sp.setMargins(LifeOsUi.dp(this,7),0,0,0);related.addView(search,sp);body.addView(related);
        body.addView(LifeOsUi.section(this,"Evidence"));ArrayList<LifeDb.Event> ordered=new ArrayList<>(events);Collections.reverse(ordered);int shown=0;for(LifeDb.Event e:ordered){body.addView(evidenceRow(e));if(++shown>=14)break;}if(ordered.size()>shown){TextView more=LifeOsUi.text(this,(ordered.size()-shown)+" older captured items remain available in LifeOS.",9.7f,LifeOsUi.MUTED);LifeOsUi.weight(more,500);more.setPadding(0,LifeOsUi.dp(this,4),0,0);body.addView(more);}
        body.addView(LifeOsUi.section(this,"Actions"));LinearLayout actions=new LinearLayout(this);Button open=LifeOsUi.button(this,"Open conversation");open.setOnClickListener(v->openSource(seed));actions.addView(open,new LinearLayout.LayoutParams(0,LifeOsUi.dp(this,44),1));Button ask=LifeOsUi.primary(this,"Ask about this");ask.setOnClickListener(v->startActivity(new Intent(this,AskLifeOsActivity.class).putExtra("initial_question","Summarize my conversation with "+label+", tell me what is still open, and what I should do next.")));LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,LifeOsUi.dp(this,44),1);ap.setMargins(LifeOsUi.dp(this,8),0,0,0);actions.addView(ask,ap);body.addView(actions);setContentView(root);
    }

    private View evidenceRow(LifeDb.Event e){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,LifeOsUi.dp(this,6),0,LifeOsUi.dp(this,6));row.addView(LifeOsUi.appIcon(this,e.app,34),new LinearLayout.LayoutParams(LifeOsUi.dp(this,34),LifeOsUi.dp(this,34)));LinearLayout copy=new LinearLayout(this);copy.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(LifeOsUi.dp(this,9),0,LifeOsUi.dp(this,7),0);row.addView(copy,p);TextView t=LifeOsUi.text(this,PresentationSemantics.title(this,e),11.5f,LifeOsUi.TEXT);LifeOsUi.weight(t,600);t.setMaxLines(1);copy.addView(t);TextView s=LifeOsUi.text(this,PresentationSemantics.summary(this,e),9.8f,LifeOsUi.MUTED);LifeOsUi.weight(s,500);s.setMaxLines(2);copy.addView(s);row.addView(LifeOsUi.icon(this,LifeOsIconView.CHEVRON,LifeOsUi.TERTIARY,11),new LinearLayout.LayoutParams(LifeOsUi.dp(this,11),LifeOsUi.dp(this,18)));row.setOnClickListener(v->startActivity(new Intent(this,EvidenceDetailActivity.class).putExtra("event_id",e.id).putExtra("mode","conversation")));return row;}
    private void openPerson(){if(seed!=null)startActivity(new Intent(this,PersonDetailActivity.class).putExtra("event_id",seed.id));}
    private void openSource(LifeDb.Event e){try{String who=LifeDb.personLabel(e);String digits=who.replaceAll("[^0-9+]","");if(e.app!=null&&e.app.toLowerCase().contains("whatsapp")&&digits.matches("\\+?[0-9]{8,}")){startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://wa.me/"+digits.replace("+",""))));return;}Intent i=getPackageManager().getLaunchIntentForPackage(e.app);if(i==null)throw new IllegalStateException();startActivity(i);}catch(Exception ex){Toast.makeText(this,"Android cannot target this exact conversation. LifeOS still keeps the captured context here.",Toast.LENGTH_LONG).show();}}
}
