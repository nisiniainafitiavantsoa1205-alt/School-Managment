package com.prisma.repository;

import com.prisma.entity.Configuration;
import java.util.Optional;

public interface ConfigurationRepository extends GenericRepository<Configuration, Integer> {
    Optional<Configuration> findByCle(String cle);
    void saveOrUpdate(String cle, String valeur);
}
