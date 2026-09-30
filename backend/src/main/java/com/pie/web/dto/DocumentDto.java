package com.pie.web.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DocumentDto(
    UUID id,
    String filename,
    String docType,
    String contentType,
    LocalDate asOfDate,
    Instant createdAt) {}
