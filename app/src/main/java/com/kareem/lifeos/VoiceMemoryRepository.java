package com.kareem.lifeos;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** First-party voice memories: real WAV source, durable transcript state and manual retry. */
final class VoiceMemoryRepository {
    static final class VoiceObject {
        final String id,filePath,status,transcript,language,engine,engineVersion,error;final long createdAt,durationMs,sizeBytes;
        VoiceObject(String id,String path,long created,long duration,long size,String status,String transcript,String language,String engine,String version,String error){this.id=s(id);this.filePath=s(path);this.createdAt=created;this.durationMs=duration;this.sizeBytes=size;this.status=s(status);this.transcript=s(transcript);this.language=s(language);this.engine=s(engine);this.engineVersion=s(version);this.error=s(error);}
        List<VoiceTranscript.Segment> segments(Context c){return VoiceMemoryStore.get(c).segments(id);}
        boolean hasTranscript(){return !transcript.isEmpty();}
    }
    interface Callback {void ok(VoiceObject voice);void fail(VoiceObject voice,Exception error);}
    private VoiceMemoryRepository(){}

    static VoiceObject register(Context c,File file,long durationMs){if(file==null||!file.exists())return null;String id="voice:"+sha(file.getAbsolutePath()+":"+file.lastModified()).substring(0,24);VoiceMemoryStore.get(c).add(id,file.getAbsolutePath(),System.currentTimeMillis(),durationMs,file.length());return load(c,id);}
    static VoiceObject load(Context c,String id){String[] x=VoiceMemoryStore.get(c).load(id);return x==null?null:read(x);}
    static List<VoiceObject> list(Context c,int limit){ArrayList<VoiceObject> out=new ArrayList<>();for(String[] x:VoiceMemoryStore.get(c).list(limit))out.add(read(x));return out;}
    static int count(Context c){return VoiceMemoryStore.get(c).count();}
    static void transcribe(Context c,String id,Callback callback){Context app=c.getApplicationContext();VoiceObject v=load(app,id);if(v==null){if(callback!=null)callback.fail(null,new IllegalArgumentException("Voice memory not found"));return;}File f=new File(v.filePath);if(!f.exists()){VoiceMemoryStore.get(app).setStatus(id,"missing_audio","Audio file is missing");if(callback!=null)callback.fail(load(app,id),new IllegalStateException("Audio file is missing"));return;}VoiceMemoryStore.get(app).setStatus(id,"transcribing","");VoiceTranscriber.transcribe(f,new VoiceTranscriber.Callback(){public void ok(VoiceTranscript result){VoiceMemoryStore.get(app).saveTranscript(id,result);post(()->{if(callback!=null)callback.ok(load(app,id));});}public void fail(Exception error){VoiceMemoryStore.get(app).setStatus(id,"transcription_failed",error.getMessage()==null?error.getClass().getSimpleName():error.getMessage());post(()->{if(callback!=null)callback.fail(load(app,id),error);});}});}
    static String searchableText(VoiceObject v){if(v==null)return "";return (v.transcript+" "+v.language+" "+v.engine).trim();}
    static void eraseFilesAndStore(Context c){for(VoiceObject v:list(c,10000))try{new File(v.filePath).delete();}catch(Exception ignored){}VoiceMemoryStore.get(c).eraseAll();}
    private static VoiceObject read(String[] x){return new VoiceObject(x[0],x[1],lv(x[2]),lv(x[3]),lv(x[4]),x[5],x[6],x[7],x[8],x[9],x[10]);}
    private static long lv(String x){try{return Long.parseLong(x);}catch(Exception e){return 0;}}
    private static void post(Runnable r){new Handler(Looper.getMainLooper()).post(r);}
    private static String sha(String value){try{byte[] b=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte x:b)s.append(String.format(Locale.US,"%02x",x));return s.toString();}catch(Exception e){return Integer.toHexString(value.hashCode())+"000000000000000000000000";}}
    private static String s(String x){return x==null?"":x.trim();}
}
