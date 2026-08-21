package com.prisma.security;

import com.prisma.entity.Utilisateur;

/**
 * Contexte de session de l'utilisateur connecté (Design Pattern Singleton).
 * Stocke l'utilisateur actif pour la durée de la session de travail.
 */
public class SessionContext {

    private static SessionContext instance;
    private Utilisateur utilisateurConnecte;

    private SessionContext() {}

    public static synchronized SessionContext getInstance() {
        if (instance == null) {
            instance = new SessionContext();
        }
        return instance;
    }

    public void connecter(Utilisateur utilisateur) {
        this.utilisateurConnecte = utilisateur;
    }

    public void deconnecter() {
        this.utilisateurConnecte = null;
    }

    public Utilisateur getUtilisateurConnecte() {
        return utilisateurConnecte;
    }

    public boolean estConnecte() {
        return utilisateurConnecte != null;
    }

    /**
     * Vérifie si l'utilisateur connecté possède le rôle demandé.
     *
     * @param nomRole Le nom du rôle à vérifier (ex: "ADMINISTRATEUR")
     * @return {@code true} si l'utilisateur possède ce rôle
     */
    public boolean aLeRole(String nomRole) {
        return estConnecte()
                && utilisateurConnecte.getRole() != null
                && nomRole.equalsIgnoreCase(utilisateurConnecte.getRole().getNom());
    }

    public boolean estAdministrateur() {
        return aLeRole("ADMINISTRATEUR");
    }

    public boolean estDirecteur() {
        return aLeRole("DIRECTEUR");
    }

    public boolean estProfesseur() {
        return aLeRole("PROFESSEUR");
    }
}
