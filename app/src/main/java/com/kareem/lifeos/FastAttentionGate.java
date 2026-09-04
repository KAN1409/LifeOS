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
        List<OpenLoopExtractor.Candidate> candidates=OpenLoopExtractor.extract(event.title,event.body,now);
        OpenLoopExtractor.Candidate best=null;
        for(OpenLoopExtractor.Candidate c:candidates){
            if(c==null)continue;
            if(best==null||c.priority>best.priority||(c.priority==best.priority&&c.confidence>best.confidence))best=c;
        }
        if(best==null)return none(queue);
        queue=Math.max(queue,best.priority);

        boolean human=LifeDb.isConversationLike(event)||"true".equalsIgnoreCase(raw.attributes.get("structured_message"));
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
