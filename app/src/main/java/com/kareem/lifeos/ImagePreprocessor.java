package com.kareem.lifeos;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import androidx.exifinterface.media.ExifInterface;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** Deterministic OCR-oriented preprocessing. Raw source bytes are never modified. */
final class ImagePreprocessor {
    static final class Variant {final String name;final Bitmap bitmap;Variant(String name,Bitmap bitmap){this.name=name;this.bitmap=bitmap;}}
    private ImagePreprocessor(){}

    static List<Variant> variants(Context c,Uri uri)throws Exception{Bitmap base=decode(c,uri,2600);if(base==null)throw new IllegalArgumentException("Image could not be decoded");base=orient(c,uri,base);ArrayList<Variant> out=new ArrayList<>();out.add(new Variant("original",base));
        Bitmap working=base;int max=Math.max(base.getWidth(),base.getHeight());if(max>0&&max<1700){float scale=1700f/max;working=Bitmap.createScaledBitmap(base,Math.max(1,Math.round(base.getWidth()*scale)),Math.max(1,Math.round(base.getHeight()*scale)),true);out.add(new Variant("upscaled",working));}
        Bitmap gray=contrastGray(working,1.35f);out.add(new Variant("gray-contrast",gray));out.add(new Variant("otsu",otsu(gray)));return out;}

    static void recycle(List<Variant> xs){if(xs==null)return;java.util.HashSet<Bitmap> seen=new java.util.HashSet<>();for(Variant v:xs)if(v!=null&&v.bitmap!=null&&seen.add(v.bitmap)&&!v.bitmap.isRecycled())try{v.bitmap.recycle();}catch(Exception ignored){}}

    private static Bitmap decode(Context c,Uri uri,int maxDim)throws Exception{BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;try(InputStream in=c.getContentResolver().openInputStream(uri)){BitmapFactory.decodeStream(in,null,bounds);}int sample=1;while(bounds.outWidth/sample>maxDim||bounds.outHeight/sample>maxDim)sample*=2;BitmapFactory.Options o=new BitmapFactory.Options();o.inSampleSize=Math.max(1,sample);o.inPreferredConfig=Bitmap.Config.ARGB_8888;try(InputStream in=c.getContentResolver().openInputStream(uri)){return BitmapFactory.decodeStream(in,null,o);}}
    private static Bitmap orient(Context c,Uri uri,Bitmap b){try(InputStream in=c.getContentResolver().openInputStream(uri)){ExifInterface exif=new ExifInterface(in);int o=exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);float deg=0;if(o==ExifInterface.ORIENTATION_ROTATE_90)deg=90;else if(o==ExifInterface.ORIENTATION_ROTATE_180)deg=180;else if(o==ExifInterface.ORIENTATION_ROTATE_270)deg=270;if(deg==0)return b;Matrix m=new Matrix();m.postRotate(deg);Bitmap r=Bitmap.createBitmap(b,0,0,b.getWidth(),b.getHeight(),m,true);if(r!=b)b.recycle();return r;}catch(Exception e){return b;}}
    private static Bitmap contrastGray(Bitmap src,float contrast){int w=src.getWidth(),h=src.getHeight();Bitmap out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);int[] px=new int[w*h];src.getPixels(px,0,w,0,0,w,h);for(int i=0;i<px.length;i++){int p=px[i];int y=Math.round(.299f*Color.red(p)+.587f*Color.green(p)+.114f*Color.blue(p));y=Math.round((y-128)*contrast+128);y=Math.max(0,Math.min(255,y));px[i]=Color.rgb(y,y,y);}out.setPixels(px,0,w,0,0,w,h);return out;}
    private static Bitmap otsu(Bitmap gray){int w=gray.getWidth(),h=gray.getHeight(),n=w*h;int[] px=new int[n],hist=new int[256];gray.getPixels(px,0,w,0,0,w,h);for(int p:px)hist[Color.red(p)]++;long sum=0;for(int i=0;i<256;i++)sum+=(long)i*hist[i];long sumB=0;int wB=0,threshold=127;double best=-1;for(int t=0;t<256;t++){wB+=hist[t];if(wB==0)continue;int wF=n-wB;if(wF==0)break;sumB+=(long)t*hist[t];double mB=(double)sumB/wB,mF=(double)(sum-sumB)/wF;double between=(double)wB*wF*(mB-mF)*(mB-mF);if(between>best){best=between;threshold=t;}}Bitmap out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);for(int i=0;i<n;i++){int v=Color.red(px[i])>threshold?255:0;px[i]=Color.rgb(v,v,v);}out.setPixels(px,0,w,0,0,w,h);return out;}
}
