package com.kareem.lifeos;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Disposable-emulator E2E round trips: source -> durable object -> search/detail/lifecycle. */
@RunWith(AndroidJUnit4.class)
public final class RepositoryRoundTripInstrumentationTest {
    private Context c; private String suffix;
    @Before public void setup(){c=InstrumentationRegistry.getInstrumentation().getTargetContext();suffix=UUID.randomUUID().toString().substring(0,8);}

    @Test public void projectCreateSearchCompleteReopenRoundTrip(){
        String name="QA Project "+suffix;
        ProjectRepository.ProjectObject p=ProjectRepository.create(c,name,"exhaustive QA fixture");
        assertNotNull(p);assertEquals("active",p.status);assertNotNull(ProjectRepository.load(c,p.id));
        assertFound("projects",name,p.id);
        ProjectRepository.setStatus(c,p.id,"completed");assertEquals("completed",ProjectRepository.load(c,p.id).status);
        ProjectRepository.setStatus(c,p.id,"active");assertEquals("active",ProjectRepository.load(c,p.id).status);
    }

    @Test public void decisionCreateSearchReloadRoundTrip(){
        String title="QA Decision "+suffix;long id;
        try(LifeDb db=new LifeDb(c)){id=db.addDecision(title,"context","A | B","A","expected outcome");}
        assertTrue(id>0);DecisionRepository.DecisionObject d=DecisionRepository.load(c,"decision:"+id);assertNotNull(d);assertEquals(title,d.title);
        assertFound("decisions",title,d.id);
    }

    @Test public void fileAndImageSourcePersistenceRoundTrip() throws Exception {
        File text=new File(c.getCacheDir(),"qa_"+suffix+".txt");try(FileOutputStream o=new FileOutputStream(text)){o.write(("LifeOS QA "+suffix).getBytes());}
        FileRepository.FileObject f=FileRepository.importUri(c,Uri.fromFile(text));assertNotNull(f);assertNotNull(FileRepository.load(c,f.id));assertFound("files",f.displayName,f.id);

        File png=new File(c.getCacheDir(),"qa_"+suffix+".png");Bitmap b=Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888);try(FileOutputStream o=new FileOutputStream(png)){assertTrue(b.compress(Bitmap.CompressFormat.PNG,100,o));}b.recycle();
        ImageRepository.ImageObject image=ImageRepository.importUri(c,Uri.fromFile(png),"QA generated fixture");assertNotNull(image);assertNotNull(ImageRepository.load(c,image.id));assertTrue(image.width>0&&image.height>0);
    }

    @Test public void voiceSourceSurvivesRepositoryRoundTrip() throws Exception {
        File wav=new File(c.getFilesDir(),"qa_voice_"+suffix+".wav");writeSilentWav(wav,1600,16000);
        VoiceMemoryRepository.VoiceObject v=VoiceMemoryRepository.register(c,wav,100);assertNotNull(v);assertNotNull(VoiceMemoryRepository.load(c,v.id));
        assertTrue(new File(v.filePath).exists());assertTrue(new File(v.filePath).length()>=44);assertFound("voice","Voice memory",v.id);
    }

    @Test public void conversationEvidenceBecomesStableSearchableObject(){
        long now=System.currentTimeMillis();String thread="com.whatsapp|qa-"+suffix;long event;
        try(LifeDb db=new LifeDb(c)){event=db.upsertEvent("qa-conv-"+suffix,"com.whatsapp","QA Friend "+suffix,"Testing a normal conversation message",thread,now);}
        assertTrue(event>0);ConversationRepository.ConversationObject hit=null;for(ConversationRepository.ConversationObject x:ConversationRepository.list(c,2000))if(x.threadKey.equals(thread)){hit=x;break;}
        assertNotNull("Conversation fixture did not become a typed conversation",hit);assertNotNull(ConversationRepository.load(c,hit.id));assertFalse(ConversationRepository.evidence(c,hit.id,20).isEmpty());assertFound("conversations",hit.label,hit.id);
    }

    @Test public void groundedObligationCreatesAndResolvesWithoutReopening(){
        long now=System.currentTimeMillis();String stream="com.whatsapp|obligation-"+suffix,obs="qa-obs-"+suffix,body="Can you send me the QA invoice?";long event;
        try(LifeDb db=new LifeDb(c)){event=db.upsertEvent("qa-obligation-"+suffix,"com.whatsapp","QA Person "+suffix,body,stream,now);}
        NotificationMeaning m=new NotificationMeaning(stream,obs,"PERSON_CONVERSATION","QUESTION","WAITING_ON_USER","MEDIUM","REPLY","Send the QA invoice","Direct question",body,"",.96,"qa",now);
        NotificationMeaningStore.get(c).put(m,now);AttentionStore.get(c).applyModel(m,event,now);
        ObligationRepository.ObligationObject found=null;for(ObligationRepository.ObligationObject o:ObligationRepository.open(c,2000))if(o.latestEventId==event){found=o;break;}
        assertNotNull("Grounded direct question did not surface as an obligation",found);assertNotNull(ObligationRepository.load(c,found.id));
        AttentionStore.get(c).resolve(event,"QA completion evidence");
        assertNull("Resolved obligation reopened",ObligationRepository.load(c,found.id));
        assertEquals(AttentionStore.RESOLVED,AttentionStore.get(c).forEvent(event).status);
    }

    private void assertFound(String cap,String query,String id){List<FunctionalCapabilityRegistry.ObjectItem> xs=FunctionalSearchEngine.search(c,query,cap,100);for(FunctionalCapabilityRegistry.ObjectItem x:xs)if(id.equals(x.objectId))return;fail("Search did not round-trip "+cap+" object "+id+" for query "+query);}

    private static void writeSilentWav(File f,int samples,int rate)throws Exception{
        int data=samples*2;ByteBuffer h=ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);h.put("RIFF".getBytes());h.putInt(36+data);h.put("WAVE".getBytes());h.put("fmt ".getBytes());h.putInt(16);h.putShort((short)1);h.putShort((short)1);h.putInt(rate);h.putInt(rate*2);h.putShort((short)2);h.putShort((short)16);h.put("data".getBytes());h.putInt(data);
        try(FileOutputStream o=new FileOutputStream(f)){o.write(h.array());o.write(new byte[data]);}
    }
}
