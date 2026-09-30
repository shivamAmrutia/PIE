package com.pie.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "clients")
public class Client {

  @Id
  private UUID id;

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(name = "display_name", nullable = false)
  private String displayName;

  @Column(name = "external_ref")
  private String externalRef;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Client() {}

  public Client(UUID id, UUID tenantId, String displayName, String externalRef) {
    this.id = id;
    this.tenantId = tenantId;
    this.displayName = displayName;
    this.externalRef = externalRef;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getExternalRef() {
    return externalRef;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void touch() {
    this.updatedAt = Instant.now();
  }
}
