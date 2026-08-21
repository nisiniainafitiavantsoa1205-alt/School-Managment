package com.prisma.security;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utilitaire de chiffrement des mots de passe avec l'algorithme BCrypt.
 * Le facteur de coût (work factor) est fixé à 12 conformément aux règles de sécurité.
 */
public class PasswordHasher {

    private static final int WORK_FACTOR = 12;

    private PasswordHasher() {}

    /**
     * Génère un hash BCrypt sécurisé pour un mot de passe en clair.
     *
     * @param motDePasseClair Le mot de passe en clair à chiffrer
     * @return Le hash BCrypt du mot de passe
     */
    public static String hacher(String motDePasseClair) {
        return BCrypt.hashpw(motDePasseClair, BCrypt.gensalt(WORK_FACTOR));
    }

    /**
     * Vérifie si un mot de passe en clair correspond à un hash BCrypt stocké.
     *
     * @param motDePasseClair Le mot de passe en clair à vérifier
     * @param hash            Le hash BCrypt stocké en base de données
     * @return {@code true} si le mot de passe correspond, {@code false} sinon
     */
    public static boolean verifier(String motDePasseClair, String hash) {
        if (motDePasseClair == null || hash == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(motDePasseClair, hash);
        } catch (Exception e) {
            return false;
        }
    }
}
