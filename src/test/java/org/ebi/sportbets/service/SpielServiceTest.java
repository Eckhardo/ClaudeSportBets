package org.ebi.sportbets.service;

import org.ebi.sportbets.dto.SpielDto;
import org.ebi.sportbets.entity.CompetitionGroup;
import org.ebi.sportbets.entity.Spiel;
import org.ebi.sportbets.entity.Spieltag;
import org.ebi.sportbets.entity.Team;
import org.ebi.sportbets.exception.BusinessRuleException;
import org.ebi.sportbets.exception.ResourceNotFoundException;
import org.ebi.sportbets.repository.CompetitionGroupRepository;
import org.ebi.sportbets.repository.SpielRepository;
import org.ebi.sportbets.repository.SpieltagRepository;
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
class SpielServiceTest {

    private static final LocalDateTime ANPFIFF = LocalDateTime.of(2026, 8, 21, 20, 30);

    @Mock
    private SpielRepository spielRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private SpieltagRepository spieltagRepository;

    @Mock
    private CompetitionGroupRepository competitionGroupRepository;

    @InjectMocks
    private SpielService spielService;

    private static SpielDto.SpielDtoBuilder validRequest() {
        return SpielDto.builder()
                .id(99L)
                .spielNumber(1)
                .anpfiffDate(ANPFIFF)
                .heimTore(2)
                .gastTore(1)
                .stattgefunden(true);
    }

    @Test
    void should_mapAllFieldsToDto_when_spieleExist() {
        // Arrange
        LocalDateTime createdOn = LocalDateTime.of(2026, 8, 1, 12, 0);
        given(spielRepository.findAll()).willReturn(List.of(Spiel.builder()
                .id(11L)
                .spielNumber(1)
                .anpfiffDate(ANPFIFF)
                .heimTore(2)
                .gastTore(1)
                .stattgefunden(true)
                .heimTeam(Team.builder().id(1L).build())
                .gastTeam(Team.builder().id(2L).build())
                .spieltag(Spieltag.builder().id(8L).build())
                .competitionGroup(CompetitionGroup.builder().id(6L).build())
                .createdOn(createdOn)
                .build()));

        // Act
        List<SpielDto> result = spielService.findAll();

        // Assert
        assertThat(result).containsExactly(validRequest()
                .id(11L)
                .heimTeamId(1L)
                .gastTeamId(2L)
                .spieltagId(8L)
                .competitionGroupId(6L)
                .createdOn(createdOn)
                .build());
    }

    @Test
    void should_mapMissingReferencesToNull_when_spielHasNoReferences() {
        // Arrange
        given(spielRepository.findById(11L)).willReturn(Optional.of(Spiel.builder().id(11L).spielNumber(1).build()));

        // Act
        SpielDto result = spielService.findById(11L);

        // Assert
        assertThat(result)
                .extracting(SpielDto::getHeimTeamId, SpielDto::getGastTeamId, SpielDto::getSpieltagId, SpielDto::getCompetitionGroupId)
                .containsOnlyNulls();
    }

    @Test
    void should_returnSpieleOfSpieltag_when_findBySpieltagId() {
        // Arrange
        given(spielRepository.findBySpieltagIdOrderBySpielNumberAsc(8L)).willReturn(List.of(
                Spiel.builder().id(11L).spielNumber(1).build(),
                Spiel.builder().id(12L).spielNumber(2).build()));

        // Act
        List<SpielDto> result = spielService.findBySpieltagId(8L);

        // Assert
        assertThat(result).extracting(SpielDto::getId).containsExactly(11L, 12L);
    }

    @Test
    void should_throwResourceNotFound_when_spielWithIdDoesNotExist() {
        // Arrange
        given(spielRepository.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> spielService.findById(42L));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Spiel with id 42 not found");
    }

    @Test
    void should_saveWithResolvedReferences_when_allReferencesExist() {
        // Arrange
        given(teamRepository.findById(1L)).willReturn(Optional.of(Team.builder().id(1L).build()));
        given(teamRepository.findById(2L)).willReturn(Optional.of(Team.builder().id(2L).build()));
        given(spieltagRepository.findById(8L)).willReturn(Optional.of(Spieltag.builder().id(8L).build()));
        given(spielRepository.save(any(Spiel.class))).willAnswer(invocation -> {
            Spiel toSave = invocation.getArgument(0);
            toSave.setId(11L);
            return toSave;
        });
        SpielDto request = validRequest().heimTeamId(1L).gastTeamId(2L).spieltagId(8L).build();

        // Act
        SpielDto result = spielService.create(request);

        // Assert
        assertThat(result)
                .extracting(SpielDto::getId, SpielDto::getHeimTeamId, SpielDto::getGastTeamId,
                        SpielDto::getSpieltagId, SpielDto::getCompetitionGroupId)
                .containsExactly(11L, 1L, 2L, 8L, null);
    }

    @Test
    void should_throwResourceNotFoundAndSaveNothing_when_creatingWithUnknownTeam() {
        // Arrange
        given(teamRepository.findById(1L)).willReturn(Optional.empty());
        SpielDto request = validRequest().heimTeamId(1L).gastTeamId(2L).build();

        // Act
        Throwable thrown = catchThrowable(() -> spielService.create(request));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Team with id 1 not found");
        then(spielRepository).should(never()).save(any(Spiel.class));
    }

    @Test
    void should_throwBusinessRuleAndSaveNothing_when_heimAndGastTeamAreEqual() {
        // Arrange
        SpielDto request = validRequest().heimTeamId(1L).gastTeamId(1L).build();

        // Act
        Throwable thrown = catchThrowable(() -> spielService.create(request));

        // Assert
        assertThat(thrown)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Heim team and Gast team must be different");
        then(spielRepository).should(never()).save(any(Spiel.class));
    }

    @Test
    void should_updateFieldsAndClearReferences_when_spielExistsAndRequestHasNoReferences() {
        // Arrange
        LocalDateTime createdOn = LocalDateTime.of(2026, 8, 1, 12, 0);
        Spiel existing = Spiel.builder()
                .id(11L)
                .heimTeam(Team.builder().id(1L).build())
                .spieltag(Spieltag.builder().id(8L).build())
                .createdOn(createdOn)
                .build();
        given(spielRepository.findById(11L)).willReturn(Optional.of(existing));

        // Act
        SpielDto result = spielService.update(11L, validRequest().build());

        // Assert
        assertThat(result).isEqualTo(validRequest().id(11L).createdOn(createdOn).build());
    }

    @Test
    void should_throwResourceNotFound_when_updatingUnknownSpiel() {
        // Arrange
        given(spielRepository.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> spielService.update(42L, validRequest().build()));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Spiel with id 42 not found");
    }

    @Test
    void should_deleteSpiel_when_spielExists() {
        // Arrange
        Spiel existing = Spiel.builder().id(11L).build();
        given(spielRepository.findById(11L)).willReturn(Optional.of(existing));

        // Act
        spielService.delete(11L);

        // Assert
        then(spielRepository).should().delete(existing);
    }

    @Test
    void should_throwResourceNotFoundAndDeleteNothing_when_deletingUnknownSpiel() {
        // Arrange
        given(spielRepository.findById(42L)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> spielService.delete(42L));

        // Assert
        assertThat(thrown)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Spiel with id 42 not found");
        then(spielRepository).should(never()).delete(any(Spiel.class));
    }
}
