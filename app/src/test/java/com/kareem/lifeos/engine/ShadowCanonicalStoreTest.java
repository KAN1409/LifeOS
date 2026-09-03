package com.kareem.lifeos.engine;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class ShadowCanonicalStoreTest {
    @Test public void replacingCanonicalNeverDeletesRawEvidence(){
        ShadowCanonicalStore.resetForTests();
        ShadowCanonicalStore store=ShadowCanonicalStore.shared();
        store.appendRaw(new RawEvidenceRecord("SCREEN","chat","MESSAGE",MessageObservation.Direction.IN,"hello",1000L,0.7));
        store.replaceCanonical(Arrays.asList(new CanonicalEvent("MESSAGE",MessageObservation.Direction.IN,"hello",1000L,0.8,Arrays.asList("SCREEN"))));
        assertEquals(1,store.rawEvidence().size());
        assertEquals(1,store.canonicalEvents().size());
        store.clearCanonicalOnly();
        assertEquals(1,store.rawEvidence().size());
        assertEquals(0,store.canonicalEvents().size());
    }
}
