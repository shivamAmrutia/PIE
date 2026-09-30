package com.pie.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "client_access")
public class ClientAccess {

  @Id
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "client_id", nullable = false)
  private UUID clientId;

  @Enumerated(EnumType.STRING)
  @Column(name = "access_level", nullable = false)
  private AccessLevel accessLevel;

  @Column(name = "granted_at", nullable = false)
  private Instant grantedAt;

  protected ClientAccess() {}

  public ClientAccess(UUID id, UUID userId, UUID clientId, AccessLevel accessLevel) {
    this.id = id;
    this.userId = userId;
    this.clientId = clientId;
    this.accessLevel = accessLevel;
    this.grantedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public UUID getClientId() {
    return clientId;
  }

  public AccessLevel getAccessLevel() {
    return accessLevel;
  }

  public Instant getGrantedAt() {
    return grantedAt;
  }
}
