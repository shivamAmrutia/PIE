package com.pie.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "holdings")
public class Holding {

  @Id
  private UUID id;

  @Column(name = "client_id", nullable = false)
  private UUID clientId;

  @Column(name = "account_id")
  private UUID accountId;

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(name = "account_name", nullable = false)
  private String accountName;

  @Column(nullable = false)
  private String ticker;

  @Column(name = "security_name")
  private String securityName;

  @Column(nullable = false)
  private BigDecimal quantity;

  @Column(nullable = false)
  private BigDecimal price;

  @Column(name = "market_value", nullable = false)
  private BigDecimal marketValue;

  @Column(name = "asset_class")
  private String assetClass;

  @Column(name = "as_of_date", nullable = false)
  private LocalDate asOfDate;

  @Column(name = "source_document_id")
  private UUID sourceDocumentId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected Holding() {}

  public Holding(
      UUID id,
      UUID clientId,
      UUID tenantId,
      String accountName,
      String ticker,
      String securityName,
      BigDecimal quantity,
      BigDecimal price,
      BigDecimal marketValue,
      String assetClass,
      LocalDate asOfDate,
      UUID sourceDocumentId) {
    this.id = id;
    this.clientId = clientId;
    this.tenantId = tenantId;
    this.accountName = accountName;
    this.ticker = ticker;
    this.securityName = securityName;
    this.quantity = quantity;
    this.price = price;
    this.marketValue = marketValue;
    this.assetClass = assetClass;
    this.asOfDate = asOfDate;
    this.sourceDocumentId = sourceDocumentId;
    this.createdAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getClientId() {
    return clientId;
  }

  public UUID getAccountId() {
    return accountId;
  }

  public void setAccountId(UUID accountId) {
    this.accountId = accountId;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public String getAccountName() {
    return accountName;
  }

  public String getTicker() {
    return ticker;
  }

  public String getSecurityName() {
    return securityName;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public BigDecimal getMarketValue() {
    return marketValue;
  }

  public String getAssetClass() {
    return assetClass;
  }

  public LocalDate getAsOfDate() {
    return asOfDate;
  }

  public UUID getSourceDocumentId() {
    return sourceDocumentId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
