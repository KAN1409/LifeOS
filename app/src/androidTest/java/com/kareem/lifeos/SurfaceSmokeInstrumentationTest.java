package com.kareem.lifeos;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.test.InstrumentationTestCase;

/** Clean-device smoke test: every core surface must construct, resume and render without crashing. */
public final class SurfaceSmokeInstrumentationTest extends InstrumentationTestCase {
    private Instrumentation instrumentation;
    private Context target;

    @Override protected void setUp() throws Exception {
        super.setUp();
        instrumentation=getInstrumentation();
        target=instrumentation.getTargetContext();
    }

    public void testCoreSurfacesLaunch() throws Exception {
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

    public void testEveryFunctionalCapabilitySurfaceLaunches() throws Exception {
        for(String id:new String[]{"conversations","commitments","decisions","voice","files","projects","places","people","events"})
            launch(new Intent(target,CapabilityActivity.class).putExtra("capability",id));
    }

    public void testRuntimeQaHasNoHardFailureOnCleanInstall() {
        FullQaHarness.Report report=FullQaHarness.run(target);
        assertEquals("FullQaHarness has a hard failure on a clean emulator: "+report.json.toString(),0,report.fail);
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
