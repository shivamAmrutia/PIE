package com.pie.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pie")
public record PieProperties(
    Jwt jwt,
    Storage storage,
    Cors cors,
    Neo4j neo4j,
    Embedding embedding,
    Ask ask) {

  public record Jwt(String secret, long expirationMs) {}

  public record Storage(String uploadDir) {}

  public record Cors(String allowedOrigins) {}

  public record Neo4j(String uri, String username, String password) {}

  public record Embedding(int dimensions) {}

  public record Ask(int topK, String disclaimer) {}
}
