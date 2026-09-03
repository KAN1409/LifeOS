package com.kareem.lifeos.engine;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

/** Persistent shadow store, intentionally isolated from legacy LifeDb. */
public final class PersistentUnderstandingStore extends SQLiteOpenHelper {
    private static final String DB_NAME="lifeos_understanding.db";
    private static final int DB_VERSION=2;
    private static final int MAX_RAW_ROWS=5000;
    private static volatile PersistentUnderstandingStore instance;

    private PersistentUnderstandingStore(Context context){super(context.getApplicationContext(),DB_NAME,null,DB_VERSION);}
    public static PersistentUnderstandingStore get(Context context){if(instance==null){synchronized(PersistentUnderstandingStore.class){if(instance==null)instance=new PersistentUnderstandingStore(context);}}return instance;}

    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE raw_evidence (id INTEGER PRIMARY KEY AUTOINCREMENT,dedupe_key TEXT UNIQUE,source TEXT NOT NULL,thread TEXT NOT NULL,type TEXT NOT NULL,direction TEXT NOT NULL,text TEXT NOT NULL,observed_at INTEGER NOT NULL,confidence REAL NOT NULL,payload TEXT NOT NULL)");
        db.execSQL("CREATE INDEX raw_evidence_time_idx ON raw_evidence(observed_at DESC)");
        db.execSQL("CREATE TABLE canonical_events (id INTEGER PRIMARY KEY AUTOINCREMENT,canonical_key TEXT UNIQUE,type TEXT NOT NULL,direction TEXT NOT NULL,text TEXT NOT NULL,observed_at INTEGER NOT NULL,confidence REAL NOT NULL,sources TEXT NOT NULL)");
        db.execSQL("CREATE INDEX canonical_events_time_idx ON canonical_events(observed_at DESC)");
        createMetadata(db);
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){if(oldVersion<2)createMetadata(db);}
    private static void createMetadata(SQLiteDatabase db){db.execSQL("CREATE TABLE IF NOT EXISTS understanding_metadata (meta_key TEXT PRIMARY KEY,meta_value TEXT NOT NULL)");}

    public synchronized void recordScreen(RawScreenSnapshot snapshot,List<MessageObservation> messages){
        if(snapshot==null)return;SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try{ContentValues tree=new ContentValues();tree.put("dedupe_key","tree|"+snapshot.packageName+"|"+snapshot.capturedAt);tree.put("source","SCREEN_TREE");tree.put("thread","");tree.put("type","SCREEN_SNAPSHOT");tree.put("direction","UNKNOWN");tree.put("text","");tree.put("observed_at",snapshot.capturedAt);tree.put("confidence",1.0);tree.put("payload",RawEvidenceSerializer.snapshot(snapshot));db.insertWithOnConflict("raw_evidence",null,tree,SQLiteDatabase.CONFLICT_IGNORE);if(messages!=null)for(MessageObservation m:messages)if(m!=null){ContentValues v=new ContentValues();v.put("dedupe_key","screen|"+m.observedAt+"|"+m.direction.name()+"|"+ReconciliationKey.normalize(m.text));v.put("source","SCREEN");v.put("thread","");v.put("type",m.type);v.put("direction",m.direction.name());v.put("text",m.text);v.put("observed_at",m.observedAt);v.put("confidence",m.confidence);v.put("payload","");db.insertWithOnConflict("raw_evidence",null,v,SQLiteDatabase.CONFLICT_IGNORE);}trimRaw(db);db.setTransactionSuccessful();}finally{db.endTransaction();}
    }
    public synchronized void recordNotification(NotificationObservation n){if(n==null)return;SQLiteDatabase db=getWritableDatabase();ContentValues v=new ContentValues();String identity=n.evidenceId.trim().isEmpty()?n.thread+"|"+n.observedAt+"|"+ReconciliationKey.normalize(n.text):n.evidenceId;v.put("dedupe_key","notification|"+identity);v.put("source","NOTIFICATION");v.put("thread",n.thread);v.put("type","MESSAGE");v.put("direction","IN");v.put("text",n.text);v.put("observed_at",n.observedAt);v.put("confidence",n.confidence);v.put("payload",n.evidenceId);db.insertWithOnConflict("raw_evidence",null,v,SQLiteDatabase.CONFLICT_REPLACE);trimRaw(db);}

    public synchronized List<PersistentRawEvidence> loadRawEvidence(){List<PersistentRawEvidence> out=new ArrayList<PersistentRawEvidence>();Cursor c=getReadableDatabase().query("raw_evidence",new String[]{"id","source","thread","type","direction","text","observed_at","confidence","payload"},null,null,null,null,"observed_at ASC,id ASC");try{while(c.moveToNext()){MessageObservation.Direction d;try{d=MessageObservation.Direction.valueOf(c.getString(4));}catch(Exception e){d=MessageObservation.Direction.UNKNOWN;}out.add(new PersistentRawEvidence(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),d,c.getString(5),c.getLong(6),c.getDouble(7),c.getString(8)));}}finally{c.close();}return out;}

    public synchronized List<CanonicalEventRecord> recentCanonical(int limit){
        int safe=Math.max(1,Math.min(500,limit));List<CanonicalEventRecord> out=new ArrayList<CanonicalEventRecord>();
        Cursor c=getReadableDatabase().query("canonical_events",new String[]{"id","type","direction","text","observed_at","confidence","sources"},null,null,null,null,"observed_at DESC,id DESC",Integer.toString(safe));
        try{while(c.moveToNext()){MessageObservation.Direction d;try{d=MessageObservation.Direction.valueOf(c.getString(2));}catch(Exception e){d=MessageObservation.Direction.UNKNOWN;}out.add(new CanonicalEventRecord(c.getLong(0),c.getString(1),d,c.getString(3),c.getLong(4),c.getDouble(5),c.getString(6)));}}finally{c.close();}return out;
    }

    public synchronized void replaceCanonical(List<CanonicalEvent> events){replaceCanonical(events,UnderstandingEngineVersion.CURRENT,System.currentTimeMillis());}
    public synchronized void replaceCanonical(List<CanonicalEvent> events,String engineVersion,long rebuiltAt){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{db.delete("canonical_events",null,null);if(events!=null)for(CanonicalEvent e:events)if(e!=null){ContentValues v=new ContentValues();v.put("canonical_key",e.type+"|"+e.direction.name()+"|"+e.observedAt+"|"+ReconciliationKey.normalize(e.text));v.put("type",e.type);v.put("direction",e.direction.name());v.put("text",e.text);v.put("observed_at",e.observedAt);v.put("confidence",e.confidence);v.put("sources",join(e.sources));db.insertWithOnConflict("canonical_events",null,v,SQLiteDatabase.CONFLICT_REPLACE);}putMeta(db,"engine_version",engineVersion);putMeta(db,"rebuilt_at",Long.toString(rebuiltAt));db.setTransactionSuccessful();}finally{db.endTransaction();}}
    public synchronized String canonicalEngineVersion(){return getMeta("engine_version");}
    public synchronized long canonicalRebuiltAt(){try{return Long.parseLong(getMeta("rebuilt_at"));}catch(Exception e){return 0L;}}
    public synchronized boolean needsReplay(){return !UnderstandingEngineVersion.isCurrent(canonicalEngineVersion());}
    public synchronized void clearCanonicalOnly(){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{db.delete("canonical_events",null,null);putMeta(db,"engine_version","");putMeta(db,"rebuilt_at","0");db.setTransactionSuccessful();}finally{db.endTransaction();}}
    public synchronized void eraseAllUnderstanding(){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{db.delete("raw_evidence",null,null);db.delete("canonical_events",null,null);db.delete("understanding_metadata",null,null);db.setTransactionSuccessful();}finally{db.endTransaction();}}
    private String getMeta(String key){Cursor c=getReadableDatabase().query("understanding_metadata",new String[]{"meta_value"},"meta_key=?",new String[]{key},null,null,null);try{return c.moveToFirst()?c.getString(0):"";}finally{c.close();}}
    private static void putMeta(SQLiteDatabase db,String key,String value){ContentValues v=new ContentValues();v.put("meta_key",key);v.put("meta_value",value==null?"":value);db.insertWithOnConflict("understanding_metadata",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    private static void trimRaw(SQLiteDatabase db){db.execSQL("DELETE FROM raw_evidence WHERE id NOT IN (SELECT id FROM raw_evidence ORDER BY id DESC LIMIT "+MAX_RAW_ROWS+")");}
    private static String join(List<String> xs){if(xs==null||xs.isEmpty())return "";StringBuilder b=new StringBuilder();for(String x:xs){if(b.length()>0)b.append(',');b.append(x==null?"":x);}return b.toString();}
}
