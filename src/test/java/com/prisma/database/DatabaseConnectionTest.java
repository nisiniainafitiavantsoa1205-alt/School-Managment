package com.prisma.database;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.prisma.entity.Utilisateur;
import com.prisma.entity.Role;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseConnectionTest {

    private static SessionFactory sessionFactory;

    @BeforeAll
    public static void setUp() {
        try {
            // Initialisation de la session factory
            sessionFactory = DatabaseConnectionManager.getSessionFactory();
        } catch (Exception e) {
            org.junit.jupiter.api.Assumptions.abort("Base de données locale hors ligne - test d'intégration ignoré.");
        }
    }

    @AfterAll
    public static void tearDown() {
        // Fermeture propre
        DatabaseConnectionManager.shutdown();
    }

    @Test
    public void testDatabaseConnection() {
        assertNotNull(sessionFactory, "La SessionFactory ne doit pas être nulle");
        
        try (Session session = sessionFactory.openSession()) {
            assertNotNull(session, "La session Hibernate ne doit pas être nulle");
            assertTrue(session.isOpen(), "La session doit être ouverte");
        }
    }

    @Test
    public void testRetrieveAdminUser() {
        try (Session session = sessionFactory.openSession()) {
            // Recherche de l'utilisateur admin inséré par data.sql
            Utilisateur admin = session.createQuery("from Utilisateur where username = :username", Utilisateur.class)
                    .setParameter("username", "admin")
                    .uniqueResult();
            
            assertNotNull(admin, "L'utilisateur 'admin' doit exister en base");
            assertEquals("admin@prisma.com", admin.getEmail(), "L'email de l'admin doit correspondre");
            
            // Vérification de la relation avec le rôle
            Role role = admin.getRole();
            assertNotNull(role, "L'admin doit avoir un rôle");
            assertEquals("ADMINISTRATEUR", role.getNom(), "Le rôle de l'admin doit être ADMINISTRATEUR");
        }
    }
}
