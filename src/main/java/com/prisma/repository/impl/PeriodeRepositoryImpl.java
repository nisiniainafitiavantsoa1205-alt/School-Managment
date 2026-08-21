package com.prisma.repository.impl;

import com.prisma.entity.Periode;
import com.prisma.repository.PeriodeRepository;
import com.prisma.database.DatabaseConnectionManager;
import com.prisma.exception.DatabaseException;
import org.hibernate.Session;
import java.util.Optional;

public class PeriodeRepositoryImpl extends GenericRepositoryImpl<Periode, Integer> implements PeriodeRepository {

    public PeriodeRepositoryImpl() {
        super(Periode.class);
    }

    @Override
    public Optional<Periode> findActive() {
        try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
            return session.createQuery("from Periode where active = true and closed = false", Periode.class)
                    .setMaxResults(1)
                    .uniqueResultOptional();
        } catch (Exception e) {
            throw new DatabaseException("Erreur lors de la recherche de la période active", e);
        }
    }
}
