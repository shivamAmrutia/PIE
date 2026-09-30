package com.pie.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class Document {

  @Id
  private UUID id;

  @Column(name = "client_id", nullable = false)
  private UUID clientId;

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(nullable = false)
  private String filename;

  @Column(name = "content_type", nullable = false)
  private String contentType;

  @Enumerated(EnumType.STRING)
  @Column(name = "doc_type", nullable = false)
  private DocumentType docType;

  @Column(name = "storage_path", nullable = false)
  private String storagePath;

  @Column(name = "page_count")
  private Integer pageCount;

  @Column(name = "as_of_date")
  private LocalDate asOfDate;

  @Column(name = "uploaded_by")
  private UUID uploadedBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected Document() {}

  public Document(
      UUID id,
      UUID clientId,
      UUID tenantId,
      String filename,
      String contentType,
      DocumentType docType,
      String storagePath,
      UUID uploadedBy) {
    this.id = id;
    this.clientId = clientId;
    this.tenantId = tenantId;
    this.filename = filename;
    this.contentType = contentType;
    this.docType = docType;
    this.storagePath = storagePath;
    this.uploadedBy = uploadedBy;
    this.createdAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getClientId() {
    return clientId;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public String getFilename() {
    return filename;
  }

  public String getContentType() {
    return contentType;
  }

  public DocumentType getDocType() {
    return docType;
  }

  public String getStoragePath() {
    return storagePath;
  }

  public Integer getPageCount() {
    return pageCount;
  }

  public LocalDate getAsOfDate() {
    return asOfDate;
  }

  public UUID getUploadedBy() {
    return uploadedBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setDocType(DocumentType docType) {
    this.docType = docType;
  }

  public void setPageCount(Integer pageCount) {
    this.pageCount = pageCount;
  }

  public void setAsOfDate(LocalDate asOfDate) {
    this.asOfDate = asOfDate;
  }
}
