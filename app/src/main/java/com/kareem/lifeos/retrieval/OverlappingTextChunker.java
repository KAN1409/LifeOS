package com.kareem.lifeos.retrieval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Word-based overlapping chunker port/adaptation from
 * openintelligence-labs/secondbrain chunking.py (MIT).
 */
public final class OverlappingTextChunker {
    private OverlappingTextChunker() {}

    public static final class Chunk {
        public final int index;
        public final String text;
        public final int startWord;
        public final int endWord;

        Chunk(int index, String text, int startWord, int endWord) {
            this.index=index; this.text=text; this.startWord=startWord; this.endWord=endWord;
        }
    }

    public static List<Chunk> chunk(String text) { return chunk(text, 400, 50); }

    public static List<Chunk> chunk(String text, int maxWords, int overlapWords) {
        if (maxWords <= 0) throw new IllegalArgumentException("maxWords must be positive");
        if (overlapWords < 0 || overlapWords >= maxWords)
            throw new IllegalArgumentException("overlapWords must be >= 0 and less than maxWords");
        if (text == null || text.trim().isEmpty()) return Collections.emptyList();

        String[] words = text.trim().split("\\s+");
        int stride = maxWords - overlapWords;
        List<Chunk> chunks = new ArrayList<Chunk>();
        int index = 0;
        for (int start=0; start<words.length; start += stride) {
            int end = Math.min(start + maxWords, words.length);
            StringBuilder body = new StringBuilder();
            for (int i=start;i<end;i++) {
                if (body.length()>0) body.append(' ');
                body.append(words[i]);
            }
            chunks.add(new Chunk(index++, body.toString(), start, end));
            if (end == words.length) break;
        }
        return chunks;
    }
}
