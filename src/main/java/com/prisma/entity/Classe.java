package com.prisma.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "classes")
public class Classe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String niveau;

    @Column(name = "annee_scolaire", nullable = false)
    private String anneeScolaire;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "professeur_principal_id")
    private Utilisateur professeurPrincipal;

    public Classe() {}

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

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }

    public String getAnneeScolaire() {
        return anneeScolaire;
    }

    public void setAnneeScolaire(String anneeScolaire) {
        this.anneeScolaire = anneeScolaire;
    }

    public Utilisateur getProfesseurPrincipal() {
        return professeurPrincipal;
    }

    public void setProfesseurPrincipal(Utilisateur professeurPrincipal) {
        this.professeurPrincipal = professeurPrincipal;
    }
}
