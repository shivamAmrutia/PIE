package com.pie.security;

import com.pie.config.PieProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

  private final SecretKey key;
  private final long expirationMs;

  public JwtService(PieProperties properties) {
    this.key = Keys.hmacShaKeyFor(properties.jwt().secret().getBytes(StandardCharsets.UTF_8));
    this.expirationMs = properties.jwt().expirationMs();
  }

  public String createToken(PieUserPrincipal principal) {
    Date now = new Date();
    Date exp = new Date(now.getTime() + expirationMs);
    return Jwts.builder()
        .subject(principal.getUserId().toString())
        .claim("email", principal.getUsername())
        .claim("tenantId", principal.getTenantId().toString())
        .claim("role", principal.getRole().name())
        .claim("displayName", principal.getDisplayName())
        .issuedAt(now)
        .expiration(exp)
        .signWith(key)
        .compact();
  }

  public Claims parse(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }

  public UUID userId(Claims claims) {
    return UUID.fromString(claims.getSubject());
  }
}
