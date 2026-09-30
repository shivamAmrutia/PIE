package com.pie.ask;

import com.pie.audit.AuditService;
import com.pie.config.PieProperties;
import com.pie.repository.DocumentChunkRepository.RetrievedChunk;
import com.pie.retrieval.HybridRetrievalService;
import com.pie.retrieval.HybridRetrievalService.FactHit;
import com.pie.retrieval.HybridRetrievalService.RetrievalBundle;
import com.pie.security.PieUserPrincipal;
import com.pie.service.ClientAuthorizationService;
import com.pie.web.dto.AskRequest;
import com.pie.web.dto.AskResponse;
import com.pie.web.dto.CitationDto;
import com.pie.web.dto.ClaimDto;
import com.pie.web.dto.ConcentrationItemDto;
import com.pie.web.dto.PortfolioAnalysisDto;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AskPieService {

  private final ClientAuthorizationService authorizationService;
  private final HybridRetrievalService retrievalService;
  private final AdviceValidator adviceValidator;
  private final AuditService auditService;
  private final String disclaimer;

  public AskPieService(
      ClientAuthorizationService authorizationService,
      HybridRetrievalService retrievalService,
      AdviceValidator adviceValidator,
      AuditService auditService,
      PieProperties properties) {
    this.authorizationService = authorizationService;
    this.retrievalService = retrievalService;
    this.adviceValidator = adviceValidator;
    this.auditService = auditService;
    this.disclaimer = properties.ask().disclaimer();
  }

  public AskResponse ask(
      PieUserPrincipal user, UUID clientId, AskRequest request, HttpServletRequest http) {
    authorizationService.requireAccessibleClient(user, clientId);
    String question = request.question().trim();

    RetrievalBundle bundle = retrievalService.retrieve(user, clientId, question);
    AnswerDraft draft = compose(question, bundle);
    AdviceValidator.ValidationResult validated = adviceValidator.validate(draft.answer());

    auditService.record(
        user,
        clientId,
        "ASK_PIE",
        "CLIENT",
        clientId.toString(),
        question.length() > 200 ? question.substring(0, 200) : question,
        http);

    return new AskResponse(
        validated.answer(),
        draft.claims(),
        draft.citations(),
        draft.toolsUsed(),
        disclaimer,
        validated.rewritten(),
        validated.note(),
        "HYBRID_RETRIEVAL_V1");
  }

  private AnswerDraft compose(String question, RetrievalBundle bundle) {
    String q = question.toLowerCase(Locale.ROOT);
    List<ClaimDto> claims = new ArrayList<>();
    List<CitationDto> citations = new ArrayList<>();
    List<String> tools = new ArrayList<>();
    StringBuilder answer = new StringBuilder();

    tools.add("hybrid_retrieval");

    if (looksLikePortfolioQuestion(q) && bundle.analysis() != null) {
      tools.add("portfolio_calculator");
      PortfolioAnalysisDto analysis = bundle.analysis();
      answer
          .append("As of ")
          .append(analysis.asOfDate())
          .append(", total portfolio market value is ")
          .append(analysis.totalMarketValue())
          .append(".\n\n");

      claims.add(
          new ClaimDto(
              "Total market value " + analysis.totalMarketValue() + " as of " + analysis.asOfDate(),
              "CALCULATED",
              List.of("portfolio_calculator")));

      ConcentrationItemDto top = analysis.concentration().isEmpty() ? null : analysis.concentration().get(0);
      if (top != null) {
        answer
            .append("Largest position: ")
            .append(top.ticker())
            .append(" at ")
            .append(top.weightPct())
            .append("% of portfolio.\n");
        claims.add(
            new ClaimDto(
                top.ticker() + " weight " + top.weightPct() + "%",
                "CALCULATED",
                List.of("portfolio_calculator")));
      }

      BigDecimal ipsLimit = findConcentrationLimit(bundle.facts());
      if (top != null && ipsLimit != null && top.weightPct().compareTo(ipsLimit) > 0) {
        answer
            .append("\nThis exceeds the documented IPS individual-security limit of ")
            .append(ipsLimit)
            .append("%. This may warrant advisor review.\n");
        claims.add(
            new ClaimDto(
                "Position exceeds IPS concentration limit of " + ipsLimit + "%",
                "INTERPRETATION",
                citationIdsForFacts(bundle.facts())));
      }

      for (var bucket : analysis.allocation()) {
        answer
            .append("- ")
            .append(bucket.assetClass())
            .append(": ")
            .append(bucket.weightPct())
            .append("%\n");
      }
    }

    if (!bundle.facts().isEmpty() && (looksLikeGoalOrIpsQuestion(q) || answer.isEmpty())) {
      tools.add("structured_facts");
      answer.append("\nExtracted client facts:\n");
      for (FactHit fact : bundle.facts()) {
        answer.append("- ").append(fact.factType()).append(": ").append(fact.factValue()).append("\n");
        claims.add(
            new ClaimDto(
                fact.factValue(),
                "EXTRACTED",
                List.of(fact.sourceDocumentId() != null ? fact.sourceDocumentId().toString() : "fact")));
        if (fact.sourceDocumentId() != null) {
          citations.add(
              new CitationDto(
                  fact.sourceDocumentId(),
                  null,
                  "Extracted fact",
                  fact.factType(),
                  fact.sourcePage(),
                  fact.sourceSnippet(),
                  "EXTRACTED"));
        }
      }
    }

    if (!bundle.chunks().isEmpty()) {
      tools.add("document_chunks");
      if (answer.isEmpty()) {
        answer.append("Based on retrieved client documents:\n\n");
      } else {
        answer.append("\nSupporting document evidence:\n");
      }
      int i = 1;
      for (RetrievedChunk hit : bundle.chunks()) {
        String snippet = truncate(hit.chunk().getContent(), 280);
        answer.append(i++).append(". ").append(snippet).append("\n");
        claims.add(
            new ClaimDto(snippet, "EXTRACTED", List.of(hit.chunk().getDocumentId().toString())));
        citations.add(
            new CitationDto(
                hit.chunk().getDocumentId(),
                hit.chunk().getId(),
                hit.filename(),
                hit.docType(),
                hit.chunk().getPageNumber(),
                snippet,
                "EXTRACTED"));
      }
    }

    if (!bundle.graphTickers().isEmpty() && q.contains("holding")) {
      tools.add("graph");
      answer.append("\nGraph-linked holdings: ").append(String.join(", ", bundle.graphTickers())).append("\n");
    }

    if (answer.isEmpty()) {
      answer.append(
          "I could not find enough grounded evidence in this client's documents or holdings to answer that yet. "
              + "Upload the IPS, statements, meeting notes, and holdings CSV, then try again.");
    }

    return new AnswerDraft(answer.toString().trim(), claims, citations, tools);
  }

  private boolean looksLikePortfolioQuestion(String q) {
    return q.contains("portfolio")
        || q.contains("allocation")
        || q.contains("concentration")
        || q.contains("holding")
        || q.contains("xyz")
        || q.contains("drift")
        || q.contains("perform")
        || q.contains("flag");
  }

  private boolean looksLikeGoalOrIpsQuestion(String q) {
    return q.contains("goal")
        || q.contains("objective")
        || q.contains("ips")
        || q.contains("policy")
        || q.contains("retirement")
        || q.contains("limit");
  }

  private BigDecimal findConcentrationLimit(List<FactHit> facts) {
    for (FactHit fact : facts) {
      if ("IPS_CONCENTRATION_LIMIT".equals(fact.factType())) {
        try {
          return new BigDecimal(fact.factValue());
        } catch (NumberFormatException ignored) {
        }
      }
    }
    // also scan all facts if keyword search missed type
    return null;
  }

  private List<String> citationIdsForFacts(List<FactHit> facts) {
    List<String> ids = new ArrayList<>();
    for (FactHit fact : facts) {
      if (fact.sourceDocumentId() != null) {
        ids.add(fact.sourceDocumentId().toString());
      }
    }
    return ids;
  }

  private String truncate(String text, int max) {
    if (text.length() <= max) {
      return text;
    }
    return text.substring(0, max - 1) + "…";
  }

  private record AnswerDraft(
      String answer, List<ClaimDto> claims, List<CitationDto> citations, List<String> toolsUsed) {}
}
