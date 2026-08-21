package com.prisma.service.impl;

import com.prisma.entity.Matiere;
import com.prisma.repository.MatiereRepository;
import com.prisma.repository.impl.MatiereRepositoryImpl;
import com.prisma.service.MatiereService;
import com.prisma.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Optional;

public class MatiereServiceImpl implements MatiereService {

    private static final Logger logger = LoggerFactory.getLogger(MatiereServiceImpl.class);
    private final MatiereRepository matiereRepository;

    public MatiereServiceImpl() {
        this.matiereRepository = new MatiereRepositoryImpl();
    }

    public MatiereServiceImpl(MatiereRepository matiereRepository) {
        this.matiereRepository = matiereRepository;
    }

    @Override
    public Matiere creer(Matiere matiere) {
        valider(matiere);
        logger.info("Création de la matière: {}", matiere.getNom());
        return matiereRepository.save(matiere);
    }

    @Override
    public Matiere modifier(Matiere matiere) {
        if (matiere.getId() == null) {
            throw new ValidationException("L'identifiant de la matière est requis pour la modification.");
        }
        valider(matiere);
        logger.info("Modification de la matière ID: {}", matiere.getId());
        return matiereRepository.update(matiere);
    }

    @Override
    public void supprimer(Integer id) {
        logger.info("Suppression de la matière ID: {}", id);
        matiereRepository.deleteById(id);
    }

    @Override
    public Optional<Matiere> trouverParId(Integer id) {
        return matiereRepository.findById(id);
    }

    @Override
    public List<Matiere> listerTout() {
        return matiereRepository.findAll();
    }

    @Override
    public List<Matiere> listerActives() {
        return matiereRepository.findAllActiveOrderByOrdre();
    }

    private void valider(Matiere matiere) {
        if (matiere.getNom() == null || matiere.getNom().trim().isEmpty()) {
            throw new ValidationException("Le nom de la matière est obligatoire.");
        }
    }
}
