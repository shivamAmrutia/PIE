package com.pie.web.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ClientWorkspaceDto(
    ClientDto client,
    List<AccountDto> accounts,
    List<DocumentDto> documents,
    List<LocalDate> holdingAsOfDates,
    List<HoldingDto> latestHoldings,
    String disclaimer) {}
