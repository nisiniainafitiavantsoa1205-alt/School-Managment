package com.prisma.report;

import com.prisma.database.DatabaseConnectionManager;
import com.prisma.entity.*;
import com.prisma.repository.impl.GenericRepositoryImpl;
import com.prisma.util.MathUtil;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Implémentation du service de génération des bulletins PDF via JasperReports.
 * Étapes 89 & 90 — Génération unitaire et de classe, compilation du template.
 */
public class ReportServiceImpl implements ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportServiceImpl.class);
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final GenericRepositoryImpl<Bulletin, Integer> bulletinRepository =
            new GenericRepositoryImpl<>(Bulletin.class) {};

    @Override
    public File genererBulletinPdf(Bulletin bulletin, File outputDir) throws Exception {
        // Recharger le bulletin complet avec ses relations dans une session active
        Bulletin b = bulletin;
        if (bulletin != null && bulletin.getId() != null) {
            try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
                Bulletin reloaded = session.createQuery(
                        "select distinct b from Bulletin b join fetch b.eleve e left join fetch e.classe join fetch b.periode p left join fetch b.classe c where b.id = :bId", Bulletin.class)
                        .setParameter("bId", bulletin.getId())
                        .uniqueResult();
                if (reloaded != null) {
                    b = reloaded;
                }
            } catch (Exception ex) {
                logger.warn("Impossible de recharger le bulletin complet ID={}, utilisation de l'objet initial", bulletin.getId(), ex);
            }
        }

        if (b == null || b.getEleve() == null || b.getPeriode() == null) {
            throw new IllegalArgumentException("Le bulletin fourni ou ses relations (élève, période) sont invalides.");
        }

        logger.info("Génération bulletin PDF pour élève : {} {}", b.getEleve().getNom(), b.getEleve().getPrenoms());

        // 1. Compiler le template principal
        InputStream templateStream = getClass().getResourceAsStream("/reports/bulletin.jrxml");
        if (templateStream == null) {
            throw new FileNotFoundException("Template bulletin.jrxml introuvable dans les ressources.");
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);

        // 2. Construire les lignes de notes depuis la base
        List<BulletinLigneBean> lignes = construireLignes(b);

        // 3. Construire les paramètres du rapport
        Map<String, Object> params = construireParametres(b, lignes);

        // 4. Remplir le rapport avec les lignes de notes comme source de données
        JasperPrint jasperPrint = JasperFillManager.fillReport(
                jasperReport,
                params,
                new JRBeanCollectionDataSource(lignes)
        );

        // 5. Exporter en PDF
        if (!outputDir.exists()) outputDir.mkdirs();
        String matriculeClean = b.getEleve().getMatricule() != null ? b.getEleve().getMatricule().replaceAll("[^a-zA-Z0-9_-]", "_") : "eleve";
        String periodeClean = b.getPeriode().getNom() != null ? b.getPeriode().getNom().replaceAll("\\s+", "_") : "trimestre";
        String nomFichier = String.format("bulletin_%s_%s_%s.pdf",
                matriculeClean,
                periodeClean,
                LocalDateTime.now().format(TS_FORMAT));
        File pdfFile = new File(outputDir, nomFichier);

        JRPdfExporter exporter = new JRPdfExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(pdfFile));

        SimplePdfExporterConfiguration config = new SimplePdfExporterConfiguration();
        config.setMetadataAuthor("PRISMA School System");
        config.setMetadataTitle("Bulletin — " + b.getEleve().getNom());
        exporter.setConfiguration(config);
        exporter.exportReport();

        // 6. Mettre à jour le chemin PDF dans l'entité Bulletin
        if (b.getId() != null) {
            try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
                org.hibernate.Transaction tx = session.beginTransaction();
                Bulletin bToUpdate = session.get(Bulletin.class, b.getId());
                if (bToUpdate != null) {
                    bToUpdate.setPdfPath(pdfFile.getAbsolutePath());
                    session.merge(bToUpdate);
                }
                tx.commit();
            } catch (Exception ex) {
                logger.warn("Erreur lors de la mise à jour du pdfPath pour le bulletin ID={}", b.getId(), ex);
            }
        }

        logger.info("Bulletin PDF généré avec succès : {}", pdfFile.getAbsolutePath());
        return pdfFile;
    }

    @Override
    public File genererBulletinsClassePdf(List<Bulletin> bulletins, File outputDir) throws Exception {
        if (bulletins == null || bulletins.isEmpty()) throw new IllegalArgumentException("Liste de bulletins vide.");

        logger.info("Génération PDF de classe : {} bulletins", bulletins.size());

        // Compiler le template une seule fois
        InputStream templateStream = getClass().getResourceAsStream("/reports/bulletin.jrxml");
        if (templateStream == null) {
            throw new FileNotFoundException("Template bulletin.jrxml introuvable dans les ressources.");
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);

        List<JasperPrint> prints = new ArrayList<>();
        Bulletin premierLoaded = null;

        for (Bulletin bulletin : bulletins) {
            Bulletin b = bulletin;
            if (bulletin != null && bulletin.getId() != null) {
                try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
                    Bulletin reloaded = session.createQuery(
                            "select distinct b from Bulletin b join fetch b.eleve e left join fetch e.classe join fetch b.periode p left join fetch b.classe c where b.id = :bId", Bulletin.class)
                            .setParameter("bId", bulletin.getId())
                            .uniqueResult();
                    if (reloaded != null) {
                        b = reloaded;
                    }
                } catch (Exception ex) {
                    logger.warn("Impossible de recharger le bulletin ID={} dans genererBulletinsClassePdf", bulletin.getId(), ex);
                }
            }
            if (b != null && b.getEleve() != null && b.getPeriode() != null) {
                if (premierLoaded == null) premierLoaded = b;
                List<BulletinLigneBean> lignes = construireLignes(b);
                Map<String, Object> params = construireParametres(b, lignes);
                JasperPrint print = JasperFillManager.fillReport(
                        jasperReport, params, new JRBeanCollectionDataSource(lignes));
                prints.add(print);
            }
        }

        if (prints.isEmpty()) {
            throw new IllegalArgumentException("Aucun bulletin valide n'a pu être traité pour la génération PDF.");
        }

        // Fusionner tous les prints en un seul
        JasperPrint merged = mergeReports(prints);

        if (!outputDir.exists()) outputDir.mkdirs();
        Classe classeBul = premierLoaded.getClasse() != null ? premierLoaded.getClasse() : (premierLoaded.getEleve() != null ? premierLoaded.getEleve().getClasse() : null);
        String nomClasseClean = classeBul != null ? classeBul.getNom().replaceAll("[^a-zA-Z0-9_-]", "_") : "classe";
        String nomPeriodeClean = premierLoaded.getPeriode().getNom() != null ? premierLoaded.getPeriode().getNom().replaceAll("\\s+", "_") : "trimestre";

        String nomFichier = String.format("bulletins_classe_%s_%s_%s.pdf",
                nomClasseClean,
                nomPeriodeClean,
                LocalDateTime.now().format(TS_FORMAT));
        File pdfFile = new File(outputDir, nomFichier);

        JRPdfExporter exporter = new JRPdfExporter();
        exporter.setExporterInput(new SimpleExporterInput(merged));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(pdfFile));

        SimplePdfExporterConfiguration config = new SimplePdfExporterConfiguration();
        config.setMetadataAuthor("PRISMA School System");
        config.setMetadataTitle("Bulletins de classe — " + premierLoaded.getPeriode().getNom());
        exporter.setConfiguration(config);
        exporter.exportReport();

        logger.info("PDF de classe généré avec succès : {}", pdfFile.getAbsolutePath());
        return pdfFile;
    }

    // ─────────────────────────────────────────────────────────────────
    // Méthodes privées
    // ─────────────────────────────────────────────────────────────────

    /**
     * Construit les lignes de notes du bulletin depuis la base pour cet élève/période.
     * Respecte la règle CDC §3.1 : les matières sans note apparaissent avec tirets.
     */
    private List<BulletinLigneBean> construireLignes(Bulletin bulletin) {
        List<BulletinLigneBean> lignes = new ArrayList<>();

        try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
            // Charger les coefficients de la classe (dans l'ordre d'affichage)
            Classe classe = bulletin.getClasse() != null ? bulletin.getClasse() : (bulletin.getEleve() != null ? bulletin.getEleve().getClasse() : null);
            if (classe == null) return lignes;

            List<Coefficient> coefs = session.createQuery(
                    "from Coefficient c join fetch c.matiere m " +
                    "where c.classe.id = :cId and c.periode.id = :pId " +
                    "order by m.ordreAffichage",
                    Coefficient.class)
                    .setParameter("cId", classe.getId())
                    .setParameter("pId", bulletin.getPeriode().getId())
                    .getResultList();

            if (coefs.isEmpty()) {
                List<Coefficient> classCoefs = session.createQuery(
                        "from Coefficient c join fetch c.matiere m " +
                        "where c.classe.id = :cId " +
                        "order by m.ordreAffichage",
                        Coefficient.class)
                        .setParameter("cId", classe.getId())
                        .getResultList();
                Map<Integer, Coefficient> coefMap = new LinkedHashMap<>();
                for (Coefficient c : classCoefs) {
                    coefMap.putIfAbsent(c.getMatiere().getId(), c);
                }
                coefs = new ArrayList<>(coefMap.values());
            }

            // Charger les notes de cet élève pour ce trimestre
            List<Note> notes = session.createQuery(
                    "from Note n join fetch n.matiere where n.eleve.id = :eId and n.periode.id = :pId",
                    Note.class)
                    .setParameter("eId", bulletin.getEleve().getId())
                    .setParameter("pId", bulletin.getPeriode().getId())
                    .getResultList();

            Map<Integer, Note> noteMap = new HashMap<>();
            for (Note n : notes) noteMap.put(n.getMatiere().getId(), n);

            int idx = 1;
            for (Coefficient coef : coefs) {
                Note note = noteMap.get(coef.getMatiere().getId());
                String noteStr = "—";
                String coefStr = "—";
                String mPondStr = "—";
                String appStr = "";

                if (note != null) {
                    appStr = note.getAppreciation() != null ? note.getAppreciation() : "";
                    if (note.isAbsent()) {
                        noteStr = "Absent";
                    } else if (note.getValeur() != null) {
                        noteStr = MathUtil.formatMoyenne(note.getValeur());
                        coefStr = String.valueOf((int) coef.getValeur());
                        double points = note.getValeur() * coef.getValeur();
                        mPondStr = String.format("%.2f", points);
                    }
                }

                BulletinLigneBean ligne = new BulletinLigneBean(
                        String.valueOf(idx++),
                        coef.getMatiere().getNom(),
                        noteStr, coefStr, mPondStr, appStr
                );
                lignes.add(ligne);
            }
        } catch (Exception e) {
            logger.error("Erreur construction lignes bulletin", e);
        }
        return lignes;
    }

    /**
     * Construit la map de paramètres JasperReports pour un bulletin.
     */
    private Map<String, Object> construireParametres(Bulletin bulletin,
                                                      List<BulletinLigneBean> lignes) {
        Eleve eleve = bulletin.getEleve();
        Periode periode = bulletin.getPeriode();

        // Compter le nombre total d'élèves dans la classe pour le rang
        int totalEleves = 0;
        try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
            totalEleves = ((Number) session
                    .createQuery("select count(e) from Eleve e where e.classe.id = :cId")
                    .setParameter("cId", eleve.getClasse() != null ? eleve.getClasse().getId() : 0)
                    .uniqueResult()).intValue();
        } catch (Exception e) {
            logger.warn("Impossible de compter les élèves", e);
        }

        String logoPath = "";
        try {
            java.net.URL logoUrl = getClass().getResource("/images/logo_prisma.jpeg");
            if (logoUrl == null) {
                logoUrl = getClass().getResource("/images/logo_prisma.png");
            }
            if (logoUrl != null) {
                logoPath = logoUrl.toExternalForm();
            }
        } catch (Exception e) {
            logger.warn("Impossible de charger le logo PRISMA", e);
        }

        Classe classeBul = bulletin.getClasse() != null ? bulletin.getClasse() : (eleve != null ? eleve.getClasse() : null);
        Map<String, Object> params = new HashMap<>();
        params.put("PARAM_ANNEE_SCOLAIRE",   periode.getAnneeScolaire());
        params.put("PARAM_TRIMESTRE",        periode.getNom().toUpperCase());
        params.put("PARAM_ELEVE_NOM",        eleve != null ? eleve.getNom() : "");
        params.put("PARAM_ELEVE_PRENOM",     (eleve != null && eleve.getPrenoms() != null) ? eleve.getPrenoms() : "");
        params.put("PARAM_CLASSE",           classeBul != null ? classeBul.getNom() : "—");
        params.put("PARAM_NUMERO_APPEL",     eleve.getNumeroAppel() != null ? eleve.getNumeroAppel() : "—");
        params.put("PARAM_TOTAL_COEF",       String.valueOf((int) bulletin.getTotalCoefficient()));
        params.put("PARAM_TOTAL_POINTS",     String.format("%.2f", bulletin.getTotalMoyennePonderee()));
        params.put("PARAM_MOYENNE_GENERALE", MathUtil.formatMoyenne(bulletin.getMoyenneGenerale()));
        params.put("PARAM_RANG",             MathUtil.formatRang(bulletin.getRang(), totalEleves));
        params.put("PARAM_MENTION",          bulletin.getMention() != null ? bulletin.getMention() : "—");
        params.put("PARAM_APPRECIATION",     bulletin.getAppreciationGenerale() != null ? bulletin.getAppreciationGenerale() : "");
        params.put("PARAM_LOGO_PATH",        logoPath);

        // Les lignes de notes sont utilisées directement comme source de données principale

        return params;
    }

    /**
     * Fusionne plusieurs JasperPrint en un seul document multi-pages.
     */
    private JasperPrint mergeReports(List<JasperPrint> prints) {
        JasperPrint merged = prints.get(0);
        for (int i = 1; i < prints.size(); i++) {
            for (net.sf.jasperreports.engine.JRPrintPage page : prints.get(i).getPages()) {
                merged.addPage(page);
            }
        }
        return merged;
    }
}
