package com.pie.service;

import com.pie.domain.Holding;
import com.pie.repository.HoldingRepository;
import com.pie.security.PieUserPrincipal;
import com.pie.web.dto.AllocationBucketDto;
import com.pie.web.dto.ConcentrationItemDto;
import com.pie.web.dto.PortfolioAnalysisDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PortfolioAnalyticsService {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private final HoldingRepository holdingRepository;
  private final ClientAuthorizationService authorizationService;

  public PortfolioAnalyticsService(
      HoldingRepository holdingRepository, ClientAuthorizationService authorizationService) {
    this.holdingRepository = holdingRepository;
    this.authorizationService = authorizationService;
  }

  @Transactional(readOnly = true)
  public PortfolioAnalysisDto analyze(PieUserPrincipal user, UUID clientId, LocalDate asOfDate) {
    authorizationService.requireAccessibleClient(user, clientId);

    LocalDate effective = asOfDate;
    if (effective == null) {
      List<LocalDate> dates = holdingRepository.findDistinctAsOfDates(clientId);
      if (dates.isEmpty()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No holdings for client");
      }
      effective = dates.get(0);
    }

    List<Holding> holdings = holdingRepository.findByClientIdAndAsOfDate(clientId, effective);
    if (holdings.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No holdings for as-of " + effective);
    }

    BigDecimal total =
        holdings.stream().map(Holding::getMarketValue).reduce(BigDecimal.ZERO, BigDecimal::add);

    Map<String, BigDecimal> byAsset = new HashMap<>();
    Map<String, BigDecimal> byTicker = new HashMap<>();
    for (Holding h : holdings) {
      String asset = h.getAssetClass() != null ? h.getAssetClass() : "Unknown";
      byAsset.merge(asset, h.getMarketValue(), BigDecimal::add);
      byTicker.merge(h.getTicker(), h.getMarketValue(), BigDecimal::add);
    }

    List<AllocationBucketDto> allocation = new ArrayList<>();
    for (var e : byAsset.entrySet()) {
      allocation.add(
          new AllocationBucketDto(
              e.getKey(), e.getValue(), weight(e.getValue(), total)));
    }
    allocation.sort(Comparator.comparing(AllocationBucketDto::weightPct).reversed());

    List<ConcentrationItemDto> concentration = new ArrayList<>();
    for (var e : byTicker.entrySet()) {
      concentration.add(
          new ConcentrationItemDto(e.getKey(), e.getValue(), weight(e.getValue(), total)));
    }
    concentration.sort(Comparator.comparing(ConcentrationItemDto::weightPct).reversed());

    return new PortfolioAnalysisDto(
        clientId,
        effective,
        total,
        allocation,
        concentration,
        "CALCULATED",
        "Deterministic backend calculation from holdings snapshot");
  }

  private BigDecimal weight(BigDecimal part, BigDecimal total) {
    if (total.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }
    return part.multiply(HUNDRED).divide(total, 4, RoundingMode.HALF_UP);
  }
}
