package com.idan.postgresapp.service;

import com.idan.postgresapp.model.AuditLog;
import com.idan.postgresapp.repository.AuditLogRepository;
import com.idan.postgresapp.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    public void log(String action, String entityType, Long entityId, String details) {
        String actor = getCurrentActor();
        logWithActor(action, entityType, entityId, details, actor);
    }

    public void logWithActor(String action, String entityType, Long entityId, String details, String actor) {
        try {
            AuditLog entry = new AuditLog(
                    action,
                    entityType,
                    entityId,
                    details,
                    (actor != null && !actor.isBlank()) ? actor : "מערכת"
            );
            auditLogRepository.save(entry);
        } catch (Exception ignored) {
            // Do not break business transactions if audit logging fails
        }
    }

    public String getCurrentActor() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserDetailsImpl userDetails) {
                return userDetails.getName() + " (" + userDetails.getEmail() + ")";
            }
        } catch (Exception ignored) {}
        return "מערכת / אורח";
    }
}
