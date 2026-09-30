package com.pie.web;

import com.pie.security.SecurityUtils;
import com.pie.service.ChangeDetectionService;
import com.pie.service.ReviewBriefingService;
import com.pie.web.dto.ChangeDetectionDto;
import com.pie.web.dto.ReviewBriefingDto;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clients/{clientId}")
public class ReviewController {

  private final ReviewBriefingService reviewBriefingService;
  private final ChangeDetectionService changeDetectionService;

  public ReviewController(
      ReviewBriefingService reviewBriefingService, ChangeDetectionService changeDetectionService) {
    this.reviewBriefingService = reviewBriefingService;
    this.changeDetectionService = changeDetectionService;
  }

  @PostMapping("/review/prepare")
  public ReviewBriefingDto prepareReview(@PathVariable UUID clientId, HttpServletRequest request) {
    return reviewBriefingService.prepare(SecurityUtils.currentUser(), clientId, request);
  }

  @GetMapping("/changes")
  public ChangeDetectionDto changes(
      @PathVariable UUID clientId,
      @RequestParam(value = "fromDate", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate fromDate,
      @RequestParam(value = "toDate", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate toDate,
      HttpServletRequest request) {
    return changeDetectionService.compare(
        SecurityUtils.currentUser(), clientId, fromDate, toDate, request);
  }
}
