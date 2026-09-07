package com.kareem.lifeos;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** UX robustness: accessible controls, usable touch targets, large text and Arabic content. */
@RunWith(AndroidJUnit4.class)
public final class UiRobustnessInstrumentationTest {
    private Instrumentation ins;private Context c;private UiDevice device;
    @Before public void setup(){ins=InstrumentationRegistry.getInstrumentation();c=ins.getTargetContext();device=UiDevice.getInstance(ins);}

    @Test public void coreScreensHaveNoUnlabelledVisibleClickTargetsOrTinyTouchTargets() throws Exception {
        for(Intent i:new Intent[]{new Intent(c,FeedActivity.class),new Intent(c,TimelineActivity.class),new Intent(c,SearchActivity.class),new Intent(c,AskLifeOsActivity.class),new Intent(c,SystemStatusActivity.class)}){
            Activity a=launch(i);List<String> badLabels=new ArrayList<>(),tiny=new ArrayList<>();inspect(a.getWindow().getDecorView(),badLabels,tiny);
            assertTrue("Unlabelled clickable controls in "+a.getClass().getSimpleName()+": "+badLabels,badLabels.isEmpty());
            assertTrue("Touch targets below 40dp in "+a.getClass().getSimpleName()+": "+tiny,tiny.isEmpty());finish(a);
        }
    }

    @Test public void largeFontDoesNotClipButtonLabelsOnPrimaryScreens() throws Exception {
        String old=device.executeShellCommand("settings get system font_scale").trim();try{
            device.executeShellCommand("settings put system font_scale 1.30");
            for(Intent i:new Intent[]{new Intent(c,FeedActivity.class),new Intent(c,SearchActivity.class),new Intent(c,AskLifeOsActivity.class),new Intent(c,CapabilityActivity.class).putExtra("capability","projects")}){
                Activity a=launch(i);List<String> clipped=new ArrayList<>();collectClippedButtons(a.getWindow().getDecorView(),clipped);assertTrue("Button labels clipped at font_scale=1.30 in "+a.getClass().getSimpleName()+": "+clipped,clipped.isEmpty());finish(a);
            }
        } finally {device.executeShellCommand("settings put system font_scale "+(old.isEmpty()?"1.0":old));}
    }

    @Test public void longArabicAndMixedLanguageObjectsRemainSearchableAndRenderable() throws Exception {
        String token=UUID.randomUUID().toString().substring(0,6);String title="مشروع اختبار شامل "+token+" — LifeOS mixed English عربي";
        ProjectRepository.ProjectObject p=ProjectRepository.create(c,title,"وصف طويل لاختبار اتجاه النص والبحث وعرض التفاصيل بدون فقدان البيانات أو اختراع أي معلومات إضافية داخل الواجهة.");assertNotNull(p);
        Activity search=launch(new Intent(c,SearchActivity.class).putExtra("initial_query",token).putExtra("initial_filter","projects"));
        assertTrue("Mixed Arabic project not present in rendered Search view",containsText(search.getWindow().getDecorView(),title));finish(search);
        Activity detail=launch(new Intent(c,FunctionalObjectDetailActivity.class).putExtra("capability_id","projects").putExtra("object_id",p.id));assertTrue("Mixed Arabic title did not render in detail",containsText(detail.getWindow().getDecorView(),title));finish(detail);
    }

    private void inspect(View v,List<String> unlabeled,List<String> tiny){
        if(v.getVisibility()!=View.VISIBLE)return;Rect r=new Rect();boolean onScreen=v.getGlobalVisibleRect(r);if(onScreen&&v.isClickable()){
            String label=accessibleText(v).trim();if(label.isEmpty())unlabeled.add(v.getClass().getSimpleName()+"@"+r.toShortString());
            float density=c.getResources().getDisplayMetrics().density;float w=r.width()/density,h=r.height()/density;if(w<40||h<40)tiny.add(v.getClass().getSimpleName()+"("+Math.round(w)+"x"+Math.round(h)+"dp) "+label);
        }
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)inspect(g.getChildAt(i),unlabeled,tiny);}
    }
    private static String accessibleText(View v){CharSequence d=v.getContentDescription();StringBuilder b=new StringBuilder(d==null?"":d.toString());if(v instanceof TextView){CharSequence t=((TextView)v).getText();if(t!=null&&!t.toString().trim().isEmpty())b.append(' ').append(t);}if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)b.append(' ').append(accessibleText(g.getChildAt(i)));}return b.toString();}
    private static void collectClippedButtons(View v,List<String> out){if(v.getVisibility()!=View.VISIBLE)return;if(v instanceof Button){Button b=(Button)v;if(b.getLayout()!=null)for(int i=0;i<b.getLineCount();i++)if(b.getLayout().getEllipsisCount(i)>0){out.add(b.getText().toString());break;}}if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)collectClippedButtons(g.getChildAt(i),out);}}
    private static boolean containsText(View v,String wanted){if(v instanceof TextView&&wanted.contentEquals(((TextView)v).getText()))return true;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)if(containsText(g.getChildAt(i),wanted))return true;}return false;}
    private Activity launch(Intent i){i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);Activity a=ins.startActivitySync(i);assertNotNull(a);ins.waitForIdleSync();return a;}
    private void finish(Activity a){if(a!=null&&!a.isFinishing())a.runOnUiThread(a::finish);ins.waitForIdleSync();}
}
