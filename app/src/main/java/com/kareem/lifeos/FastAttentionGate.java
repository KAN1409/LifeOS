package com.kareem.lifeos;

import com.kareem.lifeos.context.RawObservation;
import java.util.List;
import java.util.Locale;

/**
 * Instant, background-safe provisional triage.
 *
 * This is deliberately NOT the semantic authority. It combines source structure with the existing
 * conservative local extractor only to reserve obvious actionable evidence immediately. Gemini
 * Nano later confirms/rejects the provisional item from the durable queue. No source-open/read
 * signal can mark an item handled.
 */
final class FastAttentionGate {
    static final class Result {
        final boolean provisional;
        final int queuePriority,attentionPriority;
        final String type,intent,urgency,action,summary,reason;
        final double confidence;
        Result(boolean provisional,int queuePriority,int attentionPriority,String type,String intent,
               String urgency,String action,String summary,String reason,double confidence){
            this.provisional=provisional;this.queuePriority=queuePriority;this.attentionPriority=attentionPriority;
            this.type=type;this.intent=intent;this.urgency=urgency;this.action=action;this.summary=summary;
            this.reason=reason;this.confidence=confidence;
        }
    }

    private FastAttentionGate(){}

    static Result evaluate(LifeDb.Event event,RawObservation raw,long now){
        int queue=structuralQueuePriority(raw);
        if(event==null||raw==null)return none(queue);
        boolean human=LifeDb.isConversationLike(event)||"true".equalsIgnoreCase(raw.attributes.get("structured_message"));

        List<OpenLoopExtractor.Candidate> candidates=OpenLoopExtractor.extract(event.title,event.body,now);
        OpenLoopExtractor.Candidate best=null;
        for(OpenLoopExtractor.Candidate c:candidates){
            if(c==null)continue;
            if(best==null||c.priority>best.priority||(c.priority==best.priority&&c.confidence>best.confidence))best=c;
        }

        // Egyptian/colloquial Arabic often expresses a direct request with a second-person
        // imperfect verb (تـ...) rather than the imperative form stored by older heuristics.
        // Detect the grammatical shape, not one sentence/verb. This is provisional only; Nano is
        // still the semantic authority and may reject it on the next foreground pass.
        if(best==null&&human&&looksLikeColloquialDirectRequest(event.body)){
            int priority=78;double confidence=.82;queue=Math.max(queue,priority);
            return new Result(true,queue,priority,"PERSON_CONVERSATION","REQUEST","MEDIUM","DO_TASK",
                    compact(event.body),"A direct conversational request was reserved immediately from request structure; deep on-device analysis is pending.",confidence);
        }
        if(best==null)return none(queue);
        queue=Math.max(queue,best.priority);

        boolean humanOnly="request".equals(best.kind)||"commitment".equals(best.kind)||"appointment".equals(best.kind);
        if(best.confidence<.80||(humanOnly&&!human))return none(queue);

        String type=human?"PERSON_CONVERSATION":"OTHER";
        String intent="REQUEST",action="REVIEW";
        if("security".equals(best.kind)){type="SECURITY_ALERT";intent="ALERT";action="VERIFY";}
        else if("financial_alert".equals(best.kind)){type="FINANCIAL_ALERT";intent="ALERT";action="REVIEW";}
        else if("commitment".equals(best.kind)){intent="COMMITMENT";action="REVIEW";}
        else if("appointment".equals(best.kind)){intent="SCHEDULE";action="REVIEW";}
        else if("request".equals(best.kind)){intent="REQUEST";action="DO_TASK";}
        else if("deadline".equals(best.kind)){intent="REQUEST";action="REVIEW";}

        String urgency=best.priority>=90?"HIGH":best.priority>=68?"MEDIUM":"LOW";
        return new Result(true,queue,best.priority,type,intent,urgency,action,best.title,
                "Potentially actionable evidence was reserved immediately; deep on-device analysis is pending.",
                best.confidence);
    }

    /**
     * High-recall grammatical reservation for conversational Arabic. Two independent signals are
     * required: a request/urgency cue plus a plausible second-person verbal form. This intentionally
     * avoids a verb dictionary so new actions do not require new code paths.
     */
    static boolean looksLikeColloquialDirectRequest(String text){
        String n=normalizeArabic(text);if(n.isEmpty())return false;
        boolean cue=n.contains(" ضروري ")||n.contains(" لازم ")||n.contains(" ممكن ")||n.contains(" لو سمحت ")||n.contains(" من فضلك ")||n.contains(" محتاج ")||n.contains(" محتاجك ");
        if(!cue)return false;
        String[] tokens=n.trim().split("\\s+");
        for(String token:tokens){
            String t=stripArabicPunctuation(token);
            if(t.length()<4||t.length()>18||t.charAt(0)!='ت')continue;
            // A second-person verb is much more plausible than a noun when it carries a personal
            // clitic (لي/ني/نا/ها/هم/ه) or appears immediately in a request construction.
            if(t.endsWith("لي")||t.endsWith("ني")||t.endsWith("نا")||t.endsWith("ها")||t.endsWith("هم")||t.endsWith("ه")||cue)return true;
        }
        return false;
    }

    private static String normalizeArabic(String value){
        String x=" "+safe(value).toLowerCase(Locale.ROOT)+" ";
        x=x.replace('أ','ا').replace('إ','ا').replace('آ','ا').replace('ى','ي').replace('ؤ','و').replace('ئ','ي');
        x=x.replaceAll("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]","");
        x=x.replaceAll("[^\\p{L}\\p{N}]+"," ").replaceAll("\\s+"," ");
        return " "+x.trim()+" ";
    }
    private static String stripArabicPunctuation(String x){return safe(x).replaceAll("[^\\p{L}\\p{N}]","");}
    private static String compact(String x){x=safe(x).trim().replaceAll("\\s+"," ");return x.length()<=180?x:x.substring(0,180)+"…";}

    static int structuralQueuePriority(RawObservation raw){
        if(raw==null)return 20;
        int score=25;
        if("true".equalsIgnoreCase(raw.attributes.get("structured_message")))score+=35;
        String category=safe(raw.attributes.get("category")).toLowerCase(Locale.ROOT);
        if(category.contains("msg")||category.contains("email")||category.contains("call")||category.contains("event")||category.contains("reminder")||category.contains("alarm"))score+=20;
        if("true".equalsIgnoreCase(raw.attributes.get("ongoing")))score-=20;
        String app=safe(raw.sourcePackage).toLowerCase(Locale.ROOT);
        if(app.equals("android")||app.contains("systemui"))score-=20;
        return Math.max(0,Math.min(100,score));
    }

    private static Result none(int queue){return new Result(false,queue,0,"OTHER","NONE","NONE","NONE","","",0);}
    private static String safe(String x){return x==null?"":x;}
}
