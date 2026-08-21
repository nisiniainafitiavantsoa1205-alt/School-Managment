-- CRÉATION DE LA BASE DE DONNÉES PRISMA

-- Drop tables in order of dependencies if they exist
DROP TABLE IF EXISTS sauvegardes CASCADE;
DROP TABLE IF EXISTS configurations CASCADE;
DROP TABLE IF EXISTS journaux CASCADE;
DROP TABLE IF EXISTS appreciations_suggestions CASCADE;
DROP TABLE IF EXISTS mentions CASCADE;
DROP TABLE IF EXISTS bulletins CASCADE;
DROP TABLE IF EXISTS notes CASCADE;
DROP TABLE IF EXISTS coefficients CASCADE;
DROP TABLE IF EXISTS periodes CASCADE;
DROP TABLE IF EXISTS matieres CASCADE;
DROP TABLE IF EXISTS eleves CASCADE;
DROP TABLE IF EXISTS classes CASCADE;
DROP TABLE IF EXISTS utilisateurs CASCADE;
DROP TABLE IF EXISTS roles CASCADE;

-- 1. Table des Rôles
CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255)
);

-- 2. Table des Utilisateurs
CREATE TABLE utilisateurs (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    active BOOLEAN DEFAULT TRUE,
    role_id INT NOT NULL REFERENCES roles(id)
);

-- 3. Table des Classes
CREATE TABLE classes (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50) NOT NULL,
    niveau VARCHAR(50) NOT NULL,
    annee_scolaire VARCHAR(20) NOT NULL,
    professeur_principal_id INT REFERENCES utilisateurs(id) ON DELETE SET NULL
);

-- 4. Table des Élèves
CREATE TABLE eleves (
    id SERIAL PRIMARY KEY,
    matricule VARCHAR(50) UNIQUE NOT NULL,
    numero_appel VARCHAR(10) NOT NULL,
    nom VARCHAR(100) NOT NULL,
    prenoms VARCHAR(150),
    date_naissance DATE NOT NULL,
    photo_data BYTEA,
    adresse VARCHAR(255),
    nom_parent VARCHAR(150),
    telephone_parent VARCHAR(50),
    nom_parent2 VARCHAR(150),
    telephone_parent2 VARCHAR(50),
    tuteur VARCHAR(255),
    classe_id INT REFERENCES classes(id) ON DELETE SET NULL
);

-- 5. Table des Matières
CREATE TABLE matieres (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    code VARCHAR(20) UNIQUE,
    ordre_affichage INT DEFAULT 0,
    active BOOLEAN DEFAULT TRUE,
    couleur_hex VARCHAR(7) DEFAULT '#002F6C'
);

-- 6. Table des Périodes (Trimestres)
CREATE TABLE periodes (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50) NOT NULL,
    annee_scolaire VARCHAR(20) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    closed BOOLEAN DEFAULT FALSE
);

-- 7. Table de Liaison pour les Coefficients (Classe <-> Matière <-> Période)
CREATE TABLE coefficients (
    id SERIAL PRIMARY KEY,
    classe_id INT NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    matiere_id INT NOT NULL REFERENCES matieres(id) ON DELETE CASCADE,
    periode_id INT NOT NULL REFERENCES periodes(id) ON DELETE CASCADE,
    valeur DOUBLE PRECISION NOT NULL,
    CONSTRAINT unique_coeff UNIQUE (classe_id, matiere_id, periode_id)
);

-- 8. Table des Notes
CREATE TABLE notes (
    id SERIAL PRIMARY KEY,
    eleve_id INT NOT NULL REFERENCES eleves(id) ON DELETE CASCADE,
    matiere_id INT NOT NULL REFERENCES matieres(id) ON DELETE CASCADE,
    periode_id INT NOT NULL REFERENCES periodes(id) ON DELETE CASCADE,
    valeur DOUBLE PRECISION,
    absent BOOLEAN DEFAULT FALSE,
    appreciation VARCHAR(255),
    saisie_par_id INT REFERENCES utilisateurs(id) ON DELETE SET NULL,
    date_saisie TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_note UNIQUE (eleve_id, matiere_id, periode_id)
);

-- 9. Table des Bulletins
CREATE TABLE bulletins (
    id SERIAL PRIMARY KEY,
    eleve_id INT NOT NULL REFERENCES eleves(id) ON DELETE CASCADE,
    periode_id INT NOT NULL REFERENCES periodes(id) ON DELETE CASCADE,
    total_moyenne_ponderee DOUBLE PRECISION NOT NULL,
    total_coefficient DOUBLE PRECISION NOT NULL,
    moyenne_generale DOUBLE PRECISION NOT NULL,
    rang INT NOT NULL,
    mention VARCHAR(50),
    appreciation_generale TEXT,
    pdf_path VARCHAR(500),
    locked BOOLEAN DEFAULT FALSE,
    date_generation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_bulletin UNIQUE (eleve_id, periode_id)
);

-- 10. Table des Mentions configurables
CREATE TABLE mentions (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50) UNIQUE NOT NULL,
    moyenne_min DOUBLE PRECISION NOT NULL,
    moyenne_max DOUBLE PRECISION NOT NULL
);

-- 11. Table des Suggestions d'appréciations
CREATE TABLE appreciations_suggestions (
    id SERIAL PRIMARY KEY,
    note_min DOUBLE PRECISION NOT NULL,
    note_max DOUBLE PRECISION NOT NULL,
    appreciation_defaut VARCHAR(255) NOT NULL
);

-- 12. Table de Log d'Audit (Journal)
CREATE TABLE journaux (
    id SERIAL PRIMARY KEY,
    utilisateur_id INT REFERENCES utilisateurs(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    details TEXT,
    date_action TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_adresse VARCHAR(45)
);

-- 13. Table des configurations globales (Logo, Nom d'école, adresse, etc.)
CREATE TABLE configurations (
    id SERIAL PRIMARY KEY,
    cle VARCHAR(50) UNIQUE NOT NULL,
    valeur TEXT NOT NULL
);

-- 14. Table de suivi des Sauvegardes
CREATE TABLE sauvegardes (
    id SERIAL PRIMARY KEY,
    nom_fichier VARCHAR(255) NOT NULL,
    chemin_fichier VARCHAR(500) NOT NULL,
    taille_octets BIGINT NOT NULL,
    date_sauvegarde TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    statut VARCHAR(50) NOT NULL
);
