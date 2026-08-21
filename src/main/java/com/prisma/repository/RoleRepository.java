package com.prisma.repository;

import com.prisma.entity.Role;
import java.util.Optional;

public interface RoleRepository extends GenericRepository<Role, Integer> {
    Optional<Role> findByNom(String nom);
}
