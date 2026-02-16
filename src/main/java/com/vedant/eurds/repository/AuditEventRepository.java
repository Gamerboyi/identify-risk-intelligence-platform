package com.vedant.eurds.repository;

import com.vedant.eurds.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    // Get all audit events for a specific user
    List<AuditEvent> findByActorIdOrderByCreatedAtDesc(UUID actorId);

    // Get all events of a specific type — e.g. all HIGH_RISK_DETECTED events
    List<AuditEvent> findByEventTypeOrderByCreatedAtDesc(String eventType);
}