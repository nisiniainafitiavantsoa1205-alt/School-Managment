package com.prisma.service.impl;

import com.prisma.entity.Eleve;
import com.prisma.repository.EleveRepository;
import com.prisma.repository.impl.EleveRepositoryImpl;
import com.prisma.service.EleveService;
import com.prisma.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Optional;

public class EleveServiceImpl implements EleveService {

    private static final Logger logger = LoggerFactory.getLogger(EleveServiceImpl.class);
    private final EleveRepository eleveRepository;

    public EleveServiceImpl() {
        this.eleveRepository = new EleveRepositoryImpl();
    }

    // Constructeur pour injection de dépendances (tests Mockito)
    public EleveServiceImpl(EleveRepository eleveRepository) {
        this.eleveRepository = eleveRepository;
    }

    @Override
    public Eleve creer(Eleve eleve) {
        valider(eleve);
        logger.info("Création de l'élève: {} {}", eleve.getNom(), eleve.getPrenoms());
        return eleveRepository.save(eleve);
    }

    @Override
    public Eleve modifier(Eleve eleve) {
        if (eleve.getId() == null) {
            throw new ValidationException("L'identifiant de l'élève est requis pour la modification.");
        }
        valider(eleve);
        logger.info("Modification de l'élève ID: {}", eleve.getId());
        return eleveRepository.update(eleve);
    }

    @Override
    public void supprimer(Integer id) {
        logger.info("Suppression de l'élève ID: {}", id);
        eleveRepository.deleteById(id);
    }

    @Override
    public Optional<Eleve> trouverParId(Integer id) {
        return eleveRepository.findById(id);
    }

    @Override
    public Optional<Eleve> trouverParMatricule(String matricule) {
        return eleveRepository.findByMatricule(matricule);
    }

    @Override
    public List<Eleve> trouverParClasse(Integer classeId) {
        return eleveRepository.findByClasse(classeId);
    }

    @Override
    public List<Eleve> rechercher(String query, Integer classeId, int page, int pageSize) {
        return eleveRepository.search(query, classeId, null, page, pageSize);
    }

    @Override
    public List<Eleve> rechercher(String query, Integer classeId, String statut, int page, int pageSize) {
        return eleveRepository.search(query, classeId, statut, page, pageSize);
    }

    @Override
    public long compterRecherche(String query, Integer classeId) {
        return eleveRepository.countSearch(query, classeId, null);
    }

    @Override
    public long compterRecherche(String query, Integer classeId, String statut) {
        return eleveRepository.countSearch(query, classeId, statut);
    }

    @Override
    public String genererMatricule(String anneeScolaire) {
        // Format : PRISMA-2025-0042
        long count = eleveRepository.countSearch(null, null) + 1;
        String annee = anneeScolaire.split("-")[0];
        return String.format("PRISMA-%s-%04d", annee, count);
    }

    private void valider(Eleve eleve) {
        if (eleve.getNom() == null || eleve.getNom().trim().isEmpty()) {
            throw new ValidationException("Le nom de l'élève est obligatoire.");
        }
        if (eleve.getDateNaissance() == null) {
            throw new ValidationException("La date de naissance de l'élève est obligatoire.");
        }
        if (eleve.getNumeroAppel() == null || eleve.getNumeroAppel().trim().isEmpty()) {
            throw new ValidationException("Le numéro d'appel de l'élève est obligatoire.");
        }
    }
}
