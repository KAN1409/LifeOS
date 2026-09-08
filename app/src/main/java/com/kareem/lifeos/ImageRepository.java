package com.kareem.lifeos;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.OpenableColumns;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Explicit image memories selected/shared by the user. OCR is derived and may be rerun independently. */
final class ImageRepository {
    static final class ImageObject {
        final String id,uri,displayName,mimeType,source,ocrStatus,latestRunId;final int width,height;final long addedAt;
        ImageObject(String id,String uri,String name,String mime,int width,int height,String source,long addedAt,String status,String runId){this.id=s(id);this.uri=s(uri);this.displayName=s(name);this.mimeType=s(mime);this.width=width;this.height=height;this.source=s(source);this.addedAt=addedAt;this.ocrStatus=s(status);this.latestRunId=s(runId);}
        OcrResult latest(Context c){return ImageOcrStore.get(c).latest(id);}
    }
    private ImageRepository(){}

    static ImageObject importUri(Context c,Uri uri,String source){if(uri==null)return null;try{c.getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
        String name="Image",mime=s(c.getContentResolver().getType(uri));long now=System.currentTimeMillis();Cursor cur=null;try{cur=c.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null);if(cur!=null&&cur.moveToFirst()){String n=cur.getString(0);if(n!=null&&!n.trim().isEmpty())name=n.trim();}}catch(Exception ignored){}finally{if(cur!=null)cur.close();}
        int[] d=dimensions(c,uri);String raw=uri.toString(),id="image:"+sha(raw).substring(0,24);ImageOcrStore.get(c).upsertImage(id,raw,name,mime,d[0],d[1],source==null?"Android image picker":source,now);return load(c,id);}
    static List<ImageObject> list(Context c,int limit){ArrayList<ImageObject> out=new ArrayList<>();for(String[] x:ImageOcrStore.get(c).images(limit))out.add(read(x));return out;}
    static ImageObject load(Context c,String id){String[] x=ImageOcrStore.get(c).image(id);return x==null?null:read(x);}
    static int count(Context c){return ImageOcrStore.get(c).imageCount();}
    static void analyze(Context c,String imageId,ImageOcrEngine.Callback cb){ImageObject image=load(c,imageId);if(image==null){if(cb!=null)cb.fail(new IllegalArgumentException("Image not found"));return;}ImageOcrEngine.analyze(c,image,cb);}
    static String searchableText(Context c,String imageId){OcrResult r=ImageOcrStore.get(c).latest(imageId);return r==null?"":r.searchText;}

    private static int[] dimensions(Context c,Uri uri){BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;try(InputStream in=c.getContentResolver().openInputStream(uri)){BitmapFactory.decodeStream(in,null,o);}catch(Exception ignored){}return new int[]{Math.max(0,o.outWidth),Math.max(0,o.outHeight)};}
    private static ImageObject read(String[] x){return new ImageObject(x[0],x[1],x[2],x[3],iv(x[4]),iv(x[5]),x[6],lv(x[7]),x[8],x[9]);}
    private static int iv(String x){try{return Integer.parseInt(x);}catch(Exception e){return 0;}}private static long lv(String x){try{return Long.parseLong(x);}catch(Exception e){return 0;}}
    private static String sha(String value){try{byte[] b=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte x:b)s.append(String.format(Locale.US,"%02x",x));return s.toString();}catch(Exception e){return Integer.toHexString(value.hashCode())+"000000000000000000000000";}}
    private static String s(String x){return x==null?"":x.trim();}
}
