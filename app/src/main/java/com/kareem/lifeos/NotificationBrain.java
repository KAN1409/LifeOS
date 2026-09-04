package com.kareem.lifeos;

import android.content.Context;
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
    private static final int MAX_STREAMS=8,MAX_MESSAGES_PER_STREAM=6,SCAN=90;

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
                db.applyBrainMeaning(meaning);
                if(meaning.needsAttention())attention++;
            }
        }
        return new Result("ready",modelName,meanings.size(),attention);
    }

    private static List<StreamBatch> pendingBatches(Context context){
        List<RawObservation> recent=UniversalObservationStore.get(context).recent(SCAN);
        LinkedHashMap<String,List<RawObservation>> grouped=new LinkedHashMap<>();
        for(RawObservation o:recent){
            if(o==null||o.sourceKind!=RawObservation.SourceKind.NOTIFICATION||o.streamId.trim().isEmpty())continue;
            if(o.text.trim().isEmpty()||o.text.startsWith("[sensitive notification"))continue;
            List<RawObservation> xs=grouped.get(o.streamId);if(xs==null){xs=new ArrayList<>();grouped.put(o.streamId,xs);}
            if(xs.size()<MAX_MESSAGES_PER_STREAM)xs.add(o);
        }
        NotificationMeaningStore store=NotificationMeaningStore.get(context);
        ArrayList<StreamBatch> out=new ArrayList<>();
        for(Map.Entry<String,List<RawObservation>> entry:grouped.entrySet()){
            if(out.size()>=MAX_STREAMS)break;
            List<RawObservation> xs=entry.getValue();if(xs.isEmpty())continue;
            RawObservation latest=xs.get(0);
            if(store.isCurrent(entry.getKey(),latest.observationId))continue;
            Collections.sort(xs,new Comparator<RawObservation>(){@Override public int compare(RawObservation a,RawObservation b){return Long.compare(a.observedAt,b.observedAt);}});
            out.add(new StreamBatch(entry.getKey(),latest.observationId,xs));
        }
        return out;
    }

    private static PromptBundle promptFor(List<StreamBatch> batches)throws Exception{
        JSONArray input=new JSONArray();LinkedHashMap<String,StreamBatch> ids=new LinkedHashMap<>();
        int n=1;
        for(StreamBatch batch:batches){
            String id="s"+(n++);ids.put(id,batch);
            JSONObject stream=new JSONObject();stream.put("id",id);stream.put("stream",batch.streamId);
            JSONArray messages=new JSONArray();
            for(RawObservation o:batch.messages){
                JSONObject m=new JSONObject();m.put("time",o.observedAt);m.put("app",o.sourcePackage);
                putIf(m,"title",o.attributes.get("title"));putIf(m,"conversation",o.attributes.get("conversation_title"));
                putIf(m,"sender",o.attributes.get("sender"));putIf(m,"category",o.attributes.get("category"));m.put("text",o.text);messages.put(m);
            }
            stream.put("messages",messages);input.put(stream);
        }
        String instruction="You are the local semantic engine inside a personal notification organizer. Analyze each stream using ONLY the supplied evidence. Do not invent missing people, objects, dates, tasks, or outcomes. Treat the messages in each stream as chronological context, not separate unrelated notifications. Return ONLY a JSON array, one object for every input id, with these exact keys: id,type,intent,state,urgency,action,summary,reason,confidence. Allowed type: PERSON_CONVERSATION, SECURITY_ALERT, FINANCIAL_ALERT, TRANSACTION, DELIVERY, CONTENT_READY, PROMOTION, SYSTEM_EVENT, OTHER. Allowed intent: REQUEST, QUESTION, COMMITMENT, SCHEDULE, INFORMATION, ALERT, NONE. Allowed state: WAITING_ON_USER, WAITING_ON_OTHER, INFORMATIONAL, RESOLVED, UNKNOWN. Allowed urgency: HIGH, MEDIUM, LOW, NONE. Allowed action: REPLY, DO_TASK, VERIFY, PAY, REVIEW, NONE. summary must be one short glanceable sentence in the dominant language of the evidence and describe meaning, not notification UI wording. reason must be one short evidence-based explanation, not hidden reasoning. confidence must be 0 to 1. Use WAITING_ON_USER only when the evidence supports that the user is expected to do or answer something. Promotions, content-ready notices, routine system status, and informational messages should normally have action NONE.\nINPUT:\n"+input.toString();
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

    private static final class StreamBatch{final String streamId,latestObservationId;final List<RawObservation> messages;StreamBatch(String streamId,String latestObservationId,List<RawObservation> messages){this.streamId=streamId;this.latestObservationId=latestObservationId;this.messages=messages;}}
    private static final class PromptBundle{final String text;final Map<String,StreamBatch> ids;PromptBundle(String text,Map<String,StreamBatch> ids){this.text=text;this.ids=ids;}}
}
