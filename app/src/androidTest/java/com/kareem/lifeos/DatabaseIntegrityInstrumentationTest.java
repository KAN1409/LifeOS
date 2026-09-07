package com.kareem.lifeos;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Under-the-hood storage checks over every database created by the installed app. */
@RunWith(AndroidJUnit4.class)
public final class DatabaseIntegrityInstrumentationTest {
    @Test public void everyDatabasePassesSqliteIntegrityChecks() {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        // Force the main durable stores to exist before enumeration.
        try(LifeDb ignored=new LifeDb(c)){ignored.getReadableDatabase();}
        AttentionStore.get(c).openCount();
        NotificationMeaningStore.get(c).count();
        UserObjectStore.get(c).fileCount();
        VoiceMemoryStore.get(c).count();
        ImageOcrStore.get(c).imageCount();

        String[] names=c.databaseList();
        assertTrue("LifeOS did not expose any databases to test",names.length>0);
        for(String name:names){
            File f=c.getDatabasePath(name);
            if(!f.exists()||f.length()==0)continue;
            SQLiteDatabase db=null;
            try{
                db=SQLiteDatabase.openDatabase(f.getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY);
                try(Cursor q=db.rawQuery("PRAGMA quick_check",null)){
                    assertTrue("quick_check returned no row for "+name,q.moveToFirst());
                    assertEquals("SQLite corruption detected in "+name,"ok",q.getString(0));
                }
                try(Cursor fk=db.rawQuery("PRAGMA foreign_key_check",null)){
                    assertEquals("Foreign-key violation detected in "+name,0,fk.getCount());
                }
            } finally { if(db!=null)db.close(); }
        }
    }

    @Test public void durableDatabaseFilesRemainInsidePrivateAppStorage(){
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        for(String name:c.databaseList()){
            File f=c.getDatabasePath(name);
            String path=f.getAbsolutePath();
            assertTrue("Database escaped app-private storage: "+path,
                    path.startsWith(c.getApplicationInfo().dataDir+File.separator));
        }
    }
}
