package com.prisma.repository;

import com.prisma.entity.Periode;
import java.util.Optional;

public interface PeriodeRepository extends GenericRepository<Periode, Integer> {
    Optional<Periode> findActive();
}
