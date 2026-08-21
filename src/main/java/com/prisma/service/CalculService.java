package com.prisma.service;

import com.prisma.entity.Bulletin;
import java.util.List;

public interface CalculService {

    /**
     * Calcule, classe et génère/met à jour les bulletins scolaires pour tous les élèves 
     * d'une classe donnée et pour un trimestre donné.
     *
     * @param classeId ID de la classe
     * @param periodeId ID du trimestre
     * @return La liste des bulletins calculés et sauvegardés
     */
    List<Bulletin> calculerBulletinsClasse(Integer classeId, Integer periodeId);
}
