package com.kareem.lifeos;

import org.junit.Test;
import static org.junit.Assert.*;

/** Stable source identity must survive mutable notification/person presentation labels. */
public final class ConversationStableIdTest {
    @Test public void sameRealThreadKeepsSameIdWhenDisplayLabelChanges(){
        String a=ConversationRepository.idFor("com.whatsapp","com.whatsapp|thread-42","Alice");
        String b=ConversationRepository.idFor("com.whatsapp","com.whatsapp|thread-42","Alice ❤️");
        assertEquals(a,b);
    }

    @Test public void differentThreadsNeverCollapseOnlyBecauseLabelsMatch(){
        String a=ConversationRepository.idFor("com.whatsapp","com.whatsapp|thread-a","Same Name");
        String b=ConversationRepository.idFor("com.whatsapp","com.whatsapp|thread-b","Same Name");
        assertNotEquals(a,b);
    }

    @Test public void labelIsOnlyFallbackWhenNoThreadIdentityExists(){
        assertEquals(ConversationRepository.idFor("com.example","","Alice"),ConversationRepository.idFor("com.example","","alice"));
        assertNotEquals(ConversationRepository.idFor("com.example","","Alice"),ConversationRepository.idFor("com.example","","Bob"));
    }
}
