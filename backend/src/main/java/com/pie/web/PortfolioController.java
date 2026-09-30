package com.pie.web;

import com.pie.security.SecurityUtils;
import com.pie.service.PortfolioAnalyticsService;
import com.pie.web.dto.PortfolioAnalysisDto;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clients/{clientId}/portfolio")
public class PortfolioController {

  private final PortfolioAnalyticsService portfolioAnalyticsService;

  public PortfolioController(PortfolioAnalyticsService portfolioAnalyticsService) {
    this.portfolioAnalyticsService = portfolioAnalyticsService;
  }

  @GetMapping("/analysis")
  public PortfolioAnalysisDto analysis(
      @PathVariable UUID clientId,
      @RequestParam(value = "asOfDate", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate asOfDate) {
    return portfolioAnalyticsService.analyze(SecurityUtils.currentUser(), clientId, asOfDate);
  }
}
