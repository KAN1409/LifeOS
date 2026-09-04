package com.kareem.lifeos;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;

public final class SearchActivity extends Activity {
    private LinearLayout content;private EditText query;private String filter="All";
    @Override public void onCreate(Bundle s){super.onCreate(s);render();}
    private void render(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(LifeOsUi.BG);root.addView(LifeOsUi.topBar(this,"Search",true));query=new EditText(this);query.setHint("Search your life…");query.setHintTextColor(LifeOsUi.MUTED);query.setTextColor(LifeOsUi.TEXT);query.setSingleLine(true);query.setTextSize(14);query.setImeOptions(EditorInfo.IME_ACTION_SEARCH);query.setPadding(LifeOsUi.dp(this,14),0,LifeOsUi.dp(this,14),0);query.setBackground(LifeOsUi.round(this,LifeOsUi.SURFACE_2,LifeOsUi.BORDER,12));LinearLayout.LayoutParams qp=new LinearLayout.LayoutParams(-1,LifeOsUi.dp(this,48));qp.setMargins(LifeOsUi.dp(this,16),0,LifeOsUi.dp(this,16),LifeOsUi.dp(this,10));root.addView(query,qp);LinearLayout chips=new LinearLayout(this);chips.setPadding(LifeOsUi.dp(this,12),0,LifeOsUi.dp(this,12),LifeOsUi.dp(this,8));for(String x:new String[]{"All","People","Memory","Decision","Attention"}){Button b=LifeOsUi.button(this,x);b.setTextSize(10);b.setOnClickListener(v->{filter=x;runSearch();});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,LifeOsUi.dp(this,36),1);p.setMargins(LifeOsUi.dp(this,3),0,LifeOsUi.dp(this,3),0);chips.addView(b,p);}root.addView(chips);ScrollView sc=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(LifeOsUi.dp(this,16),LifeOsUi.dp(this,5),LifeOsUi.dp(this,16),LifeOsUi.dp(this,18));sc.addView(content);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));root.addView(LifeOsUi.bottomNav(this,SearchActivity.class));setContentView(root);query.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){runSearch();}public void afterTextChanged(Editable e){}});showSuggestions();}
    private void showSuggestions(){content.removeAllViews();TextView h=LifeOsUi.section(this,"Suggested");content.addView(h);for(String s:new String[]{"Open commitments","Who is waiting on me?","School fees","What did I decide about the car?"}){Button b=LifeOsUi.button(this,s+"  ›");b.setOnClickListener(v->query.setText(s));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,LifeOsUi.dp(this,50));p.setMargins(0,0,0,LifeOsUi.dp(this,8));content.addView(b,p);}}
    private void runSearch(){String q=query.getText().toString().trim();if(q.isEmpty()){showSuggestions();return;}content.removeAllViews();List<LifeIntelligenceEngine.Result> xs=LifeIntelligenceEngine.search(this,q,40);int shown=0;for(LifeIntelligenceEngine.Result r:xs){if(!"All".equals(filter)&&!r.kind.equalsIgnoreCase(filter))continue;LinearLayout c=LifeOsUi.card(this);c.addView(LifeOsUi.badge(this,r.kind,LifeOsUi.BLUE));TextView t=LifeOsUi.text(this,r.title+"  ›",14,LifeOsUi.TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(0,LifeOsUi.dp(this,7),0,0);c.addView(t);TextView s=LifeOsUi.text(this,clip(r.summary,220),12,LifeOsUi.MUTED);s.setPadding(0,LifeOsUi.dp(this,5),0,0);s.setMaxLines(3);c.addView(s);if(r.eventId>0)c.setOnClickListener(v->startActivity(new Intent(this,EvidenceDetailActivity.class).putExtra("event_id",r.eventId)));content.addView(c);shown++;}if(shown==0)content.addView(LifeOsUi.text(this,"No grounded result found for this search yet.",14,LifeOsUi.MUTED));}
    private static String clip(String x,int n){String v=x==null?"":x.trim();return v.length()>n?v.substring(0,n)+"…":v;}
}
