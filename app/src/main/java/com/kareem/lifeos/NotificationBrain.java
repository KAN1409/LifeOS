package com.kareem.lifeos;

import android.content.Context;
import android.database.Cursor;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Zero-config, on-device language brain for captured notification streams.
 *
 * Android AICore only permits GenAI inference while LifeOS is foreground. Notification capture
 * remains continuous in the background; this class batches pending streams when Home resumes.
 */
final class NotificationBrain {
    interface Listener { void onFinished(Result result); }
    static final class Result {
        final String status,model;final int analyzed,attention;
        Result(String status,String model,int analyzed,int attention){this.status=status;this.model=model;this.analyzed=analyzed;this.attention=attention;}
    }

    private static final ExecutorService EXECUTOR=Executors.newSingleThreadExecutor();
    private static volatile boolean running;
    private static final int MAX_STREAMS=6,MAX_MESSAGES_PER_STREAM=5,SCAN=120,MAX_MESSAGE_CHARS=420;

    private NotificationBrain(){}

    static void analyzeForeground(Context context,Listener listener){
        if(context==null)return;
        synchronized(NotificationBrain.class){if(running)return;running=true;}
        Context app=context.getApplicationContext();
        EXECUTOR.execute(()->{
            Result result;
            try{result=run(app);}catch(Throwable t){result=new Result("unavailable:"+safeMessage(t),"",0,0);}
            synchronized(NotificationBrain.class){running=false;}
            Result delivered=result;
            if(listener!=null)new Handler(Looper.getMainLooper()).post(()->listener.onFinished(delivered));
        });
    }

    private static Result run(Context context)throws Exception{
        List<StreamBatch> batches=pendingBatches(context);
        if(batches.isEmpty())return new Result("current","",0,0);

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
        if(status!=FeatureStatus.AVAILABLE)return new Result(status==FeatureStatus.DOWNLOADING?"model_downloading":"model_unavailable","",0,0);

        String modelName="Gemini Nano";
        try{String x=model.getBaseModelName().get();if(x!=null&&!x.trim().isEmpty())modelName=x.trim();}catch(Throwable ignored){}
        try{model.warmup().get();}catch(Throwable ignored){}

        PromptBundle prompt=promptFor(batches);
        GenerateContentResponse response=model.generateContent(prompt.text).get();
        String output=firstText(response);
        List<NotificationMeaning> meanings=parse(output,prompt,modelName,System.currentTimeMillis());
        if(meanings.isEmpty())return new Result("no_grounded_output",modelName,0,0);

        NotificationMeaningStore store=NotificationMeaningStore.get(context);
        int attention=0;
        try(LifeDb db=new LifeDb(context)){
            for(NotificationMeaning meaning:meanings){
                store.put(meaning);
                applyMeaning(context,db,meaning);
                if(meaning.needsAttention())attention++;
            }
        }
        return new Result("ready",modelName,meanings.size(),attention);
    }

    /**
     * Replace heuristic thread loops with one current model-grounded state. This is deliberate
     * semantic compression: many notifications in one stream produce at most one active item.
     */
    private static void applyMeaning(Context context,LifeDb db,NotificationMeaning meaning){
        if(db==null||meaning==null||!meaning.canSummarize())return;
        SQLiteDatabase sql=db.getWritableDatabase();long eventId=0;
        try(Cursor c=sql.rawQuery("SELECT id FROM events WHERE thread_key=? ORDER BY captured_at DESC LIMIT 1",new String[]{meaning.streamId})){
            if(c.moveToFirst())eventId=c.getLong(0);
        }
        if(eventId<=0)return;

        LifeDb.Event source=db.eventById(eventId);
        // A small subset of strongly grounded conversational states becomes compressed episodic
        // memory. Raw message memory remains authoritative and is retained alongside this state.
        LocalGroundedMemory.materializeMeaning(context,source,meaning);

        // Once the local brain has understood this current stream, its single grounded state
        // supersedes per-message keyword loops whether that state needs attention or not.
        sql.execSQL("UPDATE open_loops SET status='superseded' WHERE status='open' AND evidence_id IN (SELECT id FROM events WHERE thread_key=?)",new Object[]{meaning.streamId});
        if(!meaning.needsAttention())return;

        if(!attentionCompatible(source,meaning))return;
        String kind=meaning.loopKind();
        String fingerprint="brain|"+meaning.sourceObservationId;
        long now=System.currentTimeMillis();
        sql.execSQL("INSERT OR IGNORE INTO open_loops(evidence_id,fingerprint,kind,title,due_at,confidence,priority,status,created_at) VALUES(?,?,?,?,0,?,?, 'open',?)",
                new Object[]{eventId,fingerprint,kind,meaning.summary,meaning.confidence,meaning.priority(),now});
        sql.execSQL("UPDATE open_loops SET kind=?,title=?,confidence=?,priority=?,status='open' WHERE fingerprint=?",
                new Object[]{kind,meaning.summary,meaning.confidence,meaning.priority(),fingerprint});
    }

    /** Structural source compatibility only; free-text keyword lists are deliberately not used. */
    private static boolean attentionCompatible(LifeDb.Event source,NotificationMeaning meaning){
        if(source==null||meaning==null)return false;
        if("PERSON_CONVERSATION".equals(meaning.type))return EventSemantics.isPersonConversation(source);
        if("CONTENT_READY".equals(meaning.type)||"PROMOTION".equals(meaning.type)||"SYSTEM_EVENT".equals(meaning.type)||"OTHER".equals(meaning.type))return false;
        // High-confidence model semantics can recognize novel banks, couriers, mail/calendar apps,
        // reminders and calls that our older English/Arabic keyword dictionaries have never seen.
        return !EventSemantics.isPersonConversation(source);
    }

    private static List<StreamBatch> pendingBatches(Context context){
        List<RawObservation> recent=UniversalObservationStore.get(context).recent(SCAN);
        LinkedHashMap<String,List<RawObservation>> grouped=new LinkedHashMap<>();
        for(RawObservation o:recent){
            if(o==null||o.sourceKind!=RawObservation.SourceKind.NOTIFICATION||o.streamId.trim().isEmpty())continue;
            if(o.text.trim().isEmpty()||o.text.startsWith("[sensitive notification")||CapturePolicy.isNotificationSummary(o.text))continue;
            List<RawObservation> xs=grouped.get(o.streamId);if(xs==null){xs=new ArrayList<>();grouped.put(o.streamId,xs);}
            if(xs.size()<MAX_MESSAGES_PER_STREAM)xs.add(o);
        }
        NotificationMeaningStore store=NotificationMeaningStore.get(context);
        ArrayList<StreamBatch> candidates=new ArrayList<>();
        for(Map.Entry<String,List<RawObservation>> entry:grouped.entrySet()){
            List<RawObservation> xs=entry.getValue();if(xs.isEmpty())continue;
            RawObservation latest=latest(xs);
            if(latest==null||store.isCurrent(entry.getKey(),latest.observationId))continue;
            Collections.sort(xs,new Comparator<RawObservation>(){@Override public int compare(RawObservation a,RawObservation b){return Long.compare(a.observedAt,b.observedAt);}});
            candidates.add(new StreamBatch(entry.getKey(),latest.observationId,xs,streamScore(latest),latest.observedAt));
        }
        Collections.sort(candidates,new Comparator<StreamBatch>(){@Override public int compare(StreamBatch a,StreamBatch b){int p=Integer.compare(b.priority,a.priority);return p!=0?p:Long.compare(b.latestAt,a.latestAt);}});
        if(candidates.size()<=MAX_STREAMS)return candidates;
        return new ArrayList<StreamBatch>(candidates.subList(0,MAX_STREAMS));
    }

    /** Prioritize structurally rich/high-signal sources; this does not interpret message meaning. */
    private static int streamScore(RawObservation o){
        int score=0;String app=safe(o.sourcePackage).toLowerCase(Locale.ROOT),category=safe(o.attributes.get("category")).toLowerCase(Locale.ROOT);
        if("true".equals(o.attributes.get("structured_message")))score+=100;
        if(app.contains("whatsapp")||app.contains("telegram")||app.contains("messenger")||app.contains("signal")||app.contains("messaging"))score+=60;
        if(category.contains("msg")||category.contains("email")||category.contains("call")||category.contains("event")||category.contains("reminder")||category.contains("alarm"))score+=45;
        if("true".equals(o.attributes.get("ongoing")))score-=25;
        if(app.equals("android")||app.contains("systemui"))score-=30;
        return score;
    }

    private static RawObservation latest(List<RawObservation> xs){RawObservation best=null;for(RawObservation o:xs)if(o!=null&&(best==null||o.observedAt>best.observedAt))best=o;return best;}

    private static PromptBundle promptFor(List<StreamBatch> batches)throws Exception{
        JSONArray input=new JSONArray();LinkedHashMap<String,StreamBatch> ids=new LinkedHashMap<>();
        int n=1;
        for(StreamBatch batch:batches){
            String id="s"+(n++);ids.put(id,batch);
            JSONObject stream=new JSONObject();stream.put("id",id);stream.put("stream",clip(batch.streamId,160));
            JSONArray messages=new JSONArray();
            for(RawObservation o:batch.messages){
                JSONObject m=new JSONObject();m.put("time",o.observedAt);m.put("app",clip(o.sourcePackage,100));
                putIf(m,"title",clip(o.attributes.get("title"),120));putIf(m,"conversation",clip(o.attributes.get("conversation_title"),120));
                putIf(m,"sender",clip(o.attributes.get("sender"),120));putIf(m,"category",clip(o.attributes.get("category"),80));m.put("text",clip(o.text,MAX_MESSAGE_CHARS));messages.put(m);
            }
            stream.put("messages",messages);input.put(stream);
        }
        String instruction="You are the on-device semantic engine for a personal notification organizer. Analyze each stream using ONLY supplied evidence. Treat messages in a stream as chronological context. Never invent a missing person, object, date, task, outcome, relationship, urgency, or user obligation. If evidence is ambiguous, choose OTHER or INFORMATION, UNKNOWN, NONE and lower confidence instead of guessing. Return ONLY a JSON array with exactly one object per input id and exact keys id,type,intent,state,urgency,action,summary,reason,confidence. Allowed type: PERSON_CONVERSATION, EMAIL, CALENDAR_EVENT, MISSED_CALL, REMINDER, SECURITY_ALERT, FINANCIAL_ALERT, TRANSACTION, DELIVERY, CONTENT_READY, PROMOTION, SYSTEM_EVENT, OTHER. Allowed intent: REQUEST, QUESTION, COMMITMENT, SCHEDULE, INFORMATION, ALERT, NONE. Allowed state: WAITING_ON_USER, WAITING_ON_OTHER, INFORMATIONAL, RESOLVED, UNKNOWN. Allowed urgency: HIGH, MEDIUM, LOW, NONE. Allowed action: REPLY, DO_TASK, VERIFY, PAY, REVIEW, CALL_BACK, NONE. Use WAITING_ON_USER only when the evidence itself supports that the user is expected to act or answer. Do not infer an obligation merely because a notification exists. Promotions, content-ready notices, routine system status and ordinary informational updates normally have action NONE. A missed call is MISSED_CALL only if the evidence says it was missed; an email is EMAIL only if the source/evidence supports mail. summary: one short glanceable sentence in the dominant language of the evidence describing meaning rather than notification UI. reason: one short evidence-based explanation, not hidden reasoning. confidence: 0..1.\nINPUT:\n"+input.toString();
        return new PromptBundle(instruction,ids);
    }

    private static List<NotificationMeaning> parse(String output,PromptBundle prompt,String model,long now){
        ArrayList<NotificationMeaning> out=new ArrayList<>();if(output==null)return out;
        int start=output.indexOf('['),end=output.lastIndexOf(']');if(start<0||end<=start)return out;
        try{
            JSONArray a=new JSONArray(output.substring(start,end+1));
            for(int i=0;i<a.length();i++){
                JSONObject o=a.optJSONObject(i);if(o==null)continue;String id=o.optString("id","");StreamBatch b=prompt.ids.get(id);if(b==null)continue;
                NotificationMeaning m=NotificationMeaning.fromModel(o,b.streamId,b.latestObservationId,model,now);if(m!=null)out.add(m);
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

    private static final class StreamBatch{final String streamId,latestObservationId;final List<RawObservation> messages;final int priority;final long latestAt;StreamBatch(String streamId,String latestObservationId,List<RawObservation> messages,int priority,long latestAt){this.streamId=streamId;this.latestObservationId=latestObservationId;this.messages=messages;this.priority=priority;this.latestAt=latestAt;}}
    private static final class PromptBundle{final String text;final Map<String,StreamBatch> ids;PromptBundle(String text,Map<String,StreamBatch> ids){this.text=text;this.ids=ids;}}
}
