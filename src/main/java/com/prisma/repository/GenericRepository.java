package com.prisma.repository;

import java.util.List;
import java.util.Optional;

/**
 * Interface générique définissant les opérations CRUD de base.
 *
 * @param <T>  Type de l'entité
 * @param <ID> Type de l'identifiant (clé primaire)
 */
public interface GenericRepository<T, ID> {

    T save(T entity);

    T update(T entity);

    T saveOrUpdate(T entity);

    void delete(T entity);

    void deleteById(ID id);

    Optional<T> findById(ID id);

    List<T> findAll();
}
