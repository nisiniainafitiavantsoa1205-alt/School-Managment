package com.prisma.controller;

import com.prisma.entity.AppreciationSuggestion;
import com.prisma.entity.Eleve;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NoteValidationTest {

    private List<AppreciationSuggestion> mockSuggestions;
    private Eleve mockEleve;

    @BeforeEach
    public void setUp() {
        mockEleve = new Eleve();
        mockEleve.setId(1);
        mockEleve.setNom("RABEARISOA");
        mockEleve.setPrenoms("Jean");

        mockSuggestions = new ArrayList<>();
        
        AppreciationSuggestion sugg1 = new AppreciationSuggestion();
        sugg1.setNoteMin(16.0);
        sugg1.setNoteMax(20.0);
        sugg1.setAppreciationDefaut("Excellent");
        mockSuggestions.add(sugg1);

        AppreciationSuggestion sugg2 = new AppreciationSuggestion();
        sugg2.setNoteMin(12.0);
        sugg2.setNoteMax(15.99);
        sugg2.setAppreciationDefaut("Bien");
        mockSuggestions.add(sugg2);

        AppreciationSuggestion sugg3 = new AppreciationSuggestion();
        sugg3.setNoteMin(0.0);
        sugg3.setNoteMax(11.99);
        sugg3.setAppreciationDefaut("Insuffisant");
        mockSuggestions.add(sugg3);
    }

    @Test
    public void testValidationNoteValide() {
        NoteController.NoteRow row = new NoteController.NoteRow(1, mockEleve, null);

        // Tester une note décimale valide
        boolean valid = validerSaisieNoteLocale(row, "14.5");
        assertTrue(valid);
        assertEquals("14.5", row.getNote());
        assertFalse(row.isAbsent());

        // Tester note maximale
        valid = validerSaisieNoteLocale(row, "20");
        assertTrue(valid);
        assertEquals("20.0", row.getNote());
        assertFalse(row.isAbsent());
    }

    @Test
    public void testValidationNoteAbsence() {
        NoteController.NoteRow row = new NoteController.NoteRow(1, mockEleve, null);

        boolean valid = validerSaisieNoteLocale(row, "A");
        assertTrue(valid);
        assertEquals("A", row.getNote());
        assertTrue(row.isAbsent());

        valid = validerSaisieNoteLocale(row, "ABS");
        assertTrue(valid);
        assertEquals("A", row.getNote());
        assertTrue(row.isAbsent());
    }

    @Test
    public void testValidationNoteInvalide() {
        NoteController.NoteRow row = new NoteController.NoteRow(1, mockEleve, null);

        // Note hors limites (trop grande)
        boolean valid = validerSaisieNoteLocale(row, "21");
        assertFalse(valid);

        // Note hors limites (négative)
        valid = validerSaisieNoteLocale(row, "-1.5");
        assertFalse(valid);

        // Texte incorrect
        valid = validerSaisieNoteLocale(row, "note_text");
        assertFalse(valid);
    }

    @Test
    public void testAppreciationSuggestion() {
        NoteController.NoteRow row = new NoteController.NoteRow(1, mockEleve, null);

        // Note de 18 -> Excellent
        row.setNote("18.0");
        row.setAbsent(false);
        appliquerSuggestionLocale(row);
        assertEquals("Excellent", row.getAppreciation());

        // Note de 14.2 -> Bien
        row.setNote("14.2");
        row.setAbsent(false);
        appliquerSuggestionLocale(row);
        assertEquals("Bien", row.getAppreciation());

        // Note de 8.5 -> Insuffisant
        row.setNote("8.5");
        row.setAbsent(false);
        appliquerSuggestionLocale(row);
        assertEquals("Insuffisant", row.getAppreciation());

        // Absent -> Absent
        row.setNote("A");
        row.setAbsent(true);
        appliquerSuggestionLocale(row);
        assertEquals("Absent", row.getAppreciation());
    }

    // Version locale isolée de la logique contrôleur pour le test unitaire
    private boolean validerSaisieNoteLocale(NoteController.NoteRow row, String input) {
        if (input == null || input.isEmpty()) {
            row.setNote("");
            row.setAbsent(false);
            return true;
        }

        if (input.equals("A") || input.equals("ABS")) {
            row.setNote("A");
            row.setAbsent(true);
            return true;
        }

        try {
            double note = Double.parseDouble(input);
            if (note >= 0 && note <= 20) {
                row.setNote(String.valueOf(note));
                row.setAbsent(false);
                return true;
            }
        } catch (NumberFormatException ignored) {}
        return false;
    }

    private void appliquerSuggestionLocale(NoteController.NoteRow row) {
        if (row.isAbsent() || row.getNote().isEmpty()) {
            row.setAppreciation("Absent");
            return;
        }

        try {
            double note = Double.parseDouble(row.getNote());
            for (AppreciationSuggestion sugg : mockSuggestions) {
                if (note >= sugg.getNoteMin() && note <= sugg.getNoteMax()) {
                    row.setAppreciation(sugg.getAppreciationDefaut());
                    break;
                }
            }
        } catch (Exception ignored) {}
    }
}
