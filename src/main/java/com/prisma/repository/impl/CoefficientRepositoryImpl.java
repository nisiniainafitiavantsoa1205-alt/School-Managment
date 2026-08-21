package com.prisma.repository.impl;

import com.prisma.entity.Coefficient;
import com.prisma.repository.CoefficientRepository;
import com.prisma.database.DatabaseConnectionManager;
import com.prisma.exception.DatabaseException;
import org.hibernate.Session;
import java.util.List;
import java.util.Optional;

public class CoefficientRepositoryImpl extends GenericRepositoryImpl<Coefficient, Integer> implements CoefficientRepository {

    public CoefficientRepositoryImpl() {
        super(Coefficient.class);
    }

    @Override
    public List<Coefficient> findByClasseAndPeriode(Integer classeId, Integer periodeId) {
        try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
            List<Coefficient> list = session.createQuery("from Coefficient c join fetch c.matiere where c.classe.id = :classeId and c.periode.id = :periodeId order by c.matiere.ordreAffichage", Coefficient.class)
                    .setParameter("classeId", classeId)
                    .setParameter("periodeId", periodeId)
                    .getResultList();

            if (list.isEmpty()) {
                List<Coefficient> classCoefs = session.createQuery("from Coefficient c join fetch c.matiere where c.classe.id = :classeId order by c.matiere.ordreAffichage", Coefficient.class)
                        .setParameter("classeId", classeId)
                        .getResultList();
                java.util.Map<Integer, Coefficient> map = new java.util.LinkedHashMap<>();
                for (Coefficient c : classCoefs) {
                    map.putIfAbsent(c.getMatiere().getId(), c);
                }
                list = new java.util.ArrayList<>(map.values());
            }

            return list;
        } catch (Exception e) {
            throw new DatabaseException("Erreur lors de la recherche des coefficients pour la classe " + classeId + " et la période " + periodeId, e);
        }
    }

    @Override
    public Optional<Coefficient> findByClasseAndMatiereAndPeriode(Integer classeId, Integer matiereId, Integer periodeId) {
        try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
            return session.createQuery("from Coefficient c where c.classe.id = :classeId and c.matiere.id = :matiereId and c.periode.id = :periodeId", Coefficient.class)
                    .setParameter("classeId", classeId)
                    .setParameter("matiereId", matiereId)
                    .setParameter("periodeId", periodeId)
                    .uniqueResultOptional();
        } catch (Exception e) {
            throw new DatabaseException("Erreur lors de la recherche du coefficient", e);
        }
    }
}
