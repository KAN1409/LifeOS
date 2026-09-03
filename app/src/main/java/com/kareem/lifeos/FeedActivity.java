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
import com.kareem.lifeos.actions.PersistentActionQueue;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/** Proactive home: ranked situations first, plumbing hidden, everything drillable. */
public final class FeedActivity extends Activity {
    private static final int BG=Color.rgb(13,17,23),SURFACE=Color.rgb(22,27,34),BORDER=Color.rgb(48,54,61),TEXT=Color.rgb(230,237,243),MUTED=Color.rgb(139,148,158),GREEN=Color.rgb(63,185,80);
    private LifeDb db;private LinearLayout content;
    @Override public void onCreate(Bundle s){super.onCreate(s);db=new LifeDb(this);render();}
    @Override protected void onResume(){super.onResume();refresh();}
    @Override protected void onDestroy(){db.close();super.onDestroy();}

    private void render(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(dp(20),dp(18),dp(16),dp(14));top.setBackgroundColor(SURFACE);TextView title=text("LifeOS",24,TEXT);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);top.addView(title,new LinearLayout.LayoutParams(0,-2,1));Button more=secondary("•••");more.setOnClickListener(v->startActivity(new Intent(this,MainActivity.class)));top.addView(more,new LinearLayout.LayoutParams(dp(52),dp(40)));root.addView(top);root.addView(divider());ScrollView sc=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(16),dp(14),dp(16),dp(28));sc.addView(content);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);}

    private void refresh(){
        content.removeAllViews();
        List<LifeDb.Loop> attention=db.openLoops(50);
        List<PersistentActionQueue.Item> agentActions=new PersistentActionQueue(this).pending();
        List<SituationEngine.Situation> situations=SituationEngine.build(db,attention,agentActions,System.currentTimeMillis());
        List<ProactiveFeedEngine.RankedConversation> ranked=ProactiveFeedEngine.rank(db,db.recentConversations(30),System.currentTimeMillis());
        List<ProactiveFeedEngine.Suggestion> suggestions=ProactiveFeedEngine.suggestions(ranked);
        ProactiveFeedEngine.DaySummary day=ProactiveFeedEngine.daySummary(ranked,attention,agentActions.size());
        daySummary(day);

        situationHeader(situations.size());
        if(situations.isEmpty())empty("No active situations have formed yet.");else for(int i=0;i<Math.min(4,situations.size());i++)situationCard(situations.get(i));

        sectionHeader("Needs attention",attention.size(),"attention");
        if(attention.isEmpty())empty("Nothing currently needs attention.");else for(int i=0;i<Math.min(3,attention.size());i++)attentionCard(attention.get(i));

        int suggestionCount=suggestions.size()+agentActions.size();
        sectionHeader("Suggested actions",suggestionCount,"actions");
        int shown=0;for(ProactiveFeedEngine.Suggestion s:suggestions){if(shown++>=3)break;suggestionCard(s);}for(PersistentActionQueue.Item x:agentActions){if(shown++>=3)break;actionCard(x);}if(suggestionCount==0)empty("No useful action is being suggested right now.");

        sectionHeader("Today",ranked.size(),"activity");
        if(ranked.isEmpty()){List<LifeDb.Event> events=db.recentEvents(30);if(events.isEmpty())empty("No meaningful activity captured yet.");else for(int i=0;i<Math.min(5,events.size());i++)eventCard(events.get(i));}else for(int i=0;i<Math.min(5,ranked.size());i++)conversationCard(ranked.get(i));

        navCard("People & Social Radar","Relationship history, conversations and changes",LifeSignalsActivity.class);navCard("Decisions","Decision history, context and consequences",LifeSignalsActivity.class);navCard("Action history","Approvals and executed actions",ActionCenterActivity.class);
    }

    private void daySummary(ProactiveFeedEngine.DaySummary d){LinearLayout r=row();TextView k=text("DAY SUMMARY",11,GREEN);k.setTypeface(Typeface.DEFAULT,Typeface.BOLD);r.addView(k);TextView h=text(d.headline,20,TEXT);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);h.setPadding(0,dp(7),0,0);r.addView(h);r.addView(meta(d.detail));r.setOnClickListener(v->openSection("activity"));content.addView(r);}
    private void situationHeader(int count){LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);r.setPadding(dp(2),dp(18),dp(2),dp(8));TextView t=text("Situations",17,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);r.addView(t,new LinearLayout.LayoutParams(0,-2,1));r.addView(text(count+" active",13,MUTED));content.addView(r);}
    private void situationCard(SituationEngine.Situation s){LinearLayout c=row();c.addView(badge(s.status));TextView t=text(s.title+"  ›",16,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(0,dp(8),0,0);c.addView(t);TextView sm=text(trim(s.summary,260),13,TEXT);sm.setPadding(0,dp(7),0,0);c.addView(sm);if(s.why!=null&&!s.why.isEmpty()){TextView why=text(s.why,12,GREEN);why.setPadding(0,dp(7),0,0);c.addView(why);}c.addView(meta(s.eventCount+" evidence · "+s.attentionCount+" attention · "+s.actionCount+" actions · priority "+s.score));c.setOnClickListener(v->startActivity(new Intent(this,SituationDetailActivity.class).putExtra("situation_id",s.id)));content.addView(c);}
    private void sectionHeader(String title,int count,String mode){LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);r.setPadding(dp(2),dp(18),dp(2),dp(8));TextView t=text(title,17,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);r.addView(t,new LinearLayout.LayoutParams(0,-2,1));r.addView(text(count+"  ›",13,MUTED));r.setOnClickListener(v->openSection(mode));content.addView(r);}
    private void attentionCard(LifeDb.Loop x){LinearLayout c=row();c.addView(badge(x.kind.toUpperCase()));TextView t=text(x.title,15,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(0,dp(7),0,0);c.addView(t);LifeDb.Event e=db.eventById(x.evidenceId);if(e!=null)c.addView(meta(app(e.app)+" · "+time(e.at)));c.setOnClickListener(v->openEvidence(x.evidenceId,"attention",x.id));content.addView(c);}
    private void suggestionCard(ProactiveFeedEngine.Suggestion s){LinearLayout c=row();c.addView(badge(s.kind));TextView t=text(s.title+"  ›",15,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(0,dp(7),0,0);c.addView(t);c.addView(meta(s.why));c.setOnClickListener(v->startActivity(new Intent(this,ConversationDetailActivity.class).putExtra("event_id",s.eventId)));content.addView(c);}
    private void actionCard(PersistentActionQueue.Item x){LinearLayout c=row();c.addView(badge("AGENT ACTION"));TextView t=text((x.proposal.payloadSummary.isEmpty()?x.proposal.actionType:x.proposal.payloadSummary)+"  ›",15,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(0,dp(7),0,0);c.addView(t);c.addView(meta(x.proposal.target));c.setOnClickListener(v->openSection("actions"));content.addView(c);}
    private void conversationCard(ProactiveFeedEngine.RankedConversation r){LifeDb.Conversation x=r.conversation;ProactiveSummaryEngine.Result s=r.summary;LinearLayout c=row();TextView t=text(x.label+"  ›",15,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);c.addView(t);String signal=ProactiveSummaryEngine.signalLine(s);if(!signal.isEmpty())c.addView(badge(signal.toUpperCase()));TextView p=text(trim(s.summary,240),13,TEXT);p.setPadding(0,dp(7),0,0);c.addView(p);if(!s.why.isEmpty()){TextView why=text(s.why,12,GREEN);why.setPadding(0,dp(7),0,0);c.addView(why);}c.addView(meta(x.count+" captured items · "+app(x.app)+" · "+time(x.latestAt)+" · priority "+r.score));c.setOnClickListener(v->startActivity(new Intent(this,ConversationDetailActivity.class).putExtra("event_id",x.latestEventId)));content.addView(c);}
    private void eventCard(LifeDb.Event e){LinearLayout c=row();TextView t=text((e.title==null||e.title.isEmpty()?app(e.app):e.title)+"  ›",15,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);c.addView(t);c.addView(text(trim(e.body,180),13,TEXT));c.addView(meta(app(e.app)+" · "+time(e.at)));c.setOnClickListener(v->{if(LifeDb.isConversationLike(e))startActivity(new Intent(this,ConversationDetailActivity.class).putExtra("event_id",e.id));else openEvidence(e.id,"activity",0);});content.addView(c);}
    private void navCard(String title,String sub,Class<?> cls){LinearLayout c=row();TextView t=text(title+"  ›",15,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);c.addView(t);c.addView(meta(sub));c.setOnClickListener(v->startActivity(new Intent(this,cls)));LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)c.getLayoutParams();p.setMargins(0,dp(12),0,dp(8));c.setLayoutParams(p);content.addView(c);}
    private void openSection(String m){startActivity(new Intent(this,FeedSectionActivity.class).putExtra("mode",m));}private void openEvidence(long id,String m,long loop){startActivity(new Intent(this,EvidenceDetailActivity.class).putExtra("event_id",id).putExtra("mode",m).putExtra("loop_id",loop));}
    private void empty(String s){TextView v=text(s,13,MUTED);v.setPadding(dp(4),dp(4),dp(4),dp(8));content.addView(v);}private LinearLayout row(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(13),dp(14),dp(13));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(8));c.setLayoutParams(p);c.setBackground(round(SURFACE,BORDER,9));return c;}private TextView badge(String s){TextView v=text(s,11,GREEN);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setPadding(dp(8),dp(4),dp(8),dp(4));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.setMargins(0,dp(7),0,0);v.setLayoutParams(p);v.setBackground(round(Color.TRANSPARENT,GREEN,20));return v;}private TextView meta(String s){TextView v=text(s,11,MUTED);v.setPadding(0,dp(7),0,0);return v;}private Button secondary(String s){Button b=new Button(this);b.setText(s);b.setTextColor(TEXT);b.setTextSize(13);b.setAllCaps(false);b.setBackground(round(SURFACE,BORDER,8));return b;}private View divider(){View v=new View(this);v.setBackgroundColor(BORDER);v.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(1)));return v;}private GradientDrawable round(int fill,int stroke,int radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));g.setStroke(dp(1),stroke);return g;}private TextView text(String s,int size,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setLineSpacing(0,1.08f);return v;}private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}private static String trim(String s,int n){if(s==null)return "";s=s.trim();return s.length()>n?s.substring(0,n)+"…":s;}private static String app(String p){if(p==null)return "Unknown";int i=p.lastIndexOf('.');String x=i>=0?p.substring(i+1):p;return x.isEmpty()?p:Character.toUpperCase(x.charAt(0))+x.substring(1);}private static String time(long at){return DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(at));}
}
