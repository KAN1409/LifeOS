package com.kareem.lifeos;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

/**
 * Durable attention ledger plus semantic work queue.
 *
 * Seeing/opening/dismissing a source notification never mutates attention state here. Confirmed
 * attention remains OPEN until the user explicitly marks the item handled inside LifeOS.
 */
final class AttentionStore extends SQLiteOpenHelper {
    private static final String DB="lifeos_attention.db";
    private static final int VERSION=1;
    private static volatile AttentionStore instance;

    static final String PROVISIONAL="provisional";
    static final String OPEN="open";
    static final String HANDLED="handled";
    static final String REJECTED="rejected";

    static final class Item {
        final long id,eventId,sourceAt,createdAt,updatedAt,handledAt;
        final int priority;
        final String sourceObservationId,streamId,status,type,intent,urgency,action,summary,reason,model;
        final double confidence;
        final boolean provisional;
        Item(long id,long eventId,long sourceAt,long createdAt,long updatedAt,long handledAt,int priority,
             String sourceObservationId,String streamId,String status,String type,String intent,
             String urgency,String action,String summary,String reason,String model,double confidence,
             boolean provisional){
            this.id=id;this.eventId=eventId;this.sourceAt=sourceAt;this.createdAt=createdAt;
            this.updatedAt=updatedAt;this.handledAt=handledAt;this.priority=priority;
            this.sourceObservationId=safe(sourceObservationId);this.streamId=safe(streamId);
            this.status=safe(status);this.type=safe(type);this.intent=safe(intent);
            this.urgency=safe(urgency);this.action=safe(action);this.summary=safe(summary);
            this.reason=safe(reason);this.model=safe(model);this.confidence=confidence;
            this.provisional=provisional;
        }
        boolean isOpen(){return OPEN.equals(status)||PROVISIONAL.equals(status);}
    }

    static final class WorkItem {
        final String observationId,streamId;
        final long eventId,sourceAt,queuedAt;
        final int priority,attempts;
        WorkItem(String observationId,String streamId,long eventId,long sourceAt,long queuedAt,int priority,int attempts){
            this.observationId=safe(observationId);this.streamId=safe(streamId);this.eventId=eventId;
            this.sourceAt=sourceAt;this.queuedAt=queuedAt;this.priority=priority;this.attempts=attempts;
        }
    }

    static AttentionStore get(Context context){
        if(instance==null)synchronized(AttentionStore.class){
            if(instance==null)instance=new AttentionStore(context.getApplicationContext());
        }
        return instance;
    }
    private AttentionStore(Context context){super(context,DB,null,VERSION);}

    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE semantic_queue(observation_id TEXT PRIMARY KEY,stream_id TEXT NOT NULL,event_id INTEGER NOT NULL,source_at INTEGER NOT NULL,queued_at INTEGER NOT NULL,priority INTEGER NOT NULL DEFAULT 0,status TEXT NOT NULL DEFAULT 'pending',attempts INTEGER NOT NULL DEFAULT 0,last_error TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE INDEX semantic_queue_pending ON semantic_queue(status,attempts,priority DESC,queued_at)");
        db.execSQL("CREATE TABLE attention_items(id INTEGER PRIMARY KEY AUTOINCREMENT,event_id INTEGER NOT NULL UNIQUE,source_observation_id TEXT NOT NULL,stream_id TEXT NOT NULL,source_at INTEGER NOT NULL,status TEXT NOT NULL,type TEXT NOT NULL,intent TEXT NOT NULL,urgency TEXT NOT NULL,action TEXT NOT NULL,summary TEXT NOT NULL,reason TEXT NOT NULL,confidence REAL NOT NULL,priority INTEGER NOT NULL,model TEXT NOT NULL,provisional INTEGER NOT NULL DEFAULT 0,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL,handled_at INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX attention_open ON attention_items(status,priority DESC,updated_at DESC)");
        db.execSQL("CREATE INDEX attention_stream ON attention_items(stream_id,source_at DESC)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){}

    synchronized void enqueue(String observationId,String streamId,long eventId,long sourceAt,int priority){
        if(safe(observationId).isEmpty()||safe(streamId).isEmpty()||eventId<=0)return;
        ContentValues v=new ContentValues();v.put("observation_id",observationId);v.put("stream_id",streamId);
        v.put("event_id",eventId);v.put("source_at",sourceAt);v.put("queued_at",System.currentTimeMillis());
        v.put("priority",priority);v.put("status","pending");
        getWritableDatabase().insertWithOnConflict("semantic_queue",null,v,SQLiteDatabase.CONFLICT_IGNORE);
        getWritableDatabase().execSQL("UPDATE semantic_queue SET priority=MAX(priority,?),event_id=?,source_at=? WHERE observation_id=? AND status='pending'",
                new Object[]{priority,eventId,sourceAt,observationId});
    }

    synchronized List<WorkItem> pendingWork(int limit){
        ArrayList<WorkItem> out=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT observation_id,stream_id,event_id,source_at,queued_at,priority,attempts FROM semantic_queue WHERE status='pending' ORDER BY attempts ASC,priority DESC,queued_at ASC LIMIT ?",new String[]{String.valueOf(Math.max(1,limit))})){
            while(c.moveToNext())out.add(new WorkItem(c.getString(0),c.getString(1),c.getLong(2),c.getLong(3),c.getLong(4),c.getInt(5),c.getInt(6)));
        }
        return out;
    }

    synchronized void markAttempt(String observationId){
        getWritableDatabase().execSQL("UPDATE semantic_queue SET attempts=attempts+1 WHERE observation_id=? AND status='pending'",new Object[]{safe(observationId)});
    }
    synchronized void markFailure(String observationId,String error){
        getWritableDatabase().execSQL("UPDATE semantic_queue SET last_error=? WHERE observation_id=? AND status='pending'",new Object[]{clip(safe(error),240),safe(observationId)});
    }
    synchronized void markAnalyzed(String observationId,long eventId){
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try{
            db.execSQL("UPDATE semantic_queue SET status='done',last_error='' WHERE observation_id=?",new Object[]{safe(observationId)});
            if(eventId>0)db.execSQL("UPDATE semantic_queue SET status='done',last_error='' WHERE event_id=? AND status='pending'",new Object[]{eventId});
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }
    synchronized void markOrphaned(String observationId){
        getWritableDatabase().execSQL("UPDATE semantic_queue SET status='orphaned',last_error='raw observation no longer available' WHERE observation_id=?",new Object[]{safe(observationId)});
    }

    /** Fast local gate may create a provisional item, never a handled item. */
    synchronized void provisional(String sourceObservationId,String streamId,long eventId,long sourceAt,
                                  String type,String intent,String urgency,String action,String summary,
                                  String reason,double confidence,int priority){
        if(eventId<=0||safe(sourceObservationId).isEmpty())return;
        long now=System.currentTimeMillis();SQLiteDatabase db=getWritableDatabase();
        Item existing=forEvent(eventId);
        if(existing!=null&&(HANDLED.equals(existing.status)||OPEN.equals(existing.status)))return;
        ContentValues v=values(sourceObservationId,streamId,eventId,sourceAt,PROVISIONAL,type,intent,urgency,action,
                summary,reason,confidence,priority,"fast-local",true,now);
        if(existing==null)db.insertWithOnConflict("attention_items",null,v,SQLiteDatabase.CONFLICT_IGNORE);
        else db.update("attention_items",v,"event_id=?",new String[]{String.valueOf(eventId)});
    }

    /** Deep model confirms/rejects only this evidence item; it never closes another open item. */
    synchronized void applyModel(NotificationMeaning m,long eventId,long sourceAt){
        if(m==null||eventId<=0)return;
        Item existing=forEvent(eventId);long now=System.currentTimeMillis();
        if(m.needsAttention()){
            if(existing!=null&&HANDLED.equals(existing.status)){
                updateMeaningOnly(m,eventId,sourceAt,existing.status,existing.handledAt,now,false);
                return;
            }
            ContentValues v=values(m.sourceObservationId,m.streamId,eventId,sourceAt,OPEN,m.type,m.intent,m.urgency,m.action,
                    m.summary,m.reason,m.confidence,m.priority(),m.model,false,now);
            if(existing==null)getWritableDatabase().insertWithOnConflict("attention_items",null,v,SQLiteDatabase.CONFLICT_IGNORE);
            else getWritableDatabase().update("attention_items",v,"event_id=?",new String[]{String.valueOf(eventId)});
            return;
        }
        // A model may reject a provisional false positive, but confirmed OPEN/HANDLED items are
        // never auto-closed. Only explicit Mark handled closes confirmed attention.
        if(existing!=null&&PROVISIONAL.equals(existing.status)){
            updateMeaningOnly(m,eventId,sourceAt,REJECTED,0,now,false);
        }
    }

    private void updateMeaningOnly(NotificationMeaning m,long eventId,long sourceAt,String status,long handledAt,long now,boolean provisional){
        ContentValues v=values(m.sourceObservationId,m.streamId,eventId,sourceAt,status,m.type,m.intent,m.urgency,m.action,
                m.summary,m.reason,m.confidence,m.priority(),m.model,provisional,now);
        v.put("handled_at",handledAt);
        getWritableDatabase().update("attention_items",v,"event_id=?",new String[]{String.valueOf(eventId)});
    }

    synchronized void markHandled(long eventId){
        long now=System.currentTimeMillis();
        getWritableDatabase().execSQL("UPDATE attention_items SET status='handled',provisional=0,handled_at=?,updated_at=? WHERE event_id=? AND status IN ('open','provisional')",new Object[]{now,now,eventId});
    }

    synchronized Item forEvent(long eventId){
        if(eventId<=0)return null;
        try(Cursor c=getReadableDatabase().rawQuery(select()+" WHERE event_id=? LIMIT 1",new String[]{String.valueOf(eventId)})){
            return c.moveToFirst()?read(c):null;
        }
    }

    synchronized List<Item> openItems(int limit){
        ArrayList<Item> out=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery(select()+" WHERE status IN ('open','provisional') ORDER BY priority DESC,updated_at DESC LIMIT ?",new String[]{String.valueOf(Math.max(1,limit))})){
            while(c.moveToNext())out.add(read(c));
        }
        return out;
    }

    synchronized int openCount(){
        try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM attention_items WHERE status IN ('open','provisional')",null)){return c.moveToFirst()?c.getInt(0):0;}
    }
    synchronized int pendingCount(){
        try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM semantic_queue WHERE status='pending'",null)){return c.moveToFirst()?c.getInt(0):0;}
    }
    synchronized int confirmedOpenCount(){
        try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM attention_items WHERE status='open'",null)){return c.moveToFirst()?c.getInt(0):0;}
    }
    synchronized void eraseAll(){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{db.delete("semantic_queue",null,null);db.delete("attention_items",null,null);db.setTransactionSuccessful();}finally{db.endTransaction();}}

    private static ContentValues values(String observationId,String streamId,long eventId,long sourceAt,String status,
                                        String type,String intent,String urgency,String action,String summary,String reason,
                                        double confidence,int priority,String model,boolean provisional,long now){
        ContentValues v=new ContentValues();v.put("event_id",eventId);v.put("source_observation_id",safe(observationId));v.put("stream_id",safe(streamId));
        v.put("source_at",sourceAt);v.put("status",safe(status));v.put("type",safe(type));v.put("intent",safe(intent));v.put("urgency",safe(urgency));
        v.put("action",safe(action));v.put("summary",clip(safe(summary),220));v.put("reason",clip(safe(reason),260));v.put("confidence",Math.max(0,Math.min(1,confidence)));
        v.put("priority",priority);v.put("model",safe(model));v.put("provisional",provisional?1:0);v.put("updated_at",now);v.put("handled_at",0);
        v.put("created_at",now);return v;
    }

    private static String select(){return "SELECT id,event_id,source_at,created_at,updated_at,handled_at,priority,source_observation_id,stream_id,status,type,intent,urgency,action,summary,reason,model,confidence,provisional FROM attention_items";}
    private static Item read(Cursor c){return new Item(c.getLong(0),c.getLong(1),c.getLong(2),c.getLong(3),c.getLong(4),c.getLong(5),c.getInt(6),c.getString(7),c.getString(8),c.getString(9),c.getString(10),c.getString(11),c.getString(12),c.getString(13),c.getString(14),c.getString(15),c.getString(16),c.getDouble(17),c.getInt(18)!=0);}
    private static String safe(String x){return x==null?"":x;}
    private static String clip(String x,int n){return x.length()<=n?x:x.substring(0,n)+"…";}
}
