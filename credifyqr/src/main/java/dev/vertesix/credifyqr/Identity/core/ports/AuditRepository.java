package dev.vertesix.credifyqr.Identity.core.ports;

import dev.vertesix.credifyqr.Identity.core.domain.AuditLog;

// Defined the port for audit log persistence
public interface AuditRepository {
    void save(AuditLog log);
}