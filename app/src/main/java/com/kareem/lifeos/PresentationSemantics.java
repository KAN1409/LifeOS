package com.kareem.lifeos;

import android.content.Context;
import java.util.Locale;

/** User-facing semantic compression. Raw capture is evidence, never the default presentation. */
final class PresentationSemantics {
    private PresentationSemantics(){}

    static NotificationMeaning meaning(Context c,LifeDb.Event e){
        if(e==null)return null;NotificationMeaningStore store=NotificationMeaningStore.get(c);
        AttentionStore.Item item=AttentionStore.get(c).forEvent(e.id);
        if(item!=null&&!item.sourceObservationId.isEmpty()){NotificationMeaning exact=store.forObservation(item.sourceObservationId);if(exact!=null)return exact;}
        return store.forStreamAt(e.threadKey,e.at);
    }

    static String title(Context c,LifeDb.Event e){
        if(e==null)return "Activity";NotificationMeaning m=meaning(c,e);String all=clean(e.title)+" "+clean(e.body);
        if(educationPayment(all,m))return "School fee payment";
        if(carBooking(all,m)){
            String source=cleanLabel(e.title);String subject=m==null?"":subjectFromSummary(m.summary);String combined=norm(source+" "+subject+" "+(m==null?"":m.summary));
            if(combined.contains("glio"))return "GLIO Car Care";
            if(usefulSubject(subject)&&isMostlyLatin(subject))return UserFacingText.humanize(clip(subject,48));
            if(usefulSubject(source)&&isMostlyLatin(source))return UserFacingText.humanize(clip(source,48));
            return "Car care booking";
        }
        if(isCallLike(c,e)){String who=cleanIdentity(LifeDb.personLabel(e));if(usefulSubject(who))return UserFacingText.humanize(clip(who,48));}
        if(m!=null&&m.canSummarize()){
            if("PERSON_CONVERSATION".equals(m.type)){String who=cleanIdentity(LifeDb.personLabel(e));if(usefulSubject(who))return UserFacingText.humanize(clip(who,48));}
            String subject=subjectFromSummary(m.summary);if(usefulSubject(subject)&&isMostlyLatin(subject))return UserFacingText.humanize(clip(subject,48));
        }
        String source=cleanLabel(e.title);String fromMeta=identityFromMetaTitle(source);if(usefulSubject(fromMeta))return UserFacingText.humanize(clip(fromMeta,48));
        if(LifeDb.isConversationLike(e)){String who=cleanIdentity(LifeDb.personLabel(e));if(usefulSubject(who))return UserFacingText.humanize(clip(who,48));}
        if(!generic(source,e.app)&&safeLabel(source))return UserFacingText.humanize(clip(source,48));
        if(m!=null&&m.canSummarize()){String t=compactTitle(m.summary);if(!t.isEmpty()&&isMostlyLatin(t))return UserFacingText.humanize(t);}
        return LifeDb.friendlyApp(e.app);
    }

    static String summary(Context c,LifeDb.Event e){
        if(e==null)return "";NotificationMeaning m=meaning(c,e);String all=clean(e.title)+" "+clean(e.body);
        if(educationPayment(all,m)){
            if(m!=null&&"RESOLVED".equals(m.state))return "The installment payment appears resolved.";
            return "First installment is still unpaid.";
        }
        if(carBooking(all,m))return "They need your car details to confirm the booking.";
        if(bookingRequest(all,m))return "They need information from you to confirm the booking.";
        if(isCallLike(c,e)){String n=norm(e.title+" "+e.body);if(n.contains("incoming call")||n.contains("اتصال وارد")||n.contains("مكالمة واردة"))return "Incoming call.";return "Missed call.";}
        if(m!=null&&m.canSummarize()){
            String semantic=UserFacingText.humanize(clip(oneLine(m.summary),116));
            if(!unsafeRawEvidence(semantic))return semantic;
            return safeSourceSummary(e);
        }
        String b=clean(e.body);if(platformActivity(b))return "";
        if(LifeDb.isConversationLike(e)&&unsafeRawEvidence(b))return safeSourceSummary(e);
        String first=firstLine(b);if(!first.isEmpty()&&!sameLoose(first,title(c,e)))return UserFacingText.humanize(clip(first,112));
        return "Activity from "+LifeDb.friendlyApp(e.app)+".";
    }

    static String status(Context c,LifeDb.Event e){NotificationMeaning m=meaning(c,e);if(m==null)return "";if("WAITING_ON_USER".equals(m.state))return "Waiting on you";if("WAITING_ON_OTHER".equals(m.state))return "Waiting on them";if("PAY".equals(m.action))return "Payment due";if("HIGH".equals(m.urgency))return "Important";if("REQUEST".equals(m.intent))return "Action needed";if("COMMITMENT".equals(m.intent))return "Commitment";if("SCHEDULE".equals(m.intent))return "Scheduled";return "";}
    static int statusColor(Context c,LifeDb.Event e,String status){String s=norm(status);if(s.contains("waiting on you")||s.contains("action needed"))return LifeOsUi.BLUE;if(s.contains("scheduled")||s.contains("appointment"))return LifeOsUi.PURPLE;if(s.contains("commitment")||s.contains("waiting on them"))return LifeOsUi.GREEN;if(s.contains("due")||s.contains("overdue")||s.contains("important")||s.contains("payment"))return LifeOsUi.RED;NotificationMeaning m=meaning(c,e);if(m!=null&&"WAITING_ON_USER".equals(m.state))return LifeOsUi.BLUE;return LifeOsUi.MUTED;}
    static int accent(Context c,LifeDb.Event e){NotificationMeaning m=meaning(c,e);if(m!=null){if("HIGH".equals(m.urgency)||"SECURITY_ALERT".equals(m.type)||"FINANCIAL_ALERT".equals(m.type)||"PAY".equals(m.action))return LifeOsUi.RED;if("WAITING_ON_USER".equals(m.state)||"REQUEST".equals(m.intent))return LifeOsUi.BLUE;if("CALENDAR_EVENT".equals(m.type)||"SCHEDULE".equals(m.intent))return LifeOsUi.PURPLE;if("COMMITMENT".equals(m.intent))return LifeOsUi.GREEN;}if(isCallLike(c,e))return LifeOsUi.RED;return LifeOsUi.MUTED;}
    static int iconAccent(Context c,LifeDb.Event e){if(e==null)return LifeOsUi.MUTED;NotificationMeaning m=meaning(c,e);String all=clean(e.title)+" "+clean(e.body);if(educationPayment(all,m))return LifeOsUi.AMBER;if(carBooking(all,m))return LifeOsUi.BLUE;String icon=iconName(c,e);if(LifeOsIconView.COMMITMENT.equals(icon))return LifeOsUi.GREEN;if(LifeOsIconView.EVENT.equals(icon))return LifeOsUi.PURPLE;if(LifeOsIconView.ALERT.equals(icon))return LifeOsUi.RED;return accent(c,e);}

    static String iconName(Context c,LifeDb.Event e){if(e==null)return LifeOsIconView.ACTIVITY;NotificationMeaning m=meaning(c,e);String all=clean(e.title)+" "+clean(e.body);if(educationPayment(all,m))return LifeOsIconView.SCHOOL;if(carBooking(all,m))return LifeOsIconView.CAR;if(m!=null){if("CALENDAR_EVENT".equals(m.type)||"SCHEDULE".equals(m.intent))return LifeOsIconView.EVENT;if("PERSON_CONVERSATION".equals(m.type))return LifeOsIconView.PEOPLE;if("COMMITMENT".equals(m.intent))return LifeOsIconView.COMMITMENT;if("PAY".equals(m.action))return LifeOsIconView.ALERT;if("SECURITY_ALERT".equals(m.type)||"FINANCIAL_ALERT".equals(m.type))return LifeOsIconView.ALERT;}return LifeOsIconView.ACTIVITY;}
    static boolean preferSemanticIcon(Context c,LifeDb.Event e){String n=iconName(c,e);return !LifeOsIconView.ACTIVITY.equals(n)&&!LifeOsIconView.PEOPLE.equals(n);}

    static boolean meaningfulTimeline(Context c,LifeDb.Event e){
        if(e==null)return false;NotificationMeaning m=meaning(c,e);String raw=norm(e.title+" "+e.body);String semantic=norm((m==null?"":m.summary)+" "+title(c,e));
        if(platformActivity(raw)||platformActivity(semantic)||looksPromotional(raw)||looksPromotional(semantic))return false;
        if(isCallLike(c,e))return true;
        if(m!=null){
            if("PROMOTION".equals(m.type)||"SYSTEM_EVENT".equals(m.type))return m.needsAttention()||!"NONE".equals(m.action);
            if(m.needsAttention()||!"NONE".equals(m.action)||"REQUEST".equals(m.intent)||"QUESTION".equals(m.intent)||"COMMITMENT".equals(m.intent)||"SCHEDULE".equals(m.intent)||"ALERT".equals(m.intent))return true;
            if("EMAIL".equals(m.type)||"CALENDAR_EVENT".equals(m.type)||"MISSED_CALL".equals(m.type)||"TRANSACTION".equals(m.type)||"DELIVERY".equals(m.type))return true;
            // Informational chat traffic is deliberately compressed out of the life timeline.
            if("PERSON_CONVERSATION".equals(m.type))return false;
        }
        if(LifeDb.isConversationLike(e))return explicitSignalText(e.body);
        return meaningfulMessageText(e.body)&&!clean(summary(c,e)).isEmpty();
    }

    static String timelineFingerprint(Context c,LifeDb.Event e){if(e==null)return "";NotificationMeaning m=meaning(c,e);if(isCallLike(c,e))return norm(e.app+"|call|"+cleanIdentity(LifeDb.personLabel(e)));if(LifeDb.isConversationLike(e)||(m!=null&&"PERSON_CONVERSATION".equals(m.type)))return norm(e.app+"|conversation|"+e.threadKey+"|"+cleanIdentity(LifeDb.personLabel(e)));return norm(e.app+"|"+title(c,e)+"|"+summary(c,e));}
    static long timelineCollapseWindowMs(Context c,LifeDb.Event e){if(isCallLike(c,e))return 3*60*1000L;NotificationMeaning m=meaning(c,e);if(LifeDb.isConversationLike(e)||(m!=null&&"PERSON_CONVERSATION".equals(m.type)))return 4*60*60*1000L;return 2*60*60*1000L;}
    static String attentionFingerprint(Context c,LifeDb.Event e){if(e==null)return "";NotificationMeaning m=meaning(c,e);if(LifeDb.isConversationLike(e)||(m!=null&&"PERSON_CONVERSATION".equals(m.type))){String who=cleanIdentity(LifeDb.personLabel(e));if(usefulSubject(who))return "conversation|"+norm(e.app+"|"+who);return "conversation|"+norm(e.app+"|"+e.threadKey);}String all=clean(e.title)+" "+clean(e.body);if(educationPayment(all,m))return "school-fee-payment";if(carBooking(all,m))return "car-booking|"+norm(LifeDb.personLabel(e));return norm(title(c,e)+"|"+summary(c,e));}
    static String kind(Context c,LifeDb.Event e){NotificationMeaning m=meaning(c,e);if(isCallLike(c,e))return "Call";if(m!=null){if("EMAIL".equals(m.type))return "Email";if("CALENDAR_EVENT".equals(m.type))return "Event";if("MISSED_CALL".equals(m.type))return "Call";if("TRANSACTION".equals(m.type))return "Payment";if("DELIVERY".equals(m.type))return "Delivery";if("PERSON_CONVERSATION".equals(m.type))return "Message";}return LifeDb.friendlyApp(e==null?"":e.app);}

    private static boolean educationPayment(String x,NotificationMeaning m){String n=norm(x);boolean money=n.contains("school fee")||n.contains("tuition")||n.contains("installment")||n.contains("first installment")||n.contains("مصروفات")||n.contains("القسط")||n.contains("قسط")||n.contains("أولياء الأمور")||n.contains("مدرسة");boolean payment=m!=null&&("PAY".equals(m.action)||"FINANCIAL_ALERT".equals(m.type)||"TRANSACTION".equals(m.type));return money&&(payment||n.contains("سداد")||n.contains("payment")||n.contains("unpaid"));}
    private static boolean carBooking(String x,NotificationMeaning m){String n=norm(x);boolean car=n.contains("car")||n.contains("vehicle")||n.contains("سيارة")||n.contains("العربية")||n.contains("ماركة السيارة")||n.contains("رقم اللوحة");boolean booking=n.contains("book")||n.contains("booking")||n.contains("appointment")||n.contains("حجز")||n.contains("الحجز")||n.contains("موعد");boolean request=m!=null&&("REQUEST".equals(m.intent)||"WAITING_ON_USER".equals(m.state));return car&&booking&&(request||n.contains("please")||n.contains("من فضلك")||n.contains("يرجى"));}
    private static boolean bookingRequest(String x,NotificationMeaning m){String n=norm(x);boolean booking=n.contains("book")||n.contains("booking")||n.contains("reservation")||n.contains("appointment")||n.contains("حجز")||n.contains("الحجز")||n.contains("تأكيد الحجز")||n.contains("موعد");boolean asks=n.contains("please")||n.contains("need")||n.contains("send")||n.contains("confirm")||n.contains("يرجى")||n.contains("محتاج")||n.contains("إرسال")||n.contains("ارسال")||n.contains("ابعت")||n.contains("تأكيد");boolean semantic=m!=null&&("REQUEST".equals(m.intent)||"WAITING_ON_USER".equals(m.state));return booking&&(asks||semantic);}
    private static boolean isCallLike(Context c,LifeDb.Event e){if(e==null)return false;NotificationMeaning m=meaning(c,e);if(m!=null&&"MISSED_CALL".equals(m.type))return true;String x=norm(e.title+" "+e.body);return x.contains("incoming call")||x.contains("missed call")||x.contains("tried to call")||x.contains("call back")||x.contains("حاول الاتصال")||x.contains("اتصال وارد")||x.contains("مكالمة واردة")||x.contains("مكالمة فائتة")||x.contains("مكالمة لم يرد عليها");}
    private static boolean meaningfulMessageText(String b){String x=clean(b);if(x.isEmpty())return false;if(platformActivity(x))return false;String[] words=norm(x).split(" ");return words.length>=3||x.length()>=20;}
    private static boolean explicitSignalText(String b){String x=norm(b);if(x.isEmpty())return false;for(String k:new String[]{"tomorrow","today","deadline","appointment","meeting","please send","need you","waiting","pay","payment","booking","confirm","بكرة","غدا","غداً","موعد","يرجى","لازم","محتاج","سداد","حجز","تأكيد","ابعت","ارسل","إرسال"})if(x.contains(k))return true;return false;}
    private static boolean platformActivity(String value){String x=norm(value);return x.contains("is typing")||x.contains("typing")||x.contains("replied to your chat")||x.contains("reacted to")||x.contains("liked your message")||x.contains("sent you a snap")||x.contains("you sent a snap")||x.contains("you sent a chat")||x.contains("sent a chat")||x.contains("snapchat you sent")||x.contains("added to their stor")||x.contains("viewed your story")||x.contains("story update")||x.contains("new story")||x.contains("following")||x.contains("voice note from snapchat")||x.contains("received a voice note")||x.contains("receiving a voice note")||x.contains("receiving a chat")||x.contains("received a chat")||x.contains("receiving an image")||x.contains("new snap from");}
    private static boolean looksPromotional(String x){return x.contains("% off")||x.contains(" discount ")||x.contains("promo")||x.contains("unlock your gift")||x.contains("earn egp")||x.contains("special offer");}
    private static String safeSourceSummary(LifeDb.Event e){if(e==null)return "";String who=cleanIdentity(LifeDb.personLabel(e));if(usefulSubject(who))return "Message from "+UserFacingText.humanize(clip(who,48))+".";return "Message from "+LifeDb.friendlyApp(e.app)+".";}
    private static boolean unsafeRawEvidence(String value){String x=clean(value);if(x.isEmpty())return false;String n=norm(x);if(containsLongNumber(x))return true;if(n.contains("instapay")||n.contains("bank account")||n.contains("account number")||n.contains("iban")||n.contains("transfer to")||n.contains("card number")||n.contains("انستاباي")||n.contains("انستا باي")||n.contains("تحويل")||n.contains("رقم الحساب"))return true;return x.length()>160;}
    private static boolean containsLongNumber(String x){int run=0;for(int i=0;i<x.length();i++){char ch=x.charAt(i);if(Character.isDigit(ch)){if(++run>=7)return true;}else if(ch!=' '&&ch!='-'&&ch!='+')run=0;}return false;}
    private static String identityFromMetaTitle(String s){String x=clean(s),low=x.toLowerCase(Locale.ROOT);for(String p:new String[]{"chat message from ","message from ","new message from ","missed call from ","call from "}){if(low.startsWith(p)){String v=x.substring(p.length()).trim();String lv=v.toLowerCase(Locale.ROOT);int cut=v.length();for(String stop:new String[]{" expressing"," saying"," asking"," sent"," with"," about"}){int i=lv.indexOf(stop);if(i>0&&i<cut)cut=i;}v=v.substring(0,cut).trim();return v;}}return "";}
    private static String cleanIdentity(String s){String x=clean(s);String meta=identityFromMetaTitle(x);return meta.isEmpty()?x:meta;}
    private static boolean safeLabel(String s){String x=clean(s);return !x.isEmpty()&&x.length()<=54&&!platformActivity(x)&&!x.contains("\n")&&!x.endsWith(".");}
    private static String subjectFromSummary(String s){String x=oneLine(UserFacingText.humanize(s));if(x.isEmpty())return "";for(String marker:new String[]{" is "," has "," needs "," requested "," requests "," will "," sent "," asks "}){int i=x.toLowerCase(Locale.ROOT).indexOf(marker);if(i>1&&i<58)return x.substring(0,i).trim();}return "";}
    private static boolean usefulSubject(String s){String x=norm(s);return !x.isEmpty()&&!x.equals("you")&&!x.equals("user")&&!x.equals("info")&&!x.equals("information")&&!x.equals("notification")&&!x.equals("message")&&!x.equals("gmail")&&!x.equals("whatsapp")&&!x.equals("visible conversation");}
    private static boolean generic(String title,String app){String x=norm(title);if(x.isEmpty()||x.equals("info")||x.equals("information")||x.equals("notification")||x.equals("message")||x.equals("new message")||x.equals("visible conversation"))return true;String a=norm(LifeDb.friendlyApp(app));return x.equals(a);}
    private static String cleanLabel(String s){String x=clean(s);int dot=x.indexOf(" · ");if(dot>0)x=x.substring(0,dot);return x;}
    private static String compactTitle(String s){String x=clean(s);if(x.isEmpty())return "";int cut=x.indexOf('.');if(cut>12)x=x.substring(0,cut);cut=x.indexOf('،');if(cut>12)x=x.substring(0,cut);cut=x.indexOf(';');if(cut>12)x=x.substring(0,cut);return clip(x,52);}
    private static String firstLine(String s){String x=clean(s);if(x.isEmpty())return "";int n=x.indexOf('\n');return n>=0?x.substring(0,n).trim():x;}
    private static String oneLine(String s){return clean(s).replaceAll("\\s+"," ");}
    private static boolean isMostlyLatin(String s){String x=clean(s);int latin=0,letters=0;for(int i=0;i<x.length();i++){char ch=x.charAt(i);if(Character.isLetter(ch)){letters++;if(ch<128)latin++;}}return letters==0||latin>=Math.ceil(letters*.65);}
    private static boolean sameLoose(String a,String b){return norm(a).equals(norm(b));}
    private static String norm(String x){return clean(x).toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+"," ").trim();}
    private static String clean(String x){return x==null?"":x.trim();}
    private static String clip(String x,int n){String v=clean(x);return v.length()>n?v.substring(0,n)+"…":v;}
}
