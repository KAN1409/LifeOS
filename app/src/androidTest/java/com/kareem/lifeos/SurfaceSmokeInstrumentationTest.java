package com.kareem.lifeos;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Clean-device smoke test: every core surface must construct, resume and render without crashing. */
@RunWith(AndroidJUnit4.class)
public final class SurfaceSmokeInstrumentationTest {
    private Instrumentation instrumentation;
    private Context target;

    @Before public void setUp() {
        instrumentation=InstrumentationRegistry.getInstrumentation();
        target=instrumentation.getTargetContext();
    }

    @Test public void coreSurfacesLaunch() throws Exception {
        launch(new Intent(target,FeedActivity.class));
        launch(new Intent(target,TimelineActivity.class));
        launch(new Intent(target,SearchActivity.class));
        launch(new Intent(target,AskLifeOsActivity.class));
        launch(new Intent(target,SystemStatusActivity.class));
        launch(new Intent(target,ExperienceAuditActivity.class));
        launch(new Intent(target,OcrLabActivity.class));
        launch(new Intent(target,VoiceRecorderActivity.class));
        launch(new Intent(target,MainActivity.class));
        launch(new Intent(target,LifeSignalsActivity.class));
        launch(new Intent(target,SecondBrainActivity.class));
        launch(new Intent(target,ActionCenterActivity.class));
        launch(new Intent(target,FeedSectionActivity.class).putExtra("mode","attention"));
        launch(new Intent(target,FeedSectionActivity.class).putExtra("mode","actions"));
        launch(new Intent(target,FeedSectionActivity.class).putExtra("mode","activity"));
    }

    @Test public void everyFunctionalCapabilitySurfaceLaunches() throws Exception {
        for(String id:new String[]{"conversations","commitments","decisions","voice","files","projects","places","people","events"})
            launch(new Intent(target,CapabilityActivity.class).putExtra("capability",id));
    }

    @Test public void runtimeQaHasNoHardFailureOnCleanInstall() {
        FullQaHarness.Report report=FullQaHarness.run(target);
        assertEquals("FullQaHarness has a hard failure on a clean emulator: "+report.json.toString(),0,report.fail);
    }

    @Test public void fullQaButtonDoesNotBlockAndroidMainThread() throws Exception {
        Intent intent=new Intent(target,ExperienceAuditActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        Activity activity=instrumentation.startActivitySync(intent);
        instrumentation.waitForIdleSync();
        View run=findText(activity.getWindow().getDecorView(),"Run full LifeOS test");
        assertNotNull("Full QA run button missing",run);

        CountDownLatch pulse=new CountDownLatch(1);
        activity.runOnUiThread(()->{
            run.performClick();
            new Handler(Looper.getMainLooper()).post(pulse::countDown);
        });
        assertTrue("Full QA click blocked the Android main thread",pulse.await(750,TimeUnit.MILLISECONDS));

        activity.runOnUiThread(activity::finish);
        instrumentation.waitForIdleSync();
    }

    private static View findText(View v,String wanted){
        if(v instanceof TextView&&wanted.contentEquals(((TextView)v).getText()))return v;
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){View found=findText(g.getChildAt(i),wanted);if(found!=null)return found;}}
        return null;
    }

    private void launch(Intent intent) throws Exception {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        Activity activity=instrumentation.startActivitySync(intent);
        assertNotNull("Activity failed to launch: "+intent.getComponent(),activity);
        instrumentation.waitForIdleSync();
        assertFalse("Activity finished immediately: "+intent.getComponent(),activity.isFinishing());
        activity.runOnUiThread(activity::finish);
        instrumentation.waitForIdleSync();
    }
}
