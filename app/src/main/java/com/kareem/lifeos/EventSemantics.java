package com.kareem.lifeos;

import java.util.Locale;

/**
 * Central semantic gate between captured evidence and user-facing objects.
 * Raw evidence is intentionally cheap to capture; promotion is intentionally strict.
 */
final class EventSemantics {
    enum Type {
        PERSON_CONVERSATION,
        SECURITY_ALERT,
        FINANCIAL_ALERT,
        TRANSACTION,
        DELIVERY,
        CONTENT_READY,
        PROMOTION,
        SYSTEM_EVENT,
        OTHER
    }

    static final class Assessment {
        final Type type;
        final boolean personConversation;
        final boolean worthSurfacing;
        Assessment(Type type,boolean personConversation,boolean worthSurfacing){this.type=type;this.personConversation=personConversation;this.worthSurfacing=worthSurfacing;}
    }

    private static final String[] SECURITY={"security alert","new sign-in","new login","unrecognized login","unknown device","suspicious activity","access to your account","has access to your google account","تنبيه أمان","تسجيل دخول جديد","جهاز غير معروف","نشاط مريب","وصول إلى حسابك"};
    private static final String[] FINANCIAL_ALERT={"declined","payment failed","insufficient funds","past due","overdue","over the limit","credit limit exceeded","minimum payment due","مرفوضة","فشل الدفع","رصيد غير كاف","متأخر السداد","تجاوزت الحد","الحد الائتماني","مستحق الدفع"};
    private static final String[] TRANSACTION={"charged","transaction","purchase","payment received","payment successful","receipt","invoice","credit card","debit card","تم خصم","تم السحب","عملية شراء","معاملة","فاتورة"};
    private static final String[] DELIVERY={"out for delivery","delivered","delivery status","shipment","package arriving","package delivered","تم الشحن","جاري التوصيل","تم التوصيل","الشحنة"};
    private static final String[] CONTENT_READY={"image ready","image is ready","ready to view","generation complete","download complete","downloaded successfully","export complete","render complete","جاهزة للعرض","الصورة جاهزة","اكتمل التنزيل"};
    private static final String[] PROMOTION={"sponsored","limited offer","special offer","discount","sale ends","shop now","promo code","promotion","خصم","عرض خاص","إعلان ممول","تسوق الآن"};
    private static final String[] SYSTEM={"verification code","one-time password","otp","login code","password reset","sync complete","backup complete","battery low","storage space","notification settings","رمز التحقق","كود التحقق","اكتملت المزامنة","البطارية منخفضة"};

    private EventSemantics(){}

    static Assessment classify(LifeDb.Event e){
        if(e==null)return new Assessment(Type.OTHER,false,false);
        String app=clean(e.app).toLowerCase(Locale.ROOT);
        String title=clean(e.title);
        String body=clean(e.body);
        String low=(title+" "+body).toLowerCase(Locale.ROOT);

        // Structural identity wins over words inside a human message. A person can
        // legitimately write "payment failed" without turning the conversation into
        // a bank transaction object.
        if(isMessagingApp(app)&&hasHumanConversationIdentity(e))
            return new Assessment(Type.PERSON_CONVERSATION,true,true);

        if(any(low,SECURITY))return new Assessment(Type.SECURITY_ALERT,false,true);
        if(any(low,FINANCIAL_ALERT))return new Assessment(Type.FINANCIAL_ALERT,false,true);
        if(any(low,DELIVERY))return new Assessment(Type.DELIVERY,false,true);
        if(any(low,CONTENT_READY))return new Assessment(Type.CONTENT_READY,false,false);
        if(any(low,PROMOTION))return new Assessment(Type.PROMOTION,false,false);
        if(any(low,SYSTEM)||isSystemPackage(app))return new Assessment(Type.SYSTEM_EVENT,false,false);
        if(any(low,TRANSACTION))return new Assessment(Type.TRANSACTION,false,true);
        return new Assessment(Type.OTHER,false,false);
    }

    static boolean isPersonConversation(LifeDb.Event e){return classify(e).personConversation;}

    /** Whether an extracted open loop is semantically compatible with its source evidence. */
    static boolean supportsLoop(LifeDb.Event e,String kind){
        if(e==null)return false;
        Assessment a=classify(e);
        String k=clean(kind).toLowerCase(Locale.ROOT);
        if("security".equals(k))return a.type==Type.SECURITY_ALERT;
        if("financial_alert".equals(k))return a.type==Type.FINANCIAL_ALERT;
        if("request".equals(k)||"commitment".equals(k))return a.personConversation;
        if("appointment".equals(k))return a.personConversation||isCalendarLike(e.app);
        if("deadline".equals(k))return a.type!=Type.PROMOTION&&a.type!=Type.SYSTEM_EVENT&&a.type!=Type.CONTENT_READY&&a.type!=Type.DELIVERY;
        return a.personConversation;
    }

    /** Low-level fallback feed policy. This is deliberately broader than Situation promotion. */
    static boolean shouldShowInToday(LifeDb.Event e){
        Type t=classify(e).type;
        return t==Type.PERSON_CONVERSATION||t==Type.SECURITY_ALERT||t==Type.FINANCIAL_ALERT||t==Type.TRANSACTION||t==Type.DELIVERY;
    }

    static String typeName(LifeDb.Event e){return classify(e).type.name();}

    private static boolean hasHumanConversationIdentity(LifeDb.Event e){
        String title=clean(e.title),thread=threadLabel(e.threadKey),app=clean(e.app);
        if("Visible conversation".equals(title))return humanLabel(thread,app);
        if(humanLabel(title,app))return true;
        return humanLabel(thread,app)&&!genericTitle(title,app);
    }

    private static boolean humanLabel(String value,String app){
        String x=clean(value);
        if(x.isEmpty()||x.startsWith("com.")||x.contains("|"))return false;
        if(genericTitle(x,app))return false;
        boolean letter=false;for(int i=0;i<x.length();i++)if(Character.isLetter(x.charAt(i))){letter=true;break;}
        return letter&&x.length()<=120;
    }

    private static boolean genericTitle(String value,String app){
        String x=clean(value).toLowerCase(Locale.ROOT);
        if(x.isEmpty()||x.equals("visible conversation")||x.equals("message")||x.equals("messages")||x.contains("new message")||x.contains("notification"))return true;
        String source=friendlyApp(app).toLowerCase(Locale.ROOT);
        return x.equals(source)||x.equals("whatsapp")||x.equals("messenger")||x.equals("telegram")||x.equals("signal");
    }

    private static boolean isMessagingApp(String app){return app.contains("whatsapp")||app.contains("telegram")||app.contains("facebook.orca")||app.contains("messenger")||app.contains("signal")||app.contains("android.apps.messaging");}
    private static boolean isCalendarLike(String app){String a=clean(app).toLowerCase(Locale.ROOT);return a.contains("calendar")||a.contains("planner");}
    private static boolean isSystemPackage(String app){return app.equals("android")||app.contains("systemui")||app.contains("permissioncontroller")||app.contains("packageinstaller");}
    private static boolean any(String s,String[] xs){for(String x:xs)if(s.contains(x))return true;return false;}
    private static String threadLabel(String value){String x=clean(value);int pipe=x.lastIndexOf('|');if(pipe>=0&&pipe<x.length()-1)x=x.substring(pipe+1).trim();return x;}
    private static String friendlyApp(String p){String x=clean(p);if(x.isEmpty())return "Unknown";int i=x.lastIndexOf('.');x=i>=0?x.substring(i+1):x;if(x.equalsIgnoreCase("gm"))return "Gmail";if(x.equalsIgnoreCase("chatgpt"))return "ChatGPT";return x;}
    private static String clean(String s){return s==null?"":s.trim();}
}
