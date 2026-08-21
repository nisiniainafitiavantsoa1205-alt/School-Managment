package com.prisma.service;

import com.prisma.entity.Eleve;
import java.util.List;
import java.util.Optional;

public interface EleveService {
    Eleve creer(Eleve eleve);
    Eleve modifier(Eleve eleve);
    void supprimer(Integer id);
    Optional<Eleve> trouverParId(Integer id);
    Optional<Eleve> trouverParMatricule(String matricule);
    List<Eleve> trouverParClasse(Integer classeId);
    List<Eleve> rechercher(String query, Integer classeId, int page, int pageSize);
    List<Eleve> rechercher(String query, Integer classeId, String statut, int page, int pageSize);
    long compterRecherche(String query, Integer classeId);
    long compterRecherche(String query, Integer classeId, String statut);
    String genererMatricule(String anneeScolaire);
}
