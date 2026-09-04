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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class FeedSectionActivity extends Activity {
    private static final int BG=Color.rgb(13,17,23),SURFACE=Color.rgb(22,27,34),BORDER=Color.rgb(48,54,61),TEXT=Color.rgb(230,237,243),MUTED=Color.rgb(139,148,158),GREEN=Color.rgb(63,185,80);
    private LifeDb db;private LinearLayout content;private String mode;
    @Override public void onCreate(Bundle s){super.onCreate(s);db=new LifeDb(this);mode=getIntent().getStringExtra("mode");render();}
    @Override protected void onResume(){super.onResume();load();}
    @Override protected void onDestroy(){db.close();super.onDestroy();}
    private void render(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(dp(12),dp(10),dp(16),dp(8));top.setBackgroundColor(SURFACE);Button back=button("‹");back.setContentDescription("Back");back.setOnClickListener(v->finish());top.addView(back,new LinearLayout.LayoutParams(dp(48),dp(48)));TextView t=text(title(),22,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);top.addView(t,new LinearLayout.LayoutParams(0,-2,1));root.addView(top);root.addView(divider());ScrollView sc=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(16),dp(14),dp(16),dp(24));sc.addView(content);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);}
    private String title(){if("attention".equals(mode))return "Needs attention";if("actions".equals(mode))return "Suggested actions";return "Today";}

    private void load(){
        content.removeAllViews();
        List<AttentionStore.Item> durable=AttentionStore.get(this).openItems(100);Set<Long> covered=new HashSet<>();for(AttentionStore.Item item:durable)covered.add(item.eventId);
        List<LifeDb.Loop> fallback=new ArrayList<>();for(LifeDb.Loop loop:db.openLoops(100))if(!covered.contains(loop.evidenceId))fallback.add(loop);

        if("attention".equals(mode)){
            for(AttentionStore.Item item:durable)durableAttentionCard(item);
            for(LifeDb.Loop x:fallback)legacyAttentionCard(x);
            if(durable.isEmpty()&&fallback.isEmpty())empty();return;
        }

        List<ProactiveFeedEngine.RankedConversation> ranked=ProactiveFeedEngine.rank(db,db.recentConversations(100),System.currentTimeMillis());
        if("actions".equals(mode)){
            for(AttentionStore.Item item:durable)if(!item.provisional&&AttentionStore.OPEN.equals(item.status))durableActionCard(item);
            List<ProactiveFeedEngine.Suggestion> derived=ProactiveFeedEngine.suggestions(db,fallback);
            List<PersistentActionQueue.Item> agent=new PersistentActionQueue(this).pending();
            for(ProactiveFeedEngine.Suggestion s:derived){LinearLayout c=row();c.addView(badge(s.kind));TextView t=text(s.title+"  ›",15,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(0,dp(7),0,0);c.addView(t);c.addView(meta(s.why));c.setOnClickListener(v->startActivity(new Intent(this,EvidenceDetailActivity.class).putExtra("event_id",s.eventId).putExtra("loop_id",s.loopId).putExtra("mode","action")));content.addView(c);}
            for(PersistentActionQueue.Item x:agent){LinearLayout c=row();c.addView(badge("AGENT · "+x.proposal.actionType.toUpperCase()));TextView t=text((x.proposal.payloadSummary.isEmpty()?x.proposal.actionType:x.proposal.payloadSummary)+"  ›",15,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(0,dp(7),0,0);c.addView(t);c.addView(meta(x.proposal.target));c.setOnClickListener(v->startActivity(new Intent(this,SuggestedActionDetailActivity.class).putExtra("proposal_id",x.proposal.proposalId)));content.addView(c);}
            boolean hasDurable=false;for(AttentionStore.Item item:durable)if(!item.provisional&&AttentionStore.OPEN.equals(item.status)){hasDurable=true;break;}
            if(!hasDurable&&derived.isEmpty()&&agent.isEmpty())empty();return;
        }

        if(!ranked.isEmpty()){for(ProactiveFeedEngine.RankedConversation r:ranked){LifeDb.Conversation x=r.conversation;LinearLayout c=row();TextView t=text(x.label+"  ›",15,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);c.addView(t);String sig=ProactiveSummaryEngine.signalLine(r.summary);if(!sig.isEmpty())c.addView(badge(sig.toUpperCase()));TextView b=text(trim(r.summary.summary,260),13,TEXT);b.setPadding(0,dp(6),0,0);c.addView(b);if(!r.summary.why.isEmpty())c.addView(meta(r.summary.why));c.addView(meta(x.count+" captured items · "+app(x.app)+" · "+time(x.latestAt)));c.setOnClickListener(v->startActivity(new Intent(this,ConversationDetailActivity.class).putExtra("event_id",x.latestEventId)));content.addView(c);}return;}

        List<LifeDb.Event> es=db.recentEvents(100);for(LifeDb.Event e:es){LinearLayout c=row();TextView t=text((e.title==null||e.title.isEmpty()?app(e.app):e.title)+"  ›",15,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);c.addView(t);TextView b=text(trim(e.body,260),13,TEXT);b.setPadding(0,dp(6),0,0);c.addView(b);c.addView(meta(app(e.app)+" · "+time(e.at)));c.setOnClickListener(v->{if(LifeDb.isConversationLike(e))startActivity(new Intent(this,ConversationDetailActivity.class).putExtra("event_id",e.id));else startActivity(new Intent(this,EvidenceDetailActivity.class).putExtra("event_id",e.id).putExtra("mode","activity"));});content.addView(c);}if(es.isEmpty())empty();
    }

    private void durableAttentionCard(AttentionStore.Item item){LifeDb.Event e=db.eventById(item.eventId);LinearLayout c=row();c.addView(badge(item.provisional?"ANALYZING":(!"NONE".equals(item.intent)?item.intent:item.type)));String who=e==null?"":(LifeDb.isConversationLike(e)?LifeDb.personLabel(e):LifeDb.friendlyApp(e.app));TextView t=text((who.isEmpty()?"":who+" · ")+item.summary+"  ›",15,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(0,dp(7),0,0);c.addView(t);c.addView(meta(item.provisional?"Reserved instantly · deep analysis pending":time(item.sourceAt)));c.setOnClickListener(v->startActivity(new Intent(this,EvidenceDetailActivity.class).putExtra("event_id",item.eventId).putExtra("mode","attention")));content.addView(c);}
    private void legacyAttentionCard(LifeDb.Loop x){LinearLayout c=row();c.addView(badge(kind(x.kind)));LifeDb.Event e=db.eventById(x.evidenceId);String who=e==null?"":(LifeDb.isConversationLike(e)?LifeDb.personLabel(e):LifeDb.friendlyApp(e.app));TextView t=text((who.isEmpty()?"":who+" · ")+x.title+"  ›",15,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(0,dp(7),0,0);c.addView(t);if(e!=null)c.addView(meta(time(e.at)+(x.dueAt>0?" · dated":"")));c.setOnClickListener(v->startActivity(new Intent(this,EvidenceDetailActivity.class).putExtra("event_id",x.evidenceId).putExtra("loop_id",x.id).putExtra("mode","attention")));content.addView(c);}
    private void durableActionCard(AttentionStore.Item item){LinearLayout c=row();c.addView(badge(actionLabel(item.action).toUpperCase()));TextView t=text(actionLabel(item.action)+": "+item.summary+"  ›",15,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(0,dp(7),0,0);c.addView(t);if(!item.reason.isEmpty())c.addView(meta(item.reason));c.setOnClickListener(v->startActivity(new Intent(this,EvidenceDetailActivity.class).putExtra("event_id",item.eventId).putExtra("mode","action")));content.addView(c);}

    private static String actionLabel(String action){if("REPLY".equals(action))return "Reply";if("DO_TASK".equals(action))return "Do task";if("VERIFY".equals(action))return "Verify";if("PAY".equals(action))return "Pay";if("REVIEW".equals(action))return "Review";if("CALL_BACK".equals(action))return "Call back";return "Review";}
    private static String trim(String s,int n){if(s==null)return "";s=s.trim();return s.length()>n?s.substring(0,n)+"…":s;}
    private void empty(){TextView v=text("Nothing here right now.",14,MUTED);v.setPadding(dp(4),dp(8),0,0);content.addView(v);}
    private LinearLayout row(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(13),dp(14),dp(13));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(8));c.setLayoutParams(p);c.setBackground(round(SURFACE,BORDER,9));return c;}
    private TextView badge(String s){TextView v=text(s,11,GREEN);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setPadding(dp(8),dp(4),dp(8),dp(4));v.setLayoutParams(new LinearLayout.LayoutParams(-2,-2));v.setBackground(round(Color.TRANSPARENT,GREEN,20));return v;}
    private TextView meta(String s){TextView v=text(s,11,MUTED);v.setPadding(0,dp(7),0,0);return v;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(TEXT);b.setTextSize(18);b.setAllCaps(false);b.setBackgroundColor(Color.TRANSPARENT);return b;}
    private View divider(){View v=new View(this);v.setBackgroundColor(BORDER);v.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(1)));return v;}
    private GradientDrawable round(int fill,int stroke,int radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));g.setStroke(dp(1),stroke);return g;}
    private TextView text(String s,int size,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setLineSpacing(0,1.08f);return v;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private static String app(String p){return LifeDb.friendlyApp(p);}private static String kind(String value){if("financial_alert".equals(value))return "FINANCIAL";return value==null?"ATTENTION":value.toUpperCase();}
    private static String time(long at){return DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(at));}
}
