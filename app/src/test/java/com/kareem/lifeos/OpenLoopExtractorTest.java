package com.kareem.lifeos;

import org.junit.Test;
import static org.junit.Assert.*;

public class OpenLoopExtractorTest {
    @Test public void extractsEnglishRequest(){assertEquals("request",OpenLoopExtractor.extract("Alex","Please send me the plan",1).get(0).kind);}
    @Test public void extractsArabicCommitment(){assertEquals("commitment",OpenLoopExtractor.extract("أحمد","هبعت لك الملف بكرة",1).get(0).kind);}
    @Test public void extractsAppointment(){assertEquals("appointment",OpenLoopExtractor.extract("Clinic","Tomorrow at 6 pm",1).get(0).kind);}
    @Test public void ignoresOrdinaryStatus(){assertTrue(OpenLoopExtractor.extract("Battery","Charging complete",1).isEmpty());}
    @Test public void fingerprintIsStable(){String a=OpenLoopExtractor.extract("A","Please send me X",1).get(0).fingerprint;String b=OpenLoopExtractor.extract("A","Please send me X",2).get(0).fingerprint;assertEquals(a,b);}
    @Test public void cibChargeIsNotAppointment(){assertTrue(OpenLoopExtractor.extract("CIB","Your credit card #8883 was charged EGP 49.99 at Google GetConta",1).isEmpty());}
    @Test public void subscriptionReceiptIsNotRequest(){assertTrue(OpenLoopExtractor.extract("Google Play","Your subscription renewed. Receipt sent to your email.",1).isEmpty());}
    @Test public void otpIsNotAttention(){assertTrue(OpenLoopExtractor.extract("Bank","Your OTP verification code is 123456",1).isEmpty());}
    @Test public void humanArabicRequestStillWorks(){assertEquals("request",OpenLoopExtractor.extract("Suzanne","ممكن ابعتلي البرومبتس؟",1).get(0).kind);}
    @Test public void securityAccessChangeIsHighPriority(){OpenLoopExtractor.Candidate x=OpenLoopExtractor.extract("Security Alert","Plaud now has access to your Google account. If you did not grant access, review activity.",1).get(0);assertEquals("security",x.kind);assertEquals(100,x.priority);}
    @Test public void ordinaryChargeWithLimitExceptionIsFinancialAttention(){OpenLoopExtractor.Candidate x=OpenLoopExtractor.extract("CIB","Your credit card was charged EGP 49.99. You are over the limit by EGP 299.12",1).get(0);assertEquals("financial_alert",x.kind);assertEquals(95,x.priority);}
    @Test public void datedSchoolPaymentBecomesDeadline(){OpenLoopExtractor.Candidate x=OpenLoopExtractor.extract("info","يرجى التوجه إلى قسم الحسابات يوم السبت الموافق 5/9/2026 لاستكمال سداد مصروفات القسط الأول",1788393600000L).get(0);assertEquals("deadline",x.kind);assertTrue(x.dueAt>0);}
    @Test public void datedReceiptStillIsNotDeadline(){assertTrue(OpenLoopExtractor.extract("Google Play","Order date 3/9/2026. Payment method Visa. Your subscription renewed.",1).isEmpty());}
    @Test public void politenessWithoutActionIsNotARequest(){assertTrue(OpenLoopExtractor.extract("Alex","Thanks, please 🙂",1).isEmpty());}
    @Test public void identicalRequestsFromDifferentPeopleStaySeparate(){OpenLoopExtractor.Candidate x=OpenLoopExtractor.extract("","Please send me the file",1).get(0);assertNotEquals(x.scopedFingerprint("com.whatsapp|alex"),x.scopedFingerprint("com.whatsapp|mona"));}
}
