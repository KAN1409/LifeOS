package com.kareem.lifeos;

import android.content.Context;
import com.googlecode.tesseract.android.TessBaseAPI;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/** Multi-pass Arabic OCR using the higher-accuracy tessdata_best model. Local/offline after first model download. */
final class ArabicOcrEngine {
    static final class Candidate {final String text,variant,status;final float score;Candidate(String text,String variant,String status,float score){this.text=text;this.variant=variant;this.status=status;this.score=score;}}
    private static final String MODEL_URL="https://github.com/tesseract-ocr/tessdata_best/raw/main/ara.traineddata";private static final long MIN_MODEL_BYTES=2_000_000;
    private ArabicOcrEngine(){}

    static List<Candidate> run(Context context,List<ImagePreprocessor.Variant> variants){ArrayList<Candidate> out=new ArrayList<>();File model;try{model=ensureModel(context.getApplicationContext());}catch(Exception e){out.add(new Candidate("","","Arabic model unavailable: "+e.getClass().getSimpleName(),0));return out;}for(ImagePreprocessor.Variant v:variants){TessBaseAPI tess=null;try{tess=new TessBaseAPI();String dataPath=new File(context.getFilesDir(),"tesseract_best").getAbsolutePath();if(!tess.init(dataPath,"ara")){out.add(new Candidate("",v.name,"Arabic OCR init failed",0));continue;}tess.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);tess.setVariable("preserve_interword_spaces","1");tess.setImage(v.bitmap);String text=tess.getUTF8Text();text=text==null?"":text.trim();float score=(float)Math.min(1,OcrQuality.score(text)*.78+arabicDensity(text)*.22);out.add(new Candidate(text,v.name,"ready",score));}catch(Exception e){out.add(new Candidate("",v.name,"Arabic OCR failed: "+e.getClass().getSimpleName(),0));}finally{try{if(tess!=null)tess.recycle();}catch(Exception ignored){}}}return out;}
    static Candidate best(List<Candidate> xs){Candidate best=null;for(Candidate x:xs)if(best==null||x.score>best.score)best=x;return best;}
    private static float arabicDensity(String text){if(text==null||text.isEmpty())return 0;int ar=0,useful=0;for(char c:text.toCharArray()){if(Character.isLetterOrDigit(c))useful++;if(c>='\u0600'&&c<='\u06FF')ar++;}return useful==0?0:(float)ar/useful;}

    private static File ensureModel(Context ctx)throws Exception{File root=new File(ctx.getFilesDir(),"tesseract_best"),dir=new File(root,"tessdata");if(!dir.exists()&&!dir.mkdirs())throw new java.io.IOException("Cannot create tessdata directory");File target=new File(dir,"ara.traineddata");if(target.exists()&&target.length()>=MIN_MODEL_BYTES)return target;File tmp=new File(dir,"ara.traineddata.part");if(tmp.exists())tmp.delete();HttpURLConnection c=(HttpURLConnection)new URL(MODEL_URL).openConnection();c.setConnectTimeout(15_000);c.setReadTimeout(90_000);c.setInstanceFollowRedirects(true);c.setRequestProperty("User-Agent","LifeOS/0.13 Android OCR");int code=c.getResponseCode();if(code<200||code>=300){c.disconnect();throw new java.io.IOException("Model HTTP "+code);}try(InputStream in=new BufferedInputStream(c.getInputStream());BufferedOutputStream out=new BufferedOutputStream(new FileOutputStream(tmp))){byte[] buf=new byte[64*1024];int n;long total=0;while((n=in.read(buf))!=-1){out.write(buf,0,n);total+=n;if(total>60_000_000)throw new java.io.IOException("Unexpected model size");}}finally{c.disconnect();}if(tmp.length()<MIN_MODEL_BYTES){tmp.delete();throw new java.io.IOException("Arabic model incomplete");}if(target.exists()&&!target.delete())throw new java.io.IOException("Cannot replace Arabic model");if(!tmp.renameTo(target)){try(InputStream in=new java.io.FileInputStream(tmp);FileOutputStream out=new FileOutputStream(target)){byte[] b=new byte[64*1024];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}tmp.delete();}return target;}
}
