package com.pie.ingestion;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TextChunker {

  private static final int TARGET_CHARS = 900;
  private static final int OVERLAP_CHARS = 120;

  public List<String> chunk(String text) {
    if (text == null || text.isBlank()) {
      return List.of();
    }
    String normalized = text.replace("\r\n", "\n").trim();
    List<String> chunks = new ArrayList<>();
    int start = 0;
    while (start < normalized.length()) {
      int end = Math.min(normalized.length(), start + TARGET_CHARS);
      if (end < normalized.length()) {
        int breakAt = normalized.lastIndexOf('\n', end);
        if (breakAt > start + TARGET_CHARS / 2) {
          end = breakAt;
        }
      }
      String piece = normalized.substring(start, end).trim();
      if (!piece.isEmpty()) {
        chunks.add(piece);
      }
      if (end >= normalized.length()) {
        break;
      }
      start = Math.max(end - OVERLAP_CHARS, start + 1);
    }
    return chunks;
  }
}
