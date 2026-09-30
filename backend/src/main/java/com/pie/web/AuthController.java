package com.pie.web;

import com.pie.audit.AuditService;
import com.pie.security.JwtService;
import com.pie.security.PieUserDetailsService;
import com.pie.security.PieUserPrincipal;
import com.pie.web.dto.AuthResponse;
import com.pie.web.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final PieUserDetailsService userDetailsService;
  private final AuditService auditService;

  public AuthController(
      AuthenticationManager authenticationManager,
      JwtService jwtService,
      PieUserDetailsService userDetailsService,
      AuditService auditService) {
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
    this.userDetailsService = userDetailsService;
    this.auditService = auditService;
  }

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
    try {
      authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(request.email(), request.password()));
    } catch (AuthenticationException ex) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    PieUserPrincipal principal =
        (PieUserPrincipal) userDetailsService.loadUserByUsername(request.email());
    String token = jwtService.createToken(principal);
    auditService.record(principal, null, "LOGIN", "USER", principal.getUserId().toString(), null, http);
    return new AuthResponse(
        token,
        principal.getUserId(),
        principal.getTenantId(),
        principal.getUsername(),
        principal.getDisplayName(),
        principal.getRole().name());
  }
}
