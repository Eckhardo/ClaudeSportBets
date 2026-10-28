package org.ebi.sportbets.service;

import org.ebi.sportbets.dto.CompetitionDto;
import org.ebi.sportbets.entity.Competition;
import org.ebi.sportbets.entity.CompetitionFamily;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.repository.CompetitionFamilyRepository;
import org.ebi.sportbets.repository.CompetitionRepository;
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
class CompetitionServiceTest {

    @Mock
    private CompetitionRepository competitionRepository;

    @Mock
    private CompetitionFamilyRepository competitionFamilyRepository;

    @InjectMocks
    private CompetitionService competitionService;

    @Test
    void should_returnDto_when_competitionWithIdExists() {
        // Arrange
        Competition competition = Competition.builder()
                .id(1L)
                .name("Bundesliga 2026/27")
                .winMultiplicator(3)
                .remisMultiplicator(1)
                .competitionFamily(CompetitionFamily.builder().id(7L).build())
                .build();
        given(competitionRepository.findById(1L)).willReturn(Optional.of(competition));

        // Act
        CompetitionDto result = competitionService.findById(1L);

        // Assert
        assertThat(result).isEqualTo(CompetitionDto.builder()
                .id(1L)
                .name("Bundesliga 2026/27")
                .winMultiplicator(3)
                .remisMultiplicator(1)
                .competitionFamilyId(7L)
                .build());
    }

    @Test
    void should_throwResourceNotFound_when_competitionWithIdDoesNotExist() {
        // Arrange
        given(competitionRepository.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> competitionService.findById(42L));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Competition with id 42 not found");
    }

    @Test
    void should_saveAndReturnCompetition_when_familyExists() {
        // Arrange
        CompetitionFamily family = CompetitionFamily.builder().id(7L).build();
        given(competitionFamilyRepository.findById(7L)).willReturn(Optional.of(family));
        given(competitionRepository.save(any(Competition.class))).willAnswer(invocation -> {
            Competition toSave = invocation.getArgument(0);
            toSave.setId(1L);
            return toSave;
        });
        CompetitionDto request = CompetitionDto.builder()
                .id(99L)
                .name("Bundesliga 2026/27")
                .winMultiplicator(3)
                .remisMultiplicator(1)
                .competitionFamilyId(7L)
                .build();

        // Act
        CompetitionDto result = competitionService.create(request);

        // Assert
        assertThat(result).isEqualTo(CompetitionDto.builder()
                .id(1L)
                .name("Bundesliga 2026/27")
                .winMultiplicator(3)
                .remisMultiplicator(1)
                .competitionFamilyId(7L)
                .build());
    }

    @Test
    void should_updateFieldsAndKeepIdAndCreatedOn_when_competitionExists() {
        // Arrange
        LocalDateTime createdOn = LocalDateTime.of(2026, 8, 1, 12, 0);
        Competition existing = Competition.builder()
                .id(1L)
                .name("Old name")
                .winMultiplicator(2)
                .remisMultiplicator(1)
                .competitionFamily(CompetitionFamily.builder().id(7L).build())
                .createdOn(createdOn)
                .build();
        given(competitionRepository.findById(1L)).willReturn(Optional.of(existing));
        given(competitionFamilyRepository.findById(8L)).willReturn(Optional.of(CompetitionFamily.builder().id(8L).build()));
        CompetitionDto request = CompetitionDto.builder()
                .id(99L)
                .name("New name")
                .description("Updated")
                .winMultiplicator(3)
                .remisMultiplicator(2)
                .competitionFamilyId(8L)
                .createdOn(LocalDateTime.of(2000, 1, 1, 0, 0))
                .build();

        // Act
        CompetitionDto result = competitionService.update(1L, request);

        // Assert
        assertThat(result).isEqualTo(CompetitionDto.builder()
                .id(1L)
                .name("New name")
                .description("Updated")
                .winMultiplicator(3)
                .remisMultiplicator(2)
                .competitionFamilyId(8L)
                .createdOn(createdOn)
                .build());
    }

    @Test
    void should_throwResourceNotFound_when_updatingUnknownCompetition() {
        // Arrange
        given(competitionRepository.findById(42L)).willReturn(Optional.empty());
        CompetitionDto request = CompetitionDto.builder()
                .name("New name")
                .winMultiplicator(3)
                .remisMultiplicator(1)
                .competitionFamilyId(7L)
                .build();

        // Act
        Throwable thrown = catchThrowable(() -> competitionService.update(42L, request));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Competition with id 42 not found");
    }

    @Test
    void should_throwResourceNotFound_when_updatingWithUnknownFamily() {
        // Arrange
        Competition existing = Competition.builder()
                .id(1L)
                .competitionFamily(CompetitionFamily.builder().id(7L).build())
                .build();
        given(competitionRepository.findById(1L)).willReturn(Optional.of(existing));
        given(competitionFamilyRepository.findById(8L)).willReturn(Optional.empty());
        CompetitionDto request = CompetitionDto.builder()
                .name("New name")
                .winMultiplicator(3)
                .remisMultiplicator(1)
                .competitionFamilyId(8L)
                .build();

        // Act
        Throwable thrown = catchThrowable(() -> competitionService.update(1L, request));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("CompetitionFamily with id 8 not found");
    }

    @Test
    void should_deleteCompetition_when_competitionExists() {
        // Arrange
        Competition existing = Competition.builder().id(1L).build();
        given(competitionRepository.findById(1L)).willReturn(Optional.of(existing));

        // Act
        competitionService.delete(1L);

        // Assert
        then(competitionRepository).should().delete(existing);
    }

    @Test
    void should_throwResourceNotFoundAndDeleteNothing_when_deletingUnknownCompetition() {
        // Arrange
        given(competitionRepository.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> competitionService.delete(42L));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Competition with id 42 not found");
        then(competitionRepository).should(never()).delete(any(Competition.class));
    }

    @Test
    void should_throwResourceNotFound_when_familyDoesNotExist() {
        // Arrange
        given(competitionFamilyRepository.findById(7L)).willReturn(Optional.empty());
        CompetitionDto request = CompetitionDto.builder()
                .name("Bundesliga 2026/27")
                .winMultiplicator(3)
                .remisMultiplicator(1)
                .competitionFamilyId(7L)
                .build();

        // Act
        Throwable thrown = catchThrowable(() -> competitionService.create(request));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("CompetitionFamily with id 7 not found");
    }

    @Test
    void should_mapAllFieldsToDto_when_competitionsExist() {
        // Arrange
        LocalDateTime createdOn = LocalDateTime.of(2026, 8, 1, 12, 0);
        Competition competition = Competition.builder()
                .id(1L)
                .name("Bundesliga 2026/27")
                .description("1. Liga")
                .winMultiplicator(3)
                .remisMultiplicator(1)
                .competitionFamily(CompetitionFamily.builder().id(7L).build())
                .createdOn(createdOn)
                .build();
        given(competitionRepository.findAll()).willReturn(List.of(competition));

        // Act
        List<CompetitionDto> result = competitionService.findAll();

        // Assert
        assertThat(result).containsExactly(CompetitionDto.builder()
                .id(1L)
                .name("Bundesliga 2026/27")
                .description("1. Liga")
                .winMultiplicator(3)
                .remisMultiplicator(1)
                .competitionFamilyId(7L)
                .createdOn(createdOn)
                .build());
    }

    @Test
    void should_returnEmptyList_when_noCompetitionsExist() {
        // Arrange
        given(competitionRepository.findAll()).willReturn(List.of());

        // Act
        List<CompetitionDto> result = competitionService.findAll();

        // Assert
        assertThat(result).isEmpty();
    }
}
