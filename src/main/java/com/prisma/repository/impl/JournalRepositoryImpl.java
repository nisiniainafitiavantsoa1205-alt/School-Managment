package com.prisma.repository.impl;

import com.prisma.entity.Journal;
import com.prisma.repository.JournalRepository;
import com.prisma.database.DatabaseConnectionManager;
import com.prisma.exception.DatabaseException;
import org.hibernate.Session;
import java.util.List;

public class JournalRepositoryImpl extends GenericRepositoryImpl<Journal, Integer> implements JournalRepository {

    public JournalRepositoryImpl() {
        super(Journal.class);
    }

    @Override
    public List<Journal> findRecentLogs(int limit) {
        try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
            return session.createQuery("from Journal j left join fetch j.utilisateur order by j.dateAction desc", Journal.class)
                    .setMaxResults(limit)
                    .getResultList();
        } catch (Exception e) {
            throw new DatabaseException("Erreur lors de la recherche des logs récents", e);
        }
    }
}
