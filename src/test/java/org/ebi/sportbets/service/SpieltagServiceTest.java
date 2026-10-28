package org.ebi.sportbets.service;

import org.ebi.sportbets.dto.SpieltagDto;
import org.ebi.sportbets.entity.CompetitionRound;
import org.ebi.sportbets.entity.Spieltag;
import org.ebi.sportbets.exception.DuplicateResourceException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.repository.CompetitionRoundRepository;
import org.ebi.sportbets.repository.SpieltagRepository;
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
class SpieltagServiceTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 8, 21, 20, 30);

    @Mock
    private SpieltagRepository spieltagRepository;

    @Mock
    private CompetitionRoundRepository competitionRoundRepository;

    @InjectMocks
    private SpieltagService spieltagService;

    private static SpieltagDto validRequest(Long competitionRoundId) {
        return SpieltagDto.builder()
                .id(99L)
                .spieltagNumber(1)
                .startDate(START)
                .competitionRoundId(competitionRoundId)
                .build();
    }

    @Test
    void should_mapAllFieldsToDto_when_spieltageExist() {
        // Arrange
        LocalDateTime createdOn = LocalDateTime.of(2026, 8, 1, 12, 0);
        given(spieltagRepository.findAll()).willReturn(List.of(Spieltag.builder()
                .id(8L)
                .spieltagNumber(1)
                .startDate(START)
                .competitionRound(CompetitionRound.builder().id(5L).build())
                .createdOn(createdOn)
                .build()));

        // Act
        List<SpieltagDto> result = spieltagService.findAll();

        // Assert
        assertThat(result).containsExactly(SpieltagDto.builder()
                .id(8L)
                .spieltagNumber(1)
                .startDate(START)
                .competitionRoundId(5L)
                .createdOn(createdOn)
                .build());
    }

    @Test
    void should_returnSpieltageOfRound_when_findByCompetitionRoundId() {
        // Arrange
        CompetitionRound round = CompetitionRound.builder().id(5L).build();
        given(spieltagRepository.findByCompetitionRoundIdOrderBySpieltagNumberAsc(5L)).willReturn(List.of(
                Spieltag.builder().id(8L).spieltagNumber(1).competitionRound(round).build(),
                Spieltag.builder().id(9L).spieltagNumber(2).competitionRound(round).build()));

        // Act
        List<SpieltagDto> result = spieltagService.findByCompetitionRoundId(5L);

        // Assert
        assertThat(result).extracting(SpieltagDto::getId).containsExactly(8L, 9L);
    }

    @Test
    void should_throwResourceNotFound_when_spieltagWithIdDoesNotExist() {
        // Arrange
        given(spieltagRepository.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> spieltagService.findById(42L));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Spieltag with id 42 not found");
    }

    @Test
    void should_saveAndReturnSpieltag_when_roundExistsAndNumberIsFree() {
        // Arrange
        given(competitionRoundRepository.findById(5L)).willReturn(Optional.of(CompetitionRound.builder().id(5L).build()));
        given(spieltagRepository.findByCompetitionRoundIdAndSpieltagNumber(5L, 1)).willReturn(Optional.empty());
        given(spieltagRepository.save(any(Spieltag.class))).willAnswer(invocation -> {
            Spieltag toSave = invocation.getArgument(0);
            toSave.setId(8L);
            return toSave;
        });

        // Act
        SpieltagDto result = spieltagService.create(validRequest(5L));

        // Assert
        assertThat(result)
                .extracting(SpieltagDto::getId, SpieltagDto::getStartDate, SpieltagDto::getCompetitionRoundId)
                .containsExactly(8L, START, 5L);
    }

    @Test
    void should_throwResourceNotFoundAndSaveNothing_when_creatingForUnknownRound() {
        // Arrange
        given(competitionRoundRepository.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> spieltagService.create(validRequest(42L)));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("CompetitionRound with id 42 not found");
        then(spieltagRepository).should(never()).save(any(Spieltag.class));
    }

    @Test
    void should_throwDuplicateAndSaveNothing_when_creatingWithTakenNumber() {
        // Arrange
        given(competitionRoundRepository.findById(5L)).willReturn(Optional.of(CompetitionRound.builder().id(5L).build()));
        given(spieltagRepository.findByCompetitionRoundIdAndSpieltagNumber(5L, 1))
                .willReturn(Optional.of(Spieltag.builder().id(8L).build()));

        // Act
        Throwable thrown = catchThrowable(() -> spieltagService.create(validRequest(5L)));

        // Assert
        assertThat(thrown)
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Spieltag with number 1 already exists for CompetitionRound 5");
        then(spieltagRepository).should(never()).save(any(Spieltag.class));
    }

    @Test
    void should_updateFieldsAndKeepIdAndCreatedOn_when_spieltagExists() {
        // Arrange
        LocalDateTime createdOn = LocalDateTime.of(2026, 8, 1, 12, 0);
        CompetitionRound round = CompetitionRound.builder().id(5L).build();
        Spieltag existing = Spieltag.builder().id(8L).spieltagNumber(1).competitionRound(round).createdOn(createdOn).build();
        given(spieltagRepository.findById(8L)).willReturn(Optional.of(existing));
        given(competitionRoundRepository.findById(5L)).willReturn(Optional.of(round));
        // The Spieltag itself holds number 1, so it must not count as a duplicate
        given(spieltagRepository.findByCompetitionRoundIdAndSpieltagNumber(5L, 1)).willReturn(Optional.of(existing));

        // Act
        SpieltagDto result = spieltagService.update(8L, validRequest(5L));

        // Assert
        assertThat(result).isEqualTo(SpieltagDto.builder()
                .id(8L)
                .spieltagNumber(1)
                .startDate(START)
                .competitionRoundId(5L)
                .createdOn(createdOn)
                .build());
    }

    @Test
    void should_throwDuplicateAndKeepNumber_when_updatingToNumberOfOtherSpieltag() {
        // Arrange
        CompetitionRound round = CompetitionRound.builder().id(5L).build();
        Spieltag existing = Spieltag.builder().id(8L).spieltagNumber(2).competitionRound(round).build();
        given(spieltagRepository.findById(8L)).willReturn(Optional.of(existing));
        given(competitionRoundRepository.findById(5L)).willReturn(Optional.of(round));
        given(spieltagRepository.findByCompetitionRoundIdAndSpieltagNumber(5L, 1))
                .willReturn(Optional.of(Spieltag.builder().id(9L).build()));

        // Act
        Throwable thrown = catchThrowable(() -> spieltagService.update(8L, validRequest(5L)));

        // Assert
        assertThat(thrown).isInstanceOf(DuplicateResourceException.class);
        assertThat(existing.getSpieltagNumber()).isEqualTo(2);
    }

    @Test
    void should_deleteSpieltag_when_spieltagExists() {
        // Arrange
        Spieltag existing = Spieltag.builder().id(8L).build();
        given(spieltagRepository.findById(8L)).willReturn(Optional.of(existing));

        // Act
        spieltagService.delete(8L);

        // Assert
        then(spieltagRepository).should().delete(existing);
    }

    @Test
    void should_throwResourceNotFoundAndDeleteNothing_when_deletingUnknownSpieltag() {
        // Arrange
        given(spieltagRepository.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> spieltagService.delete(42L));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Spieltag with id 42 not found");
        then(spieltagRepository).should(never()).delete(any(Spieltag.class));
    }
}
