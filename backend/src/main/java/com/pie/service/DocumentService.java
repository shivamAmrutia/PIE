package com.pie.service;

import com.pie.audit.AuditService;
import com.pie.config.PieProperties;
import com.pie.domain.Document;
import com.pie.domain.DocumentType;
import com.pie.domain.Holding;
import com.pie.graph.ClientGraphSyncService;
import com.pie.ingestion.DocumentIngestionService;
import com.pie.repository.DocumentRepository;
import com.pie.repository.HoldingRepository;
import com.pie.security.PieUserPrincipal;
import com.pie.web.dto.DocumentDto;
import com.pie.web.dto.HoldingImportResult;
import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DocumentService {

  private final DocumentRepository documentRepository;
  private final HoldingRepository holdingRepository;
  private final ClientAuthorizationService authorizationService;
  private final AuditService auditService;
  private final DocumentIngestionService documentIngestionService;
  private final ClientGraphSyncService graphSyncService;
  private final Path uploadRoot;

  public DocumentService(
      DocumentRepository documentRepository,
      HoldingRepository holdingRepository,
      ClientAuthorizationService authorizationService,
      AuditService auditService,
      DocumentIngestionService documentIngestionService,
      ClientGraphSyncService graphSyncService,
      PieProperties properties) {
    this.documentRepository = documentRepository;
    this.holdingRepository = holdingRepository;
    this.authorizationService = authorizationService;
    this.auditService = auditService;
    this.documentIngestionService = documentIngestionService;
    this.graphSyncService = graphSyncService;
    this.uploadRoot = Path.of(properties.storage().uploadDir()).toAbsolutePath().normalize();
  }

  @Transactional
  public DocumentDto upload(
      PieUserPrincipal user,
      UUID clientId,
      MultipartFile file,
      DocumentType docType,
      LocalDate asOfDate,
      HttpServletRequest request)
      throws Exception {
    authorizationService.requireAccessibleClient(user, clientId);
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
    }

    Path clientDir = uploadRoot.resolve(user.getTenantId().toString()).resolve(clientId.toString());
    Files.createDirectories(clientDir);
    UUID docId = UUID.randomUUID();
    String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.bin";
    Path target = clientDir.resolve(docId + "_" + original);
    Files.copy(file.getInputStream(), target);

    DocumentType resolved = docType != null ? docType : inferType(original);
    Document document =
        new Document(
            docId,
            clientId,
            user.getTenantId(),
            original,
            file.getContentType() != null ? file.getContentType() : "application/octet-stream",
            resolved,
            target.toString(),
            user.getUserId());
    document.setAsOfDate(asOfDate);
    documentRepository.saveAndFlush(document);
    documentIngestionService.ingest(document);

    auditService.record(
        user,
        clientId,
        "DOCUMENT_UPLOAD",
        "DOCUMENT",
        docId.toString(),
        original + " (" + resolved + ")",
        request);

    return new DocumentDto(
        document.getId(),
        document.getFilename(),
        document.getDocType().name(),
        document.getContentType(),
        document.getAsOfDate(),
        document.getCreatedAt());
  }

  @Transactional
  public HoldingImportResult importHoldingsCsv(
      PieUserPrincipal user, UUID clientId, MultipartFile file, HttpServletRequest request)
      throws Exception {
    authorizationService.requireAccessibleClient(user, clientId);
    DocumentDto uploaded =
        upload(user, clientId, file, DocumentType.PORTFOLIO_CSV, null, request);

    Document saved =
        documentRepository
            .findById(uploaded.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Upload missing"));

    List<Holding> holdings = new ArrayList<>();
    try (BufferedReader reader =
        Files.newBufferedReader(Path.of(saved.getStoragePath()), StandardCharsets.UTF_8)) {
      String headerLine = reader.readLine();
      if (headerLine == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV is empty");
      }
      Map<String, Integer> header = parseHeader(headerLine);
      requireColumns(header, "account", "quantity", "price", "market_value", "as_of_date");
      if (!header.containsKey("ticker") && !header.containsKey("security")) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV requires ticker or security");
      }

      String line;
      int row = 1;
      while ((line = reader.readLine()) != null) {
        row++;
        if (line.isBlank()) {
          continue;
        }
        String[] cols = splitCsv(line);
        String account = col(cols, header, "account");
        String ticker =
            header.containsKey("ticker") ? col(cols, header, "ticker") : col(cols, header, "security");
        String securityName = header.containsKey("security_name") ? col(cols, header, "security_name") : ticker;
        String assetClass = header.containsKey("asset_class") ? col(cols, header, "asset_class") : null;
        BigDecimal quantity = new BigDecimal(col(cols, header, "quantity"));
        BigDecimal price = new BigDecimal(col(cols, header, "price"));
        BigDecimal marketValue = new BigDecimal(col(cols, header, "market_value"));
        LocalDate asOf = LocalDate.parse(col(cols, header, "as_of_date"));

        holdings.add(
            new Holding(
                UUID.randomUUID(),
                clientId,
                user.getTenantId(),
                account,
                ticker,
                securityName,
                quantity,
                price,
                marketValue,
                assetClass,
                asOf,
                uploaded.id()));
      }
      if (holdings.isEmpty()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No holding rows found (checked " + row + " lines)");
      }
    }

    holdingRepository.saveAll(holdings);
    graphSyncService.syncHoldings(clientId, holdings);
    auditService.record(
        user,
        clientId,
        "HOLDINGS_IMPORT",
        "HOLDINGS",
        uploaded.id().toString(),
        holdings.size() + " rows",
        request);

    return new HoldingImportResult(uploaded.id(), holdings.size(), holdings.get(0).getAsOfDate());
  }

  private DocumentType inferType(String filename) {
    String lower = filename.toLowerCase(Locale.ROOT);
    if (lower.contains("ips")) {
      return DocumentType.IPS;
    }
    if (lower.contains("statement")) {
      return DocumentType.STATEMENT;
    }
    if (lower.contains("meeting") || lower.contains("notes")) {
      return DocumentType.MEETING_NOTES;
    }
    if (lower.contains("research")) {
      return DocumentType.RESEARCH;
    }
    if (lower.endsWith(".csv")) {
      return DocumentType.PORTFOLIO_CSV;
    }
    return DocumentType.OTHER;
  }

  private Map<String, Integer> parseHeader(String headerLine) {
    String[] parts = splitCsv(headerLine);
    Map<String, Integer> map = new HashMap<>();
    for (int i = 0; i < parts.length; i++) {
      map.put(parts[i].trim().toLowerCase(Locale.ROOT), i);
    }
    return map;
  }

  private void requireColumns(Map<String, Integer> header, String... columns) {
    for (String column : columns) {
      if (!header.containsKey(column)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing CSV column: " + column);
      }
    }
  }

  private String col(String[] cols, Map<String, Integer> header, String name) {
    Integer idx = header.get(name);
    if (idx == null || idx >= cols.length) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing value for " + name);
    }
    return cols[idx].trim();
  }

  private String[] splitCsv(String line) {
    return line.split(",", -1);
  }
}
