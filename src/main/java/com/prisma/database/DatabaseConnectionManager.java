package com.prisma.database;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.prisma.exception.DatabaseException;

public class DatabaseConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionManager.class);
    private static SessionFactory sessionFactory;

    private DatabaseConnectionManager() {}

    /**
     * Retourne la SessionFactory Hibernate unique (Singleton).
     *
     * @return SessionFactory Hibernate
     * @throws DatabaseException Si la connexion échoue
     */
    public static synchronized SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            try {
                logger.info("Initialisation de la SessionFactory Hibernate...");
                new java.io.File("./data").mkdirs();
                Configuration configuration = new Configuration().configure();
                
                java.io.File propFile = new java.io.File("database.properties");
                if (!propFile.exists()) {
                    // Créer un fichier de configuration modèle pour l'utilisateur
                    java.util.Properties defaultProps = new java.util.Properties();
                    defaultProps.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
                    defaultProps.setProperty("hibernate.connection.url", "jdbc:h2:./data/prisma_db;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE;DB_CLOSE_ON_EXIT=TRUE");
                    defaultProps.setProperty("hibernate.connection.username", "sa");
                    defaultProps.setProperty("hibernate.connection.password", "");
                    defaultProps.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
                    
                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(propFile)) {
                        defaultProps.store(fos, "Configuration de la base de donnees de PRISMA School\nModifiez ces valeurs pour correspondre a votre base locale.");
                        logger.info("Fichier database.properties cree avec les valeurs par defaut.");
                    } catch (Exception e) {
                        logger.warn("Impossible de creer le fichier database.properties par defaut.", e);
                    }
                } else {
                    logger.info("Chargement des configurations depuis database.properties...");
                    java.util.Properties props = new java.util.Properties();
                    try (java.io.FileInputStream fis = new java.io.FileInputStream(propFile)) {
                        props.load(fis);
                        configuration.addProperties(props);
                    } catch (Exception e) {
                        logger.error("Erreur lors de la lecture de database.properties", e);
                    }
                }

                sessionFactory = configuration.buildSessionFactory();
                logger.info("SessionFactory Hibernate initialisée avec succès.");

                // Initialiser la base de données avec les données par défaut si elle est vide
                seederBaseDeDonnees();
            } catch (Exception e) {
                logger.error("Échec de la création de la SessionFactory Hibernate.", e);
                throw new DatabaseException("Impossible de se connecter à la base de données locale.", e);
            }
        }
        return sessionFactory;
    }

    /**
     * Vérifie si la base est vide et exécute le script data.sql d'initialisation le cas échéant.
     */
    private static void seederBaseDeDonnees() {
        try (Session session = sessionFactory.openSession()) {
            // Vérifier s'il y a déjà des utilisateurs dans la base
            Long count = session.createQuery("select count(u) from Utilisateur u", Long.class).getSingleResult();
            if (count == 0) {
                logger.info("La table utilisateurs est vide. Initialisation de la base de données avec data.sql...");
                Transaction tx = null;
                try {
                    tx = session.beginTransaction();
                    
                    // Lire le fichier SQL de ressources
                    java.io.InputStream is = DatabaseConnectionManager.class.getResourceAsStream("/sql/data.sql");
                    if (is != null) {
                        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8))) {
                            StringBuilder sqlBuilder = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                String trimmed = line.trim();
                                if (trimmed.startsWith("--") || trimmed.isEmpty()) {
                                    continue;
                                }
                                sqlBuilder.append(line).append("\n");
                                if (trimmed.endsWith(";")) {
                                    String sql = sqlBuilder.toString().trim();
                                    if (sql.endsWith(";")) {
                                        sql = sql.substring(0, sql.length() - 1);
                                    }
                                    if (!sql.isEmpty()) {
                                        session.createNativeQuery(sql).executeUpdate();
                                    }
                                    sqlBuilder.setLength(0);
                                }
                            }
                        }
                    } else {
                        logger.error("Fichier /sql/data.sql introuvable dans le classpath.");
                    }
                    tx.commit();
                    logger.info("Initialisation des données de base réussie.");
                } catch (Exception e) {
                    if (tx != null) {
                        try { tx.rollback(); } catch (Exception ex) { /* Ignore */ }
                    }
                    logger.error("Erreur lors de l'exécution du script d'initialisation SQL.", e);
                }
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la vérification de l'état de la base pour le seeding.", e);
        }
    }

    /**
     * Ferme proprement la SessionFactory d'Hibernate à la fermeture de l'application.
     */
    public static synchronized void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            logger.info("Fermeture de la SessionFactory Hibernate...");
            sessionFactory.close();
            sessionFactory = null;
            logger.info("SessionFactory Hibernate fermée.");
        }
    }
}
