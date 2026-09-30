package com.pie.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

  @Id
  private UUID id;

  @Column(name = "tenant_id")
  private UUID tenantId;

  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "client_id")
  private UUID clientId;

  @Column(nullable = false)
  private String action;

  @Column(name = "resource_type")
  private String resourceType;

  @Column(name = "resource_id")
  private String resourceId;

  @Column(columnDefinition = "TEXT")
  private String detail;

  @Column(name = "ip_address")
  private String ipAddress;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected AuditEvent() {}

  public AuditEvent(
      UUID id,
      UUID tenantId,
      UUID userId,
      UUID clientId,
      String action,
      String resourceType,
      String resourceId,
      String detail,
      String ipAddress) {
    this.id = id;
    this.tenantId = tenantId;
    this.userId = userId;
    this.clientId = clientId;
    this.action = action;
    this.resourceType = resourceType;
    this.resourceId = resourceId;
    this.detail = detail;
    this.ipAddress = ipAddress;
    this.createdAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public UUID getUserId() {
    return userId;
  }

  public UUID getClientId() {
    return clientId;
  }

  public String getAction() {
    return action;
  }

  public String getResourceType() {
    return resourceType;
  }

  public String getResourceId() {
    return resourceId;
  }

  public String getDetail() {
    return detail;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
