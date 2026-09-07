package com.kareem.lifeos;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

/** Guards against correctness fixes turning real 4k+ LifeOS histories into visible freezes. */
@RunWith(AndroidJUnit4.class)
public final class PerformanceBudgetInstrumentationTest {
    @Test public void coreProviderInventoryCompletesWithinLargeHistoryBudget(){
        assumeTrue(Build.VERSION.SDK_INT>=35);Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        try(LifeDb db=new LifeDb(c)){
            SQLiteDatabase sql=db.getWritableDatabase();try{
                sql.beginTransaction();try{
                    long now=System.currentTimeMillis()-600000;
                    for(int i=0;i<5000;i++){ContentValues v=new ContentValues();v.put("source_key","qa-perf-"+i);v.put("app","com.whatsapp");v.put("title","QA Contact "+(i%120));v.put("body","ordinary captured message "+i);v.put("thread_key","com.whatsapp|qa-perf-"+(i%120));v.put("captured_at",now+i);v.put("updated_at",now+i);sql.insertWithOnConflict("events",null,v,SQLiteDatabase.CONFLICT_IGNORE);}sql.setTransactionSuccessful();
                } finally {sql.endTransaction();}
                long start=SystemClock.elapsedRealtime();FunctionalCapabilityRegistry.all(c);long elapsed=SystemClock.elapsedRealtime()-start;
                assertTrue("Provider inventory took "+elapsed+" ms on a 5k-event history; move counts/indexing off reconstructive O(N) paths",elapsed<10000);
            } finally {sql.delete("events","source_key LIKE 'qa-perf-%'",null);}
        }
    }

    @Test public void fullQaRuntimeScanHasBoundedCompletionTime(){
        assumeTrue(Build.VERSION.SDK_INT>=35);Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();long start=SystemClock.elapsedRealtime();FullQaHarness.Report r=FullQaHarness.run(c);long elapsed=SystemClock.elapsedRealtime()-start;
        assertTrue("FullQaHarness took "+elapsed+" ms on disposable emulator; device button needs bounded progress and cancellation",elapsed<15000);
        assertEquals("Runtime QA found hard failures while measuring performance",0,r.fail);
    }
}
