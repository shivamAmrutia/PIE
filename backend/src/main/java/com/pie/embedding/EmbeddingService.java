package com.pie.embedding;

public interface EmbeddingService {
  float[] embed(String text);

  int dimensions();
}
