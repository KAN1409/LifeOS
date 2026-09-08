package com.kareem.lifeos;

public final class ManualSelfTest {
    public static void main(String[] args) {
        check("request", OpenLoopExtractor.extract("Alex", "Please send me the plan", 1).get(0).kind);
        check("commitment", OpenLoopExtractor.extract("أحمد", "هبعت لك الملف بكرة", 1).get(0).kind);
        check("appointment", OpenLoopExtractor.extract("Clinic", "Tomorrow at 6 pm", 1).get(0).kind);
        if (!OpenLoopExtractor.extract("Battery", "Charging complete", 1).isEmpty()) throw new AssertionError("ordinary status was extracted");
        String a=OpenLoopExtractor.extract("A", "Please send me X", 1).get(0).fingerprint;
        String b=OpenLoopExtractor.extract("A", "Please send me X", 2).get(0).fingerprint;
        check(a,b);
        if(!CapturePolicy.isNotificationSummary("3 new messages"))throw new AssertionError("English summary accepted");
        if(!CapturePolicy.isNotificationSummary("3 رسائل جديدة"))throw new AssertionError("Arabic summary accepted");
        if(CapturePolicy.isNotificationSummary("How are u"))throw new AssertionError("real message rejected");
        if(!OpenLoopExtractor.extract("","Today",1).isEmpty())throw new AssertionError("date separator became appointment");
        if(!CapturePolicy.isLauncherSnapshot("My Files Spotify Gallery Device care Calculator Play Store"))throw new AssertionError("launcher snapshot accepted");
        if(!CapturePolicy.isUnreadMarker("7 unread messages"))throw new AssertionError("unread marker accepted");
        String old="+20 11 44445113 · Sure i will send them at 9 pm · Today · Hi there · Test 1 · How are u · Test · 10:58 PM · 1 · 2 · 3 · Message";
        String latest="Kareem Abdel Nasser · Hey man · Sure i will send them at 9 pm · Hi there · Test 1 · How are u · Test · 1 · 2 · 3";
        if(!CapturePolicy.sameConversationSnapshot(old,latest))throw new AssertionError("overlapping conversation snapshots not collapsed");
        if(CapturePolicy.sameConversationSnapshot("Mona · Lunch tomorrow · Bring notes","Ahmed · Project update · Send invoice"))throw new AssertionError("unrelated conversations collapsed");
        String home="Ask Meta AI or Search · Edit your sent messages · Touch and hold on a chat for more options · Channels · Find channels to follow · CREATE CHANNEL · START YOUR COMMUNITY";
        if(!CapturePolicy.isMessagingHomeSnapshot(home))throw new AssertionError("WhatsApp home accepted as conversation");
        if(CapturePolicy.isMessagingHomeSnapshot("Kareem · Hi there · Test 1 · How are u"))throw new AssertionError("real conversation rejected");
        if(!CapturePolicy.screenThreadMatchesNotification("com.whatsapp|kareem abdel nasser","Kareem Abdel Nasser"))throw new AssertionError("notification not linked to visible chat");
        OpenLoopExtractor.Candidate security=OpenLoopExtractor.extract("Security Alert","Plaud now has access to your Google account. If you did not grant access, review activity.",1).get(0);
        check("security",security.kind);if(security.priority!=100)throw new AssertionError("security priority is not highest");
        OpenLoopExtractor.Candidate financial=OpenLoopExtractor.extract("CIB","Your card was charged EGP 49.99. You are over the limit by EGP 299.12",1).get(0);
        check("financial_alert",financial.kind);if(financial.priority!=95)throw new AssertionError("financial exception priority is wrong");
        OpenLoopExtractor.Candidate deadline=OpenLoopExtractor.extract("info","يرجى التوجه إلى قسم الحسابات يوم السبت الموافق 5/9/2026 لاستكمال سداد مصروفات القسط الأول",1788393600000L).get(0);
        check("deadline",deadline.kind);if(deadline.dueAt<=0)throw new AssertionError("deadline date was not parsed");
        if(!OpenLoopExtractor.extract("Google Play","Order date 3/9/2026. Payment method Visa. Your subscription renewed.",1).isEmpty())throw new AssertionError("dated receipt became deadline");
        if(!OpenLoopExtractor.extract("Alex","Thanks, please 🙂",1).isEmpty())throw new AssertionError("politeness without an action became request");
        OpenLoopExtractor.Candidate scoped=OpenLoopExtractor.extract("","Please send me the file",1).get(0);
        if(scoped.scopedFingerprint("com.whatsapp|alex").equals(scoped.scopedFingerprint("com.whatsapp|mona")))throw new AssertionError("same request from different people collapsed");
        System.out.println("LifeOS policy + extractor self-test: 22/22 PASS");
    }
    private static void check(String expected,String actual){if(!expected.equals(actual))throw new AssertionError("expected="+expected+" actual="+actual);}
}
