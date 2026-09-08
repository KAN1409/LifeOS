package com.kareem.lifeos;

import android.content.Context;
import android.os.Bundle;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

/** Two-phase probe. CI runs seed -> force-stop -> verify as separate instrumentation invocations. */
@RunWith(AndroidJUnit4.class)
public final class ProcessDeathPersistenceInstrumentationTest {
    private static final String ID="project:qa-process-death-v48";
    private static final String NAME="QA process death persistence";

    @Test public void seedBeforeExternalForceStop(){
        assumeTrue("seed".equals(phase()));Context c=context();
        c.getSharedPreferences("qa_process_death",Context.MODE_PRIVATE).edit().putString("marker",ID).commit();
        android.database.sqlite.SQLiteDatabase db=UserObjectStore.get(c).getWritableDatabase();db.delete("projects","id=?",new String[]{ID});
        UserObjectStore.get(c).addProject(ID,NAME,"must survive a real am force-stop","active",System.currentTimeMillis());
        assertNotNull(ProjectRepository.load(c,ID));
    }

    @Test public void verifyAfterExternalForceStop(){
        assumeTrue("verify".equals(phase()));Context c=context();
        assertEquals("SharedPreferences marker did not survive process death",ID,c.getSharedPreferences("qa_process_death",Context.MODE_PRIVATE).getString("marker",null));
        ProjectRepository.ProjectObject p=ProjectRepository.load(c,ID);assertNotNull("Durable project did not survive process death",p);assertEquals(NAME,p.name);
        assertNotNull("Functional reload lost persisted object after process death",FunctionalCapabilityRegistry.load(c,"projects",ID));
    }

    private static Context context(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
    private static String phase(){Bundle b=InstrumentationRegistry.getArguments();return b==null?"":b.getString("phase","");}
}
