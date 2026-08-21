package com.prisma.service;

import com.prisma.entity.Utilisateur;
import java.util.List;
import java.util.Optional;

public interface UtilisateurService {
    Utilisateur creer(Utilisateur utilisateur, String motDePasse);
    Utilisateur modifier(Utilisateur utilisateur);
    void changerMotDePasse(Integer id, String nouveauMotDePasse);
    void supprimer(Integer id);
    Optional<Utilisateur> trouverParId(Integer id);
    Optional<Utilisateur> trouverParUsername(String username);
    List<Utilisateur> listerTout();
}
