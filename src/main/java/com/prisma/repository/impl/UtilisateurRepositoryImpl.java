package com.prisma.repository.impl;

import com.prisma.entity.Utilisateur;
import com.prisma.repository.UtilisateurRepository;
import com.prisma.database.DatabaseConnectionManager;
import com.prisma.exception.DatabaseException;
import org.hibernate.Session;
import java.util.Optional;

public class UtilisateurRepositoryImpl extends GenericRepositoryImpl<Utilisateur, Integer> implements UtilisateurRepository {

    public UtilisateurRepositoryImpl() {
        super(Utilisateur.class);
    }

    @Override
    public Optional<Utilisateur> findByUsername(String username) {
        try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
            return session.createQuery("from Utilisateur u join fetch u.role where u.username = :username", Utilisateur.class)
                    .setParameter("username", username)
                    .uniqueResultOptional();
        } catch (Exception e) {
            throw new DatabaseException("Erreur lors de la recherche de l'utilisateur par nom d'utilisateur: " + username, e);
        }
    }
}
