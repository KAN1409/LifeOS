package com.kareem.lifeos;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Hybrid local OCR coordinator: preprocessing -> multi-pass Latin/Arabic -> confidence/consensus -> durable result. */
final class ImageOcrEngine {
    interface Callback {void ok(OcrResult result);void fail(Exception error);}
    private static final ExecutorService EXEC=Executors.newSingleThreadExecutor();private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private ImageOcrEngine(){}

    static void analyze(Context context,ImageRepository.ImageObject image,Callback cb){Context app=context.getApplicationContext();ImageOcrStore.get(app).setImageStatus(image.id,"processing");EXEC.execute(()->{long started=System.currentTimeMillis();List<ImagePreprocessor.Variant> variants=null;try{variants=ImagePreprocessor.variants(app,Uri.parse(image.uri));List<LatinOcrEngine.Candidate> latin=new ArrayList<>();List<ArabicOcrEngine.Candidate> arabic=new ArrayList<>();String latinError="",arabicError="";try{latin=LatinOcrEngine.run(variants);}catch(Exception e){latinError=e.getClass().getSimpleName();}try{arabic=ArabicOcrEngine.run(app,variants);}catch(Exception e){arabicError=e.getClass().getSimpleName();}
            LatinOcrEngine.Candidate l=LatinOcrEngine.best(latin);ArabicOcrEngine.Candidate a=ArabicOcrEngine.best(arabic);String lt=l==null?"":l.text,at=a==null?"":a.text;String merged=OcrQuality.mergeDistinct(lt,at);float lc=l==null?0:consensusLatin(l,latin),ac=a==null?0:consensusArabic(a,arabic);float confidence=combineConfidence(lt,at,lc,ac);String status=merged.isEmpty()?"failed":"complete";String engines="ML Kit Latin"+(a!=null&&!a.text.isEmpty()?" + Tesseract Best Arabic":"");ArrayList<OcrResult.Line> lines=new ArrayList<>();if(l!=null)lines.addAll(l.lines);if(a!=null&&!a.text.isEmpty())lines.add(new OcrResult.Line(a.text,"Arabic","TesseractBest/"+a.variant,0,0,0,0,ac));ArrayList<String> candidates=new ArrayList<>();for(LatinOcrEngine.Candidate x:latin)candidates.add("MLKit/"+x.variant+" score="+fmt(x.score)+" text="+clip(x.text,420));for(ArabicOcrEngine.Candidate x:arabic)candidates.add("TesseractBest/"+x.variant+" score="+fmt(x.score)+" status="+x.status+" text="+clip(x.text,420));if(!latinError.isEmpty())candidates.add("MLKit error="+latinError);if(!arabicError.isEmpty())candidates.add("Arabic error="+arabicError);long now=System.currentTimeMillis();OcrResult result=new OcrResult("ocr:"+UUID.randomUUID(),image.id,status,engines,merged,OcrQuality.normalizeForSearch(merged),OcrQuality.scripts(merged),confidence,now-started,now,lines,OcrQuality.criticalTokens(merged),candidates);ImageOcrStore.get(app).saveRun(result);postOk(cb,result);
        }catch(Exception e){ImageOcrStore.get(app).setImageStatus(image.id,"failed");postFail(cb,e);}finally{ImagePreprocessor.recycle(variants);}});}

    private static float consensusLatin(LatinOcrEngine.Candidate best,List<LatinOcrEngine.Candidate> xs){float agreement=0;int n=0;for(LatinOcrEngine.Candidate x:xs)if(x!=best&&!x.text.isEmpty()){agreement+=agreement(best.text,x.text);n++;}return clamp(best.score*.78f+(n==0?.65f:agreement/n)*.22f);}
    private static float consensusArabic(ArabicOcrEngine.Candidate best,List<ArabicOcrEngine.Candidate> xs){float agreement=0;int n=0;for(ArabicOcrEngine.Candidate x:xs)if(x!=best&&!x.text.isEmpty()){agreement+=agreement(best.text,x.text);n++;}return clamp(best.score*.72f+(n==0?.55f:agreement/n)*.28f);}
    private static float combineConfidence(String latin,String arabic,float lc,float ac){if(latin.isEmpty()&&arabic.isEmpty())return 0;if(latin.isEmpty())return ac;if(arabic.isEmpty())return lc;float cross=agreement(latin,arabic);return clamp(Math.max(lc,ac)*.62f+Math.min(lc,ac)*.23f+cross*.15f);}
    private static float agreement(String a,String b){Set<String>x=words(a),y=words(b);if(x.isEmpty()||y.isEmpty())return 0;int same=0;for(String s:x)if(y.contains(s))same++;int union=x.size()+y.size()-same;return union==0?0:(float)same/union;}
    private static Set<String> words(String text){HashSet<String> out=new HashSet<>();for(String s:OcrQuality.normalizeForSearch(text).split(" "))if(s.length()>1)out.add(s);return out;}
    private static float clamp(float x){return Math.max(0,Math.min(1,x));}private static String fmt(float x){return String.format(Locale.US,"%.2f",x);}private static String clip(String x,int n){String s=x==null?"":x.replace('\n',' ').trim();return s.length()<=n?s:s.substring(0,n)+"…";}
    private static void postOk(Callback cb,OcrResult r){if(cb!=null)MAIN.post(()->cb.ok(r));}private static void postFail(Callback cb,Exception e){if(cb!=null)MAIN.post(()->cb.fail(e));}
}
