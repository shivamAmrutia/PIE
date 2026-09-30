package com.pie.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ClientCreateRequest(
    @NotBlank String displayName, String externalRef) {}
