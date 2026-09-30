package com.pie.web.dto;

import java.math.BigDecimal;

public record ConcentrationItemDto(String ticker, BigDecimal marketValue, BigDecimal weightPct) {}
