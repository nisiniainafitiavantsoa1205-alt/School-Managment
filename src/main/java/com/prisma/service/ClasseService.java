package com.prisma.service;

import com.prisma.entity.Classe;
import java.util.List;
import java.util.Optional;

public interface ClasseService {
    Classe creer(Classe classe);
    Classe modifier(Classe classe);
    void supprimer(Integer id);
    Optional<Classe> trouverParId(Integer id);
    List<Classe> trouverParAnneeScolaire(String anneeScolaire);
    List<Classe> trouverParNiveau(String niveau);
    List<Classe> listerTout();
}
