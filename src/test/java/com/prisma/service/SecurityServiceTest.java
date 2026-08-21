package com.prisma.service;

import com.prisma.entity.Role;
import com.prisma.entity.Utilisateur;
import com.prisma.exception.PrismaException;
import com.prisma.repository.UtilisateurRepository;
import com.prisma.security.PasswordHasher;
import com.prisma.security.SessionContext;
import com.prisma.service.impl.SecurityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;

    private SecurityService securityService;
    private SessionContext sessionContext;

    @BeforeEach
    void setUp() {
        // Utiliser une instance propre (non-singleton) pour les tests
        sessionContext = SessionContext.getInstance();
        sessionContext.deconnecter(); // Réinitialiser la session avant chaque test
        securityService = new SecurityServiceImpl(utilisateurRepository, sessionContext);
    }

    @Test
    void connecter_devrait_reussir_avec_bons_identifiants() {
        // Arrange
        Utilisateur utilisateur = creerUtilisateurAdmin("admin123");
        when(utilisateurRepository.findByUsername("admin"))
                .thenReturn(Optional.of(utilisateur));

        // Act
        Utilisateur resultat = securityService.connecter("admin", "admin123");

        // Assert
        assertNotNull(resultat);
        assertEquals("admin", resultat.getUsername());
        assertTrue(sessionContext.estConnecte());
        assertEquals("admin", sessionContext.getUtilisateurConnecte().getUsername());
    }

    @Test
    void connecter_devrait_echouer_avec_mauvais_mot_de_passe() {
        // Arrange
        Utilisateur utilisateur = creerUtilisateurAdmin("admin123");
        when(utilisateurRepository.findByUsername("admin"))
                .thenReturn(Optional.of(utilisateur));

        // Act & Assert
        assertThrows(PrismaException.class,
                () -> securityService.connecter("admin", "mauvais_mot_de_passe"));
        assertFalse(sessionContext.estConnecte());
    }

    @Test
    void connecter_devrait_echouer_si_utilisateur_inexistant() {
        // Arrange
        when(utilisateurRepository.findByUsername("inconnu"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(PrismaException.class,
                () -> securityService.connecter("inconnu", "n'importe quoi"));
    }

    @Test
    void connecter_devrait_echouer_si_compte_desactive() {
        // Arrange
        Utilisateur utilisateur = creerUtilisateurAdmin("admin123");
        utilisateur.setActive(false);
        when(utilisateurRepository.findByUsername("admin"))
                .thenReturn(Optional.of(utilisateur));

        // Act & Assert
        PrismaException ex = assertThrows(PrismaException.class,
                () -> securityService.connecter("admin", "admin123"));
        assertTrue(ex.getMessage().contains("désactivé"));
    }

    @Test
    void connecter_devrait_rejeter_identifiants_vides() {
        assertThrows(PrismaException.class, () -> securityService.connecter("", "pass"));
        assertThrows(PrismaException.class, () -> securityService.connecter("admin", ""));
        assertThrows(PrismaException.class, () -> securityService.connecter(null, "pass"));
    }

    @Test
    void deconnecter_devrait_vider_la_session() {
        // Arrange : simuler une session connectée
        Utilisateur utilisateur = creerUtilisateurAdmin("admin123");
        sessionContext.connecter(utilisateur);
        assertTrue(sessionContext.estConnecte());

        // Act
        securityService.deconnecter();

        // Assert
        assertFalse(sessionContext.estConnecte());
    }

    @Test
    void verifierAcces_devrait_accepter_le_role_correct() {
        // Arrange
        Utilisateur utilisateur = creerUtilisateurAdmin("admin123");
        sessionContext.connecter(utilisateur);

        // Act & Assert — ne doit pas lever d'exception
        assertDoesNotThrow(() -> securityService.verifierAcces("ADMINISTRATEUR"));
    }

    @Test
    void verifierAcces_devrait_refuser_un_role_insuffisant() {
        // Arrange
        Utilisateur utilisateur = creerUtilisateurAdmin("admin123");
        sessionContext.connecter(utilisateur);

        // Act & Assert — rôle PROFESSEUR n'a pas accès à ADMINISTRATEUR
        assertThrows(PrismaException.class,
                () -> securityService.verifierAcces("DIRECTEUR", "PROFESSEUR"));
    }

    // --- Méthode utilitaire ---
    private Utilisateur creerUtilisateurAdmin(String motDePasse) {
        Role role = new Role();
        role.setId(1);
        role.setNom("ADMINISTRATEUR");

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(1);
        utilisateur.setUsername("admin");
        utilisateur.setPasswordHash(PasswordHasher.hacher(motDePasse));
        utilisateur.setEmail("admin@prisma.com");
        utilisateur.setActive(true);
        utilisateur.setRole(role);
        return utilisateur;
    }
}
