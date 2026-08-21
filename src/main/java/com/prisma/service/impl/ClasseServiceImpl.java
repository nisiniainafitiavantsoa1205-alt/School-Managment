package com.prisma.service.impl;

import com.prisma.entity.Classe;
import com.prisma.repository.ClasseRepository;
import com.prisma.repository.impl.ClasseRepositoryImpl;
import com.prisma.service.ClasseService;
import com.prisma.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Optional;

public class ClasseServiceImpl implements ClasseService {

    private static final Logger logger = LoggerFactory.getLogger(ClasseServiceImpl.class);
    private final ClasseRepository classeRepository;

    public ClasseServiceImpl() {
        this.classeRepository = new ClasseRepositoryImpl();
    }

    public ClasseServiceImpl(ClasseRepository classeRepository) {
        this.classeRepository = classeRepository;
    }

    @Override
    public Classe creer(Classe classe) {
        valider(classe);
        logger.info("Création de la classe: {}", classe.getNom());
        return classeRepository.save(classe);
    }

    @Override
    public Classe modifier(Classe classe) {
        if (classe.getId() == null) {
            throw new ValidationException("L'identifiant de la classe est requis pour la modification.");
        }
        valider(classe);
        logger.info("Modification de la classe ID: {}", classe.getId());
        return classeRepository.update(classe);
    }

    @Override
    public void supprimer(Integer id) {
        logger.info("Suppression de la classe ID: {}", id);
        classeRepository.deleteById(id);
    }

    @Override
    public Optional<Classe> trouverParId(Integer id) {
        return classeRepository.findById(id);
    }

    @Override
    public List<Classe> trouverParAnneeScolaire(String anneeScolaire) {
        return classeRepository.findByAnneeScolaire(anneeScolaire);
    }

    @Override
    public List<Classe> trouverParNiveau(String niveau) {
        return classeRepository.findByNiveau(niveau);
    }

    @Override
    public List<Classe> listerTout() {
        return classeRepository.findAll();
    }

    private void valider(Classe classe) {
        if (classe.getNom() == null || classe.getNom().trim().isEmpty()) {
            throw new ValidationException("Le nom de la classe est obligatoire.");
        }
        if (classe.getNiveau() == null || classe.getNiveau().trim().isEmpty()) {
            throw new ValidationException("Le niveau de la classe est obligatoire.");
        }
        if (classe.getAnneeScolaire() == null || classe.getAnneeScolaire().trim().isEmpty()) {
            throw new ValidationException("L'année scolaire de la classe est obligatoire.");
        }
    }
}
