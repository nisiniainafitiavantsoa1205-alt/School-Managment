package com.prisma.repository;

import com.prisma.entity.Utilisateur;
import java.util.Optional;

public interface UtilisateurRepository extends GenericRepository<Utilisateur, Integer> {
    Optional<Utilisateur> findByUsername(String username);
}
