package com.pie.security;

import com.pie.domain.UserRole;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class PieUserPrincipal implements UserDetails {

  private final UUID userId;
  private final UUID tenantId;
  private final String email;
  private final String passwordHash;
  private final String displayName;
  private final UserRole role;
  private final boolean active;

  public PieUserPrincipal(
      UUID userId,
      UUID tenantId,
      String email,
      String passwordHash,
      String displayName,
      UserRole role,
      boolean active) {
    this.userId = userId;
    this.tenantId = tenantId;
    this.email = email;
    this.passwordHash = passwordHash;
    this.displayName = displayName;
    this.role = role;
    this.active = active;
  }

  public UUID getUserId() {
    return userId;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public String getDisplayName() {
    return displayName;
  }

  public UserRole getRole() {
    return role;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
  }

  @Override
  public String getPassword() {
    return passwordHash;
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return active;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return active;
  }
}
