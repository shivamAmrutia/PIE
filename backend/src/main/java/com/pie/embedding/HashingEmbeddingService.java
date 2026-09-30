package com.pie.embedding;

import com.pie.config.PieProperties;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Deterministic local embedding for demo/offline use. Swap for a model-backed
 * implementation later without changing callers.
 */
@Service
public class HashingEmbeddingService implements EmbeddingService {

  private final int dimensions;

  public HashingEmbeddingService(PieProperties properties) {
    this.dimensions = properties.embedding().dimensions();
  }

  @Override
  public float[] embed(String text) {
    float[] vector = new float[dimensions];
    if (text == null || text.isBlank()) {
      return vector;
    }
    String[] tokens = text.toLowerCase(Locale.ROOT).split("[^a-z0-9$%./]+");
    for (String token : tokens) {
      if (token.isBlank()) {
        continue;
      }
      int h1 = murmur(token.getBytes(StandardCharsets.UTF_8), 0x9747b28c);
      int h2 = murmur(token.getBytes(StandardCharsets.UTF_8), 0x5bd1e995);
      int i1 = Math.floorMod(h1, dimensions);
      int i2 = Math.floorMod(h2, dimensions);
      vector[i1] += 1.0f;
      vector[i2] += 0.5f;
    }
    normalize(vector);
    return vector;
  }

  @Override
  public int dimensions() {
    return dimensions;
  }

  private static void normalize(float[] v) {
    double sum = 0;
    for (float x : v) {
      sum += x * x;
    }
    if (sum == 0) {
      return;
    }
    float inv = (float) (1.0 / Math.sqrt(sum));
    for (int i = 0; i < v.length; i++) {
      v[i] *= inv;
    }
  }

  private static int murmur(byte[] data, int seed) {
    int length = data.length;
    int h = seed ^ length;
    int i = 0;
    while (length >= 4) {
      int k =
          (data[i] & 0xff)
              | ((data[i + 1] & 0xff) << 8)
              | ((data[i + 2] & 0xff) << 16)
              | ((data[i + 3] & 0xff) << 24);
      k *= 0x5bd1e995;
      k ^= k >>> 24;
      k *= 0x5bd1e995;
      h *= 0x5bd1e995;
      h ^= k;
      i += 4;
      length -= 4;
    }
    switch (length) {
      case 3:
        h ^= (data[i + 2] & 0xff) << 16;
      case 2:
        h ^= (data[i + 1] & 0xff) << 8;
      case 1:
        h ^= (data[i] & 0xff);
        h *= 0x5bd1e995;
    }
    h ^= h >>> 13;
    h *= 0x5bd1e995;
    h ^= h >>> 15;
    return h;
  }
}
