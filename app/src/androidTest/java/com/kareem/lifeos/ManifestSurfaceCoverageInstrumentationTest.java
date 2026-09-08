package com.kareem.lifeos;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Machine-checkable inventory: every manifest Activity must have an explicit launch/error-state test. */
@RunWith(AndroidJUnit4.class)
public final class ManifestSurfaceCoverageInstrumentationTest {
    @Test public void everyDeclaredActivityHasLaunchCoverage() throws Exception {
        Instrumentation ins=InstrumentationRegistry.getInstrumentation();Context c=ins.getTargetContext();
        Map<String,Intent> cases=new HashMap<>();
        add(cases,c,FeedActivity.class,new Intent(c,FeedActivity.class));
        add(cases,c,TimelineActivity.class,new Intent(c,TimelineActivity.class));
        add(cases,c,SearchActivity.class,new Intent(c,SearchActivity.class));
        add(cases,c,AskLifeOsActivity.class,new Intent(c,AskLifeOsActivity.class));
        add(cases,c,CapabilityActivity.class,new Intent(c,CapabilityActivity.class).putExtra("capability","projects"));
        add(cases,c,FunctionalObjectDetailActivity.class,new Intent(c,FunctionalObjectDetailActivity.class).putExtra("capability_id","projects").putExtra("object_id","project:qa-missing"));
        add(cases,c,EntityDetailActivity.class,new Intent(c,EntityDetailActivity.class));
        add(cases,c,SystemStatusActivity.class,new Intent(c,SystemStatusActivity.class));
        add(cases,c,ExperienceAuditActivity.class,new Intent(c,ExperienceAuditActivity.class));
        add(cases,c,OcrLabActivity.class,new Intent(c,OcrLabActivity.class));
        add(cases,c,ImageOcrDetailActivity.class,new Intent(c,ImageOcrDetailActivity.class).putExtra("image_id","image:qa-missing"));
        add(cases,c,VoiceRecorderActivity.class,new Intent(c,VoiceRecorderActivity.class));
        add(cases,c,VoiceMemoryDetailActivity.class,new Intent(c,VoiceMemoryDetailActivity.class).putExtra("voice_id","voice:qa-missing"));
        add(cases,c,FeedSectionActivity.class,new Intent(c,FeedSectionActivity.class).putExtra("mode","attention"));
        add(cases,c,EvidenceDetailActivity.class,new Intent(c,EvidenceDetailActivity.class).putExtra("event_id",-1L));
        add(cases,c,ConversationDetailActivity.class,new Intent(c,ConversationDetailActivity.class).putExtra("conversation_id","conversation:qa-missing"));
        add(cases,c,PersonDetailActivity.class,new Intent(c,PersonDetailActivity.class).putExtra("person_id","person:contact:qa-missing"));
        add(cases,c,SuggestedActionDetailActivity.class,new Intent(c,SuggestedActionDetailActivity.class));
        add(cases,c,SituationDetailActivity.class,new Intent(c,SituationDetailActivity.class));
        add(cases,c,MainActivity.class,new Intent(c,MainActivity.class));
        add(cases,c,SecondBrainActivity.class,new Intent(c,SecondBrainActivity.class));
        add(cases,c,LifeSignalsActivity.class,new Intent(c,LifeSignalsActivity.class));
        add(cases,c,ActionCenterActivity.class,new Intent(c,ActionCenterActivity.class));

        PackageInfo p=c.getPackageManager().getPackageInfo(c.getPackageName(),PackageManager.GET_ACTIVITIES);Set<String> declared=new HashSet<>();
        if(p.activities!=null)for(ActivityInfo a:p.activities)if(a.name.startsWith("com.kareem.lifeos."))declared.add(a.name);
        assertEquals("Manifest Activity inventory changed without updating exhaustive QA coverage. Declared="+declared+" covered="+cases.keySet(),declared,cases.keySet());

        for(Map.Entry<String,Intent> e:cases.entrySet())launchWithoutCrash(ins,e.getValue(),e.getKey());
    }

    private static void add(Map<String,Intent> m,Context c,Class<?> cls,Intent i){m.put(cls.getName(),i);}
    private static void launchWithoutCrash(Instrumentation ins,Intent i,String name)throws Exception{
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);Activity a=ins.startActivitySync(i);assertNotNull("Activity failed to instantiate: "+name,a);ins.waitForIdleSync();
        if(!a.isFinishing())a.runOnUiThread(a::finish);ins.waitForIdleSync();
    }
}
