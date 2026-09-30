package com.pie.audit;

import com.pie.domain.AuditEvent;
import com.pie.repository.AuditEventRepository;
import com.pie.security.PieUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

  private final AuditEventRepository auditEventRepository;

  public AuditService(AuditEventRepository auditEventRepository) {
    this.auditEventRepository = auditEventRepository;
  }

  @Transactional
  public void record(
      PieUserPrincipal user,
      UUID clientId,
      String action,
      String resourceType,
      String resourceId,
      String detail,
      HttpServletRequest request) {
    String ip = request != null ? request.getRemoteAddr() : null;
    AuditEvent event =
        new AuditEvent(
            UUID.randomUUID(),
            user != null ? user.getTenantId() : null,
            user != null ? user.getUserId() : null,
            clientId,
            action,
            resourceType,
            resourceId,
            detail,
            ip);
    auditEventRepository.save(event);
  }
}
