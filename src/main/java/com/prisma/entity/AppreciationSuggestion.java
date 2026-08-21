package com.prisma.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "appreciations_suggestions")
public class AppreciationSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "note_min", nullable = false)
    private double noteMin;

    @Column(name = "note_max", nullable = false)
    private double noteMax;

    @Column(name = "appreciation_defaut", nullable = false)
    private String appreciationDefaut;

    public AppreciationSuggestion() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public double getNoteMin() {
        return noteMin;
    }

    public void setNoteMin(double noteMin) {
        this.noteMin = noteMin;
    }

    public double getNoteMax() {
        return noteMax;
    }

    public void setNoteMax(double noteMax) {
        this.noteMax = noteMax;
    }

    public String getAppreciationDefaut() {
        return appreciationDefaut;
    }

    public void setAppreciationDefaut(String appreciationDefaut) {
        this.appreciationDefaut = appreciationDefaut;
    }
}
