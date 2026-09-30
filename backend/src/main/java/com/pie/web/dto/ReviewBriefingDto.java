package com.pie.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ReviewBriefingDto(
    UUID clientId,
    String clientName,
    LocalDate asOfDate,
    BigDecimal portfolioValue,
    BigDecimal periodReturnPct,
    String benchmarkNote,
    List<ReviewItemDto> reviewItems,
    List<ChangeItemDto> recentChanges,
    List<String> goals,
    List<String> recentDiscussions,
    List<DocumentDto> keyDocuments,
    String disclaimer,
    String claimType) {}
