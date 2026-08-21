package com.prisma.report;

/**
 * Bean de transfert de données représentant une ligne du tableau des notes
 * dans le bulletin PDF. Utilisé comme source de données JasperReports.
 *
 * Étape 88 — Intégration dynamique des données dans le template.
 */
public class BulletinLigneBean {

    private String numero;
    private String matiere;
    private String note;
    private String coefficient;
    private String moyennePonderee;
    private String appreciation;

    public BulletinLigneBean() {}

    public BulletinLigneBean(String numero, String matiere, String note,
                              String coefficient, String moyennePonderee, String appreciation) {
        this.numero = numero;
        this.matiere = matiere;
        this.note = note;
        this.coefficient = coefficient;
        this.moyennePonderee = moyennePonderee;
        this.appreciation = appreciation;
    }

    public String getNumero()           { return numero; }
    public void setNumero(String v)     { this.numero = v; }

    public String getMatiere()          { return matiere; }
    public void setMatiere(String v)    { this.matiere = v; }

    public String getNote()             { return note; }
    public void setNote(String v)       { this.note = v; }

    public String getCoefficient()      { return coefficient; }
    public void setCoefficient(String v){ this.coefficient = v; }

    public String getMoyennePonderee()           { return moyennePonderee; }
    public void setMoyennePonderee(String v)     { this.moyennePonderee = v; }

    public String getAppreciation()     { return appreciation; }
    public void setAppreciation(String v){ this.appreciation = v; }
}
