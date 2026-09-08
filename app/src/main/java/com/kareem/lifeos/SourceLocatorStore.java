package com.kareem.lifeos;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/** Durable return-address metadata captured alongside evidence. */
final class SourceLocatorStore extends SQLiteOpenHelper {
    static final class Locator {final long eventId,capturedAt;final String notificationKey,app,thread;Locator(long eventId,String notificationKey,String app,String thread,long capturedAt){this.eventId=eventId;this.notificationKey=s(notificationKey);this.app=s(app);this.thread=s(thread);this.capturedAt=capturedAt;}}
    private static volatile SourceLocatorStore instance;
    static SourceLocatorStore get(Context c){if(instance==null)synchronized(SourceLocatorStore.class){if(instance==null)instance=new SourceLocatorStore(c.getApplicationContext());}return instance;}
    private SourceLocatorStore(Context c){super(c,"lifeos_source_locator.db",null,1);}
    @Override public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE locators(event_id INTEGER PRIMARY KEY,notification_key TEXT NOT NULL,app TEXT NOT NULL,thread TEXT NOT NULL,captured_at INTEGER NOT NULL)");db.execSQL("CREATE INDEX locator_notification ON locators(notification_key)");}
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){}
    synchronized void record(long eventId,String notificationKey,String app,String thread,long at){if(eventId<=0)return;ContentValues v=new ContentValues();v.put("event_id",eventId);v.put("notification_key",s(notificationKey));v.put("app",s(app));v.put("thread",s(thread));v.put("captured_at",at);getWritableDatabase().insertWithOnConflict("locators",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    synchronized Locator forEvent(long eventId){try(Cursor c=getReadableDatabase().rawQuery("SELECT event_id,notification_key,app,thread,captured_at FROM locators WHERE event_id=? LIMIT 1",new String[]{Long.toString(eventId)})){return c.moveToFirst()?new Locator(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getLong(4)):null;}}
    synchronized void eraseAll(){getWritableDatabase().delete("locators",null,null);}
    private static String s(String x){return x==null?"":x;}
}
