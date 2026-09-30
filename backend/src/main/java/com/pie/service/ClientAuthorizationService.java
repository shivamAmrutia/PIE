package com.pie.service;

import com.pie.domain.Client;
import com.pie.domain.ClientAccess;
import com.pie.repository.ClientAccessRepository;
import com.pie.repository.ClientRepository;
import com.pie.security.PieUserPrincipal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ClientAuthorizationService {

  private final ClientRepository clientRepository;
  private final ClientAccessRepository clientAccessRepository;

  public ClientAuthorizationService(
      ClientRepository clientRepository, ClientAccessRepository clientAccessRepository) {
    this.clientRepository = clientRepository;
    this.clientAccessRepository = clientAccessRepository;
  }

  public Client requireAccessibleClient(PieUserPrincipal user, UUID clientId) {
    Client client =
        clientRepository
            .findById(clientId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

    if (!client.getTenantId().equals(user.getTenantId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found");
    }

    boolean allowed = clientAccessRepository.existsByUserIdAndClientId(user.getUserId(), clientId);
    if (!allowed) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No access to this client");
    }
    return client;
  }

  public ClientAccess requireAccessRecord(PieUserPrincipal user, UUID clientId) {
    requireAccessibleClient(user, clientId);
    return clientAccessRepository
        .findByUserIdAndClientId(user.getUserId(), clientId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No access to this client"));
  }
}
