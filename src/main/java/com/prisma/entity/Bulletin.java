package com.prisma.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bulletins", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"eleve_id", "periode_id"})
})
public class Bulletin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "eleve_id", nullable = false)
    private Eleve eleve;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "periode_id", nullable = false)
    private Periode periode;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "classe_id")
    private Classe classe;

    @Column(name = "total_moyenne_ponderee", nullable = false)
    private double totalMoyennePonderee;

    @Column(name = "total_coefficient", nullable = false)
    private double totalCoefficient;

    @Column(name = "moyenne_generale", nullable = false)
    private double moyenneGenerale;

    @Column(nullable = false)
    private int rang;

    private String mention;

    @Column(name = "appreciation_generale")
    private String appreciationGenerale;

    @Column(name = "pdf_path")
    private String pdfPath;

    private boolean locked = false;

    @Column(name = "date_generation")
    private LocalDateTime dateGeneration = LocalDateTime.now();

    public Bulletin() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Eleve getEleve() {
        return eleve;
    }

    public void setEleve(Eleve eleve) {
        this.eleve = eleve;
    }

    public Periode getPeriode() {
        return periode;
    }

    public void setPeriode(Periode periode) {
        this.periode = periode;
    }

    public double getTotalMoyennePonderee() {
        return totalMoyennePonderee;
    }

    public void setTotalMoyennePonderee(double totalMoyennePonderee) {
        this.totalMoyennePonderee = totalMoyennePonderee;
    }

    public double getTotalCoefficient() {
        return totalCoefficient;
    }

    public void setTotalCoefficient(double totalCoefficient) {
        this.totalCoefficient = totalCoefficient;
    }

    public double getMoyenneGenerale() {
        return moyenneGenerale;
    }

    public void setMoyenneGenerale(double moyenneGenerale) {
        this.moyenneGenerale = moyenneGenerale;
    }

    public int getRang() {
        return rang;
    }

    public void setRang(int rang) {
        this.rang = rang;
    }

    public String getMention() {
        return mention;
    }

    public void setMention(String mention) {
        this.mention = mention;
    }

    public String getAppreciationGenerale() {
        return appreciationGenerale;
    }

    public void setAppreciationGenerale(String appreciationGenerale) {
        this.appreciationGenerale = appreciationGenerale;
    }

    public String getPdfPath() {
        return pdfPath;
    }

    public void setPdfPath(String pdfPath) {
        this.pdfPath = pdfPath;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public LocalDateTime getDateGeneration() {
        return dateGeneration;
    }

    public void setDateGeneration(LocalDateTime dateGeneration) {
        this.dateGeneration = dateGeneration;
    }

    public Classe getClasse() {
        return classe;
    }

    public void setClasse(Classe classe) {
        this.classe = classe;
    }
}
