package com.prisma.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Utilitaires mathématiques pour le moteur de calcul PRISMA.
 * Toutes les règles issues du Cahier des Charges sont encapsulées ici
 * pour une réutilisation garantie à travers l'application.
 */
public class MathUtil {

    /**
     * Format décimal utilisé pour l'affichage des moyennes sur les bulletins.
     * Utilise la virgule française : 16,65
     */
    private static final DecimalFormat BULLETIN_FORMAT;
    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRENCH);
        symbols.setDecimalSeparator(',');
        BULLETIN_FORMAT = new DecimalFormat("0.00", symbols);
    }

    /**
     * Tronque un nombre décimal à 2 chiffres après la virgule sans aucun arrondi.
     * Règle critique CDC §2.3.3 : 16.65789... → 16.65 et NON 16.66.
     *
     * @param value La valeur décimale à tronquer
     * @return La valeur tronquée à 2 décimales
     */
    public static double truncate(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return value;
        }
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.DOWN)
                .doubleValue();
    }

    /**
     * Formate une moyenne pour affichage sur le bulletin (virgule française).
     * Exemple : 16.65 → "16,65"
     *
     * @param moyenne La moyenne à formater (déjà tronquée)
     * @return La chaîne formatée
     */
    public static String formatMoyenne(double moyenne) {
        return BULLETIN_FORMAT.format(moyenne);
    }

    /**
     * Formate le rang d'un élève selon les règles d'affichage du bulletin PRISMA.
     * Exemple : (1, 25) → "1er / 25 élèves"
     *           (2, 25) → "2ème / 25 élèves"
     *
     * @param rang      Le rang de l'élève (1-indexed)
     * @param totalEleves Le nombre total d'élèves de la classe
     * @return La chaîne formatée pour le bulletin PDF
     */
    public static String formatRang(int rang, int totalEleves) {
        String suffixe = (rang == 1) ? "er" : "ème";
        return rang + suffixe + " / " + totalEleves + " élève" + (totalEleves > 1 ? "s" : "");
    }

    /**
     * Calcule la moyenne pondérée d'une note selon la formule PRISMA.
     * Formule CDC §2.3.1 : note × coefficient
     *
     * @param note        La note de l'élève sur 20
     * @param coefficient Le coefficient de la matière
     * @return La valeur de la moyenne pondérée (points contribués)
     */
    public static double calculerMoyennePonderee(double note, double coefficient) {
        return note * coefficient;
    }
}
