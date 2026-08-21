package com.prisma.service;

/**
 * Service de journalisation d'audit pour tracer toutes les actions critiques.
 * Chaque appel génère une entrée dans la table {@code journaux}.
 */
public interface AuditLogService {
    void logConnexion(String username);
    void logDeconnexion(String username);
    void logAction(String action, String details);
}
