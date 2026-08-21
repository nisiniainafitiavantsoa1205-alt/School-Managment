package com.prisma.repository;

import com.prisma.entity.Bulletin;
import java.util.List;
import java.util.Optional;

public interface BulletinRepository extends GenericRepository<Bulletin, Integer> {
    Optional<Bulletin> findByEleveAndPeriode(Integer eleveId, Integer periodeId);
    List<Bulletin> findByClasseAndPeriodeOrderByMoyenneDesc(Integer classeId, Integer periodeId);
}
