package com.pie.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pie.domain.Holding;
import com.pie.repository.HoldingRepository;
import com.pie.security.PieUserPrincipal;
import com.pie.web.dto.PortfolioAnalysisDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.pie.domain.Client;
import com.pie.domain.UserRole;

@ExtendWith(MockitoExtension.class)
class PortfolioAnalyticsServiceTest {

  @Mock HoldingRepository holdingRepository;
  @Mock ClientAuthorizationService authorizationService;
  @InjectMocks PortfolioAnalyticsService service;

  @Test
  void computesAllocationAndConcentration() {
    UUID clientId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    LocalDate asOf = LocalDate.of(2026, 9, 20);
    PieUserPrincipal user =
        new PieUserPrincipal(
            UUID.randomUUID(), tenantId, "a@b.com", "x", "A", UserRole.ADVISOR, true);

    when(authorizationService.requireAccessibleClient(eq(user), eq(clientId)))
        .thenReturn(new Client(clientId, tenantId, "John", "x"));
    when(holdingRepository.findByClientIdAndAsOfDate(clientId, asOf))
        .thenReturn(
            List.of(
                holding(clientId, tenantId, "Taxable", "XYZ", "Equities", "500", asOf),
                holding(clientId, tenantId, "Taxable", "BND", "Fixed Income", "500", asOf)));

    PortfolioAnalysisDto dto = service.analyze(user, clientId, asOf);

    assertEquals(0, new BigDecimal("1000.00").compareTo(dto.totalMarketValue()));
    assertEquals(2, dto.allocation().size());
    assertTrue(
        dto.concentration().stream().anyMatch(c -> c.ticker().equals("XYZ") && c.weightPct().doubleValue() == 50.0));
  }

  private Holding holding(
      UUID clientId,
      UUID tenantId,
      String account,
      String ticker,
      String assetClass,
      String value,
      LocalDate asOf) {
    return new Holding(
        UUID.randomUUID(),
        clientId,
        tenantId,
        account,
        ticker,
        ticker,
        BigDecimal.ONE,
        new BigDecimal(value),
        new BigDecimal(value),
        assetClass,
        asOf,
        null);
  }
}
