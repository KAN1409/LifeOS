package com.kareem.lifeos;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Locked LifeOS visual language from the approved Now / Timeline / Search / Ask reference. */
final class LifeOsUi {
    static final int BG=Color.rgb(8,12,17), SURFACE=Color.rgb(18,24,31), SURFACE_2=Color.rgb(23,30,38), BORDER=Color.rgb(43,52,62);
    static final int TEXT=Color.rgb(238,243,248), MUTED=Color.rgb(145,154,166), BLUE=Color.rgb(54,139,255), GREEN=Color.rgb(59,190,96), RED=Color.rgb(245,74,85), AMBER=Color.rgb(225,163,57);
    private LifeOsUi(){}

    static int dp(Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    static TextView text(Context c,String s,int size,int color){TextView v=new TextView(c);v.setText(s==null?"":s);v.setTextSize(size);v.setTextColor(color);v.setLineSpacing(0,1.08f);return v;}
    static GradientDrawable round(Context c,int fill,int stroke,int radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(c,radius));g.setStroke(dp(c,1),stroke);return g;}
    static LinearLayout card(Context c){LinearLayout r=new LinearLayout(c);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(dp(c,14),dp(c,13),dp(c,14),dp(c,13));r.setBackground(round(c,SURFACE,BORDER,10));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(c,9));r.setLayoutParams(p);return r;}
    static TextView badge(Context c,String label,int color){TextView v=text(c,label==null?"":label.toUpperCase(),10,color);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setPadding(dp(c,8),dp(c,4),dp(c,8),dp(c,4));v.setBackground(round(c,Color.TRANSPARENT,color,20));v.setLayoutParams(new LinearLayout.LayoutParams(-2,-2));return v;}
    static Button button(Context c,String label){Button b=new Button(c);b.setText(label);b.setAllCaps(false);b.setTextSize(12);b.setTextColor(TEXT);b.setGravity(Gravity.CENTER);b.setBackground(round(c,SURFACE_2,BORDER,9));return b;}
    static Button primary(Context c,String label){Button b=button(c,label);b.setBackground(round(c,BLUE,BLUE,9));b.setTextColor(Color.WHITE);return b;}
    static TextView section(Context c,String label){TextView t=text(c,label,17,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(dp(c,1),dp(c,14),0,dp(c,9));return t;}
    static View divider(Context c){View v=new View(c);v.setBackgroundColor(BORDER);v.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(c,1)));return v;}

    static LinearLayout topBar(Activity a,String title,boolean settings){LinearLayout h=new LinearLayout(a);h.setGravity(Gravity.CENTER_VERTICAL);h.setPadding(dp(a,16),dp(a,12),dp(a,12),dp(a,10));h.setBackgroundColor(BG);TextView t=text(a,title,22,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);h.addView(t,new LinearLayout.LayoutParams(0,-2,1));if(settings){Button gear=button(a,"⚙");gear.setContentDescription("Settings and diagnostics");gear.setTextSize(18);gear.setBackgroundColor(Color.TRANSPARENT);gear.setOnClickListener(v->a.startActivity(new Intent(a,MainActivity.class)));h.addView(gear,new LinearLayout.LayoutParams(dp(a,48),dp(a,48)));}return h;}

    static LinearLayout bottomNav(Activity a,Class<?> active){LinearLayout wrap=new LinearLayout(a);wrap.setOrientation(LinearLayout.VERTICAL);wrap.setBackgroundColor(SURFACE);wrap.addView(divider(a));LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER);row.setPadding(dp(a,4),dp(a,3),dp(a,4),dp(a,4));row.addView(nav(a,"⌂","Now",active==FeedActivity.class,FeedActivity.class),new LinearLayout.LayoutParams(0,dp(a,58),1));row.addView(nav(a,"▤","Timeline",active==TimelineActivity.class,TimelineActivity.class),new LinearLayout.LayoutParams(0,dp(a,58),1));row.addView(nav(a,"⌕","Search",active==SearchActivity.class,SearchActivity.class),new LinearLayout.LayoutParams(0,dp(a,58),1));row.addView(nav(a,"◉","Ask",active==AskLifeOsActivity.class,AskLifeOsActivity.class),new LinearLayout.LayoutParams(0,dp(a,58),1));wrap.addView(row);return wrap;}
    private static TextView nav(Activity a,String icon,String label,boolean active,Class<?> target){TextView v=text(a,icon+"\n"+label,10,active?BLUE:MUTED);v.setGravity(Gravity.CENTER);v.setTypeface(Typeface.DEFAULT,active?Typeface.BOLD:Typeface.NORMAL);v.setLineSpacing(0,0.92f);v.setOnClickListener(x->go(a,target));return v;}
    static void go(Activity a,Class<?> target){if(a.getClass()==target)return;Intent i=new Intent(a,target);i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);a.startActivity(i);a.overridePendingTransition(0,0);}
}
