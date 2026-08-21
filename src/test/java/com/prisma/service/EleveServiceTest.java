package com.prisma.service;

import com.prisma.entity.Eleve;
import com.prisma.entity.Classe;
import com.prisma.repository.EleveRepository;
import com.prisma.service.impl.EleveServiceImpl;
import com.prisma.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EleveServiceTest {

    @Mock
    private EleveRepository eleveRepository;

    private EleveService eleveService;

    @BeforeEach
    void setUp() {
        eleveService = new EleveServiceImpl(eleveRepository);
    }

    @Test
    void creer_devrait_sauvegarder_eleve_valide() {
        // Arrange
        Eleve eleve = creerEleveValide();
        when(eleveRepository.save(any(Eleve.class))).thenReturn(eleve);

        // Act
        Eleve result = eleveService.creer(eleve);

        // Assert
        assertNotNull(result);
        assertEquals("DUPONT", result.getNom());
        verify(eleveRepository, times(1)).save(eleve);
    }

    @Test
    void creer_devrait_rejeter_eleve_sans_nom() {
        // Arrange
        Eleve eleve = new Eleve();
        eleve.setDateNaissance(LocalDate.of(2010, 3, 15));
        eleve.setNumeroAppel("01");

        // Act & Assert
        assertThrows(ValidationException.class, () -> eleveService.creer(eleve));
        verify(eleveRepository, never()).save(any());
    }

    @Test
    void creer_devrait_rejeter_eleve_sans_date_naissance() {
        // Arrange
        Eleve eleve = new Eleve();
        eleve.setNom("DUPONT");
        eleve.setNumeroAppel("01");

        // Act & Assert
        assertThrows(ValidationException.class, () -> eleveService.creer(eleve));
        verify(eleveRepository, never()).save(any());
    }

    @Test
    void rechercher_devrait_deléguer_au_repository() {
        // Arrange
        List<Eleve> eleves = List.of(creerEleveValide());
        when(eleveRepository.search("DUPONT", null, null, 1, 20)).thenReturn(eleves);

        // Act
        List<Eleve> result = eleveService.rechercher("DUPONT", null, 1, 20);

        // Assert
        assertEquals(1, result.size());
        verify(eleveRepository, times(1)).search("DUPONT", null, null, 1, 20);
    }

    @Test
    void trouverParId_devrait_retourner_optional() {
        // Arrange
        Eleve eleve = creerEleveValide();
        eleve.setId(1);
        when(eleveRepository.findById(1)).thenReturn(Optional.of(eleve));

        // Act
        Optional<Eleve> result = eleveService.trouverParId(1);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(1, result.get().getId());
    }

    @Test
    void supprimer_devrait_appeler_deleteById() {
        // Act
        eleveService.supprimer(5);

        // Assert
        verify(eleveRepository, times(1)).deleteById(5);
    }

    private Eleve creerEleveValide() {
        Eleve eleve = new Eleve();
        eleve.setNom("DUPONT");
        eleve.setPrenoms("Jean");
        eleve.setDateNaissance(LocalDate.of(2010, 3, 15));
        eleve.setNumeroAppel("01");
        eleve.setMatricule("PRISMA-2025-0001");
        return eleve;
    }
}
