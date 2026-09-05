package com.kareem.lifeos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable OCR output. Raw image remains authoritative; OCR is a replaceable derived projection. */
final class OcrResult {
    static final class Line {
        final String text,script,engine;final int left,top,right,bottom;final float confidence;
        Line(String text,String script,String engine,int left,int top,int right,int bottom,float confidence){this.text=s(text);this.script=s(script);this.engine=s(engine);this.left=left;this.top=top;this.right=right;this.bottom=bottom;this.confidence=Math.max(0,Math.min(1,confidence));}
    }
    final String runId,imageId,status,engineSummary,rawText,searchText,languages;
    final float confidence;final long durationMs,createdAt;final List<Line> lines;final List<String> criticalTokens;final List<String> candidateSummaries;
    OcrResult(String runId,String imageId,String status,String engineSummary,String rawText,String searchText,String languages,float confidence,long durationMs,long createdAt,List<Line> lines,List<String> criticalTokens,List<String> candidateSummaries){
        this.runId=s(runId);this.imageId=s(imageId);this.status=s(status);this.engineSummary=s(engineSummary);this.rawText=s(rawText);this.searchText=s(searchText);this.languages=s(languages);this.confidence=Math.max(0,Math.min(1,confidence));this.durationMs=Math.max(0,durationMs);this.createdAt=createdAt;this.lines=Collections.unmodifiableList(lines==null?new ArrayList<>():new ArrayList<>(lines));this.criticalTokens=Collections.unmodifiableList(criticalTokens==null?new ArrayList<>():new ArrayList<>(criticalTokens));this.candidateSummaries=Collections.unmodifiableList(candidateSummaries==null?new ArrayList<>():new ArrayList<>(candidateSummaries));
    }
    boolean success(){return "complete".equals(status)&&!rawText.isEmpty();}
    private static String s(String x){return x==null?"":x.trim();}
}
