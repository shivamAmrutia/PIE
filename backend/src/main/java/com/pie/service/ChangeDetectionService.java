package com.pie.service;

import com.pie.audit.AuditService;
import com.pie.config.PieProperties;
import com.pie.repository.HoldingRepository;
import com.pie.security.PieUserPrincipal;
import com.pie.web.dto.AllocationBucketDto;
import com.pie.web.dto.ChangeDetectionDto;
import com.pie.web.dto.ChangeItemDto;
import com.pie.web.dto.ConcentrationItemDto;
import com.pie.web.dto.PortfolioAnalysisDto;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChangeDetectionService {

  private static final BigDecimal ALLOCATION_DRIFT_PP = new BigDecimal("5");
  private static final BigDecimal CASH_CHANGE_PCT = new BigDecimal("10");
  private static final BigDecimal HUNDRED = new BigDecimal("100");

  private final ClientAuthorizationService authorizationService;
  private final HoldingRepository holdingRepository;
  private final PortfolioAnalyticsService portfolioAnalyticsService;
  private final AuditService auditService;
  private final JdbcTemplate jdbcTemplate;
  private final String disclaimer;

  public ChangeDetectionService(
      ClientAuthorizationService authorizationService,
      HoldingRepository holdingRepository,
      PortfolioAnalyticsService portfolioAnalyticsService,
      AuditService auditService,
      JdbcTemplate jdbcTemplate,
      PieProperties properties) {
    this.authorizationService = authorizationService;
    this.holdingRepository = holdingRepository;
    this.portfolioAnalyticsService = portfolioAnalyticsService;
    this.auditService = auditService;
    this.jdbcTemplate = jdbcTemplate;
    this.disclaimer = properties.ask().disclaimer();
  }

  public ChangeDetectionDto compare(
      PieUserPrincipal user,
      UUID clientId,
      LocalDate fromDate,
      LocalDate toDate,
      HttpServletRequest request) {
    authorizationService.requireAccessibleClient(user, clientId);

    List<LocalDate> dates = holdingRepository.findDistinctAsOfDates(clientId);
    if (dates.size() < 2 && (fromDate == null || toDate == null)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Need at least two holdings as-of dates to compare");
    }

    LocalDate to = toDate != null ? toDate : dates.get(0);
    LocalDate from =
        fromDate != null
            ? fromDate
            : dates.stream().filter(d -> d.isBefore(to)).findFirst().orElse(dates.get(dates.size() - 1));

    if (!from.isBefore(to)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fromDate must be before toDate");
    }

    PortfolioAnalysisDto before = portfolioAnalyticsService.analyze(user, clientId, from);
    PortfolioAnalysisDto after = portfolioAnalyticsService.analyze(user, clientId, to);

    List<ChangeItemDto> material = new ArrayList<>();
    List<ChangeItemDto> routine = new ArrayList<>();

    compareSecurities(before, after, material, routine);
    compareAllocation(before, after, material, routine);
    compareCash(before, after, material, routine);
    flagConcentrationBreaches(after, material);

    BigDecimal totalChangePct = pctChange(before.totalMarketValue(), after.totalMarketValue());
    List<ChangeItemDto> documentChanges = documentChanges(clientId, from, to);

    auditService.record(
        user,
        clientId,
        "CHANGE_DETECT",
        "CLIENT",
        clientId.toString(),
        from + " → " + to,
        request);

    return new ChangeDetectionDto(
        clientId,
        from,
        to,
        before.totalMarketValue(),
        after.totalMarketValue(),
        totalChangePct,
        material,
        routine,
        documentChanges,
        "CALCULATED",
        disclaimer);
  }

  private void compareSecurities(
      PortfolioAnalysisDto before,
      PortfolioAnalysisDto after,
      List<ChangeItemDto> material,
      List<ChangeItemDto> routine) {
    Map<String, BigDecimal> beforeMap = toTickerWeights(before);
    Map<String, BigDecimal> afterMap = toTickerWeights(after);
    Set<String> all = new HashSet<>();
    all.addAll(beforeMap.keySet());
    all.addAll(afterMap.keySet());

    for (String ticker : all) {
      boolean was = beforeMap.containsKey(ticker);
      boolean now = afterMap.containsKey(ticker);
      if (was && !now) {
        material.add(
            new ChangeItemDto(
                "PORTFOLIO",
                "MATERIAL",
                "Removed security: " + ticker,
                ticker + " was held as of " + before.asOfDate() + " and is absent as of " + after.asOfDate(),
                "holdings@" + before.asOfDate()));
      } else if (!was && now) {
        material.add(
            new ChangeItemDto(
                "PORTFOLIO",
                "MATERIAL",
                "New security: " + ticker,
                ticker
                    + " added at "
                    + afterMap.get(ticker)
                    + "% of portfolio as of "
                    + after.asOfDate(),
                "holdings@" + after.asOfDate()));
      } else {
        BigDecimal delta = afterMap.get(ticker).subtract(beforeMap.get(ticker)).abs();
        if (delta.compareTo(ALLOCATION_DRIFT_PP) >= 0) {
          material.add(
              new ChangeItemDto(
                  "PORTFOLIO",
                  "MATERIAL",
                  "Material weight change: " + ticker,
                  ticker
                      + " moved from "
                      + beforeMap.get(ticker)
                      + "% to "
                      + afterMap.get(ticker)
                      + "% ("
                      + delta
                      + " pp)",
                  "holdings@" + before.asOfDate() + "→" + after.asOfDate()));
        } else if (delta.compareTo(BigDecimal.ONE) >= 0) {
          routine.add(
              new ChangeItemDto(
                  "PORTFOLIO",
                  "ROUTINE",
                  "Weight change: " + ticker,
                  ticker
                      + " "
                      + beforeMap.get(ticker)
                      + "% → "
                      + afterMap.get(ticker)
                      + "%",
                  "holdings@" + before.asOfDate() + "→" + after.asOfDate()));
        }
      }
    }
  }

  private void compareAllocation(
      PortfolioAnalysisDto before,
      PortfolioAnalysisDto after,
      List<ChangeItemDto> material,
      List<ChangeItemDto> routine) {
    Map<String, BigDecimal> beforeMap = toAssetWeights(before);
    Map<String, BigDecimal> afterMap = toAssetWeights(after);
    Set<String> all = new HashSet<>();
    all.addAll(beforeMap.keySet());
    all.addAll(afterMap.keySet());
    for (String asset : all) {
      BigDecimal b = beforeMap.getOrDefault(asset, BigDecimal.ZERO);
      BigDecimal a = afterMap.getOrDefault(asset, BigDecimal.ZERO);
      BigDecimal delta = a.subtract(b).abs();
      if (delta.compareTo(ALLOCATION_DRIFT_PP) >= 0) {
        material.add(
            new ChangeItemDto(
                "ALLOCATION",
                "MATERIAL",
                "Allocation drift: " + asset,
                asset + " " + b + "% → " + a + "% (" + delta + " pp)",
                "allocation@" + before.asOfDate() + "→" + after.asOfDate()));
      } else if (delta.compareTo(new BigDecimal("1")) >= 0) {
        routine.add(
            new ChangeItemDto(
                "ALLOCATION",
                "ROUTINE",
                "Allocation change: " + asset,
                asset + " " + b + "% → " + a + "%",
                "allocation@" + before.asOfDate() + "→" + after.asOfDate()));
      }
    }
  }

  private void compareCash(
      PortfolioAnalysisDto before,
      PortfolioAnalysisDto after,
      List<ChangeItemDto> material,
      List<ChangeItemDto> routine) {
    BigDecimal beforeCash = assetValue(before, "Cash");
    BigDecimal afterCash = assetValue(after, "Cash");
    if (before.totalMarketValue().compareTo(BigDecimal.ZERO) == 0) {
      return;
    }
    BigDecimal changeVsPortfolio =
        afterCash
            .subtract(beforeCash)
            .abs()
            .multiply(HUNDRED)
            .divide(before.totalMarketValue(), 4, RoundingMode.HALF_UP);
    if (changeVsPortfolio.compareTo(CASH_CHANGE_PCT) > 0) {
      material.add(
          new ChangeItemDto(
              "CASH",
              "MATERIAL",
              "Material cash change",
              "Cash moved from "
                  + beforeCash
                  + " to "
                  + afterCash
                  + " ("
                  + changeVsPortfolio
                  + "% of prior portfolio value)",
              "cash@" + before.asOfDate() + "→" + after.asOfDate()));
    } else if (beforeCash.compareTo(afterCash) != 0) {
      routine.add(
          new ChangeItemDto(
              "CASH",
              "ROUTINE",
              "Cash balance changed",
              beforeCash + " → " + afterCash,
              "cash@" + before.asOfDate() + "→" + after.asOfDate()));
    }
  }

  private void flagConcentrationBreaches(PortfolioAnalysisDto after, List<ChangeItemDto> material) {
    BigDecimal limit = loadConcentrationLimit(after.clientId());
    if (limit == null) {
      return;
    }
    for (ConcentrationItemDto item : after.concentration()) {
      if (isCashLike(item.ticker())) {
        continue;
      }
      if (item.weightPct().compareTo(limit) > 0) {
        material.add(
            new ChangeItemDto(
                "CONCENTRATION",
                "MATERIAL",
                "IPS concentration breach: " + item.ticker(),
                item.ticker()
                    + " is "
                    + item.weightPct()
                    + "% vs IPS max "
                    + limit
                    + "% as of "
                    + after.asOfDate(),
                "IPS_CONCENTRATION_LIMIT + holdings@" + after.asOfDate()));
      }
    }
  }

  private boolean isCashLike(String ticker) {
    String t = ticker == null ? "" : ticker.trim().toUpperCase();
    return t.equals("CASH") || t.contains("CASH");
  }

  private List<ChangeItemDto> documentChanges(UUID clientId, LocalDate from, LocalDate to) {
    return jdbcTemplate.query(
        """
        SELECT filename, doc_type, as_of_date, created_at::date AS created_day
        FROM documents
        WHERE client_id = ?
          AND (
            (as_of_date IS NOT NULL AND as_of_date > ? AND as_of_date <= ?)
            OR (as_of_date IS NULL AND doc_type IN ('IPS', 'MEETING_NOTES', 'STATEMENT', 'RESEARCH')
                AND created_at::date >= ?)
          )
        ORDER BY coalesce(as_of_date, created_at::date) DESC
        """,
        (rs, rowNum) ->
            new ChangeItemDto(
                "DOCUMENT",
                "MATERIAL",
                "Document in review window: " + rs.getString("filename"),
                rs.getString("doc_type")
                    + (rs.getDate("as_of_date") != null
                        ? " as-of " + rs.getDate("as_of_date")
                        : " uploaded " + rs.getDate("created_day")),
                rs.getString("filename")),
        clientId,
        java.sql.Date.valueOf(from),
        java.sql.Date.valueOf(to),
        java.sql.Date.valueOf(from));
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

  private Map<String, BigDecimal> toTickerWeights(PortfolioAnalysisDto dto) {
    Map<String, BigDecimal> map = new HashMap<>();
    for (ConcentrationItemDto item : dto.concentration()) {
      map.put(item.ticker(), item.weightPct());
    }
    return map;
  }

  private Map<String, BigDecimal> toAssetWeights(PortfolioAnalysisDto dto) {
    Map<String, BigDecimal> map = new HashMap<>();
    for (AllocationBucketDto item : dto.allocation()) {
      map.put(item.assetClass(), item.weightPct());
    }
    return map;
  }

  private BigDecimal assetValue(PortfolioAnalysisDto dto, String assetClass) {
    return dto.allocation().stream()
        .filter(a -> assetClass.equalsIgnoreCase(a.assetClass()))
        .map(AllocationBucketDto::marketValue)
        .findFirst()
        .orElse(BigDecimal.ZERO);
  }

  private BigDecimal pctChange(BigDecimal from, BigDecimal to) {
    if (from.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }
    return to.subtract(from).multiply(HUNDRED).divide(from, 4, RoundingMode.HALF_UP);
  }

  /** Package-visible helper for tests — material thresholds from PRD. */
  static BigDecimal allocationDriftThreshold() {
    return ALLOCATION_DRIFT_PP;
  }
}
