package com.kareem.lifeos;

import java.util.List;
import java.util.Locale;

/** Product-level promotion gate. Capture may be broad; canonical truth is deliberately conservative. */
final class CanonicalSemanticPolicy {
    private CanonicalSemanticPolicy(){}

    static boolean isPlatformOrCompletedActivity(String text){String x=norm(text);if(x.isEmpty())return false;return containsAny(x,
            "you reacted","reacted to","you are responding","you responded","you replied","added you back","started following you","liked your","sent a reel","sent a reaction",
            "message from ","image is ready","ready to review","ready to view","generation complete","download complete","downloaded successfully","app installed","installed successfully",
            "analyzing the current","processing in the background","sync complete","backup complete");}

    static boolean isPromotionOrMarketing(String text){String x=norm(text);if(x.isEmpty())return false;return containsAny(x,
            "sponsored","limited offer","special offer","promo code","promotion","shop now","sale ends","free voucher","unlock your free","claim your free","deal ends","save up to",
            "we lowered our","new school year, fresh home","discount","خصم","عرض خاص","إعلان ممول","تسوق الآن");}

    /** Legacy overload remains for non-conversation callers/tests; person attention must use a stored exact meaning. */
    static boolean isCanonicalAttention(AttentionStore.Item item,LifeDb.Event event){return isCanonicalAttention(item,event,null);}

    static boolean isCanonicalAttention(AttentionStore.Item item,LifeDb.Event event,NotificationMeaning meaning){
        if(item==null||event==null)return false;if(!AttentionStore.OPEN.equals(item.status))return false;if(item.confidence<NotificationMeaning.ATTENTION_CONFIDENCE)return false;
        String text=event.title+" "+event.body+" "+item.summary+" "+item.reason;if(isPlatformOrCompletedActivity(text)||isPromotionOrMarketing(text))return false;
        EventSemantics.Assessment eventType=EventSemantics.classify(event);if(eventType.type==EventSemantics.Type.PROMOTION||eventType.type==EventSemantics.Type.CONTENT_READY||eventType.type==EventSemantics.Type.SYSTEM_EVENT)return false;
        String action=upper(item.action),intent=upper(item.intent),type=upper(item.type);if(action.isEmpty()||"NONE".equals(action))return false;
        if(eventType.personConversation){
            if(meaning==null||!meaning.sourceObservationId.equals(item.sourceObservationId)||!meaning.needsAttention())return false;
            if(!"PERSON_CONVERSATION".equals(meaning.type)||!intent.equals(meaning.intent)||!action.equals(meaning.action))return false;
            return exactTargetEvidenceSupportsObligation(event,meaning);
        }
        return "SECURITY_ALERT".equals(type)||"FINANCIAL_ALERT".equals(type)||"MISSED_CALL".equals(type)||"REMINDER".equals(type)||"CALENDAR_EVENT".equals(type);
    }

    /** Independent verifier used before a model result can ever create person attention. */
    static boolean exactTargetEvidenceSupportsObligation(LifeDb.Event event,NotificationMeaning meaning){
        if(event==null||meaning==null||!"PERSON_CONVERSATION".equals(meaning.type)||!meaning.needsAttention())return false;
        String span=meaning.evidenceSpan.trim();if(span.isEmpty()||!containsNormalized(event.body,span))return false;
        String intent=upper(meaning.intent);if(!("REQUEST".equals(intent)||"QUESTION".equals(intent)||"COMMITMENT".equals(intent)||"SCHEDULE".equals(intent)))return false;
        String body=norm(event.body);
        if("QUESTION".equals(intent))return event.body.contains("?")||event.body.contains("؟")||startsLikeDirectQuestion(body);
        List<OpenLoopExtractor.Candidate> xs=OpenLoopExtractor.extract(event.title,event.body,event.at);
        for(OpenLoopExtractor.Candidate x:xs)if(x!=null&&x.confidence>=.80&&("request".equals(x.kind)||"commitment".equals(x.kind)||"appointment".equals(x.kind)||"deadline".equals(x.kind)))return true;
        return FastAttentionGate.looksLikeColloquialDirectRequest(event.body)||hasDirectRequestStructure(body);
    }

    private static boolean hasDirectRequestStructure(String x){return containsAny(" "+x+" ",
            " please "," can you "," could you "," would you "," will you "," i need you to "," need you to ",
            " يرجى "," لو سمحت "," من فضلك "," ممكن "," عايزك "," عاوزك "," محتاجك ");}
    private static boolean startsLikeDirectQuestion(String x){return x.startsWith("can you ")||x.startsWith("could you ")||x.startsWith("would you ")||x.startsWith("will you ")||x.startsWith("do you ")||x.startsWith("did you ")||x.startsWith("are you ")||x.startsWith("ممكن ")||x.startsWith("هل ")||x.startsWith("امتى ")||x.startsWith("فين ")||x.startsWith("ليه ");}
    private static boolean containsNormalized(String target,String span){String t=norm(target),s=norm(span);return !s.isEmpty()&&t.contains(s);}

    static boolean isCanonicalTimeline(LifeDb.Event event,NotificationMeaning meaning){
        if(event==null)return false;String text=event.title+" "+event.body+(meaning==null?"":" "+meaning.summary+" "+meaning.reason);if(isPlatformOrCompletedActivity(text)||isPromotionOrMarketing(text))return false;
        if(meaning!=null&&meaning.confidence>=NotificationMeaning.SUMMARY_CONFIDENCE){String type=upper(meaning.type);if("PROMOTION".equals(type)||"CONTENT_READY".equals(type)||"SYSTEM_EVENT".equals(type)||"OTHER".equals(type))return false;if("PERSON_CONVERSATION".equals(type)||"EMAIL".equals(type)||"CALENDAR_EVENT".equals(type)||"MISSED_CALL".equals(type)||"REMINDER".equals(type)||"SECURITY_ALERT".equals(type)||"FINANCIAL_ALERT".equals(type)||"TRANSACTION".equals(type)||"DELIVERY".equals(type))return true;}
        EventSemantics.Type t=EventSemantics.classify(event).type;return t==EventSemantics.Type.PERSON_CONVERSATION||t==EventSemantics.Type.SECURITY_ALERT||t==EventSemantics.Type.FINANCIAL_ALERT||t==EventSemantics.Type.TRANSACTION||t==EventSemantics.Type.DELIVERY;
    }

    static String canonicalTimelineKind(LifeDb.Event event,NotificationMeaning meaning){if(meaning!=null&&meaning.confidence>=NotificationMeaning.SUMMARY_CONFIDENCE){String t=upper(meaning.type);if("PERSON_CONVERSATION".equals(t))return "Message";if("EMAIL".equals(t))return "Email";if("CALENDAR_EVENT".equals(t)||"REMINDER".equals(t))return "Event";if("MISSED_CALL".equals(t))return "Call";if("SECURITY_ALERT".equals(t))return "Security";if("FINANCIAL_ALERT".equals(t)||"TRANSACTION".equals(t))return "Finance";if("DELIVERY".equals(t))return "Delivery";}EventSemantics.Type t=EventSemantics.classify(event).type;if(t==EventSemantics.Type.PERSON_CONVERSATION)return "Message";if(t==EventSemantics.Type.SECURITY_ALERT)return "Security";if(t==EventSemantics.Type.FINANCIAL_ALERT||t==EventSemantics.Type.TRANSACTION)return "Finance";if(t==EventSemantics.Type.DELIVERY)return "Delivery";return "Event";}
    static String canonicalTimelineSummary(ContextText context,LifeDb.Event event,NotificationMeaning meaning){if(meaning!=null&&meaning.canSummarize()&&!meaning.summary.trim().isEmpty())return UserFacingText.humanize(meaning.summary);return context.summary(event);}

    interface ContextText { String summary(LifeDb.Event event); }
    private static boolean containsAny(String text,String... values){for(String v:values)if(text.contains(v))return true;return false;}
    private static String upper(String x){return x==null?"":x.trim().toUpperCase(Locale.ROOT);}
    private static String norm(String x){return x==null?"":x.toLowerCase(Locale.ROOT).replaceAll("\\s+"," ").trim();}
}
