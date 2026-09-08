package com.kareem.lifeos;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Durable image + OCR store. The source image record survives every OCR failure or engine upgrade. */
final class ImageOcrStore extends SQLiteOpenHelper {
    private static final String DB="lifeos_image_ocr.db";private static final int VERSION=1;private static volatile ImageOcrStore instance;
    static ImageOcrStore get(Context c){if(instance==null)synchronized(ImageOcrStore.class){if(instance==null)instance=new ImageOcrStore(c.getApplicationContext());}return instance;}
    private ImageOcrStore(Context c){super(c,DB,null,VERSION);}
    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE images(id TEXT PRIMARY KEY,uri TEXT NOT NULL UNIQUE,display_name TEXT NOT NULL,mime_type TEXT NOT NULL,width INTEGER NOT NULL DEFAULT 0,height INTEGER NOT NULL DEFAULT 0,source TEXT NOT NULL,added_at INTEGER NOT NULL,ocr_status TEXT NOT NULL DEFAULT 'not_run',latest_run_id TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE INDEX images_time ON images(added_at DESC)");
        db.execSQL("CREATE TABLE ocr_runs(run_id TEXT PRIMARY KEY,image_id TEXT NOT NULL,status TEXT NOT NULL,engine_summary TEXT NOT NULL,raw_text TEXT NOT NULL,search_text TEXT NOT NULL,languages TEXT NOT NULL,confidence REAL NOT NULL,duration_ms INTEGER NOT NULL,created_at INTEGER NOT NULL,lines_json TEXT NOT NULL,critical_json TEXT NOT NULL,candidates_json TEXT NOT NULL)");
        db.execSQL("CREATE INDEX ocr_runs_image_time ON ocr_runs(image_id,created_at DESC)");
        db.execSQL("CREATE TABLE corrections(id INTEGER PRIMARY KEY AUTOINCREMENT,image_id TEXT NOT NULL,run_id TEXT NOT NULL,original_text TEXT NOT NULL,corrected_text TEXT NOT NULL,created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX corrections_image ON corrections(image_id,created_at DESC)");
        db.execSQL("CREATE TABLE ground_truth(image_id TEXT PRIMARY KEY,text TEXT NOT NULL,created_at INTEGER NOT NULL)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){}

    synchronized void upsertImage(String id,String uri,String name,String mime,int width,int height,String source,long addedAt){ContentValues v=new ContentValues();v.put("id",id);v.put("uri",uri);v.put("display_name",name);v.put("mime_type",mime);v.put("width",width);v.put("height",height);v.put("source",source);v.put("added_at",addedAt);getWritableDatabase().insertWithOnConflict("images",null,v,SQLiteDatabase.CONFLICT_IGNORE);}
    synchronized void setImageStatus(String id,String status){getWritableDatabase().execSQL("UPDATE images SET ocr_status=? WHERE id=?",new Object[]{safe(status),safe(id)});}
    synchronized void saveRun(OcrResult r){if(r==null||r.runId.isEmpty()||r.imageId.isEmpty())return;ContentValues v=new ContentValues();v.put("run_id",r.runId);v.put("image_id",r.imageId);v.put("status",r.status);v.put("engine_summary",r.engineSummary);v.put("raw_text",r.rawText);v.put("search_text",r.searchText);v.put("languages",r.languages);v.put("confidence",r.confidence);v.put("duration_ms",r.durationMs);v.put("created_at",r.createdAt);v.put("lines_json",lines(r.lines).toString());v.put("critical_json",new JSONArray(r.criticalTokens).toString());v.put("candidates_json",new JSONArray(r.candidateSummaries).toString());SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{db.insertWithOnConflict("ocr_runs",null,v,SQLiteDatabase.CONFLICT_REPLACE);db.execSQL("UPDATE images SET ocr_status=?,latest_run_id=? WHERE id=?",new Object[]{r.status,r.runId,r.imageId});db.setTransactionSuccessful();}finally{db.endTransaction();}}
    synchronized List<String[]> images(int limit){ArrayList<String[]> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT id,uri,display_name,mime_type,width,height,source,added_at,ocr_status,latest_run_id FROM images ORDER BY added_at DESC LIMIT ?",new String[]{String.valueOf(Math.max(1,limit))})){while(c.moveToNext())out.add(row(c));}return out;}
    synchronized String[] image(String id){try(Cursor c=getReadableDatabase().rawQuery("SELECT id,uri,display_name,mime_type,width,height,source,added_at,ocr_status,latest_run_id FROM images WHERE id=? LIMIT 1",new String[]{safe(id)})){return c.moveToFirst()?row(c):null;}}
    synchronized int imageCount(){try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM images",null)){return c.moveToFirst()?c.getInt(0):0;}}
    synchronized OcrResult latest(String imageId){try(Cursor c=getReadableDatabase().rawQuery("SELECT run_id,image_id,status,engine_summary,raw_text,search_text,languages,confidence,duration_ms,created_at,lines_json,critical_json,candidates_json FROM ocr_runs WHERE image_id=? ORDER BY created_at DESC LIMIT 1",new String[]{safe(imageId)})){return c.moveToFirst()?readRun(c):null;}}
    synchronized void recordCorrection(String imageId,String runId,String original,String corrected){ContentValues v=new ContentValues();v.put("image_id",safe(imageId));v.put("run_id",safe(runId));v.put("original_text",safe(original));v.put("corrected_text",safe(corrected));v.put("created_at",System.currentTimeMillis());getWritableDatabase().insert("corrections",null,v);}
    synchronized void setGroundTruth(String imageId,String text){ContentValues v=new ContentValues();v.put("image_id",safe(imageId));v.put("text",text==null?"":text);v.put("created_at",System.currentTimeMillis());getWritableDatabase().insertWithOnConflict("ground_truth",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    synchronized String groundTruth(String imageId){try(Cursor c=getReadableDatabase().rawQuery("SELECT text FROM ground_truth WHERE image_id=? LIMIT 1",new String[]{safe(imageId)})){return c.moveToFirst()?c.getString(0):"";}}
    synchronized List<String[]> scoredSamples(){ArrayList<String[]> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT g.image_id,g.text,r.raw_text,r.engine_summary FROM ground_truth g JOIN ocr_runs r ON r.run_id=(SELECT r2.run_id FROM ocr_runs r2 WHERE r2.image_id=g.image_id ORDER BY r2.created_at DESC LIMIT 1) WHERE LENGTH(TRIM(g.text))>0",null)){while(c.moveToNext())out.add(new String[]{c.getString(0),c.getString(1),c.getString(2),c.getString(3)});}return out;}
    synchronized void eraseAll(){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{db.delete("corrections",null,null);db.delete("ground_truth",null,null);db.delete("ocr_runs",null,null);db.delete("images",null,null);db.setTransactionSuccessful();}finally{db.endTransaction();}}

    private static String[] row(Cursor c){return new String[]{c.getString(0),c.getString(1),c.getString(2),c.getString(3),String.valueOf(c.getInt(4)),String.valueOf(c.getInt(5)),c.getString(6),String.valueOf(c.getLong(7)),c.getString(8),c.getString(9)};}
    private static JSONArray lines(List<OcrResult.Line> xs){JSONArray a=new JSONArray();try{for(OcrResult.Line x:xs){JSONObject o=new JSONObject();o.put("text",x.text);o.put("script",x.script);o.put("engine",x.engine);o.put("left",x.left);o.put("top",x.top);o.put("right",x.right);o.put("bottom",x.bottom);o.put("confidence",x.confidence);a.put(o);}}catch(Exception ignored){}return a;}
    private static OcrResult readRun(Cursor c){ArrayList<OcrResult.Line> lines=new ArrayList<>();ArrayList<String> critical=new ArrayList<>(),candidates=new ArrayList<>();try{JSONArray a=new JSONArray(c.getString(10));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);lines.add(new OcrResult.Line(o.optString("text"),o.optString("script"),o.optString("engine"),o.optInt("left"),o.optInt("top"),o.optInt("right"),o.optInt("bottom"),(float)o.optDouble("confidence",0)));}JSONArray b=new JSONArray(c.getString(11));for(int i=0;i<b.length();i++)critical.add(b.optString(i));JSONArray d=new JSONArray(c.getString(12));for(int i=0;i<d.length();i++)candidates.add(d.optString(i));}catch(Exception ignored){}return new OcrResult(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getString(6),c.getFloat(7),c.getLong(8),c.getLong(9),lines,critical,candidates);}
    private static String safe(String x){return x==null?"":x.trim();}
}
