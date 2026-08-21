package com.prisma.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "mentions")
public class Mention {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String nom;

    @Column(name = "moyenne_min", nullable = false)
    private double moyenneMin;

    @Column(name = "moyenne_max", nullable = false)
    private double moyenneMax;

    public Mention() {}

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

    public double getMoyenneMin() {
        return moyenneMin;
    }

    public void setMoyenneMin(double moyenneMin) {
        this.moyenneMin = moyenneMin;
    }

    public double getMoyenneMax() {
        return moyenneMax;
    }

    public void setMoyenneMax(double moyenneMax) {
        this.moyenneMax = moyenneMax;
    }
}
