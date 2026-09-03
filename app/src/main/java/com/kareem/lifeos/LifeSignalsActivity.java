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
import com.kareem.lifeos.graphiti.GraphitiGateway;
import java.util.List;

/** GitHub-Mobile-style shell over the upstream Graphiti temporal graph service. */
public final class LifeSignalsActivity extends Activity {
    private static final int BG=Color.rgb(13,17,23),SURFACE=Color.rgb(22,27,34),BORDER=Color.rgb(48,54,61),TEXT=Color.rgb(230,237,243),MUTED=Color.rgb(139,148,158),GREEN=Color.rgb(63,185,80),BLUE=Color.rgb(47,129,247);
    private GraphitiGateway api; private LinearLayout content; private TextView connection; private Button social,decisions;
    @Override public void onCreate(Bundle state){super.onCreate(state);api=new GraphitiGateway(this);render();checkHealth();}
    private void render(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
        LinearLayout header=new LinearLayout(this);header.setOrientation(LinearLayout.VERTICAL);header.setPadding(dp(16),dp(16),dp(16),dp(12));header.setBackgroundColor(SURFACE);
        LinearLayout titleRow=new LinearLayout(this);titleRow.setGravity(Gravity.CENTER_VERTICAL);Button back=secondary("‹");back.setOnClickListener(v->finish());titleRow.addView(back,new LinearLayout.LayoutParams(dp(42),dp(40)));TextView title=text("Life Signals",21,TEXT);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,-2,1);tp.setMargins(dp(10),0,0,0);titleRow.addView(title,tp);Button server=secondary("Server");server.setOnClickListener(v->configure());titleRow.addView(server,new LinearLayout.LayoutParams(dp(84),dp(40)));header.addView(titleRow);
        connection=text("Graphiti · checking…",12,MUTED);connection.setPadding(0,dp(8),0,0);header.addView(connection);root.addView(header);root.addView(divider());
        LinearLayout tabs=new LinearLayout(this);tabs.setPadding(dp(8),0,dp(8),0);social=tab("Social Radar");decisions=tab("Decision Memory");tabs.addView(social,new LinearLayout.LayoutParams(0,dp(48),1));tabs.addView(decisions,new LinearLayout.LayoutParams(0,dp(48),1));root.addView(tabs);root.addView(divider());
        ScrollView scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(16),dp(14),dp(16),dp(24));scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        social.setOnClickListener(v->loadSocial());decisions.setOnClickListener(v->loadDecisions());setContentView(root);loadSocial();
    }
    private void configure(){EditText input=new EditText(this);input.setText(api.baseUrl());input.setTextColor(TEXT);input.setHintTextColor(MUTED);input.setSingleLine(true);input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);new AlertDialog.Builder(this).setTitle("Graphiti server").setMessage("Default: upstream Graphiti FastAPI service on this device.").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{api.setBaseUrl(input.getText().toString());checkHealth();}).show();}
    private void checkHealth(){new Thread(()->{try{boolean ok=api.health();runOnUiThread(()->{connection.setText(ok?"● Graphiti connected · "+api.baseUrl():"○ Graphiti unavailable · "+api.baseUrl());connection.setTextColor(ok?GREEN:MUTED);});}catch(Exception e){runOnUiThread(()->{connection.setText("○ Graphiti offline · "+api.baseUrl());connection.setTextColor(MUTED);});}},"graphiti-health").start();}
    private void loadSocial(){select(social);loading("Reading temporal relationship signals…");new Thread(()->{try{List<GraphitiGateway.Fact> xs=api.search("people relationships interactions follow-ups changes commitments communication",30);runOnUiThread(()->showFacts("Social Radar","Temporal relationship facts from Graphiti",xs));}catch(Exception e){runOnUiThread(()->error(e));}},"graphiti-social").start();}
    private void loadDecisions(){select(decisions);loading("Reading decision history…");new Thread(()->{try{List<GraphitiGateway.Fact> xs=api.search("decisions choices alternatives reasons consequences superseded changed mind",30);runOnUiThread(()->showFacts("Decision Memory","Decision facts and their temporal validity from Graphiti",xs));}catch(Exception e){runOnUiThread(()->error(e));}},"graphiti-decisions").start();}
    private void showFacts(String title,String sub,List<GraphitiGateway.Fact> xs){content.removeAllViews();section(title,sub);if(xs.isEmpty()){empty("No matching temporal facts returned yet.");return;}for(GraphitiGateway.Fact x:xs){LinearLayout r=row();TextView n=text(x.name==null||x.name.isEmpty()?"Fact":x.name,12,GREEN);n.setTypeface(Typeface.DEFAULT,Typeface.BOLD);r.addView(n);TextView f=text(x.fact,14,TEXT);f.setPadding(0,dp(6),0,0);r.addView(f);String when=x.validAt==null?x.createdAt:x.validAt;if(when!=null)r.addView(meta(when+(x.invalidAt==null?" · current":" · superseded "+x.invalidAt)));content.addView(r);}}
    private void loading(String message){content.removeAllViews();section("Life Signals",message);} private void error(Exception e){content.removeAllViews();section("Graphiti unavailable","The upstream temporal graph service could not answer.");empty(e.getMessage()==null?"Connection failed":e.getMessage());Button b=primary("Configure server");b.setOnClickListener(v->configure());content.addView(b,new LinearLayout.LayoutParams(-1,dp(44)));}
    private void section(String t,String s){TextView h=text(t,20,TEXT);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);content.addView(h);TextView sub=text(s,12,MUTED);sub.setPadding(0,dp(2),0,dp(12));content.addView(sub);} private void empty(String x){LinearLayout r=row();r.addView(text(x,14,MUTED));content.addView(r);}
    private LinearLayout row(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(13),dp(14),dp(13));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(8));c.setLayoutParams(p);c.setBackground(round(SURFACE,BORDER,8));return c;} private TextView meta(String x){TextView v=text(x,11,MUTED);v.setPadding(0,dp(8),0,0);return v;}
    private Button tab(String x){Button b=new Button(this);b.setText(x);b.setTextColor(MUTED);b.setTextSize(12);b.setAllCaps(false);b.setBackgroundColor(Color.TRANSPARENT);return b;} private void select(Button active){for(Button b:new Button[]{social,decisions}){b.setTextColor(b==active?TEXT:MUTED);b.setTypeface(Typeface.DEFAULT,b==active?Typeface.BOLD:Typeface.NORMAL);}}
    private Button secondary(String x){Button b=new Button(this);b.setText(x);b.setTextColor(TEXT);b.setTextSize(12);b.setAllCaps(false);b.setGravity(Gravity.CENTER);b.setBackground(round(SURFACE,BORDER,7));return b;} private Button primary(String x){Button b=secondary(x);b.setBackground(round(BLUE,BLUE,7));b.setTextColor(Color.WHITE);return b;}
    private View divider(){View v=new View(this);v.setBackgroundColor(BORDER);v.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(1)));return v;} private GradientDrawable round(int fill,int stroke,int radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));g.setStroke(dp(1),stroke);return g;} private TextView text(String x,int size,int color){TextView v=new TextView(this);v.setText(x);v.setTextSize(size);v.setTextColor(color);v.setLineSpacing(0,1.08f);return v;} private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
