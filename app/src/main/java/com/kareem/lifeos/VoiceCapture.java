package com.kareem.lifeos;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/** Proven Cortex WAV recorder migrated into LifeOS as a first-party capture source. */
final class VoiceCapture {
    static final int RATE=16000;private AudioRecord record;private Thread thread;private volatile boolean running;private RandomAccessFile out;private File file;private long pcmBytes,startedAt;
    boolean hasPermission(Context c){return c.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;}
    synchronized File start(Context ctx)throws Exception{if(running)throw new IllegalStateException("Already recording");File dir=new File(ctx.getFilesDir(),"voice_memories");if(!dir.exists()&&!dir.mkdirs())throw new IOException("Cannot create voice memory directory");file=new File(dir,"voice_"+System.currentTimeMillis()+".wav");int min=AudioRecord.getMinBufferSize(RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT),buffer=Math.max(min,8192);record=new AudioRecord(MediaRecorder.AudioSource.MIC,RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,buffer);if(record.getState()!=AudioRecord.STATE_INITIALIZED)throw new IOException("Microphone initialization failed");out=new RandomAccessFile(file,"rw");writeHeader(out,0);pcmBytes=0;running=true;startedAt=System.currentTimeMillis();record.startRecording();thread=new Thread(()->{byte[] b=new byte[buffer];try{while(running){int n=record.read(b,0,b.length);if(n>0){out.write(b,0,n);pcmBytes+=n;}}}catch(Exception ignored){}},"LifeOSVoiceRecorder");thread.start();return file;}
    synchronized File stop()throws Exception{if(!running)return file;running=false;try{record.stop();}catch(Exception ignored){}if(thread!=null)try{thread.join(1500);}catch(InterruptedException ignored){Thread.currentThread().interrupt();}try{record.release();}catch(Exception ignored){}record=null;thread=null;if(out!=null){out.seek(0);writeHeader(out,pcmBytes);out.close();out=null;}return file;}
    boolean isRunning(){return running;}long elapsedMs(){return running?Math.max(0,System.currentTimeMillis()-startedAt):0;}long audioDurationMs(){return pcmBytes<=0?0:(pcmBytes*1000L)/(RATE*2L);}
    void abandon(){running=false;try{if(record!=null)record.stop();}catch(Exception ignored){}try{if(record!=null)record.release();}catch(Exception ignored){}record=null;try{if(out!=null)out.close();}catch(Exception ignored){}out=null;}
    private static void writeHeader(RandomAccessFile f,long data)throws IOException{int channels=1,bits=16;long byteRate=(long)RATE*channels*bits/8;f.writeBytes("RIFF");le32(f,36+data);f.writeBytes("WAVEfmt ");le32(f,16);le16(f,1);le16(f,channels);le32(f,RATE);le32(f,byteRate);le16(f,channels*bits/8);le16(f,bits);f.writeBytes("data");le32(f,data);}
    private static void le16(RandomAccessFile f,long v)throws IOException{f.write((int)(v&255));f.write((int)((v>>8)&255));}private static void le32(RandomAccessFile f,long v)throws IOException{f.write((int)(v&255));f.write((int)((v>>8)&255));f.write((int)((v>>16)&255));f.write((int)((v>>24)&255));}
}
