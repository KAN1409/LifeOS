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

/** Proactive, drill-down-first LifeOS home. No infrastructure controls live here. */
public final class FeedActivity extends Activity {
    private static final int BG=Color.rgb(13,17,23),SURFACE=Color.rgb(22,27,34),BORDER=Color.rgb(48,54,61),TEXT=Color.rgb(230,237,243),MUTED=Color.rgb(139,148,158),GREEN=Color.rgb(63,185,80),BLUE=Color.rgb(47,129,247);
    private LifeDb db; private LinearLayout content;
    @Override public void onCreate(Bundle state){super.onCreate(state);db=new LifeDb(this);render();}
    @Override protected void onResume(){super.onResume();refresh();}
    @Override protected void onDestroy(){db.close();super.onDestroy();}

    private void render(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(dp(20),dp(18),dp(16),dp(14));top.setBackgroundColor(SURFACE);
        TextView title=text("LifeOS",24,TEXT);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);top.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        Button more=secondary("•••");more.setOnClickListener(v->startActivity(new Intent(this,MainActivity.class)));top.addView(more,new LinearLayout.LayoutParams(dp(52),dp(40)));root.addView(top);root.addView(divider());
        ScrollView scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(16),dp(14),dp(16),dp(28));scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    }

    private void refresh(){
        if(content==null)return;content.removeAllViews();
        List<LifeDb.Loop> attention=db.openLoops(50);List<PersistentActionQueue.Item> actions=new PersistentActionQueue(this).pending();List<LifeDb.Event> events=db.recentEvents(30);
        hero(attention.size(),actions.size(),events.size());
        sectionHeader("Needs attention",attention.size(),"attention");
        if(attention.isEmpty())empty("Nothing currently needs attention.");else for(int i=0;i<Math.min(3,attention.size());i++)attentionCard(attention.get(i));
        sectionHeader("Suggested actions",actions.size(),"actions");
        if(actions.isEmpty())empty("No suggested actions waiting right now.");else for(int i=0;i<Math.min(3,actions.size());i++)actionCard(actions.get(i));
        sectionHeader("Today",events.size(),"activity");
        if(events.isEmpty())empty("No meaningful activity captured yet.");else for(int i=0;i<Math.min(5,events.size());i++)eventCard(events.get(i));
        navCard("People & Social Radar","Relationship signals, conversations and changes",LifeSignalsActivity.class);
        navCard("Decisions","Decision history, context and consequences",LifeSignalsActivity.class);
        navCard("Action history","Approvals and executed actions",ActionCenterActivity.class);
    }

    private void hero(int attention,int actions,int events){LinearLayout c=row();TextView h=text(attention==0?"You’re clear right now":attention+" things may need you",20,TEXT);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);c.addView(h);c.addView(meta(events+" meaningful recent items · "+actions+" suggested actions"));content.addView(c);}
    private void sectionHeader(String title,int count,String mode){LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);r.setPadding(dp(2),dp(18),dp(2),dp(8));TextView t=text(title,17,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);r.addView(t,new LinearLayout.LayoutParams(0,-2,1));TextView n=text(count+"  ›",13,MUTED);r.addView(n);r.setOnClickListener(v->openSection(mode));content.addView(r);}
    private void attentionCard(LifeDb.Loop x){LinearLayout c=row();c.addView(badge(x.kind.toUpperCase()));TextView t=text(x.title,15,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(0,dp(7),0,0);c.addView(t);LifeDb.Event e=db.eventById(x.evidenceId);if(e!=null)c.addView(meta(friendlyApp(e.app)+" · "+formatTime(e.at)));c.setOnClickListener(v->openEvidence(x.evidenceId,"attention",x.id));content.addView(c);}
    private void actionCard(PersistentActionQueue.Item x){LinearLayout c=row();c.addView(badge("SUGGESTED ACTION"));TextView t=text(x.proposal.payloadSummary.isEmpty()?x.proposal.actionType:x.proposal.payloadSummary,15,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(0,dp(7),0,0);c.addView(t);c.addView(meta(x.proposal.target));c.setOnClickListener(v->startActivity(new Intent(this,ActionCenterActivity.class)));content.addView(c);}
    private void eventCard(LifeDb.Event e){LinearLayout c=row();TextView t=text(blank(e.title)?friendlyApp(e.app):e.title,15,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);c.addView(t);String body=e.body==null?"":e.body;TextView b=text(body.length()>180?body.substring(0,180)+"…":body,13,TEXT);b.setPadding(0,dp(6),0,0);c.addView(b);c.addView(meta(friendlyApp(e.app)+" · "+formatTime(e.at)));c.setOnClickListener(v->openEvidence(e.id,"activity",0));content.addView(c);}
    private void navCard(String title,String subtitle,Class<?> cls){LinearLayout c=row();TextView t=text(title+"  ›",15,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);c.addView(t);c.addView(meta(subtitle));c.setOnClickListener(v->startActivity(new Intent(this,cls)));LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)c.getLayoutParams();p.setMargins(0,dp(12),0,dp(8));c.setLayoutParams(p);content.addView(c);}
    private void openSection(String mode){startActivity(new Intent(this,FeedSectionActivity.class).putExtra("mode",mode));}
    private void openEvidence(long eventId,String mode,long loopId){startActivity(new Intent(this,EvidenceDetailActivity.class).putExtra("event_id",eventId).putExtra("mode",mode).putExtra("loop_id",loopId));}
    private void empty(String s){TextView v=text(s,13,MUTED);v.setPadding(dp(4),dp(4),dp(4),dp(8));content.addView(v);}
    private LinearLayout row(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(13),dp(14),dp(13));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(8));c.setLayoutParams(p);c.setBackground(round(SURFACE,BORDER,9));return c;}
    private TextView badge(String s){TextView v=text(s,11,GREEN);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setPadding(dp(8),dp(4),dp(8),dp(4));v.setBackground(round(Color.TRANSPARENT,GREEN,20));return v;}
    private TextView meta(String s){TextView v=text(s,11,MUTED);v.setPadding(0,dp(7),0,0);return v;}
    private Button secondary(String s){Button b=new Button(this);b.setText(s);b.setTextColor(TEXT);b.setTextSize(13);b.setAllCaps(false);b.setBackground(round(SURFACE,BORDER,8));return b;}
    private View divider(){View v=new View(this);v.setBackgroundColor(BORDER);v.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(1)));return v;}
    private GradientDrawable round(int fill,int stroke,int radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));g.setStroke(dp(1),stroke);return g;}
    private TextView text(String s,int size,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setLineSpacing(0,1.08f);return v;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private static boolean blank(String x){return x==null||x.trim().isEmpty();}
    private static String friendlyApp(String p){if(p==null)return "Unknown";int i=p.lastIndexOf('.');String x=i>=0?p.substring(i+1):p;return x.isEmpty()?p:Character.toUpperCase(x.charAt(0))+x.substring(1);}
    private static String formatTime(long at){return DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(at));}
}
