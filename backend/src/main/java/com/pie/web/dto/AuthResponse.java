package com.pie.web.dto;

import java.util.UUID;

public record AuthResponse(
    String accessToken, UUID userId, UUID tenantId, String email, String displayName, String role) {}
