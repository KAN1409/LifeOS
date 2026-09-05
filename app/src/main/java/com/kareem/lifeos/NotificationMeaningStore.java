package com.kareem.lifeos;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

/** Durable model meaning per evidence item. Raw evidence remains authoritative. */
final class NotificationMeaningStore extends SQLiteOpenHelper {
    private static final String DB="lifeos_notification_meaning.db";
    private static final int VERSION=2;
    private static volatile NotificationMeaningStore instance;

    static NotificationMeaningStore get(Context context){
        if(instance==null)synchronized(NotificationMeaningStore.class){if(instance==null)instance=new NotificationMeaningStore(context.getApplicationContext());}
        return instance;
    }
    private NotificationMeaningStore(Context context){super(context,DB,null,VERSION);}

    @Override public void onCreate(SQLiteDatabase db){createSchema(db);}
    private static void createSchema(SQLiteDatabase db){
        db.execSQL("CREATE TABLE IF NOT EXISTS meanings(source_observation_id TEXT PRIMARY KEY,stream_id TEXT NOT NULL,source_observed_at INTEGER NOT NULL DEFAULT 0,type TEXT NOT NULL,intent TEXT NOT NULL,state TEXT NOT NULL,urgency TEXT NOT NULL,action TEXT NOT NULL,summary TEXT NOT NULL,reason TEXT NOT NULL,confidence REAL NOT NULL,model TEXT NOT NULL,understood_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS meanings_time ON meanings(understood_at DESC)");
        db.execSQL("CREATE INDEX IF NOT EXISTS meanings_stream_time ON meanings(stream_id,source_observed_at DESC,understood_at DESC)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){
        if(oldVersion<2){
            db.execSQL("DROP INDEX IF EXISTS meanings_time");
            db.execSQL("DROP INDEX IF EXISTS meanings_stream_time");
            db.execSQL("ALTER TABLE meanings RENAME TO meanings_v1");
            createSchema(db);
            db.execSQL("INSERT OR IGNORE INTO meanings(source_observation_id,stream_id,source_observed_at,type,intent,state,urgency,action,summary,reason,confidence,model,understood_at) SELECT source_observation_id,stream_id,0,type,intent,state,urgency,action,summary,reason,confidence,model,understood_at FROM meanings_v1");
            db.execSQL("DROP TABLE meanings_v1");
        }
    }

    synchronized void put(NotificationMeaning m){put(m,0);}
    synchronized void put(NotificationMeaning m,long sourceObservedAt){
        if(m==null||m.streamId.isEmpty()||m.sourceObservationId.isEmpty())return;
        ContentValues v=new ContentValues();v.put("source_observation_id",m.sourceObservationId);v.put("stream_id",m.streamId);
        v.put("source_observed_at",Math.max(0,sourceObservedAt));v.put("type",m.type);v.put("intent",m.intent);v.put("state",m.state);
        v.put("urgency",m.urgency);v.put("action",m.action);v.put("summary",m.summary);v.put("reason",m.reason);
        v.put("confidence",m.confidence);v.put("model",m.model);v.put("understood_at",m.understoodAt);
        getWritableDatabase().insertWithOnConflict("meanings",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }

    synchronized NotificationMeaning forStream(String streamId){
        try(Cursor c=getReadableDatabase().rawQuery(select()+" WHERE stream_id=? ORDER BY source_observed_at DESC,understood_at DESC LIMIT 1",new String[]{safe(streamId)})){
            return c.moveToFirst()?read(c):null;
        }
    }

    /** Exact/near-exact evidence projection, preventing a newer message in one thread from repainting older events. */
    synchronized NotificationMeaning forStreamAt(String streamId,long sourceAt){
        long lo=Math.max(0,sourceAt-7000),hi=sourceAt+7000;
        try(Cursor c=getReadableDatabase().rawQuery(select()+" WHERE stream_id=? AND source_observed_at BETWEEN ? AND ? ORDER BY ABS(source_observed_at-?) ASC,understood_at DESC LIMIT 1",new String[]{safe(streamId),String.valueOf(lo),String.valueOf(hi),String.valueOf(sourceAt)})){
            return c.moveToFirst()?read(c):null;
        }
    }

    synchronized NotificationMeaning forObservation(String sourceObservationId){
        try(Cursor c=getReadableDatabase().rawQuery(select()+" WHERE source_observation_id=? LIMIT 1",new String[]{safe(sourceObservationId)})){
            return c.moveToFirst()?read(c):null;
        }
    }

    synchronized boolean isCurrent(String streamId,String sourceObservationId){NotificationMeaning m=forStream(streamId);return m!=null&&m.sourceObservationId.equals(safe(sourceObservationId));}

    /** Latest projection per stream, used by conversation/Today UI. */
    synchronized List<NotificationMeaning> recent(int limit){
        ArrayList<NotificationMeaning> out=new ArrayList<>();
        String sql=select()+" m WHERE NOT EXISTS (SELECT 1 FROM meanings n WHERE n.stream_id=m.stream_id AND (n.source_observed_at>m.source_observed_at OR (n.source_observed_at=m.source_observed_at AND n.understood_at>m.understood_at))) ORDER BY m.understood_at DESC LIMIT ?";
        try(Cursor c=getReadableDatabase().rawQuery(sql,new String[]{String.valueOf(Math.max(1,limit))})){while(c.moveToNext())out.add(read(c));}return out;
    }

    synchronized int count(){try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM meanings",null)){return c.moveToFirst()?c.getInt(0):0;}}
    synchronized void eraseAll(){getWritableDatabase().delete("meanings",null,null);}

    private static String select(){return "SELECT stream_id,source_observation_id,type,intent,state,urgency,action,summary,reason,confidence,model,understood_at FROM meanings";}
    private static NotificationMeaning read(Cursor c){return new NotificationMeaning(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getString(6),c.getString(7),c.getString(8),c.getDouble(9),c.getString(10),c.getLong(11));}
    private static String safe(String x){return x==null?"":x;}
}
