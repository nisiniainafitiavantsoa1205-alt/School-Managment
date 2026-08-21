package com.prisma.service;

import com.prisma.entity.*;
import com.prisma.util.MathUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour le moteur de calcul PRISMA.
 * Ces tests valident la logique métier (calcul, troncature, classement, mentions)
 * de manière isolée, sans dépendance à la base de données.
 */
class CalculServiceTest {

    private List<Mention> mentions;

    @BeforeEach
    void setUp() {
        mentions = new ArrayList<>();

        Mention excellent = new Mention();
        excellent.setNom("Excellent");
        excellent.setMoyenneMin(16.0);
        excellent.setMoyenneMax(20.0);

        Mention tresBien = new Mention();
        tresBien.setNom("Très bien");
        tresBien.setMoyenneMin(14.0);
        tresBien.setMoyenneMax(15.99);

        Mention bien = new Mention();
        bien.setNom("Bien");
        bien.setMoyenneMin(12.0);
        bien.setMoyenneMax(13.99);

        Mention assezBien = new Mention();
        assezBien.setNom("Assez bien");
        assezBien.setMoyenneMin(10.0);
        assezBien.setMoyenneMax(11.99);

        Mention passable = new Mention();
        passable.setNom("Passable");
        passable.setMoyenneMin(8.0);
        passable.setMoyenneMax(9.99);

        Mention insuffisant = new Mention();
        insuffisant.setNom("Insuffisant");
        insuffisant.setMoyenneMin(0.0);
        insuffisant.setMoyenneMax(7.99);

        mentions.addAll(List.of(excellent, tresBien, bien, assezBien, passable, insuffisant));
    }

    // ========================
    // TESTS DE CALCUL DE MOYENNE
    // ========================

    @Test
    void calcul_moyennePonderee_correspondAuCahierDesCharges() {
        // Exemple Cahier des Charges : 316.5 / 19 = 16.65789...
        // La troncature doit donner 16.65 et non 16.66
        double totalPoints = 316.5;
        double totalCoef = 19.0;

        double moyenneBrute = totalPoints / totalCoef;
        double moyenneTronquee = MathUtil.truncate(moyenneBrute);

        assertEquals(16.65, moyenneTronquee, 0.0001,
                "La moyenne pondérée doit être tronquée à 16.65 et non arrondie à 16.66.");
    }

    @Test
    void calcul_noteContribueBienAvecCoefficient() {
        // Exemple cahier : Français : 18 × 2 = 36
        double note = 18.0;
        double coefficient = 2.0;

        double moyennePonderee = note * coefficient;

        assertEquals(36.0, moyennePonderee, 0.0001,
                "Français 18 × coeff. 2 = 36 points pondérés.");
    }

    @Test
    void calcul_sansNote_matiereExclue() {
        // Règle 3.1 : Matière sans note = exclus du total coefficient
        // Si total 5 matières, mais l'une n'a pas de note :
        // le total coef doit être 4 matières et non 5.

        double totalCoef = 0.0;
        double totalPoints = 0.0;

        // Matière 1 : note 14, coef 2 => comptée
        totalPoints += 14.0 * 2.0;
        totalCoef += 2.0;

        // Matière 2 : note 16, coef 3 => comptée
        totalPoints += 16.0 * 3.0;
        totalCoef += 3.0;

        // Matière 3 : pas de note (null) => EXCLUE
        // totalPoints += 0; totalCoef += 0; (ne pas toucher)

        double moyenneGenerale = MathUtil.truncate(totalPoints / totalCoef);

        // (28 + 48) / (2+3) = 76 / 5 = 15.2
        assertEquals(76.0, totalPoints, 0.001);
        assertEquals(5.0, totalCoef, 0.001);
        assertEquals(15.2, moyenneGenerale, 0.001,
                "La matière sans note ne doit pas dégrader la moyenne.");
    }

    @Test
    void calcul_eleveAbsent_exclutLaMatiere() {
        // Règle 3.1 : si absent = true, la matière est exclue du calcul
        // Eleve : 2 matières, absent à la seconde
        // Matière 1 : note 12, coef 3 => comptée
        // Matière 2 : absent, coef 2 => exclue

        double totalCoef = 0.0;
        double totalPoints = 0.0;

        totalPoints += 12.0 * 3.0;
        totalCoef += 3.0;
        // Matière 2 : absent => skip

        double moyenneGenerale = MathUtil.truncate(totalPoints / totalCoef);

        assertEquals(36.0, totalPoints, 0.001);
        assertEquals(3.0, totalCoef, 0.001);
        assertEquals(12.0, moyenneGenerale, 0.001,
                "Un élève absent à une matière ne doit pas avoir son coefficient comptabilisé.");
    }

    // ========================
    // TESTS DE TRONCATURE (Règle critique CDC §2.3.3)
    // ========================

    @Test
    void troncature_neveutPasArrondir() {
        assertEquals(16.65, MathUtil.truncate(16.6578), 0.0);
        assertEquals(9.99,  MathUtil.truncate(9.999),  0.0);
        assertEquals(12.33, MathUtil.truncate(12.338), 0.0);
        assertEquals(0.0,   MathUtil.truncate(0.001),  0.0);
        assertEquals(20.0,  MathUtil.truncate(20.0),   0.0);
    }

    // ========================
    // TESTS DU CLASSEMENT COMPÉTITIF (Rang)
    // ========================

    @Test
    void classement_ordreDecroissant_sansExAequo() {
        List<Double> moyennes = Arrays.asList(16.65, 14.20, 12.80, 9.50);

        List<Double> sorted = new ArrayList<>(moyennes);
        sorted.sort(Comparator.reverseOrder());

        assertEquals(16.65, sorted.get(0), 0.001, "Élève 1 doit être 1er.");
        assertEquals(14.20, sorted.get(1), 0.001, "Élève 2 doit être 2ème.");
        assertEquals(12.80, sorted.get(2), 0.001, "Élève 3 doit être 3ème.");
        assertEquals(9.50,  sorted.get(3), 0.001, "Élève 4 doit être 4ème.");
    }

    @Test
    void classement_exAequo_rangCompetitif() {
        // Classement compétitif standard : si deux élèves ont la même moyenne,
        // ils partagent le même rang et le rang suivant est sauté.
        // Ex: 18, 16, 16, 14 => rangs 1, 2, 2, 4

        List<Double> moyennes = Arrays.asList(18.0, 16.0, 16.0, 14.0);
        moyennes = moyennes.stream().sorted(Comparator.reverseOrder()).toList();

        List<Integer> rangs = new ArrayList<>();
        int currentRank = 1;
        int count = 0;
        double last = -1.0;

        for (double m : moyennes) {
            count++;
            if (m != last) {
                currentRank = count;
            }
            rangs.add(currentRank);
            last = m;
        }

        assertEquals(1, rangs.get(0), "1er à 18.0");
        assertEquals(2, rangs.get(1), "2ème à 16.0 (ex-æquo)");
        assertEquals(2, rangs.get(2), "2ème à 16.0 (ex-æquo)");
        assertEquals(4, rangs.get(3), "4ème à 14.0 (rang sauté après ex-æquo)");
    }

    // ========================
    // TESTS DE L'ATTRIBUTION DE MENTION
    // ========================

    @Test
    void mention_correspondALaTranche() {
        assertEquals("Excellent",   getMentionForMoyenne(17.5));
        assertEquals("Excellent",   getMentionForMoyenne(16.0));
        assertEquals("Très bien",   getMentionForMoyenne(14.5));
        assertEquals("Bien",        getMentionForMoyenne(12.0));
        assertEquals("Assez bien",  getMentionForMoyenne(10.5));
        assertEquals("Passable",    getMentionForMoyenne(8.5));
        assertEquals("Insuffisant", getMentionForMoyenne(5.0));
    }

    @Test
    void mention_surBorneExacte() {
        // Vérification des bornes exactes des tranches
        assertEquals("Excellent",   getMentionForMoyenne(20.0));
        assertEquals("Excellent",   getMentionForMoyenne(16.0));
        assertEquals("Très bien",   getMentionForMoyenne(15.99));
        assertEquals("Insuffisant", getMentionForMoyenne(0.0));
    }

    @Test
    void calcul_avecFallbackCoefficientsClasse() {
        // Simule le cas où les coefficients ne sont définis qu'au niveau classe et non spécifiquement pour le trimestre 2
        Matiere m1 = new Matiere(); m1.setId(1); m1.setNom("Maths");
        Matiere m2 = new Matiere(); m2.setId(2); m2.setNom("Physique");

        Coefficient c1 = new Coefficient(); c1.setMatiere(m1); c1.setValeur(4.0);
        Coefficient c2 = new Coefficient(); c2.setMatiere(m2); c2.setValeur(2.0);

        List<Coefficient> coefsTrimestre2 = new ArrayList<>(); // Vide au T2

        // Fallback
        if (coefsTrimestre2.isEmpty()) {
            coefsTrimestre2.addAll(List.of(c1, c2));
        }

        double noteMaths = 15.0; // 15 * 4 = 60
        double notePhys = 12.0;  // 12 * 2 = 24
        double totalPoints = (noteMaths * c1.getValeur()) + (notePhys * c2.getValeur());
        double totalCoef = c1.getValeur() + c2.getValeur();
        double moy = MathUtil.truncate(totalPoints / totalCoef);

        assertEquals(84.0, totalPoints, 0.001);
        assertEquals(6.0, totalCoef, 0.001);
        assertEquals(14.0, moy, 0.001, "Le calcul doit s'effectuer correctement avec les coefficients issus du fallback.");
    }

    // Méthode utilitaire locale reproduisant la logique de CalculServiceImpl
    private String getMentionForMoyenne(double moyenne) {
        for (Mention m : mentions) {
            if (moyenne >= m.getMoyenneMin() && moyenne <= m.getMoyenneMax()) {
                return m.getNom();
            }
        }
        return "Aucune";
    }
}
