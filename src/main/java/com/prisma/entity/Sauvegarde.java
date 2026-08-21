package com.prisma.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sauvegardes")
public class Sauvegarde {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nom_fichier", nullable = false)
    private String nomFichier;

    @Column(name = "chemin_fichier", nullable = false)
    private String cheminFichier;

    @Column(name = "taille_octets", nullable = false)
    private long tailleOctets;

    @Column(name = "date_sauvegarde")
    private LocalDateTime dateSauvegarde = LocalDateTime.now();

    @Column(nullable = false)
    private String statut;

    public Sauvegarde() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNomFichier() {
        return nomFichier;
    }

    public void setNomFichier(String nomFichier) {
        this.nomFichier = nomFichier;
    }

    public String getCheminFichier() {
        return cheminFichier;
    }

    public void setCheminFichier(String cheminFichier) {
        this.cheminFichier = cheminFichier;
    }

    public long getTailleOctets() {
        return tailleOctets;
    }

    public void setTailleOctets(long tailleOctets) {
        this.tailleOctets = tailleOctets;
    }

    public LocalDateTime getDateSauvegarde() {
        return dateSauvegarde;
    }

    public void setDateSauvegarde(LocalDateTime dateSauvegarde) {
        this.dateSauvegarde = dateSauvegarde;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }
}
