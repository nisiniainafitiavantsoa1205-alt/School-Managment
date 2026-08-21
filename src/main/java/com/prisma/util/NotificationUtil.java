package com.prisma.util;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.scene.control.Label;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Utilitaire pour l'affichage de notifications éphémères (Toasts)
 * modernes et animées sur l'interface JavaFX.
 */
public class NotificationUtil {

    private NotificationUtil() {}

    /**
     * Affiche une notification de succès (verte).
     *
     * @param ownerStage La fenêtre principale hôte
     * @param message    Le message à afficher
     */
    public static void showSuccess(Stage ownerStage, String message) {
        showToast(ownerStage, message, "toast-success");
    }

    /**
     * Affiche une notification d'erreur (rouge).
     *
     * @param ownerStage La fenêtre principale hôte
     * @param message    Le message à afficher
     */
    public static void showError(Stage ownerStage, String message) {
        showToast(ownerStage, message, "toast-error");
    }

    private static void showToast(Stage ownerStage, String message, String cssClass) {
        if (ownerStage == null) return;

        Popup popup = new Popup();
        popup.setAutoFix(true);
        popup.setAutoHide(true);

        Label label = new Label(message);
        label.getStyleClass().add(cssClass);
        
        // Charger la feuille de style globale pour appliquer les styles CSS du Toast
        String cssPath = NotificationUtil.class.getResource("/css/application.css").toExternalForm();
        label.getStylesheets().add(cssPath);

        popup.getContent().add(label);

        // Positionner le toast centré horizontalement en bas de la fenêtre hôte
        popup.setOnShown(e -> {
            popup.setX(ownerStage.getX() + (ownerStage.getWidth() / 2) - (popup.getWidth() / 2));
            popup.setY(ownerStage.getY() + ownerStage.getHeight() - 100);
        });

        // Configurer les transitions animées de fondu (Fade)
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), label);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        PauseTransition delay = new PauseTransition(Duration.millis(2500));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), label);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        SequentialTransition sequence = new SequentialTransition(fadeIn, delay, fadeOut);
        sequence.setOnFinished(e -> popup.hide());

        popup.show(ownerStage);
        sequence.play();
    }
}
