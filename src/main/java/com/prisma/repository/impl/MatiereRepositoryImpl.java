package com.prisma.repository.impl;

import com.prisma.entity.Matiere;
import com.prisma.repository.MatiereRepository;
import com.prisma.database.DatabaseConnectionManager;
import com.prisma.exception.DatabaseException;
import org.hibernate.Session;
import java.util.List;

public class MatiereRepositoryImpl extends GenericRepositoryImpl<Matiere, Integer> implements MatiereRepository {

    public MatiereRepositoryImpl() {
        super(Matiere.class);
    }

    @Override
    public List<Matiere> findAllActiveOrderByOrdre() {
        try (Session session = DatabaseConnectionManager.getSessionFactory().openSession()) {
            return session.createQuery("from Matiere where active = true order by ordreAffichage, nom", Matiere.class)
                    .getResultList();
        } catch (Exception e) {
            throw new DatabaseException("Erreur lors de la récupération des matières actives ordonnées", e);
        }
    }
}
