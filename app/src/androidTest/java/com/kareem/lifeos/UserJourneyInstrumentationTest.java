package com.kareem.lifeos;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
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
        Activity a=null;try{
            a=launch(new Intent(c,SearchActivity.class).putExtra("initial_query",name).putExtra("initial_filter","projects"));
            assertTrue("Search UI did not render a real project result",device.wait(Until.hasObject(By.text(name)),2500));
        } finally {finish(a);}
    }

    @Test public void projectCanBeCreatedThroughTheVisibleUi() throws Exception {
        String name="QA UI Project "+UUID.randomUUID().toString().substring(0,8);Activity a=null;try{
            a=launch(new Intent(c,CapabilityActivity.class).putExtra("capability","projects"));
            UiObject2 create=device.wait(Until.findObject(By.text("Create project")),2000);assertNotNull("Create project action missing",create);create.click();
            assertTrue("Project creation dialog did not appear",device.wait(Until.hasObject(By.text("Create project")),1500));
            List<UiObject2> edits=device.findObjects(By.clazz("android.widget.EditText"));assertTrue("Project dialog inputs missing",edits.size()>=2);edits.get(0).setText(name);edits.get(1).setText("Created by exhaustive user-journey QA");
            UiObject2 confirm=device.findObject(By.text("Create"));assertNotNull("Project dialog Create button missing",confirm);confirm.click();
            assertTrue("Created project did not appear in Browse",device.wait(Until.hasObject(By.text(name)),2500));ProjectRepository.ProjectObject created=findProject(name);assertNotNull(created);assertNotNull(ProjectRepository.load(c,created.id));
        } finally {finish(a);}
    }

    @Test public void projectCompleteAndReopenLifecycleWorksThroughVisibleDetailActions() throws Exception {
        ProjectRepository.ProjectObject p=ProjectRepository.create(c,"QA Project Lifecycle "+UUID.randomUUID().toString().substring(0,8),"visible lifecycle journey");assertNotNull(p);Activity a=null;try{
            a=launch(new Intent(c,FunctionalObjectDetailActivity.class).putExtra("capability_id","projects").putExtra("object_id",p.id));
            UiObject2 complete=device.wait(Until.findObject(By.text("Mark completed")),1500);assertNotNull("Project detail has no Mark completed action",complete);complete.click();
            assertTrue("Project detail did not rerender completed state",device.wait(Until.hasObject(By.text("Reopen")),2000));assertEquals("completed",ProjectRepository.load(c,p.id).status);
            UiObject2 reopen=device.findObject(By.text("Reopen"));assertNotNull(reopen);reopen.click();
            assertTrue("Project detail did not rerender active state",device.wait(Until.hasObject(By.text("Mark completed")),2000));assertEquals("active",ProjectRepository.load(c,p.id).status);
        } finally {finish(a);}
    }

    @Test public void groundedCommitmentCanBeMarkedHandledThroughVisibleDetailAction() throws Exception {
        String suffix=UUID.randomUUID().toString().substring(0,8),stream="com.whatsapp|qa-ui-obligation-"+suffix,obs="qa-ui-obs-"+suffix,body="Can you send the QA contract?";long now=System.currentTimeMillis(),event;
        try(LifeDb db=new LifeDb(c)){event=db.upsertEvent("qa-ui-obligation-"+suffix,"com.whatsapp","QA Person "+suffix,body,stream,now);}
        NotificationMeaning meaning=new NotificationMeaning(stream,obs,"PERSON_CONVERSATION","QUESTION","WAITING_ON_USER","MEDIUM","REPLY","Send the QA contract","Direct question",body,"",.97,"qa-user-journey",now);
        NotificationMeaningStore.get(c).put(meaning,now);AttentionStore.get(c).applyModel(meaning,event,now);
        ObligationRepository.ObligationObject obligation=null;for(ObligationRepository.ObligationObject o:ObligationRepository.open(c,2000))if(o.latestEventId==event){obligation=o;break;}assertNotNull("Grounded fixture never became a canonical commitment",obligation);
        Activity a=null;try{
            a=launch(new Intent(c,FunctionalObjectDetailActivity.class).putExtra("capability_id","commitments").putExtra("object_id",obligation.id));
            UiObject2 handled=device.wait(Until.findObject(By.text("Mark handled")),1500);assertNotNull("Commitment detail has no Mark handled action",handled);handled.click();ins.waitForIdleSync();
            assertEquals("Visible Mark handled action did not update durable attention state",AttentionStore.HANDLED,AttentionStore.get(c).forEvent(event).status);
            assertNull("Handled commitment remained in canonical open projection",ObligationRepository.load(c,obligation.id));
        } finally {finish(a);}
    }

    @Test public void filePickerActionActuallyOpensAndroidDocumentUi() throws Exception {
        Activity a=null;try{
            a=launch(new Intent(c,CapabilityActivity.class).putExtra("capability","files"));
            UiObject2 button=device.wait(Until.findObject(By.text("Connect a file")),1500);assertNotNull(button);button.click();
            String current=waitForExternalPackage(3500);
            assertNotNull("Connect a file did not leave LifeOS for Android's document UI",current);
            assertNotEquals("Connect a file stayed inside LifeOS instead of opening ACTION_OPEN_DOCUMENT",c.getPackageName(),current);
            device.pressBack();device.waitForIdle();
        } finally {
            String current=device.getCurrentPackageName();if(current!=null&&!c.getPackageName().equals(current))device.pressBack();
            finish(a);
        }
    }

    @Test public void voiceAndDecisionProviderActionsOpenTheirRealScreens() throws Exception {
        assertCapabilityAction("voice","Record voice memory",VoiceRecorderActivity.class);
        assertCapabilityAction("decisions","Open Decision Memory",LifeSignalsActivity.class);
    }

    @Test public void detailBackControlReturnsWithoutCrash() throws Exception {
        ProjectRepository.ProjectObject p=ProjectRepository.create(c,"QA Back "+UUID.randomUUID().toString().substring(0,8),"");assertNotNull(p);
        Activity a=null;try{
            a=launch(new Intent(c,FunctionalObjectDetailActivity.class).putExtra("capability_id","projects").putExtra("object_id",p.id));
            UiObject2 back=device.wait(Until.findObject(By.desc("Back")),1500);assertNotNull("Detail Back control has no accessible description",back);back.click();ins.waitForIdleSync();assertTrue(a.isFinishing()||a.isDestroyed());
        } finally {finish(a);}
    }

    private void assertNav(String label,Class<? extends Activity> target)throws Exception{
        Activity feed=null,opened=null;Instrumentation.ActivityMonitor m=null;try{
            feed=launch(new Intent(c,FeedActivity.class));m=ins.addMonitor(target.getName(),null,false);UiObject2 nav=device.wait(Until.findObject(By.text(label)),1500);assertNotNull("Missing bottom-nav item "+label,nav);nav.click();opened=ins.waitForMonitorWithTimeout(m,2500);assertNotNull(label+" did not open "+target.getSimpleName(),opened);
        } finally {if(m!=null)ins.removeMonitor(m);finish(opened);finish(feed);}
    }
    private void assertCapabilityAction(String cap,String label,Class<? extends Activity> target)throws Exception{
        Activity list=null,opened=null;Instrumentation.ActivityMonitor m=null;try{
            list=launch(new Intent(c,CapabilityActivity.class).putExtra("capability",cap));m=ins.addMonitor(target.getName(),null,false);UiObject2 b=device.wait(Until.findObject(By.text(label)),1500);assertNotNull("Missing action "+label,b);b.click();opened=ins.waitForMonitorWithTimeout(m,2500);assertNotNull(label+" did not open "+target.getSimpleName(),opened);
        } finally {if(m!=null)ins.removeMonitor(m);finish(opened);finish(list);}
    }
    private String waitForExternalPackage(long timeoutMs)throws Exception{long end=System.currentTimeMillis()+timeoutMs;String pkg;do{pkg=device.getCurrentPackageName();if(pkg!=null&&!c.getPackageName().equals(pkg))return pkg;Thread.sleep(100);}while(System.currentTimeMillis()<end);return device.getCurrentPackageName();}
    private ProjectRepository.ProjectObject findProject(String name){for(ProjectRepository.ProjectObject p:ProjectRepository.list(c,5000))if(name.equals(p.name))return p;return null;}
    private Activity launch(Intent i){i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);Activity a=ins.startActivitySync(i);assertNotNull(a);ins.waitForIdleSync();return a;}
    private void finish(Activity a){if(a==null)return;try{if(!a.isFinishing())a.runOnUiThread(a::finish);ins.waitForIdleSync();}catch(Throwable ignored){}}
}
