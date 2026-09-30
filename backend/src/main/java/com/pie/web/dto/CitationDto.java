package com.pie.web.dto;

import java.util.List;
import java.util.UUID;

public record CitationDto(
    UUID documentId,
    UUID chunkId,
    String filename,
    String docType,
    Integer pageNumber,
    String snippet,
    String claimType) {}
