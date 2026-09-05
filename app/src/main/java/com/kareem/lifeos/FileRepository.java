package com.kareem.lifeos;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Real file capability containing documents the user explicitly connected through Android SAF. */
final class FileRepository {
    static final class FileObject {final String id,uri,displayName,mimeType,source;final long sizeBytes,addedAt;FileObject(String id,String uri,String displayName,String mimeType,long sizeBytes,String source,long addedAt){this.id=id;this.uri=s(uri);this.displayName=s(displayName);this.mimeType=s(mimeType);this.sizeBytes=sizeBytes;this.source=s(source);this.addedAt=addedAt;}}
    private FileRepository(){}
    static FileObject importUri(Context c,Uri uri){if(uri==null)return null;String name="Document",mime=s(c.getContentResolver().getType(uri));long size=-1;Cursor cur=null;try{cur=c.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE},null,null,null);if(cur!=null&&cur.moveToFirst()){String n=cur.getString(0);if(n!=null&&!n.trim().isEmpty())name=n.trim();if(!cur.isNull(1))size=cur.getLong(1);}}catch(Exception ignored){}finally{if(cur!=null)cur.close();}String raw=uri.toString(),id="file:"+sha(raw).substring(0,24);long now=System.currentTimeMillis();UserObjectStore.get(c).upsertFile(id,raw,name,mime,size,"Android document picker",now);return load(c,id);}
    static List<FileObject> list(Context c,int limit){ArrayList<FileObject> out=new ArrayList<>();for(String[] x:UserObjectStore.get(c).files(limit))out.add(read(x));return out;}
    static FileObject load(Context c,String id){String[] x=UserObjectStore.get(c).file(id);return x==null?null:read(x);}
    static int count(Context c){return UserObjectStore.get(c).fileCount();}
    private static FileObject read(String[] x){return new FileObject(x[0],x[1],x[2],x[3],longv(x[4],-1),x[5],longv(x[6],0));}
    private static long longv(String x,long d){try{return Long.parseLong(x);}catch(Exception e){return d;}}
    private static String sha(String value){try{byte[] b=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte x:b)s.append(String.format(Locale.US,"%02x",x));return s.toString();}catch(Exception e){return Integer.toHexString(value.hashCode())+"000000000000000000000000";}}
    private static String s(String x){return x==null?"":x.trim();}
}
