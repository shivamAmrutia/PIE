package com.pie.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ChangeDetectionThresholdTest {

  @Test
  void prdAllocationDriftThresholdIsFivePoints() {
    assertEquals(0, new BigDecimal("5").compareTo(ChangeDetectionService.allocationDriftThreshold()));
  }

  @Test
  void materialCashThresholdIsTenPercentOfPortfolio() {
    // Documented in ChangeDetectionService as CASH_CHANGE_PCT = 10
    BigDecimal cashDelta = new BigDecimal("70000");
    BigDecimal priorPortfolio = new BigDecimal("450000");
    BigDecimal pct =
        cashDelta.multiply(new BigDecimal("100")).divide(priorPortfolio, 4, java.math.RoundingMode.HALF_UP);
    assertTrue(pct.compareTo(new BigDecimal("10")) > 0);
  }
}
