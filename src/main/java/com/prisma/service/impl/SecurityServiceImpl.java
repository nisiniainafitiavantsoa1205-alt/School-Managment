package com.prisma.service.impl;

import com.prisma.entity.Utilisateur;
import com.prisma.exception.PrismaException;
import com.prisma.repository.UtilisateurRepository;
import com.prisma.repository.impl.UtilisateurRepositoryImpl;
import com.prisma.security.PasswordHasher;
import com.prisma.security.SessionContext;
import com.prisma.service.SecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Optional;

public class SecurityServiceImpl implements SecurityService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityServiceImpl.class);
    private final UtilisateurRepository utilisateurRepository;
    private final SessionContext sessionContext;

    public SecurityServiceImpl() {
        this.utilisateurRepository = new UtilisateurRepositoryImpl();
        this.sessionContext = SessionContext.getInstance();
    }

    // Constructeur pour les tests (injection de dépendances)
    public SecurityServiceImpl(UtilisateurRepository utilisateurRepository, SessionContext sessionContext) {
        this.utilisateurRepository = utilisateurRepository;
        this.sessionContext = sessionContext;
    }

    @Override
    public Utilisateur connecter(String username, String motDePasse) {
        if (username == null || username.isBlank()) {
            throw new PrismaException("Le nom d'utilisateur est requis.");
        }
        if (motDePasse == null || motDePasse.isBlank()) {
            throw new PrismaException("Le mot de passe est requis.");
        }

        Utilisateur utilisateur = utilisateurRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.warn("Tentative de connexion échouée pour le compte inexistant: {}", username);
                    return new PrismaException("Nom d'utilisateur ou mot de passe incorrect.");
                });

        if (!utilisateur.isActive()) {
            logger.warn("Tentative de connexion sur un compte désactivé: {}", username);
            throw new PrismaException("Ce compte est désactivé. Contactez l'administrateur.");
        }

        if (!PasswordHasher.verifier(motDePasse, utilisateur.getPasswordHash())) {
            logger.warn("Mot de passe incorrect pour l'utilisateur: {}", username);
            throw new PrismaException("Nom d'utilisateur ou mot de passe incorrect.");
        }

        sessionContext.connecter(utilisateur);
        logger.info("Connexion réussie pour l'utilisateur: {} (rôle: {})",
                username, utilisateur.getRole().getNom());
        return utilisateur;
    }

    @Override
    public void deconnecter() {
        if (sessionContext.estConnecte()) {
            logger.info("Déconnexion de l'utilisateur: {}",
                    sessionContext.getUtilisateurConnecte().getUsername());
        }
        sessionContext.deconnecter();
    }

    @Override
    public Optional<Utilisateur> getUtilisateurConnecte() {
        return Optional.ofNullable(sessionContext.getUtilisateurConnecte());
    }

    @Override
    public void verifierAcces(String... roles) {
        if (!sessionContext.estConnecte()) {
            throw new PrismaException("Accès refusé : aucun utilisateur connecté.");
        }
        String roleUtilisateur = sessionContext.getUtilisateurConnecte().getRole().getNom();
        boolean autorise = Arrays.stream(roles)
                .anyMatch(r -> r.equalsIgnoreCase(roleUtilisateur));
        if (!autorise) {
            logger.warn("Accès refusé pour l'utilisateur {} (rôle: {}). Rôles requis: {}",
                    sessionContext.getUtilisateurConnecte().getUsername(),
                    roleUtilisateur,
                    Arrays.toString(roles));
            throw new PrismaException("Accès refusé : vous n'avez pas les droits suffisants.");
        }
    }
}
