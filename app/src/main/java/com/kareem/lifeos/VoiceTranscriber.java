package com.kareem.lifeos;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * LifeOS wrapper around the proven Cortex cloud ASR backend.
 * Provider credentials remain server-side; the Android APK contains no ASR provider key.
 */
final class VoiceTranscriber {
    interface Callback {void ok(VoiceTranscript result);void fail(Exception error);}
    static final class RetryableException extends Exception {RetryableException(String message){super(message);}RetryableException(String message,Throwable cause){super(message,cause);}}
    private static final String ENDPOINT="https://kareemabdelaziz.com/ai/transcribe.php";private static final int MAX_REDIRECTS=3;private static final ExecutorService EXEC=Executors.newCachedThreadPool();
    private VoiceTranscriber(){}
    static void transcribe(File audio,Callback cb){EXEC.execute(()->{try{cb.ok(transcribeBlocking(audio));}catch(Exception e){cb.fail(e);}});}
    static VoiceTranscript transcribeBlocking(File audio)throws Exception{if(audio==null||!audio.exists()||!audio.isFile())throw new IllegalArgumentException("Audio file is missing");if(audio.length()<=44)throw new IllegalArgumentException("Audio file is empty");if(audio.length()>25L*1024L*1024L)throw new IllegalArgumentException("Audio exceeds the 25 MB transcription limit");return post(new URL(ENDPOINT),audio,0);}
    private static VoiceTranscript post(URL endpoint,File audio,int redirects)throws Exception{HttpURLConnection c=null;String boundary="----LifeOSVoice"+UUID.randomUUID().toString().replace("-","");try{c=(HttpURLConnection)endpoint.openConnection();c.setInstanceFollowRedirects(false);c.setRequestMethod("POST");c.setConnectTimeout(20_000);c.setReadTimeout(180_000);c.setDoOutput(true);c.setUseCaches(false);c.setRequestProperty("Accept","application/json");c.setRequestProperty("Content-Type","multipart/form-data; boundary="+boundary);c.setRequestProperty("X-LifeOS-Client","android-voice-memory-v1");c.setRequestProperty("X-Cortex-Client","android-cloud-voice-v2");try(OutputStream out=c.getOutputStream()){writeText(out,boundary,"mode","ar-EG+en-codeswitch-auto");writeText(out,boundary,"languages","ar,en");writeFile(out,boundary,"audio",audio,"audio/wav");out.write(("--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));out.flush();}
            int code=c.getResponseCode();if(code==301||code==302||code==307||code==308){if(redirects>=MAX_REDIRECTS)throw new IllegalStateException("Transcription backend redirect loop");String loc=c.getHeaderField("Location");if(loc==null||loc.trim().isEmpty())throw new IllegalStateException("Transcription redirect missing Location");URL next=new URL(endpoint,loc.trim());validateRedirect(endpoint,next);c.disconnect();c=null;return post(next,audio,redirects+1);}String body=read(code>=200&&code<300?c.getInputStream():c.getErrorStream());if(code<200||code>=300){String msg="Transcription backend HTTP "+code+(body.isEmpty()?"":": "+body);if(code==408||code==425||code==429||code>=500)throw new RetryableException(msg);throw new IllegalStateException(msg);}JSONObject json=new JSONObject(body);if(!json.optBoolean("ok",true)){String msg=json.optString("error","Transcription failed");if(json.optBoolean("retryable",false))throw new RetryableException(msg);throw new IllegalStateException(msg);}String text=json.optString("transcript",json.optString("text","")).trim();if(text.isEmpty())throw new IllegalStateException("Transcription returned no text");ArrayList<VoiceTranscript.Segment> segments=new ArrayList<>();JSONArray a=json.optJSONArray("segments");if(a!=null)for(int i=0;i<a.length();i++){JSONObject s=a.optJSONObject(i);if(s==null)continue;String t=s.optString("text","").trim();if(t.isEmpty())continue;long start=s.optLong("start_ms",0),end=s.optLong("end_ms",start);segments.add(new VoiceTranscript.Segment(start,end,t,(float)s.optDouble("confidence",0)));}return new VoiceTranscript(text,json.optString("language","ar-EG+en-codeswitch-auto"),json.optString("engine","gpt-transcribe_cloud"),json.optString("version","cloud-v2"),json.optLong("duration_ms",0),segments);
        }catch(RetryableException e){throw e;}catch(java.net.SocketTimeoutException|java.net.ConnectException|java.net.UnknownHostException e){throw new RetryableException("Transcription unavailable: "+e.getMessage(),e);}finally{if(c!=null)c.disconnect();}}
    private static void validateRedirect(URL from,URL to){if(!"https".equalsIgnoreCase(to.getProtocol()))throw new SecurityException("Refusing non-HTTPS transcription redirect");if(!host(from).equals(host(to)))throw new SecurityException("Refusing transcription redirect to another host");}
    private static String host(URL u){String h=u.getHost()==null?"":u.getHost().toLowerCase(Locale.US);return h.startsWith("www.")?h.substring(4):h;}
    private static void writeText(OutputStream out,String boundary,String name,String value)throws Exception{out.write(("--"+boundary+"\r\n").getBytes(StandardCharsets.UTF_8));out.write(("Content-Disposition: form-data; name=\""+name+"\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));out.write(value.getBytes(StandardCharsets.UTF_8));out.write("\r\n".getBytes(StandardCharsets.UTF_8));}
    private static void writeFile(OutputStream out,String boundary,String name,File file,String mime)throws Exception{out.write(("--"+boundary+"\r\n").getBytes(StandardCharsets.UTF_8));out.write(("Content-Disposition: form-data; name=\""+name+"\"; filename=\""+safe(file.getName())+"\"\r\n").getBytes(StandardCharsets.UTF_8));out.write(("Content-Type: "+mime+"\r\n\r\n").getBytes(StandardCharsets.UTF_8));try(FileInputStream in=new FileInputStream(file)){byte[] b=new byte[64*1024];int n;while((n=in.read(b))>=0)if(n>0)out.write(b,0,n);}out.write("\r\n".getBytes(StandardCharsets.UTF_8));}
    private static String read(InputStream in)throws Exception{if(in==null)return "";try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){StringBuilder s=new StringBuilder();String line;while((line=r.readLine())!=null)s.append(line);return s.toString();}}
    private static String safe(String x){return x==null?"voice.wav":x.replace("\"","_").replace("\r","_").replace("\n","_");}
}
