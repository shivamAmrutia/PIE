package com.pie.web;

import com.pie.security.PieUserPrincipal;
import com.pie.security.SecurityUtils;
import com.pie.service.ClientService;
import com.pie.web.dto.ClientCreateRequest;
import com.pie.web.dto.ClientDto;
import com.pie.web.dto.ClientWorkspaceDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

  private final ClientService clientService;

  public ClientController(ClientService clientService) {
    this.clientService = clientService;
  }

  @GetMapping
  public List<ClientDto> list() {
    PieUserPrincipal user = SecurityUtils.currentUser();
    return clientService.listAccessible(user);
  }

  @PostMapping
  public ClientDto create(@Valid @RequestBody ClientCreateRequest request, HttpServletRequest http) {
    return clientService.create(SecurityUtils.currentUser(), request, http);
  }

  @GetMapping("/{clientId}/workspace")
  public ClientWorkspaceDto workspace(@PathVariable UUID clientId, HttpServletRequest http) {
    return clientService.workspace(SecurityUtils.currentUser(), clientId, http);
  }
}
