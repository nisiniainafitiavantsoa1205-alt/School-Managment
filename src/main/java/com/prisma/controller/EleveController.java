package com.prisma.controller;

import com.prisma.database.DatabaseConnectionManager;
import com.prisma.entity.Bulletin;
import com.prisma.entity.Classe;
import com.prisma.entity.Eleve;
import com.prisma.report.ReportService;
import com.prisma.report.ReportServiceImpl;
import com.prisma.service.ClasseService;
import com.prisma.service.EleveService;
import com.prisma.service.impl.ClasseServiceImpl;
import com.prisma.service.impl.EleveServiceImpl;
import com.prisma.util.NotificationUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class EleveController {

    private static final Logger logger = LoggerFactory.getLogger(EleveController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int PAGE_SIZE = 15;

    @FXML private TextField txtSearch;
    @FXML private ComboBox<Classe> cmbFilterClasse;
    @FXML private ComboBox<String> cmbFilterStatut;
    @FXML private TableView<Eleve> tblEleves;
    @FXML private TableColumn<Eleve, String> colMatricule;
    @FXML private TableColumn<Eleve, String> colNom;
    @FXML private TableColumn<Eleve, String> colPrenoms;
    @FXML private TableColumn<Eleve, String> colSexe;
    @FXML private TableColumn<Eleve, String> colClasse;
    @FXML private TableColumn<Eleve, String> colDateNaissance;
    @FXML private TableColumn<Eleve, String> colNumeroAppel;
    @FXML private TableColumn<Eleve, String> colContactParent;
    @FXML private TableColumn<Eleve, String> colAdresse;
    @FXML private TableColumn<Eleve, Void> colActions;

    @FXML private Button btnPrevPage;
    @FXML private Button btnNextPage;
    @FXML private Label lblPaginationInfo;

    // Formulaire
    @FXML private Label lblFormTitle;
    @FXML private ImageView imgPhoto;
    @FXML private Label lblPhotoPlaceholder;
    @FXML private TextField txtMatricule;
    @FXML private TextField txtNom;
    @FXML private TextField txtPrenoms;
    @FXML private DatePicker dpDateNaissance;
    @FXML private ComboBox<String> cmbSexe;
    @FXML private TextField txtNumeroAppel;
    @FXML private TextField txtAdresse;
    @FXML private ComboBox<Classe> cmbClasse;

    // Parents & Tuteur
    @FXML private TextField txtNomParent;
    @FXML private TextField txtProfessionParent;
    @FXML private TextField txtTelParent;
    @FXML private TextField txtNomParent2;
    @FXML private TextField txtProfessionParent2;
    @FXML private TextField txtTelParent2;
    @FXML private TextField txtTuteur;

    @FXML private Button btnDelete;

    private final EleveService eleveService;
    private final ClasseService classeService;

    private int currentPage = 1;
    private int totalPages = 1;
    private Eleve selectedEleve;
    private byte[] selectedPhotoBytes;

    public EleveController() {
        this.eleveService = new EleveServiceImpl();
        this.classeService = new ClasseServiceImpl();
    }

    @FXML
    public void initialize() {
        if (cmbFilterStatut != null) {
            cmbFilterStatut.setItems(FXCollections.observableArrayList("⚡ Élèves Actifs", "📦 Archives (Quittés)"));
            cmbFilterStatut.setValue("⚡ Élèves Actifs");
        }
        if (cmbSexe != null) {
            cmbSexe.setItems(FXCollections.observableArrayList("Masculin", "Féminin"));
        }
        configurerTableau();
        configurerFormulaires();
        chargerClasses();
        rechercherEleves();
        btnDelete.setVisible(false);
    }

    private void configurerTableau() {
        colMatricule.setCellValueFactory(new PropertyValueFactory<>("matricule"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenoms.setCellValueFactory(new PropertyValueFactory<>("prenoms"));
        if (colSexe != null) {
            colSexe.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getSexe() != null ? cellData.getValue().getSexe() : ""));
        }
        colNumeroAppel.setCellValueFactory(new PropertyValueFactory<>("numeroAppel"));

        colDateNaissance.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().getDateNaissance();
            return new SimpleStringProperty(date != null ? date.format(DATE_FORMATTER) : "");
        });

        colClasse.setCellValueFactory(cellData -> {
            Classe classe = cellData.getValue().getClasse();
            return new SimpleStringProperty(classe != null ? classe.getNom() : "Non affecté");
        });

        colAdresse.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getAdresse() != null ? cellData.getValue().getAdresse() : ""));

        colContactParent.setCellValueFactory(cellData -> {
            Eleve e = cellData.getValue();
            if (e.getTelephoneParent() != null && !e.getTelephoneParent().trim().isEmpty()) {
                String p1 = (e.getNomParent() != null && !e.getNomParent().trim().isEmpty()) ? e.getNomParent() : "";
                String prof = (e.getProfessionParent() != null && !e.getProfessionParent().trim().isEmpty()) ? " (" + e.getProfessionParent() + ")" : "";
                return new SimpleStringProperty((p1.isEmpty() ? "" : p1 + prof + ": ") + e.getTelephoneParent());
            } else if (e.getTelephoneParent2() != null && !e.getTelephoneParent2().trim().isEmpty()) {
                String p2 = (e.getNomParent2() != null && !e.getNomParent2().trim().isEmpty()) ? e.getNomParent2() : "";
                String prof2 = (e.getProfessionParent2() != null && !e.getProfessionParent2().trim().isEmpty()) ? " (" + e.getProfessionParent2() + ")" : "";
                return new SimpleStringProperty((p2.isEmpty() ? "" : p2 + prof2 + ": ") + e.getTelephoneParent2());
            } else if (e.getTuteur() != null && !e.getTuteur().trim().isEmpty()) {
                return new SimpleStringProperty("Tuteur: " + e.getTuteur());
            }
            return new SimpleStringProperty("Non renseigné");
        });

        if (colActions != null) {
            colActions.setCellFactory(col -> new TableCell<>() {
                private final Button btnParcours = new Button("📜 Bulletins");
                private final Button btnStatut = new Button();

                {
                    btnParcours.setStyle("-fx-background-color: #FFD700; -fx-text-fill: #0A1628; -fx-font-weight: bold; -fx-padding: 4px 8px; -fx-font-size: 11px; -fx-background-radius: 4px; -fx-cursor: hand;");
                    btnParcours.setOnAction(event -> {
                        Eleve e = getTableView().getItems().get(getIndex());
                        if (e != null) {
                            showParcoursDialog(e, tblEleves.getScene().getWindow());
                        }
                    });

                    btnStatut.setOnAction(event -> {
                        Eleve e = getTableView().getItems().get(getIndex());
                        if (e != null) {
                            if ("QUITTE".equalsIgnoreCase(e.getStatut())) {
                                showReintegrationDialog(e, tblEleves.getScene().getWindow());
                            } else {
                                showTransitionDialog(e, tblEleves.getScene().getWindow());
                            }
                        }
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        Eleve e = getTableView().getItems().get(getIndex());
                        if (e != null && "QUITTE".equalsIgnoreCase(e.getStatut())) {
                            btnStatut.setText("↩️ Réintégrer");
                            btnStatut.setStyle("-fx-background-color: #27AE60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4px 8px; -fx-font-size: 11px; -fx-background-radius: 4px; -fx-cursor: hand;");
                        } else {
                            btnStatut.setText("🔄 Statut");
                            btnStatut.setStyle("-fx-background-color: #34495E; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4px 8px; -fx-font-size: 11px; -fx-background-radius: 4px; -fx-cursor: hand;");
                        }
                        HBox container = new HBox(6, btnParcours, btnStatut);
                        container.setStyle("-fx-alignment: CENTER;");
                        setGraphic(container);
                    }
                }
            });
        }

        // Ecouteur de sélection dans le tableau
        tblEleves.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                chargerEleveFormulaire(newSelection);
            }
        });
    }

    private void configurerFormulaires() {
        // Personnaliser le rendu de la combo Classe pour afficher le nom de la classe
        javafx.util.StringConverter<Classe> classConverter = new javafx.util.StringConverter<>() {
            @Override
            public String toString(Classe classe) {
                return classe != null ? classe.getNom() : "";
            }
            @Override
            public Classe fromString(String string) {
                return null;
            }
        };
        cmbClasse.setConverter(classConverter);
        cmbFilterClasse.setConverter(classConverter);
    }

    private void chargerClasses() {
        Task<List<Classe>> task = new Task<>() {
            @Override
            protected List<Classe> call() {
                return classeService.listerTout();
            }

            @Override
            protected void succeeded() {
                List<Classe> classes = getValue();
                cmbClasse.setItems(FXCollections.observableArrayList(classes));
                
                // Ajouter une option "Toutes les classes" pour le filtre
                Classe optionToutes = new Classe();
                optionToutes.setId(null);
                optionToutes.setNom("Toutes les classes");
                
                ObservableList<Classe> filterList = FXCollections.observableArrayList();
                filterList.add(optionToutes);
                filterList.addAll(classes);
                cmbFilterClasse.setItems(filterList);
            }
        };
        new Thread(task).start();
    }

    private void rechercherEleves() {
        String query = txtSearch.getText().trim();
        Classe filterClasse = cmbFilterClasse.getValue();
        Integer classeId = (filterClasse != null && filterClasse.getId() != null) ? filterClasse.getId() : null;

        String filterStatutVal = (cmbFilterStatut != null && cmbFilterStatut.getValue() != null) ? cmbFilterStatut.getValue() : "⚡ Élèves Actifs";
        String statut = filterStatutVal.contains("Archives") ? "QUITTE" : "ACTIF";

        Task<List<Eleve>> searchTask = new Task<>() {
            private long totalCount;

            @Override
            protected List<Eleve> call() {
                totalCount = eleveService.compterRecherche(query, classeId, statut);
                return eleveService.rechercher(query, classeId, statut, currentPage, PAGE_SIZE);
            }

            @Override
            protected void succeeded() {
                tblEleves.setItems(FXCollections.observableArrayList(getValue()));
                
                // Calculer les pages
                totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);
                if (totalPages == 0) totalPages = 1;
                
                lblPaginationInfo.setText(String.format("Page %d / %d", currentPage, totalPages));
                btnPrevPage.setDisable(currentPage <= 1);
                btnNextPage.setDisable(currentPage >= totalPages);
            }
        };
        new Thread(searchTask).start();
    }

    @FXML
    private void handleSearch() {
        currentPage = 1;
        rechercherEleves();
    }

    @FXML
    private void handleFilterClasse() {
        currentPage = 1;
        rechercherEleves();
    }

    @FXML
    private void handleFilterStatut() {
        currentPage = 1;
        rechercherEleves();
    }

    @FXML
    private void handlePrevPage() {
        if (currentPage > 1) {
            currentPage--;
            rechercherEleves();
        }
    }

    @FXML
    private void handleNextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            rechercherEleves();
        }
    }

    private void chargerEleveFormulaire(Eleve eleve) {
        this.selectedEleve = eleve;
        this.selectedPhotoBytes = eleve.getPhoto();

        lblFormTitle.setText("Modification Élève");
        txtMatricule.setText(eleve.getMatricule());
        txtNom.setText(eleve.getNom());
        txtPrenoms.setText(eleve.getPrenoms());
        dpDateNaissance.setValue(eleve.getDateNaissance());
        if (cmbSexe != null) cmbSexe.setValue(eleve.getSexe());
        txtNumeroAppel.setText(eleve.getNumeroAppel());
        txtAdresse.setText(eleve.getAdresse() != null ? eleve.getAdresse() : "");
        cmbClasse.setValue(eleve.getClasse());

        txtNomParent.setText(eleve.getNomParent() != null ? eleve.getNomParent() : "");
        if (txtProfessionParent != null) txtProfessionParent.setText(eleve.getProfessionParent() != null ? eleve.getProfessionParent() : "");
        txtTelParent.setText(eleve.getTelephoneParent() != null ? eleve.getTelephoneParent() : "");
        txtNomParent2.setText(eleve.getNomParent2() != null ? eleve.getNomParent2() : "");
        if (txtProfessionParent2 != null) txtProfessionParent2.setText(eleve.getProfessionParent2() != null ? eleve.getProfessionParent2() : "");
        txtTelParent2.setText(eleve.getTelephoneParent2() != null ? eleve.getTelephoneParent2() : "");
        txtTuteur.setText(eleve.getTuteur() != null ? eleve.getTuteur() : "");

        btnDelete.setVisible(true);

        afficherPhoto(selectedPhotoBytes);
    }

    private void afficherPhoto(byte[] bytes) {
        if (bytes != null && bytes.length > 0) {
            imgPhoto.setImage(new Image(new ByteArrayInputStream(bytes)));
            lblPhotoPlaceholder.setVisible(false);
        } else {
            imgPhoto.setImage(null);
            lblPhotoPlaceholder.setVisible(true);
        }
    }

    @FXML
    private void handleBrowsePhoto() {
        Stage stage = (Stage) txtNom.getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir la photo de l'élève");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try {
                selectedPhotoBytes = Files.readAllBytes(file.toPath());
                afficherPhoto(selectedPhotoBytes);
            } catch (IOException e) {
                NotificationUtil.showError(stage, "Erreur lors du chargement de l'image.");
            }
        }
    }

    @FXML
    private void handleDeletePhoto() {
        selectedPhotoBytes = null;
        afficherPhoto(null);
    }

    @FXML
    private void handleNewEleve() {
        tblEleves.getSelectionModel().clearSelection();
        selectedEleve = null;
        selectedPhotoBytes = null;

        lblFormTitle.setText("Nouvel Élève");
        txtMatricule.setText("Génération automatique");
        txtNom.clear();
        txtPrenoms.clear();
        dpDateNaissance.setValue(null);
        if (cmbSexe != null) cmbSexe.setValue(null);
        txtNumeroAppel.clear();
        txtAdresse.clear();
        cmbClasse.setValue(null);

        txtNomParent.clear();
        if (txtProfessionParent != null) txtProfessionParent.clear();
        txtTelParent.clear();
        txtNomParent2.clear();
        if (txtProfessionParent2 != null) txtProfessionParent2.clear();
        txtTelParent2.clear();
        txtTuteur.clear();

        btnDelete.setVisible(false);

        afficherPhoto(null);
        txtNom.requestFocus();
    }

    @FXML
    private void handleCancel() {
        handleNewEleve();
    }

    @FXML
    private void handleSaveEleve() {
        Stage stage = (Stage) txtNom.getScene().getWindow();
        
        String nom = txtNom.getText().trim();
        String prenoms = txtPrenoms.getText().trim();
        LocalDate dateNaiss = dpDateNaissance.getValue();
        String sexe = (cmbSexe != null) ? cmbSexe.getValue() : null;
        String numAppel = txtNumeroAppel.getText().trim();
        String adresse = txtAdresse.getText().trim();
        Classe classe = cmbClasse.getValue();

        String nomParent = txtNomParent.getText().trim();
        String professionParent = (txtProfessionParent != null) ? txtProfessionParent.getText().trim() : "";
        String telParent = txtTelParent.getText().trim();
        String nomParent2 = txtNomParent2.getText().trim();
        String professionParent2 = (txtProfessionParent2 != null) ? txtProfessionParent2.getText().trim() : "";
        String telParent2 = txtTelParent2.getText().trim();
        String tuteur = txtTuteur.getText().trim();

        if (nom.isEmpty() || dateNaiss == null || numAppel.isEmpty() || classe == null) {
            NotificationUtil.showError(stage, "Veuillez remplir le nom, la date de naissance, le N° d'appel et la classe (*).");
            return;
        }

        boolean isNew = (selectedEleve == null);
        Eleve eleve = isNew ? new Eleve() : selectedEleve;
        eleve.setNom(nom.toUpperCase());
        eleve.setPrenoms(prenoms);
        eleve.setDateNaissance(dateNaiss);
        eleve.setSexe(sexe);
        eleve.setNumeroAppel(numAppel);
        eleve.setAdresse(adresse);
        eleve.setClasse(classe);
        eleve.setPhoto(selectedPhotoBytes);

        eleve.setNomParent(nomParent);
        eleve.setProfessionParent(professionParent);
        eleve.setTelephoneParent(telParent);
        eleve.setNomParent2(nomParent2);
        eleve.setProfessionParent2(professionParent2);
        eleve.setTelephoneParent2(telParent2);
        eleve.setTuteur(tuteur);

        Task<Eleve> saveTask = new Task<>() {
            @Override
            protected Eleve call() {
                if (isNew) {
                    // Récupérer une année scolaire par défaut ou utiliser la courante
                    String anneeScolaire = classe != null ? classe.getAnneeScolaire() : "2025-2026";
                    eleve.setMatricule(eleveService.genererMatricule(anneeScolaire));
                    return eleveService.creer(eleve);
                } else {
                    return eleveService.modifier(eleve);
                }
            }

            @Override
            protected void succeeded() {
                NotificationUtil.showSuccess(stage, isNew ? "Élève créé avec succès !" : "Élève modifié avec succès !");
                rechercherEleves();
                handleNewEleve();
            }

            @Override
            protected void failed() {
                NotificationUtil.showError(stage, "Erreur lors de la sauvegarde : " + getException().getMessage());
            }
        };
        new Thread(saveTask).start();
    }

    @FXML
    private void handleDeleteEleve() {
        if (selectedEleve == null) return;
        Stage stage = (Stage) txtNom.getScene().getWindow();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer l'élève ?");
        alert.setContentText(String.format("Voulez-vous supprimer définitivement l'élève %s %s ?", 
                selectedEleve.getNom(), selectedEleve.getPrenoms()));

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            Task<Void> deleteTask = new Task<>() {
                @Override
                protected Void call() {
                    eleveService.supprimer(selectedEleve.getId());
                    return null;
                }

                @Override
                protected void succeeded() {
                    NotificationUtil.showSuccess(stage, "Élève supprimé avec succès !");
                    rechercherEleves();
                    handleNewEleve();
                }

                @Override
                protected void failed() {
                    NotificationUtil.showError(stage, "Impossible de supprimer l'élève (des notes lui sont peut-être associées).");
                }
            };
            new Thread(deleteTask).start();
        }
    }

    public static void showParcoursDialog(Eleve eleve, Window owner) {
        if (eleve == null) return;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Parcours Académique & Bulletins — " + eleve.getNom() + " " + (eleve.getPrenoms() != null ? eleve.getPrenoms() : ""));
        dialog.setHeaderText(null);
        if (owner != null) dialog.initOwner(owner);

        DialogPane pane = dialog.getDialogPane();
        try {
            pane.getStylesheets().add(EleveController.class.getResource("/css/application.css").toExternalForm());
        } catch (Exception ignored) {}
        pane.setPrefWidth(880);
        pane.setPrefHeight(560);

        VBox mainContainer = new VBox(15);
        mainContainer.setPadding(new javafx.geometry.Insets(15));

        // En-tête avec les détails de l'élève
        HBox headerInfo = new HBox(15);
        headerInfo.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        headerInfo.setStyle("-fx-background-color: #0F2040; -fx-padding: 12px 16px; -fx-background-radius: 8px; -fx-border-color: rgba(255,215,0,0.3); -fx-border-radius: 8px;");

        // Mini cadre photo
        StackPane photoContainer = new StackPane();
        photoContainer.setPrefSize(60, 70);
        photoContainer.setMaxSize(60, 70);
        photoContainer.setStyle("-fx-border-color: rgba(255,255,255,0.2); -fx-border-radius: 4px; -fx-background-color: rgba(0,0,0,0.3);");
        ImageView imgView = new ImageView();
        imgView.setFitWidth(58);
        imgView.setFitHeight(68);
        imgView.setPreserveRatio(true);
        if (eleve.getPhoto() != null && eleve.getPhoto().length > 0) {
            try {
                imgView.setImage(new Image(new ByteArrayInputStream(eleve.getPhoto())));
            } catch (Exception ignored) {}
        }
        photoContainer.getChildren().add(imgView);

        VBox studentDetails = new VBox(4);
        Label lblName = new Label(eleve.getNom() + " " + (eleve.getPrenoms() != null ? eleve.getPrenoms() : ""));
        lblName.setStyle("-fx-text-fill: #FFD700; -fx-font-weight: bold; -fx-font-size: 16px;");
        String classeNom = eleve.getClasse() != null ? eleve.getClasse().getNom() + " (" + eleve.getClasse().getAnneeScolaire() + ")" : "Non affecté";
        Label lblSub = new Label("Matricule: " + eleve.getMatricule() + "   |   Classe actuelle: " + classeNom + "   |   N° Appel: " + eleve.getNumeroAppel());
        lblSub.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 12px;");

        studentDetails.getChildren().addAll(lblName, lblSub);
        headerInfo.getChildren().addAll(photoContainer, studentDetails);

        // Tableau des bulletins du parcours
        TableView<Bulletin> tblParcours = new TableView<>();
        tblParcours.setPlaceholder(new Label("Aucun bulletin archivé ou calculé trouvé pour cet élève."));
        VBox.setVgrow(tblParcours, Priority.ALWAYS);

        TableColumn<Bulletin, String> colAnnee = new TableColumn<>("Année Scolaire");
        colAnnee.setPrefWidth(110);
        colAnnee.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getPeriode() != null ? data.getValue().getPeriode().getAnneeScolaire() : "—"));

        TableColumn<Bulletin, String> colCls = new TableColumn<>("Classe");
        colCls.setPrefWidth(90);
        colCls.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getClasse() != null ? data.getValue().getClasse().getNom() :
                (eleve.getClasse() != null ? eleve.getClasse().getNom() : "—")));

        TableColumn<Bulletin, String> colPer = new TableColumn<>("Période");
        colPer.setPrefWidth(120);
        colPer.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getPeriode() != null ? data.getValue().getPeriode().getNom() : "—"));

        TableColumn<Bulletin, String> colMoy = new TableColumn<>("Moyenne");
        colMoy.setPrefWidth(95);
        colMoy.setCellValueFactory(data -> new SimpleStringProperty(
                String.format("%.2f / 20", data.getValue().getMoyenneGenerale())));

        TableColumn<Bulletin, String> colRang = new TableColumn<>("Rang");
        colRang.setPrefWidth(70);
        colRang.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getRang() + "e"));

        TableColumn<Bulletin, String> colMention = new TableColumn<>("Mention");
        colMention.setPrefWidth(100);
        colMention.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getMention() != null ? data.getValue().getMention() : "—"));

        TableColumn<Bulletin, String> colApp = new TableColumn<>("Appréciation");
        colApp.setPrefWidth(160);
        colApp.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getAppreciationGenerale() != null ? data.getValue().getAppreciationGenerale() : ""));

        TableColumn<Bulletin, Void> colPdf = new TableColumn<>("Action");
        colPdf.setPrefWidth(100);
        colPdf.setStyle("-fx-alignment: CENTER;");

        ReportService reportService = new ReportServiceImpl();

        colPdf.setCellFactory(col -> new TableCell<>() {
            private final Button btnViewPdf = new Button("👁️ PDF");
            {
                btnViewPdf.getStyleClass().add("btn-secondary");
                btnViewPdf.setStyle("-fx-padding: 4px 10px; -fx-font-size: 11px;");
                btnViewPdf.setOnAction(event -> {
                    Bulletin b = getTableView().getItems().get(getIndex());
                    if (b != null) {
                        btnViewPdf.setDisable(true);
                        Task<File> pdfTask = new Task<>() {
                            @Override
                            protected File call() throws Exception {
                                File outDir = new File(System.getProperty("user.home"), "Documents");
                                if (!outDir.exists()) outDir.mkdirs();
                                return reportService.genererBulletinPdf(b, outDir);
                            }

                            @Override
                            protected void succeeded() {
                                btnViewPdf.setDisable(false);
                                File pdf = getValue();
                                if (pdf != null && pdf.exists()) {
                                    ouvrirPdfSafely(pdf);
                                }
                            }

                            @Override
                            protected void failed() {
                                btnViewPdf.setDisable(false);
                                Throwable ex = getException();
                                NotificationUtil.showError((Stage) dialog.getDialogPane().getScene().getWindow(),
                                        "Erreur lors de la génération PDF : " + (ex != null ? ex.getMessage() : ""));
                            }
                        };
                        new Thread(pdfTask).start();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnViewPdf);
            }
        });

        TableColumn<Bulletin, String> colStatutArch = new TableColumn<>("Statut");
        colStatutArch.setPrefWidth(90);
        colStatutArch.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getPeriode() != null && data.getValue().getPeriode().isClosed() ? "📦 Archivé" : "⚡ Actif"));
        colStatutArch.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("Archiv&eacute;") || item.contains("Archiv&eacute;s") || item.contains("Archivé")) {
                        setStyle("-fx-text-fill: #F39C12; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    } else {
                        setStyle("-fx-text-fill: #2ECC71; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    }
                }
            }
        });

        tblParcours.getColumns().addAll(colAnnee, colCls, colPer, colStatutArch, colMoy, colRang, colMention, colApp, colPdf);

        // Chargement asynchrone des bulletins depuis la base
        Task<List<Bulletin>> task = new Task<>() {
            @Override
            protected List<Bulletin> call() {
                try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
                    return session.createQuery(
                            "select distinct b from Bulletin b join fetch b.eleve e left join fetch e.classe join fetch b.periode p left join fetch b.classe c " +
                            "where e.id = :eId order by p.anneeScolaire desc, p.id asc", Bulletin.class)
                            .setParameter("eId", eleve.getId())
                            .getResultList();
                } catch (Exception e) {
                    logger.error("Erreur lors du chargement des bulletins de l'élève", e);
                    return List.of();
                }
            }

            @Override
            protected void succeeded() {
                tblParcours.setItems(FXCollections.observableArrayList(getValue()));
            }
        };
        new Thread(task).start();

        // Actions au bas de la modale
        Button btnExportAll = new Button("📥 Exporter Tout en PDF");
        btnExportAll.getStyleClass().add("btn-primary");
        btnExportAll.setOnAction(e -> {
            List<Bulletin> bulletins = tblParcours.getItems();
            if (bulletins == null || bulletins.isEmpty()) {
                NotificationUtil.showError((Stage) dialog.getDialogPane().getScene().getWindow(), "Aucun bulletin à exporter.");
                return;
            }
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Choisir le dossier d'export des bulletins");
            File dir = chooser.showDialog(dialog.getDialogPane().getScene().getWindow());
            if (dir != null) {
                btnExportAll.setDisable(true);
                Task<Void> exportTask = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        for (Bulletin b : bulletins) {
                            reportService.genererBulletinPdf(b, dir);
                        }
                        return null;
                    }

                    @Override
                    protected void succeeded() {
                        btnExportAll.setDisable(false);
                        NotificationUtil.showSuccess((Stage) dialog.getDialogPane().getScene().getWindow(),
                                bulletins.size() + " bulletin(s) exporté(s) dans le dossier " + dir.getName());
                    }

                    @Override
                    protected void failed() {
                        btnExportAll.setDisable(false);
                        Throwable ex = getException();
                        NotificationUtil.showError((Stage) dialog.getDialogPane().getScene().getWindow(),
                                "Erreur lors de l'export : " + (ex != null ? ex.getMessage() : ""));
                    }
                };
                new Thread(exportTask).start();
            }
        });

        mainContainer.getChildren().addAll(headerInfo, tblParcours);
        pane.setContent(mainContainer);
        pane.getButtonTypes().add(ButtonType.CLOSE);

        ButtonBar buttonBar = (ButtonBar) pane.lookup(".button-bar");
        if (buttonBar != null) {
            buttonBar.getButtons().add(0, btnExportAll);
        }

        dialog.showAndWait();
    }

    private void showTransitionDialog(Eleve eleve, Window owner) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Transition d'année & Statut - " + eleve.getNom());
        dialog.initOwner(owner);

        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("-fx-background-color: #0A1628; -fx-text-fill: white;");

        VBox mainBox = new VBox(15);
        mainBox.setStyle("-fx-padding: 15;");

        Label lblTitle = new Label("Ajustement du statut de l'élève : " + eleve.getNom() + " " + (eleve.getPrenoms() != null ? eleve.getPrenoms() : ""));
        lblTitle.setStyle("-fx-text-fill: #FFD700; -fx-font-weight: bold; -fx-font-size: 14px;");

        String clsCurrent = eleve.getClasse() != null ? eleve.getClasse().getNom() : "Aucune";
        Label lblInfo = new Label("Matricule : " + eleve.getMatricule() + "   |   Classe actuelle : " + clsCurrent);
        lblInfo.setStyle("-fx-text-fill: #CCCCCC; -fx-font-size: 12px;");

        Label lblChoice = new Label("Sélectionner la décision de fin d'année / transition :");
        lblChoice.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        ComboBox<String> cmbDecision = new ComboBox<>(FXCollections.observableArrayList(
                "↗️ Passer en classe supérieure",
                "🔁 Redoubler dans une classe",
                "🚪 Quitter l'établissement (Archiver)"
        ));
        cmbDecision.setValue("↗️ Passer en classe supérieure");
        cmbDecision.setStyle("-fx-background-color: #1A2744; -fx-text-fill: white;");

        Label lblTargetCls = new Label("Nouvelle classe d'affectation :");
        lblTargetCls.setStyle("-fx-text-fill: white;");

        ComboBox<Classe> cmbTargetClasse = new ComboBox<>();
        List<Classe> allActiveClasses = classeService.listerTout();
        cmbTargetClasse.setItems(FXCollections.observableArrayList(allActiveClasses));
        cmbTargetClasse.setStyle("-fx-background-color: #1A2744; -fx-text-fill: white;");

        javafx.util.StringConverter<Classe> converter = new javafx.util.StringConverter<>() {
            @Override public String toString(Classe c) { return c != null ? c.getNom() + " (" + c.getAnneeScolaire() + ")" : ""; }
            @Override public Classe fromString(String string) { return null; }
        };
        cmbTargetClasse.setConverter(converter);
        if (eleve.getClasse() != null) cmbTargetClasse.setValue(eleve.getClasse());

        cmbDecision.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.contains("Quitter")) {
                cmbTargetClasse.setDisable(true);
            } else {
                cmbTargetClasse.setDisable(false);
            }
        });

        mainBox.getChildren().addAll(lblTitle, lblInfo, new Separator(), lblChoice, cmbDecision, lblTargetCls, cmbTargetClasse);
        pane.setContent(mainBox);

        ButtonType btnSave = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().addAll(btnSave, btnCancel);

        dialog.setResultConverter(button -> {
            if (button == btnSave) {
                String decision = cmbDecision.getValue();
                if (decision.contains("Quitter")) {
                    eleve.setStatut("QUITTE");
                    eleveService.modifier(eleve);
                    NotificationUtil.showSuccess((Stage) owner, "Élève marqué comme ayant quitté l'établissement et transféré aux Archives.");
                } else {
                    Classe targetClass = cmbTargetClasse.getValue();
                    if (targetClass == null) {
                        NotificationUtil.showError((Stage) owner, "Veuillez sélectionner une classe de destination.");
                        return null;
                    }
                    eleve.setClasse(targetClass);
                    eleve.setStatut("ACTIF");
                    eleveService.modifier(eleve);
                    NotificationUtil.showSuccess((Stage) owner, "Mise à jour réussie : l'élève a été affecté à la classe " + targetClass.getNom());
                }
                rechercherEleves();
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void showReintegrationDialog(Eleve eleve, Window owner) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Réintégration de l'élève - " + eleve.getNom());
        dialog.initOwner(owner);

        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("-fx-background-color: #0A1628; -fx-text-fill: white;");

        VBox mainBox = new VBox(15);
        mainBox.setStyle("-fx-padding: 15;");

        Label lblTitle = new Label("Réintégrer l'élève archivé : " + eleve.getNom() + " " + (eleve.getPrenoms() != null ? eleve.getPrenoms() : ""));
        lblTitle.setStyle("-fx-text-fill: #FFD700; -fx-font-weight: bold; -fx-font-size: 14px;");

        Label lblPrompt = new Label("Sélectionner la classe d'affectation pour la réintégration :");
        lblPrompt.setStyle("-fx-text-fill: white;");

        ComboBox<Classe> cmbTargetClasse = new ComboBox<>();
        List<Classe> allActiveClasses = classeService.listerTout();
        cmbTargetClasse.setItems(FXCollections.observableArrayList(allActiveClasses));
        cmbTargetClasse.setStyle("-fx-background-color: #1A2744; -fx-text-fill: white;");

        javafx.util.StringConverter<Classe> converter = new javafx.util.StringConverter<>() {
            @Override public String toString(Classe c) { return c != null ? c.getNom() + " (" + c.getAnneeScolaire() + ")" : ""; }
            @Override public Classe fromString(String string) { return null; }
        };
        cmbTargetClasse.setConverter(converter);
        if (!allActiveClasses.isEmpty()) cmbTargetClasse.setValue(allActiveClasses.get(0));

        mainBox.getChildren().addAll(lblTitle, lblPrompt, cmbTargetClasse);
        pane.setContent(mainBox);

        ButtonType btnSave = new ButtonType("Réintégrer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().addAll(btnSave, btnCancel);

        dialog.setResultConverter(button -> {
            if (button == btnSave) {
                Classe targetClass = cmbTargetClasse.getValue();
                if (targetClass == null) {
                    NotificationUtil.showError((Stage) owner, "Veuillez sélectionner une classe.");
                    return null;
                }
                eleve.setClasse(targetClass);
                eleve.setStatut("ACTIF");
                eleveService.modifier(eleve);
                NotificationUtil.showSuccess((Stage) owner, "L'élève " + eleve.getNom() + " a été réintégré en classe " + targetClass.getNom());
                rechercherEleves();
            }
            return null;
        });

        dialog.showAndWait();
    }

    private static void ouvrirPdfSafely(File pdfFile) {
        new Thread(() -> {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(pdfFile);
                } else {
                    new ProcessBuilder("xdg-open", pdfFile.getAbsolutePath()).start();
                }
            } catch (Exception e) {
                logger.warn("Impossible d'ouvrir le PDF : {}", pdfFile.getAbsolutePath(), e);
            }
        }).start();
    }
}
