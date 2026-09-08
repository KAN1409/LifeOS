package com.kareem.lifeos;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kareem.lifeos.context.UniversalObservationStore;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Large-data tests catch hidden UI/search/reload/retention caps that are invisible on an empty emulator. */
@RunWith(AndroidJUnit4.class)
public final class ScalabilityInstrumentationTest {
    @Test public void decisionsCountAndSearchRemainCorrectPastOneThousand(){
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();String token=UUID.randomUUID().toString().substring(0,8),needle="QA Old Decision "+token;long base=System.currentTimeMillis()-100000;
        try(LifeDb db=new LifeDb(c)){
            SQLiteDatabase sql=db.getWritableDatabase();sql.beginTransaction();try{
                for(int i=0;i<1205;i++){ContentValues v=new ContentValues();v.put("title",i==0?needle:"QA Decision "+token+" "+i);v.put("context","stress");v.put("options","");v.put("choice","");v.put("consequences","");v.put("status","active");v.put("created_at",base+i);v.put("updated_at",base+i);sql.insertOrThrow("decisions",null,v);}sql.setTransactionSuccessful();
            } finally {sql.endTransaction();}
        }
        assertTrue("Decision count is silently capped",DecisionRepository.count(c)>=1205);
        boolean found=false;for(FunctionalCapabilityRegistry.ObjectItem x:FunctionalSearchEngine.search(c,needle,"decisions",20))if(needle.equals(x.title)){found=true;break;}
        assertTrue("Federated search silently ignored an older decision beyond its scan window",found);
    }

    @Test public void providerObjectReloadDoesNotDependOnFirstThreeThousandRows(){
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();String token=UUID.randomUUID().toString().substring(0,8),oldId="project:qa-old-"+token,oldName="QA Old Project "+token;long base=System.currentTimeMillis()-500000;
        UserObjectStore store=UserObjectStore.get(c);SQLiteDatabase sql=store.getWritableDatabase();sql.beginTransaction();try{
            store.addProject(oldId,oldName,"stress fixture","active",base);
            for(int i=0;i<3105;i++)store.addProject("project:qa-"+token+"-"+i,"QA Project "+token+" "+i,"stress","active",base+1000+i);
            sql.setTransactionSuccessful();
        } finally {sql.endTransaction();}
        assertNotNull("Direct project repository lost the old object",ProjectRepository.load(c,oldId));
        assertNotNull("Functional object reload silently caps lookup to the newest 3000 rows",FunctionalCapabilityRegistry.load(c,"projects",oldId));
    }

    @Test public void rawEvidenceReadDoesNotSilentlyStopAtTwoThousand(){
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();UniversalObservationStore store=UniversalObservationStore.get(c);SQLiteDatabase sql=store.getWritableDatabase();String token=UUID.randomUUID().toString().substring(0,8),prefix="qa-raw-"+token+"-";long base=System.currentTimeMillis()+10000;
        try{
            sql.beginTransaction();try{
                for(int i=0;i<2505;i++){ContentValues v=new ContentValues();v.put("observation_id",prefix+i);v.put("source_kind","OTHER");v.put("source_package","qa.exhaustive");v.put("stream_id","qa:"+token);v.put("event_type","QA_RAW");v.put("observed_at",base+i);v.put("text","raw evidence "+i);v.put("raw_payload","{}");v.put("attributes_json","{}");sql.insertOrThrow("observations",null,v);}sql.setTransactionSuccessful();
            } finally {sql.endTransaction();}
            assertEquals("UniversalObservationStore.recent silently capped an explicit 2505-row request",2505,store.recent(2505).size());
            assertNotNull("Oldest raw evidence row disappeared while reading a >2000 corpus",store.byObservationId(prefix+0));
        } finally {sql.delete("observations","observation_id LIKE ?",new String[]{prefix+"%"});}
    }
}
