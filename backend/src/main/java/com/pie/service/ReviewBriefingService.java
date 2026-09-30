package com.pie.service;

import com.pie.audit.AuditService;
import com.pie.config.PieProperties;
import com.pie.domain.Client;
import com.pie.domain.Document;
import com.pie.repository.DocumentRepository;
import com.pie.repository.HoldingRepository;
import com.pie.security.PieUserPrincipal;
import com.pie.web.dto.ChangeDetectionDto;
import com.pie.web.dto.ChangeItemDto;
import com.pie.web.dto.ConcentrationItemDto;
import com.pie.web.dto.DocumentDto;
import com.pie.web.dto.PortfolioAnalysisDto;
import com.pie.web.dto.ReviewBriefingDto;
import com.pie.web.dto.ReviewItemDto;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReviewBriefingService {

  private final ClientAuthorizationService authorizationService;
  private final PortfolioAnalyticsService portfolioAnalyticsService;
  private final ChangeDetectionService changeDetectionService;
  private final HoldingRepository holdingRepository;
  private final DocumentRepository documentRepository;
  private final AuditService auditService;
  private final JdbcTemplate jdbcTemplate;
  private final String disclaimer;

  public ReviewBriefingService(
      ClientAuthorizationService authorizationService,
      PortfolioAnalyticsService portfolioAnalyticsService,
      ChangeDetectionService changeDetectionService,
      HoldingRepository holdingRepository,
      DocumentRepository documentRepository,
      AuditService auditService,
      JdbcTemplate jdbcTemplate,
      PieProperties properties) {
    this.authorizationService = authorizationService;
    this.portfolioAnalyticsService = portfolioAnalyticsService;
    this.changeDetectionService = changeDetectionService;
    this.holdingRepository = holdingRepository;
    this.documentRepository = documentRepository;
    this.auditService = auditService;
    this.jdbcTemplate = jdbcTemplate;
    this.disclaimer = properties.ask().disclaimer();
  }

  public ReviewBriefingDto prepare(
      PieUserPrincipal user, UUID clientId, HttpServletRequest request) {
    Client client = authorizationService.requireAccessibleClient(user, clientId);
    List<LocalDate> dates = holdingRepository.findDistinctAsOfDates(clientId);
    if (dates.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Import holdings before generating a review briefing");
    }

    LocalDate asOf = dates.get(0);
    PortfolioAnalysisDto analysis = portfolioAnalyticsService.analyze(user, clientId, asOf);

    BigDecimal periodReturn = null;
    List<ChangeItemDto> recentChanges = List.of();
    if (dates.size() >= 2) {
      LocalDate prior = dates.get(1);
      ChangeDetectionDto diff =
          changeDetectionService.compare(user, clientId, prior, asOf, request);
      recentChanges = diff.materialChanges();
      periodReturn = diff.totalChangePct();
    }

    List<ReviewItemDto> reviewItems = buildReviewItems(clientId, analysis);
    List<String> goals = loadGoals(clientId);
    List<String> discussions = loadMeetingSnippets(clientId);
    List<DocumentDto> keyDocuments =
        documentRepository.findByClientIdOrderByCreatedAtDesc(clientId).stream()
            .limit(8)
            .map(this::toDocDto)
            .toList();

    auditService.record(
        user, clientId, "REVIEW_BRIEFING", "CLIENT", clientId.toString(), asOf.toString(), request);

    return new ReviewBriefingDto(
        clientId,
        client.getDisplayName(),
        asOf,
        analysis.totalMarketValue(),
        periodReturn,
        "Benchmark comparison not configured in MVP (synthetic demo)",
        reviewItems,
        recentChanges,
        goals,
        discussions,
        keyDocuments,
        disclaimer,
        "MIXED");
  }

  private List<ReviewItemDto> buildReviewItems(UUID clientId, PortfolioAnalysisDto analysis) {
    List<ReviewItemDto> items = new ArrayList<>();
    BigDecimal limit = loadConcentrationLimit(clientId);
    if (limit != null) {
      for (ConcentrationItemDto c : analysis.concentration()) {
        if (isCashLike(c.ticker())) {
          continue;
        }
        if (c.weightPct().compareTo(limit) > 0) {
          items.add(
              new ReviewItemDto(
                  "WARNING",
                  c.ticker() + " concentration above IPS threshold",
                  c.ticker()
                      + " is "
                      + c.weightPct()
                      + "% of portfolio vs IPS max "
                      + limit
                      + "%",
                  "IPS_CONCENTRATION_LIMIT + holdings@" + analysis.asOfDate(),
                  "CALCULATED"));
        }
      }
    }

    analysis.allocation().stream()
        .filter(a -> "Cash".equalsIgnoreCase(a.assetClass()) && a.weightPct().compareTo(new BigDecimal("15")) > 0)
        .findFirst()
        .ifPresent(
            cash ->
                items.add(
                    new ReviewItemDto(
                        "INFO",
                        "Significant cash balance",
                        "Cash is " + cash.weightPct() + "% of portfolio as of " + analysis.asOfDate(),
                        "allocation@" + analysis.asOfDate(),
                        "CALCULATED")));

    analysis.allocation().stream()
        .filter(a -> "Equities".equalsIgnoreCase(a.assetClass()))
        .findFirst()
        .ifPresent(
            eq -> {
              BigDecimal target = new BigDecimal("60");
              BigDecimal drift = eq.weightPct().subtract(target).abs();
              if (drift.compareTo(new BigDecimal("5")) >= 0) {
                items.add(
                    new ReviewItemDto(
                        "WARNING",
                        "Equity allocation diverges from IPS target",
                        "Equities at "
                            + eq.weightPct()
                            + "% vs illustrative IPS target 60% (drift "
                            + drift
                            + " pp)",
                        "IPS target allocation + holdings@" + analysis.asOfDate(),
                        "CALCULATED"));
              }
            });

    if (items.isEmpty()) {
      items.add(
          new ReviewItemDto(
              "INFO",
              "No material review flags from current rules",
              "No IPS concentration breaches or large cash/equity drifts detected from available data",
              "portfolio@" + analysis.asOfDate(),
              "INTERPRETATION"));
    }
    return items;
  }

  private List<String> loadGoals(UUID clientId) {
    return jdbcTemplate.query(
        """
        SELECT fact_value FROM extracted_facts
        WHERE client_id = ? AND fact_type = 'GOAL'
        ORDER BY fact_key
        """,
        (rs, rowNum) -> rs.getString(1),
        clientId);
  }

  private List<String> loadMeetingSnippets(UUID clientId) {
    return jdbcTemplate.query(
        """
        SELECT left(c.content, 240)
        FROM document_chunks c
        JOIN documents d ON d.id = c.document_id
        WHERE c.client_id = ?
          AND d.doc_type = 'MEETING_NOTES'
        ORDER BY c.chunk_index
        LIMIT 5
        """,
        (rs, rowNum) -> rs.getString(1),
        clientId);
  }

  private BigDecimal loadConcentrationLimit(UUID clientId) {
    List<String> values =
        jdbcTemplate.query(
            """
            SELECT fact_value FROM extracted_facts
            WHERE client_id = ? AND fact_type = 'IPS_CONCENTRATION_LIMIT'
            ORDER BY created_at DESC LIMIT 1
            """,
            (rs, rowNum) -> rs.getString(1),
            clientId);
    if (values.isEmpty()) {
      return null;
    }
    try {
      return new BigDecimal(values.get(0));
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private DocumentDto toDocDto(Document document) {
    return new DocumentDto(
        document.getId(),
        document.getFilename(),
        document.getDocType().name(),
        document.getContentType(),
        document.getAsOfDate(),
        document.getCreatedAt());
  }

  private boolean isCashLike(String ticker) {
    String t = ticker == null ? "" : ticker.trim().toUpperCase();
    return t.equals("CASH") || t.contains("CASH");
  }
}
