package com.kareem.lifeos;

import org.json.JSONObject;

/** Compact, grounded meaning of one notification stream/conversation. */
final class NotificationMeaning {
    static final double SUMMARY_CONFIDENCE=.62;
    static final double ATTENTION_CONFIDENCE=.78;
    static final String[] TYPES={"PERSON_CONVERSATION","SECURITY_ALERT","FINANCIAL_ALERT","TRANSACTION","DELIVERY","CONTENT_READY","PROMOTION","SYSTEM_EVENT","OTHER"};
    static final String[] INTENTS={"REQUEST","QUESTION","COMMITMENT","SCHEDULE","INFORMATION","ALERT","NONE"};
    static final String[] STATES={"WAITING_ON_USER","WAITING_ON_OTHER","INFORMATIONAL","RESOLVED","UNKNOWN"};
    static final String[] URGENCIES={"HIGH","MEDIUM","LOW","NONE"};
    static final String[] ACTIONS={"REPLY","DO_TASK","VERIFY","PAY","REVIEW","NONE"};

    final String streamId,sourceObservationId,type,intent,state,urgency,action,summary,reason,model;
    final double confidence;
    final long understoodAt;

    NotificationMeaning(String streamId,String sourceObservationId,String type,String intent,String state,
                        String urgency,String action,String summary,String reason,double confidence,
                        String model,long understoodAt){
        this.streamId=safe(streamId);this.sourceObservationId=safe(sourceObservationId);
        this.type=allowed(type,TYPES,"OTHER");this.intent=allowed(intent,INTENTS,"NONE");
        this.state=allowed(state,STATES,"UNKNOWN");this.urgency=allowed(urgency,URGENCIES,"NONE");
        this.action=allowed(action,ACTIONS,"NONE");this.summary=clip(safe(summary),220);this.reason=clip(safe(reason),260);
        this.confidence=Math.max(0,Math.min(1,confidence));this.model=safe(model);this.understoodAt=understoodAt;
    }

    boolean canSummarize(){return confidence>=SUMMARY_CONFIDENCE&&!summary.isEmpty();}

    boolean needsAttention(){
        if(confidence<ATTENTION_CONFIDENCE)return false;
        if("WAITING_ON_USER".equals(state)&&!"NONE".equals(action))return true;
        return ("SECURITY_ALERT".equals(type)||"FINANCIAL_ALERT".equals(type))&&("HIGH".equals(urgency)||"MEDIUM".equals(urgency))&&!"NONE".equals(action);
    }

    String loopKind(){
        if("SECURITY_ALERT".equals(type))return "security";
        if("FINANCIAL_ALERT".equals(type))return "financial_alert";
        if("QUESTION".equals(intent))return "question";
        if("COMMITMENT".equals(intent))return "commitment";
        if("SCHEDULE".equals(intent))return "appointment";
        return "request";
    }

    int priority(){
        if("HIGH".equals(urgency))return 92;
        if("MEDIUM".equals(urgency))return 74;
        return 58;
    }

    static NotificationMeaning fromModel(JSONObject o,String streamId,String sourceObservationId,String model,long now){
        if(o==null)return null;
        String summary=o.optString("summary","").trim();
        if(summary.isEmpty())return null;
        return new NotificationMeaning(streamId,sourceObservationId,o.optString("type"),o.optString("intent"),
                o.optString("state"),o.optString("urgency"),o.optString("action"),summary,
                o.optString("reason"),o.optDouble("confidence",0),model,now);
    }

    private static String allowed(String value,String[] allowed,String fallback){String x=safe(value).trim().toUpperCase();for(String a:allowed)if(a.equals(x))return x;return fallback;}
    private static String safe(String x){return x==null?"":x;}
    private static String clip(String x,int n){return x.length()<=n?x:x.substring(0,n)+"…";}
}
