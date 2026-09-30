package com.pie.web.dto;

import java.math.BigDecimal;

public record AllocationBucketDto(String assetClass, BigDecimal marketValue, BigDecimal weightPct) {}
