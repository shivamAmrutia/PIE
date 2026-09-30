package com.pie.web.dto;

import java.util.List;

public record ClaimDto(String text, String claimType, List<String> evidenceRefs) {}
