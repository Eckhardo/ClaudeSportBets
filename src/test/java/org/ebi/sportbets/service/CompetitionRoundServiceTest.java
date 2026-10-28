package org.ebi.sportbets.service;

import org.ebi.sportbets.dto.CompetitionRoundDto;
import org.ebi.sportbets.entity.Competition;
import org.ebi.sportbets.entity.CompetitionRound;
import org.ebi.sportbets.exception.DuplicateResourceException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.repository.CompetitionRepository;
import org.ebi.sportbets.repository.CompetitionRoundRepository;
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
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CompetitionRoundServiceTest {

    @Mock
    private CompetitionRoundRepository competitionRoundRepository;

    @Mock
    private CompetitionRepository competitionRepository;

    @InjectMocks
    private CompetitionRoundService competitionRoundService;

    private static CompetitionRoundDto validRequest(Long competitionId) {
        return CompetitionRoundDto.builder()
                .id(99L)
                .name("Hinrunde")
                .roundNumber(1)
                .firstMatchday(1)
                .matchdaysSize(17)
                .teamsSize(18)
                .hasGroups(false)
                .competitionId(competitionId)
                .build();
    }

    @Test
    void should_mapAllFieldsToDto_when_roundsExist() {
        // Arrange
        LocalDateTime createdOn = LocalDateTime.of(2026, 8, 1, 12, 0);
        CompetitionRound round = CompetitionRound.builder()
                .id(5L)
                .name("Hinrunde")
                .roundNumber(1)
                .firstMatchday(1)
                .matchdaysSize(17)
                .teamsSize(18)
                .hasGroups(true)
                .competition(Competition.builder().id(3L).build())
                .createdOn(createdOn)
                .build();
        given(competitionRoundRepository.findAll()).willReturn(List.of(round));

        // Act
        List<CompetitionRoundDto> result = competitionRoundService.findAll();

        // Assert
        assertThat(result).containsExactly(CompetitionRoundDto.builder()
                .id(5L)
                .name("Hinrunde")
                .roundNumber(1)
                .firstMatchday(1)
                .matchdaysSize(17)
                .teamsSize(18)
                .hasGroups(true)
                .competitionId(3L)
                .createdOn(createdOn)
                .build());
    }

    @Test
    void should_returnRoundsOfCompetition_when_findByCompetitionId() {
        // Arrange
        Competition competition = Competition.builder().id(3L).build();
        given(competitionRoundRepository.findByCompetitionIdOrderByRoundNumberAsc(3L)).willReturn(List.of(
                CompetitionRound.builder().id(5L).roundNumber(1).competition(competition).build(),
                CompetitionRound.builder().id(6L).roundNumber(2).competition(competition).build()));

        // Act
        List<CompetitionRoundDto> result = competitionRoundService.findByCompetitionId(3L);

        // Assert
        assertThat(result)
                .extracting(CompetitionRoundDto::getId, CompetitionRoundDto::getRoundNumber)
                .containsExactly(
                        tuple(5L, 1),
                        tuple(6L, 2));
    }

    @Test
    void should_throwResourceNotFound_when_roundWithIdDoesNotExist() {
        // Arrange
        given(competitionRoundRepository.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> competitionRoundService.findById(42L));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("CompetitionRound with id 42 not found");
    }

    @Test
    void should_saveAndReturnRound_when_competitionExistsAndRoundNumberIsFree() {
        // Arrange
        Competition competition = Competition.builder().id(3L).build();
        given(competitionRepository.findById(3L)).willReturn(Optional.of(competition));
        given(competitionRoundRepository.findByCompetitionIdAndRoundNumber(3L, 1)).willReturn(Optional.empty());
        given(competitionRoundRepository.save(any(CompetitionRound.class))).willAnswer(invocation -> {
            CompetitionRound toSave = invocation.getArgument(0);
            toSave.setId(5L);
            return toSave;
        });

        // Act
        CompetitionRoundDto result = competitionRoundService.create(validRequest(3L));

        // Assert
        assertThat(result)
                .extracting(CompetitionRoundDto::getId, CompetitionRoundDto::getName, CompetitionRoundDto::getCompetitionId)
                .containsExactly(5L, "Hinrunde", 3L);
    }

    @Test
    void should_throwResourceNotFoundAndSaveNothing_when_creatingForUnknownCompetition() {
        // Arrange
        given(competitionRepository.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> competitionRoundService.create(validRequest(42L)));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Competition with id 42 not found");
        then(competitionRoundRepository).should(never()).save(any(CompetitionRound.class));
    }

    @Test
    void should_throwDuplicateAndSaveNothing_when_creatingWithTakenRoundNumber() {
        // Arrange
        Competition competition = Competition.builder().id(3L).build();
        given(competitionRepository.findById(3L)).willReturn(Optional.of(competition));
        given(competitionRoundRepository.findByCompetitionIdAndRoundNumber(3L, 1))
                .willReturn(Optional.of(CompetitionRound.builder().id(5L).build()));

        // Act
        Throwable thrown = catchThrowable(() -> competitionRoundService.create(validRequest(3L)));

        // Assert
        assertThat(thrown)
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("CompetitionRound with round number 1 already exists for Competition 3");
        then(competitionRoundRepository).should(never()).save(any(CompetitionRound.class));
    }

    @Test
    void should_updateFieldsAndKeepIdAndCreatedOn_when_roundExists() {
        // Arrange
        LocalDateTime createdOn = LocalDateTime.of(2026, 8, 1, 12, 0);
        Competition competition = Competition.builder().id(3L).build();
        CompetitionRound existing = CompetitionRound.builder()
                .id(5L)
                .name("Vorrunde")
                .roundNumber(1)
                .competition(competition)
                .createdOn(createdOn)
                .build();
        given(competitionRoundRepository.findById(5L)).willReturn(Optional.of(existing));
        given(competitionRepository.findById(3L)).willReturn(Optional.of(competition));
        // The round itself holds round number 1, so it must not count as a duplicate
        given(competitionRoundRepository.findByCompetitionIdAndRoundNumber(3L, 1)).willReturn(Optional.of(existing));

        // Act
        CompetitionRoundDto result = competitionRoundService.update(5L, validRequest(3L));

        // Assert
        assertThat(result).isEqualTo(CompetitionRoundDto.builder()
                .id(5L)
                .name("Hinrunde")
                .roundNumber(1)
                .firstMatchday(1)
                .matchdaysSize(17)
                .teamsSize(18)
                .hasGroups(false)
                .competitionId(3L)
                .createdOn(createdOn)
                .build());
    }

    @Test
    void should_throwResourceNotFound_when_updatingUnknownRound() {
        // Arrange
        given(competitionRoundRepository.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> competitionRoundService.update(42L, validRequest(3L)));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("CompetitionRound with id 42 not found");
    }

    @Test
    void should_throwDuplicateAndKeepRoundNumber_when_updatingToRoundNumberOfOtherRound() {
        // Arrange
        Competition competition = Competition.builder().id(3L).build();
        CompetitionRound existing = CompetitionRound.builder().id(5L).roundNumber(2).competition(competition).build();
        given(competitionRoundRepository.findById(5L)).willReturn(Optional.of(existing));
        given(competitionRepository.findById(3L)).willReturn(Optional.of(competition));
        given(competitionRoundRepository.findByCompetitionIdAndRoundNumber(3L, 1))
                .willReturn(Optional.of(CompetitionRound.builder().id(6L).build()));

        // Act
        Throwable thrown = catchThrowable(() -> competitionRoundService.update(5L, validRequest(3L)));

        // Assert
        assertThat(thrown)
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("CompetitionRound with round number 1 already exists for Competition 3");
        assertThat(existing.getRoundNumber()).isEqualTo(2);
    }

    @Test
    void should_deleteRound_when_roundExists() {
        // Arrange
        CompetitionRound existing = CompetitionRound.builder().id(5L).build();
        given(competitionRoundRepository.findById(5L)).willReturn(Optional.of(existing));

        // Act
        competitionRoundService.delete(5L);

        // Assert
        then(competitionRoundRepository).should().delete(existing);
    }

    @Test
    void should_throwResourceNotFoundAndDeleteNothing_when_deletingUnknownRound() {
        // Arrange
        given(competitionRoundRepository.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> competitionRoundService.delete(42L));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("CompetitionRound with id 42 not found");
        then(competitionRoundRepository).should(never()).delete(any(CompetitionRound.class));
    }
}
