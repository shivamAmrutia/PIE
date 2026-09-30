package com.pie.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record HoldingDto(
    UUID id,
    String accountName,
    String ticker,
    String securityName,
    BigDecimal quantity,
    BigDecimal price,
    BigDecimal marketValue,
    String assetClass,
    LocalDate asOfDate) {}
