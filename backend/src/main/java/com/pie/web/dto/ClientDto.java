package com.pie.web.dto;

import java.time.Instant;
import java.util.UUID;

public record ClientDto(
    UUID id, String displayName, String externalRef, Instant createdAt, Instant updatedAt) {}
