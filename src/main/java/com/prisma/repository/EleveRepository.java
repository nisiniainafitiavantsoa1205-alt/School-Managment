package com.prisma.repository;

import com.prisma.entity.Eleve;
import java.util.List;
import java.util.Optional;

public interface EleveRepository extends GenericRepository<Eleve, Integer> {
    Optional<Eleve> findByMatricule(String matricule);
    List<Eleve> findByClasse(Integer classeId);
    List<Eleve> search(String query, Integer classeId, String statut, int page, int pageSize);
    long countSearch(String query, Integer classeId, String statut);

    default List<Eleve> search(String query, Integer classeId, int page, int pageSize) {
        return search(query, classeId, null, page, pageSize);
    }
    default long countSearch(String query, Integer classeId) {
        return countSearch(query, classeId, null);
    }
}
