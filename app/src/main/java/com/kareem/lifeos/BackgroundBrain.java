package com.kareem.lifeos;

import android.content.Context;
import android.os.PowerManager;
import com.kareem.lifeos.context.RawObservation;
import com.kareem.lifeos.context.UniversalObservationStore;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Primary semantic worker. It is independent from Activity/foreground lifecycle and runs from the
 * NotificationListener process using LiteRT-LM. Gemini Nano remains a foreground fallback/refiner.
 */
final class BackgroundBrain {
    private static final ExecutorService EXECUTOR=Executors.newSingleThreadExecutor();
    private static volatile boolean running;
    private static final int MAX_ITEMS_PER_PASS=24,MAX_CONTEXT_MESSAGES=4,MAX_CHARS=420;
    private static final String MODEL_LABEL="Qwen3-0.6B-int4-background";

    private BackgroundBrain(){}

    static void poke(Context context){
        if(context==null)return;Context app=context.getApplicationContext();
        synchronized(BackgroundBrain.class){if(running)return;running=true;}
        EXECUTOR.execute(()->{
            boolean continueWork=false;
            try{continueWork=run(app);}catch(Throwable ignored){}
            finally{synchronized(BackgroundBrain.class){running=false;}}
            if(continueWork)poke(app);
        });
    }

    static boolean isRunning(){return running;}

    private static boolean run(Context context)throws Exception{
        AttentionStore queue=AttentionStore.get(context);
        if(queue.pendingCount()==0)return false;
        File model=BackgroundModelManager.ensureReadyBlocking(context);
        if(model==null)return false;

        PowerManager.WakeLock wake=null;
        try{
            PowerManager pm=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
            if(pm!=null){wake=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"LifeOS:BackgroundBrain");wake.setReferenceCounted(false);wake.acquire(5*60*1000L);}
            UniversalObservationStore rawStore=UniversalObservationStore.get(context);
            NotificationMeaningStore meaningStore=NotificationMeaningStore.get(context);
            int processed=0;
            try(LifeDb db=new LifeDb(context)){
                while(processed<MAX_ITEMS_PER_PASS){
                    List<AttentionStore.WorkItem> work=queue.pendingWork(1);if(work.isEmpty())break;
                    AttentionStore.WorkItem item=work.get(0);RawObservation target=rawStore.byObservationId(item.observationId);
                    if(target==null){queue.markOrphaned(item.observationId);continue;}
                    if(!eligible(target)){queue.markAnalyzed(item.observationId,item.eventId);continue;}
                    queue.markAttempt(item.observationId);
                    try{
                        String output=BackgroundSemanticRuntime.generate(context,model.getAbsolutePath(),prompt(rawStore,item,target));
                        NotificationMeaning meaning=parse(output,target,item,System.currentTimeMillis());
                        if(meaning==null){queue.markFailure(item.observationId,"background model returned no grounded JSON object");break;}
                        meaningStore.put(meaning,target.observedAt);
                        NotificationMeaningApplier.apply(context,db,meaning,item.eventId,target.observedAt);
                        queue.markAnalyzed(item.observationId,item.eventId);processed++;
                    }catch(Throwable t){queue.markFailure(item.observationId,"background:"+safeMessage(t));BackgroundSemanticRuntime.reset();break;}
                }
            }
            return processed>0&&queue.pendingCount()>0;
        }finally{if(wake!=null&&wake.isHeld())try{wake.release();}catch(Throwable ignored){}}
    }

    private static String prompt(UniversalObservationStore store,AttentionStore.WorkItem item,RawObservation target)throws Exception{
        JSONObject input=new JSONObject();input.put("stream",clip(target.streamId,160));input.put("target",observationJson(target));
        JSONArray previous=new JSONArray();List<RawObservation> raw=store.streamThrough(item.streamId,target.observedAt,12);ArrayList<RawObservation> eligible=new ArrayList<>();
        for(RawObservation o:raw)if(eligible(o)&&o.observedAt<=target.observedAt&&!item.observationId.equals(o.observationId))eligible.add(o);
        int start=Math.max(0,eligible.size()-MAX_CONTEXT_MESSAGES);for(int i=start;i<eligible.size();i++)previous.put(observationJson(eligible.get(i)));input.put("previous_context",previous);
        return "Analyze ONLY target. previous_context is context and must never create an obligation that target does not contain. " +
                "Use only supplied evidence. Return ONLY one flat JSON object with exact keys type,intent,state,urgency,action,summary,reason,confidence. " +
                "Allowed type: PERSON_CONVERSATION, EMAIL, CALENDAR_EVENT, MISSED_CALL, REMINDER, SECURITY_ALERT, FINANCIAL_ALERT, TRANSACTION, DELIVERY, CONTENT_READY, PROMOTION, SYSTEM_EVENT, OTHER. " +
                "Allowed intent: REQUEST, QUESTION, COMMITMENT, SCHEDULE, INFORMATION, ALERT, NONE. Allowed state: WAITING_ON_USER, WAITING_ON_OTHER, INFORMATIONAL, RESOLVED, UNKNOWN. " +
                "Allowed urgency: HIGH, MEDIUM, LOW, NONE. Allowed action: REPLY, DO_TASK, VERIFY, PAY, REVIEW, CALL_BACK, NONE. " +
                "WAITING_ON_USER requires evidence in target that the user is expected to act or answer. Promotions/content-ready/routine system info normally action NONE. " +
                "summary must be one short sentence in the dominant language of target. reason must be one short evidence-based explanation. confidence is 0..1.\nINPUT:\n"+input.toString();
    }

    private static JSONObject observationJson(RawObservation o)throws Exception{
        JSONObject j=new JSONObject();j.put("time",o.observedAt);j.put("app",clip(o.sourcePackage,100));
        putIf(j,"title",clip(o.attributes.get("title"),120));putIf(j,"conversation",clip(o.attributes.get("conversation_title"),120));putIf(j,"sender",clip(o.attributes.get("sender"),120));putIf(j,"category",clip(o.attributes.get("category"),80));j.put("text",clip(o.text,MAX_CHARS));return j;
    }

    private static NotificationMeaning parse(String output,RawObservation target,AttentionStore.WorkItem item,long now){
        if(output==null)return null;int start=output.indexOf('{'),end=output.lastIndexOf('}');if(start<0||end<=start)return null;
        try{return NotificationMeaning.fromModel(new JSONObject(output.substring(start,end+1)),target.streamId,item.observationId,MODEL_LABEL,now);}catch(Throwable ignored){return null;}
    }
    private static boolean eligible(RawObservation o){return o!=null&&o.sourceKind==RawObservation.SourceKind.NOTIFICATION&&!safe(o.streamId).trim().isEmpty()&&!safe(o.text).trim().isEmpty()&&!o.text.startsWith("[sensitive notification")&&!CapturePolicy.isNotificationSummary(o.text);}
    private static void putIf(JSONObject o,String k,String v)throws Exception{if(v!=null&&!v.trim().isEmpty())o.put(k,v);}
    private static String safe(String x){return x==null?"":x;}
    private static String clip(String x,int n){x=safe(x).trim();return x.length()<=n?x:x.substring(0,n)+"…";}
    private static String safeMessage(Throwable t){String x=t==null?"":t.getMessage();if(x==null||x.trim().isEmpty())x=t==null?"error":t.getClass().getSimpleName();return x.replaceAll("\\s+"," ").trim();}
}
