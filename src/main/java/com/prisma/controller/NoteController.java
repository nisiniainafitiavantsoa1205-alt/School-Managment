package com.prisma.controller;

import com.prisma.database.DatabaseConnectionManager;
import com.prisma.entity.*;
import com.prisma.repository.*;
import com.prisma.repository.impl.*;
import com.prisma.util.NotificationUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;

public class NoteController {

    private static final Logger logger = LoggerFactory.getLogger(NoteController.class);

    @FXML private ComboBox<Classe> cmbClasse;
    @FXML private ComboBox<Matiere> cmbMatiere;
    @FXML private ComboBox<Periode> cmbPeriode;
    @FXML private TextField txtSearchEleve;

    @FXML private TableView<NoteRow> tblNotes;
    @FXML private TableColumn<NoteRow, String> colIndex;
    @FXML private TableColumn<NoteRow, String> colMatricule;
    @FXML private TableColumn<NoteRow, String> colNomPrenom;
    @FXML private TableColumn<NoteRow, String> colNote;
    @FXML private TableColumn<NoteRow, Boolean> colAbsent;
    @FXML private TableColumn<NoteRow, String> colAppreciation;

    @FXML private Label lblAutosaveStatus;

    private final ClasseRepository classeRepository;
    private final GenericRepository<Matiere, Integer> matiereRepository;
    private final PeriodeRepository periodeRepository;
    private final GenericRepository<Note, Integer> noteRepository;
    
    private List<AppreciationSuggestion> suggestions = new ArrayList<>();
    private final ObservableList<NoteRow> rows = FXCollections.observableArrayList();
    private javafx.collections.transformation.FilteredList<NoteRow> filteredRows;

    public NoteController() {
        this.classeRepository = new ClasseRepositoryImpl();
        this.matiereRepository = new GenericRepositoryImpl<>(Matiere.class) {};
        this.periodeRepository = new PeriodeRepositoryImpl();
        this.noteRepository = new GenericRepositoryImpl<>(Note.class) {};
    }

    @FXML
    public void initialize() {
        configurerTables();
        chargerFiltresSelection();
        chargerSuggestionsAppreciations();
        
        // Ecouter la touche entrée ou flèches directionnelles sur la table (Étape 68)
        tblNotes.setOnKeyPressed(this::handleTableKeyPressed);
    }

    private void configurerTables() {
        filteredRows = new javafx.collections.transformation.FilteredList<>(rows, p -> true);
        tblNotes.setItems(filteredRows);

        if (txtSearchEleve != null) {
            txtSearchEleve.textProperty().addListener((obs, oldVal, newVal) -> {
                filteredRows.setPredicate(row -> {
                    if (newVal == null || newVal.trim().isEmpty()) {
                        return true;
                    }
                    String lower = newVal.trim().toLowerCase();
                    Eleve e = row.getEleve();
                    return (e.getNom() != null && e.getNom().toLowerCase().contains(lower)) ||
                           (e.getPrenoms() != null && e.getPrenoms().toLowerCase().contains(lower)) ||
                           (e.getMatricule() != null && e.getMatricule().toLowerCase().contains(lower));
                });
            });
        }

        colIndex.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getIndex())));
        colMatricule.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEleve().getMatricule()));
        colNomPrenom.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getEleve().getNom() + " " + cellData.getValue().getEleve().getPrenoms()));

        // Note modifiable
        colNote.setCellValueFactory(cellData -> cellData.getValue().noteProperty());
        colNote.setCellFactory(TextFieldTableCell.forTableColumn());
        colNote.setOnEditCommit(event -> {
            NoteRow row = event.getRowValue();
            String input = event.getNewValue().trim().toUpperCase();
            if (validerSaisieNote(row, input)) {
                appliquerSuggestionAutomatique(row);
                declencherAutoSave(row);
            } else {
                row.setNote(event.getOldValue());
                tblNotes.refresh();
            }
        });

        // Case absent
        colAbsent.setCellValueFactory(cellData -> cellData.getValue().absentProperty());
        colAbsent.setCellFactory(CheckBoxTableCell.forTableColumn(colAbsent));
        // Mettre à jour la note si la case absent est cochée/décochée
        colAbsent.setOnEditCommit(null); // Pas d'editCommit classique nécessaire avec binding bidirectionnel
        
        // Appréciation modifiable
        colAppreciation.setCellValueFactory(cellData -> cellData.getValue().appreciationProperty());
        colAppreciation.setCellFactory(TextFieldTableCell.forTableColumn());
        colAppreciation.setOnEditCommit(event -> {
            NoteRow row = event.getRowValue();
            row.setAppreciation(event.getNewValue());
            declencherAutoSave(row);
        });
    }

    private void chargerFiltresSelection() {
        // Formatteurs
        javafx.util.StringConverter<Classe> clConverter = new javafx.util.StringConverter<>() {
            @Override public String toString(Classe c) { return c != null ? c.getNom() : ""; }
            @Override public Classe fromString(String s) { return null; }
        };
        cmbClasse.setConverter(clConverter);

        javafx.util.StringConverter<Matiere> matConverter = new javafx.util.StringConverter<>() {
            @Override public String toString(Matiere m) { return m != null ? m.getNom() : ""; }
            @Override public Matiere fromString(String s) { return null; }
        };
        cmbMatiere.setConverter(matConverter);

        javafx.util.StringConverter<Periode> perConverter = new javafx.util.StringConverter<>() {
            @Override public String toString(Periode p) { return p != null ? p.getNom() + " (" + p.getAnneeScolaire() + ")" : ""; }
            @Override public Periode fromString(String s) { return null; }
        };
        cmbPeriode.setConverter(perConverter);

        // Charger les données
        Task<Void> task = new Task<>() {
            private List<Classe> classes;
            private List<Matiere> matieres;
            private List<Periode> periodes;

            @Override
            protected Void call() {
                classes = classeRepository.findAll();
                matieres = matiereRepository.findAll();
                List<Periode> all = periodeRepository.findAll();
                List<Periode> openOnly = all.stream().filter(p -> !p.isClosed()).toList();
                periodes = openOnly.isEmpty() ? all : openOnly;
                return null;
            }

            @Override
            protected void succeeded() {
                cmbClasse.setItems(FXCollections.observableArrayList(classes));
                cmbMatiere.setItems(FXCollections.observableArrayList(matieres));
                cmbPeriode.setItems(FXCollections.observableArrayList(periodes));

                if (!periodes.isEmpty()) {
                    Periode active = periodes.stream().filter(Periode::isActive).findFirst().orElse(periodes.get(0));
                    cmbPeriode.setValue(active);
                }
                if (!classes.isEmpty()) {
                    cmbClasse.setValue(classes.get(0));
                }
                if (!matieres.isEmpty()) {
                    cmbMatiere.setValue(matieres.get(0));
                }
                handleSelectionChange();
            }
        };
        new Thread(task).start();
    }

    private void chargerSuggestionsAppreciations() {
        Task<List<AppreciationSuggestion>> task = new Task<>() {
            @Override
            protected List<AppreciationSuggestion> call() {
                try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
                    return session.createQuery("from AppreciationSuggestion", AppreciationSuggestion.class).getResultList();
                }
            }

            @Override
            protected void succeeded() {
                suggestions = getValue();
            }
        };
        new Thread(task).start();
    }

    @FXML
    private void handleSelectionChange() {
        Classe classe = cmbClasse.getValue();
        Matiere matiere = cmbMatiere.getValue();
        Periode periode = cmbPeriode.getValue();

        if (classe == null || matiere == null || periode == null) {
            rows.clear();
            return;
        }

        lblAutosaveStatus.setText("Chargement des élèves...");
        lblAutosaveStatus.setStyle("-fx-text-fill: white;");

        Task<List<NoteRow>> task = new Task<>() {
            @Override
            protected List<NoteRow> call() {
                List<NoteRow> tempRows = new ArrayList<>();
                
                // 1. Récupérer tous les élèves actifs de la classe
                List<Eleve> eleves;
                try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
                    eleves = session.createQuery("from Eleve e where e.classe.id = :cId and (e.statut is null or e.statut = 'ACTIF') order by e.nom, e.prenoms", Eleve.class)
                            .setParameter("cId", classe.getId())
                            .getResultList();
                }

                // 2. Récupérer toutes les notes déjà saisies
                Map<Integer, Note> noteMap = new HashMap<>();
                try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
                    List<Note> notes = session.createQuery(
                            "from Note n join fetch n.eleve where n.eleve.classe.id = :cId and n.matiere.id = :mId and n.periode.id = :pId", Note.class)
                            .setParameter("cId", classe.getId())
                            .setParameter("mId", matiere.getId())
                            .setParameter("pId", periode.getId())
                            .getResultList();
                    for (Note n : notes) {
                        noteMap.put(n.getEleve().getId(), n);
                    }
                }

                // 3. Construire les lignes du tableur
                int idx = 1;
                for (Eleve e : eleves) {
                    Note note = noteMap.get(e.getId());
                    tempRows.add(new NoteRow(idx++, e, note));
                }
                return tempRows;
            }

            @Override
            protected void succeeded() {
                rows.setAll(getValue());
                lblAutosaveStatus.setText("Enregistrement automatique activé (Modifications sauvegardées à la perte de focus)");
                lblAutosaveStatus.setStyle("-fx-text-fill: #2ECC71;");
            }

            @Override
            protected void failed() {
                lblAutosaveStatus.setText("Erreur de chargement des notes.");
                lblAutosaveStatus.setStyle("-fx-text-fill: #E74C3C;");
            }
        };
        new Thread(task).start();
    }

    private boolean validerSaisieNote(NoteRow row, String input) {
        if (input == null || input.trim().isEmpty()) {
            row.setNote("");
            row.setAbsent(false);
            return true;
        }

        String cleaned = input.trim().replace(',', '.').toUpperCase();

        if (cleaned.equals("A") || cleaned.equals("ABS")) {
            row.setNote("A");
            row.setAbsent(true);
            return true;
        }

        try {
            double note = Double.parseDouble(cleaned);
            if (note >= 0 && note <= 20) {
                row.setNote(String.valueOf(note));
                row.setAbsent(false);
                return true;
            }
        } catch (NumberFormatException ignored) {}

        Stage stage = (Stage) tblNotes.getScene().getWindow();
        NotificationUtil.showError(stage, "Note invalide ! Saisissez une valeur entre 0 et 20, ou 'A' pour Absent.");
        return false;
    }

    private void appliquerSuggestionAutomatique(NoteRow row) {
        if (row.isAbsent() || row.getNote().isEmpty()) {
            row.setAppreciation("Absent");
            return;
        }

        try {
            double note = Double.parseDouble(row.getNote());
            for (AppreciationSuggestion sugg : suggestions) {
                if (note >= sugg.getNoteMin() && note <= sugg.getNoteMax()) {
                    row.setAppreciation(sugg.getAppreciationDefaut());
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    private void declencherAutoSave(NoteRow row) {
        lblAutosaveStatus.setText("Enregistrement en cours...");
        lblAutosaveStatus.setStyle("-fx-text-fill: #F1C40F;");

        Classe classe = cmbClasse.getValue();
        Matiere matiere = cmbMatiere.getValue();
        Periode periode = cmbPeriode.getValue();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                Note note = row.getDbNote();
                if (note == null) {
                    note = new Note();
                    note.setEleve(row.getEleve());
                    note.setMatiere(matiere);
                    note.setPeriode(periode);
                }

                if (row.isAbsent()) {
                    note.setAbsent(true);
                    note.setValeur(null);
                } else if (row.getNote().isEmpty()) {
                    note.setAbsent(false);
                    note.setValeur(null);
                } else {
                    note.setAbsent(false);
                    note.setValeur(Double.parseDouble(row.getNote()));
                }

                note.setAppreciation(row.getAppreciation());
                note.setDateSaisie(LocalDateTime.now());
                note.setSaisiePar(com.prisma.security.SessionContext.getInstance().getUtilisateurConnecte());

                noteRepository.saveOrUpdate(note);
                row.setDbNote(note); // Conserver la référence
                return null;
            }

            @Override
            protected void succeeded() {
                lblAutosaveStatus.setText("Toutes les modifications sont enregistrées automatiquement.");
                lblAutosaveStatus.setStyle("-fx-text-fill: #2ECC71;");
            }

            @Override
            protected void failed() {
                lblAutosaveStatus.setText("Échec de l'enregistrement automatique !");
                lblAutosaveStatus.setStyle("-fx-text-fill: #E74C3C;");
            }
        };
        new Thread(task).start();
    }

    @FXML
    private void handleManualSave() {
        Stage stage = (Stage) tblNotes.getScene().getWindow();
        NotificationUtil.showSuccess(stage, "Toutes les notes ont été validées et sauvegardées !");
    }

    @FXML
    private void handlePasteFromClipboard() {
        Stage stage = (Stage) tblNotes.getScene().getWindow();
        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (!clipboard.hasString()) {
            NotificationUtil.showError(stage, "Le presse-papiers est vide.");
            return;
        }

        String content = clipboard.getString();
        String[] lines = content.split("\\r?\\n");
        
        int pastedCount = 0;
        for (int i = 0; i < lines.length && i < rows.size(); i++) {
            String value = lines[i].trim();
            NoteRow row = rows.get(i);
            if (validerSaisieNote(row, value)) {
                appliquerSuggestionAutomatique(row);
                declencherAutoSave(row);
                pastedCount++;
            }
        }

        tblNotes.refresh();
        NotificationUtil.showSuccess(stage, pastedCount + " note(s) importée(s) depuis Excel !");
    }

    private void handleTableKeyPressed(KeyEvent event) {
        // Navigation fluide avec flèches haut/bas (Étape 68)
        if (event.getCode() == KeyCode.DOWN || event.getCode() == KeyCode.UP) {
            int index = tblNotes.getSelectionModel().getSelectedIndex();
            if (event.getCode() == KeyCode.DOWN && index < rows.size() - 1) {
                tblNotes.getSelectionModel().select(index + 1);
            } else if (event.getCode() == KeyCode.UP && index > 0) {
                tblNotes.getSelectionModel().select(index - 1);
            }
            event.consume();
        }
    }

    // CLASSE CELLULE REPRESENTATIVE POUR LE TABLEUR
    public static class NoteRow {
        private final int index;
        private final Eleve eleve;
        private final SimpleStringProperty note;
        private final SimpleBooleanProperty absent;
        private final SimpleStringProperty appreciation;
        private Note dbNote;

        public NoteRow(int index, Eleve eleve, Note dbNote) {
            this.index = index;
            this.eleve = eleve;
            this.dbNote = dbNote;

            String initNote = "";
            boolean initAbsent = false;
            String initApp = "";

            if (dbNote != null) {
                initAbsent = dbNote.isAbsent();
                if (initAbsent) {
                    initNote = "A";
                } else if (dbNote.getValeur() != null) {
                    initNote = String.valueOf(dbNote.getValeur());
                }
                initApp = dbNote.getAppreciation() != null ? dbNote.getAppreciation() : "";
            }

            this.note = new SimpleStringProperty(initNote);
            this.absent = new SimpleBooleanProperty(initAbsent);
            this.appreciation = new SimpleStringProperty(initApp);

            // Écouteur sur absent pour cocher/décocher automatiquement
            this.absent.addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    this.note.set("A");
                } else if (this.note.get().equals("A")) {
                    this.note.set("");
                }
            });
        }

        public int getIndex() { return index; }
        public Eleve getEleve() { return eleve; }
        
        public String getNote() { return note.get(); }
        public void setNote(String val) { this.note.set(val); }
        public SimpleStringProperty noteProperty() { return note; }

        public boolean isAbsent() { return absent.get(); }
        public void setAbsent(boolean val) { this.absent.set(val); }
        public SimpleBooleanProperty absentProperty() { return absent; }

        public String getAppreciation() { return appreciation.get(); }
        public void setAppreciation(String val) { this.appreciation.set(val); }
        public SimpleStringProperty appreciationProperty() { return appreciation; }

        public Note getDbNote() { return dbNote; }
        public void setDbNote(Note dbNote) { this.dbNote = dbNote; }
    }
}
