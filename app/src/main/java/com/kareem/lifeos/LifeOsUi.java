package com.kareem.lifeos;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Locked LifeOS visual language from the approved Now / Timeline / Search / Ask reference. */
final class LifeOsUi {
    static final int BG=Color.rgb(7,11,16),SURFACE=Color.rgb(17,23,30),SURFACE_2=Color.rgb(22,29,37),BORDER=Color.rgb(42,51,61);
    static final int TEXT=Color.rgb(239,244,249),MUTED=Color.rgb(143,153,165),BLUE=Color.rgb(52,139,255),GREEN=Color.rgb(58,188,96),RED=Color.rgb(244,73,86),AMBER=Color.rgb(225,163,57),PURPLE=Color.rgb(151,91,255);
    private LifeOsUi(){}

    static int dp(Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    static TextView text(Context c,String s,int size,int color){TextView v=new TextView(c);v.setText(s==null?"":s);v.setTextSize(size);v.setTextColor(color);v.setLineSpacing(0,1.04f);return v;}
    static GradientDrawable round(Context c,int fill,int stroke,int radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(c,radius));if(stroke!=Color.TRANSPARENT)g.setStroke(dp(c,1),stroke);return g;}
    static LinearLayout card(Context c){LinearLayout r=new LinearLayout(c);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(dp(c,11),dp(c,10),dp(c,11),dp(c,10));r.setBackground(round(c,SURFACE,BORDER,9));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(c,7));r.setLayoutParams(p);return r;}
    static TextView badge(Context c,String label,int color){TextView v=text(c,label==null?"":label.toUpperCase(),9,color);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setPadding(dp(c,7),dp(c,3),dp(c,7),dp(c,3));v.setBackground(round(c,Color.TRANSPARENT,color,18));v.setLayoutParams(new LinearLayout.LayoutParams(-2,-2));return v;}
    static Button button(Context c,String label){Button b=new Button(c);b.setText(label);b.setAllCaps(false);b.setTextSize(11);b.setTextColor(TEXT);b.setGravity(Gravity.CENTER);b.setMinHeight(0);b.setMinimumHeight(0);b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(dp(c,10),0,dp(c,10),0);b.setBackground(round(c,SURFACE_2,BORDER,10));return b;}
    static Button primary(Context c,String label){Button b=button(c,label);b.setBackground(round(c,BLUE,BLUE,10));b.setTextColor(Color.WHITE);return b;}
    static Button chip(Context c,String label,boolean active){Button b=button(c,label);b.setTextSize(10);b.setPadding(dp(c,13),0,dp(c,13),0);b.setBackground(round(c,active?BLUE:SURFACE_2,active?BLUE:BORDER,18));b.setTextColor(active?Color.WHITE:TEXT);return b;}
    static TextView section(Context c,String label){TextView t=text(c,label,16,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(dp(c,1),dp(c,12),0,dp(c,7));return t;}
    static View divider(Context c){View v=new View(c);v.setBackgroundColor(BORDER);v.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(c,1)));return v;}

    static LinearLayout topBar(Activity a,String title,boolean settings){LinearLayout h=new LinearLayout(a);h.setGravity(Gravity.CENTER_VERTICAL);h.setPadding(dp(a,16),dp(a,9),dp(a,10),dp(a,7));h.setBackgroundColor(BG);TextView t=text(a,title,21,TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);h.addView(t,new LinearLayout.LayoutParams(0,-2,1));if(settings){Button gear=iconButton(a,"⚙");gear.setContentDescription("Settings and diagnostics");gear.setOnClickListener(v->a.startActivity(new Intent(a,MainActivity.class)));h.addView(gear,new LinearLayout.LayoutParams(dp(a,42),dp(a,42)));}return h;}
    static Button iconButton(Context c,String glyph){Button b=button(c,glyph);b.setTextSize(18);b.setPadding(0,0,0,0);b.setBackgroundColor(Color.TRANSPARENT);return b;}
    static TextView avatar(Context c,String initial){TextView v=text(c,initial,13,TEXT);v.setGravity(Gravity.CENTER);v.setBackground(round(c,SURFACE_2,BORDER,40));return v;}

    static View appIcon(Context c,String packageName,int size){try{Drawable d=c.getPackageManager().getApplicationIcon(packageName);ImageView v=new ImageView(c);v.setImageDrawable(d);v.setScaleType(ImageView.ScaleType.CENTER_CROP);v.setBackground(round(c,SURFACE_2,Color.TRANSPARENT,8));v.setPadding(dp(c,1),dp(c,1),dp(c,1),dp(c,1));v.setLayoutParams(new LinearLayout.LayoutParams(dp(c,size),dp(c,size)));return v;}catch(Throwable ignored){TextView v=text(c,"•",20,BLUE);v.setGravity(Gravity.CENTER);v.setBackground(round(c,SURFACE_2,BORDER,8));v.setLayoutParams(new LinearLayout.LayoutParams(dp(c,size),dp(c,size)));return v;}}
    static TextView categoryIcon(Context c,String glyph,int color,int size){TextView v=text(c,glyph,16,color);v.setGravity(Gravity.CENTER);v.setBackground(round(c,withAlpha(color,34),Color.TRANSPARENT,8));v.setLayoutParams(new LinearLayout.LayoutParams(dp(c,size),dp(c,size)));return v;}
    static int withAlpha(int color,int alpha){return Color.argb(alpha,Color.red(color),Color.green(color),Color.blue(color));}

    static View orb(Context c){return new View(c){final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);@Override protected void onDraw(Canvas canvas){super.onDraw(canvas);float cx=getWidth()/2f,cy=getHeight()/2f,r=Math.min(getWidth(),getHeight())*.29f;setLayerType(View.LAYER_TYPE_SOFTWARE,null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(getContext(),3));p.setColor(BLUE);p.setShadowLayer(dp(getContext(),18),0,0,withAlpha(BLUE,170));canvas.drawCircle(cx,cy,r,p);p.clearShadowLayer();p.setStrokeWidth(dp(getContext(),2));p.setColor(PURPLE);canvas.drawArc(cx-r,cy-r,cx+r,cy+r,300,105,false,p);}};}

    static LinearLayout bottomNav(Activity a,Class<?> active){LinearLayout wrap=new LinearLayout(a);wrap.setOrientation(LinearLayout.VERTICAL);wrap.setBackgroundColor(SURFACE);wrap.addView(divider(a));LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER);row.setPadding(dp(a,3),dp(a,2),dp(a,3),dp(a,3));row.addView(nav(a,"⌂","Now",active==FeedActivity.class,FeedActivity.class),new LinearLayout.LayoutParams(0,dp(a,54),1));row.addView(nav(a,"▤","Timeline",active==TimelineActivity.class,TimelineActivity.class),new LinearLayout.LayoutParams(0,dp(a,54),1));row.addView(nav(a,"⌕","Search",active==SearchActivity.class,SearchActivity.class),new LinearLayout.LayoutParams(0,dp(a,54),1));row.addView(nav(a,"◉","Ask",active==AskLifeOsActivity.class,AskLifeOsActivity.class),new LinearLayout.LayoutParams(0,dp(a,54),1));wrap.addView(row);return wrap;}
    private static TextView nav(Activity a,String icon,String label,boolean active,Class<?> target){TextView v=text(a,icon+"\n"+label,9,active?BLUE:MUTED);v.setGravity(Gravity.CENTER);v.setTypeface(Typeface.DEFAULT,active?Typeface.BOLD:Typeface.NORMAL);v.setLineSpacing(0,0.88f);v.setOnClickListener(x->go(a,target));return v;}
    static void go(Activity a,Class<?> target){if(a.getClass()==target)return;Intent i=new Intent(a,target);i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);a.startActivity(i);a.overridePendingTransition(0,0);}
}
