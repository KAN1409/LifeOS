package com.kareem.lifeos;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.kareem.lifeos.actions.PersistentActionQueue;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Proactive home: model meaning first, legacy heuristics only as a conservative fallback. */
public final class FeedActivity extends Activity {
    private static final int BG=Color.rgb(13,17,23),SURFACE=Color.rgb(22,27,34),BORDER=Color.rgb(48,54,61),TEXT=Color.rgb(230,237,243),MUTED=Color.rgb(139,148,158),GREEN=Color.rgb(63,185,80);
    private LifeDb db;private LinearLayout content;
    @Override public void onCreate(Bundle s){super.onCreate(s);db=new LifeDb(this);render();}
    @Override protected void onResume(){super.onResume();refresh();NotificationBrain.analyzeForeground(this,r->{if(!isFinishing()&&content!=null)refresh();});}
    @Override protected void onDestroy(){db.close();super.onDestroy();}

    private void render(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(dp(20),dp(14),dp(16),dp(10));top.setBackgroundColor(SURFACE);TextView title=text("LifeOS",24,TEXT);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);top.addView(title,new LinearLayout.LayoutParams(0,-2,1));Button more=secondary("•••");more.setContentDescription("Open diagnostics");more.setOnClickListener(v->startActivity(new Intent(this,MainActivity.class)));top.addView(more,new LinearLayout.LayoutParams(dp(52),dp(48)));root.addView(top);root.addView(divider());ScrollView sc=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(16),dp(14),dp(16),dp(28));sc.addView(content);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);}

    private void refresh(){
        content.removeAllViews();
        List<LifeDb.Loop> rawLoops=db.openLoops(50);
        List<NotificationMeaning> brainAttention=brainAttention(30);
        List<LifeDb.Loop> fallbackAttention=legacyNotCovered(rawLoops,brainAttention);
        List<PersistentActionQueue.Item> agentActions=new PersistentActionQueue(this).pending();
        List<SituationEngine.Situation> situations=SituationEngine.build(db,rawLoops,agentActions,System.currentTimeMillis());
        List<ProactiveFeedEngine.RankedConversation> ranked=ProactiveFeedEngine.rank(db,db.recentConversations(30),System.currentTimeMillis());
        List<ProactiveFeedEngine.Suggestion> fallbackSuggestions=ProactiveFeedEngine.suggestions(db,fallbackAttention);
        ProactiveFeedEngine.DaySummary day=ProactiveFeedEngine.daySummary(ranked,rawLoops,agentActions.size());
        int totalAttention=brainAttention.size()+fallbackAttention.size();
        daySummary(day,totalAttention>0);

        situationHeader(Math.min(3,situations.size()),situations.size()>3);
        if(situations.isEmpty())empty("No unresolved situation needs you right now.");else for(int i=0;i<Math.min(3,situations.size());i++)situationCard(situations.get(i));

        sectionHeader("Needs attention",Math.min(3,totalAttention),"attention");
        if(totalAttention==0)empty("Nothing currently needs attention.");else{
            int shown=0;
            for(NotificationMeaning m:brainAttention){if(shown++>=3)break;brainAttentionCard(m);}
            for(LifeDb.Loop x:fallbackAttention){if(shown++>=3)break;attentionCard(x);}
        }

        int brainSuggestionCount=brainAttention.size();
        int suggestionCount=brainSuggestionCount+fallbackSuggestions.size()+agentActions.size();
        sectionHeader("Suggested actions",Math.min(3,suggestionCount),"actions");
        int shown=0;
        for(NotificationMeaning m:brainAttention){if(shown++>=3)break;brainSuggestionCard(m);}
        for(ProactiveFeedEngine.Suggestion s:fallbackSuggestions){if(shown++>=3)break;suggestionCard(s);}
        for(PersistentActionQueue.Item x:agentActions){if(shown++>=3)break;actionCard(x);}
        if(suggestionCount==0)empty("No grounded action is being suggested right now.");

        List<ProactiveFeedEngine.RankedConversation> today=meaningfulToday(ranked,3);
        sectionHeader("Today",today.size(),"activity");
        if(today.isEmpty()){List<LifeDb.Event> events=meaningfulEvents(db.recentEvents(60),3);if(events.isEmpty())empty("No meaningful activity captured yet.");else for(LifeDb.Event e:events)eventCard(e);}else for(ProactiveFeedEngine.RankedConversation r:today)conversationCard(r);

        navCard("People & Social Radar","Relationship history, conversations and changes",LifeSignalsActivity.class);navCard("Decisions","Decision history, context and consequences",LifeSignalsActivity.class);navCard("Action history","Approvals and executed actions",ActionCenterActivity.class);
    }

    private List<NotificationMeaning> brainAttention(int limit){ArrayList<NotificationMeaning> out=new ArrayList<>();for(NotificationMeaning m:NotificationMeaningStore.get(this).recent(Math.max(limit*3,30))){if(m.needsAttention())out.add(m);if(out.size()>=limit)break;}return out;}
    private List<LifeDb.Loop> legacyNotCovered(List<LifeDb.Loop> loops,List<NotificationMeaning> meanings){Set<String> covered=new HashSet<>();for(NotificationMeaning m:meanings)if(m.canSummarize())covered.add(m.streamId);ArrayList<LifeDb.Loop> out=new ArrayList<>();for(LifeDb.Loop x:loops){LifeDb.Event e=db.eventById(x.evidenceId);if(e!=null&&covered.contains(e.threadKey))continue;out.add(x);}return out;}
    private long eventForStream(String streamId){try(Cursor c=db.getReadableDatabase().rawQuery("SELECT id FROM events WHERE thread_key=? ORDER BY captured_at DESC LIMIT 1",new String[]{streamId})){return c.moveToFirst()?c.getLong(0):0;}catch(Throwable ignored){return 0;}}

    private void daySummary(ProactiveFeedEngine.DaySummary d,boolean needsAttention){LinearLayout r=row();TextView k=text("TODAY",11,GREEN);k.setTypeface(Typeface.DEFAULT,Typeface.BOLD);r.addView(k);TextView h=limited(d.headline,19,TEXT,2);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);h.setPadding(0,dp(5),0,0);r.addView(h);r.addView(meta(d.detail));r.setOnClickListener(v->openSection(needsAttention?"attention":"activity"));content.addView(r);}
    private void situationHeader(int shown,boolean more){LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);r.setPadding(dp(2),dp(14),dp(2),dp(7));TextView t=text("Situations",17,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);r.addView(t,new LinearLayout.LayoutParams(0,-2,1));r.addView(text(shown+(more?"+":"")+" surfaced",12,MUTED));content.addView(r);}
    private void situationCard(SituationEngine.Situation s){LinearLayout c=row();c.addView(badge(s.status));TextView t=limited(s.title+"  ›",16,TEXT,1);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(0,dp(6),0,0);c.addView(t);TextView sm=limited(s.summary,13,TEXT,2);sm.setPadding(0,dp(5),0,0);c.addView(sm);if(s.why!=null&&!s.why.isEmpty()){TextView why=limited(s.why,12,GREEN,1);why.setPadding(0,dp(5),0,0);c.addView(why);}c.addView(meta("Updated "+time(s.latestAt)));c.setOnClickListener(v->startActivity(new Intent(this,SituationDetailActivity.class).putExtra("situation_id",s.id)));content.addView(c);}
    private void sectionHeader(String title,int shown,String mode){LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);r.setPadding(dp(2),dp(18),dp(2),dp(8));TextView t=text(title,17,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);r.addView(t,new LinearLayout.LayoutParams(0,-2,1));r.addView(text((shown>0?String.valueOf(shown):"")+"  ›",13,MUTED));r.setOnClickListener(v->openSection(mode));content.addView(r);}

    private void brainAttentionCard(NotificationMeaning m){long eventId=eventForStream(m.streamId);LinearLayout c=row();c.addView(badge(!"NONE".equals(m.intent)?m.intent:m.type));TextView t=limited(m.summary+"  ›",15,TEXT,2);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(0,dp(6),0,0);c.addView(t);if(!m.reason.isEmpty()){TextView why=limited(m.reason,12,GREEN,2);why.setPadding(0,dp(5),0,0);c.addView(why);}c.addView(meta(actionLabel(m.action)+" · "+m.urgency.toLowerCase()+" urgency"));if(eventId>0)c.setOnClickListener(v->openEvidence(eventId,"attention",0));content.addView(c);}
    private void brainSuggestionCard(NotificationMeaning m){long eventId=eventForStream(m.streamId);LinearLayout c=row();c.addView(badge(actionLabel(m.action).toUpperCase()));TextView t=limited(actionHeadline(m)+"  ›",15,TEXT,2);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(0,dp(6),0,0);c.addView(t);if(!m.reason.isEmpty())c.addView(meta(m.reason));if(eventId>0)c.setOnClickListener(v->openEvidence(eventId,"action",0));content.addView(c);}
    private void attentionCard(LifeDb.Loop x){LinearLayout c=row();c.addView(badge(kind(x.kind)));LifeDb.Event e=db.eventById(x.evidenceId);String who=e==null?"":(LifeDb.isConversationLike(e)?LifeDb.personLabel(e):LifeDb.friendlyApp(e.app));TextView t=limited((who.isEmpty()?"":who+" · ")+x.title,15,TEXT,2);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(0,dp(6),0,0);c.addView(t);if(e!=null)c.addView(meta(time(e.at)+(x.dueAt>0?" · dated":"")));c.setOnClickListener(v->openEvidence(x.evidenceId,"attention",x.id));content.addView(c);}
    private void suggestionCard(ProactiveFeedEngine.Suggestion s){LinearLayout c=row();c.addView(badge(s.kind));TextView t=limited(s.title+"  ›",15,TEXT,1);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(0,dp(6),0,0);c.addView(t);c.addView(meta(s.why));c.setOnClickListener(v->openEvidence(s.eventId,"action",s.loopId));content.addView(c);}
    private void actionCard(PersistentActionQueue.Item x){LinearLayout c=row();c.addView(badge("ACTION"));TextView t=text((x.proposal.payloadSummary.isEmpty()?x.proposal.actionType:x.proposal.payloadSummary)+"  ›",15,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(0,dp(7),0,0);c.addView(t);c.addView(meta(x.proposal.target));c.setOnClickListener(v->openSection("actions"));content.addView(c);}
    private void conversationCard(ProactiveFeedEngine.RankedConversation r){LifeDb.Conversation x=r.conversation;ProactiveSummaryEngine.Result s=r.summary;NotificationMeaning brain=NotificationMeaningStore.get(this).forStream(x.threadKey);LinearLayout c=row();TextView t=limited(x.label+"  ›",15,TEXT,1);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);c.addView(t);if(brain!=null&&brain.canSummarize()){String signal=!"NONE".equals(brain.intent)?brain.intent:brain.type;if(!"OTHER".equals(signal))c.addView(badge(signal));TextView p=limited(brain.summary,13,TEXT,2);p.setPadding(0,dp(5),0,0);c.addView(p);if(!brain.reason.isEmpty()){TextView why=limited(brain.reason,12,GREEN,1);why.setPadding(0,dp(5),0,0);c.addView(why);}}else{String signal=ProactiveSummaryEngine.signalLine(s);if(!signal.isEmpty())c.addView(badge(signal.toUpperCase()));TextView p=limited(s.summary,13,TEXT,2);p.setPadding(0,dp(5),0,0);c.addView(p);if(!s.why.isEmpty()){TextView why=limited(s.why,12,GREEN,1);why.setPadding(0,dp(5),0,0);c.addView(why);}}c.addView(meta(app(x.app)+" · "+time(x.latestAt)));c.setOnClickListener(v->startActivity(new Intent(this,ConversationDetailActivity.class).putExtra("event_id",x.latestEventId)));content.addView(c);}
    private void eventCard(LifeDb.Event e){NotificationMeaning brain=NotificationMeaningStore.get(this).forStream(e.threadKey);LinearLayout c=row();TextView t=text((e.title==null||e.title.isEmpty()?app(e.app):e.title)+"  ›",15,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);c.addView(t);if(brain!=null&&brain.canSummarize()){String signal=!"NONE".equals(brain.intent)?brain.intent:brain.type;if(!"OTHER".equals(signal))c.addView(badge(signal));TextView p=limited(brain.summary,13,TEXT,2);p.setPadding(0,dp(5),0,0);c.addView(p);if(!brain.reason.isEmpty())c.addView(meta(brain.reason));}else c.addView(text(trim(e.body,180),13,TEXT));c.addView(meta(app(e.app)+" · "+time(e.at)));c.setOnClickListener(v->{if(LifeDb.isConversationLike(e))startActivity(new Intent(this,ConversationDetailActivity.class).putExtra("event_id",e.id));else openEvidence(e.id,"activity",0);});content.addView(c);}
    private void navCard(String title,String sub,Class<?> cls){LinearLayout c=row();TextView t=text(title+"  ›",15,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);c.addView(t);c.addView(meta(sub));c.setOnClickListener(v->startActivity(new Intent(this,cls)));LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)c.getLayoutParams();p.setMargins(0,dp(12),0,dp(8));c.setLayoutParams(p);content.addView(c);}

    private List<ProactiveFeedEngine.RankedConversation> meaningfulToday(List<ProactiveFeedEngine.RankedConversation> ranked,int limit){ArrayList<ProactiveFeedEngine.RankedConversation> out=new ArrayList<>();long cutoff=System.currentTimeMillis()-24*60*60*1000L;NotificationMeaningStore store=NotificationMeaningStore.get(this);for(ProactiveFeedEngine.RankedConversation r:ranked){if(r.conversation.latestAt<cutoff)continue;NotificationMeaning brain=store.forStream(r.conversation.threadKey);boolean modelMeaning=brain!=null&&brain.canSummarize()&&worthToday(brain);if(!modelMeaning&&r.summary.signals.isEmpty()&&r.summary.summary.startsWith("No substantive"))continue;out.add(r);if(out.size()>=limit)break;}return out;}
    private List<LifeDb.Event> meaningfulEvents(List<LifeDb.Event> events,int limit){ArrayList<LifeDb.Event> out=new ArrayList<>();NotificationMeaningStore store=NotificationMeaningStore.get(this);for(LifeDb.Event e:events){NotificationMeaning brain=store.forStream(e.threadKey);if(!EventSemantics.shouldShowInToday(e)&&!(brain!=null&&brain.canSummarize()&&worthToday(brain)))continue;out.add(e);if(out.size()>=limit)break;}return out;}
    private static boolean worthToday(NotificationMeaning m){return m!=null&&!"PROMOTION".equals(m.type)&&!"CONTENT_READY".equals(m.type)&&!"SYSTEM_EVENT".equals(m.type)&&!"OTHER".equals(m.type);}
    private static String actionLabel(String action){if("REPLY".equals(action))return "Reply";if("DO_TASK".equals(action))return "Do task";if("VERIFY".equals(action))return "Verify";if("PAY".equals(action))return "Pay";if("REVIEW".equals(action))return "Review";if("CALL_BACK".equals(action))return "Call back";return "Review";}
    private static String actionHeadline(NotificationMeaning m){String verb=actionLabel(m.action);return verb+": "+m.summary;}
    private void openSection(String m){startActivity(new Intent(this,FeedSectionActivity.class).putExtra("mode",m));}private void openEvidence(long id,String m,long loop){startActivity(new Intent(this,EvidenceDetailActivity.class).putExtra("event_id",id).putExtra("mode",m).putExtra("loop_id",loop));}
    private void empty(String s){TextView v=text(s,13,MUTED);v.setPadding(dp(4),dp(4),dp(4),dp(8));content.addView(v);}private LinearLayout row(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(12),dp(10),dp(12),dp(10));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(6));c.setLayoutParams(p);c.setBackground(round(SURFACE,BORDER,8));return c;}private TextView badge(String s){TextView v=text(s,10,GREEN);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setPadding(dp(7),dp(3),dp(7),dp(3));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.setMargins(0,dp(5),0,0);v.setLayoutParams(p);v.setBackground(round(Color.TRANSPARENT,GREEN,20));return v;}private TextView meta(String s){TextView v=limited(s,11,MUTED,1);v.setPadding(0,dp(5),0,0);return v;}private TextView limited(String s,int size,int color,int lines){TextView v=text(s,size,color);v.setMaxLines(lines);v.setEllipsize(TextUtils.TruncateAt.END);return v;}private Button secondary(String s){Button b=new Button(this);b.setText(s);b.setTextColor(TEXT);b.setTextSize(13);b.setAllCaps(false);b.setBackground(round(SURFACE,BORDER,8));return b;}private View divider(){View v=new View(this);v.setBackgroundColor(BORDER);v.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(1)));return v;}private GradientDrawable round(int fill,int stroke,int radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));g.setStroke(dp(1),stroke);return g;}private TextView text(String s,int size,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setLineSpacing(0,1.08f);return v;}private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}private static String trim(String s,int n){if(s==null)return "";s=s.trim();return s.length()>n?s.substring(0,n)+"…":s;}private static String app(String p){return LifeDb.friendlyApp(p);}private static String time(long at){return DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(at));}private static String kind(String value){if("financial_alert".equals(value))return "FINANCIAL";return value==null?"ATTENTION":value.toUpperCase();}
}
