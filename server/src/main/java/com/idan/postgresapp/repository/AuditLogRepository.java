package com.idan.postgresapp.repository;

import com.idan.postgresapp.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findTop100ByOrderByCreatedAtDesc();
    List<AuditLog> findTop100ByEntityTypeIgnoreCaseOrderByCreatedAtDesc(String entityType);
}
