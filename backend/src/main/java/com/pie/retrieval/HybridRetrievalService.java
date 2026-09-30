package com.pie.retrieval;

import com.pie.config.PieProperties;
import com.pie.embedding.EmbeddingService;
import com.pie.graph.ClientGraphSyncService;
import com.pie.repository.DocumentChunkRepository;
import com.pie.repository.DocumentChunkRepository.RetrievedChunk;
import com.pie.repository.HoldingRepository;
import com.pie.web.dto.PortfolioAnalysisDto;
import com.pie.service.PortfolioAnalyticsService;
import com.pie.security.PieUserPrincipal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class HybridRetrievalService {

  private final DocumentChunkRepository chunkRepository;
  private final EmbeddingService embeddingService;
  private final HoldingRepository holdingRepository;
  private final PortfolioAnalyticsService portfolioAnalyticsService;
  private final ClientGraphSyncService graphSyncService;
  private final JdbcTemplate jdbcTemplate;
  private final int topK;

  public HybridRetrievalService(
      DocumentChunkRepository chunkRepository,
      EmbeddingService embeddingService,
      HoldingRepository holdingRepository,
      PortfolioAnalyticsService portfolioAnalyticsService,
      ClientGraphSyncService graphSyncService,
      JdbcTemplate jdbcTemplate,
      PieProperties properties) {
    this.chunkRepository = chunkRepository;
    this.embeddingService = embeddingService;
    this.holdingRepository = holdingRepository;
    this.portfolioAnalyticsService = portfolioAnalyticsService;
    this.graphSyncService = graphSyncService;
    this.jdbcTemplate = jdbcTemplate;
    this.topK = properties.ask().topK();
  }

  public RetrievalBundle retrieve(PieUserPrincipal user, UUID clientId, String question) {
    List<RetrievedChunk> keyword = chunkRepository.keywordSearch(clientId, question, topK);
    List<RetrievedChunk> semantic =
        chunkRepository.semanticSearch(clientId, embeddingService.embed(question), topK);
    List<RetrievedChunk> merged = mergeChunks(keyword, semantic);

    List<FactHit> facts = searchFacts(clientId, question);
    facts = mergeFacts(facts, loadIpsFacts(clientId));
    List<String> graphTickers = graphSyncService.relatedTickers(clientId, 8);

    PortfolioAnalysisDto analysis = null;
    List<java.time.LocalDate> asOfDates = holdingRepository.findDistinctAsOfDates(clientId);
    if (!asOfDates.isEmpty()) {
      analysis = portfolioAnalyticsService.analyze(user, clientId, asOfDates.get(0));
    }

    return new RetrievalBundle(merged, facts, graphTickers, analysis, asOfDates);
  }

  private List<RetrievedChunk> mergeChunks(List<RetrievedChunk> a, List<RetrievedChunk> b) {
    Map<UUID, RetrievedChunk> map = new LinkedHashMap<>();
    for (RetrievedChunk chunk : a) {
      map.put(chunk.chunk().getId(), chunk);
    }
    for (RetrievedChunk chunk : b) {
      map.merge(
          chunk.chunk().getId(),
          chunk,
          (left, right) -> left.score() >= right.score() ? left : right);
    }
    return map.values().stream()
        .sorted(Comparator.comparingDouble(RetrievedChunk::score).reversed())
        .limit(topK)
        .toList();
  }

  private List<FactHit> searchFacts(UUID clientId, String question) {
    String like = "%" + question.toLowerCase().replaceAll("\\s+", "%") + "%";
    return jdbcTemplate.query(
        """
        SELECT fact_type, fact_key, fact_value, source_document_id, source_page, source_snippet
        FROM extracted_facts
        WHERE client_id = ?
          AND (lower(fact_value) LIKE ? OR lower(fact_type) LIKE ? OR lower(fact_key) LIKE ?)
        ORDER BY created_at DESC
        LIMIT 8
        """,
        (rs, rowNum) ->
            new FactHit(
                rs.getString("fact_type"),
                rs.getString("fact_key"),
                rs.getString("fact_value"),
                (UUID) rs.getObject("source_document_id"),
                (Integer) rs.getObject("source_page"),
                rs.getString("source_snippet")),
        clientId,
        like,
        like,
        like);
  }

  private List<FactHit> loadIpsFacts(UUID clientId) {
    return jdbcTemplate.query(
        """
        SELECT fact_type, fact_key, fact_value, source_document_id, source_page, source_snippet
        FROM extracted_facts
        WHERE client_id = ?
          AND fact_type IN ('IPS_CONCENTRATION_LIMIT', 'GOAL')
        ORDER BY created_at DESC
        LIMIT 20
        """,
        (rs, rowNum) ->
            new FactHit(
                rs.getString("fact_type"),
                rs.getString("fact_key"),
                rs.getString("fact_value"),
                (UUID) rs.getObject("source_document_id"),
                (Integer) rs.getObject("source_page"),
                rs.getString("source_snippet")),
        clientId);
  }

  private List<FactHit> mergeFacts(List<FactHit> primary, List<FactHit> extra) {
    Map<String, FactHit> map = new LinkedHashMap<>();
    for (FactHit fact : primary) {
      map.put(fact.factType() + "|" + fact.factKey() + "|" + fact.factValue(), fact);
    }
    for (FactHit fact : extra) {
      map.putIfAbsent(fact.factType() + "|" + fact.factKey() + "|" + fact.factValue(), fact);
    }
    return new ArrayList<>(map.values());
  }

  public record FactHit(
      String factType,
      String factKey,
      String factValue,
      UUID sourceDocumentId,
      Integer sourcePage,
      String sourceSnippet) {}

  public record RetrievalBundle(
      List<RetrievedChunk> chunks,
      List<FactHit> facts,
      List<String> graphTickers,
      PortfolioAnalysisDto analysis,
      List<java.time.LocalDate> asOfDates) {}
}
