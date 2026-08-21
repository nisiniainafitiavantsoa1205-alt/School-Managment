-- SEEDING DATA FOR PRISMA

-- 1. Insertion des Rôles
INSERT INTO roles (nom, description) VALUES
('ADMINISTRATEUR', 'Gestion complète de l''application, des utilisateurs et des sauvegardes'),
('DIRECTEUR', 'Consultation des statistiques, historique d''audit et impression des bulletins'),
('PROFESSEUR', 'Saisie des notes et des appréciations pour ses classes affectées');

-- 2. Insertion de l'Administrateur par défaut
-- Mot de passe en clair : "admin123"
-- Haché avec BCrypt (cost factor 12)
INSERT INTO utilisateurs (username, password_hash, email, active, role_id) VALUES
('admin', '$2a$12$QP7R0yfM.Nxpu6hw3zkzM.L5M3Zs4d0N.AFeyKrFWbJcrEAT3uO6W', 'admin@prisma.com', TRUE,
 (SELECT id FROM roles WHERE nom = 'ADMINISTRATEUR'));

-- 3. Insertion des Mentions par défaut
INSERT INTO mentions (nom, moyenne_min, moyenne_max) VALUES
('Excellent', 16.0, 20.0),
('Très bien', 14.0, 15.99),
('Bien', 12.0, 13.99),
('Assez bien', 10.0, 11.99),
('Passable', 8.0, 9.99),
('Insuffisant', 0.0, 7.99);

-- 4. Insertion des Suggestions d'appréciations par défaut
INSERT INTO appreciations_suggestions (note_min, note_max, appreciation_defaut) VALUES
(16.0, 20.0, 'Très bien / Very good'),
(14.0, 15.99, 'Bien'),
(11.5, 13.99, 'Moyen / Assez bien'),
(0.0, 11.49, 'Insuffisant / Peut mieux faire');

-- 5. Insertion des Matières Standard (avec codes courts uniques)
INSERT INTO matieres (nom, code, ordre_affichage, active, couleur_hex) VALUES
('Français', 'FR', 1, TRUE, '#002F6C'),
('Mathématiques', 'MATH', 2, TRUE, '#FFD700'),
('Histoire-Géographie + EC', 'HGE', 3, TRUE, '#4682B4'),
('Sciences (SVT)', 'SVT', 4, TRUE, '#2E8B57'),
('Physique-Chimie', 'PC', 5, TRUE, '#8A2BE2'),
('Anglais', 'EN', 6, TRUE, '#D2691E'),
('Malagasy', 'MLG', 7, TRUE, '#CD5C5C'),
('Éducation Civique', 'EC', 8, TRUE, '#708090'),
('EPS (Sport)', 'EPS', 9, TRUE, '#FF4500'),
('Informatique', 'INFO', 10, TRUE, '#008080');

-- 6. Trimestre initial actif pour démarrer l'application
INSERT INTO periodes (nom, annee_scolaire, active, closed) VALUES
('1er Trimestre', '2025-2026', TRUE, FALSE);

-- 7. Configuration de Base (clés utilisées par ConfigController)
INSERT INTO configurations (cle, valeur) VALUES
('school.name', 'Collège Privé PRISMA'),
('school.address', 'Antananarivo, Madagascar'),
('school.phone', '+261 20 22 123 45'),
('school.year', '2025-2026');
