package com.pie.web.dto;

public record ReviewItemDto(
    String severity,
    String title,
    String detail,
    String evidence,
    String claimType) {}
