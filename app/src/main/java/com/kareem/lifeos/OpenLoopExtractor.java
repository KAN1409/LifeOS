package com.kareem.lifeos;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Conservative open-loop extractor. High precision is preferred over noisy attention items. */
final class OpenLoopExtractor {
    static final class Candidate {
        final String kind,title,fingerprint;
        final long dueAt;
        final double confidence;
        final int priority;
        Candidate(String kind,String title,long dueAt,double confidence,int priority){this.kind=kind;this.title=title;this.dueAt=dueAt;this.confidence=confidence;this.priority=priority;this.fingerprint=sha(kind+"\n"+canonical(title));}
        String scopedFingerprint(String threadKey){return sha(fingerprint+"\n"+canonical(threadKey));}
    }

    private static final Pattern TIME=Pattern.compile("(?i)(?:at|الساعة|الساعه)\\s*(1[0-2]|0?[1-9])(?::([0-5]\\d))?\\s*(am|pm|ص|م)?");
    private static final Pattern DATE_WORD=Pattern.compile("(?i)\\b(today|tomorrow|tonight|النهارده|النهاردة|بكرة|بكره|الليلة)\\b");
    private static final Pattern ABSOLUTE_DATE=Pattern.compile("(?<!\\d)([0-3]?\\d)[/-]([01]?\\d)(?:[/-](\\d{2,4}))?(?!\\d)");
    private static final String[] REQUEST={"please","can you","could you","send me","remind me","لو سمحت","ممكن","ابعتلي","فكرني","عايزك","عاوزك"};
    private static final String[] REQUEST_ACTION={"show","order","send","share","tell","check","review","call","book","buy","pay","bring","confirm","reply","forward","remind","open","اعمل","اطلب","ابعت","وريني","قول","شوف","راجع","كلم","احجز","اشتري","ادفع","هات","أكد","رد","فكر"};
    private static final String[] COMMITMENT={"i will","i'll","will send","i can send","هعمل","هبعت","هكلم","هخلص","هراجع","هروح"};
    private static final String[] SCHEDULE={"meeting","appointment","booked","booking","reservation","call at","meet at","موعد","حجز","مقابلة","اجتماع","هنقابل","هنتقابل","مكالمة الساعة","معاد"};
    private static final String[] TRANSACTIONAL={"charged","card #","credit card","debit card","transaction","purchase","egp ","usd ","sar ","payment","paid","receipt","invoice","renewed","renewal","subscription","google play","apple.com/bill","تم خصم","تم السحب","بطاقتك","عملية شراء","معاملة","دفع","فاتورة","اشتراك"};
    private static final String[] SYSTEM={"verification code","otp","one-time password","login code","password reset","delivery status","package delivered","تم التوصيل","رمز التحقق","كود التحقق"};
    private static final String[] SECURITY_RISK={"security alert","did not grant access","didn't grant access","has access to your google account","access to your account","new sign-in","new login","unrecognized login","unknown device","suspicious activity","تنبيه أمان","وصول إلى حسابك","تسجيل دخول جديد","نشاط مريب","جهاز غير معروف"};
    private static final String[] FINANCIAL_RISK={"over the limit","over your limit","credit limit exceeded","declined","payment failed","insufficient funds","past due","overdue","minimum payment due","تجاوزت الحد","تخطيت الحد","الحد الائتماني","مرفوضة","فشل الدفع","رصيد غير كاف","متأخر السداد","مستحق الدفع"};
    private static final String[] DEADLINE_CONTEXT={"deadline","due by","due on","must pay","please pay","complete payment","fees","tuition","accounts department","موعد أقصاه","مطلوب السداد","سداد","استكمال","مصروفات","قسط","قسم الحسابات","التوجه"};

    static List<Candidate> extract(String title,String body,long now){
        String safeTitle=clean(title),safeBody=clean(body);
        String text=(safeTitle+" "+safeBody).trim();
        String low=text.toLowerCase(Locale.ROOT);
        ArrayList<Candidate> out=new ArrayList<>();
        if(text.isEmpty())return out;

        long dueAt=parseDueAt(low,now);
        if(containsAny(low,SECURITY_RISK)){
            out.add(new Candidate("security",bestTitle(safeTitle,safeBody),dueAt,.97,100));
            return out;
        }
        if(containsAny(low,FINANCIAL_RISK)){
            out.add(new Candidate("financial_alert",bestTitle(safeTitle,safeBody),dueAt,.96,95));
            return out;
        }
        if(ABSOLUTE_DATE.matcher(low).find()&&containsAny(low,DEADLINE_CONTEXT)){
            out.add(new Candidate("deadline",bestTitle(safeTitle,safeBody),dueAt,.92,90));
            return out;
        }
        if(isMachineGenerated(low))return out;

        String kind=null;double confidence=0;
        if(containsAny(low,COMMITMENT)){kind="commitment";confidence=.84;}
        else if(containsAny(low,REQUEST)&&containsAny(low,REQUEST_ACTION)){kind="request";confidence=.82;}

        Matcher tm=TIME.matcher(low);
        boolean hasClock=tm.find();
        boolean explicitDateTime=hasClock&&DATE_WORD.matcher(low).find();
        boolean schedulingIntent=containsAny(low,SCHEDULE)||explicitDateTime;
        if(schedulingIntent&&hasClock){
            if(kind==null)kind="appointment";
            confidence=Math.max(confidence,.86);
        }else if(containsAny(low,SCHEDULE)&&kind==null){
            kind="appointment";confidence=.72;
        }

        if(kind!=null)out.add(new Candidate(kind,bestTitle(safeTitle,safeBody),dueAt,confidence,priority(kind)));
        return out;
    }

    private static boolean isMachineGenerated(String low){return containsAny(low,TRANSACTIONAL)||containsAny(low,SYSTEM);}
    private static boolean containsAny(String s,String[] xs){for(String x:xs)if(s.contains(x))return true;return false;}
    private static int priority(String kind){if("appointment".equals(kind))return 80;if("commitment".equals(kind))return 68;return 60;}
    private static String bestTitle(String title,String body){String x=body.isEmpty()?title:body;return clip(x.replaceAll("\\s+"," ").trim(),180);}
    private static long parseDueAt(String text,long now){
        try{
            Calendar c=Calendar.getInstance();c.setTimeInMillis(now);
            Matcher date=ABSOLUTE_DATE.matcher(text);boolean hasDate=date.find();
            if(hasDate){int day=Integer.parseInt(date.group(1)),month=Integer.parseInt(date.group(2))-1,year=c.get(Calendar.YEAR);String y=date.group(3);if(y!=null){year=Integer.parseInt(y);if(year<100)year+=2000;}c.set(Calendar.YEAR,year);c.set(Calendar.MONTH,month);c.set(Calendar.DAY_OF_MONTH,day);}
            else{Matcher relative=DATE_WORD.matcher(text);if(!relative.find())return 0;String word=relative.group().toLowerCase(Locale.ROOT);if(word.equals("tomorrow")||word.equals("بكرة")||word.equals("بكره"))c.add(Calendar.DAY_OF_YEAR,1);}
            Matcher time=TIME.matcher(text);if(time.find()){int hour=Integer.parseInt(time.group(1)),minute=time.group(2)==null?0:Integer.parseInt(time.group(2));String meridiem=time.group(3);if(meridiem!=null){String m=meridiem.toLowerCase(Locale.ROOT);if((m.equals("pm")||m.equals("م"))&&hour<12)hour+=12;if((m.equals("am")||m.equals("ص"))&&hour==12)hour=0;}c.set(Calendar.HOUR_OF_DAY,hour);c.set(Calendar.MINUTE,minute);}
            else{c.set(Calendar.HOUR_OF_DAY,23);c.set(Calendar.MINUTE,59);}c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);return c.getTimeInMillis();
        }catch(Exception ignored){return 0;}
    }
    private static String canonical(String s){return clean(s).toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+"," ").replaceAll("\\s+"," ").trim();}
    private static String clean(String s){return s==null?"":s.trim();}
    private static String clip(String s,int n){return s.length()<=n?s:s.substring(0,n)+"…";}
    private static String sha(String value){try{byte[] b=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder x=new StringBuilder();for(byte q:b)x.append(String.format(Locale.US,"%02x",q));return x.toString();}catch(Exception e){return Integer.toHexString(value.hashCode());}}
}
