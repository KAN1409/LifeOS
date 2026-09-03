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
import java.util.Collections;
import java.util.Date;
import java.util.List;

/** Situation -> summary -> related attention/actions -> evidence. */
public final class SituationDetailActivity extends Activity {
    private static final int BG=Color.rgb(13,17,23),SURFACE=Color.rgb(22,27,34),BORDER=Color.rgb(48,54,61),TEXT=Color.rgb(230,237,243),MUTED=Color.rgb(139,148,158),GREEN=Color.rgb(63,185,80),BLUE=Color.rgb(47,129,247);
    private LifeDb db;private String situationId;private SituationEngine.Situation situation;private List<PersistentActionQueue.Item> actions;
    @Override public void onCreate(Bundle s){super.onCreate(s);db=new LifeDb(this);situationId=getIntent().getStringExtra("situation_id");load();render();}
    @Override protected void onResume(){super.onResume();load();}
    @Override protected void onDestroy(){db.close();super.onDestroy();}
    private void load(){actions=new PersistentActionQueue(this).pending();situation=SituationEngine.find(db,db.openLoops(100),actions,situationId==null?"":situationId,System.currentTimeMillis());}
    private void render(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(dp(12),dp(14),dp(16),dp(12));top.setBackgroundColor(SURFACE);Button back=plain("‹");back.setOnClickListener(v->finish());top.addView(back,new LinearLayout.LayoutParams(dp(48),dp(40)));TextView title=text("Situation",22,TEXT);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);top.addView(title);root.addView(top);root.addView(divider());ScrollView sc=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(16),dp(18),dp(16),dp(28));sc.addView(body);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));if(situation==null){body.addView(text("This situation is no longer active.",14,MUTED));setContentView(root);return;}
        body.addView(badge(situation.status));TextView h=text(situation.title,22,TEXT);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);h.setPadding(0,dp(10),0,0);body.addView(h);body.addView(meta(time(situation.latestAt)+" · priority "+situation.score));
        LinearLayout summary=row();summary.addView(label("SUMMARY"));TextView sv=text(situation.summary,15,TEXT);sv.setPadding(0,dp(8),0,0);summary.addView(sv);if(situation.why!=null&&!situation.why.isEmpty()){TextView w=text(situation.why,13,GREEN);w.setPadding(0,dp(9),0,0);summary.addView(w);}if(!situation.signals.isEmpty())summary.addView(meta("Signals · "+join(situation.signals)));body.addView(summary);
        LinearLayout counts=row();counts.addView(label("CONNECTED"));counts.addView(text(situation.eventCount+" evidence items · "+situation.attentionCount+" attention · "+situation.actionCount+" actions",14,TEXT));body.addView(counts);
        if(situation.attentionCount>0){TextView ah=section("Needs attention");body.addView(ah);for(Long lid:situation.loopIds){LifeDb.Loop found=findLoop(lid);if(found==null)continue;LinearLayout c=row();c.addView(badge(found.kind.toUpperCase()));c.addView(text(found.title+"  ›",14,TEXT));c.setOnClickListener(v->startActivity(new Intent(this,EvidenceDetailActivity.class).putExtra("event_id",found.evidenceId).putExtra("loop_id",found.id).putExtra("mode","attention")));body.addView(c);}}
        if(situation.actionCount>0){body.addView(section("Suggested actions"));for(String pid:situation.proposalIds){PersistentActionQueue.Item item=findAction(pid);if(item==null)continue;LinearLayout c=row();c.addView(badge("ACTION"));String t=item.proposal.payloadSummary.isEmpty()?item.proposal.actionType:item.proposal.payloadSummary;c.addView(text(t+"  ›",14,TEXT));c.addView(meta(item.proposal.target));c.setOnClickListener(v->startActivity(new Intent(this,SuggestedActionDetailActivity.class).putExtra("proposal_id",item.proposal.proposalId)));body.addView(c);}}
        body.addView(section("Evidence timeline"));List<LifeDb.Event> es=new ArrayList<>(SituationEngine.evidence(db,situation,160));Collections.reverse(es);for(LifeDb.Event e:es){LinearLayout c=row();TextView t=text((e.title==null||e.title.trim().isEmpty()?"Captured evidence":e.title)+"  ›",14,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);c.addView(t);c.addView(text(clip(e.body,260),13,TEXT));c.addView(meta(time(e.at)));c.setOnClickListener(v->startActivity(new Intent(this,EvidenceDetailActivity.class).putExtra("event_id",e.id).putExtra("mode","situation")));body.addView(c);}setContentView(root);}
    private LifeDb.Loop findLoop(long id){for(LifeDb.Loop x:db.openLoops(200))if(x.id==id)return x;return null;}private PersistentActionQueue.Item findAction(String id){for(PersistentActionQueue.Item x:actions)if(x.proposal.proposalId.equals(id))return x;return null;}
    private TextView section(String s){TextView v=text(s,18,TEXT);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setPadding(0,dp(22),0,dp(2));return v;}private TextView label(String s){TextView v=text(s,11,GREEN);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}private LinearLayout row(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(13),dp(14),dp(13));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(10),0,0);c.setLayoutParams(p);c.setBackground(round(SURFACE,BORDER,9));return c;}private TextView badge(String s){TextView v=text(s,11,GREEN);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setPadding(dp(8),dp(4),dp(8),dp(4));v.setBackground(round(Color.TRANSPARENT,GREEN,20));return v;}private TextView meta(String s){TextView v=text(s,11,MUTED);v.setPadding(0,dp(7),0,0);return v;}private Button plain(String s){Button b=new Button(this);b.setText(s);b.setTextColor(TEXT);b.setTextSize(18);b.setAllCaps(false);b.setBackgroundColor(Color.TRANSPARENT);return b;}private View divider(){View v=new View(this);v.setBackgroundColor(BORDER);v.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(1)));return v;}private GradientDrawable round(int fill,int stroke,int radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));g.setStroke(dp(1),stroke);return g;}private TextView text(String s,int size,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setLineSpacing(0,1.08f);return v;}private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}private static String clip(String s,int n){if(s==null)return "";s=s.trim();return s.length()>n?s.substring(0,n)+"…":s;}private static String join(List<String> xs){StringBuilder b=new StringBuilder();for(String x:xs){if(b.length()>0)b.append(" · ");b.append(x);}return b.toString();}private static String time(long at){return DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(at));}
}
