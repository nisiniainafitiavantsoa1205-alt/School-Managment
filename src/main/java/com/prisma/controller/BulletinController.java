package com.prisma.controller;

import com.prisma.entity.*;
import com.prisma.report.ReportService;
import com.prisma.report.ReportServiceImpl;
import com.prisma.repository.impl.ClasseRepositoryImpl;
import com.prisma.repository.impl.GenericRepositoryImpl;
import com.prisma.repository.impl.PeriodeRepositoryImpl;
import com.prisma.service.CalculService;
import com.prisma.service.impl.CalculServiceImpl;
import com.prisma.util.NotificationUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.util.List;

public class BulletinController {

    private static final Logger logger = LoggerFactory.getLogger(BulletinController.class);

    @FXML private ComboBox<Classe> cmbClasse;
    @FXML private ComboBox<Periode> cmbPeriode;
    @FXML private TextField txtSearchEleve;

    // KPI Cards
    @FXML private Label lblNbEleves;
    @FXML private Label lblMoyenneClasse;
    @FXML private Label lblMeilleurEleve;
    @FXML private Label lblStatut;

    // Table
    @FXML private TableView<Bulletin> tblBulletins;
    @FXML private TableColumn<Bulletin, String> colRang;
    @FXML private TableColumn<Bulletin, String> colMatricule;
    @FXML private TableColumn<Bulletin, String> colNomPrenom;
    @FXML private TableColumn<Bulletin, Void> colParcours;
    @FXML private TableColumn<Bulletin, String> colTotalPts;
    @FXML private TableColumn<Bulletin, String> colTotalCoef;
    @FXML private TableColumn<Bulletin, String> colMoyenne;
    @FXML private TableColumn<Bulletin, String> colMention;
    @FXML private TableColumn<Bulletin, String> colAppreciation;

    @FXML private Button btnExporterPDF;

    private final CalculService calculService;
    private final ReportService reportService;
    private final ClasseRepositoryImpl classeRepository;
    private final PeriodeRepositoryImpl periodeRepository;
    private final GenericRepositoryImpl<Bulletin, Integer> bulletinRepository;

    private final javafx.collections.ObservableList<Bulletin> bulletinsList = FXCollections.observableArrayList();
    private javafx.collections.transformation.FilteredList<Bulletin> filteredBulletins;

    public BulletinController() {
        this.calculService = new CalculServiceImpl();
        this.reportService = new ReportServiceImpl();
        this.classeRepository = new ClasseRepositoryImpl();
        this.periodeRepository = new PeriodeRepositoryImpl();
        this.bulletinRepository = new GenericRepositoryImpl<>(Bulletin.class) {};
    }

    @FXML
    public void initialize() {
        configurerTable();
        configurerCombos();
        btnExporterPDF.setDisable(true);
    }

    private void configurerCombos() {
        cmbClasse.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Classe c) { return c != null ? c.getNom() + " (" + c.getAnneeScolaire() + ")" : ""; }
            @Override public Classe fromString(String s) { return null; }
        });
        cmbPeriode.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Periode p) { return p != null ? p.getNom() : ""; }
            @Override public Periode fromString(String s) { return null; }
        });

        Task<Void> task = new Task<>() {
            private List<Classe> classes;
            private List<Periode> periodes;

            @Override
            protected Void call() {
                classes = classeRepository.findAll();
                List<Periode> all = periodeRepository.findAll();
                List<Periode> openOnly = all.stream().filter(p -> !p.isClosed()).toList();
                periodes = openOnly.isEmpty() ? all : openOnly;
                return null;
            }

            @Override
            protected void succeeded() {
                cmbClasse.setItems(FXCollections.observableArrayList(classes));
                cmbPeriode.setItems(FXCollections.observableArrayList(periodes));

                if (!periodes.isEmpty()) {
                    Periode active = periodes.stream().filter(Periode::isActive).findFirst().orElse(periodes.get(0));
                    cmbPeriode.setValue(active);
                }
                if (!classes.isEmpty()) {
                    cmbClasse.setValue(classes.get(0));
                }
                handleSelectionChange();
            }
        };
        new Thread(task).start();
    }

    private void configurerTable() {
        filteredBulletins = new javafx.collections.transformation.FilteredList<>(bulletinsList, p -> true);
        tblBulletins.setItems(filteredBulletins);

        if (txtSearchEleve != null) {
            txtSearchEleve.textProperty().addListener((obs, oldVal, newVal) -> {
                filteredBulletins.setPredicate(b -> {
                    if (newVal == null || newVal.trim().isEmpty()) return true;
                    String lower = newVal.trim().toLowerCase();
                    Eleve e = b.getEleve();
                    if (e == null) return false;
                    return (e.getNom() != null && e.getNom().toLowerCase().contains(lower)) ||
                           (e.getPrenoms() != null && e.getPrenoms().toLowerCase().contains(lower)) ||
                           (e.getMatricule() != null && e.getMatricule().toLowerCase().contains(lower));
                });
            });
        }

        // Colonnes read-only
        colRang.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getRang() + "er/ème"));
        colMatricule.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getEleve().getMatricule()));
        colNomPrenom.setCellValueFactory(cellData -> {
            Eleve e = cellData.getValue().getEleve();
            return new SimpleStringProperty(e.getNom() + " " + e.getPrenoms());
        });

        if (colParcours != null) {
            colParcours.setCellFactory(col -> new TableCell<>() {
                private final Button btnParcours = new Button("📜 Parcours");
                {
                    btnParcours.setStyle("-fx-background-color: #FFD700; -fx-text-fill: #0A1628; -fx-font-weight: bold; -fx-padding: 3px 8px; -fx-font-size: 11px; -fx-background-radius: 4px; -fx-cursor: hand;");
                    btnParcours.setOnAction(event -> {
                        Bulletin b = getTableView().getItems().get(getIndex());
                        if (b != null && b.getEleve() != null) {
                            EleveController.showParcoursDialog(b.getEleve(), tblBulletins.getScene().getWindow());
                        }
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : btnParcours);
                }
            });
        }
        colTotalPts.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("%.2f", cellData.getValue().getTotalMoyennePonderee())));
        colTotalCoef.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("%.0f", cellData.getValue().getTotalCoefficient())));
        colMoyenne.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("%.2f", cellData.getValue().getMoyenneGenerale())));
        colMention.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getMention()));

        // Colonne appréciation éditable (double-clic)
        tblBulletins.setEditable(true);
        colAppreciation.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getAppreciationGenerale() != null
                        ? cellData.getValue().getAppreciationGenerale() : ""));
        colAppreciation.setCellFactory(TextFieldTableCell.forTableColumn());
        colAppreciation.setOnEditCommit(event -> {
            Bulletin b = event.getRowValue();
            b.setAppreciationGenerale(event.getNewValue());
            Task<Void> saveTask = new Task<>() {
                @Override protected Void call() {
                    bulletinRepository.saveOrUpdate(b);
                    return null;
                }
            };
            new Thread(saveTask).start();
        });

        // Coloring des rangs
        colRang.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                if (item.startsWith("1")) {
                    setStyle("-fx-text-fill: #FFD700; -fx-font-weight: bold; -fx-font-size: 13px;");
                } else if (item.startsWith("2")) {
                    setStyle("-fx-text-fill: #C0C0C0; -fx-font-weight: bold;");
                } else if (item.startsWith("3")) {
                    setStyle("-fx-text-fill: #CD7F32; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-text-fill: white;");
                }
            }
        });

        // Coloring des mentions
        colMention.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                switch (item) {
                    case "Excellent"    -> setStyle("-fx-text-fill: #2ECC71; -fx-font-weight: bold;");
                    case "Très bien"    -> setStyle("-fx-text-fill: #4FE0B6; -fx-font-weight: bold;");
                    case "Bien"         -> setStyle("-fx-text-fill: #3498DB; -fx-font-weight: bold;");
                    case "Assez bien"   -> setStyle("-fx-text-fill: #F39C12; -fx-font-weight: bold;");
                    case "Passable"     -> setStyle("-fx-text-fill: #E67E22;");
                    case "Insuffisant"  -> setStyle("-fx-text-fill: #E74C3C;");
                    default             -> setStyle("-fx-text-fill: white;");
                }
            }
        });
    }

    @FXML
    private void handleSelectionChange() {
        Classe classe = cmbClasse.getValue();
        Periode periode = cmbPeriode.getValue();

        if (classe == null || periode == null) {
            bulletinsList.clear();
            resetKpis();
            return;
        }

        lblStatut.setText("Chargement...");
        lblStatut.setStyle("-fx-text-fill: #F39C12;");

        // Charger les bulletins déjà calculés pour cette classe/période
        Task<List<Bulletin>> loadTask = new Task<>() {
            @Override
            protected List<Bulletin> call() {
                try (org.hibernate.Session session = com.prisma.database.DatabaseConnectionManager.getSessionFactory().openSession()) {
                    return session.createQuery(
                            "select distinct b from Bulletin b join fetch b.eleve e left join fetch e.classe join fetch b.periode p left join fetch b.classe c " +
                            "where (c.id = :cId or (c is null and e.classe.id = :cId)) and p.id = :pId " +
                            "order by b.rang asc", Bulletin.class)
                            .setParameter("cId", classe.getId())
                            .setParameter("pId", periode.getId())
                            .getResultList();
                } catch (Exception e) {
                    logger.error("Erreur lors du chargement des bulletins de classe", e);
                    return List.of();
                }
            }

            @Override
            protected void succeeded() {
                List<Bulletin> bulletins = getValue();
                bulletinsList.setAll(bulletins);
                mettreAJourKpis(bulletins);
                btnExporterPDF.setDisable(bulletins.isEmpty());

                if (bulletins.isEmpty()) {
                    lblStatut.setText("Bulletins non calculés");
                    lblStatut.setStyle("-fx-text-fill: #F39C12;");
                } else {
                    lblStatut.setText("Calculé ✓");
                    lblStatut.setStyle("-fx-text-fill: #2ECC71;");
                }
            }
        };
        new Thread(loadTask).start();
    }

    @FXML
    private void handleCalculer() {
        Classe classe = cmbClasse.getValue();
        Periode periode = cmbPeriode.getValue();
        Stage stage = (Stage) tblBulletins.getScene().getWindow();

        if (classe == null || periode == null) {
            NotificationUtil.showError(stage, "Sélectionnez une classe et un trimestre.");
            return;
        }

        lblStatut.setText("Calcul en cours...");
        lblStatut.setStyle("-fx-text-fill: #F39C12;");

        Task<List<Bulletin>> calculTask = new Task<>() {
            @Override
            protected List<Bulletin> call() {
                return calculService.calculerBulletinsClasse(classe.getId(), periode.getId());
            }

            @Override
            protected void succeeded() {
                List<Bulletin> bulletins = getValue();
                // Trier par rang
                List<Bulletin> sorted = bulletins.stream()
                        .sorted((b1, b2) -> Integer.compare(b1.getRang(), b2.getRang()))
                        .toList();
                bulletinsList.setAll(sorted);
                mettreAJourKpis(sorted);
                btnExporterPDF.setDisable(false);

                lblStatut.setText("Calculé ✓  (" + bulletins.size() + " bulletins)");
                lblStatut.setStyle("-fx-text-fill: #2ECC71;");
                NotificationUtil.showSuccess(stage,
                        "Calcul terminé ! " + bulletins.size() + " bulletins générés.");
            }

            @Override
            protected void failed() {
                lblStatut.setText("Échec du calcul !");
                lblStatut.setStyle("-fx-text-fill: #E74C3C;");
                NotificationUtil.showError(stage, "Erreur lors du calcul : " + getException().getMessage());
                logger.error("Erreur calcul bulletins", getException());
            }
        };
        new Thread(calculTask).start();
    }

    private void mettreAJourKpis(List<Bulletin> bulletins) {
        if (bulletins.isEmpty()) {
            resetKpis();
            return;
        }

        lblNbEleves.setText(String.valueOf(bulletins.size()));

        double somme = bulletins.stream()
                .mapToDouble(Bulletin::getMoyenneGenerale)
                .sum();
        double moyenneClasse = somme / bulletins.size();
        lblMoyenneClasse.setText(String.format("%.2f / 20", moyenneClasse));

        // 1er de classe (déjà trié par rang croissant)
        Bulletin premier = bulletins.get(0);
        String nomPremier = premier.getEleve().getNom() + " " + premier.getEleve().getPrenoms();
        lblMeilleurEleve.setText(nomPremier + "\n(" + String.format("%.2f", premier.getMoyenneGenerale()) + ")");
    }

    private void resetKpis() {
        lblNbEleves.setText("—");
        lblMoyenneClasse.setText("—");
        lblMeilleurEleve.setText("—");
        lblStatut.setText("Non calculé");
        lblStatut.setStyle("-fx-text-fill: #F39C12;");
    }

    @FXML
    private void handleGeneratePDF() {
        Stage stage = (Stage) tblBulletins.getScene().getWindow();
        List<Bulletin> bulletins = tblBulletins.getItems();

        if (bulletins == null || bulletins.isEmpty()) {
            NotificationUtil.showError(stage, "Aucun bulletin calculé à exporter.");
            return;
        }

        // Choisir le dossier de destination
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choisir le dossier de destination des bulletins PDF");
        dirChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File outputDir = dirChooser.showDialog(stage);
        if (outputDir == null) return;

        lblStatut.setText("Génération PDF en cours...");
        lblStatut.setStyle("-fx-text-fill: #F39C12;");

        Task<File> pdfTask = new Task<>() {
            @Override
            protected File call() throws Exception {
                return reportService.genererBulletinsClassePdf(bulletins, outputDir);
            }

            @Override
            protected void succeeded() {
                File pdfFile = getValue();
                lblStatut.setText("PDF généré ✓");
                lblStatut.setStyle("-fx-text-fill: #2ECC71;");
                NotificationUtil.showSuccess(stage, "PDF généré : " + pdfFile.getName());

                // Ouvrir le PDF dans le visualiseur par défaut du système
                if (pdfFile != null && pdfFile.exists()) {
                    ouvrirPdfSafely(pdfFile);
                }
            }

            @Override
            protected void failed() {
                lblStatut.setText("Échec génération PDF !");
                lblStatut.setStyle("-fx-text-fill: #E74C3C;");
                NotificationUtil.showError(stage, "Erreur PDF : " + getException().getMessage());
                logger.error("Erreur génération PDF", getException());
            }
        };
        new Thread(pdfTask).start();
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
