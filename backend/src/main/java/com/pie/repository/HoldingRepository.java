package com.pie.repository;

import com.pie.domain.Holding;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HoldingRepository extends JpaRepository<Holding, UUID> {
  List<Holding> findByClientIdAndAsOfDate(UUID clientId, LocalDate asOfDate);

  @Query(
      """
      SELECT DISTINCT h.asOfDate FROM Holding h
      WHERE h.clientId = :clientId
      ORDER BY h.asOfDate DESC
      """)
  List<LocalDate> findDistinctAsOfDates(@Param("clientId") UUID clientId);
}
