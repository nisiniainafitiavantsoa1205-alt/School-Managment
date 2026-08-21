package com.prisma.repository.impl;

import com.prisma.repository.GenericRepository;
import com.prisma.database.DatabaseConnectionManager;
import com.prisma.exception.DatabaseException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation générique abstraite des opérations CRUD avec Hibernate.
 */
public abstract class GenericRepositoryImpl<T, ID> implements GenericRepository<T, ID> {

    protected final Class<T> entityClass;

    protected GenericRepositoryImpl(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    @Override
    public T save(T entity) {
        Transaction tx = null;
        try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.persist(entity);
            tx.commit();
            return entity;
        } catch (Exception e) {
            if (tx != null) {
                try { tx.rollback(); } catch (Exception ex) { /* Ignore */ }
            }
            throw new DatabaseException("Erreur lors de la persistance de l'entité: " + entityClass.getSimpleName(), e);
        }
    }

    @Override
    public T update(T entity) {
        Transaction tx = null;
        try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            T merged = session.merge(entity);
            tx.commit();
            return merged;
        } catch (Exception e) {
            if (tx != null) {
                try { tx.rollback(); } catch (Exception ex) { /* Ignore */ }
            }
            throw new DatabaseException("Erreur lors de la mise à jour de l'entité: " + entityClass.getSimpleName(), e);
        }
    }

    @Override
    public T saveOrUpdate(T entity) {
        Transaction tx = null;
        try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Object idVal = null;
            try {
                java.lang.reflect.Method getIdMethod = entity.getClass().getMethod("getId");
                idVal = getIdMethod.invoke(entity);
            } catch (Exception e) {
                try {
                    java.lang.reflect.Field idField = entity.getClass().getDeclaredField("id");
                    idField.setAccessible(true);
                    idVal = idField.get(entity);
                } catch (Exception ex) {
                    // Ignore
                }
            }

            T result;
            if (idVal == null || (idVal instanceof Number && ((Number) idVal).longValue() <= 0)) {
                session.persist(entity);
                result = entity;
            } else {
                result = session.merge(entity);
            }
            tx.commit();
            return result;
        } catch (Exception e) {
            if (tx != null) {
                try { tx.rollback(); } catch (Exception ex) { /* Ignore */ }
            }
            throw new DatabaseException("Erreur lors du saveOrUpdate de l'entité: " + entityClass.getSimpleName(), e);
        }
    }

    @Override
    public void delete(T entity) {
        Transaction tx = null;
        try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.remove(session.contains(entity) ? entity : session.merge(entity));
            tx.commit();
        } catch (Exception e) {
            if (tx != null) {
                try { tx.rollback(); } catch (Exception ex) { /* Ignore */ }
            }
            throw new DatabaseException("Erreur lors de la suppression de l'entité: " + entityClass.getSimpleName(), e);
        }
    }

    @Override
    public void deleteById(ID id) {
        Transaction tx = null;
        try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            T entity = session.find(entityClass, id);
            if (entity != null) {
                session.remove(entity);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) {
                try { tx.rollback(); } catch (Exception ex) { /* Ignore */ }
            }
            throw new DatabaseException("Erreur lors de la suppression par ID de l'entité: " + entityClass.getSimpleName(), e);
        }
    }

    @Override
    public Optional<T> findById(ID id) {
        try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
            T entity = session.find(entityClass, id);
            return Optional.ofNullable(entity);
        } catch (Exception e) {
            throw new DatabaseException("Erreur lors de la recherche par ID de l'entité: " + entityClass.getSimpleName(), e);
        }
    }

    @Override
    public List<T> findAll() {
        try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
            return session.createQuery("from " + entityClass.getSimpleName(), entityClass).getResultList();
        } catch (Exception e) {
            throw new DatabaseException("Erreur lors du listing des entités: " + entityClass.getSimpleName(), e);
        }
    }
}
