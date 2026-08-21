package com.prisma.repository.impl;

import com.prisma.entity.Bulletin;
import com.prisma.repository.BulletinRepository;
import com.prisma.database.DatabaseConnectionManager;
import com.prisma.exception.DatabaseException;
import org.hibernate.Session;
import java.util.List;
import java.util.Optional;

public class BulletinRepositoryImpl extends GenericRepositoryImpl<Bulletin, Integer> implements BulletinRepository {

    public BulletinRepositoryImpl() {
        super(Bulletin.class);
    }

    @Override
    public Optional<Bulletin> findByEleveAndPeriode(Integer eleveId, Integer periodeId) {
        try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
            return session.createQuery("from Bulletin b join fetch b.eleve where b.eleve.id = :eleveId and b.periode.id = :periodeId", Bulletin.class)
                    .setParameter("eleveId", eleveId)
                    .setParameter("periodeId", periodeId)
                    .uniqueResultOptional();
        } catch (Exception e) {
            throw new DatabaseException("Erreur lors de la recherche du bulletin de l'élève " + eleveId + " pour la période " + periodeId, e);
        }
    }

    @Override
    public List<Bulletin> findByClasseAndPeriodeOrderByMoyenneDesc(Integer classeId, Integer periodeId) {
        try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
            return session.createQuery(
                    "select b from Bulletin b join fetch b.eleve e where e.classe.id = :classeId and b.periode.id = :periodeId order by b.moyenneGenerale desc", 
                    Bulletin.class)
                    .setParameter("classeId", classeId)
                    .setParameter("periodeId", periodeId)
                    .getResultList();
        } catch (Exception e) {
            throw new DatabaseException("Erreur lors de la recherche des bulletins pour la classe " + classeId + " et la période " + periodeId, e);
        }
    }
}
