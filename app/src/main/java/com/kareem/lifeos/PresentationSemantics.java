package com.kareem.lifeos;

import android.content.Context;
import java.util.Locale;

/** User-facing semantic compression. Raw capture is evidence, never the default presentation. */
final class PresentationSemantics {
    private PresentationSemantics(){}

    static NotificationMeaning meaning(Context c,LifeDb.Event e){return e==null?null:NotificationMeaningStore.get(c).forStream(e.threadKey);}
    static String title(Context c,LifeDb.Event e){if(e==null)return "Activity";String source=clean(e.title);if(!generic(source,e.app))return UserFacingText.humanize(clip(source,48));NotificationMeaning m=meaning(c,e);if(m!=null&&m.canSummarize()){String t=compactTitle(m.summary);if(!t.isEmpty())return UserFacingText.humanize(t);}String first=firstLine(e.body);if(!first.isEmpty())return UserFacingText.humanize(clip(first,52));return LifeDb.friendlyApp(e.app);}
    static String summary(Context c,LifeDb.Event e){if(e==null)return "";NotificationMeaning m=meaning(c,e);if(m!=null&&m.canSummarize())return UserFacingText.humanize(clip(m.summary,116));String b=clean(e.body);String first=firstLine(b);if(!first.isEmpty()&&!sameLoose(first,title(c,e)))return UserFacingText.humanize(clip(first,112));int nl=b.indexOf('\n');if(nl>=0&&nl<b.length()-1)return UserFacingText.humanize(clip(b.substring(nl+1).trim(),112));return UserFacingText.humanize(clip(b,112));}
    static String status(Context c,LifeDb.Event e){NotificationMeaning m=meaning(c,e);if(m==null)return "";if("WAITING_ON_USER".equals(m.state))return "Waiting on you";if("WAITING_ON_OTHER".equals(m.state))return "Waiting on them";if("HIGH".equals(m.urgency))return "Important";if(!"NONE".equals(m.intent))return human(m.intent);return "";}
    static int accent(Context c,LifeDb.Event e){NotificationMeaning m=meaning(c,e);if(m!=null){if("HIGH".equals(m.urgency)||"SECURITY_ALERT".equals(m.type)||"FINANCIAL_ALERT".equals(m.type))return LifeOsUi.RED;if("WAITING_ON_USER".equals(m.state)||"REQUEST".equals(m.intent))return LifeOsUi.BLUE;if("CALENDAR_EVENT".equals(m.type)||"SCHEDULE".equals(m.intent))return LifeOsUi.PURPLE;if("TRANSACTION".equals(m.type))return LifeOsUi.GREEN;}return LifeOsUi.MUTED;}
    static boolean meaningfulTimeline(Context c,LifeDb.Event e){if(e==null)return false;String x=norm(e.title+" "+e.body);if(x.contains("is typing")||x.contains("typing ")||x.contains("replied to your chat")||x.contains("reacted to")||x.contains("liked your message")||x.contains("sent you a snap"))return false;NotificationMeaning m=meaning(c,e);if(m!=null&&"PROMOTION".equals(m.type)&&"NONE".equals(m.action))return false;if(looksPromotional(x))return false;return !clean(summary(c,e)).isEmpty();}
    static String kind(Context c,LifeDb.Event e){NotificationMeaning m=meaning(c,e);if(m!=null){if("EMAIL".equals(m.type))return "Email";if("CALENDAR_EVENT".equals(m.type))return "Event";if("MISSED_CALL".equals(m.type))return "Call";if("TRANSACTION".equals(m.type))return "Payment";if("DELIVERY".equals(m.type))return "Delivery";if("PERSON_CONVERSATION".equals(m.type))return "Message";}return LifeDb.friendlyApp(e==null?"":e.app);}

    private static boolean looksPromotional(String x){return x.contains("% off")||x.contains(" discount ")||x.contains("promo")||x.contains("unlock your gift")||x.contains("earn egp")||x.contains("special offer");}
    private static boolean generic(String title,String app){String x=norm(title);if(x.isEmpty()||x.equals("info")||x.equals("information")||x.equals("notification")||x.equals("message")||x.equals("new message")||x.equals("visible conversation"))return true;String a=norm(LifeDb.friendlyApp(app));return x.equals(a);}
    private static String compactTitle(String s){String x=clean(s);if(x.isEmpty())return "";int cut=x.indexOf('.');if(cut>12)x=x.substring(0,cut);cut=x.indexOf('،');if(cut>12)x=x.substring(0,cut);cut=x.indexOf(';');if(cut>12)x=x.substring(0,cut);return clip(x,52);}
    private static String firstLine(String s){String x=clean(s);if(x.isEmpty())return "";int n=x.indexOf('\n');return n>=0?x.substring(0,n).trim():x;}
    private static boolean sameLoose(String a,String b){return norm(a).equals(norm(b));}
    private static String human(String x){String v=clean(x).toLowerCase(Locale.ROOT).replace('_',' ');return v.isEmpty()?"":Character.toUpperCase(v.charAt(0))+v.substring(1);}
    private static String norm(String x){return clean(x).toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+"," ").trim();}
    private static String clean(String x){return x==null?"":x.trim();}
    private static String clip(String x,int n){String v=clean(x);return v.length()>n?v.substring(0,n)+"…":v;}
}
