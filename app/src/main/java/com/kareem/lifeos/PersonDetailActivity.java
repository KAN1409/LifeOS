package com.kareem.lifeos;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.kareem.lifeos.memory.MemoryRecord;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Person detail uses the same connected-context grammar as every other LifeOS entity. */
public final class PersonDetailActivity extends Activity {
    private LifeDb db;private LifeDb.Event seed;private String label;private List<LifeDb.Event> events;private List<MemoryRecord> memories;
    @Override public void onCreate(Bundle s){super.onCreate(s);db=new LifeDb(this);seed=db.eventById(getIntent().getLongExtra("event_id",0));label=seed==null?"Person":LifeDb.personLabel(seed);events=seed==null?new ArrayList<>():db.eventsForPerson(seed.app,label,180);memories=seed==null?new ArrayList<>():LocalGroundedMemory.memoriesFor(this,seed,16);render();}
    @Override protected void onDestroy(){db.close();super.onDestroy();}

    private void render(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(LifeOsUi.BG);root.addView(LifeOsUi.detailTopBar(this,label));ScrollView sc=new ScrollView(this);sc.setVerticalScrollBarEnabled(false);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(LifeOsUi.dp(this,16),LifeOsUi.dp(this,7),LifeOsUi.dp(this,16),LifeOsUi.dp(this,24));sc.addView(body);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));if(seed==null){TextView none=LifeOsUi.text(this,"No grounded person evidence is available.",11.5f,LifeOsUi.MUTED);LifeOsUi.weight(none,500);body.addView(none);setContentView(root);return;}
        TextView eyebrow=LifeOsUi.text(this,"PERSON",9.5f,LifeOsUi.PURPLE);LifeOsUi.weight(eyebrow,700);body.addView(eyebrow);TextView hero=LifeOsUi.text(this,label,20f,LifeOsUi.TEXT);LifeOsUi.weight(hero,700);hero.setPadding(0,LifeOsUi.dp(this,4),0,0);body.addView(hero);String latest=events.isEmpty()?"No recent interaction":DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new java.util.Date(events.get(0).at));TextView meta=LifeOsUi.text(this,"Latest interaction · "+latest+" · "+LifeDb.friendlyApp(seed.app),10.2f,LifeOsUi.MUTED);LifeOsUi.weight(meta,500);meta.setPadding(0,LifeOsUi.dp(this,3),0,0);body.addView(meta);
        ProactiveSummaryEngine.Result insight=ProactiveSummaryEngine.summarizeConversation(label,events);
        body.addView(LifeOsUi.section(this,"Summary"));LinearLayout summaryCard=LifeOsUi.card(this);TextView sv=LifeOsUi.text(this,insight.summary,11.6f,LifeOsUi.TEXT);LifeOsUi.weight(sv,500);summaryCard.addView(sv);if(!insight.why.isEmpty()){TextView why=LifeOsUi.text(this,insight.why,10f,LifeOsUi.PURPLE);LifeOsUi.weight(why,600);why.setPadding(0,LifeOsUi.dp(this,6),0,0);summaryCard.addView(why);}body.addView(summaryCard);
        body.addView(LifeOsUi.section(this,"What matters"));body.addView(statsRow());
        if(!memories.isEmpty()){for(int i=0;i<Math.min(4,memories.size());i++)body.addView(memoryRow(memories.get(i)));}
        body.addView(LifeOsUi.section(this,"Related"));LinearLayout related=new LinearLayout(this);Button conversations=LifeOsUi.button(this,"Conversation");conversations.setOnClickListener(v->openConversation());related.addView(conversations,new LinearLayout.LayoutParams(0,LifeOsUi.dp(this,40),1));Button search=LifeOsUi.button(this,"Search related");search.setOnClickListener(v->startActivity(new Intent(this,SearchActivity.class).putExtra("initial_query",label)));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,LifeOsUi.dp(this,40),1);rp.setMargins(LifeOsUi.dp(this,7),0,0,0);related.addView(search,rp);body.addView(related);
        body.addView(LifeOsUi.section(this,"Evidence"));int shown=0;for(LifeDb.Event e:events){if(!EventSemantics.isPersonConversation(e))continue;body.addView(evidenceRow(e));if(++shown>=8)break;}if(shown==0){TextView no=LifeOsUi.text(this,"No readable interaction evidence yet.",10.5f,LifeOsUi.MUTED);LifeOsUi.weight(no,500);body.addView(no);}
        body.addView(LifeOsUi.section(this,"Actions"));Button ask=LifeOsUi.primary(this,"Ask about "+label);ask.setOnClickListener(v->startActivity(new Intent(this,AskLifeOsActivity.class).putExtra("initial_question","Catch me up on "+label+". What matters, what is open, and what should I do next?")));body.addView(ask,new LinearLayout.LayoutParams(-1,LifeOsUi.dp(this,44)));setContentView(root);
    }

    private View statsRow(){int open=0;Set<Long> eventIds=new HashSet<>();for(LifeDb.Event e:events)eventIds.add(e.id);for(LifeDb.Loop l:db.openLoops(300))if(eventIds.contains(l.evidenceId))open++;LinearLayout row=new LinearLayout(this);row.setPadding(0,0,0,LifeOsUi.dp(this,3));stat(row,String.valueOf(events.size()),"Interactions",LifeOsIconView.TIMELINE,LifeOsUi.BLUE);stat(row,String.valueOf(memories.size()),"Memories",LifeOsIconView.COMMITMENT,LifeOsUi.GREEN);stat(row,String.valueOf(open),"Open items",LifeOsIconView.ALERT,open>0?LifeOsUi.RED:LifeOsUi.TERTIARY);return row;}
    private void stat(LinearLayout row,String value,String label,String icon,int color){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(LifeOsUi.dp(this,8),LifeOsUi.dp(this,7),LifeOsUi.dp(this,6),LifeOsUi.dp(this,6));c.setBackground(LifeOsUi.round(this,LifeOsUi.SURFACE,LifeOsUi.BORDER,12));LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.addView(LifeOsUi.icon(this,icon,color,12),new LinearLayout.LayoutParams(LifeOsUi.dp(this,12),LifeOsUi.dp(this,12)));TextView n=LifeOsUi.text(this,value,14f,color);LifeOsUi.weight(n,700);LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-2,-2);np.leftMargin=LifeOsUi.dp(this,5);top.addView(n,np);c.addView(top);TextView l=LifeOsUi.text(this,label,8.9f,LifeOsUi.MUTED);LifeOsUi.weight(l,500);l.setPadding(0,LifeOsUi.dp(this,3),0,0);c.addView(l);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,LifeOsUi.dp(this,52),1);p.setMargins(0,0,LifeOsUi.dp(this,6),0);row.addView(c,p);}
    private View memoryRow(MemoryRecord m){LinearLayout c=LifeOsUi.card(this);c.addView(LifeOsUi.badge(this,m.category.name(),LifeOsUi.GREEN));TextView value=LifeOsUi.text(this,trim(m.text,260),10.7f,LifeOsUi.TEXT);LifeOsUi.weight(value,500);value.setPadding(0,LifeOsUi.dp(this,5),0,0);c.addView(value);long evidence=firstEvidence(m);if(evidence>0)c.setOnClickListener(v->startActivity(new Intent(this,EvidenceDetailActivity.class).putExtra("event_id",evidence).putExtra("mode","memory")));return c;}
    private View evidenceRow(LifeDb.Event e){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,LifeOsUi.dp(this,6),0,LifeOsUi.dp(this,6));row.addView(LifeOsUi.appIcon(this,e.app,34),new LinearLayout.LayoutParams(LifeOsUi.dp(this,34),LifeOsUi.dp(this,34)));LinearLayout copy=new LinearLayout(this);copy.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(LifeOsUi.dp(this,9),0,LifeOsUi.dp(this,7),0);row.addView(copy,p);TextView t=LifeOsUi.text(this,PresentationSemantics.title(this,e),11.5f,LifeOsUi.TEXT);LifeOsUi.weight(t,600);t.setMaxLines(1);copy.addView(t);TextView s=LifeOsUi.text(this,PresentationSemantics.summary(this,e),9.8f,LifeOsUi.MUTED);LifeOsUi.weight(s,500);s.setMaxLines(1);copy.addView(s);row.addView(LifeOsUi.icon(this,LifeOsIconView.CHEVRON,LifeOsUi.TERTIARY,11),new LinearLayout.LayoutParams(LifeOsUi.dp(this,11),LifeOsUi.dp(this,18)));row.setOnClickListener(v->startActivity(new Intent(this,EvidenceDetailActivity.class).putExtra("event_id",e.id).putExtra("mode","person")));return row;}
    private void openConversation(){if(!events.isEmpty())startActivity(new Intent(this,ConversationDetailActivity.class).putExtra("event_id",events.get(0).id));}
    private static long firstEvidence(MemoryRecord m){if(m==null||m.evidenceIds.isEmpty())return 0;try{return Long.parseLong(m.evidenceIds.get(0));}catch(Exception e){return 0;}}
    private static String trim(String s,int n){String x=s==null?"":s.trim();return x.length()>n?x.substring(0,n)+"…":x;}
}
