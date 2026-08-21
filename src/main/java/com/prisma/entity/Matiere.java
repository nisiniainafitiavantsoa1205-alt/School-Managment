package com.prisma.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "matieres")
public class Matiere {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String nom;

    /** Code court de la matière (ex: MATH, FR, HGE) — optionnel */
    @Column(unique = true)
    private String code;

    @Column(name = "ordre_affichage")
    private Integer ordreAffichage = 0;

    private boolean active = true;

    @Column(name = "couleur_hex")
    private String couleurHex = "#002F6C";

    public Matiere() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getOrdreAffichage() {
        return ordreAffichage;
    }

    public void setOrdreAffichage(Integer ordreAffichage) {
        this.ordreAffichage = ordreAffichage;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getCouleurHex() {
        return couleurHex;
    }

    public void setCouleurHex(String couleurHex) {
        this.couleurHex = couleurHex;
    }
}
