package com.pie.web;

import com.pie.ask.AskPieService;
import com.pie.security.SecurityUtils;
import com.pie.web.dto.AskRequest;
import com.pie.web.dto.AskResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clients/{clientId}/ask")
public class AskController {

  private final AskPieService askPieService;

  public AskController(AskPieService askPieService) {
    this.askPieService = askPieService;
  }

  @PostMapping
  public AskResponse ask(
      @PathVariable UUID clientId,
      @Valid @RequestBody AskRequest request,
      HttpServletRequest http) {
    return askPieService.ask(SecurityUtils.currentUser(), clientId, request, http);
  }
}
