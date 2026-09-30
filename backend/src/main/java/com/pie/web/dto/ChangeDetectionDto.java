package com.pie.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ChangeDetectionDto(
    UUID clientId,
    LocalDate fromDate,
    LocalDate toDate,
    BigDecimal fromTotal,
    BigDecimal toTotal,
    BigDecimal totalChangePct,
    List<ChangeItemDto> materialChanges,
    List<ChangeItemDto> routineChanges,
    List<ChangeItemDto> documentChanges,
    String claimType,
    String disclaimer) {}
