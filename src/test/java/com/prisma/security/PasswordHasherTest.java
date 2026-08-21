package com.prisma.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {

    @Test
    void hacher_devrait_produire_un_hash_non_nul() {
        String hash = PasswordHasher.hacher("admin123");
        assertNotNull(hash);
        assertNotEquals("admin123", hash);
        assertTrue(hash.startsWith("$2a$12$"), "Le hash doit commencer par le préfixe BCrypt work factor 12");
    }

    @Test
    void verifier_devrait_valider_le_mot_de_passe_correct() {
        String motDePasse = "mon_mot_de_passe_securise";
        String hash = PasswordHasher.hacher(motDePasse);
        assertTrue(PasswordHasher.verifier(motDePasse, hash));
    }

    @Test
    void verifier_devrait_rejeter_un_mot_de_passe_incorrect() {
        String hash = PasswordHasher.hacher("admin123");
        assertFalse(PasswordHasher.verifier("mauvais_mdp", hash));
    }

    @Test
    void verifier_devrait_retourner_false_si_null() {
        assertFalse(PasswordHasher.verifier(null, "$2a$12$xxx"));
        assertFalse(PasswordHasher.verifier("admin123", null));
    }

    @Test
    void deux_hashs_du_meme_mot_de_passe_doivent_etre_differents() {
        // BCrypt génère un sel aléatoire à chaque fois
        String hash1 = PasswordHasher.hacher("admin123");
        String hash2 = PasswordHasher.hacher("admin123");
        assertNotEquals(hash1, hash2);
        // Mais les deux doivent vérifier le même mot de passe
        assertTrue(PasswordHasher.verifier("admin123", hash1));
        assertTrue(PasswordHasher.verifier("admin123", hash2));
    }

    @Test
    void testAdminHashInSql() {
        assertTrue(PasswordHasher.verifier("admin123", "$2a$12$QP7R0yfM.Nxpu6hw3zkzM.L5M3Zs4d0N.AFeyKrFWbJcrEAT3uO6W"));
    }
}
