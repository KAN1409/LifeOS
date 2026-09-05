package com.kareem.lifeos;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

/** Durable voice-memory source + verbatim transcript segments. Audio survives transcription failure. */
final class VoiceMemoryStore extends SQLiteOpenHelper {
    private static final String DB="lifeos_voice_memory.db";private static final int VERSION=1;private static volatile VoiceMemoryStore instance;
    static VoiceMemoryStore get(Context c){if(instance==null)synchronized(VoiceMemoryStore.class){if(instance==null)instance=new VoiceMemoryStore(c.getApplicationContext());}return instance;}
    private VoiceMemoryStore(Context c){super(c,DB,null,VERSION);}
    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE voice_memories(id TEXT PRIMARY KEY,file_path TEXT NOT NULL UNIQUE,created_at INTEGER NOT NULL,duration_ms INTEGER NOT NULL DEFAULT 0,size_bytes INTEGER NOT NULL DEFAULT 0,status TEXT NOT NULL,transcript TEXT NOT NULL DEFAULT '',language TEXT NOT NULL DEFAULT '',engine TEXT NOT NULL DEFAULT '',engine_version TEXT NOT NULL DEFAULT '',error TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE INDEX voice_time ON voice_memories(created_at DESC)");
        db.execSQL("CREATE TABLE transcript_segments(id INTEGER PRIMARY KEY AUTOINCREMENT,voice_id TEXT NOT NULL,start_ms INTEGER NOT NULL,end_ms INTEGER NOT NULL,text TEXT NOT NULL,confidence REAL NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX voice_segments ON transcript_segments(voice_id,start_ms)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){}
    synchronized void add(String id,String path,long createdAt,long duration,long size){ContentValues v=new ContentValues();v.put("id",id);v.put("file_path",path);v.put("created_at",createdAt);v.put("duration_ms",Math.max(0,duration));v.put("size_bytes",Math.max(0,size));v.put("status","recorded");getWritableDatabase().insertWithOnConflict("voice_memories",null,v,SQLiteDatabase.CONFLICT_IGNORE);}
    synchronized void setStatus(String id,String status,String error){getWritableDatabase().execSQL("UPDATE voice_memories SET status=?,error=? WHERE id=?",new Object[]{s(status),s(error),s(id)});}
    synchronized void saveTranscript(String id,VoiceTranscript t){if(t==null)return;SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{ContentValues v=new ContentValues();v.put("status","transcribed");v.put("transcript",t.text);v.put("language",t.language);v.put("engine",t.engine);v.put("engine_version",t.version);v.put("error","");if(t.durationMs>0)v.put("duration_ms",t.durationMs);db.update("voice_memories",v,"id=?",new String[]{id});db.delete("transcript_segments","voice_id=?",new String[]{id});for(VoiceTranscript.Segment x:t.segments){ContentValues sv=new ContentValues();sv.put("voice_id",id);sv.put("start_ms",x.startMs);sv.put("end_ms",x.endMs);sv.put("text",x.text);sv.put("confidence",x.confidence);db.insert("transcript_segments",null,sv);}db.setTransactionSuccessful();}finally{db.endTransaction();}}
    synchronized int count(){try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM voice_memories",null)){return c.moveToFirst()?c.getInt(0):0;}}
    synchronized List<String[]> list(int limit){ArrayList<String[]> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT id,file_path,created_at,duration_ms,size_bytes,status,transcript,language,engine,engine_version,error FROM voice_memories ORDER BY created_at DESC LIMIT ?",new String[]{String.valueOf(Math.max(1,limit))})){while(c.moveToNext())out.add(row(c));}return out;}
    synchronized String[] load(String id){try(Cursor c=getReadableDatabase().rawQuery("SELECT id,file_path,created_at,duration_ms,size_bytes,status,transcript,language,engine,engine_version,error FROM voice_memories WHERE id=? LIMIT 1",new String[]{s(id)})){return c.moveToFirst()?row(c):null;}}
    synchronized List<VoiceTranscript.Segment> segments(String id){ArrayList<VoiceTranscript.Segment> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT start_ms,end_ms,text,confidence FROM transcript_segments WHERE voice_id=? ORDER BY start_ms",new String[]{s(id)})){while(c.moveToNext())out.add(new VoiceTranscript.Segment(c.getLong(0),c.getLong(1),c.getString(2),c.getFloat(3)));}return out;}
    synchronized void eraseAll(){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{db.delete("transcript_segments",null,null);db.delete("voice_memories",null,null);db.setTransactionSuccessful();}finally{db.endTransaction();}}
    private static String[] row(Cursor c){return new String[]{c.getString(0),c.getString(1),String.valueOf(c.getLong(2)),String.valueOf(c.getLong(3)),String.valueOf(c.getLong(4)),c.getString(5),c.getString(6),c.getString(7),c.getString(8),c.getString(9),c.getString(10)};}
    private static String s(String x){return x==null?"":x.trim();}
}
