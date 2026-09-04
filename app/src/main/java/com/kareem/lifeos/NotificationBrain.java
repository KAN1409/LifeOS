package com.kareem.lifeos;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import com.google.mlkit.genai.common.DownloadCallback;
import com.google.mlkit.genai.common.FeatureStatus;
import com.google.mlkit.genai.common.GenAiException;
import com.google.mlkit.genai.prompt.Candidate;
import com.google.mlkit.genai.prompt.GenerateContentResponse;
import com.google.mlkit.genai.prompt.Generation;
import com.google.mlkit.genai.prompt.java.GenerativeModelFutures;
import com.kareem.lifeos.context.RawObservation;
import com.kareem.lifeos.context.UniversalObservationStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Zero-config, on-device deep semantic brain for durably queued notification evidence.
 *
 * Background capture/queueing is immediate. AICore deep inference is consumed while LifeOS is
 * foreground. Each target observation is classified independently with chronological stream
 * context, so a later message can never erase an earlier actionable request.
 */
final class NotificationBrain {
    interface Listener { void onFinished(Result result); }
    static final class Result {
        final String status,model;final int analyzed,attention,pending;
        Result(String status,String model,int analyzed,int attention,int pending){this.status=status;this.model=model;this.analyzed=analyzed;this.attention=attention;this.pending=pending;}
    }

    private static final ExecutorService EXECUTOR=Executors.newSingleThreadExecutor();
    private static volatile boolean running;
    private static final int MAX_BATCH_ITEMS=6,MAX_MESSAGES_PER_TARGET=5,MAX_BATCHES_PER_RUN=4,MAX_MESSAGE_CHARS=420;

    private NotificationBrain(){}

    static void analyzeForeground(Context context,Listener listener){
        if(context==null)return;
        synchronized(NotificationBrain.class){if(running)return;running=true;}
        Context app=context.getApplicationContext();
        EXECUTOR.execute(()->{
            Result result;
            try{result=run(app);}catch(Throwable t){result=new Result("unavailable:"+safeMessage(t),"",0,0,AttentionStore.get(app).pendingCount());}
            synchronized(NotificationBrain.class){running=false;}
            Result delivered=result;
            if(listener!=null)new Handler(Looper.getMainLooper()).post(()->listener.onFinished(delivered));
        });
    }

    private static Result run(Context context)throws Exception{
        AttentionStore attentionStore=AttentionStore.get(context);
        if(attentionStore.pendingCount()==0)return new Result("current","",0,attentionStore.confirmedOpenCount(),0);

        GenerativeModelFutures model=GenerativeModelFutures.from(Generation.INSTANCE.getClient());
        int status=model.checkStatus().get();
        if(status==FeatureStatus.DOWNLOADABLE){
            model.download(new DownloadCallback(){
                @Override public void onDownloadStarted(long bytes){}
                @Override public void onDownloadProgress(long bytes){}
                @Override public void onDownloadCompleted(){}
                @Override public void onDownloadFailed(GenAiException e){}
            }).get();
            status=model.checkStatus().get();
        }
        if(status!=FeatureStatus.AVAILABLE)return new Result(status==FeatureStatus.DOWNLOADING?"model_downloading":"model_unavailable","",0,attentionStore.confirmedOpenCount(),attentionStore.pendingCount());

        String modelName="Gemini Nano";
        try{String x=model.getBaseModelName().get();if(x!=null&&!x.trim().isEmpty())modelName=x.trim();}catch(Throwable ignored){}
        try{model.warmup().get();}catch(Throwable ignored){}

        int totalAnalyzed=0,totalAttention=0;
        NotificationMeaningStore meaningStore=NotificationMeaningStore.get(context);
        try(LifeDb db=new LifeDb(context)){
            for(int batchIndex=0;batchIndex<MAX_BATCHES_PER_RUN;batchIndex++){
                List<AttentionStore.WorkItem> work=attentionStore.pendingWork(MAX_BATCH_ITEMS);
                if(work.isEmpty())break;
                List<TargetBatch> targets=targetsFor(context,attentionStore,work);
                if(targets.isEmpty())continue;
                for(TargetBatch target:targets)attentionStore.markAttempt(target.work.observationId);

                PromptBundle prompt=promptFor(targets);
                GenerateContentResponse response;
                try{response=model.generateContent(prompt.text).get();}
                catch(Throwable t){
                    String error=safeMessage(t);for(TargetBatch target:targets)attentionStore.markFailure(target.work.observationId,error);
                    return new Result("retry:"+error,modelName,totalAnalyzed,totalAttention,attentionStore.pendingCount());
                }
                String output=firstText(response);
                List<NotificationMeaning> meanings=parse(output,prompt,modelName,System.currentTimeMillis());
                Map<String,NotificationMeaning> byObservation=new HashMap<>();
                for(NotificationMeaning m:meanings)byObservation.put(m.sourceObservationId,m);

                for(TargetBatch target:targets){
                    NotificationMeaning meaning=byObservation.get(target.work.observationId);
                    if(meaning==null){attentionStore.markFailure(target.work.observationId,"model returned no grounded object for target");continue;}
                    meaningStore.put(meaning,target.target.observedAt);
                    boolean accepted=applyMeaning(context,db,meaning,target.work.eventId,target.target.observedAt);
                    attentionStore.markAnalyzed(target.work.observationId,target.work.eventId);
                    totalAnalyzed++;if(accepted)totalAttention++;
                }
            }
        }
        return new Result("ready",modelName,totalAnalyzed,totalAttention,attentionStore.pendingCount());
    }

    /** Apply semantic meaning only to the exact evidence event. Never supersede an entire stream. */
    private static boolean applyMeaning(Context context,LifeDb db,NotificationMeaning meaning,long eventId,long sourceAt){
        if(db==null||meaning==null||eventId<=0)return false;
        LifeDb.Event source=db.eventById(eventId);AttentionStore attention=AttentionStore.get(context);
        boolean compatible=meaning.needsAttention()&&attentionCompatible(source,meaning);

        if(meaning.canSummarize()&&source!=null)LocalGroundedMemory.materializeMeaning(context,source,meaning);
        if(compatible)attention.applyModel(meaning,eventId,sourceAt);else attention.rejectProvisional(meaning,eventId,sourceAt);

        if(!meaning.canSummarize())return false;
        SQLiteDatabase sql=db.getWritableDatabase();
        // Deep meaning may reject/replace legacy guesses for THIS evidence only. It can never
        // silently close another request from the same person/thread.
        sql.execSQL("UPDATE open_loops SET status='superseded' WHERE status='open' AND evidence_id=?",new Object[]{eventId});
        AttentionStore.Item current=attention.forEvent(eventId);
        if(current!=null&&AttentionStore.HANDLED.equals(current.status))return false;
        if(!compatible)return false;

        String kind=meaning.loopKind();String fingerprint="brain|"+meaning.sourceObservationId;long now=System.currentTimeMillis();
        sql.execSQL("INSERT OR IGNORE INTO open_loops(evidence_id,fingerprint,kind,title,due_at,confidence,priority,status,created_at) VALUES(?,?,?,?,0,?,?, 'open',?)",
                new Object[]{eventId,fingerprint,kind,meaning.summary,meaning.confidence,meaning.priority(),now});
        sql.execSQL("UPDATE open_loops SET kind=?,title=?,confidence=?,priority=?,status='open' WHERE fingerprint=?",
                new Object[]{kind,meaning.summary,meaning.confidence,meaning.priority(),fingerprint});
        return true;
    }

    /** Structural source compatibility only; free-text keyword lists are deliberately not used. */
    private static boolean attentionCompatible(LifeDb.Event source,NotificationMeaning meaning){
        if(source==null||meaning==null)return false;
        if("PERSON_CONVERSATION".equals(meaning.type))return EventSemantics.isPersonConversation(source);
        if("CONTENT_READY".equals(meaning.type)||"PROMOTION".equals(meaning.type)||"SYSTEM_EVENT".equals(meaning.type)||"OTHER".equals(meaning.type))return false;
        return !EventSemantics.isPersonConversation(source);
    }

    private static List<TargetBatch> targetsFor(Context context,AttentionStore queue,List<AttentionStore.WorkItem> work){
        UniversalObservationStore rawStore=UniversalObservationStore.get(context);ArrayList<TargetBatch> out=new ArrayList<>();
        for(AttentionStore.WorkItem item:work){
            RawObservation target=rawStore.byObservationId(item.observationId);
            if(target==null){queue.markOrphaned(item.observationId);continue;}
            if(!eligible(target)){queue.markAnalyzed(item.observationId,item.eventId);continue;}
            List<RawObservation> contextRaw=rawStore.streamThrough(item.streamId,target.observedAt,Math.max(MAX_MESSAGES_PER_TARGET*3,12));
            ArrayList<RawObservation> contextMessages=new ArrayList<>();
            for(RawObservation o:contextRaw)if(eligible(o))contextMessages.add(o);
            while(contextMessages.size()>MAX_MESSAGES_PER_TARGET)contextMessages.remove(0);
            boolean hasTarget=false;for(RawObservation o:contextMessages)if(item.observationId.equals(o.observationId)){hasTarget=true;break;}
            if(!hasTarget){if(contextMessages.size()>=MAX_MESSAGES_PER_TARGET)contextMessages.remove(0);contextMessages.add(target);}
            out.add(new TargetBatch(item,target,contextMessages));
        }
        return out;
    }

    private static boolean eligible(RawObservation o){
        return o!=null&&o.sourceKind==RawObservation.SourceKind.NOTIFICATION&&!safe(o.streamId).trim().isEmpty()&&!safe(o.text).trim().isEmpty()&&!o.text.startsWith("[sensitive notification")&&!CapturePolicy.isNotificationSummary(o.text);
    }

    private static PromptBundle promptFor(List<TargetBatch> targets)throws Exception{
        JSONArray input=new JSONArray();LinkedHashMap<String,TargetBatch> ids=new LinkedHashMap<>();int n=1;
        for(TargetBatch batch:targets){
            String id="s"+(n++);ids.put(id,batch);JSONObject stream=new JSONObject();stream.put("id",id);stream.put("stream",clip(batch.target.streamId,160));
            stream.put("target_time",batch.target.observedAt);JSONArray messages=new JSONArray();
            for(RawObservation o:batch.messages){
                JSONObject m=new JSONObject();m.put("time",o.observedAt);m.put("target",batch.work.observationId.equals(o.observationId));m.put("app",clip(o.sourcePackage,100));
                putIf(m,"title",clip(o.attributes.get("title"),120));putIf(m,"conversation",clip(o.attributes.get("conversation_title"),120));
                putIf(m,"sender",clip(o.attributes.get("sender"),120));putIf(m,"category",clip(o.attributes.get("category"),80));m.put("text",clip(o.text,MAX_MESSAGE_CHARS));messages.put(m);
            }
            stream.put("messages",messages);input.put(stream);
        }
        String instruction="You are the on-device semantic engine for a personal notification organizer. Analyze ONLY the message marked target=true in each item; earlier messages are context and must not cause the target to inherit an old obligation. Use ONLY supplied evidence. Never invent a missing person, object, date, task, outcome, relationship, urgency, or user obligation. If evidence is ambiguous, choose OTHER or INFORMATION, UNKNOWN, NONE and lower confidence instead of guessing. Return ONLY a JSON array with exactly one object per input id and exact keys id,type,intent,state,urgency,action,summary,reason,confidence. Allowed type: PERSON_CONVERSATION, EMAIL, CALENDAR_EVENT, MISSED_CALL, REMINDER, SECURITY_ALERT, FINANCIAL_ALERT, TRANSACTION, DELIVERY, CONTENT_READY, PROMOTION, SYSTEM_EVENT, OTHER. Allowed intent: REQUEST, QUESTION, COMMITMENT, SCHEDULE, INFORMATION, ALERT, NONE. Allowed state: WAITING_ON_USER, WAITING_ON_OTHER, INFORMATIONAL, RESOLVED, UNKNOWN. Allowed urgency: HIGH, MEDIUM, LOW, NONE. Allowed action: REPLY, DO_TASK, VERIFY, PAY, REVIEW, CALL_BACK, NONE. Use WAITING_ON_USER only when the target evidence itself supports that the user is expected to act or answer. Promotions, content-ready notices, routine system status and ordinary informational updates normally have action NONE. A missed call is MISSED_CALL only if the evidence says it was missed; an email is EMAIL only if the source/evidence supports mail. summary: one short glanceable sentence in the dominant language of the target evidence describing meaning rather than notification UI. reason: one short evidence-based explanation, not hidden reasoning. confidence: 0..1.\nINPUT:\n"+input.toString();
        return new PromptBundle(instruction,ids);
    }

    private static List<NotificationMeaning> parse(String output,PromptBundle prompt,String model,long now){
        ArrayList<NotificationMeaning> out=new ArrayList<>();if(output==null)return out;
        int start=output.indexOf('['),end=output.lastIndexOf(']');if(start<0||end<=start)return out;
        try{
            JSONArray a=new JSONArray(output.substring(start,end+1));
            for(int i=0;i<a.length();i++){
                JSONObject o=a.optJSONObject(i);if(o==null)continue;String id=o.optString("id","");TargetBatch b=prompt.ids.get(id);if(b==null)continue;
                NotificationMeaning m=NotificationMeaning.fromModel(o,b.target.streamId,b.work.observationId,model,now);if(m!=null)out.add(m);
            }
        }catch(Exception ignored){}
        return out;
    }

    private static String firstText(GenerateContentResponse response){
        if(response==null||response.getCandidates()==null||response.getCandidates().isEmpty())return "";
        Candidate c=response.getCandidates().get(0);return c==null||c.getText()==null?"":c.getText();
    }
    private static void putIf(JSONObject o,String key,String value)throws Exception{if(value!=null&&!value.trim().isEmpty())o.put(key,value);}
    private static String safeMessage(Throwable t){String x=t==null?"":t.getMessage();if(x==null||x.trim().isEmpty())x=t==null?"error":t.getClass().getSimpleName();return x.replaceAll("\\s+"," ").trim();}
    private static String safe(String x){return x==null?"":x;}
    private static String clip(String x,int n){x=safe(x).trim();return x.length()<=n?x:x.substring(0,n)+"…";}

    private static final class TargetBatch{
        final AttentionStore.WorkItem work;final RawObservation target;final List<RawObservation> messages;
        TargetBatch(AttentionStore.WorkItem work,RawObservation target,List<RawObservation> messages){this.work=work;this.target=target;this.messages=messages;}
    }
    private static final class PromptBundle{final String text;final Map<String,TargetBatch> ids;PromptBundle(String text,Map<String,TargetBatch> ids){this.text=text;this.ids=ids;}}
}
