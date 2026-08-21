package com.prisma.controller;

import com.prisma.database.DatabaseConnectionManager;
import com.prisma.entity.Periode;
import com.prisma.repository.*;
import com.prisma.repository.impl.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @FXML private Label lblTotalEleves;
    @FXML private Label lblTotalClasses;
    @FXML private Label lblTotalMatieres;
    @FXML private Label lblTrimestreActif;

    @FXML private BarChart<String, Number> chartEffectifs;
    @FXML private LineChart<String, Number> chartPerformances;

    private final EleveRepository eleveRepository;
    private final ClasseRepository classeRepository;
    private final MatiereRepository matiereRepository;
    private final PeriodeRepository periodeRepository;

    public DashboardController() {
        this.eleveRepository = new EleveRepositoryImpl();
        this.classeRepository = new ClasseRepositoryImpl();
        this.matiereRepository = new MatiereRepositoryImpl();
        this.periodeRepository = new PeriodeRepositoryImpl();
    }

    @FXML
    public void initialize() {
        chargerStatistiques();
    }

    private void chargerStatistiques() {
        Task<Void> task = new Task<>() {
            private long totalEleves;
            private int totalClasses;
            private int totalMatieres;
            private String nomTrimestre = "Aucun trimestre actif";
            
            private List<Object[]> effectifsData;
            private List<Object[]> performancesData;

            @Override
            protected Void call() {
                // 1. Charger les KPI de base
                totalEleves = eleveRepository.countSearch(null, null);
                totalClasses = classeRepository.findAll().size();
                totalMatieres = matiereRepository.findAll().size();
                
                Optional<Periode> activePeriode = periodeRepository.findActive();
                if (activePeriode.isPresent()) {
                    nomTrimestre = activePeriode.get().getNom() + " (" + activePeriode.get().getAnneeScolaire() + ")";
                }

                // 2. Charger les données du BarChart (effectifs par classe)
                try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
                    effectifsData = session.createQuery(
                            "select c.nom, count(e) from Eleve e join e.classe c group by c.nom order by c.nom",
                            Object[].class).getResultList();

                    performancesData = session.createQuery(
                            "select c.nom, avg(b.moyenneGenerale) from Bulletin b join b.eleve e join e.classe c group by c.nom order by c.nom",
                            Object[].class).getResultList();
                } catch (Exception e) {
                    logger.error("Erreur lors de la récupération des données de graphiques", e);
                }
                return null;
            }

            @Override
            protected void succeeded() {
                // Mettre à jour l'UI sur le thread JavaFX
                lblTotalEleves.setText(String.valueOf(totalEleves));
                lblTotalClasses.setText(String.valueOf(totalClasses));
                lblTotalMatieres.setText(String.valueOf(totalMatieres));
                lblTrimestreActif.setText(nomTrimestre);

                // Remplir le graphique d'effectifs
                XYChart.Series<String, Number> seriesEffectifs = new XYChart.Series<>();
                if (effectifsData != null && !effectifsData.isEmpty()) {
                    for (Object[] row : effectifsData) {
                        seriesEffectifs.getData().add(new XYChart.Data<>((String) row[0], (Number) row[1]));
                    }
                } else {
                    // Données fictives si vide pour l'aperçu esthétique
                    seriesEffectifs.getData().add(new XYChart.Data<>("6ème A", 24));
                    seriesEffectifs.getData().add(new XYChart.Data<>("5ème A", 28));
                    seriesEffectifs.getData().add(new XYChart.Data<>("4ème A", 22));
                    seriesEffectifs.getData().add(new XYChart.Data<>("3ème A", 19));
                }
                chartEffectifs.getData().clear();
                chartEffectifs.getData().add(seriesEffectifs);

                // Remplir le graphique de performances
                XYChart.Series<String, Number> seriesPerformances = new XYChart.Series<>();
                if (performancesData != null && !performancesData.isEmpty()) {
                    for (Object[] row : performancesData) {
                        seriesPerformances.getData().add(new XYChart.Data<>((String) row[0], (Number) row[1]));
                    }
                } else {
                    // Données fictives si vide pour l'aperçu esthétique
                    seriesPerformances.getData().add(new XYChart.Data<>("6ème A", 12.5));
                    seriesPerformances.getData().add(new XYChart.Data<>("5ème A", 14.2));
                    seriesPerformances.getData().add(new XYChart.Data<>("4ème A", 11.8));
                    seriesPerformances.getData().add(new XYChart.Data<>("3ème A", 13.0));
                }
                chartPerformances.getData().clear();
                chartPerformances.getData().add(seriesPerformances);
            }
        };

        new Thread(task).start();
    }
}
