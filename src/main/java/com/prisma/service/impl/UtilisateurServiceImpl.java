package com.prisma.service.impl;

import com.prisma.entity.Utilisateur;
import com.prisma.repository.UtilisateurRepository;
import com.prisma.repository.impl.UtilisateurRepositoryImpl;
import com.prisma.security.PasswordHasher;
import com.prisma.service.UtilisateurService;
import com.prisma.exception.ValidationException;
import com.prisma.exception.PrismaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Optional;

public class UtilisateurServiceImpl implements UtilisateurService {

    private static final Logger logger = LoggerFactory.getLogger(UtilisateurServiceImpl.class);
    private final UtilisateurRepository utilisateurRepository;

    public UtilisateurServiceImpl() {
        this.utilisateurRepository = new UtilisateurRepositoryImpl();
    }

    public UtilisateurServiceImpl(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    @Override
    public Utilisateur creer(Utilisateur utilisateur, String motDePasse) {
        valider(utilisateur);
        if (motDePasse == null || motDePasse.length() < 6) {
            throw new ValidationException("Le mot de passe doit contenir au moins 6 caractères.");
        }
        // Vérification de l'unicité du nom d'utilisateur
        if (utilisateurRepository.findByUsername(utilisateur.getUsername()).isPresent()) {
            throw new PrismaException("Le nom d'utilisateur '" + utilisateur.getUsername() + "' est déjà utilisé.");
        }
        utilisateur.setPasswordHash(PasswordHasher.hacher(motDePasse));
        logger.info("Création de l'utilisateur: {}", utilisateur.getUsername());
        return utilisateurRepository.save(utilisateur);
    }

    @Override
    public Utilisateur modifier(Utilisateur utilisateur) {
        if (utilisateur.getId() == null) {
            throw new ValidationException("L'identifiant de l'utilisateur est requis pour la modification.");
        }
        valider(utilisateur);
        logger.info("Modification de l'utilisateur ID: {}", utilisateur.getId());
        return utilisateurRepository.update(utilisateur);
    }

    @Override
    public void changerMotDePasse(Integer id, String nouveauMotDePasse) {
        if (nouveauMotDePasse == null || nouveauMotDePasse.length() < 6) {
            throw new ValidationException("Le nouveau mot de passe doit contenir au moins 6 caractères.");
        }
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new PrismaException("Utilisateur introuvable avec l'ID: " + id));
        utilisateur.setPasswordHash(PasswordHasher.hacher(nouveauMotDePasse));
        utilisateurRepository.update(utilisateur);
        logger.info("Mot de passe modifié pour l'utilisateur ID: {}", id);
    }

    @Override
    public void supprimer(Integer id) {
        logger.info("Suppression de l'utilisateur ID: {}", id);
        utilisateurRepository.deleteById(id);
    }

    @Override
    public Optional<Utilisateur> trouverParId(Integer id) {
        return utilisateurRepository.findById(id);
    }

    @Override
    public Optional<Utilisateur> trouverParUsername(String username) {
        return utilisateurRepository.findByUsername(username);
    }

    @Override
    public List<Utilisateur> listerTout() {
        return utilisateurRepository.findAll();
    }

    private void valider(Utilisateur utilisateur) {
        if (utilisateur.getUsername() == null || utilisateur.getUsername().trim().isEmpty()) {
            throw new ValidationException("Le nom d'utilisateur est obligatoire.");
        }
        if (utilisateur.getRole() == null) {
            throw new ValidationException("Le rôle de l'utilisateur est obligatoire.");
        }
    }
}
