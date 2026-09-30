package com.pie.service;

import com.pie.audit.AuditService;
import com.pie.domain.AccessLevel;
import com.pie.domain.Account;
import com.pie.domain.Client;
import com.pie.domain.ClientAccess;
import com.pie.domain.Document;
import com.pie.domain.Holding;
import com.pie.graph.ClientGraphSyncService;
import com.pie.repository.AccountRepository;
import com.pie.repository.ClientAccessRepository;
import com.pie.repository.ClientRepository;
import com.pie.repository.DocumentRepository;
import com.pie.repository.HoldingRepository;
import com.pie.security.PieUserPrincipal;
import com.pie.web.dto.AccountDto;
import com.pie.web.dto.ClientCreateRequest;
import com.pie.web.dto.ClientDto;
import com.pie.web.dto.ClientWorkspaceDto;
import com.pie.web.dto.DocumentDto;
import com.pie.web.dto.HoldingDto;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientService {

  private final ClientRepository clientRepository;
  private final ClientAccessRepository clientAccessRepository;
  private final AccountRepository accountRepository;
  private final DocumentRepository documentRepository;
  private final HoldingRepository holdingRepository;
  private final ClientAuthorizationService authorizationService;
  private final AuditService auditService;
  private final ClientGraphSyncService graphSyncService;

  public ClientService(
      ClientRepository clientRepository,
      ClientAccessRepository clientAccessRepository,
      AccountRepository accountRepository,
      DocumentRepository documentRepository,
      HoldingRepository holdingRepository,
      ClientAuthorizationService authorizationService,
      AuditService auditService,
      ClientGraphSyncService graphSyncService) {
    this.clientRepository = clientRepository;
    this.clientAccessRepository = clientAccessRepository;
    this.accountRepository = accountRepository;
    this.documentRepository = documentRepository;
    this.holdingRepository = holdingRepository;
    this.authorizationService = authorizationService;
    this.auditService = auditService;
    this.graphSyncService = graphSyncService;
  }

  @Transactional(readOnly = true)
  public List<ClientDto> listAccessible(PieUserPrincipal user) {
    return clientRepository.findAccessibleByUser(user.getTenantId(), user.getUserId()).stream()
        .map(this::toDto)
        .toList();
  }

  @Transactional
  public ClientDto create(PieUserPrincipal user, ClientCreateRequest request, HttpServletRequest http) {
    Client client =
        new Client(
            UUID.randomUUID(),
            user.getTenantId(),
            request.displayName().trim(),
            request.externalRef());
    clientRepository.save(client);
    clientAccessRepository.save(
        new ClientAccess(UUID.randomUUID(), user.getUserId(), client.getId(), AccessLevel.READ_WRITE));
    graphSyncService.syncClient(client);
    auditService.record(
        user, client.getId(), "CLIENT_CREATE", "CLIENT", client.getId().toString(), client.getDisplayName(), http);
    return toDto(client);
  }

  @Transactional(readOnly = true)
  public ClientWorkspaceDto workspace(PieUserPrincipal user, UUID clientId, HttpServletRequest http) {
    Client client = authorizationService.requireAccessibleClient(user, clientId);
    auditService.record(user, clientId, "CLIENT_VIEW", "CLIENT", clientId.toString(), null, http);

    List<AccountDto> accounts =
        accountRepository.findByClientIdOrderByNameAsc(clientId).stream().map(this::toAccountDto).toList();
    List<DocumentDto> documents =
        documentRepository.findByClientIdOrderByCreatedAtDesc(clientId).stream()
            .map(this::toDocumentDto)
            .toList();
    List<HoldingDto> latestHoldings = List.of();
    var dates = holdingRepository.findDistinctAsOfDates(clientId);
    if (!dates.isEmpty()) {
      latestHoldings =
          holdingRepository.findByClientIdAndAsOfDate(clientId, dates.get(0)).stream()
              .map(this::toHoldingDto)
              .toList();
    }

    return new ClientWorkspaceDto(
        toDto(client),
        accounts,
        documents,
        dates,
        latestHoldings,
        "PIE surfaces information and evidence. The advisor remains responsible for interpretation and decisions.");
  }

  private ClientDto toDto(Client client) {
    return new ClientDto(
        client.getId(),
        client.getDisplayName(),
        client.getExternalRef(),
        client.getCreatedAt(),
        client.getUpdatedAt());
  }

  private AccountDto toAccountDto(Account account) {
    return new AccountDto(account.getId(), account.getName(), account.getAccountType());
  }

  private DocumentDto toDocumentDto(Document document) {
    return new DocumentDto(
        document.getId(),
        document.getFilename(),
        document.getDocType().name(),
        document.getContentType(),
        document.getAsOfDate(),
        document.getCreatedAt());
  }

  private HoldingDto toHoldingDto(Holding holding) {
    return new HoldingDto(
        holding.getId(),
        holding.getAccountName(),
        holding.getTicker(),
        holding.getSecurityName(),
        holding.getQuantity(),
        holding.getPrice(),
        holding.getMarketValue(),
        holding.getAssetClass(),
        holding.getAsOfDate());
  }
}
