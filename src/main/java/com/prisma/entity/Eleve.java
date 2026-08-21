package com.prisma.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "eleves")
public class Eleve {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String matricule;

    @Column(name = "numero_appel", nullable = false)
    private String numeroAppel;

    @Column(nullable = false)
    private String nom;

    private String prenoms;

    @Column(name = "date_naissance", nullable = false)
    private LocalDate dateNaissance;

    @Lob
    @Column(name = "photo_data")
    private byte[] photoData;

    @Column(length = 20)
    private String sexe;

    @Column(length = 255)
    private String adresse;

    @Column(name = "nom_parent", length = 150)
    private String nomParent;

    @Column(name = "profession_parent", length = 150)
    private String professionParent;

    @Column(name = "telephone_parent", length = 50)
    private String telephoneParent;

    @Column(name = "nom_parent2", length = 150)
    private String nomParent2;

    @Column(name = "profession_parent2", length = 150)
    private String professionParent2;

    @Column(name = "telephone_parent2", length = 50)
    private String telephoneParent2;

    @Column(length = 255)
    private String tuteur;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "classe_id")
    private Classe classe;

    @Column(length = 30)
    private String statut = "ACTIF";

    public Eleve() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMatricule() {
        return matricule;
    }

    public void setMatricule(String matricule) {
        this.matricule = matricule;
    }

    public String getNumeroAppel() {
        return numeroAppel;
    }

    public void setNumeroAppel(String numeroAppel) {
        this.numeroAppel = numeroAppel;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenoms() {
        return prenoms;
    }

    public void setPrenoms(String prenoms) {
        this.prenoms = prenoms;
    }

    public LocalDate getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(LocalDate dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    public byte[] getPhotoData() {
        return photoData;
    }

    public void setPhotoData(byte[] photoData) {
        this.photoData = photoData;
    }

    public byte[] getPhoto() {
        return getPhotoData();
    }

    public void setPhoto(byte[] photo) {
        setPhotoData(photo);
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getNomParent() {
        return nomParent;
    }

    public void setNomParent(String nomParent) {
        this.nomParent = nomParent;
    }

    public String getTelephoneParent() {
        return telephoneParent;
    }

    public void setTelephoneParent(String telephoneParent) {
        this.telephoneParent = telephoneParent;
    }

    public String getNomParent2() {
        return nomParent2;
    }

    public void setNomParent2(String nomParent2) {
        this.nomParent2 = nomParent2;
    }

    public String getTelephoneParent2() {
        return telephoneParent2;
    }

    public void setTelephoneParent2(String telephoneParent2) {
        this.telephoneParent2 = telephoneParent2;
    }

    public String getTuteur() {
        return tuteur;
    }

    public void setTuteur(String tuteur) {
        this.tuteur = tuteur;
    }

    public Classe getClasse() {
        return classe;
    }

    public void setClasse(Classe classe) {
        this.classe = classe;
    }

    public String getSexe() {
        return sexe;
    }

    public void setSexe(String sexe) {
        this.sexe = sexe;
    }

    public String getProfessionParent() {
        return professionParent;
    }

    public void setProfessionParent(String professionParent) {
        this.professionParent = professionParent;
    }

    public String getProfessionParent2() {
        return professionParent2;
    }

    public void setProfessionParent2(String professionParent2) {
        this.professionParent2 = professionParent2;
    }

    public String getStatut() {
        return statut != null ? statut : "ACTIF";
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }
}
