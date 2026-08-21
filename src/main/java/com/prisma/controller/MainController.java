package com.prisma.controller;

import com.prisma.security.SessionContext;
import com.prisma.service.AuditLogService;
import com.prisma.service.impl.AuditLogServiceImpl;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.GridPane;

import java.io.IOException;

/**
 * Contrôleur principal de l'application. Gère la navigation,
 * la barre latérale dynamique et la déconnexion.
 */
public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @FXML private BorderPane rootPane;
    @FXML private TextField txtGlobalSearch;

    @FXML private Button btnDashboard;
    @FXML private Button btnEleves;
    @FXML private Button btnClasses;
    @FXML private Button btnNotes;
    @FXML private Button btnBulletins;
    @FXML private Button btnConfig;

    @FXML private Label lblUsername;
    @FXML private Label lblRole;
    @FXML private Label lblBreadcrumb;
    @FXML private Label lblPageTitle;
    @FXML private StackPane contentArea;

    private final SessionContext sessionContext;
    private final AuditLogService auditLogService;
    private final com.prisma.repository.UtilisateurRepository utilisateurRepository;
    private String currentFxmlPath;

    public MainController() {
        this.sessionContext = SessionContext.getInstance();
        this.auditLogService = new AuditLogServiceImpl();
        this.utilisateurRepository = new com.prisma.repository.impl.UtilisateurRepositoryImpl();
    }

    @FXML
    public void initialize() {
        // Initialiser les informations de l'utilisateur connecté
        if (sessionContext.estConnecte()) {
            lblUsername.setText(sessionContext.getUtilisateurConnecte().getUsername());
            lblRole.setText(sessionContext.getUtilisateurConnecte().getRole().getNom());
        }

        // Appliquer les autorisations de visibilité basées sur les rôles (Étape 46)
        appliquerPermissions();

        // Configurer les raccourcis clavier globaux après le chargement de la Scene (Étape 53)
        Platform.runLater(() -> {
            if (rootPane.getScene() != null) {
                rootPane.getScene().setOnKeyPressed(event -> {
                    switch (event.getCode()) {
                        case F11:
                            event.consume();
                            toggleFullScreen();
                            break;
                        case F5:
                            event.consume();
                            rafraichirVueCourante();
                            break;
                        case F:
                            if (event.isControlDown()) {
                                event.consume();
                                txtGlobalSearch.requestFocus();
                                txtGlobalSearch.selectAll();
                            }
                            break;
                        case N:
                            if (event.isControlDown()) {
                                event.consume();
                                handleNavEleves();
                            }
                            break;
                    }
                });
            }
        });

        // Charger la vue par défaut (Tableau de bord)
        handleNavDashboard();
    }

    @FXML
    private void handleGlobalSearch() {
        String query = txtGlobalSearch.getText().trim().toLowerCase();
        txtGlobalSearch.clear();
        if (query.isEmpty()) return;

        Stage stage = (Stage) rootPane.getScene().getWindow();

        if (query.contains("élè") || query.contains("eleve")) {
            handleNavEleves();
            com.prisma.util.NotificationUtil.showSuccess(stage, "Navigation : Gestion des élèves");
        } else if (query.contains("class") || query.contains("coef")) {
            if (btnClasses.isVisible()) {
                handleNavClasses();
                com.prisma.util.NotificationUtil.showSuccess(stage, "Navigation : Classes & Coefficients");
            } else {
                com.prisma.util.NotificationUtil.showError(stage, "Accès refusé pour votre rôle.");
            }
        } else if (query.contains("note")) {
            handleNavNotes();
            com.prisma.util.NotificationUtil.showSuccess(stage, "Navigation : Saisie des notes");
        } else if (query.contains("bulletin") || query.contains("rang") || query.contains("moy")) {
            if (btnBulletins.isVisible()) {
                handleNavBulletins();
                com.prisma.util.NotificationUtil.showSuccess(stage, "Navigation : Bulletins & Rangs");
            } else {
                com.prisma.util.NotificationUtil.showError(stage, "Accès refusé pour votre rôle.");
            }
        } else if (query.contains("param") || query.contains("log") || query.contains("config") || query.contains("sauv")) {
            if (btnConfig.isVisible()) {
                handleNavConfig();
                com.prisma.util.NotificationUtil.showSuccess(stage, "Navigation : Paramètres & Logs");
            } else {
                com.prisma.util.NotificationUtil.showError(stage, "Accès refusé pour votre rôle.");
            }
        } else if (query.contains("accueil") || query.contains("board") || query.contains("dash")) {
            handleNavDashboard();
            com.prisma.util.NotificationUtil.showSuccess(stage, "Navigation : Tableau de bord");
        } else {
            com.prisma.util.NotificationUtil.showError(stage, "Raccourci non reconnu : '" + query + "'");
        }
    }

    private void rafraichirVueCourante() {
        if (currentFxmlPath != null) {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            changerVue(currentFxmlPath, lblPageTitle.getText(), lblBreadcrumb.getText(), null);
            com.prisma.util.NotificationUtil.showSuccess(stage, "Affichage actualisé (F5)");
        }
    }

    @FXML
    private void handleNavDashboard() {
        changerVue("/fxml/dashboard.fxml", "Tableau de bord", "Accueil / Tableau de bord", btnDashboard);
    }

    @FXML
    private void handleNavEleves() {
        changerVue("/fxml/eleves.fxml", "Gestion des Élèves", "Accueil / Élèves", btnEleves);
    }

    @FXML
    private void handleNavClasses() {
        changerVue("/fxml/classes.fxml", "Classes & Coefficients", "Accueil / Structure / Classes", btnClasses);
    }

    @FXML
    private void handleNavNotes() {
        changerVue("/fxml/notes.fxml", "Saisie des Notes & Trimestres", "Accueil / Notes", btnNotes);
    }

    @FXML
    private void handleNavBulletins() {
        changerVue("/fxml/bulletins.fxml", "Bulletins & Rangs", "Accueil / Évaluation / Bulletins", btnBulletins);
    }

    @FXML
    private void handleNavConfig() {
        changerVue("/fxml/config.fxml", "Paramètres & Logs d'audit", "Accueil / Système", btnConfig);
    }

    @FXML
    private void handleDeconnexion() {
        if (sessionContext.estConnecte()) {
            auditLogService.logDeconnexion(sessionContext.getUtilisateurConnecte().getUsername());
        }
        sessionContext.deconnecter();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnDashboard.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 620));
            stage.setTitle("PRISMA School Management — Connexion");
            stage.setResizable(false);
            stage.setMaximized(false);
            stage.centerOnScreen();
        } catch (IOException e) {
            logger.error("Impossible de retourner à la page de connexion", e);
        }
    }

    @FXML
    private void toggleFullScreen() {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        if (stage != null) {
            stage.setFullScreen(!stage.isFullScreen());
        }
    }

    @FXML
    private void handleEditProfile() {
        if (!sessionContext.estConnecte()) return;
        com.prisma.entity.Utilisateur currentUser = sessionContext.getUtilisateurConnecte();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier mon profil");
        dialog.setHeaderText("Modifier mon nom d'utilisateur et/ou mon mot de passe");

        Stage stage = (Stage) rootPane.getScene().getWindow();
        if (stage != null) {
            dialog.initOwner(stage);
        }

        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 20, 10, 10));

        TextField usernameInput = new TextField(currentUser.getUsername());
        PasswordField passInput = new PasswordField();
        passInput.setPromptText("Nouveau mot de passe (optionnel)");
        PasswordField confirmPassInput = new PasswordField();
        confirmPassInput.setPromptText("Confirmer le mot de passe");

        grid.add(new Label("Identifiant :"), 0, 0);
        grid.add(usernameInput, 1, 0);
        grid.add(new Label("Nouveau mot de passe :"), 0, 1);
        grid.add(passInput, 1, 1);
        grid.add(new Label("Confirmation :"), 0, 2);
        grid.add(confirmPassInput, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(response -> {
            if (response == saveButtonType) {
                String newUsername = usernameInput.getText().trim();
                String newPassword = passInput.getText().trim();
                String confirmPassword = confirmPassInput.getText().trim();

                if (newUsername.isEmpty()) {
                    com.prisma.util.NotificationUtil.showError(stage, "Le nom d'utilisateur ne peut pas être vide.");
                    return;
                }

                if (!newPassword.isEmpty() && !newPassword.equals(confirmPassword)) {
                    com.prisma.util.NotificationUtil.showError(stage, "Les mots de passe ne correspondent pas.");
                    return;
                }

                try {
                    currentUser.setUsername(newUsername);
                    if (!newPassword.isEmpty()) {
                        currentUser.setPasswordHash(com.prisma.security.PasswordHasher.hacher(newPassword));
                    }
                    utilisateurRepository.saveOrUpdate(currentUser);

                    lblUsername.setText(newUsername);
                    com.prisma.util.NotificationUtil.showSuccess(stage, "Profil mis à jour avec succès !");
                    logger.info("Profil utilisateur mis à jour : {}", newUsername);
                } catch (Exception e) {
                    logger.error("Erreur lors de la mise à jour du profil", e);
                    com.prisma.util.NotificationUtil.showError(stage, "Erreur lors de la mise à jour (l'identifiant est peut-être déjà utilisé).");
                }
            }
        });
    }

    /**
     * Charge dynamiquement un fichier FXML dans le conteneur central
     * et met à jour l'état actif des boutons de la Sidebar.
     */
    private void changerVue(String fxmlPath, String titre, String breadcrumb, Button activeButton) {
        try {
            logger.info("Chargement de la vue : {}", fxmlPath);
            currentFxmlPath = fxmlPath;
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            contentArea.getChildren().clear();
            contentArea.getChildren().add(root);

            lblPageTitle.setText(titre);
            lblBreadcrumb.setText(breadcrumb);

            // Mettre à jour l'état CSS des boutons de la Sidebar
            resetActiveButtons();
            if (activeButton != null) {
                activeButton.getStyleClass().add("sidebar-btn-active");
            }
        } catch (IOException e) {
            logger.error("Erreur lors du chargement de la sous-vue : {}", fxmlPath, e);
            afficherErreurChargement(titre);
        }
    }

    private void resetActiveButtons() {
        btnDashboard.getStyleClass().remove("sidebar-btn-active");
        btnEleves.getStyleClass().remove("sidebar-btn-active");
        btnClasses.getStyleClass().remove("sidebar-btn-active");
        btnNotes.getStyleClass().remove("sidebar-btn-active");
        btnBulletins.getStyleClass().remove("sidebar-btn-active");
        btnConfig.getStyleClass().remove("sidebar-btn-active");
    }

    private void appliquerPermissions() {
        if (!sessionContext.estConnecte()) return;

        // Rôles attendus : ADMINISTRATEUR, DIRECTEUR, PROFESSEUR
        if (sessionContext.estProfesseur()) {
            // Un professeur n'a pas accès aux configs globales, ni à la configuration des classes, ni à l'administration des bulletins
            btnConfig.setVisible(false);
            btnConfig.setManaged(false);

            btnClasses.setVisible(false);
            btnClasses.setManaged(false);

            btnBulletins.setVisible(false);
            btnBulletins.setManaged(false);
        } else if (sessionContext.estDirecteur()) {
            // Le directeur a accès à tout sauf aux configurations système / journaux
            btnConfig.setVisible(false);
            btnConfig.setManaged(false);
        }
    }

    private void afficherErreurChargement(String titrePage) {
        contentArea.getChildren().clear();
        Label errLabel = new Label("Impossible de charger la page : " + titrePage + "\nVeuillez réessayer ultérieurement.");
        errLabel.setStyle("-fx-text-fill: #FF4D4D; -fx-font-size: 16px; -fx-font-weight: bold; -fx-alignment: center;");
        contentArea.getChildren().add(errLabel);
    }
}
