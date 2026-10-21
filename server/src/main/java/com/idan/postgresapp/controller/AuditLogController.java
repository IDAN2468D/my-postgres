package com.idan.postgresapp.controller;

import com.idan.postgresapp.model.AuditLog;
import com.idan.postgresapp.repository.AuditLogRepository;
import com.idan.postgresapp.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/audit")
public class AuditLogController {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAuditLogs(
            @RequestParam(required = false) String entityType) {
        Map<String, Object> response = new HashMap<>();
        List<AuditLog> logs;
        if (entityType != null && !entityType.isBlank() && !"ALL".equalsIgnoreCase(entityType)) {
            logs = auditLogRepository.findTop100ByEntityTypeIgnoreCaseOrderByCreatedAtDesc(entityType.trim());
        } else {
            logs = auditLogRepository.findTop100ByOrderByCreatedAtDesc();
        }
        response.put("status", "success");
        response.put("data", logs);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Object>> clearAuditLogs() {
        Map<String, Object> response = new HashMap<>();
        auditLogRepository.deleteAll();
        auditLogService.log("CLEAR_AUDIT_LOGS", "SYSTEM", null, "יומן הפעילות נוקה על ידי המשתמש");
        response.put("status", "success");
        response.put("message", "יומן הפעילות נוקה בהצלחה");
        return ResponseEntity.ok(response);
    }
}
