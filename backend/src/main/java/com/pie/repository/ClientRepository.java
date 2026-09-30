package com.pie.repository;

import com.pie.domain.Client;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClientRepository extends JpaRepository<Client, UUID> {

  @Query(
      """
      SELECT c FROM Client c
      WHERE c.tenantId = :tenantId
        AND c.id IN (
          SELECT ca.clientId FROM ClientAccess ca WHERE ca.userId = :userId
        )
      ORDER BY c.displayName
      """)
  List<Client> findAccessibleByUser(
      @Param("tenantId") UUID tenantId, @Param("userId") UUID userId);
}
