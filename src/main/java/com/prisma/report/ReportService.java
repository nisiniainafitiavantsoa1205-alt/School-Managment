package com.prisma.report;

import com.prisma.entity.Bulletin;
import java.io.File;
import java.util.List;

/**
 * Service de génération des bulletins PDF.
 * Étape 89 — Service d'exportation PDF.
 */
public interface ReportService {

    /**
     * Génère le PDF du bulletin d'un élève pour un trimestre donné.
     * Le fichier est sauvegardé dans le répertoire de sortie et le chemin
     * est mis à jour dans l'entité Bulletin en base.
     *
     * @param bulletin     Le bulletin calculé (avec rang, moyenne, mention)
     * @param outputDir    Répertoire de destination du PDF
     * @return             Le fichier PDF généré
     * @throws Exception   En cas d'échec de compilation ou de remplissage
     */
    File genererBulletinPdf(Bulletin bulletin, File outputDir) throws Exception;

    /**
     * Génère les PDF pour tous les bulletins d'une classe et d'un trimestre.
     * Les PDFs sont fusionnés en un seul fichier d'impression A4 paysage.
     *
     * @param bulletins    Liste des bulletins de la classe (déjà triés par rang)
     * @param outputDir    Répertoire de destination
     * @return             Le fichier PDF fusionné (tous les élèves)
     * @throws Exception   En cas d'échec
     */
    File genererBulletinsClassePdf(List<Bulletin> bulletins, File outputDir) throws Exception;
}
