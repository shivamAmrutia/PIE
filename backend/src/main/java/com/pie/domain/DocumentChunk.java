package com.pie.domain;

import java.time.Instant;
import java.util.UUID;

public class DocumentChunk {

  private final UUID id;
  private final UUID documentId;
  private final UUID clientId;
  private final UUID tenantId;
  private final int chunkIndex;
  private final Integer pageNumber;
  private final String content;
  private final Instant createdAt;

  public DocumentChunk(
      UUID id,
      UUID documentId,
      UUID clientId,
      UUID tenantId,
      int chunkIndex,
      Integer pageNumber,
      String content,
      Instant createdAt) {
    this.id = id;
    this.documentId = documentId;
    this.clientId = clientId;
    this.tenantId = tenantId;
    this.chunkIndex = chunkIndex;
    this.pageNumber = pageNumber;
    this.content = content;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getDocumentId() {
    return documentId;
  }

  public UUID getClientId() {
    return clientId;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public int getChunkIndex() {
    return chunkIndex;
  }

  public Integer getPageNumber() {
    return pageNumber;
  }

  public String getContent() {
    return content;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
