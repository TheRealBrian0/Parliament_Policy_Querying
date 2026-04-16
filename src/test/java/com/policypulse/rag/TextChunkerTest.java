package com.policypulse.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextChunkerTest {

    private final TextChunker chunker = new TextChunker();

    @Test
    void chunkProducesOverlappingSegments() {
        String text = "a".repeat(1800);
        List<String> chunks = chunker.chunk(text);
        assertTrue(chunks.size() >= 3);
        assertTrue(chunks.get(0).length() <= 700);
        assertFalse(chunks.get(1).isBlank());
    }
}
