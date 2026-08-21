package com.prisma.service.impl;

import com.prisma.entity.Journal;
import com.prisma.repository.JournalRepository;
import com.prisma.repository.impl.JournalRepositoryImpl;
import com.prisma.security.SessionContext;
import com.prisma.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

public class AuditLogServiceImpl implements AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogServiceImpl.class);
    private final JournalRepository journalRepository;
    private final SessionContext sessionContext;

    public AuditLogServiceImpl() {
        this.journalRepository = new JournalRepositoryImpl();
        this.sessionContext = SessionContext.getInstance();
    }

    public AuditLogServiceImpl(JournalRepository journalRepository, SessionContext sessionContext) {
        this.journalRepository = journalRepository;
        this.sessionContext = sessionContext;
    }

    @Override
    public void logConnexion(String username) {
        enregistrer("CONNEXION", "Connexion de l'utilisateur: " + username);
    }

    @Override
    public void logDeconnexion(String username) {
        enregistrer("DECONNEXION", "Déconnexion de l'utilisateur: " + username);
    }

    @Override
    public void logAction(String action, String details) {
        enregistrer(action, details);
    }

    private void enregistrer(String action, String details) {
        try {
            Journal journal = new Journal();
            journal.setAction(action);
            journal.setDetails(details);
            journal.setDateAction(LocalDateTime.now());
            // Attacher l'utilisateur connecté s'il existe
            if (sessionContext.estConnecte()) {
                journal.setUtilisateur(sessionContext.getUtilisateurConnecte());
            }
            journalRepository.save(journal);
        } catch (Exception e) {
            // La journalisation ne doit jamais interrompre le flux principal
            logger.error("Impossible d'enregistrer l'action d'audit '{}': {}", action, e.getMessage());
        }
    }
}
