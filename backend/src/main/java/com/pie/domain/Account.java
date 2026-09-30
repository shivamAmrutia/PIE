package com.pie.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {

  @Id
  private UUID id;

  @Column(name = "client_id", nullable = false)
  private UUID clientId;

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(nullable = false)
  private String name;

  @Column(name = "account_type", nullable = false)
  private String accountType;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected Account() {}

  public Account(UUID id, UUID clientId, UUID tenantId, String name, String accountType) {
    this.id = id;
    this.clientId = clientId;
    this.tenantId = tenantId;
    this.name = name;
    this.accountType = accountType;
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

  public String getName() {
    return name;
  }

  public String getAccountType() {
    return accountType;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
