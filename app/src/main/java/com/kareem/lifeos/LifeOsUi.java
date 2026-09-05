package com.kareem.lifeos;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Locked LifeOS visual language reconstructed from the approved golden reference. */
final class LifeOsUi {
    static final float TEXT_SCALE=1.06f;
    static final int BG=Color.rgb(7,11,16),SURFACE=Color.rgb(17,24,32),SURFACE_2=Color.rgb(28,37,48),BORDER=Color.rgb(42,52,63);
    static final int TEXT=Color.rgb(238,242,246),MUTED=Color.rgb(145,154,166),TERTIARY=Color.rgb(105,116,130);
    static final int BLUE=Color.rgb(50,136,255),GREEN=Color.rgb(67,190,104),RED=Color.rgb(240,76,89),AMBER=Color.rgb(232,165,43),PURPLE=Color.rgb(138,96,255),PINK=Color.rgb(239,76,143);
    private LifeOsUi(){}

    static int dp(Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    static float refSp(float n){return n*TEXT_SCALE;}
    static TextView text(Context c,String s,float size,int color){TextView v=new TextView(c);v.setText(s==null?"":s);v.setTextSize(refSp(size));v.setTextColor(color);v.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));v.setLineSpacing(0,1.025f);v.setIncludeFontPadding(false);return v;}
    static void weight(TextView v,int weight){if(v==null)return;if(weight>=700)v.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));else if(weight>=500)v.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));else v.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));}
    static GradientDrawable round(Context c,int fill,int stroke,int radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(c,radius));if(stroke!=Color.TRANSPARENT)g.setStroke(dp(c,1),stroke);return g;}
    static LinearLayout card(Context c){LinearLayout r=new LinearLayout(c);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(dp(c,12),dp(c,10),dp(c,12),dp(c,10));r.setBackground(round(c,SURFACE,BORDER,14));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(c,6));r.setLayoutParams(p);return r;}
    static TextView badge(Context c,String label,int color){TextView v=text(c,label==null?"":label.toUpperCase(),8.8f,color);weight(v,700);v.setPadding(dp(c,7),dp(c,3),dp(c,7),dp(c,3));v.setBackground(round(c,Color.TRANSPARENT,color,18));v.setLayoutParams(new LinearLayout.LayoutParams(-2,-2));return v;}
    static Button button(Context c,String label){Button b=new Button(c);b.setText(label);b.setAllCaps(false);b.setTextSize(refSp(11.5f));b.setTextColor(TEXT);b.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));b.setGravity(Gravity.CENTER);b.setMinHeight(0);b.setMinimumHeight(0);b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(dp(c,11),0,dp(c,11),0);b.setBackground(round(c,SURFACE_2,BORDER,13));b.setStateListAnimator(null);return b;}
    static Button primary(Context c,String label){Button b=button(c,label);b.setBackground(round(c,BLUE,BLUE,13));b.setTextColor(Color.WHITE);weight(b,600);return b;}
    static Button chip(Context c,String label,boolean active){Button b=button(c,label);b.setTextSize(refSp(10.2f));b.setPadding(dp(c,12),0,dp(c,12),0);b.setBackground(round(c,active?BLUE:SURFACE_2,active?BLUE:BORDER,18));b.setTextColor(active?Color.WHITE:TEXT);b.setTypeface(Typeface.create(active?"sans-serif-medium":"sans-serif",Typeface.NORMAL));return b;}
    static TextView section(Context c,String label){TextView t=text(c,label,15.5f,TEXT);weight(t,700);t.setPadding(dp(c,1),dp(c,14),0,dp(c,7));return t;}
    static View divider(Context c){View v=new View(c);v.setBackgroundColor(BORDER);v.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(c,1)));return v;}

    static LinearLayout topBar(Activity a,String title,boolean settings){LinearLayout h=new LinearLayout(a);h.setGravity(Gravity.CENTER_VERTICAL);h.setPadding(dp(a,16),dp(a,8),dp(a,10),dp(a,7));h.setBackgroundColor(BG);TextView t=text(a,title,21.2f,TEXT);weight(t,700);h.addView(t,new LinearLayout.LayoutParams(0,-2,1));if(settings){View gear=iconTouch(a,LifeOsIconView.MORE,TEXT,22,42);gear.setContentDescription("Settings and diagnostics");gear.setOnClickListener(v->a.startActivity(new Intent(a,MainActivity.class)));h.addView(gear,new LinearLayout.LayoutParams(dp(a,42),dp(a,42)));}return h;}
    static Button iconButton(Context c,String glyph){Button b=button(c,glyph);b.setTextSize(refSp(17));b.setPadding(0,0,0,0);b.setBackgroundColor(Color.TRANSPARENT);return b;}
    static TextView avatar(Context c,String initial){TextView v=text(c,initial,12.5f,TEXT);weight(v,500);v.setGravity(Gravity.CENTER);v.setBackground(round(c,SURFACE_2,BORDER,100));return v;}

    static View icon(Context c,String name,int color,int visibleDp){LifeOsIconView v=new LifeOsIconView(c,name,color);v.setLayoutParams(new LinearLayout.LayoutParams(dp(c,visibleDp),dp(c,visibleDp)));return v;}
    static View iconTouch(Context c,String name,int color,int visibleDp,int touchDp){FrameLayout f=new FrameLayout(c);LifeOsIconView v=new LifeOsIconView(c,name,color);FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(dp(c,visibleDp),dp(c,visibleDp),Gravity.CENTER);f.addView(v,p);f.setBackgroundColor(Color.TRANSPARENT);f.setClickable(true);f.setFocusable(true);f.setLayoutParams(new LinearLayout.LayoutParams(dp(c,touchDp),dp(c,touchDp)));return f;}
    static FrameLayout iconTile(Context c,String name,int color,int size){FrameLayout f=new FrameLayout(c);f.setBackground(round(c,withAlpha(color,28),Color.TRANSPARENT,10));LifeOsIconView v=new LifeOsIconView(c,name,color);int glyph=Math.round(size*.57f);f.addView(v,new FrameLayout.LayoutParams(dp(c,glyph),dp(c,glyph),Gravity.CENTER));f.setLayoutParams(new LinearLayout.LayoutParams(dp(c,size),dp(c,size)));return f;}
    static FrameLayout categoryIcon(Context c,String legacy,int color,int size){return iconTile(c,legacyIcon(legacy),color,size);}
    static String legacyIcon(String s){if(s==null)return LifeOsIconView.ACTIVITY;if("!".equals(s))return LifeOsIconView.ALERT;if("✓".equals(s))return LifeOsIconView.CHECK;if("▣".equals(s))return LifeOsIconView.COMMITMENT;if("◆".equals(s))return LifeOsIconView.DECISION;if("●".equals(s))return LifeOsIconView.PEOPLE;if("▤".equals(s))return LifeOsIconView.FILE;if("□".equals(s))return LifeOsIconView.EVENT;return s;}

    static View appIcon(Context c,String packageName,int size){try{Drawable d=c.getPackageManager().getApplicationIcon(packageName);ImageView v=new ImageView(c);v.setImageDrawable(d);v.setScaleType(ImageView.ScaleType.FIT_CENTER);v.setPadding(dp(c,1),dp(c,1),dp(c,1),dp(c,1));v.setLayoutParams(new LinearLayout.LayoutParams(dp(c,size),dp(c,size)));return v;}catch(Throwable ignored){return iconTile(c,LifeOsIconView.ACTIVITY,BLUE,size);}}
    static int withAlpha(int color,int alpha){return Color.argb(alpha,Color.red(color),Color.green(color),Color.blue(color));}

    static View orb(Context c){return new View(c){final Paint ring=new Paint(Paint.ANTI_ALIAS_FLAG);final Paint glow=new Paint(Paint.ANTI_ALIAS_FLAG);{setLayerType(View.LAYER_TYPE_SOFTWARE,null);} @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);float cx=getWidth()/2f,cy=getHeight()/2f;float r=Math.min(getWidth(),getHeight())*.36f;int[] colors={BLUE,BLUE,Color.rgb(88,118,255),PURPLE,BLUE};float[] pos={0f,.45f,.70f,.86f,1f};SweepGradient shader=new SweepGradient(cx,cy,colors,pos);Matrix matrix=new Matrix();matrix.setRotate(88f,cx,cy);shader.setLocalMatrix(matrix);glow.setStyle(Paint.Style.STROKE);glow.setStrokeWidth(dp(getContext(),6));glow.setShader(shader);glow.setMaskFilter(new BlurMaskFilter(dp(getContext(),20),BlurMaskFilter.Blur.NORMAL));glow.setAlpha(170);canvas.drawCircle(cx,cy,r,glow);glow.setMaskFilter(null);ring.setStyle(Paint.Style.STROKE);ring.setStrokeCap(Paint.Cap.ROUND);ring.setStrokeWidth(dp(getContext(),3));ring.setShader(shader);ring.setAlpha(255);canvas.drawCircle(cx,cy,r,ring);}};}

    static LinearLayout bottomNav(Activity a,Class<?> active){LinearLayout wrap=new LinearLayout(a);wrap.setOrientation(LinearLayout.VERTICAL);wrap.setBackgroundColor(SURFACE);wrap.addView(divider(a));LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER);row.setPadding(dp(a,2),dp(a,2),dp(a,2),dp(a,3));row.addView(nav(a,LifeOsIconView.HOME,"Now",active==FeedActivity.class,FeedActivity.class),new LinearLayout.LayoutParams(0,dp(a,60),1));row.addView(nav(a,LifeOsIconView.TIMELINE,"Timeline",active==TimelineActivity.class,TimelineActivity.class),new LinearLayout.LayoutParams(0,dp(a,60),1));row.addView(nav(a,LifeOsIconView.SEARCH,"Search",active==SearchActivity.class,SearchActivity.class),new LinearLayout.LayoutParams(0,dp(a,60),1));row.addView(nav(a,LifeOsIconView.ASK,"Ask",active==AskLifeOsActivity.class,AskLifeOsActivity.class),new LinearLayout.LayoutParams(0,dp(a,60),1));wrap.addView(row);return wrap;}
    private static LinearLayout nav(Activity a,String icon,String label,boolean active,Class<?> target){LinearLayout v=new LinearLayout(a);v.setOrientation(LinearLayout.VERTICAL);v.setGravity(Gravity.CENTER);int color=active?BLUE:TERTIARY;LifeOsIconView iv=new LifeOsIconView(a,icon,color);v.addView(iv,new LinearLayout.LayoutParams(dp(a,22),dp(a,22)));TextView t=text(a,label,9.1f,color);weight(t,active?600:400);t.setGravity(Gravity.CENTER);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.topMargin=dp(a,3);v.addView(t,lp);v.setOnClickListener(x->go(a,target));return v;}
    static void go(Activity a,Class<?> target){if(a.getClass()==target)return;Intent i=new Intent(a,target);i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);a.startActivity(i);a.overridePendingTransition(0,0);}
}
