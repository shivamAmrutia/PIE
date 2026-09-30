package com.pie.web.dto;

import java.util.List;

public record AskResponse(
    String answer,
    List<ClaimDto> claims,
    List<CitationDto> citations,
    List<String> toolsUsed,
    String disclaimer,
    boolean adviceFilterApplied,
    String validationNote,
    String engine) {}
