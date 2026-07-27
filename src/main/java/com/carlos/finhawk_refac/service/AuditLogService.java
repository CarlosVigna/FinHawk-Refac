package com.carlos.finhawk_refac.service;

import com.carlos.finhawk_refac.entity.AuditLog;
import com.carlos.finhawk_refac.entity.UserAccount;
import com.carlos.finhawk_refac.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// Log basico de quem fez o que -- registrado na MESMA transacao da acao
// que est'a sendo auditada de proposito: se o registro de auditoria falhar,
// a acao inteira deve falhar junto, senao o log fica incompleto/nao confiavel.
@Service
public class AuditLogService {

    public static final String CREATE = "CREATE";
    public static final String UPDATE = "UPDATE";
    public static final String DELETE = "DELETE";

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(UserAccount user, String action, String entityType, Long entityId, String details) {
        AuditLog log = new AuditLog();
        log.setUserId(user.getId());
        log.setUserEmail(user.getEmail());
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(details);
        log.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(log);
    }
}
