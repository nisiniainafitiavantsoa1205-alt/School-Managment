package com.prisma.service;

import com.prisma.entity.Matiere;
import java.util.List;
import java.util.Optional;

public interface MatiereService {
    Matiere creer(Matiere matiere);
    Matiere modifier(Matiere matiere);
    void supprimer(Integer id);
    Optional<Matiere> trouverParId(Integer id);
    List<Matiere> listerTout();
    List<Matiere> listerActives();
}
