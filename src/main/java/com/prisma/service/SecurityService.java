package com.prisma.service;

import com.prisma.entity.Utilisateur;
import java.util.Optional;

/**
 * Service de sécurité gérant l'authentification, la vérification des rôles
 * et la gestion des sessions utilisateur.
 */
public interface SecurityService {

    /**
     * Tente de connecter un utilisateur avec ses identifiants.
     * En cas de succès, l'utilisateur est enregistré dans le {@link com.prisma.security.SessionContext}.
     *
     * @param username    Nom d'utilisateur
     * @param motDePasse  Mot de passe en clair
     * @return L'utilisateur connecté si les identifiants sont valides
     * @throws com.prisma.exception.PrismaException si les identifiants sont incorrects ou le compte est désactivé
     */
    Utilisateur connecter(String username, String motDePasse);

    /**
     * Déconnecte l'utilisateur courant et vide le contexte de session.
     */
    void deconnecter();

    /**
     * Retourne l'utilisateur actuellement connecté, s'il existe.
     */
    Optional<Utilisateur> getUtilisateurConnecte();

    /**
     * Vérifie que l'utilisateur connecté possède au moins un des rôles spécifiés.
     *
     * @param roles Un ou plusieurs noms de rôles autorisés
     * @throws com.prisma.exception.PrismaException si l'accès est refusé
     */
    void verifierAcces(String... roles);
}
