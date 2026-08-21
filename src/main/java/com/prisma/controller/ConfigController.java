package com.prisma.controller;

import com.prisma.database.DatabaseConnectionManager;
import com.prisma.entity.*;
import com.prisma.repository.*;
import com.prisma.repository.impl.*;
import com.prisma.security.PasswordHasher;
import com.prisma.util.NotificationUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ConfigController {

    private static final Logger logger = LoggerFactory.getLogger(ConfigController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // Paramètres
    @FXML private TextField txtSchoolName;
    @FXML private TextField txtSchoolAddress;
    @FXML private TextField txtSchoolPhone;
    @FXML private TextField txtSchoolYear;

    // Sauvegardes
    @FXML private TableView<Sauvegarde> tblSauvegardes;
    @FXML private TableColumn<Sauvegarde, String> colBackupName;
    @FXML private TableColumn<Sauvegarde, String> colBackupDate;
    @FXML private TableColumn<Sauvegarde, String> colBackupSize;
    @FXML private TableColumn<Sauvegarde, String> colBackupStatus;

    // Logs
    @FXML private TableView<Journal> tblLogs;
    @FXML private TableColumn<Journal, String> colLogDate;
    @FXML private TableColumn<Journal, String> colLogUser;
    @FXML private TableColumn<Journal, String> colLogAction;
    @FXML private TableColumn<Journal, String> colLogDetails;

    // Trimestres
    @FXML private TableView<Periode> tblPeriodes;
    @FXML private TableColumn<Periode, String> colPeriodNom;
    @FXML private TableColumn<Periode, String> colPeriodAnnee;
    @FXML private TableColumn<Periode, String> colPeriodActive;
    @FXML private TableColumn<Periode, String> colPeriodClosed;
    
    @FXML private Label lblPeriodFormTitle;
    @FXML private TextField txtPeriodNom;
    @FXML private TextField txtPeriodAnnee;
    @FXML private CheckBox chkPeriodActive;
    @FXML private CheckBox chkPeriodClosed;

    // Utilisateurs
    @FXML private TableView<Utilisateur> tblUsers;
    @FXML private TableColumn<Utilisateur, String> colUserUsername;
    @FXML private TableColumn<Utilisateur, String> colUserEmail;
    @FXML private TableColumn<Utilisateur, String> colUserRole;
    @FXML private TableColumn<Utilisateur, String> colUserActive;

    @FXML private Label lblUserFormTitle;
    @FXML private Label lblUserPasswordLabel;
    @FXML private TextField txtUserUsername;
    @FXML private PasswordField txtUserPassword;
    @FXML private TextField txtUserEmail;
    @FXML private ComboBox<Role> cmbUserRole;
    @FXML private CheckBox chkUserActive;

    private final ConfigurationRepository configurationRepository;
    private final JournalRepository journalRepository;
    private final PeriodeRepository periodeRepository;
    private final GenericRepository<Sauvegarde, Integer> sauvegardeRepository;
    private final GenericRepository<Utilisateur, Integer> utilisateurRepository;
    private final GenericRepository<Role, Integer> roleRepository;

    private Periode selectedPeriod;
    private Utilisateur selectedUser;

    public ConfigController() {
        this.configurationRepository = new ConfigurationRepositoryImpl();
        this.journalRepository = new JournalRepositoryImpl();
        this.periodeRepository = new PeriodeRepositoryImpl();
        this.sauvegardeRepository = new GenericRepositoryImpl<>(Sauvegarde.class) {};
        this.utilisateurRepository = new GenericRepositoryImpl<>(Utilisateur.class) {};
        this.roleRepository = new GenericRepositoryImpl<>(Role.class) {};
    }

    @FXML
    public void initialize() {
        configurerTables();
        configurerFormulaires();
        chargerParametres();
        chargerHistoriqueSauvegardes();
        chargerPeriodes();
        chargerUtilisateurs();
        chargerRoles();
        handleRefreshLogs();
    }

    private void configurerTables() {
        // Table des journaux d'audit
        colLogDate.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getDateAction().format(DATE_FORMATTER)));
        colLogUser.setCellValueFactory(cellData -> {
            if (cellData.getValue().getUtilisateur() != null) {
                return new SimpleStringProperty(cellData.getValue().getUtilisateur().getUsername());
            }
            return new SimpleStringProperty("Système / Anonyme");
        });
        colLogAction.setCellValueFactory(new PropertyValueFactory<>("action"));
        colLogDetails.setCellValueFactory(new PropertyValueFactory<>("details"));

        // Table des sauvegardes
        colBackupName.setCellValueFactory(new PropertyValueFactory<>("nomFichier"));
        colBackupDate.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getDateSauvegarde().format(DATE_FORMATTER)));
        colBackupSize.setCellValueFactory(cellData -> {
            long size = cellData.getValue().getTailleOctets();
            if (size < 1024) return new SimpleStringProperty(size + " B");
            if (size < 1048576) return new SimpleStringProperty(String.format("%.2f KB", size / 1024.0));
            return new SimpleStringProperty(String.format("%.2f MB", size / 1048576.0));
        });
        colBackupStatus.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // Table des Trimestres
        colPeriodNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPeriodAnnee.setCellValueFactory(new PropertyValueFactory<>("anneeScolaire"));
        colPeriodActive.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().isActive() ? "Oui" : "Non"));
        colPeriodClosed.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().isClosed() ? "Oui" : "Non"));

        tblPeriodes.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedPeriod = newVal;
                lblPeriodFormTitle.setText("Modifier Trimestre");
                txtPeriodNom.setText(newVal.getNom());
                txtPeriodAnnee.setText(newVal.getAnneeScolaire());
                chkPeriodActive.setSelected(newVal.isActive());
                chkPeriodClosed.setSelected(newVal.isClosed());
            }
        });

        // Table des Utilisateurs
        colUserUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colUserEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colUserRole.setCellValueFactory(cellData -> {
            Role role = cellData.getValue().getRole();
            return new SimpleStringProperty(role != null ? role.getNom() : "");
        });
        colUserActive.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().isActive() ? "Oui" : "Non"));

        tblUsers.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedUser = newVal;
                lblUserFormTitle.setText("Modifier Utilisateur");
                lblUserPasswordLabel.setText("Mot de passe (facultatif)");
                txtUserUsername.setText(newVal.getUsername());
                txtUserPassword.clear();
                txtUserEmail.setText(newVal.getEmail());
                cmbUserRole.setValue(newVal.getRole());
                chkUserActive.setSelected(newVal.isActive());
            }
        });
    }

    private void configurerFormulaires() {
        cmbUserRole.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Role r) {
                return r != null ? r.getNom() : "";
            }
            @Override
            public Role fromString(String string) { return null; }
        });
    }

    private void chargerParametres() {
        Task<Void> task = new Task<>() {
            private String schoolName = "";
            private String schoolAddress = "";
            private String schoolPhone = "";
            private String schoolYear = "";

            @Override
            protected Void call() {
                schoolName = configurationRepository.findByCle("school.name").map(Configuration::getValeur).orElse("");
                schoolAddress = configurationRepository.findByCle("school.address").map(Configuration::getValeur).orElse("");
                schoolPhone = configurationRepository.findByCle("school.phone").map(Configuration::getValeur).orElse("");
                schoolYear = configurationRepository.findByCle("school.year").map(Configuration::getValeur).orElse("");
                return null;
            }

            @Override
            protected void succeeded() {
                txtSchoolName.setText(schoolName);
                txtSchoolAddress.setText(schoolAddress);
                txtSchoolPhone.setText(schoolPhone);
                txtSchoolYear.setText(schoolYear);
            }
        };
        new Thread(task).start();
    }

    @FXML
    private void handleSaveSettings() {
        Stage stage = (Stage) txtSchoolName.getScene().getWindow();
        String name = txtSchoolName.getText().trim();
        String address = txtSchoolAddress.getText().trim();
        String phone = txtSchoolPhone.getText().trim();
        String year = txtSchoolYear.getText().trim();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                configurationRepository.saveOrUpdate("school.name", name);
                configurationRepository.saveOrUpdate("school.address", address);
                configurationRepository.saveOrUpdate("school.phone", phone);
                configurationRepository.saveOrUpdate("school.year", year);
                
                Journal log = new Journal();
                log.setAction("PARAMETRES_UPDATE");
                log.setDetails("Mise à jour des paramètres de l'école");
                log.setDateAction(LocalDateTime.now());
                journalRepository.save(log);
                return null;
            }

            @Override
            protected void succeeded() {
                NotificationUtil.showSuccess(stage, "Paramètres enregistrés !");
            }

            @Override
            protected void failed() {
                NotificationUtil.showError(stage, "Erreur de sauvegarde.");
            }
        };
        new Thread(task).start();
    }

    private void chargerPeriodes() {
        Task<List<Periode>> task = new Task<>() {
            @Override
            protected List<Periode> call() {
                return periodeRepository.findAll();
            }

            @Override
            protected void succeeded() {
                tblPeriodes.setItems(FXCollections.observableArrayList(getValue()));
            }
        };
        new Thread(task).start();
    }

    @FXML
    private void handleNewPeriod() {
        selectedPeriod = null;
        lblPeriodFormTitle.setText("Ajouter / Modifier Période");
        txtPeriodNom.clear();
        txtPeriodAnnee.setText(txtSchoolYear.getText());
        chkPeriodActive.setSelected(true);
        chkPeriodClosed.setSelected(false);
        tblPeriodes.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleSavePeriod() {
        Stage stage = (Stage) txtPeriodNom.getScene().getWindow();
        String nom = txtPeriodNom.getText().trim();
        String annee = txtPeriodAnnee.getText().trim();
        boolean active = chkPeriodActive.isSelected();
        boolean closed = chkPeriodClosed.isSelected();

        if (nom.isEmpty() || annee.isEmpty()) {
            NotificationUtil.showError(stage, "Veuillez renseigner le nom et l'année.");
            return;
        }

        Periode p = (selectedPeriod == null) ? new Periode() : selectedPeriod;
        p.setNom(nom);
        p.setAnneeScolaire(annee);
        p.setActive(active);
        p.setClosed(closed);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                // Si on active cette période, s'assurer que toutes les autres de la même année sont désactivées
                if (active) {
                    try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
                        session.beginTransaction();
                        session.createQuery("update Periode set active = false where anneeScolaire = :annee")
                                .setParameter("annee", annee)
                                .executeUpdate();
                        session.getTransaction().commit();
                    }
                }
                periodeRepository.saveOrUpdate(p);
                return null;
            }

            @Override
            protected void succeeded() {
                NotificationUtil.showSuccess(stage, "Période enregistrée !");
                chargerPeriodes();
                handleNewPeriod();
            }

            @Override
            protected void failed() {
                NotificationUtil.showError(stage, "Erreur de sauvegarde du trimestre.");
            }
        };
        new Thread(task).start();
    }

    private void chargerUtilisateurs() {
        Task<List<Utilisateur>> task = new Task<>() {
            @Override
            protected List<Utilisateur> call() {
                try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
                    return session.createQuery("from Utilisateur u join fetch u.role", Utilisateur.class).getResultList();
                }
            }

            @Override
            protected void succeeded() {
                tblUsers.setItems(FXCollections.observableArrayList(getValue()));
            }
        };
        new Thread(task).start();
    }

    private void chargerRoles() {
        Task<List<Role>> task = new Task<>() {
            @Override
            protected List<Role> call() {
                return roleRepository.findAll();
            }

            @Override
            protected void succeeded() {
                cmbUserRole.setItems(FXCollections.observableArrayList(getValue()));
            }
        };
        new Thread(task).start();
    }

    @FXML
    private void handleNewUser() {
        selectedUser = null;
        lblUserFormTitle.setText("Créer / Modifier Utilisateur");
        lblUserPasswordLabel.setText("Mot de passe *");
        txtUserUsername.clear();
        txtUserPassword.clear();
        txtUserEmail.clear();
        cmbUserRole.setValue(null);
        chkUserActive.setSelected(true);
        tblUsers.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleSaveUser() {
        Stage stage = (Stage) txtUserUsername.getScene().getWindow();
        String username = txtUserUsername.getText().trim();
        String password = txtUserPassword.getText().trim();
        String email = txtUserEmail.getText().trim();
        Role role = cmbUserRole.getValue();
        boolean active = chkUserActive.isSelected();

        if (username.isEmpty() || role == null || (selectedUser == null && password.isEmpty())) {
            NotificationUtil.showError(stage, "Veuillez remplir les champs obligatoires (*).");
            return;
        }

        Utilisateur u = (selectedUser == null) ? new Utilisateur() : selectedUser;
        u.setUsername(username);
        u.setEmail(email);
        u.setRole(role);
        u.setActive(active);
        
        if (!password.isEmpty()) {
            u.setPasswordHash(PasswordHasher.hacher(password));
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                utilisateurRepository.saveOrUpdate(u);
                return null;
            }

            @Override
            protected void succeeded() {
                NotificationUtil.showSuccess(stage, "Compte utilisateur enregistré !");
                chargerUtilisateurs();
                handleNewUser();
            }

            @Override
            protected void failed() {
                NotificationUtil.showError(stage, "Identifiant déjà utilisé.");
            }
        };
        new Thread(task).start();
    }

    @FXML
    private void handleRefreshLogs() {
        Task<List<Journal>> task = new Task<>() {
            @Override
            protected List<Journal> call() {
                return journalRepository.findRecentLogs(100);
            }

            @Override
            protected void succeeded() {
                tblLogs.setItems(FXCollections.observableArrayList(getValue()));
            }
        };
        new Thread(task).start();
    }

    private void chargerHistoriqueSauvegardes() {
        Task<List<Sauvegarde>> task = new Task<>() {
            @Override
            protected List<Sauvegarde> call() {
                return sauvegardeRepository.findAll();
            }

            @Override
            protected void succeeded() {
                tblSauvegardes.setItems(FXCollections.observableArrayList(getValue()));
            }
        };
        new Thread(task).start();
    }

    @FXML
    private void handleBackup() {
        Stage stage = (Stage) txtSchoolName.getScene().getWindow();
        Task<Sauvegarde> task = new Task<>() {
            @Override
            protected Sauvegarde call() throws Exception {
                // Dossier de sauvegarde portable dans le répertoire utilisateur
                String backupDir = System.getProperty("user.home") + java.io.File.separator + "prisma_backups";
                Files.createDirectories(Paths.get(backupDir));

                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String fileName = "prisma_backup_" + timestamp + ".sql";
                String filePath = backupDir + java.io.File.separator + fileName;

                // Export natif H2 vers un fichier SQL
                try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
                    session.doWork(connection -> {
                        try (java.sql.Statement stmt = connection.createStatement()) {
                            stmt.execute("SCRIPT TO '" + filePath.replace("'", "''") + "'");
                        }
                    });
                }

                File file = new File(filePath);
                Sauvegarde sauvegarde = new Sauvegarde();
                sauvegarde.setNomFichier(fileName);
                sauvegarde.setCheminFichier(filePath);
                sauvegarde.setDateSauvegarde(LocalDateTime.now());
                sauvegarde.setTailleOctets(file.length());
                sauvegarde.setStatut("SUCCÈS");

                sauvegardeRepository.save(sauvegarde);
                return sauvegarde;
            }

            @Override
            protected void succeeded() {
                NotificationUtil.showSuccess(stage, "Sauvegarde créée : " + getValue().getNomFichier());
                chargerHistoriqueSauvegardes();
                handleRefreshLogs();
            }

            @Override
            protected void failed() {
                logger.error("Erreur lors de la création de la sauvegarde H2", getException());
                NotificationUtil.showError(stage, "Erreur de sauvegarde.");
            }
        };
        new Thread(task).start();
    }

    @FXML
    private void handleRestore() {
        Stage stage = (Stage) txtSchoolName.getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir un fichier de sauvegarde SQL");
        // Répertoire de sauvegarde portable
        File backupHome = new File(System.getProperty("user.home") + java.io.File.separator + "prisma_backups");
        if (backupHome.exists()) {
            fileChooser.setInitialDirectory(backupHome);
        }
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers SQL", "*.sql"));

        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile == null) return;

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation");
        confirmAlert.setHeaderText("Restaurer les données ?");
        confirmAlert.setContentText("Attention : Cette action écrasera vos données actuelles.");

        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Restauration native H2 à partir d'un script SQL
                try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
                    session.doWork(connection -> {
                        try (java.sql.Statement stmt = connection.createStatement()) {
                            stmt.execute("RUNSCRIPT FROM '" + selectedFile.getAbsolutePath().replace("'", "''") + "'");
                        }
                    });
                }
                return null;
            }

            @Override
            protected void succeeded() {
                NotificationUtil.showSuccess(stage, "Restauration complétée !");
                chargerParametres();
                chargerPeriodes();
                chargerUtilisateurs();
                handleRefreshLogs();
            }

            @Override
            protected void failed() {
                logger.error("Erreur lors de la restauration H2", getException());
                NotificationUtil.showError(stage, "Erreur lors de la restauration.");
            }
        };
        new Thread(task).start();
    }
}
