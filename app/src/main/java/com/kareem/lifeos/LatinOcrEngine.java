package com.kareem.lifeos;

import android.graphics.Rect;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.ArrayList;
import java.util.List;

/** ML Kit Latin OCR multi-pass. Runs only on a background thread. */
final class LatinOcrEngine {
    static final class Candidate {final String text,variant;final float score;final List<OcrResult.Line> lines;Candidate(String text,String variant,float score,List<OcrResult.Line> lines){this.text=text;this.variant=variant;this.score=score;this.lines=lines;}}
    private LatinOcrEngine(){}
    static List<Candidate> run(List<ImagePreprocessor.Variant> variants)throws Exception{ArrayList<Candidate> out=new ArrayList<>();TextRecognizer recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);try{for(ImagePreprocessor.Variant v:variants){if("otsu".equals(v.name))continue;Text result=Tasks.await(recognizer.process(InputImage.fromBitmap(v.bitmap,0)));String text=result.getText()==null?"":result.getText().trim();ArrayList<OcrResult.Line> lines=new ArrayList<>();for(Text.TextBlock block:result.getTextBlocks())for(Text.Line line:block.getLines()){Rect r=line.getBoundingBox();lines.add(new OcrResult.Line(line.getText(),"Latin","MLKit/"+v.name,r==null?0:r.left,r==null?0:r.top,r==null?0:r.right,r==null?0:r.bottom,.82f));}out.add(new Candidate(text,v.name,OcrQuality.score(text),lines));}}finally{recognizer.close();}return out;}
    static Candidate best(List<Candidate> xs){Candidate best=null;for(Candidate x:xs)if(best==null||x.score>best.score)best=x;return best;}
}
