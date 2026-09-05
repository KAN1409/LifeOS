package com.kareem.lifeos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Verbatim transcription result. Semantic interpretation is a separate downstream layer. */
final class VoiceTranscript {
    static final class Segment {final long startMs,endMs;final String text;final float confidence;Segment(long startMs,long endMs,String text,float confidence){this.startMs=Math.max(0,startMs);this.endMs=Math.max(this.startMs,endMs);this.text=s(text);this.confidence=Math.max(0,Math.min(1,confidence));}}
    final String text,language,engine,version;final long durationMs;final List<Segment> segments;
    VoiceTranscript(String text,String language,String engine,String version,long durationMs,List<Segment> segments){this.text=s(text);this.language=s(language);this.engine=s(engine);this.version=s(version);this.durationMs=Math.max(0,durationMs);this.segments=Collections.unmodifiableList(segments==null?new ArrayList<>():new ArrayList<>(segments));}
    private static String s(String x){return x==null?"":x.trim();}
}
