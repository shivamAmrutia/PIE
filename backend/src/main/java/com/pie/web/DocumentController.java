package com.pie.web;

import com.pie.domain.DocumentType;
import com.pie.security.SecurityUtils;
import com.pie.service.DocumentService;
import com.pie.web.dto.DocumentDto;
import com.pie.web.dto.HoldingImportResult;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/clients/{clientId}")
public class DocumentController {

  private final DocumentService documentService;

  public DocumentController(DocumentService documentService) {
    this.documentService = documentService;
  }

  @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public DocumentDto upload(
      @PathVariable UUID clientId,
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "docType", required = false) DocumentType docType,
      @RequestParam(value = "asOfDate", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate asOfDate,
      HttpServletRequest request)
      throws Exception {
    return documentService.upload(
        SecurityUtils.currentUser(), clientId, file, docType, asOfDate, request);
  }

  @PostMapping(value = "/holdings/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public HoldingImportResult importHoldings(
      @PathVariable UUID clientId,
      @RequestParam("file") MultipartFile file,
      HttpServletRequest request)
      throws Exception {
    return documentService.importHoldingsCsv(SecurityUtils.currentUser(), clientId, file, request);
  }
}
