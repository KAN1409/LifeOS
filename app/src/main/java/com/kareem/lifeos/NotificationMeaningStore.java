package com.kareem.lifeos;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

/** Durable projection of model-derived notification meaning. Raw evidence remains authoritative. */
final class NotificationMeaningStore extends SQLiteOpenHelper {
    private static final String DB="lifeos_notification_meaning.db";
    private static final int VERSION=1;
    private static volatile NotificationMeaningStore instance;

    static NotificationMeaningStore get(Context context){
        if(instance==null)synchronized(NotificationMeaningStore.class){if(instance==null)instance=new NotificationMeaningStore(context.getApplicationContext());}
        return instance;
    }
    private NotificationMeaningStore(Context context){super(context,DB,null,VERSION);}

    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE meanings(stream_id TEXT PRIMARY KEY,source_observation_id TEXT NOT NULL,type TEXT NOT NULL,intent TEXT NOT NULL,state TEXT NOT NULL,urgency TEXT NOT NULL,action TEXT NOT NULL,summary TEXT NOT NULL,reason TEXT NOT NULL,confidence REAL NOT NULL,model TEXT NOT NULL,understood_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX meanings_time ON meanings(understood_at DESC)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){}

    synchronized void put(NotificationMeaning m){
        if(m==null||m.streamId.isEmpty()||m.sourceObservationId.isEmpty())return;
        ContentValues v=new ContentValues();v.put("stream_id",m.streamId);v.put("source_observation_id",m.sourceObservationId);
        v.put("type",m.type);v.put("intent",m.intent);v.put("state",m.state);v.put("urgency",m.urgency);v.put("action",m.action);
        v.put("summary",m.summary);v.put("reason",m.reason);v.put("confidence",m.confidence);v.put("model",m.model);v.put("understood_at",m.understoodAt);
        getWritableDatabase().insertWithOnConflict("meanings",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }

    synchronized NotificationMeaning forStream(String streamId){
        try(Cursor c=getReadableDatabase().rawQuery("SELECT stream_id,source_observation_id,type,intent,state,urgency,action,summary,reason,confidence,model,understood_at FROM meanings WHERE stream_id=? LIMIT 1",new String[]{safe(streamId)})){
            return c.moveToFirst()?read(c):null;
        }
    }

    synchronized boolean isCurrent(String streamId,String sourceObservationId){
        NotificationMeaning m=forStream(streamId);return m!=null&&m.sourceObservationId.equals(safe(sourceObservationId));
    }

    synchronized List<NotificationMeaning> recent(int limit){
        ArrayList<NotificationMeaning> out=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT stream_id,source_observation_id,type,intent,state,urgency,action,summary,reason,confidence,model,understood_at FROM meanings ORDER BY understood_at DESC LIMIT ?",new String[]{String.valueOf(Math.max(1,limit))})){
            while(c.moveToNext())out.add(read(c));
        }return out;
    }

    synchronized int count(){try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM meanings",null)){return c.moveToFirst()?c.getInt(0):0;}}
    synchronized void eraseAll(){getWritableDatabase().delete("meanings",null,null);}

    private static NotificationMeaning read(Cursor c){return new NotificationMeaning(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getString(6),c.getString(7),c.getString(8),c.getDouble(9),c.getString(10),c.getLong(11));}
    private static String safe(String x){return x==null?"":x;}
}
