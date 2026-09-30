package com.pie.repository;

import com.pie.domain.ClientAccess;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientAccessRepository extends JpaRepository<ClientAccess, UUID> {
  Optional<ClientAccess> findByUserIdAndClientId(UUID userId, UUID clientId);

  boolean existsByUserIdAndClientId(UUID userId, UUID clientId);
}
