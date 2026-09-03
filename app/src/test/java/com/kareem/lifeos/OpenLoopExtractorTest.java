package com.kareem.lifeos;

import org.junit.Test;
import static org.junit.Assert.*;

public class OpenLoopExtractorTest {
    @Test public void extractsEnglishRequest(){assertEquals("request",OpenLoopExtractor.extract("Alex","Please send me the plan",1).get(0).kind);}
    @Test public void extractsArabicCommitment(){assertEquals("commitment",OpenLoopExtractor.extract("أحمد","هبعت لك الملف بكرة",1).get(0).kind);}
    @Test public void extractsAppointment(){assertEquals("appointment",OpenLoopExtractor.extract("Clinic","Tomorrow at 6 pm",1).get(0).kind);}
    @Test public void ignoresOrdinaryStatus(){assertTrue(OpenLoopExtractor.extract("Battery","Charging complete",1).isEmpty());}
    @Test public void fingerprintIsStable(){String a=OpenLoopExtractor.extract("A","Please send me X",1).get(0).fingerprint;String b=OpenLoopExtractor.extract("A","Please send me X",2).get(0).fingerprint;assertEquals(a,b);}
}
