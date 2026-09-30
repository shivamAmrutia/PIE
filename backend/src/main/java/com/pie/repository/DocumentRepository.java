package com.pie.repository;

import com.pie.domain.Document;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
  List<Document> findByClientIdOrderByCreatedAtDesc(UUID clientId);
}
