package com.kareem.lifeos;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Downloads and verifies the swappable on-device background model outside the APK. */
final class BackgroundModelManager {
    static final String MODEL_NAME="Qwen3 0.6B INT4 no-think";
    static final String FILE_NAME="qwen3_0.6b_nothink_q4_block32_ekv1280.litertlm";
    static final long EXPECTED_SIZE=347251840L;
    static final String EXPECTED_SHA256="2df6821ec12702dafd33915e7a1a1adc7c4b053f3672fd9555dfaf3a114c4139";
    private static final String URL="https://huggingface.co/litert-community/Qwen3-0.6B-int4/resolve/main/"+FILE_NAME+"?download=true";
    private static final String PREFS="lifeos_background_brain_model",KEY_ID="download_id",KEY_VERIFIED="verified";
    private static final ExecutorService EXECUTOR=Executors.newSingleThreadExecutor();

    static final class Status {
        final String state;final int progress;
        Status(String state,int progress){this.state=state==null?"unknown":state;this.progress=Math.max(0,Math.min(100,progress));}
        boolean ready(){return "ready".equals(state);}
    }

    private BackgroundModelManager(){}

    static File modelFile(Context context){
        File base=context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if(base==null)base=context.getFilesDir();
        return new File(new File(base,"lifeos-models"),FILE_NAME);
    }

    static boolean isReadyFast(Context context){
        File f=modelFile(context);return f.isFile()&&f.length()==EXPECTED_SIZE&&prefs(context).getBoolean(KEY_VERIFIED,false);
    }

    /** Start model provisioning without blocking Application/UI startup. */
    static void provisionAsync(Context context){
        if(context==null||isReadyFast(context))return;Context app=context.getApplicationContext();
        EXECUTOR.execute(()->{try{ensureReadyBlocking(app);}catch(Throwable ignored){}});
    }

    /** Returns verified model file, or null while a durable DownloadManager job is pending. */
    static File ensureReadyBlocking(Context context)throws Exception{
        Context app=context.getApplicationContext();File f=modelFile(app);SharedPreferences p=prefs(app);
        if(isReadyFast(app))return f;

        if(f.isFile()&&f.length()==EXPECTED_SIZE){
            if(EXPECTED_SHA256.equalsIgnoreCase(sha256(f))){p.edit().putBoolean(KEY_VERIFIED,true).apply();return f;}
            f.delete();p.edit().putBoolean(KEY_VERIFIED,false).apply();
        }else if(f.exists()){f.delete();p.edit().putBoolean(KEY_VERIFIED,false).apply();}

        long existing=p.getLong(KEY_ID,0L);DownloadManager dm=(DownloadManager)app.getSystemService(Context.DOWNLOAD_SERVICE);
        if(dm==null)return null;
        if(existing>0){
            int state=downloadState(dm,existing);
            if(state==DownloadManager.STATUS_SUCCESSFUL){
                p.edit().putLong(KEY_ID,0L).apply();
                if(f.isFile()&&f.length()==EXPECTED_SIZE&&EXPECTED_SHA256.equalsIgnoreCase(sha256(f))){p.edit().putBoolean(KEY_VERIFIED,true).apply();return f;}
                if(f.exists())f.delete();
            }else if(state==DownloadManager.STATUS_PENDING||state==DownloadManager.STATUS_RUNNING||state==DownloadManager.STATUS_PAUSED){return null;}
            else p.edit().putLong(KEY_ID,0L).apply();
        }

        File parent=f.getParentFile();if(parent!=null)parent.mkdirs();if(f.exists())f.delete();
        DownloadManager.Request r=new DownloadManager.Request(Uri.parse(URL));
        r.setTitle("LifeOS background brain");r.setDescription("Preparing private on-device intelligence");
        r.setAllowedOverMetered(true);r.setAllowedOverRoaming(false);
        r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        r.setDestinationInExternalFilesDir(app,Environment.DIRECTORY_DOWNLOADS,"lifeos-models/"+FILE_NAME);
        long id=dm.enqueue(r);p.edit().putLong(KEY_ID,id).putBoolean(KEY_VERIFIED,false).apply();return null;
    }

    static boolean isOurDownload(Context context,long id){return id>0&&prefs(context).getLong(KEY_ID,0L)==id;}

    static Status status(Context context){
        if(context==null)return new Status("unknown",0);if(isReadyFast(context))return new Status("ready",100);
        long id=prefs(context).getLong(KEY_ID,0L);if(id<=0)return new Status("not_downloaded",0);
        DownloadManager dm=(DownloadManager)context.getSystemService(Context.DOWNLOAD_SERVICE);if(dm==null)return new Status("unavailable",0);
        try(Cursor c=dm.query(new DownloadManager.Query().setFilterById(id))){
            if(c==null||!c.moveToFirst())return new Status("not_downloaded",0);
            int s=c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));long soFar=c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));long total=c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
            int progress=total>0?(int)Math.min(100,(soFar*100L)/total):0;
            if(s==DownloadManager.STATUS_SUCCESSFUL)return new Status("verifying",100);
            if(s==DownloadManager.STATUS_RUNNING)return new Status("downloading",progress);
            if(s==DownloadManager.STATUS_PENDING)return new Status("queued_download",progress);
            if(s==DownloadManager.STATUS_PAUSED)return new Status("download_paused",progress);
            if(s==DownloadManager.STATUS_FAILED)return new Status("download_failed",progress);
        }catch(Throwable ignored){}
        return new Status("unknown",0);
    }

    private static int downloadState(DownloadManager dm,long id){
        try(Cursor c=dm.query(new DownloadManager.Query().setFilterById(id))){return c!=null&&c.moveToFirst()?c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)):DownloadManager.STATUS_FAILED;}
    }
    private static SharedPreferences prefs(Context c){return c.getApplicationContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
    private static String sha256(File f)throws Exception{
        MessageDigest d=MessageDigest.getInstance("SHA-256");byte[] b=new byte[1024*1024];
        try(FileInputStream in=new FileInputStream(f)){for(int n;(n=in.read(b))>0;)d.update(b,0,n);}StringBuilder s=new StringBuilder();for(byte x:d.digest())s.append(String.format(Locale.US,"%02x",x));return s.toString();
    }
}
