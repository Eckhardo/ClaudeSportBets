package org.ebi.sportbets.service;

import org.ebi.sportbets.dto.TeamDto;
import org.ebi.sportbets.entity.Team;
import org.ebi.sportbets.exception.BusinessRuleException;
import org.ebi.sportbets.exception.DuplicateResourceException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.repository.CompetitionTeamRepository;
import org.ebi.sportbets.repository.SpielRepository;
import org.ebi.sportbets.repository.TeamRepository;
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
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private SpielRepository spielRepository;

    @Mock
    private CompetitionTeamRepository competitionTeamRepository;

    @InjectMocks
    private TeamService teamService;

    private static TeamDto validRequest() {
        return TeamDto.builder()
                .id(99L)
                .name("FC Bayern München")
                .acronym("FCB")
                .hasClub(true)
                .build();
    }

    @Test
    void should_mapAllFieldsToDto_when_teamsExist() {
        // Arrange
        LocalDateTime createdOn = LocalDateTime.of(2026, 8, 1, 12, 0);
        given(teamRepository.findAll()).willReturn(List.of(Team.builder()
                .id(4L)
                .name("FC Bayern München")
                .acronym("FCB")
                .hasClub(true)
                .createdOn(createdOn)
                .build()));

        // Act
        List<TeamDto> result = teamService.findAll();

        // Assert
        assertThat(result).containsExactly(TeamDto.builder()
                .id(4L)
                .name("FC Bayern München")
                .acronym("FCB")
                .hasClub(true)
                .createdOn(createdOn)
                .build());
    }

    @Test
    void should_throwResourceNotFound_when_teamWithIdDoesNotExist() {
        // Arrange
        given(teamRepository.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> teamService.findById(42L));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Team with id 42 not found");
    }

    @Test
    void should_saveAndReturnTeam_when_nameAndAcronymAreUnique() {
        // Arrange
        given(teamRepository.existsByName("FC Bayern München")).willReturn(false);
        given(teamRepository.existsByAcronym("FCB")).willReturn(false);
        given(teamRepository.save(any(Team.class))).willAnswer(invocation -> {
            Team toSave = invocation.getArgument(0);
            toSave.setId(4L);
            return toSave;
        });

        // Act
        TeamDto result = teamService.create(validRequest());

        // Assert
        assertThat(result)
                .extracting(TeamDto::getId, TeamDto::getName, TeamDto::getAcronym)
                .containsExactly(4L, "FC Bayern München", "FCB");
    }

    @Test
    void should_throwDuplicateAndSaveNothing_when_creatingWithExistingName() {
        // Arrange
        given(teamRepository.existsByName("FC Bayern München")).willReturn(true);

        // Act
        Throwable thrown = catchThrowable(() -> teamService.create(validRequest()));

        // Assert
        assertThat(thrown)
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Team with name FC Bayern München already exists");
        then(teamRepository).should(never()).save(any(Team.class));
    }

    @Test
    void should_throwDuplicateAndSaveNothing_when_creatingWithExistingAcronym() {
        // Arrange
        given(teamRepository.existsByName("FC Bayern München")).willReturn(false);
        given(teamRepository.existsByAcronym("FCB")).willReturn(true);

        // Act
        Throwable thrown = catchThrowable(() -> teamService.create(validRequest()));

        // Assert
        assertThat(thrown)
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Team with acronym FCB already exists");
        then(teamRepository).should(never()).save(any(Team.class));
    }

    @Test
    void should_updateFieldsAndKeepIdAndCreatedOn_when_teamExists() {
        // Arrange
        LocalDateTime createdOn = LocalDateTime.of(2026, 8, 1, 12, 0);
        Team existing = Team.builder().id(4L).name("Bayern").acronym("FCB").createdOn(createdOn).build();
        given(teamRepository.findById(4L)).willReturn(Optional.of(existing));
        given(teamRepository.existsByNameAndIdNot("FC Bayern München", 4L)).willReturn(false);
        given(teamRepository.existsByAcronymAndIdNot("FCB", 4L)).willReturn(false);

        // Act
        TeamDto result = teamService.update(4L, validRequest());

        // Assert
        assertThat(result).isEqualTo(TeamDto.builder()
                .id(4L)
                .name("FC Bayern München")
                .acronym("FCB")
                .hasClub(true)
                .createdOn(createdOn)
                .build());
    }

    @Test
    void should_throwDuplicateAndKeepAcronym_when_updatingToAcronymOfOtherTeam() {
        // Arrange
        Team existing = Team.builder().id(4L).name("FC Bayern München").acronym("BAY").build();
        given(teamRepository.findById(4L)).willReturn(Optional.of(existing));
        given(teamRepository.existsByNameAndIdNot("FC Bayern München", 4L)).willReturn(false);
        given(teamRepository.existsByAcronymAndIdNot("FCB", 4L)).willReturn(true);

        // Act
        Throwable thrown = catchThrowable(() -> teamService.update(4L, validRequest()));

        // Assert
        assertThat(thrown)
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Team with acronym FCB already exists");
        assertThat(existing.getAcronym()).isEqualTo("BAY");
    }

    @Test
    void should_deleteTeam_when_teamIsNotReferenced() {
        // Arrange
        Team existing = Team.builder().id(4L).build();
        given(teamRepository.findById(4L)).willReturn(Optional.of(existing));
        given(spielRepository.existsByHeimTeamIdOrGastTeamId(4L, 4L)).willReturn(false);
        given(competitionTeamRepository.existsByTeamId(4L)).willReturn(false);

        // Act
        teamService.delete(4L);

        // Assert
        then(teamRepository).should().delete(existing);
    }

    @Test
    void should_throwBusinessRuleAndDeleteNothing_when_teamIsUsedBySpiel() {
        // Arrange
        given(teamRepository.findById(4L)).willReturn(Optional.of(Team.builder().id(4L).build()));
        given(spielRepository.existsByHeimTeamIdOrGastTeamId(4L, 4L)).willReturn(true);

        // Act
        Throwable thrown = catchThrowable(() -> teamService.delete(4L));

        // Assert
        assertThat(thrown)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Team with id 4 is still used by a Spiel or Competition");
        then(teamRepository).should(never()).delete(any(Team.class));
    }

    @Test
    void should_throwBusinessRuleAndDeleteNothing_when_teamIsUsedByCompetition() {
        // Arrange
        given(teamRepository.findById(4L)).willReturn(Optional.of(Team.builder().id(4L).build()));
        given(spielRepository.existsByHeimTeamIdOrGastTeamId(4L, 4L)).willReturn(false);
        given(competitionTeamRepository.existsByTeamId(4L)).willReturn(true);

        // Act
        Throwable thrown = catchThrowable(() -> teamService.delete(4L));

        // Assert
        assertThat(thrown).isInstanceOf(BusinessRuleException.class);
        then(teamRepository).should(never()).delete(any(Team.class));
    }

    @Test
    void should_throwResourceNotFoundAndDeleteNothing_when_deletingUnknownTeam() {
        // Arrange
        given(teamRepository.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> teamService.delete(42L));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Team with id 42 not found");
        then(teamRepository).should(never()).delete(any(Team.class));
    }
}
