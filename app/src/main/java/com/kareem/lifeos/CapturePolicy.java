package com.kareem.lifeos;

import java.util.Locale;
import java.util.HashSet;
import java.util.Set;

final class CapturePolicy {
    private CapturePolicy(){}
    static boolean isNotificationSummary(String value){if(value==null)return false;String x=value.toLowerCase(Locale.ROOT).trim();return x.matches("\\d+\\s+new messages?")||x.matches("\\d+\\s+رسائل?\\s+جديدة")||x.matches("لديك\\s+\\d+\\s+رسائل?.*");}
    static boolean isLauncherSnapshot(String value){if(value==null)return false;String x=value.toLowerCase(Locale.ROOT);return x.contains("play store")&&x.contains("device care")&&x.contains("gallery")&&x.contains("calculator");}
    static boolean isMessagingHomeSnapshot(String value){if(value==null)return false;String x=value.toLowerCase(Locale.ROOT);int signals=0;if(x.contains("ask meta ai or search"))signals++;if(x.contains("edit your sent messages"))signals++;if(x.contains("touch and hold on a chat"))signals++;if(x.contains("create channel")||x.contains("find channels to follow"))signals++;if(x.contains("start your community")||x.contains("communities bring members together"))signals++;return signals>=2;}
    static boolean isUnreadMarker(String value){if(value==null)return false;String x=value.toLowerCase(Locale.ROOT).trim();return x.matches("\\d+\\s+unread messages?")||x.matches("\\d+\\s+رسائل?\\s+غير مقروءة");}
    static boolean sameConversationSnapshot(String first,String second){
        Set<String> a=segments(first),b=segments(second);if(a.isEmpty()||b.isEmpty())return false;
        int common=0;for(String x:a)if(b.contains(x))common++;
        return common>=3;
    }
    static boolean screenThreadMatchesNotification(String screenThread,String notificationTitle){if(screenThread==null||notificationTitle==null)return false;String title=notificationTitle.toLowerCase(Locale.ROOT).trim();return title.length()>2&&screenThread.toLowerCase(Locale.ROOT).endsWith("|"+title);}
    private static Set<String> segments(String value){
        HashSet<String> out=new HashSet<>();if(value==null)return out;
        for(String raw:value.split("·")){String x=raw.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+"," ");if(x.length()<3||isUnreadMarker(x)||x.matches("\\d{1,2}:\\d{2}\\s*(am|pm)?")||x.equals("today")||x.equals("message"))continue;out.add(x);}
        return out;
    }
}
