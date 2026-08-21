package com.prisma.repository.impl;

import com.prisma.entity.Configuration;
import com.prisma.repository.ConfigurationRepository;
import com.prisma.database.DatabaseConnectionManager;
import com.prisma.exception.DatabaseException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import java.util.Optional;

public class ConfigurationRepositoryImpl extends GenericRepositoryImpl<Configuration, Integer> implements ConfigurationRepository {

    public ConfigurationRepositoryImpl() {
        super(Configuration.class);
    }

    @Override
    public Optional<Configuration> findByCle(String cle) {
        try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
            return session.createQuery("from Configuration where cle = :cle", Configuration.class)
                    .setParameter("cle", cle)
                    .uniqueResultOptional();
        } catch (Exception e) {
            throw new DatabaseException("Erreur lors de la recherche de la configuration par clé: " + cle, e);
        }
    }

    @Override
    public void saveOrUpdate(String cle, String valeur) {
        Transaction tx = null;
        try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Configuration config = session.createQuery("from Configuration where cle = :cle", Configuration.class)
                    .setParameter("cle", cle)
                    .uniqueResult();

            if (config == null) {
                config = new Configuration();
                config.setCle(cle);
                config.setValeur(valeur);
                session.persist(config);
            } else {
                config.setValeur(valeur);
                session.merge(config);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) {
                try { tx.rollback(); } catch (Exception ex) { /* Ignore */ }
            }
            throw new DatabaseException("Erreur lors de la sauvegarde de la configuration: " + cle, e);
        }
    }
}
