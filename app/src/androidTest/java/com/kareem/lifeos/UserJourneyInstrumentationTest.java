package com.kareem.lifeos;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Exercises actual controls the way a user does, not only constructors/repositories. */
@RunWith(AndroidJUnit4.class)
public final class UserJourneyInstrumentationTest {
    private Instrumentation ins;private Context c;private UiDevice device;
    @Before public void setup(){ins=InstrumentationRegistry.getInstrumentation();c=ins.getTargetContext();device=UiDevice.getInstance(ins);}

    @Test public void fourPrimaryEntrancesNavigateFromNow() throws Exception {
        assertNav("Timeline",TimelineActivity.class);
        assertNav("Search",SearchActivity.class);
        assertNav("Ask",AskLifeOsActivity.class);
    }

    @Test public void searchFieldShowsARealProviderObject() throws Exception {
        String name="QA Search Project "+UUID.randomUUID().toString().substring(0,8);ProjectRepository.ProjectObject p=ProjectRepository.create(c,name,"user journey");assertNotNull(p);
        Activity a=launch(new Intent(c,SearchActivity.class).putExtra("initial_query",name).putExtra("initial_filter","projects"));
        assertTrue("Search UI did not render a real project result",device.wait(Until.hasObject(By.text(name)),2500));finish(a);
    }

    @Test public void projectCanBeCreatedThroughTheVisibleUi() throws Exception {
        String name="QA UI Project "+UUID.randomUUID().toString().substring(0,8);Activity a=launch(new Intent(c,CapabilityActivity.class).putExtra("capability","projects"));
        UiObject2 create=device.wait(Until.findObject(By.text("Create project")),2000);assertNotNull("Create project action missing",create);create.click();
        assertTrue("Project creation dialog did not appear",device.wait(Until.hasObject(By.text("Create project")),1500));
        List<UiObject2> edits=device.findObjects(By.clazz("android.widget.EditText"));assertTrue("Project dialog inputs missing",edits.size()>=2);edits.get(0).setText(name);edits.get(1).setText("Created by exhaustive user-journey QA");
        UiObject2 confirm=device.findObject(By.text("Create"));assertNotNull("Project dialog Create button missing",confirm);confirm.click();
        assertTrue("Created project did not appear in Browse",device.wait(Until.hasObject(By.text(name)),2500));assertNotNull(ProjectRepository.load(c,findProject(name).id));finish(a);
    }

    @Test public void filePickerActionActuallyDispatchesAndroidOpenDocument() throws Exception {
        Activity a=launch(new Intent(c,CapabilityActivity.class).putExtra("capability","files"));
        IntentFilter f=new IntentFilter(Intent.ACTION_OPEN_DOCUMENT);Instrumentation.ActivityMonitor monitor=ins.addMonitor(f,new Instrumentation.ActivityResult(Activity.RESULT_CANCELED,null),true);
        UiObject2 button=device.wait(Until.findObject(By.text("Connect a file")),1500);assertNotNull(button);button.click();ins.waitForIdleSync();
        assertTrue("Connect a file did not issue ACTION_OPEN_DOCUMENT",monitor.getHits()>0);ins.removeMonitor(monitor);finish(a);
    }

    @Test public void voiceAndDecisionProviderActionsOpenTheirRealScreens() throws Exception {
        assertCapabilityAction("voice","Record voice memory",VoiceRecorderActivity.class);
        assertCapabilityAction("decisions","Open Decision Memory",LifeSignalsActivity.class);
    }

    @Test public void detailBackControlReturnsWithoutCrash() throws Exception {
        ProjectRepository.ProjectObject p=ProjectRepository.create(c,"QA Back "+UUID.randomUUID().toString().substring(0,8),"");assertNotNull(p);
        Activity a=launch(new Intent(c,FunctionalObjectDetailActivity.class).putExtra("capability_id","projects").putExtra("object_id",p.id));
        UiObject2 back=device.wait(Until.findObject(By.desc("Back")),1500);assertNotNull("Detail Back control has no accessible description",back);back.click();ins.waitForIdleSync();assertTrue(a.isFinishing()||a.isDestroyed());
    }

    private void assertNav(String label,Class<? extends Activity> target)throws Exception{
        Activity feed=launch(new Intent(c,FeedActivity.class));Instrumentation.ActivityMonitor m=ins.addMonitor(target.getName(),null,false);UiObject2 nav=device.wait(Until.findObject(By.text(label)),1500);assertNotNull("Missing bottom-nav item "+label,nav);nav.click();Activity opened=ins.waitForMonitorWithTimeout(m,2500);assertNotNull(label+" did not open "+target.getSimpleName(),opened);ins.removeMonitor(m);finish(opened);finish(feed);
    }
    private void assertCapabilityAction(String cap,String label,Class<? extends Activity> target)throws Exception{
        Activity list=launch(new Intent(c,CapabilityActivity.class).putExtra("capability",cap));Instrumentation.ActivityMonitor m=ins.addMonitor(target.getName(),null,false);UiObject2 b=device.wait(Until.findObject(By.text(label)),1500);assertNotNull("Missing action "+label,b);b.click();Activity opened=ins.waitForMonitorWithTimeout(m,2500);assertNotNull(label+" did not open "+target.getSimpleName(),opened);ins.removeMonitor(m);finish(opened);finish(list);
    }
    private ProjectRepository.ProjectObject findProject(String name){for(ProjectRepository.ProjectObject p:ProjectRepository.list(c,5000))if(name.equals(p.name))return p;return null;}
    private Activity launch(Intent i){i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);Activity a=ins.startActivitySync(i);assertNotNull(a);ins.waitForIdleSync();return a;}
    private void finish(Activity a){if(a==null)return;if(!a.isFinishing())a.runOnUiThread(a::finish);ins.waitForIdleSync();}
}
