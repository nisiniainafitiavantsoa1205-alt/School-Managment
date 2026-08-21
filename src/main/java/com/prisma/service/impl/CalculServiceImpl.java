package com.prisma.service.impl;

import com.prisma.database.DatabaseConnectionManager;
import com.prisma.entity.*;
import com.prisma.repository.GenericRepository;
import com.prisma.repository.impl.GenericRepositoryImpl;
import com.prisma.service.CalculService;
import com.prisma.util.MathUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;

public class CalculServiceImpl implements CalculService {

    private static final Logger logger = LoggerFactory.getLogger(CalculServiceImpl.class);

    private final GenericRepository<Bulletin, Integer> bulletinRepository;
    private final GenericRepository<Mention, Integer> mentionRepository;

    public CalculServiceImpl() {
        this.bulletinRepository = new GenericRepositoryImpl<>(Bulletin.class) {};
        this.mentionRepository = new GenericRepositoryImpl<>(Mention.class) {};
    }

    @Override
    public List<Bulletin> calculerBulletinsClasse(Integer classeId, Integer periodeId) {
        logger.info("Début du calcul des bulletins pour classeId = {}, periodeId = {}", classeId, periodeId);
        
        List<Bulletin> bulletinsSauvegardes = new ArrayList<>();
        Transaction transaction = null;

        try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            // 1. Récupérer la classe et la période
            Classe classe = session.get(Classe.class, classeId);
            Periode periode = session.get(Periode.class, periodeId);
            if (classe == null || periode == null) {
                throw new IllegalArgumentException("Classe ou Période introuvable.");
            }

            // 2. Charger les matières associées à la classe pour cette période (coefficients)
            List<Coefficient> coefficients = session.createQuery(
                    "from Coefficient c join fetch c.matiere where c.classe.id = :cId and c.periode.id = :pId", 
                    Coefficient.class)
                    .setParameter("cId", classeId)
                    .setParameter("pId", periodeId)
                    .getResultList();

            if (coefficients.isEmpty()) {
                logger.info("Aucun coefficient spécifique trouvé pour la classe {} et la période {}, chargement des coefficients généraux de la classe...", classe.getNom(), periode.getNom());
                List<Coefficient> classCoefs = session.createQuery(
                        "from Coefficient c join fetch c.matiere where c.classe.id = :cId", 
                        Coefficient.class)
                        .setParameter("cId", classeId)
                        .getResultList();
                Map<Integer, Coefficient> coefMap = new LinkedHashMap<>();
                for (Coefficient c : classCoefs) {
                    coefMap.putIfAbsent(c.getMatiere().getId(), c);
                }
                coefficients = new ArrayList<>(coefMap.values());
            }

            if (coefficients.isEmpty()) {
                logger.warn("Aucun coefficient paramétré pour la classe {} et la période {}", classe.getNom(), periode.getNom());
            }

            // 3. Charger tous les élèves actifs de la classe avec leur classe
            List<Eleve> eleves = session.createQuery(
                    "select distinct e from Eleve e left join fetch e.classe where e.classe.id = :cId and (e.statut is null or e.statut = 'ACTIF')", Eleve.class)
                    .setParameter("cId", classeId)
                    .getResultList();

            if (eleves.isEmpty()) {
                logger.info("Aucun élève affecté à la classe {}", classe.getNom());
                transaction.commit();
                return bulletinsSauvegardes;
            }

            // 4. Charger toutes les mentions pour attribution
            List<Mention> mentions = session.createQuery("from Mention", Mention.class).getResultList();

            // Structure temporaire pour stocker les moyennes intermédiaires pour le classement
            List<CalculResult> interimResults = new ArrayList<>();

            for (Eleve eleve : eleves) {
                // Charger les notes de cet élève pour ce trimestre
                List<Note> notes = session.createQuery(
                        "from Note n join fetch n.matiere where n.eleve.id = :eId and n.periode.id = :pId", Note.class)
                        .setParameter("eId", eleve.getId())
                        .setParameter("pId", periodeId)
                        .getResultList();

                // Mapper les notes par matière ID pour un accès direct rapide
                Map<Integer, Note> noteMap = new HashMap<>();
                for (Note n : notes) {
                    noteMap.put(n.getMatiere().getId(), n);
                }

                double totalPoints = 0.0;
                double totalCoef = 0.0;

                for (Coefficient coef : coefficients) {
                    Note note = noteMap.get(coef.getMatiere().getId());
                    
                    // Règle critique : exclure coefficient si pas de note ou si l'élève est marqué absent
                    if (note != null && note.getValeur() != null && !note.isAbsent()) {
                        totalPoints += note.getValeur() * coef.getValeur();
                        totalCoef += coef.getValeur();
                    }
                }

                double moyenneGenerale = 0.0;
                if (totalCoef > 0) {
                    moyenneGenerale = MathUtil.truncate(totalPoints / totalCoef);
                }

                CalculResult result = new CalculResult();
                result.eleve = eleve;
                result.totalMoyennePonderee = totalPoints;
                result.totalCoefficient = totalCoef;
                result.moyenneGenerale = moyenneGenerale;
                
                interimResults.add(result);
            }

            // 5. Tri décroissant sur la moyenne générale pour établir le classement
            interimResults.sort((r1, r2) -> Double.compare(r2.moyenneGenerale, r1.moyenneGenerale));

            // 6. Attribution du rang (gestion des ex-æquos standard compétitif 1, 1, 3)
            int currentRank = 1;
            int count = 0;
            double lastMoyenne = -1.0;

            for (CalculResult res : interimResults) {
                count++;
                if (res.moyenneGenerale != lastMoyenne) {
                    currentRank = count;
                }
                res.rang = currentRank;
                lastMoyenne = res.moyenneGenerale;

                // 7. Attribution de la mention automatique
                res.mention = "Aucune";
                for (Mention m : mentions) {
                    if (res.moyenneGenerale >= m.getMoyenneMin() && res.moyenneGenerale <= m.getMoyenneMax()) {
                        res.mention = m.getNom();
                        break;
                    }
                }

                // 8. Sauvegarder ou mettre à jour le Bulletin en DB
                // Vérifier si le bulletin existe déjà pour cet élève et ce trimestre
                Bulletin bulletin = session.createQuery(
                        "select distinct b from Bulletin b join fetch b.eleve e left join fetch e.classe join fetch b.periode p left join fetch b.classe c where e.id = :eId and p.id = :pId", Bulletin.class)
                        .setParameter("eId", res.eleve.getId())
                        .setParameter("pId", periodeId)
                        .uniqueResultOptional().orElse(null);

                if (bulletin == null) {
                    bulletin = new Bulletin();
                    bulletin.setEleve(res.eleve);
                    bulletin.setPeriode(periode);
                }
                bulletin.setClasse(classe);

                // Si le bulletin est verrouillé (clôturé), on ne l'écrase pas
                if (!bulletin.isLocked()) {
                    bulletin.setTotalMoyennePonderee(res.totalMoyennePonderee);
                    bulletin.setTotalCoefficient(res.totalCoefficient);
                    bulletin.setMoyenneGenerale(res.moyenneGenerale);
                    bulletin.setRang(res.rang);
                    bulletin.setMention(res.mention);
                    bulletin.setDateGeneration(LocalDateTime.now());

                    if (bulletin.getId() == null) {
                        session.persist(bulletin);
                    } else {
                        bulletin = session.merge(bulletin);
                    }
                }
                
                bulletinsSauvegardes.add(bulletin);
            }

            transaction.commit();
            logger.info("Fin du calcul avec succès. {} bulletins traités.", bulletinsSauvegardes.size());

        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            logger.error("Erreur lors du calcul des bulletins", e);
            throw new RuntimeException("Erreur de calcul des bulletins", e);
        }

        return bulletinsSauvegardes;
    }

    // Structure interne d'aide aux calculs
    private static class CalculResult {
        Eleve eleve;
        double totalMoyennePonderee;
        double totalCoefficient;
        double moyenneGenerale;
        int rang;
        String mention;
    }
}
