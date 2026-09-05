package com.kareem.lifeos;

import android.content.Context;
import java.util.List;

/** Measured OCR quality. Promotion to normal product UI should be based on these metrics, not impressions. */
final class OcrBenchmark {
    static final class Summary {final int samples;final double cer,wer;Summary(int samples,double cer,double wer){this.samples=samples;this.cer=cer;this.wer=wer;}boolean readyForPromotion(){return samples>=30&&cer<=0.08&&wer<=0.18;}}
    private OcrBenchmark(){}
    static Summary summarize(Context c){List<String[]> xs=ImageOcrStore.get(c).scoredSamples();if(xs.isEmpty())return new Summary(0,1,1);double chars=0,charErr=0,words=0,wordErr=0;for(String[] x:xs){String truth=x[1]==null?"":x[1],pred=x[2]==null?"":x[2];String nt=OcrQuality.normalizeForSearch(truth),np=OcrQuality.normalizeForSearch(pred);charErr+=distance(nt,np);chars+=Math.max(1,nt.length());String[] tw=nt.isEmpty()?new String[0]:nt.split(" "),pw=np.isEmpty()?new String[0]:np.split(" ");wordErr+=distance(tw,pw);words+=Math.max(1,tw.length);}return new Summary(xs.size(),Math.min(1,charErr/chars),Math.min(1,wordErr/words));}
    static double cer(String truth,String predicted){String a=OcrQuality.normalizeForSearch(truth),b=OcrQuality.normalizeForSearch(predicted);return (double)distance(a,b)/Math.max(1,a.length());}
    static double wer(String truth,String predicted){String a=OcrQuality.normalizeForSearch(truth),b=OcrQuality.normalizeForSearch(predicted);String[] x=a.isEmpty()?new String[0]:a.split(" "),y=b.isEmpty()?new String[0]:b.split(" ");return (double)distance(x,y)/Math.max(1,x.length);}
    private static int distance(String a,String b){int[] prev=new int[b.length()+1],cur=new int[b.length()+1];for(int j=0;j<=b.length();j++)prev[j]=j;for(int i=1;i<=a.length();i++){cur[0]=i;for(int j=1;j<=b.length();j++)cur[j]=Math.min(Math.min(cur[j-1]+1,prev[j]+1),prev[j-1]+(a.charAt(i-1)==b.charAt(j-1)?0:1));int[] t=prev;prev=cur;cur=t;}return prev[b.length()];}
    private static int distance(String[] a,String[] b){int[] prev=new int[b.length+1],cur=new int[b.length+1];for(int j=0;j<=b.length;j++)prev[j]=j;for(int i=1;i<=a.length;i++){cur[0]=i;for(int j=1;j<=b.length;j++)cur[j]=Math.min(Math.min(cur[j-1]+1,prev[j]+1),prev[j-1]+(a[i-1].equals(b[j-1])?0:1));int[] t=prev;prev=cur;cur=t;}return prev[b.length];}
}
