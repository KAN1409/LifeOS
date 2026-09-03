package com.kareem.lifeos;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

final class LifeDb extends SQLiteOpenHelper {
    static final class Event {
        final long id, at;
        final String app, title, body, threadKey;
        Event(long id, long at, String app, String title, String body, String threadKey) {
            this.id=id; this.at=at; this.app=app; this.title=title; this.body=body; this.threadKey=threadKey;
        }
    }
    static final class Loop {
        final long id, evidenceId, dueAt;
        final String kind, title, status;
        Loop(long id,long evidenceId,long dueAt,String kind,String title,String status){
            this.id=id;this.evidenceId=evidenceId;this.dueAt=dueAt;this.kind=kind;this.title=title;this.status=status;
        }
    }

    LifeDb(Context context) { super(context, "lifeos.db", null, 3); }

    @Override public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
        db.enableWriteAheadLogging();
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE events(id INTEGER PRIMARY KEY AUTOINCREMENT, source_key TEXT NOT NULL UNIQUE, app TEXT NOT NULL, title TEXT NOT NULL, body TEXT NOT NULL, thread_key TEXT NOT NULL, captured_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX events_time ON events(captured_at DESC)");
        db.execSQL("CREATE INDEX events_thread ON events(thread_key,captured_at DESC)");
        db.execSQL("CREATE TABLE open_loops(id INTEGER PRIMARY KEY AUTOINCREMENT, evidence_id INTEGER NOT NULL, fingerprint TEXT NOT NULL UNIQUE, kind TEXT NOT NULL, title TEXT NOT NULL, due_at INTEGER NOT NULL DEFAULT 0, confidence REAL NOT NULL, status TEXT NOT NULL DEFAULT 'open', created_at INTEGER NOT NULL, FOREIGN KEY(evidence_id) REFERENCES events(id) ON DELETE CASCADE)");
        db.execSQL("CREATE INDEX loops_status ON open_loops(status,due_at)");
        createAliases(db);
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) {
        if(oldVersion<2){createAliases(db);db.execSQL("INSERT OR IGNORE INTO notification_aliases(source_key,event_id,first_seen_at) SELECT source_key,id,captured_at FROM events");}
        if(oldVersion<3)db.execSQL("UPDATE open_loops SET status='invalidated' WHERE status='open' AND kind='appointment' AND lower(trim(title)) IN ('today','tomorrow','tonight','النهارده','النهاردة','بكرة','بكره','الليلة')");
    }

    private static void createAliases(SQLiteDatabase db){db.execSQL("CREATE TABLE IF NOT EXISTS notification_aliases(source_key TEXT PRIMARY KEY,event_id INTEGER NOT NULL,first_seen_at INTEGER NOT NULL,FOREIGN KEY(event_id) REFERENCES events(id) ON DELETE CASCADE)");}

    long upsertEvent(String sourceKey,String app,String title,String body,String threadKey,long at) {
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{
            long id=aliasEventId(db,sourceKey);
            if(id>0){db.execSQL("UPDATE events SET title=?,body=?,thread_key=?,updated_at=? WHERE id=?",new Object[]{title,body,threadKey,System.currentTimeMillis(),id});db.setTransactionSuccessful();return id;}
            try(Cursor c=db.rawQuery("SELECT id FROM events WHERE app=? AND title=? AND body=? AND thread_key=? AND captured_at BETWEEN ? AND ? ORDER BY captured_at DESC LIMIT 1",new String[]{app,title,body,threadKey,String.valueOf(at-5000),String.valueOf(at+5000)})){if(c.moveToFirst())id=c.getLong(0);}
            if(id<=0){db.execSQL("INSERT INTO events(source_key,app,title,body,thread_key,captured_at,updated_at) VALUES(?,?,?,?,?,?,?)",new Object[]{sourceKey,app,title,body,threadKey,at,System.currentTimeMillis()});try(Cursor c=db.rawQuery("SELECT last_insert_rowid()",null)){if(c.moveToFirst())id=c.getLong(0);}}
            if(id>0)db.execSQL("INSERT OR REPLACE INTO notification_aliases(source_key,event_id,first_seen_at) VALUES(?,?,?)",new Object[]{sourceKey,id,at});
            db.setTransactionSuccessful();return id;
        }finally{db.endTransaction();}
    }

    long upsertScreenEvent(String sourceKey,String app,String title,String body,String threadKey,long at){long id=upsertEvent(sourceKey,app,title,body,threadKey,at);if(id>0)getWritableDatabase().execSQL("UPDATE events SET captured_at=?,updated_at=? WHERE id=?",new Object[]{at,System.currentTimeMillis(),id});return id;}

    private static long aliasEventId(SQLiteDatabase db,String sourceKey){try(Cursor c=db.rawQuery("SELECT event_id FROM notification_aliases WHERE source_key=?",new String[]{sourceKey})){return c.moveToFirst()?c.getLong(0):0;}}

    void upsertLoop(long evidenceId,OpenLoopExtractor.Candidate x) {
        getWritableDatabase().execSQL("INSERT OR IGNORE INTO open_loops(evidence_id,fingerprint,kind,title,due_at,confidence,created_at) VALUES(?,?,?,?,?,?,?)",
                new Object[]{evidenceId,x.fingerprint,x.kind,x.title,x.dueAt,x.confidence,System.currentTimeMillis()});
    }

    List<Event> recentEvents(int limit) {
        ArrayList<Event> out=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT id,captured_at,app,title,body,thread_key FROM events ORDER BY captured_at DESC LIMIT ?",new String[]{String.valueOf(Math.max(limit*3,limit))})) {
            while(c.moveToNext()&&out.size()<limit){Event next=new Event(c.getLong(0),c.getLong(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5));if(CapturePolicy.isNotificationSummary(next.body)||CapturePolicy.isLauncherSnapshot(next.body)||CapturePolicy.isMessagingHomeSnapshot(next.body))continue;boolean duplicate=false;for(Event seen:out){boolean visible="Visible conversation".equals(seen.title)&&"Visible conversation".equals(next.title)&&same(seen.app,next.app);boolean sameVisible=visible&&(same(seen.threadKey,next.threadKey)||CapturePolicy.sameConversationSnapshot(seen.body,next.body));boolean notificationInsideScreen="Visible conversation".equals(seen.title)&&same(seen.app,next.app)&&CapturePolicy.screenThreadMatchesNotification(seen.threadKey,next.title)&&(seen.body.contains(next.body)||CapturePolicy.sameConversationSnapshot(seen.body,next.body));if(sameVisible||notificationInsideScreen||(Math.abs(seen.at-next.at)<=10000&&same(seen.app,next.app)&&same(seen.title,next.title)&&same(seen.body,next.body))){duplicate=true;break;}}if(!duplicate)out.add(next);}
        }
        return out;
    }

    private static boolean same(String a,String b){return a==null?b==null:a.equals(b);}

    List<Loop> openLoops(int limit) {
        ArrayList<Loop> out=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT id,evidence_id,due_at,kind,title,status FROM open_loops WHERE status='open' ORDER BY CASE WHEN due_at=0 THEN 1 ELSE 0 END,due_at,created_at DESC LIMIT ?",new String[]{String.valueOf(limit)})) {
            while(c.moveToNext())out.add(new Loop(c.getLong(0),c.getLong(1),c.getLong(2),c.getString(3),c.getString(4),c.getString(5)));
        }
        return out;
    }

    void closeLoop(long id){getWritableDatabase().execSQL("UPDATE open_loops SET status='done' WHERE id=?",new Object[]{id});}
    long count(String table){try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM "+table,null)){return c.moveToFirst()?c.getLong(0):0;}}
    void eraseAll(){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{db.delete("open_loops",null,null);db.delete("events",null,null);db.setTransactionSuccessful();}finally{db.endTransaction();}}
}
