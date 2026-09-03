package com.kareem.lifeos.retrieval;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class OverlappingTextChunkerTest {
    @Test public void chunksOverlapWithoutLosingWords() {
        List<OverlappingTextChunker.Chunk> chunks = OverlappingTextChunker.chunk(
                "one two three four five six seven eight nine ten", 5, 2);
        assertEquals(3, chunks.size());
        assertEquals("one two three four five", chunks.get(0).text);
        assertEquals("four five six seven eight", chunks.get(1).text);
        assertEquals("seven eight nine ten", chunks.get(2).text);
        assertEquals(3, chunks.get(1).startWord);
        assertEquals(8, chunks.get(1).endWord);
    }

    @Test public void supportsArabicWhitespaceText() {
        List<OverlappingTextChunker.Chunk> chunks = OverlappingTextChunker.chunk(
                "ده نص عربي طويل للاختبار عشان نحافظ على السياق", 4, 1);
        assertTrue(chunks.size() >= 2);
        assertTrue(chunks.get(0).text.contains("عربي"));
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsOverlapEqualToChunkSize() {
        OverlappingTextChunker.chunk("one two", 2, 2);
    }
}
