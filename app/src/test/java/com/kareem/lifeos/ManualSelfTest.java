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
        System.out.println("LifeOS policy + extractor self-test: 16/16 PASS");
    }
    private static void check(String expected,String actual){if(!expected.equals(actual))throw new AssertionError("expected="+expected+" actual="+actual);}
}
