package com.prisma.controller;

import com.prisma.entity.Utilisateur;
import com.prisma.exception.PrismaException;
import com.prisma.service.AuditLogService;
import com.prisma.service.SecurityService;
import com.prisma.service.impl.AuditLogServiceImpl;
import com.prisma.service.impl.SecurityServiceImpl;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contrôleur de la vue de connexion.
 * Gère la validation des champs, l'appel au service de sécurité
 * et la navigation vers l'interface principale après connexion réussie.
 */
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private ProgressIndicator loadingIndicator;

    private final SecurityService securityService;
    private final AuditLogService auditLogService;

    public LoginController() {
        this.securityService = new SecurityServiceImpl();
        this.auditLogService = new AuditLogServiceImpl();
    }

    @FXML
    public void initialize() {
        // Masquer le spinner et le message d'erreur au démarrage
        loadingIndicator.setVisible(false);
        errorLabel.setVisible(false);
        // Mettre le focus sur le champ username au chargement
        Platform.runLater(() -> usernameField.requestFocus());
    }

    @FXML
    private void handleConnexion() {
        String username = usernameField.getText().trim();
        String motDePasse = passwordField.getText();

        // Validation côté client avant l'appel au service
        if (username.isEmpty()) {
            afficherErreur("Veuillez saisir votre nom d'utilisateur.");
            usernameField.requestFocus();
            return;
        }
        if (motDePasse.isEmpty()) {
            afficherErreur("Veuillez saisir votre mot de passe.");
            passwordField.requestFocus();
            return;
        }

        // Désactiver le bouton et afficher le spinner pendant la vérification
        setChargement(true);

        // Exécuter la connexion sur un thread de fond pour ne pas bloquer le thread JavaFX
        Task<Utilisateur> tacheConnexion = new Task<>() {
            @Override
            protected Utilisateur call() {
                return securityService.connecter(username, motDePasse);
            }
        };

        tacheConnexion.setOnSucceeded(event -> {
            setChargement(false);
            Utilisateur utilisateur = tacheConnexion.getValue();
            auditLogService.logConnexion(utilisateur.getUsername());
            logger.info("Connexion réussie : redirection vers le tableau de bord");
            ouvrirTableauDeBord(utilisateur);
        });

        tacheConnexion.setOnFailed(event -> {
            setChargement(false);
            Throwable erreur = tacheConnexion.getException();
            if (erreur instanceof PrismaException) {
                afficherErreur(erreur.getMessage());
            } else {
                afficherErreur("Erreur de connexion à la base de données. Vérifiez votre installation.");
                logger.error("Erreur inattendue lors de la connexion", erreur);
            }
            passwordField.clear();
            passwordField.requestFocus();
        });

        new Thread(tacheConnexion).start();
    }

    private void ouvrirTableauDeBord(Utilisateur utilisateur) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/main_layout.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setResizable(true);
            stage.setScene(new Scene(root));
            stage.setTitle("PRISMA School Management — " + utilisateur.getUsername());
            stage.setMaximized(true);
        } catch (Exception e) {
            logger.error("Impossible de charger le tableau de bord", e);
            afficherErreur("Erreur interne : impossible de charger l'interface principale.");
        }
    }

    private void afficherErreur(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        // Animation de tremblement sur les champs
        animer(usernameField);
        animer(passwordField);
    }

    private void setChargement(boolean chargement) {
        loginButton.setDisable(chargement);
        loadingIndicator.setVisible(chargement);
        errorLabel.setVisible(false);
    }

    /**
     * Applique une animation de secousse horizontale légère sur un nœud
     * pour signaler une erreur de saisie à l'utilisateur.
     */
    private void animer(javafx.scene.Node noeud) {
        javafx.animation.TranslateTransition tt =
                new javafx.animation.TranslateTransition(
                        javafx.util.Duration.millis(60), noeud);
        tt.setCycleCount(4);
        tt.setAutoReverse(true);
        tt.setByX(6);
        tt.play();
    }
}
