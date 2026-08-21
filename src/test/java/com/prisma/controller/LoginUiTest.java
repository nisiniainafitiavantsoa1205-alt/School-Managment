package com.prisma.controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Disabled;

@Disabled("Nécessite Monocle ou une session graphique X11 active")
public class LoginUiTest extends ApplicationTest {

    @BeforeAll
    public static void setupHeadless() {
        try {
            System.setProperty("testfx.robot", "glass");
            System.setProperty("testfx.headless", "true");
            System.setProperty("prism.order", "sw");
            System.setProperty("prism.text", "t2k");
            System.setProperty("java.awt.headless", "true");
        } catch (Exception e) {
            org.junit.jupiter.api.Assumptions.abort("Test FX UI non supporté sans Monocle.");
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 900, 620);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    public void testComposantsConnexionPresents() {
        // Act & Assert
        TextField usernameField = lookup("#usernameField").queryAs(TextField.class);
        assertNotNull(usernameField, "Le champ Nom d'utilisateur doit exister");
        assertEquals("", usernameField.getText(), "Le champ doit être vide initialement");

        PasswordField passwordField = lookup("#passwordField").queryAs(PasswordField.class);
        assertNotNull(passwordField, "Le champ Mot de passe doit exister");

        Button loginButton = lookup("#loginButton").queryAs(Button.class);
        assertNotNull(loginButton, "Le bouton de connexion doit exister");
        assertEquals("SE CONNECTER", loginButton.getText());

        Label errorLabel = lookup("#errorLabel").queryAs(Label.class);
        assertNotNull(errorLabel, "Le label d'erreur doit exister");
        assertFalse(errorLabel.isVisible(), "Le label d'erreur doit être masqué au démarrage");
    }
}
