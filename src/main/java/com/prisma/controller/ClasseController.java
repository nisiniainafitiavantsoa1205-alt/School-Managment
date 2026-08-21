package com.prisma.controller;

import com.prisma.database.DatabaseConnectionManager;
import com.prisma.entity.*;
import com.prisma.repository.*;
import com.prisma.repository.impl.*;
import com.prisma.util.NotificationUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClasseController {

    private static final Logger logger = LoggerFactory.getLogger(ClasseController.class);

    @FXML private TableView<Classe> tblClasses;
    @FXML private TableColumn<Classe, String> colNom;
    @FXML private TableColumn<Classe, String> colNiveau;
    @FXML private TableColumn<Classe, String> colProfPrincipal;
    @FXML private TableColumn<Classe, String> colAnneeScolaire;

    @FXML private Label lblClasseFormTitle;
    @FXML private TextField txtNomClasse;
    @FXML private ComboBox<String> cmbNiveau;
    @FXML private ComboBox<Utilisateur> cmbProfPrincipal;
    @FXML private TextField txtAnneeScolaire;
    @FXML private Button btnDeleteClasse;

    // Coefficients
    @FXML private Label lblSelectedClasseInfo;
    @FXML private TableView<Coefficient> tblCoefficients;
    @FXML private TableColumn<Coefficient, String> colCoefMatiere;
    @FXML private TableColumn<Coefficient, String> colCoefCode;
    @FXML private TableColumn<Coefficient, String> colCoefValeur;

    @FXML private ComboBox<Matiere> cmbMatiere;
    @FXML private TextField txtValeurCoef;
    @FXML private Button btnDeleteCoef;

    private final ClasseRepository classeRepository;
    private final PeriodeRepository periodeRepository;
    private final GenericRepository<Utilisateur, Integer> utilisateurRepository;
    private final GenericRepository<Matiere, Integer> matiereRepository;
    private final GenericRepository<Coefficient, Integer> coefficientRepository;

    private Classe selectedClasse;
    private Coefficient selectedCoefficient;

    public ClasseController() {
        this.classeRepository = new ClasseRepositoryImpl();
        this.periodeRepository = new PeriodeRepositoryImpl();
        this.utilisateurRepository = new GenericRepositoryImpl<>(Utilisateur.class) {};
        this.matiereRepository = new GenericRepositoryImpl<>(Matiere.class) {};
        this.coefficientRepository = new GenericRepositoryImpl<>(Coefficient.class) {};
    }

    @FXML
    public void initialize() {
        configurerTables();
        configurerFormulaires();
        chargerClasses();
        chargerProfesseurs();
        chargerMatieres();

        btnDeleteClasse.setVisible(false);
        btnDeleteCoef.setVisible(false);
    }

    private void configurerTables() {
        // Table des Classes
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colNiveau.setCellValueFactory(new PropertyValueFactory<>("niveau"));
        colAnneeScolaire.setCellValueFactory(new PropertyValueFactory<>("anneeScolaire"));
        colProfPrincipal.setCellValueFactory(cellData -> {
            Utilisateur prof = cellData.getValue().getProfesseurPrincipal();
            return new SimpleStringProperty(prof != null ? prof.getUsername() : "Non affecté");
        });

        // Écouteur de sélection de classe
        tblClasses.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                chargerClasseFormulaire(newVal);
                chargerCoefficientsClasse(newVal);
            }
        });

        // Table des Coefficients
        colCoefMatiere.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getMatiere().getNom()));
        colCoefCode.setCellValueFactory(cellData -> {
            String code = cellData.getValue().getMatiere().getCode();
            return new SimpleStringProperty(code != null ? code : "");
        });
        colCoefValeur.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.valueOf(cellData.getValue().getValeur())));

        // Écouteur de sélection de coefficient
        tblCoefficients.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedCoefficient = newVal;
                cmbMatiere.setValue(newVal.getMatiere());
                txtValeurCoef.setText(String.valueOf(newVal.getValeur()));
                btnDeleteCoef.setVisible(true);
            } else {
                btnDeleteCoef.setVisible(false);
            }
        });
    }

    private void configurerFormulaires() {
        cmbNiveau.setItems(FXCollections.observableArrayList("6ème", "5ème", "4ème", "3ème"));
        
        // Formatteur combo prof
        cmbProfPrincipal.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Utilisateur user) {
                if (user == null) return "";
                String roleNom = (user.getRole() != null) ? " (" + user.getRole().getNom() + ")" : "";
                return user.getUsername() + roleNom;
            }
            @Override
            public Utilisateur fromString(String string) { return null; }
        });

        // Formatteur combo matiere
        cmbMatiere.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Matiere mat) {
                return mat != null ? mat.getNom() : "";
            }
            @Override
            public Matiere fromString(String string) { return null; }
        });
    }

    private void chargerClasses() {
        Task<List<Classe>> task = new Task<>() {
            @Override
            protected List<Classe> call() {
                try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
                    return session.createQuery("select distinct c from Classe c left join fetch c.professeurPrincipal u left join fetch u.role order by c.nom", Classe.class)
                            .getResultList();
                } catch (Exception e) {
                    logger.error("Erreur chargement classes", e);
                    return classeRepository.findAll();
                }
            }

            @Override
            protected void succeeded() {
                tblClasses.setItems(FXCollections.observableArrayList(getValue()));
            }
        };
        new Thread(task).start();
    }

    private void chargerProfesseurs() {
        Task<List<Utilisateur>> task = new Task<>() {
            @Override
            protected List<Utilisateur> call() {
                // Charger les utilisateurs actifs (profs, directeurs, admins)
                try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
                    return session.createQuery("select u from Utilisateur u join fetch u.role r where u.active = true order by u.username", Utilisateur.class)
                            .getResultList();
                } catch (Exception e) {
                    logger.error("Erreur chargement professeurs", e);
                    return new ArrayList<>();
                }
            }

            @Override
            protected void succeeded() {
                cmbProfPrincipal.setItems(FXCollections.observableArrayList(getValue()));
            }
        };
        new Thread(task).start();
    }

    private void chargerMatieres() {
        Task<List<Matiere>> task = new Task<>() {
            @Override
            protected List<Matiere> call() {
                return matiereRepository.findAll();
            }

            @Override
            protected void succeeded() {
                cmbMatiere.setItems(FXCollections.observableArrayList(getValue()));
            }
        };
        new Thread(task).start();
    }

    private void chargerClasseFormulaire(Classe classe) {
        selectedClasse = classe;
        lblClasseFormTitle.setText("Modifier Classe : " + classe.getNom());
        txtNomClasse.setText(classe.getNom());
        cmbNiveau.setValue(classe.getNiveau());
        cmbProfPrincipal.setValue(classe.getProfesseurPrincipal());
        txtAnneeScolaire.setText(classe.getAnneeScolaire());
        btnDeleteClasse.setVisible(true);
    }

    @FXML
    private void handleResetClasseForm() {
        selectedClasse = null;
        lblClasseFormTitle.setText("Créer / Modifier une Classe");
        txtNomClasse.clear();
        cmbNiveau.setValue(null);
        cmbProfPrincipal.setValue(null);
        txtAnneeScolaire.setText("2025-2026");
        btnDeleteClasse.setVisible(false);
        tblClasses.getSelectionModel().clearSelection();

        lblSelectedClasseInfo.setText("Sélectionnez une classe pour configurer ses matières.");
        tblCoefficients.setItems(FXCollections.emptyObservableList());
    }

    @FXML
    private void handleSaveClasse() {
        Stage stage = (Stage) txtNomClasse.getScene().getWindow();
        String nom = txtNomClasse.getText().trim();
        String niveau = cmbNiveau.getValue();
        String anneeScolaire = txtAnneeScolaire.getText().trim();
        Utilisateur prof = cmbProfPrincipal.getValue();

        if (nom.isEmpty() || niveau == null || anneeScolaire.isEmpty()) {
            NotificationUtil.showError(stage, "Veuillez remplir les champs obligatoires (*).");
            return;
        }

        boolean isNew = (selectedClasse == null);
        Classe classe = isNew ? new Classe() : selectedClasse;
        classe.setNom(nom);
        classe.setNiveau(niveau);
        classe.setAnneeScolaire(anneeScolaire);
        classe.setProfesseurPrincipal(prof);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                classeRepository.saveOrUpdate(classe);
                return null;
            }

            @Override
            protected void succeeded() {
                NotificationUtil.showSuccess(stage, isNew ? "Classe créée avec succès !" : "Classe modifiée avec succès !");
                chargerClasses();
                handleResetClasseForm();
            }

            @Override
            protected void failed() {
                NotificationUtil.showError(stage, "Erreur de sauvegarde de la classe.");
            }
        };
        new Thread(task).start();
    }

    @FXML
    private void handleDeleteClasse() {
        if (selectedClasse == null) return;
        Stage stage = (Stage) txtNomClasse.getScene().getWindow();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer la classe ?");
        alert.setContentText(String.format("Voulez-vous supprimer définitivement la classe %s ?", selectedClasse.getNom()));

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() {
                    classeRepository.delete(selectedClasse);
                    return null;
                }

                @Override
                protected void succeeded() {
                    NotificationUtil.showSuccess(stage, "Classe supprimée avec succès !");
                    chargerClasses();
                    handleResetClasseForm();
                }

                @Override
                protected void failed() {
                    NotificationUtil.showError(stage, "Impossible de supprimer la classe (des élèves ou coefficients y sont associés).");
                }
            };
            new Thread(task).start();
        }
    }

    // GESTION DES COEFFICIENTS D'UNE CLASSE
    private void chargerCoefficientsClasse(Classe classe) {
        lblSelectedClasseInfo.setText("Classe active : " + classe.getNom() + " (" + classe.getAnneeScolaire() + ")");
        
        Task<List<Coefficient>> task = new Task<>() {
            @Override
            protected List<Coefficient> call() {
                try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
                    return session.createQuery("from Coefficient c join fetch c.matiere where c.classe.id = :classeId", Coefficient.class)
                            .setParameter("classeId", classe.getId())
                            .getResultList();
                } catch (Exception e) {
                    logger.error("Erreur chargement coefficients", e);
                    return new ArrayList<>();
                }
            }

            @Override
            protected void succeeded() {
                tblCoefficients.setItems(FXCollections.observableArrayList(getValue()));
                cmbMatiere.setValue(null);
                txtValeurCoef.clear();
                selectedCoefficient = null;
                btnDeleteCoef.setVisible(false);
            }
        };
        new Thread(task).start();
    }

    @FXML
    private void handleSaveCoefficient() {
        Stage stage = (Stage) txtNomClasse.getScene().getWindow();
        if (selectedClasse == null) {
            NotificationUtil.showError(stage, "Veuillez sélectionner une classe au préalable.");
            return;
        }

        Matiere matiere = cmbMatiere.getValue();
        String coefStr = txtValeurCoef.getText().trim();

        if (matiere == null || coefStr.isEmpty()) {
            NotificationUtil.showError(stage, "Sélectionnez une matière et saisissez un coefficient.");
            return;
        }

        double valeur;
        try {
            valeur = Double.parseDouble(coefStr);
            if (valeur <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            NotificationUtil.showError(stage, "Le coefficient doit être un nombre strictement positif (ex: 2.0).");
            return;
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                // Trouver ou créer la période active
                Optional<Periode> optPeriod = periodeRepository.findActive();
                Periode activePeriod;
                if (optPeriod.isPresent()) {
                    activePeriod = optPeriod.get();
                } else {
                    activePeriod = new Periode();
                    activePeriod.setNom("1er Trimestre");
                    activePeriod.setAnneeScolaire(selectedClasse.getAnneeScolaire());
                    activePeriod.setActive(true);
                    activePeriod.setClosed(false);
                    new GenericRepositoryImpl<Periode, Integer>(Periode.class) {}.save(activePeriod);
                }

                // Vérifier si le coefficient existe déjà pour cette classe / matière / période
                Coefficient coef = selectedCoefficient;
                if (coef == null) {
                    try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
                        coef = session.createQuery(
                                "from Coefficient where classe.id = :cId and matiere.id = :mId and periode.id = :pId",
                                Coefficient.class)
                                .setParameter("cId", selectedClasse.getId())
                                .setParameter("mId", matiere.getId())
                                .setParameter("pId", activePeriod.getId())
                                .uniqueResultOptional().orElse(null);
                    }
                }

                if (coef == null) {
                    coef = new Coefficient();
                    coef.setClasse(selectedClasse);
                    coef.setMatiere(matiere);
                    coef.setPeriode(activePeriod);
                }

                coef.setValeur(valeur);
                coefficientRepository.saveOrUpdate(coef);
                return null;
            }

            @Override
            protected void succeeded() {
                NotificationUtil.showSuccess(stage, "Coefficient enregistré !");
                chargerCoefficientsClasse(selectedClasse);
            }

            @Override
            protected void failed() {
                NotificationUtil.showError(stage, "Impossible d'associer la matière.");
            }
        };
        new Thread(task).start();
    }

    @FXML
    private void handleDeleteCoefficient() {
        if (selectedCoefficient == null) return;
        Stage stage = (Stage) txtNomClasse.getScene().getWindow();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                coefficientRepository.delete(selectedCoefficient);
                return null;
            }

            @Override
            protected void succeeded() {
                NotificationUtil.showSuccess(stage, "Matière dissociée de la classe.");
                chargerCoefficientsClasse(selectedClasse);
            }

            @Override
            protected void failed() {
                NotificationUtil.showError(stage, "Impossible de dissocier la matière.");
            }
        };
        new Thread(task).start();
    }

    @FXML
    private void handleCreateNewMatiere() {
        Stage stage = (Stage) txtNomClasse.getScene().getWindow();

        Dialog<Matiere> dialog = new Dialog<>();
        dialog.setTitle("Créer une nouvelle matière");
        dialog.setHeaderText("Ajouter une nouvelle matière au programme scolaire");

        ButtonType btnTypeSave = new ButtonType("Créer la matière", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnTypeSave, ButtonType.CANCEL);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 50, 10, 10));

        TextField txtNom = new TextField();
        txtNom.setPromptText("Ex: Informatique, Physique-Chimie, Malagasy");
        TextField txtCode = new TextField();
        txtCode.setPromptText("Ex: INFO, PC, MALAG");

        grid.add(new Label("Nom de la matière *:"), 0, 0);
        grid.add(txtNom, 1, 0);
        grid.add(new Label("Code court (ex: INFO) :"), 0, 1);
        grid.add(txtCode, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnTypeSave) {
                String nom = txtNom.getText().trim();
                if (nom.isEmpty()) {
                    return null;
                }
                String code = txtCode.getText().trim();
                if (code.isEmpty()) {
                    code = nom.substring(0, Math.min(4, nom.length())).toUpperCase();
                }
                Matiere m = new Matiere();
                m.setNom(nom);
                m.setCode(code.toUpperCase());
                m.setActive(true);
                return m;
            }
            return null;
        });

        Optional<Matiere> result = dialog.showAndWait();
        result.ifPresent(matiere -> {
            Task<Matiere> task = new Task<>() {
                @Override
                protected Matiere call() {
                    matiereRepository.save(matiere);
                    return matiere;
                }

                @Override
                protected void succeeded() {
                    NotificationUtil.showSuccess(stage, "Nouvelle matière '" + matiere.getNom() + "' créée !");
                    chargerMatieres();
                    Platform.runLater(() -> {
                        for (Matiere m : cmbMatiere.getItems()) {
                            if (m.getId() != null && m.getId().equals(matiere.getId())) {
                                cmbMatiere.setValue(m);
                                break;
                            }
                        }
                    });
                }

                @Override
                protected void failed() {
                    NotificationUtil.showError(stage, "Erreur lors de la création de la matière : " + getException().getMessage());
                }
            };
            new Thread(task).start();
        });
    }
}
