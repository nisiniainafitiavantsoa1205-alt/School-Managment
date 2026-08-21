package com.prisma.repository;

import com.prisma.entity.Coefficient;
import java.util.List;
import java.util.Optional;

public interface CoefficientRepository extends GenericRepository<Coefficient, Integer> {
    List<Coefficient> findByClasseAndPeriode(Integer classeId, Integer periodeId);
    Optional<Coefficient> findByClasseAndMatiereAndPeriode(Integer classeId, Integer matiereId, Integer periodeId);
}
