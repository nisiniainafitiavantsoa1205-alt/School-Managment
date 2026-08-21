package com.prisma.repository.impl;

import com.prisma.entity.Eleve;
import com.prisma.repository.EleveRepository;
import com.prisma.database.DatabaseConnectionManager;
import com.prisma.exception.DatabaseException;
import org.hibernate.Session;
import java.util.List;
import java.util.Optional;

public class EleveRepositoryImpl extends GenericRepositoryImpl<Eleve, Integer> implements EleveRepository {

    public EleveRepositoryImpl() {
        super(Eleve.class);
    }

    @Override
    public Optional<Eleve> findByMatricule(String matricule) {
        try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
            return session.createQuery("from Eleve e left join fetch e.classe where e.matricule = :matricule", Eleve.class)
                    .setParameter("matricule", matricule)
                    .uniqueResultOptional();
        } catch (Exception e) {
            throw new DatabaseException("Erreur lors de la recherche de l'élève par matricule: " + matricule, e);
        }
    }

    @Override
    public List<Eleve> findByClasse(Integer classeId) {
        try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
            return session.createQuery("from Eleve e where e.classe.id = :classeId order by e.nom, e.prenoms", Eleve.class)
                    .setParameter("classeId", classeId)
                    .getResultList();
        } catch (Exception e) {
            throw new DatabaseException("Erreur lors de la recherche des élèves par classe ID: " + classeId, e);
        }
    }

    @Override
    public List<Eleve> search(String query, Integer classeId, String statut, int page, int pageSize) {
        try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
            StringBuilder hql = new StringBuilder("select e from Eleve e left join fetch e.classe where 1=1");
            if (classeId != null) {
                hql.append(" and e.classe.id = :classeId");
            }
            if (statut != null && !statut.trim().isEmpty()) {
                if ("ACTIF".equalsIgnoreCase(statut)) {
                    hql.append(" and (e.statut is null or e.statut = 'ACTIF')");
                } else {
                    hql.append(" and e.statut = :statut");
                }
            }
            if (query != null && !query.trim().isEmpty()) {
                hql.append(" and (lower(e.nom) like :query or lower(e.prenoms) like :query or lower(e.matricule) like :query)");
            }
            hql.append(" order by e.nom, e.prenoms");

            var q = session.createQuery(hql.toString(), Eleve.class);
            if (classeId != null) {
                q.setParameter("classeId", classeId);
            }
            if (statut != null && !statut.trim().isEmpty() && !"ACTIF".equalsIgnoreCase(statut)) {
                q.setParameter("statut", statut);
            }
            if (query != null && !query.trim().isEmpty()) {
                q.setParameter("query", "%" + query.trim().toLowerCase() + "%");
            }

            q.setFirstResult((page - 1) * pageSize);
            q.setMaxResults(pageSize);
            return q.getResultList();
        } catch (Exception e) {
            throw new DatabaseException("Erreur lors de la recherche paginée des élèves", e);
        }
    }

    @Override
    public long countSearch(String query, Integer classeId, String statut) {
        try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
            StringBuilder hql = new StringBuilder("select count(e) from Eleve e where 1=1");
            if (classeId != null) {
                hql.append(" and e.classe.id = :classeId");
            }
            if (statut != null && !statut.trim().isEmpty()) {
                if ("ACTIF".equalsIgnoreCase(statut)) {
                    hql.append(" and (e.statut is null or e.statut = 'ACTIF')");
                } else {
                    hql.append(" and e.statut = :statut");
                }
            }
            if (query != null && !query.trim().isEmpty()) {
                hql.append(" and (lower(e.nom) like :query or lower(e.prenoms) like :query or lower(e.matricule) like :query)");
            }

            var q = session.createQuery(hql.toString(), Long.class);
            if (classeId != null) {
                q.setParameter("classeId", classeId);
            }
            if (statut != null && !statut.trim().isEmpty() && !"ACTIF".equalsIgnoreCase(statut)) {
                q.setParameter("statut", statut);
            }
            if (query != null && !query.trim().isEmpty()) {
                q.setParameter("query", "%" + query.trim().toLowerCase() + "%");
            }

            Long count = q.uniqueResult();
            return count != null ? count : 0L;
        } catch (Exception e) {
            throw new DatabaseException("Erreur lors du comptage des élèves recherchés", e);
        }
    }
}
