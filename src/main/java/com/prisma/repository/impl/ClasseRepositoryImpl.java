package com.prisma.repository.impl;

import com.prisma.entity.Classe;
import com.prisma.repository.ClasseRepository;
import com.prisma.database.DatabaseConnectionManager;
import com.prisma.exception.DatabaseException;
import org.hibernate.Session;
import java.util.List;
import java.util.Optional;

public class ClasseRepositoryImpl extends GenericRepositoryImpl<Classe, Integer> implements ClasseRepository {

    public ClasseRepositoryImpl() {
        super(Classe.class);
    }

    @Override
    public List<Classe> findByNiveau(String niveau) {
        try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
            return session.createQuery("from Classe where niveau = :niveau", Classe.class)
                    .setParameter("niveau", niveau)
                    .getResultList();
        } catch (Exception e) {
            throw new DatabaseException("Erreur lors de la recherche des classes par niveau: " + niveau, e);
        }
    }

    @Override
    public List<Classe> findByAnneeScolaire(String anneeScolaire) {
        try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
            return session.createQuery("from Classe where anneeScolaire = :anneeScolaire", Classe.class)
                    .setParameter("anneeScolaire", anneeScolaire)
                    .getResultList();
        } catch (Exception e) {
            throw new DatabaseException("Erreur lors de la recherche des classes par année scolaire: " + anneeScolaire, e);
        }
    }

    @Override
    public Optional<Classe> findByNomAndAnneeScolaire(String nom, String anneeScolaire) {
        try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
            return session.createQuery("from Classe where nom = :nom and anneeScolaire = :anneeScolaire", Classe.class)
                    .setParameter("nom", nom)
                    .setParameter("anneeScolaire", anneeScolaire)
                    .uniqueResultOptional();
        } catch (Exception e) {
            throw new DatabaseException("Erreur lors de la recherche de la classe: " + nom + " (" + anneeScolaire + ")", e);
        }
    }
}
