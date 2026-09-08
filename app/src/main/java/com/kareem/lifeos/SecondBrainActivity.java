package com.kareem.lifeos;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.kareem.lifeos.secondbrain.SecondBrainGateway;
import com.kareem.lifeos.actions.PersistentActionQueue;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/** GitHub-Mobile-style shell over the upstream SecondBrain HTTP API. */
public final class SecondBrainActivity extends Activity {
    private static final int BG=Color.rgb(13,17,23),SURFACE=Color.rgb(22,27,34),BORDER=Color.rgb(48,54,61),TEXT=Color.rgb(230,237,243),MUTED=Color.rgb(139,148,158),GREEN=Color.rgb(63,185,80),BLUE=Color.rgb(47,129,247),RED=Color.rgb(248,81,73);
    private SecondBrainGateway api; private LifeDb db; private LinearLayout content; private TextView connection; private Button timeline,people,attention,briefing;private volatile boolean remoteAvailable;private boolean advanced;

    @Override public void onCreate(Bundle state){super.onCreate(state);advanced=getIntent().getBooleanExtra("advanced",false);api=new SecondBrainGateway(this);db=new LifeDb(this);render();checkHealth();}
    @Override protected void onDestroy(){db.close();super.onDestroy();}

    private void render(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
        LinearLayout header=new LinearLayout(this);header.setOrientation(LinearLayout.VERTICAL);header.setPadding(dp(16),dp(16),dp(16),dp(12));header.setBackgroundColor(SURFACE);
        LinearLayout titleRow=new LinearLayout(this);titleRow.setGravity(Gravity.CENTER_VERTICAL);Button back=secondary("‹");back.setContentDescription("Back");back.setOnClickListener(v->finish());titleRow.addView(back,new LinearLayout.LayoutParams(dp(48),dp(48)));TextView title=text("Life Intelligence",21,TEXT);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);LinearLayout.LayoutParams titleP=new LinearLayout.LayoutParams(0,-2,1);titleP.setMargins(dp(10),0,0,0);titleRow.addView(title,titleP);if(advanced){Button endpoint=secondary("Server");endpoint.setOnClickListener(v->configureEndpoint());titleRow.addView(endpoint,new LinearLayout.LayoutParams(dp(84),dp(48)));}header.addView(titleRow);
        connection=text("Local intelligence active",12,GREEN);connection.setPadding(0,dp(8),0,0);header.addView(connection);root.addView(header);root.addView(divider());

        LinearLayout tabs=new LinearLayout(this);tabs.setPadding(dp(6),0,dp(6),0);tabs.setBackgroundColor(BG);
        timeline=tab("Timeline");people=tab("People");attention=tab("Attention");briefing=tab("Briefing");
        for(Button b:new Button[]{timeline,people,attention,briefing})tabs.addView(b,new LinearLayout.LayoutParams(0,dp(48),1));root.addView(tabs);root.addView(divider());
        ScrollView scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(16),dp(14),dp(16),dp(24));scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        timeline.setOnClickListener(v->loadTimeline());people.setOnClickListener(v->loadPeople());attention.setOnClickListener(v->loadAttention());briefing.setOnClickListener(v->loadBriefing());setContentView(root);loadTimeline();
    }

    private void configureEndpoint(){
        EditText input=new EditText(this);input.setText(api.baseUrl());input.setSingleLine(true);input.setTextColor(TEXT);input.setHintTextColor(MUTED);input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);input.setPadding(dp(16),dp(10),dp(16),dp(10));
        new AlertDialog.Builder(this).setTitle("SecondBrain server").setMessage("The default is the upstream gateway on this device. HTTPS remote endpoints are also supported.").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{api.setBaseUrl(input.getText().toString());checkHealth();}).show();
    }

    private void checkHealth(){new Thread(()->{try{remoteAvailable=api.health();}catch(Exception ignored){remoteAvailable=false;}runOnUiThread(()->{connection.setText(advanced?(remoteAvailable?"Enhanced memory connected · "+api.baseUrl():"Local mode · enhanced memory offline"):remoteAvailable?"Local intelligence · enhanced memory connected":"Local intelligence active");connection.setTextColor(GREEN);});},"secondbrain-health").start();}

    private void loadTimeline(){select(timeline);content.removeAllViews();section("Timeline","Recent evidence captured on this device");List<LifeDb.Event> xs=db.recentEvents(40);if(xs.isEmpty()){empty("No recent evidence has been captured yet.");return;}for(LifeDb.Event x:xs){LinearLayout r=row();TextView title=text((x.title==null||x.title.trim().isEmpty()?LifeDb.friendlyApp(x.app):x.title)+"  ›",14,TEXT);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);r.addView(title);TextView body=text(clip(x.body,180),13,TEXT);body.setPadding(0,dp(6),0,0);body.setMaxLines(3);r.addView(body);r.addView(meta(LifeDb.friendlyApp(x.app)));r.setOnClickListener(v->startActivity(new Intent(this,EvidenceDetailActivity.class).putExtra("event_id",x.id).putExtra("mode","timeline")));content.addView(r);}}
    private void showTimeline(List<SecondBrainGateway.TimelineEvent> xs){content.removeAllViews();section("Timeline","Upstream SecondBrain temporal memory · last 7 days");if(xs.isEmpty()){empty("No temporal memories returned yet.");return;}for(SecondBrainGateway.TimelineEvent x:xs){LinearLayout r=row();TextView v=text(x.content,14,TEXT);r.addView(v);r.addView(meta((x.validFrom==null?"time unknown":x.validFrom)+" · importance "+Math.round(x.importance*100)+"%"));content.addView(r);}}

    private void askPerson(){select(people);EditText input=new EditText(this);input.setTextColor(TEXT);input.setHintTextColor(MUTED);input.setHint("Name");input.setSingleLine(true);new AlertDialog.Builder(this).setTitle("Who?").setMessage("Uses SecondBrain /who — facts are resolved by the upstream person graph.").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Open",(d,w)->loadPerson(input.getText().toString().trim())).show();}
    private void loadPeople(){select(people);content.removeAllViews();section("People","Recent person-level conversation history");List<LifeDb.Conversation> xs=db.recentConversations(40);if(xs.isEmpty()){empty("No people have been resolved from captured conversations yet.");return;}for(LifeDb.Conversation x:xs){LinearLayout r=row();TextView name=text(x.label+"  ›",14,TEXT);name.setTypeface(Typeface.DEFAULT,Typeface.BOLD);r.addView(name);r.addView(meta(x.count+" captured interactions · "+LifeDb.friendlyApp(x.app)));r.setOnClickListener(v->startActivity(new Intent(this,PersonDetailActivity.class).putExtra("event_id",x.latestEventId)));content.addView(r);}}
    private void loadPerson(String name){if(name.isEmpty())return;loading("Building person card…");new Thread(()->{try{SecondBrainGateway.PersonCard p=api.who(name);runOnUiThread(()->{content.removeAllViews();section(p.name,"Person · "+(p.personId==null?"unresolved":p.personId));if(p.facts.isEmpty()){empty("No facts returned for this person.");return;}for(SecondBrainGateway.TimelineEvent x:p.facts){LinearLayout r=row();r.addView(text(x.content,14,TEXT));r.addView(meta(x.validFrom==null?"time unknown":x.validFrom));content.addView(r);}});}catch(Exception e){runOnUiThread(()->error(e));}},"secondbrain-who").start();}

    private void loadAttention(){select(attention);content.removeAllViews();section("Attention","Ranked unresolved items from grounded evidence");List<LifeDb.Loop> xs=db.openLoops(100);if(xs.isEmpty()){empty("Nothing needs attention right now.");return;}for(LifeDb.Loop x:xs){LinearLayout r=row();r.addView(badge(x.kind.replace('_',' ').toUpperCase()));TextView v=text(x.title+"  ›",14,TEXT);v.setPadding(0,dp(7),0,0);r.addView(v);r.setOnClickListener(view->startActivity(new Intent(this,EvidenceDetailActivity.class).putExtra("event_id",x.evidenceId).putExtra("loop_id",x.id).putExtra("mode","attention")));content.addView(r);}}

    private void loadBriefing(){select(briefing);content.removeAllViews();List<LifeDb.Loop> loops=db.openLoops(100);List<ProactiveFeedEngine.RankedConversation> ranked=ProactiveFeedEngine.rank(db,db.recentConversations(40),System.currentTimeMillis());ProactiveFeedEngine.DaySummary day=ProactiveFeedEngine.daySummary(ranked,loops,new PersistentActionQueue(this).pending().size());section("Daily briefing",day.headline+" · "+day.detail);List<SituationEngine.Situation> situations=SituationEngine.build(db,loops,new PersistentActionQueue(this).pending(),System.currentTimeMillis());if(situations.isEmpty()){empty("No unresolved situation needs you right now.");return;}for(int i=0;i<Math.min(5,situations.size());i++){SituationEngine.Situation x=situations.get(i);LinearLayout r=row();r.addView(badge(x.status));TextView title=text(x.title+"  ›",14,TEXT);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);r.addView(title);r.addView(meta(x.summary));r.setOnClickListener(v->startActivity(new Intent(this,SituationDetailActivity.class).putExtra("situation_id",x.id)));content.addView(r);}}

    private void group(String title,List<String> xs){if(xs==null||xs.isEmpty())return;TextView h=text(title,13,MUTED);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);h.setPadding(0,dp(8),0,dp(6));content.addView(h);for(String x:xs){LinearLayout r=row();r.addView(text(x,14,TEXT));content.addView(r);}}
    private void loading(String message){content.removeAllViews();section("Life Intelligence",message);}
    private void error(Exception e){content.removeAllViews();section("SecondBrain unavailable","The upstream service could not answer this request.");empty(e.getMessage()==null?"Connection failed":e.getMessage());Button config=primary("Configure server");config.setOnClickListener(v->configureEndpoint());content.addView(config,new LinearLayout.LayoutParams(-1,dp(48)));}
    private void section(String title,String sub){TextView t=text(title,20,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);content.addView(t);TextView s=text(sub,12,MUTED);s.setPadding(0,dp(2),0,dp(12));content.addView(s);}
    private void empty(String x){LinearLayout r=row();r.addView(text(x,14,MUTED));content.addView(r);}
    private LinearLayout row(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(13),dp(14),dp(13));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(8));c.setLayoutParams(p);c.setBackground(round(SURFACE,BORDER,8));return c;}
    private TextView badge(String x){TextView v=text(x,11,GREEN);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setPadding(dp(8),dp(4),dp(8),dp(4));v.setLayoutParams(new LinearLayout.LayoutParams(-2,-2));v.setBackground(round(Color.TRANSPARENT,GREEN,20));return v;}
    private TextView meta(String x){TextView v=text(x,11,MUTED);v.setPadding(0,dp(8),0,0);return v;}
    private Button tab(String x){Button b=new Button(this);b.setText(x);b.setTextColor(MUTED);b.setTextSize(11);b.setAllCaps(false);b.setBackgroundColor(Color.TRANSPARENT);return b;}
    private void select(Button active){for(Button b:new Button[]{timeline,people,attention,briefing}){b.setTextColor(b==active?TEXT:MUTED);b.setTypeface(Typeface.DEFAULT,b==active?Typeface.BOLD:Typeface.NORMAL);}}
    private Button secondary(String x){Button b=new Button(this);b.setText(x);b.setTextColor(TEXT);b.setTextSize(12);b.setAllCaps(false);b.setGravity(Gravity.CENTER);b.setBackground(round(SURFACE,BORDER,7));return b;}
    private Button primary(String x){Button b=secondary(x);b.setBackground(round(BLUE,BLUE,7));b.setTextColor(Color.WHITE);return b;}
    private View divider(){View v=new View(this);v.setBackgroundColor(BORDER);v.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(1)));return v;}
    private GradientDrawable round(int fill,int stroke,int radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));g.setStroke(dp(1),stroke);return g;}
    private TextView text(String x,int size,int color){TextView v=new TextView(this);v.setText(x);v.setTextSize(size);v.setTextColor(color);v.setLineSpacing(0,1.08f);return v;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private static String clip(String value,int max){String x=value==null?"":value.trim();return x.length()>max?x.substring(0,max)+"…":x;}
}
