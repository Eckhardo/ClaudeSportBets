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

@ExtendWith(MockitoExtension.class)
class CompetitionServiceTest {

    @Mock
    private CompetitionRepository competitionRepository;

    @Mock
    private CompetitionFamilyRepository competitionFamilyRepository;

    @InjectMocks
    private CompetitionService competitionService;

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
