package com.pie.web.dto;

import java.time.LocalDate;
import java.util.UUID;

public record HoldingImportResult(UUID documentId, int rowCount, LocalDate asOfDate) {}
