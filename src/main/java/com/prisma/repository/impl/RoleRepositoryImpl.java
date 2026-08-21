package com.prisma.repository.impl;

import com.prisma.entity.Role;
import com.prisma.repository.RoleRepository;
import com.prisma.database.DatabaseConnectionManager;
import com.prisma.exception.DatabaseException;
import org.hibernate.Session;
import java.util.Optional;

public class RoleRepositoryImpl extends GenericRepositoryImpl<Role, Integer> implements RoleRepository {

    public RoleRepositoryImpl() {
        super(Role.class);
    }

    @Override
    public Optional<Role> findByNom(String nom) {
        try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
            return session.createQuery("from Role where nom = :nom", Role.class)
                    .setParameter("nom", nom)
                    .uniqueResultOptional();
        } catch (Exception e) {
            throw new DatabaseException("Erreur lors de la recherche du rôle par nom: " + nom, e);
        }
    }
}
