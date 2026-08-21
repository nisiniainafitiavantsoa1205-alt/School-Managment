package com.prisma.repository;

import com.prisma.entity.Matiere;
import java.util.List;

public interface MatiereRepository extends GenericRepository<Matiere, Integer> {
    List<Matiere> findAllActiveOrderByOrdre();
}
