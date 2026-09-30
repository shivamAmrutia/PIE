package com.pie.web.dto;

public record ChangeItemDto(
    String category,
    String severity,
    String summary,
    String detail,
    String evidence) {}
