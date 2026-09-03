package com.kareem.lifeos;

import android.app.Activity;
import android.app.AlertDialog;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/** GitHub-Mobile-style shell over the upstream SecondBrain HTTP API. */
public final class SecondBrainActivity extends Activity {
    private static final int BG=Color.rgb(13,17,23),SURFACE=Color.rgb(22,27,34),BORDER=Color.rgb(48,54,61),TEXT=Color.rgb(230,237,243),MUTED=Color.rgb(139,148,158),GREEN=Color.rgb(63,185,80),BLUE=Color.rgb(47,129,247),RED=Color.rgb(248,81,73);
    private SecondBrainGateway api; private LinearLayout content; private TextView connection; private Button timeline,people,attention,briefing;

    @Override public void onCreate(Bundle state){super.onCreate(state);api=new SecondBrainGateway(this);render();checkHealth();}

    private void render(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
        LinearLayout header=new LinearLayout(this);header.setOrientation(LinearLayout.VERTICAL);header.setPadding(dp(16),dp(16),dp(16),dp(12));header.setBackgroundColor(SURFACE);
        LinearLayout titleRow=new LinearLayout(this);titleRow.setGravity(Gravity.CENTER_VERTICAL);Button back=secondary("‹");back.setOnClickListener(v->finish());titleRow.addView(back,new LinearLayout.LayoutParams(dp(42),dp(40)));TextView title=text("Life Intelligence",21,TEXT);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);LinearLayout.LayoutParams titleP=new LinearLayout.LayoutParams(0,-2,1);titleP.setMargins(dp(10),0,0,0);titleRow.addView(title,titleP);Button endpoint=secondary("Server");endpoint.setOnClickListener(v->configureEndpoint());titleRow.addView(endpoint,new LinearLayout.LayoutParams(dp(84),dp(40)));header.addView(titleRow);
        connection=text("SecondBrain · checking…",12,MUTED);connection.setPadding(0,dp(8),0,0);header.addView(connection);root.addView(header);root.addView(divider());

        LinearLayout tabs=new LinearLayout(this);tabs.setPadding(dp(6),0,dp(6),0);tabs.setBackgroundColor(BG);
        timeline=tab("Timeline");people=tab("People");attention=tab("Attention");briefing=tab("Briefing");
        for(Button b:new Button[]{timeline,people,attention,briefing})tabs.addView(b,new LinearLayout.LayoutParams(0,dp(48),1));root.addView(tabs);root.addView(divider());
        ScrollView scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(16),dp(14),dp(16),dp(24));scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        timeline.setOnClickListener(v->loadTimeline());people.setOnClickListener(v->askPerson());attention.setOnClickListener(v->loadAttention());briefing.setOnClickListener(v->loadBriefing());setContentView(root);loadTimeline();
    }

    private void configureEndpoint(){
        EditText input=new EditText(this);input.setText(api.baseUrl());input.setSingleLine(true);input.setTextColor(TEXT);input.setHintTextColor(MUTED);input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);input.setPadding(dp(16),dp(10),dp(16),dp(10));
        new AlertDialog.Builder(this).setTitle("SecondBrain server").setMessage("The default is the upstream gateway on this device. HTTPS remote endpoints are also supported.").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{api.setBaseUrl(input.getText().toString());checkHealth();}).show();
    }

    private void checkHealth(){new Thread(()->{try{boolean ok=api.health();runOnUiThread(()->{connection.setText(ok?"● SecondBrain connected · "+api.baseUrl():"○ SecondBrain unavailable · "+api.baseUrl());connection.setTextColor(ok?GREEN:MUTED);});}catch(Exception e){runOnUiThread(()->{connection.setText("○ SecondBrain offline · "+api.baseUrl());connection.setTextColor(MUTED);});}},"secondbrain-health").start();}

    private void loadTimeline(){select(timeline);loading("Loading temporal memory…");new Thread(()->{try{ZoneId z=ZoneId.systemDefault();Instant end=Instant.now();Instant start=end.minusSeconds(7L*24L*3600L);List<SecondBrainGateway.TimelineEvent> xs=api.timeline(start.toString(),end.toString());runOnUiThread(()->showTimeline(xs));}catch(Exception e){runOnUiThread(()->error(e));}},"secondbrain-timeline").start();}
    private void showTimeline(List<SecondBrainGateway.TimelineEvent> xs){content.removeAllViews();section("Timeline","Upstream SecondBrain temporal memory · last 7 days");if(xs.isEmpty()){empty("No temporal memories returned yet.");return;}for(SecondBrainGateway.TimelineEvent x:xs){LinearLayout r=row();TextView v=text(x.content,14,TEXT);r.addView(v);r.addView(meta((x.validFrom==null?"time unknown":x.validFrom)+" · importance "+Math.round(x.importance*100)+"%"));content.addView(r);}}

    private void askPerson(){select(people);EditText input=new EditText(this);input.setTextColor(TEXT);input.setHintTextColor(MUTED);input.setHint("Name");input.setSingleLine(true);new AlertDialog.Builder(this).setTitle("Who?").setMessage("Uses SecondBrain /who — facts are resolved by the upstream person graph.").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Open",(d,w)->loadPerson(input.getText().toString().trim())).show();}
    private void loadPerson(String name){if(name.isEmpty())return;loading("Building person card…");new Thread(()->{try{SecondBrainGateway.PersonCard p=api.who(name);runOnUiThread(()->{content.removeAllViews();section(p.name,"Person · "+(p.personId==null?"unresolved":p.personId));if(p.facts.isEmpty()){empty("No facts returned for this person.");return;}for(SecondBrainGateway.TimelineEvent x:p.facts){LinearLayout r=row();r.addView(text(x.content,14,TEXT));r.addView(meta(x.validFrom==null?"time unknown":x.validFrom));content.addView(r);}});}catch(Exception e){runOnUiThread(()->error(e));}},"secondbrain-who").start();}

    private void loadAttention(){select(attention);loading("Loading open commitments…");new Thread(()->{try{List<SecondBrainGateway.Commitment> xs=api.commitments("open");runOnUiThread(()->{content.removeAllViews();section("Attention","Open commitments from SecondBrain");if(xs.isEmpty()){empty("No open commitments returned.");return;}for(SecondBrainGateway.Commitment x:xs){LinearLayout r=row();r.addView(badge(x.status.toUpperCase()));TextView v=text(x.content,14,TEXT);v.setPadding(0,dp(7),0,0);r.addView(v);if(x.dueAt!=null)r.addView(meta("Due "+x.dueAt));content.addView(r);}});}catch(Exception e){runOnUiThread(()->error(e));}},"secondbrain-commitments").start();}

    private void loadBriefing(){select(briefing);loading("Building today's digest…");new Thread(()->{try{SecondBrainGateway.Digest d=api.digest(LocalDate.now().toString(),"day");runOnUiThread(()->{content.removeAllViews();section("Daily briefing","SecondBrain digest · evidence-backed");group("Themes",d.themes);group("Broken promises",d.brokenPromises);group("Suggested follow-ups",d.suggestedFollowups);if(d.themes.isEmpty()&&d.brokenPromises.isEmpty()&&d.suggestedFollowups.isEmpty())empty("No briefing signals returned yet.");});}catch(Exception e){runOnUiThread(()->error(e));}},"secondbrain-digest").start();}

    private void group(String title,List<String> xs){if(xs==null||xs.isEmpty())return;TextView h=text(title,13,MUTED);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);h.setPadding(0,dp(8),0,dp(6));content.addView(h);for(String x:xs){LinearLayout r=row();r.addView(text(x,14,TEXT));content.addView(r);}}
    private void loading(String message){content.removeAllViews();section("Life Intelligence",message);}
    private void error(Exception e){content.removeAllViews();section("SecondBrain unavailable","The upstream service could not answer this request.");empty(e.getMessage()==null?"Connection failed":e.getMessage());Button config=primary("Configure server");config.setOnClickListener(v->configureEndpoint());content.addView(config,new LinearLayout.LayoutParams(-1,dp(44)));}
    private void section(String title,String sub){TextView t=text(title,20,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);content.addView(t);TextView s=text(sub,12,MUTED);s.setPadding(0,dp(2),0,dp(12));content.addView(s);}
    private void empty(String x){LinearLayout r=row();r.addView(text(x,14,MUTED));content.addView(r);}
    private LinearLayout row(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(13),dp(14),dp(13));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(8));c.setLayoutParams(p);c.setBackground(round(SURFACE,BORDER,8));return c;}
    private TextView badge(String x){TextView v=text(x,11,GREEN);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setPadding(dp(8),dp(4),dp(8),dp(4));v.setBackground(round(Color.TRANSPARENT,GREEN,20));return v;}
    private TextView meta(String x){TextView v=text(x,11,MUTED);v.setPadding(0,dp(8),0,0);return v;}
    private Button tab(String x){Button b=new Button(this);b.setText(x);b.setTextColor(MUTED);b.setTextSize(11);b.setAllCaps(false);b.setBackgroundColor(Color.TRANSPARENT);return b;}
    private void select(Button active){for(Button b:new Button[]{timeline,people,attention,briefing}){b.setTextColor(b==active?TEXT:MUTED);b.setTypeface(Typeface.DEFAULT,b==active?Typeface.BOLD:Typeface.NORMAL);}}
    private Button secondary(String x){Button b=new Button(this);b.setText(x);b.setTextColor(TEXT);b.setTextSize(12);b.setAllCaps(false);b.setGravity(Gravity.CENTER);b.setBackground(round(SURFACE,BORDER,7));return b;}
    private Button primary(String x){Button b=secondary(x);b.setBackground(round(BLUE,BLUE,7));b.setTextColor(Color.WHITE);return b;}
    private View divider(){View v=new View(this);v.setBackgroundColor(BORDER);v.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(1)));return v;}
    private GradientDrawable round(int fill,int stroke,int radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));g.setStroke(dp(1),stroke);return g;}
    private TextView text(String x,int size,int color){TextView v=new TextView(this);v.setText(x);v.setTextSize(size);v.setTextColor(color);v.setLineSpacing(0,1.08f);return v;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
