package com.pie.config;

import com.pie.domain.AccessLevel;
import com.pie.domain.Account;
import com.pie.domain.Client;
import com.pie.domain.ClientAccess;
import com.pie.domain.Tenant;
import com.pie.domain.UserAccount;
import com.pie.domain.UserRole;
import com.pie.graph.ClientGraphSyncService;
import com.pie.repository.AccountRepository;
import com.pie.repository.ClientAccessRepository;
import com.pie.repository.ClientRepository;
import com.pie.repository.TenantRepository;
import com.pie.repository.UserAccountRepository;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoDataSeeder implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

  public static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  public static final UUID ADVISOR_ID = UUID.fromString("22222222-2222-2222-2222-222222222201");
  public static final UUID CSA_ID = UUID.fromString("22222222-2222-2222-2222-222222222202");
  public static final UUID OUTSIDER_ID = UUID.fromString("22222222-2222-2222-2222-222222222203");
  public static final UUID JOHN_SMITH_ID = UUID.fromString("33333333-3333-3333-3333-333333333301");

  private final TenantRepository tenantRepository;
  private final UserAccountRepository userAccountRepository;
  private final ClientRepository clientRepository;
  private final ClientAccessRepository clientAccessRepository;
  private final AccountRepository accountRepository;
  private final PasswordEncoder passwordEncoder;
  private final ClientGraphSyncService graphSyncService;

  public DemoDataSeeder(
      TenantRepository tenantRepository,
      UserAccountRepository userAccountRepository,
      ClientRepository clientRepository,
      ClientAccessRepository clientAccessRepository,
      AccountRepository accountRepository,
      PasswordEncoder passwordEncoder,
      ClientGraphSyncService graphSyncService) {
    this.tenantRepository = tenantRepository;
    this.userAccountRepository = userAccountRepository;
    this.clientRepository = clientRepository;
    this.clientAccessRepository = clientAccessRepository;
    this.accountRepository = accountRepository;
    this.passwordEncoder = passwordEncoder;
    this.graphSyncService = graphSyncService;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (tenantRepository.existsById(TENANT_ID)) {
      clientRepository.findById(JOHN_SMITH_ID).ifPresent(graphSyncService::syncClient);
      accountRepository.findByClientIdOrderByNameAsc(JOHN_SMITH_ID).forEach(graphSyncService::syncAccount);
      log.info("Demo data already present — refreshed Neo4j client/account nodes");
      return;
    }

    String hash = passwordEncoder.encode("password");
    tenantRepository.save(new Tenant(TENANT_ID, "PIE Demo Wealth"));

    userAccountRepository.save(
        new UserAccount(ADVISOR_ID, TENANT_ID, "advisor@pie.demo", hash, "Alex Advisor", UserRole.ADVISOR));
    userAccountRepository.save(
        new UserAccount(CSA_ID, TENANT_ID, "csa@pie.demo", hash, "Casey CSA", UserRole.CSA));
    userAccountRepository.save(
        new UserAccount(
            OUTSIDER_ID, TENANT_ID, "outsider@pie.demo", hash, "Ollie Outsider", UserRole.ADVISOR));

    Client john = new Client(JOHN_SMITH_ID, TENANT_ID, "John Smith", "SYNTH-JS-001");
    clientRepository.save(john);
    graphSyncService.syncClient(john);

    clientAccessRepository.save(
        new ClientAccess(UUID.randomUUID(), ADVISOR_ID, JOHN_SMITH_ID, AccessLevel.READ_WRITE));
    clientAccessRepository.save(
        new ClientAccess(UUID.randomUUID(), CSA_ID, JOHN_SMITH_ID, AccessLevel.READ));

    List<Account> accounts =
        List.of(
            new Account(UUID.randomUUID(), JOHN_SMITH_ID, TENANT_ID, "IRA", "IRA"),
            new Account(UUID.randomUUID(), JOHN_SMITH_ID, TENANT_ID, "Taxable Brokerage", "TAXABLE"),
            new Account(UUID.randomUUID(), JOHN_SMITH_ID, TENANT_ID, "401(k)", "401K"));
    accountRepository.saveAll(accounts);
    accounts.forEach(graphSyncService::syncAccount);

    log.info("Seeded demo tenant, users, and John Smith client");
  }
}
