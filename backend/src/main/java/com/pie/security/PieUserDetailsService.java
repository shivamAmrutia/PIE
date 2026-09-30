package com.pie.security;

import com.pie.domain.UserAccount;
import com.pie.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class PieUserDetailsService implements UserDetailsService {

  private final UserAccountRepository userAccountRepository;

  public PieUserDetailsService(UserAccountRepository userAccountRepository) {
    this.userAccountRepository = userAccountRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    UserAccount user =
        userAccountRepository
            .findByEmailIgnoreCase(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    return toPrincipal(user);
  }

  public PieUserPrincipal toPrincipal(UserAccount user) {
    return new PieUserPrincipal(
        user.getId(),
        user.getTenantId(),
        user.getEmail(),
        user.getPasswordHash(),
        user.getDisplayName(),
        user.getRole(),
        user.isActive());
  }
}
