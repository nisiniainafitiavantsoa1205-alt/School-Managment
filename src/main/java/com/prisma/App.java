package com.prisma;

import com.prisma.database.DatabaseConnectionManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App extends Application {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Démarrage de l'application PRISMA...");

            // 1. Charger et afficher le Splash Screen (fenêtre sans bordure)
            FXMLLoader splashLoader = new FXMLLoader(getClass().getResource("/fxml/splash.fxml"));
            Parent splashRoot = splashLoader.load();
            
            Stage splashStage = new Stage(javafx.stage.StageStyle.UNDECORATED);
            splashStage.setScene(new Scene(splashRoot));
            splashStage.centerOnScreen();
            splashStage.show();

            // Obtenir les références aux contrôles FXML du Splash
            ProgressBar progressBar = (ProgressBar) splashRoot.lookup("#progressBar");
            Label lblStatus = (Label) splashRoot.lookup("#lblStatus");

            // 2. Tâche d'initialisation en arrière-plan
            Task<Void> initTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    updateProgress(10, 100);
                    updateMessage("Connexion à la base de données locale (H2)...");
                    
                    // Initialiser la session Hibernate
                    DatabaseConnectionManager.getSessionFactory();
                    
                    updateProgress(50, 100);
                    updateMessage("Chargement des configurations système...");
                    Thread.sleep(800); // Petite pause pour fluidité visuelle
                    
                    updateProgress(90, 100);
                    updateMessage("Initialisation de l'interface utilisateur...");
                    Thread.sleep(400);
                    
                    updateProgress(100, 100);
                    updateMessage("Prêt !");
                    return null;
                }
            };

            // Lier les propriétés de la tâche aux composants de la vue
            progressBar.progressProperty().bind(initTask.progressProperty());
            lblStatus.textProperty().bind(initTask.messageProperty());

            initTask.setOnSucceeded(event -> {
                try {
                    // Fermer le Splash Screen
                    splashStage.close();

                    // Ouvrir la page de Connexion
                    FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
                    Parent loginRoot = loginLoader.load();

                    Scene scene = new Scene(loginRoot, 900, 620);
                    primaryStage.setTitle("PRISMA School Management — Connexion");
                    primaryStage.setScene(scene);
                    primaryStage.setResizable(true);
                    primaryStage.centerOnScreen();
                    primaryStage.show();

                    logger.info("Interface de connexion affichée après Splash Screen.");
                } catch (Exception e) {
                    logger.error("Erreur lors du passage à la connexion", e);
                }
            });

            initTask.setOnFailed(event -> {
                splashStage.close();
                logger.error("Échec de l'initialisation", initTask.getException());
                
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur Critique");
                alert.setHeaderText("Impossible d'initialiser l'application");
                alert.setContentText("Détails : " + initTask.getException().getMessage());
                alert.showAndWait();
                
                Platform.exit();
            });

            new Thread(initTask).start();

        } catch (Exception e) {
            logger.error("Erreur critique lors du démarrage de l'application", e);
        }
    }

    @Override
    public void stop() {
        // Fermeture propre de la connexion Hibernate à la sortie de l'application
        logger.info("Fermeture de l'application PRISMA...");
        DatabaseConnectionManager.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

