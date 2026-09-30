package com.pie.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PortfolioAnalysisDto(
    UUID clientId,
    LocalDate asOfDate,
    BigDecimal totalMarketValue,
    List<AllocationBucketDto> allocation,
    List<ConcentrationItemDto> concentration,
    String claimType,
    String provenanceNote) {}
