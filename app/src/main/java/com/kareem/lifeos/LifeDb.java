package com.kareem.lifeos;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

final class LifeDb extends SQLiteOpenHelper {
    static final class Event { final long id,at; final String app,title,body,type,direction; final double confidence; Event(long id,long at,String app,String title,String body,String type,String direction,double confidence){this.id=id;this.at=at;this.app=app;this.title=title;this.body=body;this.type=type;this.direction=direction;this.confidence=confidence;} }
    LifeDb(Context c){super(c,"lifeos.db",null,1);} 
    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE raw_observations(id INTEGER PRIMARY KEY AUTOINCREMENT, source TEXT, app TEXT, title TEXT, body TEXT, direction TEXT, at INTEGER, confidence REAL)");
        db.execSQL("CREATE TABLE canonical_events(id INTEGER PRIMARY KEY AUTOINCREMENT, app TEXT, title TEXT, body TEXT, type TEXT, direction TEXT, at INTEGER, confidence REAL)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){}
    long addObservation(String source,String app,String title,String body,String direction,long at,double confidence){SQLiteDatabase db=getWritableDatabase();ContentValues v=new ContentValues();v.put("source",source);v.put("app",app);v.put("title",title);v.put("body",body);v.put("direction",direction);v.put("at",at);v.put("confidence",confidence);long raw=db.insert("raw_observations",null,v);ContentValues e=new ContentValues();e.put("app",app);e.put("title",title);e.put("body",body);e.put("type","MESSAGE");e.put("direction",direction);e.put("at",at);e.put("confidence",confidence);db.insert("canonical_events",null,e);return raw;}
    List<Event> recentEvents(int limit){ArrayList<Event> out=new ArrayList<Event>();Cursor c=getReadableDatabase().rawQuery("SELECT id,app,title,body,type,direction,at,confidence FROM canonical_events ORDER BY at DESC LIMIT "+Math.max(1,limit),null);try{while(c.moveToNext())out.add(new Event(c.getLong(0),c.getLong(6),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getDouble(7)));}finally{c.close();}return out;}
    long count(String table){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM "+table,null);try{return c.moveToFirst()?c.getLong(0):0;}finally{c.close();}}
    void eraseAll(){SQLiteDatabase db=getWritableDatabase();db.delete("raw_observations",null,null);db.delete("canonical_events",null,null);}
}
