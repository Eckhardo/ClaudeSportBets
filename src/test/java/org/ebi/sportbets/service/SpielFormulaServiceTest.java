package org.ebi.sportbets.service;

import org.ebi.sportbets.dto.SpielFormulaDto;
import org.ebi.sportbets.entity.Spiel;
import org.ebi.sportbets.entity.SpielFormula;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.repository.SpielFormulaRepository;
import org.ebi.sportbets.repository.SpielRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class SpielFormulaServiceTest {

    @Mock
    private SpielFormulaRepository spielFormulaRepository;

    @Mock
    private SpielRepository spielRepository;

    @InjectMocks
    private SpielFormulaService spielFormulaService;

    private final Spiel spiel = Spiel.builder().id(11L).build();

    // Request fields as a builder, so tests can add the server-side fields to build the expected DTO
    private static SpielFormulaDto.SpielFormulaDtoBuilder requestFields() {
        return SpielFormulaDto.builder()
                .spielId(11L)
                .teamName("FC Bayern München")
                .teamNameAcronym("FCB")
                .heimTeam(true)
                .heimTore(2)
                .gastTore(1)
                .diffTore(1)
                .won(1)
                .remis(0)
                .lost(0)
                .points(3);
    }

    @Test
    void should_mapAllFieldsToDto_when_spielFormulasExist() {
        // Arrange
        LocalDateTime createdOn = LocalDateTime.of(2026, 8, 1, 12, 0);
        given(spielFormulaRepository.findAll()).willReturn(List.of(SpielFormula.builder()
                .id(30L)
                .spiel(spiel)
                .teamName("FC Bayern München")
                .teamNameAcronym("FCB")
                .heimTeam(true)
                .heimTore(2)
                .gastTore(1)
                .diffTore(1)
                .won(1)
                .remis(0)
                .lost(0)
                .points(3)
                .createdOn(createdOn)
                .build()));

        // Act
        List<SpielFormulaDto> result = spielFormulaService.findAll();

        // Assert
        assertThat(result).containsExactly(requestFields().id(30L).createdOn(createdOn).build());
    }

    @Test
    void should_returnFormulasOfSpiel_when_findBySpielId() {
        // Arrange
        given(spielFormulaRepository.findBySpielId(11L)).willReturn(List.of(
                SpielFormula.builder().id(30L).spiel(spiel).build(),
                SpielFormula.builder().id(31L).spiel(spiel).build()));

        // Act
        List<SpielFormulaDto> result = spielFormulaService.findBySpielId(11L);

        // Assert
        assertThat(result).extracting(SpielFormulaDto::getId).containsExactly(30L, 31L);
    }

    @Test
    void should_throwResourceNotFound_when_spielFormulaWithIdDoesNotExist() {
        // Arrange
        given(spielFormulaRepository.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> spielFormulaService.findById(42L));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("SpielFormula with id 42 not found");
    }

    @Test
    void should_saveAndReturnSpielFormula_when_spielExists() {
        // Arrange
        given(spielRepository.findById(11L)).willReturn(Optional.of(spiel));
        given(spielFormulaRepository.save(any(SpielFormula.class))).willAnswer(invocation -> {
            SpielFormula toSave = invocation.getArgument(0);
            toSave.setId(30L);
            return toSave;
        });

        // Act
        SpielFormulaDto result = spielFormulaService.create(requestFields().id(99L).build());

        // Assert
        assertThat(result).isEqualTo(requestFields().id(30L).build());
    }

    @Test
    void should_throwResourceNotFoundAndSaveNothing_when_creatingForUnknownSpiel() {
        // Arrange
        given(spielRepository.findById(11L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> spielFormulaService.create(requestFields().build()));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Spiel with id 11 not found");
        then(spielFormulaRepository).should(never()).save(any(SpielFormula.class));
    }

    @Test
    void should_updateFieldsAndKeepIdAndCreatedOn_when_spielFormulaExists() {
        // Arrange
        LocalDateTime createdOn = LocalDateTime.of(2026, 8, 1, 12, 0);
        SpielFormula existing = SpielFormula.builder()
                .id(30L)
                .spiel(spiel)
                .teamName("Bayern")
                .points(0)
                .createdOn(createdOn)
                .build();
        given(spielFormulaRepository.findById(30L)).willReturn(Optional.of(existing));
        given(spielRepository.findById(11L)).willReturn(Optional.of(spiel));

        // Act
        SpielFormulaDto result = spielFormulaService.update(30L, requestFields().id(99L).build());

        // Assert
        assertThat(result).isEqualTo(requestFields().id(30L).createdOn(createdOn).build());
    }

    @Test
    void should_throwResourceNotFound_when_updatingUnknownSpielFormula() {
        // Arrange
        given(spielFormulaRepository.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> spielFormulaService.update(42L, requestFields().build()));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("SpielFormula with id 42 not found");
    }

    @Test
    void should_deleteSpielFormula_when_spielFormulaExists() {
        // Arrange
        SpielFormula existing = SpielFormula.builder().id(30L).build();
        given(spielFormulaRepository.findById(30L)).willReturn(Optional.of(existing));

        // Act
        spielFormulaService.delete(30L);

        // Assert
        then(spielFormulaRepository).should().delete(existing);
    }

    @Test
    void should_throwResourceNotFoundAndDeleteNothing_when_deletingUnknownSpielFormula() {
        // Arrange
        given(spielFormulaRepository.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> spielFormulaService.delete(42L));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("SpielFormula with id 42 not found");
        then(spielFormulaRepository).should(never()).delete(any(SpielFormula.class));
    }
}
