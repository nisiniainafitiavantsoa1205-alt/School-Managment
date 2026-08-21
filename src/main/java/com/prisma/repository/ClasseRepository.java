package com.prisma.repository;

import com.prisma.entity.Classe;
import java.util.List;
import java.util.Optional;

public interface ClasseRepository extends GenericRepository<Classe, Integer> {
    List<Classe> findByNiveau(String niveau);
    List<Classe> findByAnneeScolaire(String anneeScolaire);
    Optional<Classe> findByNomAndAnneeScolaire(String nom, String anneeScolaire);
}
