package com.kareem.lifeos;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

/** Read-only SQLite integrity evidence for the user's retained physical-device QA ZIP. */
final class PhysicalStorageAudit {
    private PhysicalStorageAudit(){}

    static JSONObject run(Context context){
        Context c=context.getApplicationContext();JSONObject root=new JSONObject();JSONArray dbs=new JSONArray();int pass=0,fail=0;
        try{
            // Ensure the primary stores exist before enumeration, without changing user objects.
            try(LifeDb db=new LifeDb(c)){db.getReadableDatabase();}
            AttentionStore.get(c).openCount();NotificationMeaningStore.get(c).count();UserObjectStore.get(c).fileCount();VoiceMemoryStore.get(c).count();ImageOcrStore.get(c).imageCount();
            String privateRoot=c.getApplicationInfo().dataDir+File.separator;
            for(String name:c.databaseList()){
                JSONObject j=new JSONObject();j.put("name",name);File f=c.getDatabasePath(name);j.put("path_private",f.getAbsolutePath().startsWith(privateRoot));j.put("size_bytes",f.exists()?f.length():0);
                boolean ok=true;String quick="missing";int foreignKeys=-1;SQLiteDatabase db=null;
                try{
                    db=SQLiteDatabase.openDatabase(f.getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY);
                    try(Cursor q=db.rawQuery("PRAGMA quick_check",null)){if(q.moveToFirst())quick=q.getString(0);ok="ok".equalsIgnoreCase(quick);else ok=false;}
                    try(Cursor fk=db.rawQuery("PRAGMA foreign_key_check",null)){foreignKeys=fk.getCount();if(foreignKeys!=0)ok=false;}
                }catch(Throwable t){ok=false;j.put("error",String.valueOf(t));}finally{if(db!=null)try{db.close();}catch(Throwable ignored){}}
                if(!j.optBoolean("path_private",false))ok=false;j.put("quick_check",quick);j.put("foreign_key_violations",foreignKeys);j.put("status",ok?"PASS":"FAIL");if(ok)pass++;else fail++;dbs.put(j);
            }
        }catch(Throwable t){try{root.put("audit_error",String.valueOf(t));}catch(Exception ignored){}fail++;}
        try{root.put("database_count",dbs.length());root.put("pass",pass);root.put("fail",fail);root.put("databases",dbs);}catch(Exception ignored){}
        return root;
    }

    static void write(Context c,File out)throws Exception{JSONObject j=run(c);try(FileOutputStream f=new FileOutputStream(out)){f.write(j.toString(2).getBytes(StandardCharsets.UTF_8));}}
}
