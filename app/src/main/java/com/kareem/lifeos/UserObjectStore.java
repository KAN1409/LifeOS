package com.kareem.lifeos;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

/** Durable typed store for user-curated LifeOS objects: files, places and projects. */
final class UserObjectStore extends SQLiteOpenHelper {
    private static final String DB="lifeos_user_objects.db";private static final int VERSION=1;private static volatile UserObjectStore instance;
    static UserObjectStore get(Context c){if(instance==null)synchronized(UserObjectStore.class){if(instance==null)instance=new UserObjectStore(c.getApplicationContext());}return instance;}
    private UserObjectStore(Context c){super(c,DB,null,VERSION);}
    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE files(id TEXT PRIMARY KEY,uri TEXT NOT NULL UNIQUE,display_name TEXT NOT NULL,mime_type TEXT NOT NULL,size_bytes INTEGER NOT NULL DEFAULT -1,source TEXT NOT NULL,added_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX files_time ON files(added_at DESC)");
        db.execSQL("CREATE TABLE places(id TEXT PRIMARY KEY,label TEXT NOT NULL,latitude REAL NOT NULL,longitude REAL NOT NULL,accuracy REAL NOT NULL DEFAULT 0,provider TEXT NOT NULL,observed_at INTEGER NOT NULL,added_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX places_time ON places(observed_at DESC)");
        db.execSQL("CREATE TABLE projects(id TEXT PRIMARY KEY,name TEXT NOT NULL,description TEXT NOT NULL,status TEXT NOT NULL,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX projects_time ON projects(updated_at DESC)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){}

    synchronized void upsertFile(String id,String uri,String name,String mime,long size,String source,long addedAt){ContentValues v=new ContentValues();v.put("id",id);v.put("uri",uri);v.put("display_name",name);v.put("mime_type",mime);v.put("size_bytes",size);v.put("source",source);v.put("added_at",addedAt);getWritableDatabase().insertWithOnConflict("files",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    synchronized List<String[]> files(int limit){ArrayList<String[]> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT id,uri,display_name,mime_type,size_bytes,source,added_at FROM files ORDER BY added_at DESC LIMIT ?",new String[]{String.valueOf(Math.max(1,limit))})){while(c.moveToNext())out.add(new String[]{c.getString(0),c.getString(1),c.getString(2),c.getString(3),String.valueOf(c.getLong(4)),c.getString(5),String.valueOf(c.getLong(6))});}return out;}
    synchronized String[] file(String id){try(Cursor c=getReadableDatabase().rawQuery("SELECT id,uri,display_name,mime_type,size_bytes,source,added_at FROM files WHERE id=? LIMIT 1",new String[]{safe(id)})){return c.moveToFirst()?new String[]{c.getString(0),c.getString(1),c.getString(2),c.getString(3),String.valueOf(c.getLong(4)),c.getString(5),String.valueOf(c.getLong(6))}:null;}}
    synchronized int fileCount(){return count("files");}

    synchronized void addPlace(String id,String label,double lat,double lon,float accuracy,String provider,long observedAt,long addedAt){ContentValues v=new ContentValues();v.put("id",id);v.put("label",label);v.put("latitude",lat);v.put("longitude",lon);v.put("accuracy",accuracy);v.put("provider",provider);v.put("observed_at",observedAt);v.put("added_at",addedAt);getWritableDatabase().insertOrThrow("places",null,v);}
    synchronized List<String[]> places(int limit){ArrayList<String[]> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT id,label,latitude,longitude,accuracy,provider,observed_at,added_at FROM places ORDER BY observed_at DESC LIMIT ?",new String[]{String.valueOf(Math.max(1,limit))})){while(c.moveToNext())out.add(new String[]{c.getString(0),c.getString(1),String.valueOf(c.getDouble(2)),String.valueOf(c.getDouble(3)),String.valueOf(c.getFloat(4)),c.getString(5),String.valueOf(c.getLong(6)),String.valueOf(c.getLong(7))});}return out;}
    synchronized String[] place(String id){try(Cursor c=getReadableDatabase().rawQuery("SELECT id,label,latitude,longitude,accuracy,provider,observed_at,added_at FROM places WHERE id=? LIMIT 1",new String[]{safe(id)})){return c.moveToFirst()?new String[]{c.getString(0),c.getString(1),String.valueOf(c.getDouble(2)),String.valueOf(c.getDouble(3)),String.valueOf(c.getFloat(4)),c.getString(5),String.valueOf(c.getLong(6)),String.valueOf(c.getLong(7))}:null;}}
    synchronized int placeCount(){return count("places");}

    synchronized void addProject(String id,String name,String description,String status,long now){ContentValues v=new ContentValues();v.put("id",id);v.put("name",name);v.put("description",description);v.put("status",status);v.put("created_at",now);v.put("updated_at",now);getWritableDatabase().insertOrThrow("projects",null,v);}
    synchronized List<String[]> projects(int limit){ArrayList<String[]> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT id,name,description,status,created_at,updated_at FROM projects ORDER BY updated_at DESC LIMIT ?",new String[]{String.valueOf(Math.max(1,limit))})){while(c.moveToNext())out.add(new String[]{c.getString(0),c.getString(1),c.getString(2),c.getString(3),String.valueOf(c.getLong(4)),String.valueOf(c.getLong(5))});}return out;}
    synchronized String[] project(String id){try(Cursor c=getReadableDatabase().rawQuery("SELECT id,name,description,status,created_at,updated_at FROM projects WHERE id=? LIMIT 1",new String[]{safe(id)})){return c.moveToFirst()?new String[]{c.getString(0),c.getString(1),c.getString(2),c.getString(3),String.valueOf(c.getLong(4)),String.valueOf(c.getLong(5))}:null;}}
    synchronized int projectCount(){return count("projects");}
    synchronized void updateProjectStatus(String id,String status){long now=System.currentTimeMillis();getWritableDatabase().execSQL("UPDATE projects SET status=?,updated_at=? WHERE id=?",new Object[]{safe(status),now,safe(id)});}

    private int count(String table){try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM "+table,null)){return c.moveToFirst()?c.getInt(0):0;}}
    private static String safe(String x){return x==null?"":x.trim();}
}
