package com.pie.repository;

import com.pie.domain.Account;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {
  List<Account> findByClientIdOrderByNameAsc(UUID clientId);
}
