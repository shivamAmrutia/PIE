package com.pie.web.dto;

import java.util.UUID;

public record AccountDto(UUID id, String name, String accountType) {}
