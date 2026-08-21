package com.prisma.repository.impl;

import com.prisma.entity.Note;
import com.prisma.repository.NoteRepository;
import com.prisma.database.DatabaseConnectionManager;
import com.prisma.exception.DatabaseException;
import org.hibernate.Session;
import java.util.List;
import java.util.Optional;

public class NoteRepositoryImpl extends GenericRepositoryImpl<Note, Integer> implements NoteRepository {

    public NoteRepositoryImpl() {
        super(Note.class);
    }

    @Override
    public List<Note> findByEleveAndPeriode(Integer eleveId, Integer periodeId) {
        try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
            return session.createQuery("from Note n join fetch n.matiere where n.eleve.id = :eleveId and n.periode.id = :periodeId order by n.matiere.ordreAffichage", Note.class)
                    .setParameter("eleveId", eleveId)
                    .setParameter("periodeId", periodeId)
                    .getResultList();
        } catch (Exception e) {
            throw new DatabaseException("Erreur lors de la recherche des notes de l'élève " + eleveId + " pour la période " + periodeId, e);
        }
    }

    @Override
    public List<Note> findByClasseAndMatiereAndPeriode(Integer classeId, Integer matiereId, Integer periodeId) {
        try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
            // Jointure sur l'élève de la classe pour obtenir la note (si elle existe)
            return session.createQuery(
                    "select n from Note n join fetch n.eleve e where e.classe.id = :classeId and n.matiere.id = :matiereId and n.periode.id = :periodeId order by e.nom, e.prenoms", 
                    Note.class)
                    .setParameter("classeId", classeId)
                    .setParameter("matiereId", matiereId)
                    .setParameter("periodeId", periodeId)
                    .getResultList();
        } catch (Exception e) {
            throw new DatabaseException("Erreur lors de la recherche des notes pour la classe " + classeId + ", matière " + matiereId, e);
        }
    }

    @Override
    public Optional<Note> findByEleveAndMatiereAndPeriode(Integer eleveId, Integer matiereId, Integer periodeId) {
        try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
            return session.createQuery(
                    "from Note n where n.eleve.id = :eleveId and n.matiere.id = :matiereId and n.periode.id = :periodeId", 
                    Note.class)
                    .setParameter("eleveId", eleveId)
                    .setParameter("matiereId", matiereId)
                    .setParameter("periodeId", periodeId)
                    .uniqueResultOptional();
        } catch (Exception e) {
            throw new DatabaseException("Erreur lors de la recherche de la note individuelle", e);
        }
    }
}
