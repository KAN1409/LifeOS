package com.kareem.lifeos.memory;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class GroundedMemoryConsolidatorTest {
    private static MemoryRecord episodic(long id, String subject, String text, String evidence) {
        return new MemoryRecord(id, subject, text, MemoryRecord.Category.EPISODIC,
                1,1,1f,MemoryRecord.Tier.HOT,null,"assert:"+id,
                Collections.singletonList(evidence));
    }

    @Test public void promotesOnlyCandidateGroundedInExistingEpisodes() {
        final FakeRepo repo = new FakeRepo();
        repo.episodes.add(episodic(1,"person:ahmed","Ahmed prefers morning meetings","e1"));
        MemoryConsolidationClient client = new MemoryConsolidationClient() {
            @Override public List<MemoryConsolidationCandidate> consolidate(List<MemoryRecord> episodicMemories) {
                return Arrays.asList(
                        new MemoryConsolidationCandidate("person:ahmed","Prefers morning meetings",
                                MemoryRecord.Category.PREFERENCE,null,Collections.singletonList(1L)),
                        new MemoryConsolidationCandidate("person:fake","Invented preference",
                                MemoryRecord.Category.PREFERENCE,null,Collections.singletonList(999L)));
            }
        };
        GroundedMemoryConsolidator.Result result = new GroundedMemoryConsolidator(repo,client).run(0,50,10);
        assertEquals(1,result.promoted);
        assertEquals(1,result.rejected);
        assertEquals(Collections.singletonList("e1"),repo.lastEvidence);
        assertEquals("person:ahmed",repo.lastSubject);
    }

    private static final class FakeRepo implements LifeMemoryRepository {
        final List<MemoryRecord> episodes = new ArrayList<MemoryRecord>();
        String lastSubject=""; List<String> lastEvidence=Collections.emptyList();
        @Override public long remember(String subject,String text,MemoryRecord.Category category,float[] embedding,
                                       String sourceAssertionId,List<String> evidenceIds,long now){
            lastSubject=subject;lastEvidence=new ArrayList<String>(evidenceIds);return 42;
        }
        @Override public List<MemoryRecord> recall(String q,float[] e,int k,long n){return Collections.emptyList();}
        @Override public List<MemoryRecord> hotForSubject(String s,int l){return Collections.emptyList();}
        @Override public List<MemoryRecord> searchable(){return new ArrayList<MemoryRecord>(episodes);}
        @Override public List<MemoryRecord> recentEpisodic(long s,int l){return new ArrayList<MemoryRecord>(episodes);}
        @Override public boolean hasSimilar(String t,String s){return false;}
        @Override public PersistentLifeMemoryStore.DecaySummary runDecay(long n){return new PersistentLifeMemoryStore.DecaySummary(0,0,0,n);}
        @Override public int forgetBySubstring(String q,String s){return 0;}
        @Override public void eraseAll(){}
    }
}
